package one.dastec.javaw;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds {@code wat2wasm} command lines that work across wabt releases.
 *
 * <p>Callers name the Wasm features they need with the historical {@code --enable-*} flags. wabt 1.0.37 and later enable
 * exceptions, multi-memory and annotations by default and reject those flags as unknown options, so a flag is passed only
 * when the installed {@code wat2wasm --help} lists it. A flag that is no longer listed refers to a feature that is now on
 * by default; a feature the installed wabt does not support at all still fails when the module is assembled.
 */
public final class Wat2Wasm {
    public static final String EXCEPTIONS = "--enable-exceptions";
    public static final String MULTI_MEMORY = "--enable-multi-memory";
    public static final String ANNOTATIONS = "--enable-annotations";
    public static final String THREADS = "--enable-threads";

    private static final String EXECUTABLE = "wat2wasm";
    private static final Pattern OPTION = Pattern.compile("--[a-z0-9][a-z0-9-]*");
    private static volatile Tool tool;

    private record Tool(Set<String> options, String version) {}

    private Wat2Wasm() {}

    /**
     * Returns {@code wat2wasm [flags] input -o output}, dropping each {@code --enable-*} flag the installed wabt no longer
     * accepts. Other flags, such as {@code --debug-names}, are passed through unchanged.
     */
    public static List<String> command(Path input, Path output, String... flags) {
        List<String> command = new ArrayList<>();
        command.add(EXECUTABLE);
        command.addAll(supportedFlags(flags));
        command.add(input.toString());
        command.add("-o");
        command.add(output.toString());
        return command;
    }

    /** The requested flags that the installed wabt accepts, in their original order. */
    public static List<String> supportedFlags(String... flags) {
        return supportedFlags(tool().options(), flags);
    }

    /** Filters {@code flags} against the options listed by a {@code wat2wasm --help}; visible for tests. */
    static List<String> supportedFlags(Set<String> options, String... flags) {
        List<String> result = new ArrayList<>();
        for (String flag : flags) {
            // An unreadable --help (wabt missing) keeps every flag, so the failure names the real problem.
            if (!flag.startsWith("--enable-") || options.isEmpty() || options.contains(flag)) result.add(flag);
        }
        return result;
    }

    /** The long options named in {@code wat2wasm --help} text; visible for tests. */
    static Set<String> options(String help) {
        Set<String> options = new TreeSet<>();
        Matcher matcher = OPTION.matcher(help);
        while (matcher.find()) options.add(matcher.group());
        return Set.copyOf(options);
    }

    /** The installed {@code wat2wasm --version} output, or an empty string when it cannot be run. */
    public static String version() {
        return tool().version();
    }

    private static Tool tool() {
        Tool current = tool;
        if (current == null) {
            synchronized (Wat2Wasm.class) {
                current = tool;
                if (current == null) tool = current = probe();
            }
        }
        return current;
    }

    private static Tool probe() {
        return new Tool(options(output(EXECUTABLE, "--help")), output(EXECUTABLE, "--version").strip());
    }

    private static String output(String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String text = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "";
            }
            return process.exitValue() == 0 ? text : "";
        } catch (IOException e) {
            return "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }
}
