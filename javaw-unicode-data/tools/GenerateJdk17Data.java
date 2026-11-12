import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/**
 * Generates the Java 17 / Unicode 13 data the bundled runtime and its tests need, under target/. The build runs this
 * in the generate-resources phase with its own JDK (single-file source mode); each table is then produced by
 * {@code Jdk17Tables.java} running on a JDK 17 installation, never committed:
 *
 * <pre>java GenerateJdk17Data.java --tools=&lt;this directory&gt; --java-home=&lt;JDK 17 or empty&gt;
 *      --resources=&lt;dir&gt;</pre>
 *
 * Runtime sources go under exception-runtime/javaw/runtime/; the test oracle goes under javaw-unicode-data/, outside the
 * runtime's resource tree, in the same jar (a test jar would vanish under -Dmaven.test.skip=true). Outputs are
 * regenerated only when a tool source or the JDK 17 installation changes.
 */
public class GenerateJdk17Data {
    /** Table, path below the resource directory, and the JDK 17 options its generator needs. */
    private record Table(String name, String path, List<String> options) {}

    private static final List<Table> TABLES = List.of(
            new Table("unicode-case", "exception-runtime/javaw/runtime/UnicodeCase.java", List.of()),
            new Table("unicode-properties", "exception-runtime/javaw/runtime/UnicodeProperties.java", List.of()),
            new Table("string-uppercase", "exception-runtime/javaw/runtime/StringUpperData.java",
                    List.of("--add-exports", "java.base/sun.text=ALL-UNNAMED")),
            new Table("string-lowercase", "exception-runtime/javaw/runtime/StringLowerData.java",
                    List.of("--add-opens", "java.base/sun.text=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                            "-Djava.locale.providers=CLDR")),
            new Table("character-traversal", "javaw-unicode-data/character-traversal-jdk17.txt", List.of()));

    public static void main(String[] args) throws Exception {
        Map<String, String> options = new HashMap<>();
        for (String arg : args) {
            int equals = arg.indexOf('=');
            if (!arg.startsWith("--") || equals < 0) throw new IllegalArgumentException("expected --name=value, got " + arg);
            options.put(arg.substring(2, equals), arg.substring(equals + 1));
        }
        // Single-file source mode gives the program no path to its own source, so the build passes the tools directory.
        Path tools = Path.of(require(options, "tools")).toAbsolutePath();
        Path main = Path.of(require(options, "resources"));
        String[] jdk = jdk17(options.getOrDefault("java-home", ""));
        Path home = Path.of(jdk[0]);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        List<String> stampParts = new ArrayList<>(List.of(home.toString(), jdk[1]));
        for (String source : List.of("GenerateJdk17Data.java", "Jdk17Tables.java"))
            stampParts.add(HexFormat.of().formatHex(digest.digest(Files.readAllBytes(tools.resolve(source)))));
        String stampText = HexFormat.of().formatHex(digest.digest(String.join("\n", stampParts).getBytes(StandardCharsets.UTF_8)));
        Path stamp = main.toAbsolutePath().getParent().resolve(".jdk17-data.stamp"); // outside the resource root, so not packaged
        boolean current = Files.isRegularFile(stamp) && Files.readString(stamp).equals(stampText)
                && TABLES.stream().allMatch(table -> Files.isRegularFile(main.resolve(table.path())));
        if (current) { System.out.println("Java 17 data tables are up to date"); return; }

        ExecutorService pool = Executors.newFixedThreadPool(TABLES.size());
        try {
            List<Future<String>> results = new ArrayList<>();
            for (Table table : TABLES) results.add(pool.submit(() -> generate(home, tools, table, main.resolve(table.path()))));
            for (Future<String> result : results) {
                try { System.out.println(result.get()); }
                catch (ExecutionException failure) { throw new IllegalStateException(failure.getCause().getMessage(), failure.getCause()); }
            }
        } finally {
            pool.shutdownNow();
        }
        Files.createDirectories(stamp.getParent());
        Files.writeString(stamp, stampText);
    }

    private static String require(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("--" + name + "=<path> is required");
        return value;
    }

    private static String generate(Path home, Path tools, Table table, Path output) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(home.resolve("bin").resolve("java").toString()));
        command.addAll(table.options());
        command.addAll(List.of(tools.resolve("Jdk17Tables.java").toString(), table.name(), "--output", output.toString()));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String log = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) throw new IllegalStateException(table.name() + " failed:\n" + log);
        return table.name() + ": " + log.strip().replace("\n", "; ");
    }

    /** The first JDK 17 among the explicit path, the CI/Docker variables and the macOS default: its home and version text. */
    private static String[] jdk17(String explicit) throws IOException, InterruptedException {
        List<String> candidates = new ArrayList<>();
        if (!explicit.isEmpty()) candidates.add(explicit);
        for (String name : List.of("JAVA_HOME_17", "JAVA_HOME_17_X64", "JAVA_HOME_17_ARM64")) {
            String value = System.getenv(name);
            if (value != null && !value.isEmpty()) candidates.add(value);
        }
        if (System.getProperty("os.name").startsWith("Mac")) {
            try {
                Process process = new ProcessBuilder("/usr/libexec/java_home", "-v", "17").redirectError(ProcessBuilder.Redirect.DISCARD).start();
                String home = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
                if (process.waitFor() == 0 && !home.isEmpty()) candidates.add(home);
            } catch (IOException ignored) {
                // No java_home tool; the other candidates still apply.
            }
        }
        for (String candidate : candidates) {
            Path java = Path.of(candidate, "bin", "java");
            if (!Files.isRegularFile(java)) continue;
            Process process = new ProcessBuilder(java.toString(), "-version").redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            String version = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            process.waitFor();
            if (version.contains("\"17")) return new String[]{candidate, version};
        }
        throw new IllegalStateException("A JDK 17 installation is required to generate the Unicode tables: pass -Djavaw.jdk17.home=<path> "
                + "or set JAVA_HOME_17 (checked: " + String.join(", ", candidates) + ")");
    }
}
