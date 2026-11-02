package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.extension.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Fingerprints every outermost compilation's result when {@code javaw.compileOutputs.dir} is set: a SHA-256 of the WAT
 * text, or of the thrown exception's class and message. Writes one TSV per test class, a row per compilation in order.
 * Comparing two runs' directories shows whether a compiler change altered any output or diagnostic.
 */
public final class BytecodeCompileOutputs implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {
    static final String DIRECTORY = "javaw.compileOutputs.dir";
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(BytecodeCompileOutputs.class);
    private static final List<String> OUTCOMES = Collections.synchronizedList(new ArrayList<>());

    private record Rows(StringBuilder text) {}

    @Override public void beforeAll(ExtensionContext context) {
        if (System.getProperty(DIRECTORY) == null) return;
        context.getStore(NAMESPACE).put("class", new Rows(new StringBuilder()));
        OUTCOMES.clear();
        BytecodeCompileClock.observer = outcome -> OUTCOMES.add(fingerprint(outcome));
    }

    @Override public void beforeEach(ExtensionContext context) { flush(context, "(setup)"); }

    @Override public void afterEach(ExtensionContext context) {
        String method = context.getRequiredTestMethod().getName(), name = context.getDisplayName();
        flush(context, name.startsWith(method + "(") ? name : method + "() " + name);
    }

    @Override public void afterAll(ExtensionContext context) {
        Rows rows = context.getStore(NAMESPACE).get("class", Rows.class);
        if (rows == null) return;
        flush(context, "(teardown)");
        BytecodeCompileClock.observer = null;
        context.getStore(NAMESPACE).remove("class");
        try {
            Path directory = Files.createDirectories(Path.of(System.getProperty(DIRECTORY)));
            Files.writeString(directory.resolve(context.getRequiredTestClass().getName() + ".tsv"), rows.text(), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException error) {
            System.err.println("BytecodeCompileOutputs: could not write fingerprints: " + error);
        }
    }

    /** Attributes the compilations recorded since the last flush to {@code test}. */
    private static void flush(ExtensionContext context, String test) {
        Rows rows = null;
        for (ExtensionContext current = context; current != null && rows == null; current = current.getParent().orElse(null))
            rows = current.getStore(NAMESPACE).get("class", Rows.class);
        if (rows == null) return;
        List<String> outcomes;
        synchronized (OUTCOMES) { outcomes = new ArrayList<>(OUTCOMES); OUTCOMES.clear(); }
        for (int i = 0; i < outcomes.size(); i++)
            rows.text().append(test.replace('\t', ' ').replace('\n', ' ')).append('\t').append(i).append('\t').append(outcomes.get(i)).append('\n');
    }

    private static String fingerprint(Object outcome) {
        String text = outcome instanceof Throwable error ? error.getClass().getName() + ": " + error.getMessage() : String.valueOf(outcome);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return (outcome instanceof Throwable ? "error " : "wat ") + HexFormat.of().formatHex(digest) + "\t" + text.length();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }
}
