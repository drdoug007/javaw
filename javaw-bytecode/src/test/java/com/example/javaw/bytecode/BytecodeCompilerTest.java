package com.example.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.*;

public class BytecodeCompilerTest {

    @Test
    public void testCompileSimpleCalculatorClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "com/example/calc/Calculator", null, "java/lang/Object", null);

        // Public static int add(int a, int b) { return a + b; }
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "add", "(II)I", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ILOAD, 0);
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitInsn(Opcodes.IADD);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        // Public static int multiply(int a, int b) { return a * b; }
        MethodVisitor mv2 = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "multiply", "(II)I", null, null);
        mv2.visitCode();
        mv2.visitVarInsn(Opcodes.ILOAD, 0);
        mv2.visitVarInsn(Opcodes.ILOAD, 1);
        mv2.visitInsn(Opcodes.IMUL);
        mv2.visitInsn(Opcodes.IRETURN);
        mv2.visitMaxs(2, 2);
        mv2.visitEnd();

        cw.visitEnd();
        byte[] classBytes = cw.toByteArray();

        String wat = BytecodeCompiler.compileClass(classBytes);
        assertNotNull(wat);
        assertTrue(wat.contains("(func $com_example_calc_Calculator_add"));
        assertTrue(wat.contains("(func $com_example_calc_Calculator_multiply"));
        assertTrue(wat.contains("i32.add"));
        assertTrue(wat.contains("i32.mul"));
        assertTrue(wat.contains("export \"com.example.calc.Calculator.add\""));
        assertTrue(wat.contains("export \"com.example.calc.Calculator.multiply\""));
    }

    @Test
    public void testCompileLoopAndBranching() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "com/example/calc/LoopMath", null, "java/lang/Object", null);

        // Public static int sumTo(int n) { int sum = 0; for(int i = 1; i <= n; i++) sum += i; return sum; }
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "sumTo", "(I)I", null, null);
        mv.visitCode();
        org.objectweb.asm.Label lLoop = new org.objectweb.asm.Label();
        org.objectweb.asm.Label lExit = new org.objectweb.asm.Label();

        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ISTORE, 1); // sum = 0
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitVarInsn(Opcodes.ISTORE, 2); // i = 1

        mv.visitLabel(lLoop);
        mv.visitVarInsn(Opcodes.ILOAD, 2); // i
        mv.visitVarInsn(Opcodes.ILOAD, 0); // n
        mv.visitJumpInsn(Opcodes.IF_ICMPGT, lExit); // if i > n goto exit

        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitVarInsn(Opcodes.ILOAD, 2);
        mv.visitInsn(Opcodes.IADD);
        mv.visitVarInsn(Opcodes.ISTORE, 1); // sum += i

        mv.visitIincInsn(2, 1); // i++
        mv.visitJumpInsn(Opcodes.GOTO, lLoop);

        mv.visitLabel(lExit);
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(2, 3);
        mv.visitEnd();

        cw.visitEnd();
        byte[] classBytes = cw.toByteArray();

        String wat = BytecodeCompiler.compileClass(classBytes);
        assertNotNull(wat);
        assertTrue(wat.contains("$com_example_calc_LoopMath_sumTo"));
        assertTrue(wat.contains("loop"));
        assertTrue(wat.contains("br $loop"));
    }
}
