package one.dastec.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.*;
import java.util.function.Predicate;

/**
 * Instantiation-aware reachability (rapid type analysis) for bundled-runtime builds. Methods are lowered only once
 * they are reachable, and virtual/interface dispatch is lowered only for receiver classes that the module can
 * instantiate. Admission has already run for every linked class and method, so nothing here affects diagnostics.
 *
 * <p>Nodes are WAT function ids. Edges come from the lowered or generated text itself, so direct calls are exact.
 * Objects come into existence only through a reachable {@code NEW}, or through runtime helpers that tag fresh memory
 * themselves ({@link #helperInstance}). Hosts may pass in only references that the module created. When a newly
 * instantiated class widens a call site that was already lowered, the method containing it is lowered again.
 */
final class BytecodeReachability {
    private record Method(ClassBytecodeCompiler compiler, MethodNode method) {}
    /**
     * Class sections (declarations plus reachable methods, in class and method order), lowered methods, instantiated
     * classes, every function id the analysis reached, and the fields of the runtime text, spanned in {@code "(module\n" + runtime}.
     */
    record Result(String classes, Set<MethodNode> lowered, Set<String> instantiated, Set<String> reached,
                  List<BytecodeDeadCode.Item> runtimeFields) {}

    private final Map<String, Method> methods = new HashMap<>();
    private final Map<String, String> texts = new HashMap<>();
    private final Map<String, String> lowered = new HashMap<>();
    private final Deque<String> pending = new ArrayDeque<>();
    private final Set<String> reached = new HashSet<>();
    private final Set<String> instantiated = new HashSet<>();
    /** Uninstantiated receiver class -> methods lowered without a dispatch arm for it. */
    private final Map<String, Set<String>> waiting = new HashMap<>();
    private final Set<String> stale = new LinkedHashSet<>();
    private String current;

    private BytecodeReachability() {}

    /**
     * Lowers the reachable methods of admitted compilers. {@code runtime} is the generated runtime module text; its exports, non-function fields and
     * functions take part in the analysis exactly as in {@link BytecodeDeadCode#prune}.
     */
    static Result lower(List<ClassBytecodeCompiler> compilers, String runtime, Predicate<String> rootExport, Set<String> retained) {
        return new BytecodeReachability().run(compilers, runtime, rootExport, retained);
    }

    private Result run(List<ClassBytecodeCompiler> compilers, String runtime, Predicate<String> rootExport, Set<String> retained) {
        List<String> declarations = new ArrayList<>();
        Predicate<String> receivers = this::receiver;
        List<BytecodeDeadCode.Item> runtimeFields = addFields(runtime, rootExport);
        for (ClassBytecodeCompiler compiler : compilers) {
            String text = compiler.declarationsWat();
            declarations.add(text);
            addFields(text, rootExport);
            compiler.restrictReceivers(receivers);
            for (MethodNode method : compiler.loweredMethods()) {
                String id = TypeHelper.getMethodFunctionName(compiler.classNode().name, method.name, method.desc);
                methods.put(id, new Method(compiler, method));
                String export = compiler.exportName(method);
                if (export != null && rootExport.test(export)) pending.add(id);
            }
        }
        pending.addAll(retained);
        while (true) {
            while (!pending.isEmpty()) {
                String id = pending.pop();
                if (!reached.add(id)) continue;
                instantiate(helperInstance(id));
                if (methods.containsKey(id)) lower(id);
                else if (texts.containsKey(id)) BytecodeDeadCode.references(texts.get(id), pending);
            }
            if (stale.isEmpty()) break;
            List<String> widened = new ArrayList<>(stale);
            stale.clear();
            for (String id : widened) lower(id);
        }
        StringBuilder out = new StringBuilder();
        Set<MethodNode> bodies = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < compilers.size(); i++) {
            ClassBytecodeCompiler compiler = compilers.get(i);
            out.append(declarations.get(i));
            for (MethodNode method : compiler.loweredMethods()) {
                String text = lowered.get(TypeHelper.getMethodFunctionName(compiler.classNode().name, method.name, method.desc));
                if (text == null) continue;
                out.append(text);
                bodies.add(method);
            }
        }
        return new Result(out.toString(), bodies, Collections.unmodifiableSet(instantiated), Collections.unmodifiableSet(reached), runtimeFields);
    }

    private List<BytecodeDeadCode.Item> addFields(String text, Predicate<String> rootExport) {
        List<BytecodeDeadCode.Item> items = BytecodeDeadCode.items(text);
        if (items == null) throw new IllegalStateException("generated runtime text is not plain WAT");
        for (BytecodeDeadCode.Item item : items) {
            if (item.function() != null) texts.put(item.function(), item.text());
            if (item.export() != null) {
                if (rootExport.test(item.export())) pending.add(item.exportTarget());
            } else if (item.function() == null) BytecodeDeadCode.references(item.text(), pending);
        }
        return items;
    }

    private void lower(String id) {
        Method method = methods.get(id);
        current = id;
        String text = method.compiler().methodWat(method.method());
        lowered.put(id, text);
        BytecodeDeadCode.references(text, pending);
        for (AbstractInsnNode instruction : method.method().instructions)
            if (instruction.getOpcode() == Opcodes.NEW) instantiate(((TypeInsnNode) instruction).desc);
    }

    private boolean receiver(String name) {
        if (instantiated.contains(name)) return true;
        waiting.computeIfAbsent(name, ignored -> new HashSet<>()).add(current);
        return false;
    }

    private void instantiate(String name) {
        if (name == null || !instantiated.add(name)) return;
        Set<String> narrowed = waiting.remove(name);
        if (narrowed != null) stale.addAll(narrowed);
    }

    /** Runtime helpers that allocate and tag an object without a {@code NEW} instruction. */
    private static String helperInstance(String id) {
        if (id.startsWith("$__text")) return "java/lang/String";
        if (id.startsWith("$__mirror")) return "java/lang/Class";
        if (id.equals("$__trace_capture")) return "java/lang/StackTraceElement";
        // Includes the OutOfMemoryError/StackOverflowError data-segment objects, which escape only when thrown.
        for (BytecodeExceptions.Implicit kind : BytecodeExceptions.Implicit.values())
            if (id.equals(kind.helper)) return kind.className;
        return null;
    }
}
