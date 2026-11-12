package one.dastec.javaw.bytecode;

import one.dastec.javaw.Wat2Wasm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class WasmTestSupportTest {
    @TempDir Path directory;

    @Test void workerSeesArgumentsAndAWorkerSizedStack() throws Exception {
        Path script=directory.resolve("args.cjs"),log=directory.resolve("seen.txt");
        Files.writeString(script,"""
            const fs=require('node:fs'),assert=require('node:assert/strict');
            let depth=0;try{(function r(){depth++;r();})();}catch(e){assert.ok(e instanceof RangeError);}
            fs.writeFileSync(process.argv[2],process.argv.slice(3).join(',')+'/'+(depth>5000&&depth<20000));
            """);
        WasmTestSupport.node(script,log,Path.of("a b"),Path.of("c"));
        assertEquals("a b,c/true",Files.readString(log));
    }

    @Test void failuresReportOutputAndExitCodes() throws Exception {
        Path thrown=directory.resolve("thrown.cjs"),exited=directory.resolve("exited.cjs");
        Files.writeString(thrown,"console.log('before');require('node:assert/strict').equal(1,2,'mismatch');");
        AssertionFailedError failure=assertThrows(AssertionFailedError.class,() -> WasmTestSupport.node(thrown));
        assertTrue(failure.getMessage().contains("before"),failure.getMessage());
        assertTrue(failure.getMessage().contains("mismatch"),failure.getMessage());
        Files.writeString(exited,"process.stderr.write('bye');process.exit(3);");
        failure=assertThrows(AssertionFailedError.class,() -> WasmTestSupport.node(exited));
        assertTrue(failure.getMessage().contains("bye")&&failure.getMessage().contains("<3>"),failure.getMessage());
        // A failed run is not remembered, so it fails again.
        assertThrows(AssertionFailedError.class,() -> WasmTestSupport.node(exited));
    }

    @Test void skipsOnlyRepeatsOfAPassedRunWithIdenticalFiles() throws Exception {
        Path script=directory.resolve("count.cjs"),input=directory.resolve("input.txt"),count=directory.resolve("count.txt");
        // The count file is named in the script, not passed as an argument, so its changes do not alter the run's key.
        Files.writeString(script,"const fs=require('node:fs');fs.appendFileSync(__dirname+'/count.txt',fs.readFileSync(process.argv[2],'utf8'));");
        Files.writeString(input,"a");
        WasmTestSupport.node(script,input);
        WasmTestSupport.node(script,input);
        assertEquals("a",Files.readString(count));
        Files.writeString(input,"b");
        WasmTestSupport.node(script,input);
        assertEquals("ab",Files.readString(count));
    }

    @Test void reusesTheCachedBinaryForIdenticalWatAndFlags() throws Exception {
        Path cache=directory.resolve("cache");
        String previous=System.setProperty(WasmTestSupport.CACHE_DIRECTORY,cache.toString());
        try {
            String wat="(module (tag $e (param i32)) (func (export \"f\") (result i32) (try (result i32) (do (i32.const 1) (throw $e)) (catch $e))))";
            Path first=directory.resolve("first.wat"),second=directory.resolve("second.wat");
            Files.writeString(first,wat);Files.writeString(second,wat);
            WasmTestSupport.assemble(first,directory.resolve("first.wasm"),Wat2Wasm.EXCEPTIONS);
            assertEquals(1,Files.list(cache).count());
            // Corrupt the entry: a hit copies it instead of running wat2wasm.
            Path entry;try(var entries=Files.list(cache)){entry=entries.findFirst().orElseThrow();}
            Files.write(entry,new byte[]{0,'a','s','m',9});
            WasmTestSupport.assemble(second,directory.resolve("second.wasm"),Wat2Wasm.EXCEPTIONS);
            assertArrayEquals(new byte[]{0,'a','s','m',9},Files.readAllBytes(directory.resolve("second.wasm")));
            // Different flags miss and assemble a real module.
            WasmTestSupport.assemble(second,directory.resolve("third.wasm"),Wat2Wasm.EXCEPTIONS,"--debug-names");
            assertEquals(2,Files.list(cache).count());
            byte[] third=Files.readAllBytes(directory.resolve("third.wasm"));
            assertArrayEquals(new byte[]{0,'a','s','m'},Arrays.copyOf(third,4));
        } finally {
            if(previous==null)System.clearProperty(WasmTestSupport.CACHE_DIRECTORY);else System.setProperty(WasmTestSupport.CACHE_DIRECTORY,previous);
        }
    }
}
