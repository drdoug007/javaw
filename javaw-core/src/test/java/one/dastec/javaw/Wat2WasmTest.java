package one.dastec.javaw;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class Wat2WasmTest {
    // Abridged `wat2wasm --help` from wabt 1.0.34, where exceptions, multi-memory and annotations are opt-in.
    private static final String WABT_1_0_34 = """
              --enable-exceptions                      Enable Experimental exception handling
              --disable-mutable-globals                Disable Import/export mutable globals
              --enable-threads                         Enable Threading support
              --enable-multi-memory                    Enable Multi-memory
              --enable-annotations                     Enable Custom annotation syntax
              --debug-names                            Write debug names to the generated binary file
            """;
    // Abridged `wat2wasm --help` from wabt 1.0.42, where those three features are on by default.
    private static final String WABT_1_0_42 = """
              --disable-exceptions                     Disable Experimental exception handling
              --enable-threads                         Enable Threading support
              --disable-multi-memory                   Disable Multi-memory
              --disable-annotations                    Disable Custom annotation syntax
              --debug-names                            Write debug names to the generated binary file
            """;

    @Test void keepsOptInFeatureFlagsForOlderWabt() {
        assertEquals(List.of("--enable-threads", "--enable-exceptions", "--enable-multi-memory", "--enable-annotations", "--debug-names"),
                Wat2Wasm.supportedFlags(Wat2Wasm.options(WABT_1_0_34), Wat2Wasm.THREADS, Wat2Wasm.EXCEPTIONS, Wat2Wasm.MULTI_MEMORY, Wat2Wasm.ANNOTATIONS, "--debug-names"));
    }

    @Test void dropsFeatureFlagsThatNewerWabtEnablesByDefault() {
        assertEquals(List.of("--enable-threads", "--debug-names"),
                Wat2Wasm.supportedFlags(Wat2Wasm.options(WABT_1_0_42), Wat2Wasm.THREADS, Wat2Wasm.EXCEPTIONS, Wat2Wasm.MULTI_MEMORY, Wat2Wasm.ANNOTATIONS, "--debug-names"));
    }

    @Test void keepsEveryFlagWhenHelpIsUnavailable() {
        assertEquals(List.of("--enable-exceptions"), Wat2Wasm.supportedFlags(Set.of(), Wat2Wasm.EXCEPTIONS));
    }

    @Test void assemblesExceptionHandlingWithTheInstalledWabt(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        Path input = directory.resolve("eh.wat"), output = directory.resolve("eh.wasm");
        Files.writeString(input, """
                (module (tag $e (param i32))
                  (func (export "f") (result i32)
                    (try (result i32) (do (i32.const 1) (throw $e)) (catch $e))))
                """);
        Process process = new ProcessBuilder(Wat2Wasm.command(input, output, Wat2Wasm.EXCEPTIONS, Wat2Wasm.MULTI_MEMORY, Wat2Wasm.ANNOTATIONS))
                .redirectErrorStream(true).start();
        String log = new String(process.getInputStream().readAllBytes());
        assertEquals(0, process.waitFor(), log);
        assertTrue(Files.size(output) > 8);
    }
}
