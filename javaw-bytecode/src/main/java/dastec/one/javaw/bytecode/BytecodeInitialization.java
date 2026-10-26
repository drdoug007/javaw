package dastec.one.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Module-local, single-threaded JVMS 5.5 initialization; no Wasm start function. */
final class BytecodeInitialization {
    private BytecodeInitialization() {}

    static String constantValue(org.objectweb.asm.tree.FieldNode field) {
        Object value = field.value;
        if (value instanceof Integer integer) value = switch (field.desc) {
            case "Z" -> integer & 1;
            case "B" -> (int) (byte) integer.intValue();
            case "C" -> (int) (char) integer.intValue();
            case "S" -> (int) (short) integer.intValue();
            default -> integer;
        };
        return TypeHelper.watNumber(value instanceof String ? null : value);
    }

    static String symbol(String owner) {
        return TypeHelper.getMethodFunctionName(owner, "<initialize>", "()V");
    }

    static String runtimeWat(ClassNode type, Map<String, ClassNode> linked) {
        if (!BytecodeExceptionRuntime.enabled(linked)) return "";
        String name = symbol(type.name), state = name + "_state", failure = name + "_failure";
        StringBuilder out = new StringBuilder();
        out.append("(global ").append(state).append(" (mut i32) (i32.const 0))\n")
                .append("(global ").append(failure).append(" (mut i32) (i32.const 0))\n")
                .append("(func ").append(name).append(" (local $failure i32)\n")
                .append("global.get ").append(state).append("\ni32.const 3\ni32.eq\nif\n")
                .append("global.get ").append(failure).append("\ncall ")
                .append(support("initializationFailed")).append("\nthrow $__java_exception\nend\n")
                // In a single thread, initializing means recursive active use by this thread.
                .append("global.get ").append(state).append("\nif\nreturn\nend\n")
                .append("i32.const 1\nglobal.set ").append(state).append("\ntry\n");
        for (var field : type.fields)
            if ((field.access & Opcodes.ACC_STATIC) != 0 && field.value instanceof String text)
                out.append("call ").append(BytecodeStrings.symbol(text)).append("\nglobal.set ")
                        .append(TypeHelper.getGlobalFieldName(type.name, field.name, field.desc)).append('\n');
        if ((type.access & Opcodes.ACC_INTERFACE) == 0) {
            if (linked.containsKey(type.superName)) out.append("call ").append(symbol(type.superName)).append('\n');
            Set<String> interfaces = new LinkedHashSet<>();
            Set<String> visited = new HashSet<>();
            for (String iface : type.interfaces) defaultInterfaces(iface, linked, interfaces, visited);
            for (String iface : interfaces) out.append("call ").append(symbol(iface)).append('\n');
        }
        if (type.methods.stream().anyMatch(method -> method.name.equals("<clinit>")))
            out.append("call ").append(TypeHelper.getMethodFunctionName(type.name, "<clinit>", "()V")).append('\n');
        out.append("i32.const 2\nglobal.set ").append(state).append("\ncatch $__java_exception\n")
                .append("local.set $failure\ni32.const 3\nglobal.set ").append(state).append('\n')
                // Retain the original even if wrapping itself fails (e.g. allocation failure).
                .append("local.get $failure\nglobal.set ").append(failure).append('\n')
                .append("local.get $failure\ncall ").append(support("initializationException")).append('\n')
                .append("local.tee $failure\nglobal.set ").append(failure).append('\n')
                .append("local.get $failure\nthrow $__java_exception\ncatch_all\n")
                .append("i32.const 3\nglobal.set ").append(state).append("\nrethrow 0\nend\n)\n");
        return out.toString();
    }

    private static String support(String name) {
        return TypeHelper.getMethodFunctionName("javaw/runtime/ExceptionSupport", name,
                "(Ljava/lang/Throwable;)Ljava/lang/Throwable;");
    }

    /** Postorder, declaration-order traversal; only interfaces declaring defaults initialize. */
    private static void defaultInterfaces(String name, Map<String, ClassNode> linked, Set<String> result, Set<String> visited) {
        if (!visited.add(name)) return;
        ClassNode type = linked.get(name);
        if (type == null) return;
        for (String parent : type.interfaces) defaultInterfaces(parent, linked, result, visited);
        if (type.methods.stream().anyMatch(method -> !method.name.startsWith("<")
                && (method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_ABSTRACT)) == 0))
            result.add(name);
    }
}
