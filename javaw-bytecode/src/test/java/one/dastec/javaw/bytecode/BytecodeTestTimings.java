package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.extension.*;
import org.opentest4j.TestAbortedException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Records wall and bytecode-compile time per test when {@code javaw.compileTimes.dir} is set (the build sets it), one
 * TSV file per test class. Each test class also gets a {@code (class)} row for time outside its tests, such as static
 * setup and test-instance construction. Summarized by {@code javaw-bytecode/test-timings.py}.
 */
public final class BytecodeTestTimings implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {
    static final String DIRECTORY = "javaw.compileTimes.dir";
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(BytecodeTestTimings.class);

    private record Mark(long wall, long compile, long compiles, StringBuilder rows) {
        static Mark now(StringBuilder rows) { return new Mark(System.nanoTime(), BytecodeCompileClock.nanos(), BytecodeCompileClock.count(), rows); }
    }

    @Override public void beforeAll(ExtensionContext context) {
        if (System.getProperty(DIRECTORY) != null) context.getStore(NAMESPACE).put("class", Mark.now(new StringBuilder()));
    }

    @Override public void beforeEach(ExtensionContext context) {
        // Parameterized and repeated tests sit below a template context, so search every ancestor for the class.
        for (ExtensionContext parent = context.getParent().orElse(null); parent != null; parent = parent.getParent().orElse(null)) {
            Mark type = parent.getStore(NAMESPACE).get("class", Mark.class);
            if (type != null) { context.getStore(NAMESPACE).put("test", Mark.now(type.rows())); return; }
        }
    }

    @Override public void afterEach(ExtensionContext context) {
        Mark start = context.getStore(NAMESPACE).remove("test", Mark.class);
        if (start == null) return;
        Mark end = Mark.now(start.rows());
        String outcome = context.getExecutionException().map(error -> error instanceof TestAbortedException ? "aborted" : "failed").orElse("passed");
        // Name the method, adding the display name of a parameterized or repeated invocation.
        String method = context.getRequiredTestMethod().getName(), name = context.getDisplayName();
        row(start.rows(), context.getRequiredTestClass().getName(), name.startsWith(method + "(") ? name : method + "() " + name, outcome, start, end);
    }

    @Override public void afterAll(ExtensionContext context) {
        Mark start = context.getStore(NAMESPACE).remove("class", Mark.class);
        if (start == null) return;
        Mark end = Mark.now(start.rows());
        // The class row covers the whole class; the summary subtracts its tests to get the time outside them.
        row(start.rows(), context.getRequiredTestClass().getName(), "(class)", "total", start, end);
        // One file per class, replaced on each run like the surefire XML reports. Timing never fails a test class.
        try {
            Path directory = Files.createDirectories(Path.of(System.getProperty(DIRECTORY)));
            Files.writeString(directory.resolve(context.getRequiredTestClass().getName() + ".tsv"), start.rows(), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException error) {
            System.err.println("BytecodeTestTimings: could not write timings: " + error);
        }
    }

    private static void row(StringBuilder rows, String type, String test, String outcome, Mark start, Mark end) {
        rows.append(type).append('\t').append(test.replace('\t', ' ').replace('\n', ' ')).append('\t').append(outcome)
                .append('\t').append((end.wall() - start.wall()) / 1_000_000)
                .append('\t').append((end.compile() - start.compile()) / 1_000_000)
                .append('\t').append(end.compiles() - start.compiles()).append('\n');
    }
}
