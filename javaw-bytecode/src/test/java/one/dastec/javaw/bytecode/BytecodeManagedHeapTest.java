package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeManagedHeapTest {
    @TempDir Path directory;

    private static final String SOURCE = """
            public class Heap {
                static final class Node { Node next; Object value; }
                static java.util.ArrayList<int[]> kept = new java.util.ArrayList<>();
                static Node chain;
                // About 4 MB of garbage per call: one call needs more than the former 20-page memory.
                public static int churn(int rounds) {
                    int sum = 0;
                    for (int i = 0; i < rounds; i++) {
                        int[] block = new int[1024]; block[i % 1024] = i; sum += block[i % 1024];
                        sum += ("x" + i).length();
                    }
                    return sum;
                }
                public static int keep(int count) {
                    for (int i = 0; i < count; i++) { int[] block = new int[1024]; block[1023] = i; kept.add(block); }
                    return kept.size();
                }
                public static int checksum() { int sum = 0; for (int[] block : kept) sum += block[1023]; return sum; }
                public static int release() { int size = kept.size(); kept = new java.util.ArrayList<>(); return size; }
                public static int cycle(int count) {
                    Node first = new Node(), current = first;
                    for (int i = 0; i < count; i++) { current.next = new Node(); current = current.next; current.value = Integer.valueOf(i * 1000); }
                    current.next = first;
                    return count;
                }
                public static int buildChain(int count) {
                    Node head = null;
                    for (int i = 0; i < count; i++) { Node node = new Node(); node.next = head; node.value = String.valueOf(i); head = node; }
                    chain = head;
                    return count;
                }
                public static int chainLength() { int sum = 0; for (Node node = chain; node != null; node = node.next) sum += ((String) node.value).length(); return sum; }
                public static int dropChain() { chain = null; return 0; }
                public static String make() { return new StringBuilder("retained-").append(42).toString(); }
                // A hidden parent field still occupies, and may be the only reference from, its own slot.
                static class Parent { Object value; Object parentValue() { return value; } }
                static class Hiding extends Parent { Object value; }
                static class StaticHiding extends Parent { static Object value; }
                static Parent hidden, staticHidden;
                public static int hide() {
                    Hiding hiding = new Hiding(); ((Parent) hiding).value = new int[]{12345}; hiding.value = "own"; hidden = hiding;
                    StaticHiding other = new StaticHiding(); ((Parent) other).value = new int[]{678}; staticHidden = other;
                    return 0;
                }
                // Reuses freed blocks of the hidden values' size, overwriting them if they were wrongly reclaimed.
                public static int scribble() { int sum = 0; for (int i = 0; i < 4000; i++) sum += new int[]{-1}[0]; return sum; }
                public static int hiddenValues() { return ((int[]) hidden.parentValue())[0] + ((int[]) staticHidden.parentValue())[0]; }
                // Fill a bounded memory with 16-byte nodes, free two of every three, then refill with 24-byte objects.
                static final class Wide { Wide next; int a, b, c, d; }
                static Wide wides;
                public static int fill() {
                    int count = 0;
                    try { while (true) { Node node = new Node(); node.next = chain; chain = node; count++; } }
                    catch (OutOfMemoryError error) { return count; }
                }
                public static int thin() {
                    int kept = 0;
                    for (Node node = chain; node != null; node = node.next) {
                        Node skipped = node.next;
                        node.next = skipped == null || skipped.next == null ? null : skipped.next.next;
                        kept++;
                    }
                    return kept;
                }
                public static int refill() {
                    int count = 0;
                    try { while (true) { Wide wide = new Wide(); wide.next = wides; wides = wide; count++; } }
                    catch (OutOfMemoryError error) { return count; }
                }
            }
            """;

    @Test void reclaimsGarbageAboveTheFormerMemoryLimitAndMatchesTheJvm() throws Exception {
        byte[] jar = compile();
        String wat = BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar), BytecodeCompiler.Memory.managed());
        assertTrue(wat.contains("(import \"env\" \"memory\" (memory 2))"), "memory has no compiled maximum");
        assertFalse(wat.contains("(import \"env\" \"__heap_alloc\""), "the module owns its allocator");
        Map<String, Object> jvm = new LinkedHashMap<>();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{directory.toUri().toURL()}, null)) {
            Class<?> type = loader.loadClass("Heap");
            jvm.put("churn", type.getMethod("churn", int.class).invoke(null, 1000));
            jvm.put("keep", type.getMethod("keep", int.class).invoke(null, 512));
            jvm.put("checksum", type.getMethod("checksum").invoke(null));
            jvm.put("release", type.getMethod("release").invoke(null));
            jvm.put("chain", type.getMethod("buildChain", int.class).invoke(null, 50000));
            jvm.put("chainLength", type.getMethod("chainLength").invoke(null));
            type.getMethod("dropChain").invoke(null);
            type.getMethod("hide").invoke(null);
            jvm.put("hiddenValues", type.getMethod("hiddenValues").invoke(null));
            jvm.put("make", type.getMethod("make").invoke(null));
        }
        execute(wat, "{initial:2}", """
            const pages=()=>memory.buffer.byteLength/65536;
            // Warm up: the first call initializes classes and interns constants, which stay live.
            assert.equal(e['Heap.churn'](1000),%s);e.__gc_collect();const base=e.__gc_live_bytes();
            let settled=0;
            for(let round=0;round<24;round++){
                assert.equal(e['Heap.churn'](1000),%s);
                e.__gc_collect();
                if(round===3)settled=pages();
                if(round>3)assert.equal(pages(),settled,'garbage is reused after collection');
                assert.equal(e.__gc_live_bytes(),base,'churn leaves no live garbage');
            }
            assert.ok(settled>20,'one call needs more than the former 20-page maximum');
            assert.equal(e['Heap.keep'](512),%s);e.__gc_collect();
            assert.ok(e.__gc_live_bytes()-base>2*1024*1024,'live static data above the former limit survives collection');
            assert.equal(e['Heap.checksum'](),%s);
            assert.equal(e['Heap.release'](),%s);
            const reclaimed=e.__gc_collect();assert.ok(reclaimed>2*1024*1024,'released data is reclaimed');
            for(let round=0;round<3;round++){e['Heap.cycle'](20000);assert.ok(e.__gc_collect()>0,'cycles are reclaimed');}
            const afterCycles=e.__gc_live_bytes();
            e['Heap.cycle'](20000);e.__gc_collect();assert.equal(e.__gc_live_bytes(),afterCycles,'repeated cycles keep live bytes stable');
            assert.equal(e['Heap.buildChain'](50000),%s);e.__gc_collect();
            assert.equal(e['Heap.chainLength'](),%s,'a long chain survives collection');
            e['Heap.dropChain']();e.__gc_collect();
            e['Heap.hide']();for(let round=0;round<2;round++){e['Heap.churn'](200);e.__gc_collect();e['Heap.scribble']();}
            assert.equal(e['Heap.hiddenValues'](),%s,'hidden parent reference slots are traced');
            // The first make() caches its text constants permanently; the second must return to the same live bytes.
            let settledLive;
            for(let attempt=0;attempt<2;attempt++){
                const kept=e.__gc_retain(e['Heap.make']());
                for(let round=0;round<3;round++){e['Heap.churn'](200);e.__gc_collect();}
                assert.equal(text(kept),%s,'a retained host pointer survives collection');
                const live=e.__gc_live_bytes();e.__gc_release(kept);e.__gc_collect();
                assert.ok(e.__gc_live_bytes()<live,'a released pointer is reclaimed');
                if(attempt===0)settledLive=e.__gc_live_bytes();
                else assert.equal(e.__gc_live_bytes(),settledLive,'retain/release cycles keep live bytes stable');
            }
            """.formatted(jvm.get("churn"), jvm.get("churn"), jvm.get("keep"), jvm.get("checksum"), jvm.get("release"), jvm.get("chain"),
                jvm.get("chainLength"), jvm.get("hiddenValues"), js((String) jvm.get("make"))));
    }

    @Test void exhaustedMemoryThrowsOutOfMemoryErrorAndRecovers() throws Exception {
        byte[] jar = compile();
        String wat = BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar), new BytecodeCompiler.Memory(2, 64, true));
        assertTrue(wat.contains("(import \"env\" \"memory\" (memory 2 64))"));
        execute(wat, "{initial:2,maximum:64}", """
            let error;
            try{e['Heap.keep'](100000);}catch(thrown){error=thrown;}
            assert.ok(error instanceof WebAssembly.Exception && error.is(e.__java_exception),'exhaustion is a Java exception');
            assert.equal(text(e['java.lang.Throwable.toString'](error.getArg(e.__java_exception,0))),'java.lang.OutOfMemoryError');
            assert.ok(memory.buffer.byteLength>60*65536,'exhaustion happens only near the compiled maximum');
            e.__reset_exception_trace();
            // Collect with the live set still at the maximum: the mark stack cannot grow and must recover by rescanning.
            const count=e['Heap.keep'](0);e.__gc_collect();
            assert.ok(e.__gc_mark_overflows.value>0,'the bounded mark stack overflowed');
            assert.equal(e['Heap.checksum'](),count*(count-1)/2,'every kept block survives an overflowing collection');
            assert.equal(e['Heap.release'](),count);e.__gc_collect();
            assert.equal(e['Heap.keep'](16),16,'allocation succeeds again after collection');
            e['Heap.release']();e.__gc_collect();
            // Coalesced 40-byte runs left by freed 16-byte pairs must serve 24-byte requests without growing memory.
            const nodes=e['Heap.fill']();e.__reset_exception_trace();
            const kept=e['Heap.thin']();e.__gc_collect();
            const refilled=e['Heap.refill']();e.__reset_exception_trace();
            assert.ok(refilled>=0.9*(nodes-kept)/2,`freed small blocks are reused by larger small requests (${refilled} of ${nodes-kept} freed)`);
            """);
    }

    @Test void memoryRejectsMaximumBelowInitial() {
        var failure = assertThrows(IllegalArgumentException.class, () -> new BytecodeCompiler.Memory(4, 2, true));
        assertTrue(failure.getMessage().contains("maximum memory"));
    }

    @Test void managedHeapRequiresTheBundledRuntime() throws Exception {
        byte[] jar = compile();
        List<org.objectweb.asm.tree.ClassNode> classes = new ArrayList<>();
        try (JarInputStream input = new JarInputStream(new ByteArrayInputStream(jar))) {
            for (JarEntry entry; (entry = input.getNextJarEntry()) != null; ) classes.add(BytecodeInput.read(input.readAllBytes()));
        }
        var failure = assertThrows(IllegalArgumentException.class,
                () -> BytecodeCompiler.compileClasses(classes, false, List.of(), Set.of(), BytecodeCompiler.Memory.managed()));
        assertTrue(failure.getMessage().contains("bundled exception runtime"), failure.getMessage());
    }

    private byte[] compile() throws Exception {
        Path file = directory.resolve("Heap.java");
        Files.writeString(file, SOURCE);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-g", "--release", "17", "-d", directory.toString(), file.toString()));
        List<Path> files;
        try (var paths = Files.walk(directory)) { files = paths.filter(path -> path.toString().endsWith(".class")).sorted().toList(); }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(bytes)) {
            for (Path path : files) { jar.putNextEntry(new JarEntry(directory.relativize(path).toString())); jar.write(Files.readAllBytes(path)); jar.closeEntry(); }
        }
        return bytes.toByteArray();
    }

    private String js(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) out.append(String.format("\\u%04x", (int) c));
        return out.append('"').toString();
    }

    private void run(String... arguments) throws Exception {
        Path log = directory.resolve("command.log");
        Process process = new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(120, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("Command timed out"); }
        assertEquals(0, process.exitValue(), Files.readString(log));
    }

    /** No host allocator: the module allocates, grows and reclaims its imported memory itself. */
    private void execute(String wat, String memory, String assertions) throws Exception {
        Path input = directory.resolve("heap.wat"), output = directory.resolve("heap.wasm"), script = directory.resolve("run.cjs");
        Files.writeString(input, wat);
        run("wat2wasm", "--enable-exceptions", input.toString(), "-o", output.toString());
        Files.writeString(script, """
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const memory=new WebAssembly.Memory(%s);
            new Uint8Array(memory.buffer).fill(0x7f,1024);
            const e=new WebAssembly.Instance(new WebAssembly.Module(fs.readFileSync(process.argv[2])),{env:{memory}}).exports;
            function text(pointer){if(!pointer)return null;const view=new DataView(memory.buffer);
                const chars=view.getInt32(pointer+4,true),length=view.getInt32(chars+4,true);
                let result='';for(let i=0;i<length;i++)result+=String.fromCharCode(view.getUint16(chars+8+2*i,true));return result;}
            """.formatted(memory) + assertions);
        run("node", script.toString(), output.toString());
    }
}
