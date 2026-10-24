package com.example.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

class BytecodeDeclarationsTest implements Opcodes {
    private ClassNode type() {
        ClassNode node = new ClassNode();
        node.name = "Declarations"; node.superName = "java/lang/Object";
        node.version = V17; node.access = ACC_PUBLIC;
        return node;
    }

    private void rejects(Consumer<ClassNode> change, String reason) {
        ClassNode node = type(); change.accept(node);
        String message = assertThrows(IllegalArgumentException.class,
                () -> new ClassBytecodeCompiler(node).compileToWat()).getMessage();
        assertTrue(message.contains(reason), message);
    }

    @Test void rejectsDuplicatesEvenWhenStaticAndInstanceKindsDiffer() {
        rejects(n -> {
            n.fields.add(new FieldNode(0, "value", "I", null, null));
            n.fields.add(new FieldNode(ACC_STATIC, "value", "I", null, null));
        }, "duplicate field");
        rejects(n -> {
            n.methods.add(new MethodNode(ACC_PUBLIC | ACC_ABSTRACT, "get", "()I", null, null));
            n.methods.add(new MethodNode(ACC_PUBLIC | ACC_STATIC, "get", "()I", null, null));
        }, "duplicate method");
    }

    @Test void rejectsMalformedDescriptorsAndContradictoryFlagsBeforeLowering() {
        for (String desc : new String[]{"V", "[V", "II", "L;", "Ljava.lang.String;", "[", "L/a;"})
            rejects(n -> n.fields.add(new FieldNode(0, "bad", desc, null, null)), "invalid descriptor");
        for (String desc : new String[]{"(V)V", "()", "()VI", "(I", "I", "(Ljava//lang/String;)V"})
            rejects(n -> n.methods.add(new MethodNode(0, "bad", desc, null, null)), "invalid descriptor");
        rejects(n -> n.access |= ACC_FINAL | ACC_ABSTRACT, "final and abstract");
        rejects(n -> n.fields.add(new FieldNode(ACC_FINAL | ACC_VOLATILE, "bad", "I", null, null)), "final volatile");
        rejects(n -> n.fields.add(new FieldNode(ACC_PRIVATE | ACC_PUBLIC, "bad", "I", null, null)), "conflicting visibility");
        rejects(n -> n.methods.add(new MethodNode(ACC_ABSTRACT | ACC_STATIC, "bad", "()V", null, null)), "incompatible abstract");
        rejects(n -> n.methods.add(new MethodNode(ACC_STATIC, "<init>", "()V", null, null)), "invalid constructor");
        rejects(n -> n.methods.add(new MethodNode(ACC_STATIC, "<clinit>", "(I)V", null, null)), "invalid class initializer");
    }

    @Test void enforcesInterfaceDeclarationAndVersionRules() {
        rejects(n -> { n.access |= ACC_INTERFACE; }, "interfaces must be abstract");
        rejects(n -> {
            n.access |= ACC_INTERFACE | ACC_ABSTRACT;
            n.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, "bad", "I", null, null));
        }, "interface fields must be public static final");
        rejects(n -> {
            n.access |= ACC_INTERFACE | ACC_ABSTRACT;
            n.methods.add(new MethodNode(ACC_PROTECTED | ACC_ABSTRACT, "bad", "()V", null, null));
        }, "invalid interface method flags");
        rejects(n -> {
            n.access |= ACC_INTERFACE | ACC_ABSTRACT; n.version = V1_7;
            n.methods.add(new MethodNode(ACC_PUBLIC | ACC_STATIC, "bad", "()V", null, null));
        }, "pre-Java-8");
        rejects(n -> {
            n.access |= ACC_INTERFACE | ACC_ABSTRACT; n.version = V1_8;
            n.methods.add(new MethodNode(ACC_PRIVATE, "bad", "()V", null, null));
        }, "concrete method requires a Code body");
    }

    @Test void rejectsInvalidMemberNamesIncludingUnusedDeclarations() {
        for (String name : new String[]{"", "a.b", "a;b", "a[b", "a/b"}) {
            rejects(n -> n.fields.add(new FieldNode(0, name, "I", null, null)), "invalid name for field");
            rejects(n -> n.methods.add(new MethodNode(ACC_STATIC, name, "()V", null, null)), "invalid name for method");
        }
        for (String name : new String[]{"<other>", "a<b", "a>b", "<init>x"})
            rejects(n -> n.methods.add(new MethodNode(ACC_STATIC, name, "()V", null, null)), "invalid name for method");
        ClassNode entry = type(), unused = type(); unused.name = "Unused";
        unused.fields.add(new FieldNode(0, "bad/name", "I", null, null));
        assertThrows(IllegalArgumentException.class, () -> new ClassBytecodeCompiler(entry)
                .compileToWat(java.util.Map.of(entry.name, entry, unused.name, unused)));
        // Field names may contain angle brackets; ordinary method names may not.
        ClassNode legal = type();
        legal.fields.add(new FieldNode(ACC_STATIC, "<value>", "I", null, null));
        assertDoesNotThrow(() -> new ClassBytecodeCompiler(legal).compileToWat());
    }

    @Test void checksParameterSlotLimitWithReceiverWideValuesAndArrays() {
        for (boolean instance : new boolean[]{false, true}) {
            int limit = instance ? 254 : 255;
            for (String parameters : new String[]{"I".repeat(limit), "J".repeat(127) + (instance ? "" : "I"),
                    "D".repeat(127) + (instance ? "" : "I"), "[J".repeat(limit)}) {
                ClassNode legal = type();
                legal.methods.add(new MethodNode(instance ? 0 : ACC_STATIC, "accept", "(" + parameters + ")V", null, null));
                legal.methods.get(0).instructions.add(new InsnNode(RETURN));
                assertDoesNotThrow(() -> BytecodeDeclarations.validate(legal));
                rejects(n -> n.methods.add(new MethodNode(instance ? 0 : ACC_STATIC, "accept",
                        "(" + parameters + "I)V", null, null)), "exceeds 255 slots");
            }
        }
        rejects(n -> n.methods.add(new MethodNode(0, "<init>", "(" + "J".repeat(128) + ")V", null, null)),
                "exceeds 255 slots");
    }

    @Test void validatesConstantValueTypesEvenOnUnusedAndInstanceFields() {
        String[] descriptors = {"Z", "B", "C", "S", "I", "J", "F", "D", "Ljava/lang/String;", "[I", "Ljava/lang/Object;"};
        Object[] values = {1, 1L, 1f, 1d, "text", org.objectweb.asm.Type.getType("Ljava/lang/Object;")};
        for (String descriptor : descriptors) {
            for (Object value : values) {
                boolean valid = switch (descriptor) {
                    case "Z", "B", "C", "S", "I" -> value instanceof Integer;
                    case "J" -> value instanceof Long;
                    case "F" -> value instanceof Float;
                    case "D" -> value instanceof Double;
                    case "Ljava/lang/String;" -> value instanceof String;
                    default -> false;
                };
                for (int access : new int[]{0, ACC_STATIC, ACC_STATIC | ACC_FINAL}) {
                    ClassNode node = type();
                    node.fields.add(new FieldNode(access, "constant", descriptor, null, value));
                    if (valid) assertDoesNotThrow(() -> BytecodeDeclarations.validate(node));
                    else {
                        rejects(n -> n.fields.add(new FieldNode(access, "constant", descriptor, null, value)),
                                "ConstantValue type does not match");
                    }
                }
            }
        }
        ClassNode entry = type(), unused = type(); unused.name = "Unused";
        unused.fields.add(new FieldNode(ACC_STATIC, "constant", "J", null, 1));
        String message = assertThrows(IllegalArgumentException.class, () -> new ClassBytecodeCompiler(entry)
                .compileToWat(java.util.Map.of(entry.name, entry, unused.name, unused))).getMessage();
        assertTrue(message.contains("Unused"), message);
        assertTrue(message.contains("ConstantValue type does not match"), message);
        // A correctly typed String constant is structurally valid, but execution still needs a string runtime.
        rejects(n -> n.fields.add(new FieldNode(ACC_STATIC | ACC_FINAL, "text", "Ljava/lang/String;", null, "text")),
                "only numeric constant fields are supported");
    }

    @Test void acceptsDescriptorDistinctFieldsAndRejectsUnusedLinkedDuplicates() {
        ClassNode node = type();
        node.fields.add(new FieldNode(ACC_STATIC, "value", "I", null, null));
        node.fields.add(new FieldNode(ACC_STATIC, "value", "J", null, null));
        assertDoesNotThrow(() -> new ClassBytecodeCompiler(node).compileToWat());
        ClassNode unused = type(); unused.name = "Unused";
        unused.fields.add(new FieldNode(0, "value", "I", null, null));
        unused.fields.add(new FieldNode(0, "value", "I", null, null));
        assertThrows(IllegalArgumentException.class, () -> new ClassBytecodeCompiler(node)
                .compileToWat(java.util.Map.of(node.name, node, unused.name, unused)));
        assertNotEquals(TypeHelper.getGlobalFieldName("a/b", "c", "I"), TypeHelper.getGlobalFieldName("a_b", "c", "I"));
    }
}
