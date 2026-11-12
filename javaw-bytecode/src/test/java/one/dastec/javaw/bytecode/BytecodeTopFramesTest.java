package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeTopFramesTest implements Opcodes {
    @TempDir Path directory;

    private ClassNode owner(String descriptor, int stack) {
        ClassNode owner = new ClassNode(); owner.version = V17; owner.access = ACC_PUBLIC;
        owner.name = "Erased"; owner.superName = "java/lang/Object";
        MethodNode run = new MethodNode(ACC_PUBLIC | ACC_STATIC, "run", descriptor, null, null);
        run.maxLocals = 3; run.maxStack = stack; owner.methods.add(run); return owner;
    }

    private byte[] bytes(ClassNode owner) {
        ClassWriter writer = new ClassWriter(0); owner.accept(writer); return writer.toByteArray();
    }

    private Class<?> jvm(ClassNode owner) {
        byte[] bytes = bytes(owner);
        Class<?> type = new ClassLoader(null) {
            Class<?> load() { return defineClass(null, bytes, 0, bytes.length); }
        }.load();
        type.getDeclaredMethods(); return type;
    }

    private void run(String... command) throws Exception {
        Path log = directory.resolve("command.log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(20, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("Command timed out"); }
        assertEquals(0, process.exitValue(), Files.readString(log));
    }

    private void execute(ClassNode owner, String assertions) throws Exception {
        Path wat = directory.resolve("top.wat"), wasm = directory.resolve("top.wasm"), script = directory.resolve("top.cjs");
        Files.writeString(wat, BytecodeCompiler.compileClass(bytes(owner)));
        WasmTestSupport.assemble(wat, wasm);
        Files.writeString(script, """
                const assert = require('node:assert/strict');
                const fs = require('node:fs');
                const e = new WebAssembly.Instance(new WebAssembly.Module(fs.readFileSync(process.argv[2])),
                    {env:{memory:new WebAssembly.Memory({initial:2,maximum:20}),__heap_alloc:()=>1024}}).exports;
                """ + assertions);
        WasmTestSupport.node(script,wasm);
    }

    /** Branches retain a live integer above a value whose type/shape is erased at the join. */
    private ClassNode join(boolean wide, boolean mixedShape, boolean explicitFrames) {
        ClassNode owner = owner("(I)I", 4);
        if (!explicitFrames) owner.version = V1_5;
        var code = owner.methods.get(0).instructions;
        LabelNode alternate = new LabelNode(), join = new LabelNode();
        code.add(new VarInsnNode(ILOAD, 0)); code.add(new JumpInsnNode(IFEQ, alternate));
        code.add(new InsnNode(wide ? LCONST_0 : ICONST_0));
        code.add(new IntInsnNode(BIPUSH, 42)); code.add(new JumpInsnNode(GOTO, join));
        code.add(alternate);
        if (explicitFrames) code.add(new FrameNode(F_SAME, 0, null, 0, null));
        if (mixedShape) { code.add(new InsnNode(ICONST_0)); code.add(new InsnNode(ICONST_1)); }
        else code.add(new InsnNode(wide ? DCONST_0 : FCONST_0));
        code.add(new IntInsnNode(BIPUSH, 73)); code.add(join);
        if (explicitFrames) code.add(new FrameNode(F_FULL, 1, new Object[]{INTEGER}, wide ? 3 : 2,
                wide ? new Object[]{TOP, TOP, INTEGER} : new Object[]{TOP, INTEGER}));
        // The return consumes the live value and discards the remainder of the frame.
        code.add(new InsnNode(IRETURN)); return owner;
    }

    @Test void executesCategoryOneErasureWithJvmComparison() throws Exception {
        ClassNode owner = join(false, false, true);
        var method = jvm(owner).getMethod("run", int.class);
        execute(owner, "assert.equal(e['Erased.run'](1)," + method.invoke(null, 1) + ");"
                + "assert.equal(e['Erased.run'](0)," + method.invoke(null, 0) + ");");
    }

    @Test void executesCategoryTwoErasureWithoutMovingLiveValuesWithJvmComparison() throws Exception {
        for (boolean mixedShape : new boolean[]{false, true}) {
            ClassNode owner = join(true, mixedShape, true);
            var method = jvm(owner).getMethod("run", int.class);
            execute(owner, "assert.equal(e['Erased.run'](1)," + method.invoke(null, 1) + ");"
                    + "assert.equal(e['Erased.run'](0)," + method.invoke(null, 0) + ");");
        }
    }

    @Test void checksErasedWordsInExpandedAndCompressedByteInput() {
        ClassNode owner = join(true, true, true);
        for (int flags : new int[]{0, ClassReader.EXPAND_FRAMES}) {
            ClassNode parsed = new ClassNode(); new ClassReader(bytes(owner)).accept(parsed, flags);
            assertDoesNotThrow(() -> new ClassBytecodeCompiler(parsed).compileToWat());
        }
    }

    @Test void keepsLiveCategoryTwoValuesAboveErasedWords() throws Exception {
        ClassNode owner = owner("()J", 6); var code = owner.methods.get(0).instructions;
        LabelNode join = new LabelNode();
        code.add(new InsnNode(DCONST_0)); code.add(new LdcInsnNode(1234567890123L));
        code.add(new JumpInsnNode(GOTO, join)); code.add(join);
        code.add(new FrameNode(F_FULL, 0, null, 3, new Object[]{TOP, TOP, LONG}));
        code.add(new InsnNode(DUP2)); code.add(new InsnNode(LADD));
        code.add(new InsnNode(LRETURN));
        execute(owner, "assert.equal(e['Erased.run']()," + jvm(owner).getMethod("run").invoke(null) + "n);");
    }

    @Test void discardsErasedStackWordsOnVoidReturn() throws Exception {
        ClassNode owner = owner("()V", 3); var code = owner.methods.get(0).instructions;
        code.add(new InsnNode(LCONST_0)); code.add(new InsnNode(ICONST_0));
        // An optional frame at a fallthrough offset, without a LabelNode in direct tree input.
        code.add(new FrameNode(F_FULL, 0, null, 3, new Object[]{TOP, TOP, TOP}));
        code.add(new InsnNode(RETURN));
        jvm(owner);
        assertDoesNotThrow(() -> new ClassBytecodeCompiler(owner).compileToWat());
        execute(owner, "assert.equal(e['Erased.run'](),undefined);");
    }

    @Test void rejectsRefiningErasedWordsAndIncorrectWordHeightsLikeJvm() {
        for (boolean refine : new boolean[]{false, true}) {
            ClassNode owner = owner("()V", 2); var code = owner.methods.get(0).instructions;
            LabelNode target = new LabelNode();
            code.add(new InsnNode(LCONST_0)); code.add(new JumpInsnNode(GOTO, target)); code.add(target);
            code.add(new FrameNode(F_FULL, 0, null, refine ? 2 : 1, refine ? new Object[]{TOP, TOP} : new Object[]{TOP}));
            if (refine) {
                LabelNode next = new LabelNode(); code.add(new JumpInsnNode(GOTO, next)); code.add(next);
                code.add(new FrameNode(F_FULL, 0, null, 1, new Object[]{LONG}));
            }
            code.add(new InsnNode(RETURN));
            assertThrows(VerifyError.class, () -> jvm(owner));
            assertThrows(IllegalArgumentException.class, () -> BytecodeCompiler.compileClass(bytes(owner)));
        }
    }

    @Test void carriesSame1TopFramesThroughControlFlow() throws Exception {
        ClassNode owner = owner("()V", 1); var code = owner.methods.get(0).instructions;
        LabelNode first = new LabelNode(), second = new LabelNode();
        code.add(new InsnNode(FCONST_0)); code.add(new JumpInsnNode(GOTO, first)); code.add(first);
        code.add(new FrameNode(F_SAME1, 0, null, 1, new Object[]{TOP})); code.add(new InsnNode(NOP));
        code.add(new JumpInsnNode(GOTO, second)); code.add(second);
        code.add(new FrameNode(F_SAME1, 0, null, 1, new Object[]{TOP})); code.add(new InsnNode(RETURN));
        jvm(owner); execute(owner, "assert.equal(e['Erased.run'](),undefined);");
    }

    @Test void rejectsTypedUsesOfErasedWordsLikeJvm() {
        for (int opcode : new int[]{IRETURN, ARETURN, LRETURN, FRETURN, DRETURN, ISTORE, ASTORE, IFNULL, IADD}) {
            String result = switch (opcode) { case ARETURN -> "Ljava/lang/Object;"; case LRETURN -> "J";
                case FRETURN -> "F"; case DRETURN -> "D"; default -> "I"; };
            ClassNode owner = owner("()" + result, 4); var code = owner.methods.get(0).instructions;
            LabelNode target = new LabelNode(); code.add(new InsnNode(ICONST_0));
            code.add(new JumpInsnNode(GOTO, target)); code.add(target);
            code.add(new FrameNode(F_SAME1, 0, null, 1, new Object[]{TOP}));
            if (opcode == ISTORE || opcode == ASTORE) code.add(new VarInsnNode(opcode, 0));
            else if (opcode == IFNULL) {
                LabelNode end = new LabelNode(); code.add(new JumpInsnNode(opcode, end)); code.add(end);
                code.add(new FrameNode(F_SAME, 0, null, 0, null));
            } else {
                if (opcode == IADD) code.add(new InsnNode(ICONST_1));
                code.add(new InsnNode(opcode));
            }
            if (opcode == ISTORE || opcode == ASTORE || opcode == IFNULL || opcode == IADD) {
                code.add(new InsnNode(ICONST_0)); code.add(new InsnNode(IRETURN));
            }
            assertThrows(VerifyError.class, () -> jvm(owner), "opcode " + opcode);
            assertThrows(IllegalArgumentException.class, () -> BytecodeCompiler.compileClass(bytes(owner)), "opcode " + opcode);
        }
    }

    @Test void rejectsErasingOnlyHalfACategoryTwoValueIntoATypedWord() {
        ClassNode owner = owner("()V", 2); var code = owner.methods.get(0).instructions;
        LabelNode target = new LabelNode(); code.add(new InsnNode(LCONST_0));
        code.add(new JumpInsnNode(GOTO, target)); code.add(target);
        code.add(new FrameNode(F_FULL, 0, null, 2, new Object[]{TOP, INTEGER}));
        code.add(new InsnNode(POP2)); code.add(new InsnNode(RETURN));
        assertThrows(VerifyError.class, () -> jvm(owner));
        assertThrows(IllegalArgumentException.class, () -> BytecodeCompiler.compileClass(bytes(owner)));
    }

    @Test void rejectsManipulatingErasedWordsLikeJvm() {
        for (int opcode : new int[]{POP, POP2, DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, SWAP}) {
            ClassNode owner = owner("()V", 8); var code = owner.methods.get(0).instructions;
            LabelNode target = new LabelNode();
            for (int i = 0; i < 4; i++) code.add(new InsnNode(ICONST_0));
            code.add(new JumpInsnNode(GOTO, target)); code.add(target);
            code.add(new FrameNode(F_FULL, 0, null, 4, new Object[]{TOP, TOP, TOP, TOP}));
            code.add(new InsnNode(opcode)); code.add(new InsnNode(RETURN));
            assertThrows(VerifyError.class, () -> jvm(owner), "opcode " + opcode);
            assertThrows(IllegalArgumentException.class, () -> BytecodeCompiler.compileClass(bytes(owner)), "opcode " + opcode);
        }
    }

    @Test void rejectsConflictingInferredStackTypesForLegacyClassesLikeJvm() {
        for (boolean wide : new boolean[]{false, true}) {
            ClassNode owner = join(wide, false, false);
            assertThrows(VerifyError.class, () -> jvm(owner));
            assertThrows(IllegalArgumentException.class, () -> BytecodeCompiler.compileClass(bytes(owner)));
        }
    }

    @Test void preservesValidLegacyInferredJoinsWithJvmComparison() throws Exception {
        ClassNode owner = join(false, false, false);
        for (AbstractInsnNode instruction : owner.methods.get(0).instructions)
            if (instruction.getOpcode() == FCONST_0) owner.methods.get(0).instructions.set(instruction, new InsnNode(ICONST_1));
        var method = jvm(owner).getMethod("run", int.class);
        execute(owner, "assert.equal(e['Erased.run'](1)," + method.invoke(null, 1) + ");"
                + "assert.equal(e['Erased.run'](0)," + method.invoke(null, 0) + ");");
    }
}
