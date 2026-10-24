package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import java.util.*;

/** Conservative admission policy for the executable numeric/reference bytecode backend. */
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
            Opcodes.CHECKCAST, Opcodes.INSTANCEOF, Opcodes.NEW, Opcodes.GETFIELD, Opcodes.PUTFIELD, Opcodes.GETSTATIC, Opcodes.PUTSTATIC, Opcodes.INVOKESTATIC, Opcodes.INVOKESPECIAL, Opcodes.INVOKEVIRTUAL, Opcodes.INVOKEINTERFACE,
            Opcodes.LCONST_0, Opcodes.LCONST_1, Opcodes.LLOAD, Opcodes.LSTORE,
            Opcodes.LADD, Opcodes.LSUB, Opcodes.LMUL, Opcodes.LDIV, Opcodes.LREM, Opcodes.LNEG,
            Opcodes.LSHL, Opcodes.LSHR, Opcodes.LUSHR, Opcodes.LAND, Opcodes.LOR, Opcodes.LXOR,
            Opcodes.LCMP, Opcodes.I2L, Opcodes.L2I, Opcodes.LRETURN,
            Opcodes.POP2, Opcodes.DUP_X1, Opcodes.DUP_X2, Opcodes.DUP2, Opcodes.DUP2_X1, Opcodes.DUP2_X2, Opcodes.SWAP,
            Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2, Opcodes.DCONST_0, Opcodes.DCONST_1,
            Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.FSTORE, Opcodes.DSTORE,
            Opcodes.FADD, Opcodes.FSUB, Opcodes.FMUL, Opcodes.FDIV, Opcodes.FNEG, Opcodes.FREM,
            Opcodes.DADD, Opcodes.DSUB, Opcodes.DMUL, Opcodes.DDIV, Opcodes.DNEG, Opcodes.DREM,
            Opcodes.FCMPL, Opcodes.FCMPG, Opcodes.DCMPL, Opcodes.DCMPG,
            Opcodes.I2F, Opcodes.I2D, Opcodes.L2F, Opcodes.L2D, Opcodes.F2I, Opcodes.F2L,
            Opcodes.F2D, Opcodes.D2I, Opcodes.D2L, Opcodes.D2F, Opcodes.FRETURN, Opcodes.DRETURN,
            Opcodes.IRETURN, Opcodes.ARETURN, Opcodes.RETURN);

    static boolean admittedOpcode(int opcode) { return OPCODES.contains(opcode); }

    static void validate(ClassNode owner, Map<String, ClassNode> linked) {
        for (var entry : linked.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().name))
                throw failure(entry.getValue(), null, null, "class identity", "link metadata by its declared internal name");
            BytecodeDeclarations.validate(entry.getValue());
            if (entry.getKey().equals("java/lang/Object"))
                throw failure(entry.getValue(), null, null, "intrinsic class redefinition",
                        "java/lang/Object is supplied by the bytecode runtime; linking a replacement is unsupported");
        }
        BytecodeHierarchy.validate(owner, linked);
        BytecodeObjectLayouts layouts = new BytecodeObjectLayouts(linked);
        BytecodeDispatch dispatch = new BytecodeDispatch(linked, owner.name);
        for (ClassNode declaration : linked.values())
            dispatch.validateOverrides(declaration);
        Map<String, Integer> offsets = layouts.offsets();
        for (FieldNode field : owner.fields) {
            if (field.value != null && !numericConstant(field.value))
                throw failure(owner, null, null, "ConstantValue field " + field.name + field.desc,
                        "only numeric constant fields are supported; use source compilation for other constants");
            if ((field.access & Opcodes.ACC_VOLATILE) != 0 || !valueType(Type.getType(field.desc)))
                throw failure(owner, null, null, "field " + field.name + field.desc,
                        "volatile fields require a concurrent runtime; fields must have a JVM value type");
        }
        BytecodeLinkage.validateDescriptors(linked);
        Set<String> exports = new HashSet<>();
        for (MethodNode method : owner.methods) {
            if (method.name.equals("<clinit>"))
                throw failure(owner, method, null, "class initializer", "use an explicit initialization entry point; automatic JVM initialization is unavailable");
            if ((method.access & (Opcodes.ACC_NATIVE | Opcodes.ACC_SYNCHRONIZED)) != 0)
                throw failure(owner, method, null, "native/synchronized method", "provide a supported source or explicit host adapter");
            if ((method.access & Opcodes.ACC_ABSTRACT) != 0) continue;
            if ((method.access & Opcodes.ACC_PUBLIC) != 0 && !exports.add(method.name))
                throw failure(owner, method, null, "overloaded public export", "use unique public entry-point names");
            if (!method.tryCatchBlocks.isEmpty())
                throw failure(owner, method, method.tryCatchBlocks.get(0).start, "exception handler table", "use source compilation for Java exception handling");
            for (AbstractInsnNode instruction : method.instructions) {
                int opcode = instruction.getOpcode();
                if (opcode < 0) continue;
                if (!OPCODES.contains(opcode)) throw failure(owner, method, instruction, opcodeName(opcode),
                        "use source compilation or replace this construct with the documented numeric/reference subset");
                if (instruction instanceof LdcInsnNode constant && !numericConstant(constant.cst))
                    throw failure(owner, method, instruction, "LDC " + constant.cst.getClass().getSimpleName(),
                            "only numeric LDC constants are supported; use source compilation for strings/classes/dynamic constants");
                if (instruction instanceof TypeInsnNode test
                        && (opcode == Opcodes.CHECKCAST || opcode == Opcodes.INSTANCEOF)) {
                    try { dispatch.typeTestIds(test.desc); }
                    catch (IllegalArgumentException error) {
                        throw failure(owner, method, instruction, "type test " + test.desc, error.getMessage());
                    }
                }
                if (instruction instanceof TypeInsnNode allocation && opcode == Opcodes.NEW) {
                    try { dispatch.checkClassAccess(allocation.desc); }
                    catch (IllegalArgumentException error) {
                        throw failure(owner, method, instruction, "object allocation " + allocation.desc, error.getMessage());
                    }
                    ClassNode target = linked.get(allocation.desc);
                    if (target == null || (target.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0
                            || !layouts.contains(allocation.desc))
                        throw failure(owner, method, instruction, "object allocation " + allocation.desc,
                                "link a concrete class and its complete superclass chain through Object; cyclic or missing hierarchies are unsupported");
                }
                BytecodeLinkage.validateReference(owner, method, instruction);
                if (instruction instanceof FieldInsnNode field) {
                    try {
                        dispatch.validateField(field, method);
                        if ((opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD)
                                && !offsets.containsKey(TypeHelper.getGlobalFieldName(field.owner, field.name, field.desc)))
                            throw new IllegalArgumentException("link a supported instance layout");
                    } catch (IllegalArgumentException error) {
                        throw failure(owner, method, instruction, "field access " + field.owner + "." + field.name,
                                error.getMessage());
                    }
                }
                if (instruction instanceof MethodInsnNode call) {
                    if (opcode == Opcodes.INVOKESTATIC) {
                        try { dispatch.staticTarget(call); }
                        catch (IllegalArgumentException error) {
                            throw failure(owner, method, instruction, "static call " + call.owner + "." + call.name + call.desc,
                                    error.getMessage());
                        }
                        continue;
                    }
                    if (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE) {
                        try { dispatch.targets(call); }
                        catch (IllegalArgumentException error) {
                            throw failure(owner, method, instruction, "dynamic call " + call.owner + "." + call.name + call.desc,
                                    error.getMessage());
                        }
                        continue;
                    }
                    if (opcode == Opcodes.INVOKESPECIAL && call.itf) {
                        try { dispatch.interfaceSuperTarget(call); }
                        catch (IllegalArgumentException error) {
                            throw failure(owner, method, instruction, "interface super call " + call.owner + "." + call.name + call.desc,
                                    error.getMessage());
                        }
                        continue;
                    }
                    if (BytecodeDispatch.isObjectConstructor(call)) continue;
                    try { dispatch.specialTarget(call); }
                    catch (IllegalArgumentException error) {
                        throw failure(owner, method, instruction, "special call " + call.owner + "." + call.name + call.desc,
                                error.getMessage());
                    }
                }
            }
        }
    }

    private static boolean valueType(Type type) {
        return type.getSort() >= Type.BOOLEAN && type.getSort() <= Type.OBJECT;
    }

    private static boolean numericConstant(Object value) {
        return value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double;
    }

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
