package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import java.util.*;

/** Conservative admission policy for the executable i32 bytecode backend. */
final class BytecodeCompatibility {
    private static final Set<Integer> OPCODES = Set.of(
            Opcodes.NOP, Opcodes.ACONST_NULL, Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1,
            Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5, Opcodes.BIPUSH,
            Opcodes.SIPUSH, Opcodes.LDC, Opcodes.ILOAD, Opcodes.ALOAD, Opcodes.ISTORE, Opcodes.ASTORE,
            Opcodes.IINC, Opcodes.POP, Opcodes.DUP, Opcodes.IADD, Opcodes.ISUB, Opcodes.IMUL,
            Opcodes.IDIV, Opcodes.IREM, Opcodes.INEG, Opcodes.ISHL, Opcodes.ISHR, Opcodes.IUSHR,
            Opcodes.IAND, Opcodes.IOR, Opcodes.IXOR, Opcodes.I2B, Opcodes.I2C, Opcodes.I2S,
            Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE,
            Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE,
            Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE, Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE,
            Opcodes.IFNULL, Opcodes.IFNONNULL, Opcodes.GOTO, Opcodes.TABLESWITCH, Opcodes.LOOKUPSWITCH,
            Opcodes.NEW, Opcodes.GETFIELD, Opcodes.PUTFIELD, Opcodes.GETSTATIC, Opcodes.PUTSTATIC, Opcodes.INVOKESTATIC, Opcodes.INVOKESPECIAL,
            Opcodes.IRETURN, Opcodes.ARETURN, Opcodes.RETURN);

    static void validate(ClassNode owner, Map<String, ClassNode> linked) {
        BytecodeObjectLayouts layouts = new BytecodeObjectLayouts(linked);
        Map<String, Integer> offsets = layouts.offsets();
        for (FieldNode field : owner.fields) {
            if (field.value != null && !(field.value instanceof Integer))
                throw failure(owner, null, null, "ConstantValue field " + field.name + field.desc,
                        "only integer constant fields are supported; use source compilation for other constants");
            if ((field.access & Opcodes.ACC_VOLATILE) != 0 || !i32(Type.getType(field.desc)))
                throw failure(owner, null, null, "field " + field.name + field.desc,
                        "volatile and non-i32 fields need a typed, concurrent runtime");
        }
        Set<String> exports = new HashSet<>();
        for (MethodNode method : owner.methods) {
            if ((method.access & (Opcodes.ACC_NATIVE | Opcodes.ACC_SYNCHRONIZED)) != 0)
                throw failure(owner, method, null, "native/synchronized method", "provide a supported source or explicit host adapter");
            if ((method.access & Opcodes.ACC_ABSTRACT) != 0) continue;
            if ((method.access & Opcodes.ACC_PUBLIC) != 0 && !exports.add(method.name))
                throw failure(owner, method, null, "overloaded public export", "use unique public entry-point names");
            if (!i32OrVoid(Type.getReturnType(method.desc)) || Arrays.stream(Type.getArgumentTypes(method.desc)).anyMatch(t -> !i32(t)))
                throw failure(owner, method, null, "wide/floating-point method descriptor", "use the source backend for typed long/float/double lowering");
            if (!method.tryCatchBlocks.isEmpty())
                throw failure(owner, method, method.tryCatchBlocks.get(0).start, "exception handler table", "use source compilation for Java exception handling");
            if (method.name.equals("<clinit>"))
                throw failure(owner, method, null, "class initializer", "use an explicit initialization entry point; automatic JVM initialization is unavailable");
            for (AbstractInsnNode instruction : method.instructions) {
                int opcode = instruction.getOpcode();
                if (opcode < 0) continue;
                if (!OPCODES.contains(opcode)) throw failure(owner, method, instruction, opcodeName(opcode),
                        "use source compilation or replace this construct with the documented i32 subset");
                if (instruction instanceof LdcInsnNode constant && !(constant.cst instanceof Integer))
                    throw failure(owner, method, instruction, "LDC " + constant.cst.getClass().getSimpleName(),
                            "only integer LDC constants are supported; use source compilation for strings/classes/dynamic constants");
                if (instruction instanceof TypeInsnNode allocation && opcode == Opcodes.NEW) {
                    ClassNode target = linked.get(allocation.desc);
                    if (target == null || (target.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0
                            || !layouts.contains(allocation.desc))
                        throw failure(owner, method, instruction, "object allocation " + allocation.desc,
                                "link a concrete class and its complete superclass chain through Object; cyclic or missing hierarchies are unsupported");
                }
                if (instruction instanceof FieldInsnNode field) {
                    ClassNode target = linked.get(field.owner);
                    boolean isStatic = opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC;
                    boolean resolved = isStatic
                            ? target != null && target.fields.stream().anyMatch(f -> f.name.equals(field.name)
                                && f.desc.equals(field.desc) && (f.access & Opcodes.ACC_STATIC) != 0)
                            : offsets.containsKey(field.owner + "." + field.name + ":" + field.desc);
                    if (!resolved)
                        throw failure(owner, method, instruction, "unlinked or unsupported field " + field.owner + "." + field.name,
                                "link a supported declaring class in the same JAR/module");
                }
                if (instruction instanceof MethodInsnNode call) {
                    if (call.owner.equals("java/lang/Object") && call.name.equals("<init>") && call.desc.equals("()V")) continue;
                    ClassNode target = linked.get(call.owner);
                    if (target == null || target.methods.stream().noneMatch(m -> m.name.equals(call.name) && m.desc.equals(call.desc)
                            && (m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0
                            && ((m.access & Opcodes.ACC_STATIC) != 0) == (opcode == Opcodes.INVOKESTATIC)))
                        throw failure(owner, method, instruction, "unlinked method " + call.owner + "." + call.name + call.desc,
                                "link all supported callees or provide an explicit source/host adapter");
                }
            }
        }
    }

    private static boolean i32(Type type) { return type.getSort() != Type.VOID && type.getSort() != Type.LONG
            && type.getSort() != Type.FLOAT && type.getSort() != Type.DOUBLE; }
    private static boolean i32OrVoid(Type type) { return type.getSort() == Type.VOID || i32(type); }

    static String opcodeName(int opcode) {
        // Opcodes also contains API versions, access flags, frame tags, and handle kinds.
        for (var field : Opcodes.class.getFields()) {
            String name = field.getName();
            if (field.getType() != int.class || name.startsWith("ACC_") || name.startsWith("V")
                    || name.startsWith("ASM") || name.startsWith("T_") || name.startsWith("H_") || name.startsWith("F_")) continue;
            try { if (field.getInt(null) == opcode) return name; }
            catch (IllegalAccessException ignored) { }
        }
        return "opcode " + opcode;
    }

    static IllegalArgumentException failure(ClassNode owner, MethodNode method, AbstractInsnNode instruction, String construct, String remediation) {
        int line = -1;
        if (method != null) for (AbstractInsnNode node : method.instructions) {
            if (node instanceof LineNumberNode number) line = number.line;
            if (node == instruction) break;
            if (instruction == null && line >= 0) break;
        }
        if (line < 0 && method != null) {
            AbstractInsnNode start = instruction == null ? method.instructions.getFirst() : instruction;
            for (AbstractInsnNode node = start; node != null; node = node.getNext()) {
                if (node instanceof LineNumberNode number) { line = number.line; break; }
            }
        }
        String file = owner.sourceFile == null ? owner.name + ".class" : owner.sourceFile;
        return new IllegalArgumentException(file + (line >= 0 ? ":" + line : ":line unavailable") + ": "
                + owner.name.replace('/', '.') + (method == null ? "" : "." + method.name + method.desc)
                + ": unsupported " + construct + "; " + remediation);
    }
}
