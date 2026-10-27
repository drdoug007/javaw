package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeFramesTest implements Opcodes {
    private ClassNode owner() {
        ClassNode owner = new ClassNode();
        owner.version = V17; owner.access = ACC_PUBLIC; owner.name = "Mapped";
        owner.superName = "java/lang/Object";
        return owner;
    }

    private MethodNode method(ClassNode owner, String descriptor, int locals, int stack) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "run", descriptor, null, null);
        method.maxLocals = locals; method.maxStack = stack; owner.methods.add(method);
        return method;
    }

    private void analyze(ClassNode owner) throws Exception {
        BytecodeFrames.analyze(owner, owner.methods.get(0), Map.of(owner.name, owner));
    }

    private void jvmVerify(ClassNode owner) {
        ClassWriter writer = new ClassWriter(0); owner.accept(writer);
        byte[] bytes = writer.toByteArray();
        new ClassLoader(null) {
            Class<?> load() { return defineClass(null, bytes, 0, bytes.length); }
        }.load().getDeclaredMethods();
    }

    @Test void acceptsCompressedFramesAndCategoryTwoLocalsThroughClassReader() throws Exception {
        ClassNode owner = owner(); MethodNode m = method(owner, "(J)J", 4, 2);
        LabelNode start = new LabelNode(), end = new LabelNode();
        m.instructions.add(new VarInsnNode(LLOAD, 0)); m.instructions.add(new VarInsnNode(LSTORE, 2));
        m.instructions.add(new JumpInsnNode(GOTO, start)); m.instructions.add(start);
        m.instructions.add(new FrameNode(F_APPEND, 1, new Object[]{LONG}, 0, null));
        m.instructions.add(new JumpInsnNode(GOTO, end)); m.instructions.add(end);
        m.instructions.add(new FrameNode(F_CHOP, 1, null, 0, null));
        m.instructions.add(new VarInsnNode(LLOAD, 0)); m.instructions.add(new InsnNode(LRETURN));
        analyze(owner); jvmVerify(owner);
        ClassWriter writer = new ClassWriter(0); owner.accept(writer);
        ClassNode parsed = new ClassNode(); new ClassReader(writer.toByteArray()).accept(parsed, 0);
        analyze(parsed);
        ClassNode expanded = new ClassNode(); new ClassReader(writer.toByteArray()).accept(expanded, ClassReader.EXPAND_FRAMES);
        analyze(expanded);
        assertTrue(new ClassBytecodeCompiler(parsed).compileToWat().contains("i64"));
    }

    @Test void declaredWiderReferenceConstrainsSubsequentInstructions() {
        for (boolean valid : new boolean[]{true, false}) {
            ClassNode owner = owner(); MethodNode m = method(owner, "(LMapped;)LMapped;", 1, 1);
            LabelNode target = new LabelNode();
            m.instructions.add(new VarInsnNode(ALOAD, 0)); m.instructions.add(new JumpInsnNode(GOTO, target));
            m.instructions.add(target);
            m.instructions.add(new FrameNode(F_SAME1, 0, null, 1, new Object[]{valid ? "Mapped" : "java/lang/Object"}));
            m.instructions.add(new InsnNode(ARETURN));
            if (valid) { assertDoesNotThrow(() -> analyze(owner)); assertDoesNotThrow(() -> jvmVerify(owner)); }
            else { assertThrows(Exception.class, () -> analyze(owner)); assertThrows(VerifyError.class, () -> jvmVerify(owner)); }
        }
    }

    @Test void discardedLocalCannotBeReadAfterDeclaredFrame() {
        ClassNode owner = owner(); MethodNode m = method(owner, "(I)I", 1, 1);
        LabelNode target = new LabelNode(); m.instructions.add(new JumpInsnNode(GOTO, target)); m.instructions.add(target);
        m.instructions.add(new FrameNode(F_FULL, 0, null, 0, null));
        m.instructions.add(new VarInsnNode(ILOAD, 0)); m.instructions.add(new InsnNode(IRETURN));
        assertThrows(Exception.class, () -> analyze(owner)); assertThrows(VerifyError.class, () -> jvmVerify(owner));
        assertThrows(IllegalArgumentException.class, () -> new ClassBytecodeCompiler(owner).compileToWat());
    }

    @Test void rejectsIncompatibleIncomingStackAndLocals() {
        for (boolean local : new boolean[]{true, false}) {
            ClassNode owner = owner(); MethodNode m = method(owner, "(I)I", 1, 1);
            LabelNode target = new LabelNode();
            if (!local) m.instructions.add(new InsnNode(ICONST_1));
            m.instructions.add(new JumpInsnNode(GOTO, target)); m.instructions.add(target);
            m.instructions.add(new FrameNode(F_FULL, 1, new Object[]{local ? FLOAT : INTEGER}, local ? 0 : 1,
                    local ? null : new Object[]{FLOAT}));
            if (local) m.instructions.add(new InsnNode(ICONST_1));
            else { m.instructions.add(new InsnNode(POP)); m.instructions.add(new InsnNode(ICONST_1)); }
            m.instructions.add(new InsnNode(IRETURN));
            String message = assertThrows(IllegalArgumentException.class, () -> analyze(owner)).getMessage();
            assertTrue(message.contains("incompatible"), message);
            assertThrows(VerifyError.class, () -> jvmVerify(owner));
        }
    }

    @Test void requiresModernBranchAndDeadBlockFramesButInfersLegacyBranches() throws Exception {
        ClassNode owner = owner(); MethodNode m = method(owner, "()I", 0, 1);
        LabelNode target = new LabelNode(); m.instructions.add(new JumpInsnNode(GOTO, target));
        m.instructions.add(target); m.instructions.add(new InsnNode(ICONST_1)); m.instructions.add(new InsnNode(IRETURN));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> analyze(owner)).getMessage().contains("missing frame"));
        assertThrows(VerifyError.class, () -> jvmVerify(owner));
        owner.version = V1_6; analyze(owner); jvmVerify(owner);
        owner.version = V17;
        m.instructions.insert(target, new FrameNode(F_SAME, 0, null, 0, null));
        analyze(owner); jvmVerify(owner);
        m.instructions.add(new InsnNode(RETURN));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> analyze(owner)).getMessage().contains("non-fallthrough"));
        assertThrows(VerifyError.class, () -> jvmVerify(owner));
    }

    @Test void verifiesDeclaredUnreachableBlocks() {
        ClassNode owner = owner(); MethodNode m = method(owner, "()I", 0, 1);
        m.instructions.add(new InsnNode(ICONST_1)); m.instructions.add(new InsnNode(IRETURN));
        m.instructions.add(new LabelNode()); m.instructions.add(new FrameNode(F_SAME, 0, null, 0, null));
        m.instructions.add(new InsnNode(FCONST_0)); m.instructions.add(new InsnNode(IRETURN));
        assertThrows(Exception.class, () -> analyze(owner)); assertThrows(VerifyError.class, () -> jvmVerify(owner));
    }

    @Test void rejectsMalformedUnlinkedAndUninitializedMaps() {
        for (String scenario : new String[]{"duplicate", "append", "missingType", "uninitialized", "height", "maxStack", "maxLocals"}) {
            ClassNode owner = owner(); MethodNode m = method(owner, "()V", 0, 0);
            m.instructions.add(new InsnNode(NOP));
            FrameNode frame = switch (scenario) {
                case "append" -> new FrameNode(F_APPEND, 0, null, 0, null);
                case "missingType" -> new FrameNode(F_FULL, 0, null, 1, new Object[]{"Missing"});
                case "uninitialized" -> new FrameNode(F_FULL, 0, null, 1, new Object[]{UNINITIALIZED_THIS});
                case "height" -> new FrameNode(F_SAME1, 0, null, 1, new Object[]{INTEGER});
                case "maxStack" -> new FrameNode(F_FULL, 0, null, 1, new Object[]{LONG});
                case "maxLocals" -> new FrameNode(F_FULL, 1, new Object[]{LONG}, 0, null);
                default -> new FrameNode(F_SAME, 0, null, 0, null);
            };
            if (scenario.equals("height")) m.maxStack = 1;
            m.instructions.add(frame);
            if (scenario.equals("duplicate")) m.instructions.add(new FrameNode(F_SAME, 0, null, 0, null));
            m.instructions.add(new InsnNode(RETURN));
            String error = assertThrows(IllegalArgumentException.class, () -> analyze(owner), scenario).getMessage();
            assertTrue(error.contains("stack map"), error);
        }
    }

    @Test void checksNullAndArrayAssignmentInFrames() throws Exception {
        for (String declared : new String[]{"[I", "java/lang/Object", "[Ljava/lang/Object;"}) {
            ClassNode owner = owner(); MethodNode m = method(owner, "([I)Ljava/lang/Object;", 1, 1);
            LabelNode target = new LabelNode(); m.instructions.add(new VarInsnNode(ALOAD, 0));
            m.instructions.add(new JumpInsnNode(GOTO, target)); m.instructions.add(target);
            m.instructions.add(new FrameNode(F_SAME1, 0, null, 1, new Object[]{declared}));
            m.instructions.add(new InsnNode(ARETURN));
            if (declared.equals("[Ljava/lang/Object;")) {
                assertThrows(IllegalArgumentException.class, () -> analyze(owner));
                assertThrows(VerifyError.class, () -> jvmVerify(owner));
            } else { analyze(owner); jvmVerify(owner); }
        }
        for (boolean incomingNull : new boolean[]{true, false}) {
            ClassNode owner = owner(); MethodNode m = method(owner, "(Ljava/lang/Object;)Ljava/lang/Object;", 1, 1);
            LabelNode target = new LabelNode();
            if (incomingNull) m.instructions.add(new InsnNode(ACONST_NULL));
            else m.instructions.add(new VarInsnNode(ALOAD, 0));
            m.instructions.add(new JumpInsnNode(GOTO, target)); m.instructions.add(target);
            m.instructions.add(new FrameNode(F_SAME1, 0, null, 1, new Object[]{NULL}));
            m.instructions.add(new InsnNode(ARETURN));
            if (incomingNull) { analyze(owner); jvmVerify(owner); }
            else {
                assertThrows(IllegalArgumentException.class, () -> analyze(owner));
                assertThrows(VerifyError.class, () -> jvmVerify(owner));
            }
        }
    }

    @Test void interfaceFrameAssignmentUsesJvmRulesWithoutLoadingHostClasses() throws Exception {
        ClassNode owner = owner(); MethodNode m = method(owner, "(Ljava/lang/Object;)Ljava/lang/Object;", 1, 1);
        LabelNode target = new LabelNode(); m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new JumpInsnNode(GOTO, target)); m.instructions.add(target);
        m.instructions.add(new FrameNode(F_SAME1, 0, null, 1, new Object[]{"java/lang/Runnable"}));
        m.instructions.add(new InsnNode(ARETURN));
        ClassNode iface = owner(); iface.name = "java/lang/Runnable"; iface.access |= ACC_INTERFACE | ACC_ABSTRACT;
        BytecodeFrames.analyze(owner, m, Map.of(owner.name, owner, iface.name, iface));
        jvmVerify(owner);
    }

    @Test void verifiesSwitchTargetsAndLoopBackEdges() throws Exception {
        ClassNode owner = owner(); MethodNode m = method(owner, "(I)I", 1, 1);
        LabelNode loop = new LabelNode(), fallback = new LabelNode();
        m.instructions.add(new VarInsnNode(ILOAD, 0));
        m.instructions.add(new LookupSwitchInsnNode(fallback, new int[]{1}, new LabelNode[]{loop}));
        m.instructions.add(loop); m.instructions.add(new FrameNode(F_SAME, 0, null, 0, null));
        m.instructions.add(new IincInsnNode(0, -1)); m.instructions.add(new VarInsnNode(ILOAD, 0));
        m.instructions.add(new JumpInsnNode(IFGT, loop));
        m.instructions.add(new InsnNode(ICONST_1)); m.instructions.add(new InsnNode(IRETURN));
        m.instructions.add(fallback); FrameNode frame = new FrameNode(F_SAME, 0, null, 0, null);
        m.instructions.add(frame); m.instructions.add(new InsnNode(ICONST_0)); m.instructions.add(new InsnNode(IRETURN));
        analyze(owner); jvmVerify(owner);
        assertDoesNotThrow(() -> new ClassBytecodeCompiler(owner).compileToWat());
        m.instructions.remove(frame);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> analyze(owner)).getMessage().contains("missing frame"));
        assertThrows(VerifyError.class, () -> jvmVerify(owner));
    }

    @Test void checksCategoryTwoStackWordLimitEvenWithoutMaps() {
        ClassNode owner = owner(); MethodNode m = method(owner, "()J", 0, 1);
        m.instructions.add(new InsnNode(LCONST_0)); m.instructions.add(new InsnNode(LRETURN));
        assertThrows(Exception.class, () -> analyze(owner)); assertThrows(VerifyError.class, () -> jvmVerify(owner));
    }
}
