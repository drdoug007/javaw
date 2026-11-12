package one.dastec.javaw.bytecode;

import one.dastec.javaw.Wat2Wasm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Shared assemble-and-run support for the executable bytecode tests: {@code wat2wasm} with a content-addressed binary
 * cache, and Node execution through one persistent host process per test JVM.
 *
 * <ul>
 *   <li>{@value #CACHE_DIRECTORY}: directory of cached {@code .wasm} binaries, keyed by the SHA-256 of the WAT text, the
 *   effective {@code wat2wasm} flags and {@code wat2wasm --version}. The build points it at {@code target/wasm-cache}.
 *   Unset disables the cache. Entries unused for {@value #CACHE_DAYS} days are pruned.</li>
 *   <li>{@value #NODE_MODE}: {@code worker} (default) runs each script in a fresh worker thread of a persistent Node
 *   host; {@code process} starts {@code node} for every script, as the tests did originally.</li>
 *   <li>{@value #SKIP_IDENTICAL}: {@code true} (default) skips a Node run when the same script already passed with the
 *   same arguments and file contents in the same directory. Tests that compile one fixture JAR in both entry orders
 *   then execute only once when both orders produce identical WAT; different WAT still runs.</li>
 * </ul>
 */
final class WasmTestSupport {
    static final String CACHE_DIRECTORY = "javaw.wasmCache.dir";
    static final String NODE_MODE = "javaw.test.node";
    static final String SKIP_IDENTICAL = "javaw.test.skipIdenticalRuns";
    private static final int CACHE_DAYS = 7;
    private static final long TIMEOUT_MS = 60_000;
    // Matches the recursion depth of Node's main thread (984 KB V8 stack) within a few percent for JS and Wasm frames;
    // a worker's default stack is about four times deeper.
    private static final double WORKER_STACK_MB = 1.17;

    private static final Map<Path, Set<String>> PASSED = new ConcurrentHashMap<>();
    private static volatile boolean pruned;
    private static NodeHost host;

    private WasmTestSupport() {}

    /** The JavaScript prelude shared by most executable bytecode tests; the assertions run once per fresh instance. */
    static String standardScript(String assertions) {
        return """
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            for(let instance=0;instance<2;instance++) {
                const memory=new WebAssembly.Memory({initial:2});let heap=1024,allocations=0,failAllocation=false;
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    allocations++;if(failAllocation){failAllocation=false;return 0;}
                    const address=heap;heap+=size;
                    while(heap>memory.buffer.byteLength)memory.grow(1);
                    new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                }}}).exports;
                assert.equal(allocations,0);assert.equal(e.__gc_heap_bytes?.()??0,0);if(e.__gc_scribble)e.__gc_scribble.value=1;heap=Math.max(heap,e.__exception_heap_base?.value??0);
                function text(pointer){if(!pointer)return null;const view=new DataView(memory.buffer);
                    const chars=view.getInt32(pointer+4,true),length=view.getInt32(chars+4,true);
                    let result='';for(let i=0;i<length;i++)result+=String.fromCharCode(view.getUint16(chars+8+2*i,true));return result;}
            """+assertions+"\n}";
    }

    /** Assembles {@code wat} and runs {@code assertions} under {@link #standardScript} in {@code directory}. */
    static void execute(Path directory, String wat, String assertions) throws Exception {
        Path input=directory.resolve("metadata.wat"),output=directory.resolve("metadata.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);
        assemble(input,output,Wat2Wasm.EXCEPTIONS);
        Files.writeString(script,standardScript(assertions));
        node(script,output);
    }

    /** Assembles {@code input} into {@code output} with {@code wat2wasm}, reusing a cached binary when one exists. */
    static void assemble(Path input, Path output, String... flags) throws Exception {
        List<String> command=Wat2Wasm.command(input,output,flags);
        Path cache=cacheDirectory();
        if(cache==null){run(output.resolveSibling(output.getFileName()+".log"),command);return;}
        String key=hash(digest -> {
            digest.update(Wat2Wasm.version().getBytes(StandardCharsets.UTF_8));
            digest.update((byte)0);
            digest.update(String.join(" ",command.subList(1,command.size()-3)).getBytes(StandardCharsets.UTF_8));
            digest.update((byte)0);
            update(digest,input);
        });
        Path cached=cache.resolve(key+".wasm");
        try {
            Files.copy(cached,output,StandardCopyOption.REPLACE_EXISTING);
            Files.setLastModifiedTime(cached,FileTime.from(Instant.now()));
            return;
        } catch(NoSuchFileException missing) {
            // Not cached yet; assemble below.
        } catch(IOException unreadable) {
            // Another fork may be pruning or replacing the entry; assemble below.
        }
        run(output.resolveSibling(output.getFileName()+".log"),command);
        Path temporary=cache.resolve(key+"."+ProcessHandle.current().pid()+"."+System.nanoTime()+".tmp");
        try {
            Files.copy(output,temporary);
            Files.move(temporary,cached,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } catch(IOException error) {
            Files.deleteIfExists(temporary);
        }
    }

    /** Runs {@code node script args...}, failing with the script's output when it exits with a nonzero code. */
    static void node(Path script, Path... args) throws Exception {
        boolean skip=!"false".equals(System.getProperty(SKIP_IDENTICAL));
        Path scope=script.toAbsolutePath().getParent();
        String key=skip?hash(digest -> {
            update(digest,script);
            for(Path arg:args){digest.update((byte)0);digest.update(arg.toString().getBytes(StandardCharsets.UTF_8));if(Files.isRegularFile(arg))update(digest,arg);}
        }):null;
        if(skip&&PASSED.getOrDefault(scope,Set.of()).contains(key))return;
        if("process".equals(System.getProperty(NODE_MODE,"worker"))) {
            List<String> command=new ArrayList<>(List.of("node",script.toString()));
            for(Path arg:args)command.add(arg.toString());
            run(scope.resolve("command.log"),command);
        } else {
            List<String> arguments=new ArrayList<>();
            for(Path arg:args)arguments.add(arg.toString());
            NodeHost.Result result=host().run(script.toAbsolutePath().toString(),arguments,TIMEOUT_MS);
            assertEquals(0,result.code(),result.output());
        }
        if(skip)PASSED.computeIfAbsent(scope,ignored -> ConcurrentHashMap.newKeySet()).add(key);
    }

    /** Runs a command with a {@value #TIMEOUT_MS} ms limit, failing with its combined output on a nonzero exit. */
    static void run(Path log, List<String> command) throws Exception {
        Process process=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(TIMEOUT_MS,TimeUnit.MILLISECONDS)){process.destroyForcibly();fail("Command timed out: "+String.join(" ",command));}
        assertEquals(0,process.exitValue(),Files.readString(log));
    }

    private static synchronized NodeHost host() throws IOException {
        if(host==null||!host.alive())host=NodeHost.start(WORKER_STACK_MB);
        return host;
    }

    private static Path cacheDirectory() throws IOException {
        String configured=System.getProperty(CACHE_DIRECTORY);
        if(configured==null||configured.isBlank())return null;
        Path directory=Files.createDirectories(Path.of(configured));
        if(!pruned) {
            pruned=true;
            Instant cutoff=Instant.now().minus(Duration.ofDays(CACHE_DAYS));
            try(Stream<Path> entries=Files.list(directory)) {
                for(Path entry:(Iterable<Path>)entries::iterator) {
                    try{if(Files.getLastModifiedTime(entry).toInstant().isBefore(cutoff))Files.deleteIfExists(entry);}
                    catch(IOException ignored){/* Another fork removed or replaced it. */}
                }
            }
        }
        return directory;
    }

    private interface DigestBody { void accept(MessageDigest digest) throws IOException; }

    private static String hash(DigestBody body) throws IOException {
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            body.accept(digest);
            return HexFormat.of().formatHex(digest.digest());
        } catch(NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void update(MessageDigest digest, Path file) throws IOException {
        digest.update(Files.readAllBytes(file));
    }

    /** One {@code node node-host.cjs} process; requests are serialized, and a request that times out kills the host. */
    private static final class NodeHost {
        record Result(int code, String output) {}
        private static final String EXIT="\u0000exit";

        private final Process process;
        private final double stackSizeMb;
        private final OutputStream input;
        private final BlockingQueue<String> replies=new LinkedBlockingQueue<>();
        private long next;

        private NodeHost(Process process, double stackSizeMb) {
            this.process=process;
            this.stackSizeMb=stackSizeMb;
            this.input=process.getOutputStream();
            Thread reader=new Thread(() -> {
                try(BufferedReader lines=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))) {
                    for(String line;(line=lines.readLine())!=null;)replies.add(line);
                } catch(IOException ignored) {
                    // The host died; report it to the waiting request below.
                } finally {
                    replies.add(EXIT);
                }
            },"node-host-reader");
            reader.setDaemon(true);
            reader.start();
        }

        static NodeHost start(double stackSizeMb) throws IOException {
            Path script;
            try{script=Path.of(Objects.requireNonNull(WasmTestSupport.class.getResource("node-host.cjs"),"node-host.cjs").toURI());}
            catch(URISyntaxException e){throw new IOException(e);}
            Process process=new ProcessBuilder("node",script.toString()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
            Runtime.getRuntime().addShutdownHook(new Thread(process::destroyForcibly));
            return new NodeHost(process,stackSizeMb);
        }

        boolean alive() { return process.isAlive(); }

        synchronized Result run(String script, List<String> args, long timeoutMs) throws IOException, InterruptedException {
            String id=Long.toString(++next);
            StringBuilder request=new StringBuilder(id).append('\t').append(timeoutMs).append('\t').append(stackSizeMb).append('\t').append(base64(script));
            for(String arg:args)request.append('\t').append(base64(arg));
            input.write(request.append('\n').toString().getBytes(StandardCharsets.UTF_8));
            input.flush();
            // The host enforces the timeout itself; this longer limit only catches a host that stopped responding.
            String reply=replies.poll(timeoutMs+30_000,TimeUnit.MILLISECONDS);
            if(reply==null||reply.equals(EXIT)) {
                process.destroyForcibly();
                throw new IOException(reply==null?"Node host stopped responding":"Node host exited with code "+process.waitFor());
            }
            String[] fields=reply.split("\t",3);
            if(!fields[0].equals(id))throw new IOException("Node host replied to request "+fields[0]+" instead of "+id);
            return new Result(Integer.parseInt(fields[1]),new String(Base64.getDecoder().decode(fields.length>2?fields[2]:""),StandardCharsets.UTF_8));
        }

        private static String base64(String value) {
            return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }
    }
}
