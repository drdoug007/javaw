package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeConstructorTest implements Opcodes {
    private ClassNode type(String name, String parent) {
        ClassNode node = new ClassNode(); node.version = V17; node.access = ACC_PUBLIC | ACC_SUPER;
        node.name = name; node.superName = parent;
        return node;
    }

    private MethodNode method(ClassNode owner, String name, String desc, int access) {
        MethodNode method = new MethodNode(access, name, desc, null, null);
        method.maxLocals = 4; method.maxStack = 5; owner.methods.add(method); return method;
    }

    private MethodNode constructor(ClassNode owner, int access) {
        MethodNode m = method(owner, "<init>", "()V", access);
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new MethodInsnNode(INVOKESPECIAL, owner.superName, "<init>", "()V", false));
        m.instructions.add(new InsnNode(RETURN)); return m;
    }

    private Class<?> jvm(ClassNode owner, Map<String, ClassNode> linked) {
        Map<String, byte[]> bytes = new LinkedHashMap<>();
        linked.forEach((name, node) -> {
            ClassWriter writer = new ClassWriter(0); node.accept(writer);
            bytes.put(name.replace('/', '.'), writer.toByteArray());
        });
        try {
            Class<?> result = new ClassLoader(null) {
                @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
                    byte[] data = bytes.get(name);
                    if (data == null) throw new ClassNotFoundException(name);
                    return defineClass(name, data, 0, data.length);
                }
            }.loadClass(owner.name.replace('/', '.'));
            result.getDeclaredMethods(); result.getDeclaredConstructors(); return result;
        } catch (ClassNotFoundException error) { throw new AssertionError(error); }
    }

    private void reject(ClassNode owner, Map<String, ClassNode> linked) {
        assertThrows(VerifyError.class, () -> jvm(owner, linked), owner.sourceFile);
        String diagnostic = assertThrows(IllegalArgumentException.class,
                () -> new ClassBytecodeCompiler(owner).compileToWat(linked)).getMessage();
        assertTrue(diagnostic.contains("invalid operand stack") || diagnostic.contains("stack map"), diagnostic);
    }

    @Test void rejectsUninitializedUsesAndEscapesLikeJvm() {
        for (String scenario : new String[]{"return", "argument", "staticField", "instanceField", "readField",
                "virtual", "cast", "nullTest", "comparison", "wrongConstructor", "doubleInit", "constructorArgument", "initialized", "null"}) {
            ClassNode owner = type("Flow", "java/lang/Object"); owner.sourceFile = scenario; constructor(owner, ACC_PUBLIC);
            owner.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, "stored", "LFlow;", null, null));
            owner.fields.add(new FieldNode(ACC_PUBLIC, "n", "I", null, null));
            owner.fields.add(new FieldNode(ACC_PUBLIC, "ref", "LFlow;", null, null));
            MethodNode helper = method(owner, "helper", "(LFlow;)V", ACC_PUBLIC | ACC_STATIC);
            helper.instructions.add(new InsnNode(RETURN));
            MethodNode argumentInit = method(owner, "<init>", "(LFlow;)V", ACC_PRIVATE);
            argumentInit.instructions.add(new VarInsnNode(ALOAD, 0));
            argumentInit.instructions.add(new MethodInsnNode(INVOKESPECIAL, owner.superName, "<init>", "()V", false));
            argumentInit.instructions.add(new InsnNode(RETURN));
            MethodNode value = method(owner, "value", "()I", ACC_PUBLIC);
            value.instructions.add(new InsnNode(ICONST_1)); value.instructions.add(new InsnNode(IRETURN));
            MethodNode run = method(owner, "run", "(LFlow;)LFlow;", ACC_PUBLIC | ACC_STATIC);
            var code = run.instructions;
            if (scenario.equals("initialized")) code.add(new VarInsnNode(ALOAD, 0));
            else if (scenario.equals("null")) code.add(new InsnNode(ACONST_NULL));
            else code.add(new TypeInsnNode(NEW, owner.name));
            switch (scenario) {
                case "return" -> code.add(new InsnNode(ARETURN));
                case "argument" -> code.add(new MethodInsnNode(INVOKESTATIC, owner.name, "helper", "(LFlow;)V", false));
                case "staticField" -> code.add(new FieldInsnNode(PUTSTATIC, owner.name, "stored", "LFlow;"));
                case "instanceField" -> {
                    code.add(new VarInsnNode(ASTORE, 1)); code.add(new VarInsnNode(ALOAD, 0));
                    code.add(new VarInsnNode(ALOAD, 1)); code.add(new FieldInsnNode(PUTFIELD, owner.name, "ref", "LFlow;"));
                }
                case "readField" -> { code.add(new FieldInsnNode(GETFIELD, owner.name, "n", "I")); code.add(new InsnNode(POP)); }
                case "virtual" -> { code.add(new MethodInsnNode(INVOKEVIRTUAL, owner.name, "value", "()I", false)); code.add(new InsnNode(POP)); }
                case "cast" -> { code.add(new TypeInsnNode(CHECKCAST, owner.name)); code.add(new InsnNode(POP)); }
                case "nullTest", "comparison" -> {
                    LabelNode end = new LabelNode();
                    if (scenario.equals("comparison")) code.add(new InsnNode(DUP));
                    code.add(new JumpInsnNode(scenario.equals("comparison") ? IF_ACMPEQ : IFNULL, end));
                    code.add(end); code.add(new FrameNode(F_SAME, 0, null, 0, null));
                }
                case "constructorArgument" -> {
                    code.add(new InsnNode(DUP));
                    code.add(new MethodInsnNode(INVOKESPECIAL, owner.name, "<init>", "(LFlow;)V", false));
                }
                case "doubleInit" -> {
                    code.add(new InsnNode(DUP));
                    code.add(new MethodInsnNode(INVOKESPECIAL, owner.name, "<init>", "()V", false));
                    code.add(new MethodInsnNode(INVOKESPECIAL, owner.name, "<init>", "()V", false));
                }
                default -> code.add(new MethodInsnNode(INVOKESPECIAL,
                        scenario.equals("wrongConstructor") ? "java/lang/Object" : owner.name, "<init>", "()V", false));
            }
            if (!scenario.equals("return")) { code.add(new InsnNode(ACONST_NULL)); code.add(new InsnNode(ARETURN)); }
            if (scenario.equals("nullTest") || scenario.equals("comparison")) {
                assertDoesNotThrow(() -> jvm(owner, Map.of(owner.name, owner)), scenario);
                assertDoesNotThrow(() -> new ClassBytecodeCompiler(owner).compileToWat(), scenario);
            } else reject(owner, Map.of(owner.name, owner));
        }
    }

    @Test void constructorFlagSurvivesDiscardingAndOverwritingThis() {
        for (boolean overwrite : new boolean[]{true, false}) {
            ClassNode owner = type("NoSuper", "java/lang/Object");
            MethodNode init = method(owner, "<init>", "()V", ACC_PUBLIC);
            if (overwrite) { init.instructions.add(new InsnNode(ACONST_NULL)); init.instructions.add(new VarInsnNode(ASTORE, 0)); }
            init.instructions.add(new InsnNode(RETURN)); reject(owner, Map.of(owner.name, owner));
        }
    }

    @Test void rejectsWrongThisInitializationAndPrematureThisUse() {
        for (String scenario : new String[]{"grandparent", "read", "inheritedWrite", "escape"}) {
            ClassNode base = type("Base", "java/lang/Object"); constructor(base, ACC_PUBLIC);
            base.fields.add(new FieldNode(ACC_PUBLIC, "n", "I", null, null));
            ClassNode child = type("Child", "Base");
            child.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, "stored", "LChild;", null, null));
            MethodNode init = method(child, "<init>", "()V", ACC_PUBLIC); var code = init.instructions;
            code.add(new VarInsnNode(ALOAD, 0));
            switch (scenario) {
                case "grandparent" -> code.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
                case "read" -> { code.add(new FieldInsnNode(GETFIELD, "Base", "n", "I")); code.add(new InsnNode(POP)); }
                case "inheritedWrite" -> { code.add(new InsnNode(ICONST_1)); code.add(new FieldInsnNode(PUTFIELD, "Child", "n", "I")); }
                case "escape" -> code.add(new FieldInsnNode(PUTSTATIC, "Child", "stored", "LChild;"));
                default -> { code.add(new InsnNode(POP)); }
            }
            if (!scenario.equals("grandparent")) {
                code.add(new VarInsnNode(ALOAD, 0));
                code.add(new MethodInsnNode(INVOKESPECIAL, "Base", "<init>", "()V", false));
            }
            code.add(new InsnNode(RETURN)); reject(child, Map.of("Base", base, "Child", child));
        }
    }

    @Test void initializesEveryStackAndLocalAliasAndAllowsPreSuperOwnFieldWrite() throws Exception {
        ClassNode owner = type("Aliases", "java/lang/Object");
        owner.fields.add(new FieldNode(ACC_PUBLIC, "n", "I", null, null));
        MethodNode init = method(owner, "<init>", "()V", ACC_PUBLIC);
        init.instructions.add(new VarInsnNode(ALOAD, 0)); init.instructions.add(new VarInsnNode(ASTORE, 1));
        init.instructions.add(new VarInsnNode(ALOAD, 1)); init.instructions.add(new InsnNode(ICONST_5));
        init.instructions.add(new FieldInsnNode(PUTFIELD, owner.name, "n", "I"));
        init.instructions.add(new VarInsnNode(ALOAD, 1));
        init.instructions.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        init.instructions.add(new VarInsnNode(ALOAD, 0)); init.instructions.add(new FieldInsnNode(GETFIELD, owner.name, "n", "I"));
        init.instructions.add(new InsnNode(POP)); init.instructions.add(new InsnNode(RETURN));
        MethodNode run = method(owner, "run", "()I", ACC_PUBLIC | ACC_STATIC);
        run.instructions.add(new TypeInsnNode(NEW, owner.name)); run.instructions.add(new InsnNode(DUP));
        run.instructions.add(new VarInsnNode(ASTORE, 0)); run.instructions.add(new InsnNode(DUP));
        run.instructions.add(new MethodInsnNode(INVOKESPECIAL, owner.name, "<init>", "()V", false));
        run.instructions.add(new InsnNode(POP)); run.instructions.add(new VarInsnNode(ALOAD, 0));
        run.instructions.add(new FieldInsnNode(GETFIELD, owner.name, "n", "I")); run.instructions.add(new InsnNode(IRETURN));
        assertEquals(5, jvm(owner, Map.of(owner.name, owner)).getMethod("run").invoke(null));
        assertDoesNotThrow(() -> new ClassBytecodeCompiler(owner).compileToWat());
    }

    @Test void checksCrossPackageProtectedConstructorReceivers() {
        for (boolean allocateBase : new boolean[]{true, false}) {
            ClassNode base = type("p/Base", "java/lang/Object"); constructor(base, ACC_PROTECTED);
            ClassNode child = type("q/Child", "p/Base"); constructor(child, ACC_PUBLIC);
            if (allocateBase) {
                MethodNode run = method(child, "run", "()V", ACC_PUBLIC | ACC_STATIC);
                run.instructions.add(new TypeInsnNode(NEW, base.name));
                run.instructions.add(new MethodInsnNode(INVOKESPECIAL, base.name, "<init>", "()V", false));
                run.instructions.add(new InsnNode(RETURN));
            }
            Map<String, ClassNode> linked = Map.of(base.name, base, child.name, child);
            if (allocateBase) reject(child, linked);
            else { assertDoesNotThrow(() -> jvm(child, linked)); assertDoesNotThrow(() -> new ClassBytecodeCompiler(child).compileToWat(linked)); }
        }
    }

    @Test void supportsUninitializedThisAndNewIdentitiesInCompressedAndExpandedMaps() throws Exception {
        ClassNode owner = type("MappedInit", "java/lang/Object");
        MethodNode init = method(owner, "<init>", "()V", ACC_PUBLIC);
        LabelNode target = new LabelNode(); init.instructions.add(new JumpInsnNode(GOTO, target));
        init.instructions.add(target); init.instructions.add(new FrameNode(F_SAME, 0, null, 0, null));
        init.instructions.add(new VarInsnNode(ALOAD, 0));
        init.instructions.add(new MethodInsnNode(INVOKESPECIAL, owner.superName, "<init>", "()V", false));
        init.instructions.add(new InsnNode(RETURN));
        MethodNode run = method(owner, "run", "()LMappedInit;", ACC_PUBLIC | ACC_STATIC);
        LabelNode allocation = new LabelNode(), join = new LabelNode();
        run.instructions.add(allocation); run.instructions.add(new TypeInsnNode(NEW, owner.name));
        run.instructions.add(new VarInsnNode(ASTORE, 0)); run.instructions.add(new JumpInsnNode(GOTO, join));
        run.instructions.add(join); run.instructions.add(new FrameNode(F_APPEND, 1, new Object[]{allocation}, 0, null));
        run.instructions.add(new VarInsnNode(ALOAD, 0)); run.instructions.add(new InsnNode(DUP));
        run.instructions.add(new MethodInsnNode(INVOKESPECIAL, owner.name, "<init>", "()V", false));
        run.instructions.add(new InsnNode(ARETURN));
        jvm(owner, Map.of(owner.name, owner)); new ClassBytecodeCompiler(owner).compileToWat();
        ClassWriter writer = new ClassWriter(0); owner.accept(writer);
        for (int flags : new int[]{0, ClassReader.EXPAND_FRAMES}) {
            ClassNode parsed = new ClassNode(); new ClassReader(writer.toByteArray()).accept(parsed, flags);
            assertDoesNotThrow(() -> new ClassBytecodeCompiler(parsed).compileToWat());
        }
    }

    @Test void requiresInitializationOnEveryConstructorReturnPath() {
        for (boolean initializeBoth : new boolean[]{true, false}) {
            ClassNode owner = type("BranchInit", "java/lang/Object"); owner.version = V1_6;
            MethodNode init = method(owner, "<init>", "(I)V", ACC_PUBLIC);
            LabelNode alternate = new LabelNode(), end = new LabelNode();
            var code = init.instructions;
            code.add(new VarInsnNode(ILOAD, 1)); code.add(new JumpInsnNode(IFEQ, alternate));
            code.add(new VarInsnNode(ALOAD, 0));
            code.add(new MethodInsnNode(INVOKESPECIAL, owner.superName, "<init>", "()V", false));
            code.add(new JumpInsnNode(GOTO, end)); code.add(alternate);
            if (initializeBoth) {
                code.add(new VarInsnNode(ALOAD, 0));
                code.add(new MethodInsnNode(INVOKESPECIAL, owner.superName, "<init>", "()V", false));
            }
            code.add(end); code.add(new InsnNode(RETURN));
            if (initializeBoth) {
                assertDoesNotThrow(() -> jvm(owner, Map.of(owner.name, owner)));
                assertDoesNotThrow(() -> new ClassBytecodeCompiler(owner).compileToWat());
            } else reject(owner, Map.of(owner.name, owner));
        }
    }

    @Test void doesNotMergeDistinctNewIdentitiesIntoAnInitializableReference() {
        ClassNode owner = type("Distinct", "java/lang/Object"); owner.version = V1_6; constructor(owner, ACC_PUBLIC);
        MethodNode run = method(owner, "run", "(I)V", ACC_PUBLIC | ACC_STATIC);
        LabelNode alternate = new LabelNode(), join = new LabelNode();
        var code = run.instructions;
        code.add(new VarInsnNode(ILOAD, 0)); code.add(new JumpInsnNode(IFEQ, alternate));
        code.add(new TypeInsnNode(NEW, owner.name)); code.add(new JumpInsnNode(GOTO, join));
        code.add(alternate); code.add(new TypeInsnNode(NEW, owner.name)); code.add(join);
        code.add(new MethodInsnNode(INVOKESPECIAL, owner.name, "<init>", "()V", false)); code.add(new InsnNode(RETURN));
        reject(owner, Map.of(owner.name, owner));
    }

    @Test void supportsRevisitedNewWhenEarlierLocalAliasesAreDiscarded() throws Exception {
        ClassNode owner = type("Repeated", "java/lang/Object"); constructor(owner, ACC_PUBLIC);
        MethodNode run = method(owner, "run", "(I)LRepeated;", ACC_PUBLIC | ACC_STATIC);
        LabelNode allocation = new LabelNode(); var code = run.instructions;
        code.add(allocation); code.add(new FrameNode(F_SAME, 0, null, 0, null));
        code.add(new TypeInsnNode(NEW, owner.name)); code.add(new VarInsnNode(ASTORE, 1));
        code.add(new IincInsnNode(0, -1)); code.add(new VarInsnNode(ILOAD, 0));
        code.add(new JumpInsnNode(IFGT, allocation));
        code.add(new VarInsnNode(ALOAD, 1)); code.add(new InsnNode(DUP));
        code.add(new MethodInsnNode(INVOKESPECIAL, owner.name, "<init>", "()V", false)); code.add(new InsnNode(ARETURN));
        assertNotNull(jvm(owner, Map.of(owner.name, owner)).getMethod("run", int.class).invoke(null, 3));
        assertDoesNotThrow(() -> new ClassBytecodeCompiler(owner).compileToWat());
    }

    @Test void checksInitializationFlagWhenOnlyAStackAliasRemains() {
        ClassNode owner = type("StackThis", "java/lang/Object");
        MethodNode init = method(owner, "<init>", "()V", ACC_PUBLIC);
        var code = init.instructions; LabelNode join = new LabelNode();
        code.add(new VarInsnNode(ALOAD, 0)); code.add(new InsnNode(ACONST_NULL)); code.add(new VarInsnNode(ASTORE, 0));
        code.add(new JumpInsnNode(GOTO, join)); code.add(join);
        code.add(new FrameNode(F_FULL, 1, new Object[]{NULL}, 1, new Object[]{UNINITIALIZED_THIS}));
        code.add(new MethodInsnNode(INVOKESPECIAL, owner.superName, "<init>", "()V", false)); code.add(new InsnNode(RETURN));
        reject(owner, Map.of(owner.name, owner));
        code.remove(join.getPrevious()); // Branch.
        code.remove(join.getNext()); // Stack map; straight-line flow retains the constructor flag.
        assertDoesNotThrow(() -> jvm(owner, Map.of(owner.name, owner)));
        assertDoesNotThrow(() -> new ClassBytecodeCompiler(owner).compileToWat());
    }

    @Test void rejectsForgedUninitializedFrameTokensAndInitializationFlags() {
        for (String scenario : new String[]{"initialized", "topThis", "wrongSite", "nonNew", "outside"}) {
            ClassNode owner = type("Forged", "java/lang/Object");
            MethodNode m = scenario.equals("initialized") || scenario.equals("topThis")
                    ? method(owner, "<init>", "()V", ACC_PUBLIC)
                    : method(owner, "run", "()V", ACC_PUBLIC | ACC_STATIC);
            LabelNode site = new LabelNode(), target = new LabelNode(), other = new LabelNode();
            if (scenario.equals("initialized") || scenario.equals("topThis")) {
                m.instructions.add(new JumpInsnNode(GOTO, target)); m.instructions.add(target);
                m.instructions.add(new FrameNode(F_FULL, 1, new Object[]{scenario.equals("topThis") ? TOP : owner.name}, 0, null));
            } else {
                m.instructions.add(site); m.instructions.add(new TypeInsnNode(NEW, owner.name));
                m.instructions.add(new VarInsnNode(ASTORE, 0));
                if (scenario.equals("wrongSite")) { m.instructions.add(other); m.instructions.add(new TypeInsnNode(NEW, owner.name)); m.instructions.add(new InsnNode(POP)); }
                else if (scenario.equals("nonNew")) { m.instructions.add(other); m.instructions.add(new InsnNode(NOP)); }
                m.instructions.add(new JumpInsnNode(GOTO, target)); m.instructions.add(target);
                m.instructions.add(new FrameNode(F_APPEND, 1, new Object[]{other}, 0, null));
            }
            m.instructions.add(new InsnNode(RETURN));
            if (!scenario.equals("outside")) assertThrows(LinkageError.class, () -> jvm(owner, Map.of(owner.name, owner)), scenario);
            assertThrows(IllegalArgumentException.class, () -> new ClassBytecodeCompiler(owner).compileToWat());
        }
    }
}
