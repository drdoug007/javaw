package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/** Module-local tag shared by explicit Java throws and bytecode handler dispatch. */
final class BytecodeExceptions {
    static final String TAG_WAT = "    (tag $__java_exception (export \"__java_exception\") (param i32))\n";

    static boolean usesArithmetic(ClassNode type) {
        for (MethodNode method : type.methods)
            for (var instruction : method.instructions) {
                int op = instruction.getOpcode();
                if (op == Opcodes.IDIV || op == Opcodes.IREM || op == Opcodes.LDIV || op == Opcodes.LREM) return true;
            }
        return false;
    }

    static boolean usesRuntime(ClassNode type) {
        for (MethodNode method : type.methods) {
            if (!method.tryCatchBlocks.isEmpty()) return true;
            for (var instruction : method.instructions)
                if (instruction.getOpcode() == Opcodes.ATHROW) return true;
        }
        return false;
    }
}
