package dastec.one.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

/** Structural and linked-type validation for exception tables, independent of runtime support. */
final class BytecodeExceptionHandlers {
    record Handler(int start, int end, int target, Type type, TryCatchBlockNode declaration) {}

    static List<Handler> validate(ClassNode owner, MethodNode method, Map<String, ClassNode> linked) {
        if (method.tryCatchBlocks.isEmpty()) return List.of();
        List<Handler> handlers = new ArrayList<>();
        AbstractInsnNode[] code = method.instructions.toArray();
        Map<AbstractInsnNode, Integer> indices = new IdentityHashMap<>();
        for (int i = 0; i < code.length; i++) indices.put(code[i], i);
        for (TryCatchBlockNode table : method.tryCatchBlocks) {
            int start = offset(owner, method, table, table.start, code, indices);
            int end = offset(owner, method, table, table.end, code, indices);
            int target = offset(owner, method, table, table.handler, code, indices);
            if (start >= end || start >= code.length || target >= code.length)
                throw failure(owner, method, table, "use a nonempty forward protected range and an executable handler target; only end may identify code end");
            String name = table.type == null ? "java/lang/Throwable" : table.type;
            if (!BytecodeDeclarations.validClassName(name))
                throw failure(owner, method, table, "use a class catch type, or a catch-all entry");
            Set<String> seen = new HashSet<>();
            String current = name;
            while (true) {
                ClassNode type = linked.get(current);
                if (type == null || !seen.add(current) || (type.access & Opcodes.ACC_INTERFACE) != 0)
                    throw failure(owner, method, table, "link a complete acyclic class hierarchy for catch type " + name + " through java/lang/Throwable; host JDK types are not definitions");
                if (current.equals("java/lang/Throwable")) break;
                current = type.superName;
                if (current == null || current.equals("java/lang/Object"))
                    throw failure(owner, method, table, "catch type " + name + " must be a subclass of java/lang/Throwable");
            }
            handlers.add(new Handler(start, end, target, Type.getObjectType(name), table));
        }
        return handlers;
    }

    private static int offset(ClassNode owner, MethodNode method, TryCatchBlockNode table, LabelNode label,
                              AbstractInsnNode[] code, Map<AbstractInsnNode, Integer> indices) {
        Integer index = indices.get(label);
        if (index == null) throw failure(owner, method, table, "exception-table labels must belong to the current method");
        while (index < code.length && code[index].getOpcode() < 0) index++;
        return index;
    }

    private static IllegalArgumentException failure(ClassNode owner, MethodNode method, TryCatchBlockNode table, String reason) {
        return BytecodeCompatibility.failure(owner, method, table.start, "exception handler table", reason);
    }
}
