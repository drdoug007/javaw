package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeVerifierTest {
    private ClassNode type(String name, String parent) {
        ClassNode node = new ClassNode(); node.name = name; node.superName = parent; node.access = Opcodes.ACC_PUBLIC;
        return node;
    }

    @Test void rejectsInvalidOperandsLocalsReceiversArgumentsReturnsAndReferenceMerges() {
        for (String scenario : new String[]{"getReceiver", "putReceiver", "referenceField", "callArgument",
                "referenceReturn", "primitiveReturn", "primitiveLocal", "uninitializedLocal", "merge"}) {
            ClassNode base = type("Base", "java/lang/Object"), other = type("Other", "java/lang/Object");
            base.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "number", "I", null, null));
            base.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "reference", "LBase;", null, null));
            MethodNode helper = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "helper", "(LBase;)I", null, null);
            helper.instructions.add(new InsnNode(Opcodes.ICONST_1)); helper.instructions.add(new InsnNode(Opcodes.IRETURN));
            helper.maxLocals = 1; helper.maxStack = 1; base.methods.add(helper);
            ClassNode caller = type("Caller", "java/lang/Object"); caller.sourceFile = "Caller.java";
            String desc = switch (scenario) {
                case "referenceField" -> "(LBase;LOther;)V";
                case "referenceReturn" -> "(LOther;)LBase;";
                case "primitiveReturn" -> "()J";
                case "primitiveLocal" -> "(F)I";
                case "uninitializedLocal" -> "(LBase;)LBase;";
                case "merge" -> "(ZLBase;LOther;)LBase;";
                default -> "(LOther;)I";
            };
            MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", desc, null, null);
            LabelNode line = new LabelNode(); method.instructions.add(line); method.instructions.add(new LineNumberNode(9, line));
            var insns = method.instructions;
            switch (scenario) {
                case "getReceiver" -> { insns.add(new VarInsnNode(Opcodes.ALOAD, 0)); insns.add(new FieldInsnNode(Opcodes.GETFIELD, "Base", "number", "I")); insns.add(new InsnNode(Opcodes.IRETURN)); }
                case "putReceiver" -> { insns.add(new VarInsnNode(Opcodes.ALOAD, 0)); insns.add(new InsnNode(Opcodes.ICONST_1)); insns.add(new FieldInsnNode(Opcodes.PUTFIELD, "Base", "number", "I")); insns.add(new InsnNode(Opcodes.ICONST_1)); insns.add(new InsnNode(Opcodes.IRETURN)); }
                case "referenceField" -> { insns.add(new VarInsnNode(Opcodes.ALOAD, 0)); insns.add(new VarInsnNode(Opcodes.ALOAD, 1)); insns.add(new FieldInsnNode(Opcodes.PUTFIELD, "Base", "reference", "LBase;")); insns.add(new InsnNode(Opcodes.RETURN)); }
                case "callArgument" -> { insns.add(new VarInsnNode(Opcodes.ALOAD, 0)); insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Base", "helper", "(LBase;)I", false)); insns.add(new InsnNode(Opcodes.IRETURN)); }
                case "referenceReturn" -> { insns.add(new VarInsnNode(Opcodes.ALOAD, 0)); insns.add(new InsnNode(Opcodes.ARETURN)); }
                case "primitiveReturn" -> { insns.add(new InsnNode(Opcodes.ICONST_1)); insns.add(new InsnNode(Opcodes.IRETURN)); }
                case "primitiveLocal" -> { insns.add(new VarInsnNode(Opcodes.ILOAD, 0)); insns.add(new InsnNode(Opcodes.IRETURN)); }
                case "uninitializedLocal" -> { insns.add(new VarInsnNode(Opcodes.ALOAD, 1)); insns.add(new InsnNode(Opcodes.ARETURN)); }
                case "merge" -> {
                    LabelNode otherwise = new LabelNode(), join = new LabelNode();
                    insns.add(new VarInsnNode(Opcodes.ILOAD, 0)); insns.add(new JumpInsnNode(Opcodes.IFEQ, otherwise));
                    insns.add(new VarInsnNode(Opcodes.ALOAD, 1)); insns.add(new JumpInsnNode(Opcodes.GOTO, join));
                    insns.add(otherwise); insns.add(new VarInsnNode(Opcodes.ALOAD, 2)); insns.add(join); insns.add(new InsnNode(Opcodes.ARETURN));
                }
            }
            method.maxLocals = 3; method.maxStack = 2; caller.methods.add(method);
            String error = assertThrows(IllegalArgumentException.class,
                    () -> new ClassBytecodeCompiler(caller).compileToWat(Map.of("Base", base, "Other", other, "Caller", caller)), scenario).getMessage();
            assertTrue(error.contains("Caller.java:9:"), error);
            assertTrue(error.contains("invalid operand stack"), scenario + ": " + error);
            assertTrue(error.contains("link required hierarchy metadata"), error);
        }
    }

    @Test void infersLinkedAndArrayRelationsWithoutUsingHostClassDefinitions() {
        ClassNode base = type("Base", "java/lang/Object"), child = type("java/lang/String", "Base");
        ClassNode sibling = type("Sibling", "Base"), iface = type("View", "java/lang/Object");
        iface.access |= Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT; child.interfaces.add(iface.name);
        BytecodeVerifier verifier = new BytecodeVerifier(base, Map.of(base.name, base, child.name, child, sibling.name, sibling, iface.name, iface));
        Type baseType = Type.getObjectType("Base"), childType = Type.getObjectType("java/lang/String");
        assertTrue(verifier.isAssignableFrom(baseType, childType)); // The host String class does not extend Base.
        assertTrue(verifier.isAssignableFrom(Type.getObjectType("View"), childType));
        assertFalse(verifier.isAssignableFrom(childType, baseType));
        assertEquals(new BasicValue(baseType), verifier.merge(new BasicValue(childType), new BasicValue(Type.getObjectType("Sibling"))));
        assertTrue(verifier.isAssignableFrom(Type.getType("[LBase;"), Type.getType("[Ljava/lang/String;")));
        assertTrue(verifier.isAssignableFrom(Type.getType("[Ljava/lang/Object;"), Type.getType("[[I")));
        assertFalse(verifier.isAssignableFrom(Type.getType("[I"), Type.getType("[J")));
        assertFalse(verifier.isAssignableFrom(Type.getType("[I"), Type.getType("[Ljava/lang/Object;")));
        assertTrue(verifier.isAssignableFrom(Type.getObjectType("java/lang/Cloneable"), Type.getType("[I")));
        assertTrue(verifier.isAssignableFrom(baseType, Type.getObjectType("null")));
        assertTrue(verifier.isAssignableFrom(Type.getObjectType("java/lang/Object"), Type.getObjectType("external/Unknown")));
        assertFalse(verifier.isAssignableFrom(baseType, Type.getObjectType("external/Unknown")));
        assertThrows(IllegalArgumentException.class, () -> verifier.getClass(childType));
    }
}
