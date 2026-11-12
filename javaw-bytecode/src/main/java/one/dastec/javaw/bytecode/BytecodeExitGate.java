package one.dastec.javaw.bytecode;

import one.dastec.javaw.Wat2Wasm;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phase 7 exit-gate measurement (Todo.md): compiles a whole library JAR with the bundled runtime and records what it
 * costs. Admission stops at the first diagnostic, so the gate attributes each diagnostic to a JAR class, excludes that
 * class and every JAR class that references it (admission validates every method, so they cannot link without it),
 * and compiles again until the rest compiles. It then measures the admitted classes: compile time, peak heap, WAT and
 * {@code .wasm} size and function count, against a budget. Every public method of an admitted class is exported,
 * so the module is the worst case for that set.
 *
 * <pre>java ... BytecodeExitGate &lt;jar&gt; &lt;output-dir&gt; [budget: seconds:heap-MB:wat-MB:wasm-MB]</pre>
 *
 * Writes {@code report.md} and {@code blockers.tsv} to the output directory, and exits with status 1 when a measurement
 * exceeds the budget or a diagnostic cannot be attributed.
 */
public final class BytecodeExitGate {
    /** A class excluded because of {@code diagnostic}; {@code cause} is null for a root blocker. */
    record Exclusion(String className, String diagnostic, String cause) {}

    record Budget(double seconds, long heapMegabytes, long watMegabytes, long wasmMegabytes) {
        static Budget parse(String text) {
            String[] parts = text.split(":");
            return new Budget(Double.parseDouble(parts[0]), Long.parseLong(parts[1]), Long.parseLong(parts[2]), Long.parseLong(parts[3]));
        }
        @Override public String toString() { return seconds + " s, " + heapMegabytes + " MB heap, " + watMegabytes + " MB WAT, " + wasmMegabytes + " MB .wasm"; }
    }

    private static final Pattern INTERNAL_NAME = Pattern.compile("[A-Za-z_$][\\w$]*(?:/[\\w$]+)+");

    private BytecodeExitGate() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: BytecodeExitGate <jar> <output-dir> [seconds:heap-MB:wat-MB:wasm-MB]");
            System.exit(2);
        }
        Path jar = Path.of(args[0]), output = Path.of(args[1]);
        Budget budget = args.length > 2 ? Budget.parse(args[2]) : null;
        System.exit(run(jar, output, budget) ? 0 : 1);
    }

    static boolean run(Path jarPath, Path output, Budget budget) throws Exception {
        Files.createDirectories(output);
        byte[] jar = Files.readAllBytes(jarPath);
        List<ClassNode> all = BytecodeCompiler.readJar(new ByteArrayInputStream(jar));
        Map<String, ClassNode> byName = new TreeMap<>();
        for (ClassNode type : all) byName.putIfAbsent(type.name, type);
        Set<String> runtime = new HashSet<>(BytecodeExceptionRuntime.definitions().keySet());
        Map<String, Set<String>> referrers = referrers(byName);

        Map<String, Exclusion> excluded = new LinkedHashMap<>();
        List<String> log = new ArrayList<>();
        String wat = null, unattributed = null;
        double coldSeconds = 0;
        int measuredAttempt = 0;
        for (int attempt = 1; ; attempt++) {
            List<ClassNode> admitted = new ArrayList<>();
            for (ClassNode type : BytecodeCompiler.readJar(new ByteArrayInputStream(jar))) if (!excluded.containsKey(type.name)) admitted.add(type);
            int count = admitted.size(); // the compiler appends the runtime and generated classes to the list it is given
            long started = System.nanoTime();
            try {
                wat = BytecodeCompiler.compileClasses(admitted, true, List.of(), Set.of(), BytecodeCompiler.Memory.managed());
                coldSeconds = (System.nanoTime() - started) / 1e9;
                measuredAttempt = attempt;
                log.add(String.format(Locale.ROOT, "attempt %d: %d JAR classes compiled in %.2f s", attempt, count, coldSeconds));
                break;
            } catch (IllegalArgumentException failure) {
                String message = failure.getMessage();
                Set<String> roots = attribute(message, byName, runtime);
                roots.removeAll(excluded.keySet());
                log.add(String.format(Locale.ROOT, "attempt %d: %d JAR classes, %.2f s, excluded %s: %s", attempt, count,
                        (System.nanoTime() - started) / 1e9, roots, message));
                if (roots.isEmpty()) { unattributed = message; break; }
                for (String root : roots) exclude(root, message, null, referrers, excluded);
            }
            if (excluded.size() == byName.size()) break;
        }

        StringBuilder report = new StringBuilder("# Exit gate: ").append(jarPath.getFileName()).append("\n\n");
        report.append("JAR classes (after Multi-Release selection for Java ").append(BytecodeCompiler.RELEASE).append("): ").append(byName.size())
                .append(". Root blockers: ").append(excluded.values().stream().filter(e -> e.cause() == null).count())
                .append(". Excluded dependents: ").append(excluded.values().stream().filter(e -> e.cause() != null).count())
                .append(". Admitted: ").append(byName.size() - excluded.size()).append(".\n\n");
        boolean passed = true;
        if (unattributed != null) {
            report.append("Stopped: a diagnostic could not be attributed to a JAR class:\n\n    ").append(unattributed).append("\n\n");
            passed = false;
        } else if (wat == null) {
            // A budget is never met without a measurement.
            report.append("Nothing was measured: every JAR class was excluded.\n\n");
            passed = false;
        } else {
            // Keep only the cold WAT's file and hash, so the warm runs' heap peak does not include it.
            Path watFile = output.resolve("module.wat"), wasmFile = output.resolve("module.wasm");
            Files.writeString(watFile, wat, StandardCharsets.UTF_8);
            String coldHash = sha256(wat);
            long functions = wat.lines().filter(line -> line.startsWith("    (func ") || line.startsWith("(func ")).count();
            wat = null;
            // Warm timings: the same program again, with fresh class nodes and peak heap measured from a reset.
            List<Double> warm = new ArrayList<>();
            long peak = 0;
            for (int run = 0; run < 3; run++) {
                List<ClassNode> admitted = new ArrayList<>();
                for (ClassNode type : BytecodeCompiler.readJar(new ByteArrayInputStream(jar))) if (!excluded.containsKey(type.name)) admitted.add(type);
                System.gc();
                for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) pool.resetPeakUsage();
                long started = System.nanoTime();
                String again = BytecodeCompiler.compileClasses(admitted, true, List.of(), Set.of(), BytecodeCompiler.Memory.managed());
                warm.add((System.nanoTime() - started) / 1e9);
                if (!sha256(again).equals(coldHash)) throw new IllegalStateException("the same classes compiled to different WAT");
                again = null;
                long used = 0;
                for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans())
                    if (pool.getType() == MemoryType.HEAP) used += pool.getPeakUsage().getUsed();
                peak = Math.max(peak, used);
            }
            Collections.sort(warm);
            long assembleStarted = System.nanoTime();
            Process process = new ProcessBuilder(Wat2Wasm.command(watFile, wasmFile, Wat2Wasm.EXCEPTIONS)).redirectErrorStream(true).start();
            String assembler = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int status = process.waitFor();
            double assembleSeconds = (System.nanoTime() - assembleStarted) / 1e9;
            long watBytes = Files.size(watFile), wasmBytes = status == 0 ? Files.size(wasmFile) : -1;
            double megabyte = 1024 * 1024;
            report.append("| Measurement | Value | Budget |\n| --- | --- | --- |\n");
            report.append(String.format(Locale.ROOT, "| Compile, first full compile in this JVM (attempt %d) | %.2f s | |%n", measuredAttempt, coldSeconds));
            report.append(String.format(Locale.ROOT, "| Compile, warm median of 3 | %.2f s | %s |%n", warm.get(1), budget == null ? "" : budget.seconds() + " s"));
            // The pool peaks are reached at different times, so their sum is informational and can exceed the limit. The
            // budget instead bounds the heap limit (-Xmx) the whole run completed within.
            double limit = Runtime.getRuntime().maxMemory() / megabyte;
            report.append(String.format(Locale.ROOT, "| Heap limit the run completed within (-Xmx) | %.0f MB | %s |%n", limit, budget == null ? "" : budget.heapMegabytes() + " MB"));
            report.append(String.format(Locale.ROOT, "| Heap-pool peaks during a warm compile, summed (informational) | %.0f MB | |%n", peak / megabyte));
            report.append(String.format(Locale.ROOT, "| WAT | %.1f MB | %s |%n", watBytes / megabyte, budget == null ? "" : budget.watMegabytes() + " MB"));
            report.append(String.format(Locale.ROOT, "| .wasm (wat2wasm %s, %.2f s) | %s | %s |%n", Wat2Wasm.version().trim(), assembleSeconds,
                    wasmBytes < 0 ? "failed" : String.format(Locale.ROOT, "%.2f MB", wasmBytes / megabyte), budget == null ? "" : budget.wasmMegabytes() + " MB"));
            report.append("| Functions | ").append(functions).append(" | |\n\n");
            if (status != 0) { report.append("wat2wasm failed:\n\n```\n").append(assembler).append("\n```\n\n"); passed = false; }
            if (budget != null && (warm.get(1) > budget.seconds() || Runtime.getRuntime().maxMemory() / megabyte > budget.heapMegabytes()
                    || watBytes / megabyte > budget.watMegabytes() || wasmBytes / megabyte > budget.wasmMegabytes())) {
                report.append("**Over budget** (").append(budget).append(").\n\n");
                passed = false;
            }
        }
        report.append("## Root blockers\n\n| Class | Diagnostic | Dependents |\n| --- | --- | ---: |\n");
        StringBuilder tsv = new StringBuilder("class\tcause\tdiagnostic\n");
        for (Exclusion exclusion : excluded.values()) {
            tsv.append(exclusion.className()).append('\t').append(exclusion.cause() == null ? "" : exclusion.cause()).append('\t')
                    .append(exclusion.diagnostic().replace('\t', ' ').replace('\n', ' ')).append('\n');
            if (exclusion.cause() != null) continue;
            long dependents = excluded.values().stream().filter(e -> exclusion.className().equals(e.cause())).count();
            report.append("| `").append(exclusion.className()).append("` | ").append(exclusion.diagnostic().replace("|", "\\|")).append(" | ").append(dependents).append(" |\n");
        }
        report.append("\n## Attempts\n\n");
        for (String line : log) report.append("- ").append(line.replace("|", "\\|")).append('\n');
        Files.writeString(output.resolve("report.md"), report.toString(), StandardCharsets.UTF_8);
        Files.writeString(output.resolve("blockers.tsv"), tsv.toString(), StandardCharsets.UTF_8);
        System.out.println(report);
        return passed;
    }

    private static String sha256(String text) throws java.security.NoSuchAlgorithmException {
        return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    /** Excludes {@code name} and, transitively, every JAR class that references it. */
    private static void exclude(String name, String diagnostic, String cause, Map<String, Set<String>> referrers, Map<String, Exclusion> excluded) {
        Deque<String[]> pending = new ArrayDeque<>();
        pending.add(new String[]{name, cause});
        while (!pending.isEmpty()) {
            String[] next = pending.removeFirst();
            if (excluded.containsKey(next[0])) continue;
            excluded.put(next[0], new Exclusion(next[0], diagnostic, next[1]));
            for (String referrer : referrers.getOrDefault(next[0], Set.of())) pending.add(new String[]{referrer, next[1] == null ? next[0] : next[1]});
        }
    }

    /**
     * The JAR classes a diagnostic blames. The diagnostic names its owner (a JAR class, or a generated or runtime class).
     * For a generated or runtime owner, the internal names in the message are used: a JAR class named directly, or the
     * JAR classes whose supertypes include a named type that is neither in the JAR nor in the runtime.
     */
    static Set<String> attribute(String message, Map<String, ClassNode> jar, Set<String> runtime) {
        Set<String> result = new TreeSet<>();
        int first = message.indexOf(": "), unsupported = message.indexOf(": unsupported ");
        if (first >= 0 && unsupported > first) {
            String owner = message.substring(first + 2, unsupported);
            int open = owner.indexOf('(');
            if (open >= 0) owner = owner.substring(0, owner.lastIndexOf('.', open));
            owner = owner.replace('.', '/');
            // Normalizers generate companions such as Owner$$javaw$reflection; blame the class they were generated for.
            int generated = owner.indexOf("$$javaw$");
            if (generated > 0) owner = owner.substring(0, generated);
            if (jar.containsKey(owner)) { result.add(owner); return result; }
        }
        Matcher names = INTERNAL_NAME.matcher(message);
        Set<String> missing = new LinkedHashSet<>();
        while (names.find()) {
            String name = names.group();
            // Names inside descriptors match with their 'L' (and any '[' before it): Llib/Free; is lib/Free.
            if (!jar.containsKey(name) && name.startsWith("L") && names.end() < message.length() && message.charAt(names.end()) == ';')
                name = name.substring(1);
            if (jar.containsKey(name)) result.add(name);
            // java/lang/Object is supplied by the compiler itself, not by a runtime class.
            else if (!runtime.contains(name) && !name.startsWith("javaw/") && !name.equals("java/lang/Object")) missing.add(name);
        }
        if (!result.isEmpty()) return result;
        for (ClassNode type : jar.values())
            for (String supertype : supertypes(type)) if (missing.contains(supertype)) result.add(type.name);
        if (!result.isEmpty()) return result;
        // A whole-program hierarchy check that names no type: blame the classes with a supertype that nothing links.
        if (message.contains("hierarchy") || message.contains("superclass chain"))
            for (ClassNode type : jar.values())
                for (String supertype : supertypes(type))
                    if (!jar.containsKey(supertype) && !runtime.contains(supertype) && !supertype.equals("java/lang/Object")) result.add(type.name);
        return result;
    }

    private static List<String> supertypes(ClassNode type) {
        List<String> supertypes = new ArrayList<>(type.interfaces);
        if (type.superName != null) supertypes.add(type.superName);
        return supertypes;
    }

    /** For each JAR class, the JAR classes whose class files name it anywhere. */
    private static Map<String, Set<String>> referrers(Map<String, ClassNode> jar) {
        Map<String, Set<String>> result = new HashMap<>();
        for (ClassNode type : jar.values())
            for (String name : references(type))
                if (!name.equals(type.name) && jar.containsKey(name)) result.computeIfAbsent(name, ignored -> new TreeSet<>()).add(type.name);
        return result;
    }

    private static Set<String> references(ClassNode type) {
        Set<String> names = new HashSet<>();
        if (type.superName != null) names.add(type.superName);
        names.addAll(type.interfaces);
        if (type.outerClass != null) names.add(type.outerClass);
        if (type.nestHostClass != null) names.add(type.nestHostClass);
        for (FieldNode field : type.fields) descriptor(field.desc, names);
        for (MethodNode method : type.methods) {
            descriptor(method.desc, names);
            if (method.exceptions != null) names.addAll(method.exceptions);
            for (TryCatchBlockNode handler : method.tryCatchBlocks) if (handler.type != null) names.add(handler.type);
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof TypeInsnNode t) internal(t.desc, names);
                else if (instruction instanceof FieldInsnNode f) { internal(f.owner, names); descriptor(f.desc, names); }
                else if (instruction instanceof MethodInsnNode m) { internal(m.owner, names); descriptor(m.desc, names); }
                else if (instruction instanceof MultiANewArrayInsnNode m) descriptor(m.desc, names);
                else if (instruction instanceof LdcInsnNode l) constant(l.cst, names);
                else if (instruction instanceof InvokeDynamicInsnNode d) {
                    descriptor(d.desc, names);
                    constant(d.bsm, names);
                    for (Object argument : d.bsmArgs) constant(argument, names);
                }
            }
        }
        return names;
    }

    private static void constant(Object value, Set<String> names) {
        if (value instanceof Type t) descriptor(t.getDescriptor(), names);
        else if (value instanceof Handle h) { internal(h.getOwner(), names); descriptor(h.getDesc(), names); }
    }

    private static void internal(String name, Set<String> names) {
        if (name.startsWith("[")) descriptor(name, names); else names.add(name);
    }

    private static void descriptor(String descriptor, Set<String> names) {
        try {
            List<Type> types = new ArrayList<>();
            if (descriptor.startsWith("(")) { types.addAll(List.of(Type.getArgumentTypes(descriptor))); types.add(Type.getReturnType(descriptor)); }
            else types.add(Type.getType(descriptor));
            for (Type type : types) {
                Type element = type.getSort() == Type.ARRAY ? type.getElementType() : type;
                if (element.getSort() == Type.OBJECT) names.add(element.getInternalName());
            }
        } catch (RuntimeException malformed) {
            // Admission reports malformed descriptors; they add no references here.
        }
    }
}
