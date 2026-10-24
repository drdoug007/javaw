package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import java.util.EnumSet;
import java.util.Set;

/** Module-local tag shared by explicit Java throws and bytecode handler dispatch. */
public final class BytecodeExceptions {
    static final String TAG_WAT = "    (tag $__java_exception (export \"__java_exception\") (param i32))\n";

    public enum Implicit {
        ARITHMETIC("ArithmeticException", "arithmetic"),
        NULL_POINTER("NullPointerException", "null_pointer"),
        CLASS_CAST("ClassCastException", "class_cast");

        public final String className;
        public final String helper;
        Implicit(String name, String suffix) {
            className = "java/lang/" + name;
            helper = "$__throw_" + suffix;
        }

        boolean uses(int opcode) {
            return switch (this) {
                case ARITHMETIC -> opcode == Opcodes.IDIV || opcode == Opcodes.IREM
                        || opcode == Opcodes.LDIV || opcode == Opcodes.LREM;
                case NULL_POINTER -> opcode == Opcodes.ATHROW || opcode == Opcodes.GETFIELD
                        || opcode == Opcodes.PUTFIELD || opcode == Opcodes.INVOKEVIRTUAL
                        || opcode == Opcodes.INVOKEINTERFACE || opcode == Opcodes.INVOKESPECIAL;
                case CLASS_CAST -> opcode == Opcodes.CHECKCAST;
            };
        }
    }

    static Set<Implicit> implicitRuntime(ClassNode type, BytecodeDispatch dispatch) {
        Set<Implicit> result = EnumSet.noneOf(Implicit.class);
        for (MethodNode method : type.methods)
            for (var instruction : method.instructions)
                for (Implicit kind : Implicit.values())
                    if (kind.uses(instruction.getOpcode()) && dispatch.hasImplicitException(kind)) result.add(kind);
        return result;
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
