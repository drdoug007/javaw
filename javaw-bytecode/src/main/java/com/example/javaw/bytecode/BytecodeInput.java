package com.example.javaw.bytecode;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.*;

/** Normalize legacy return-address subroutines before admission and frame analysis. */
final class BytecodeInput {
    static boolean normalizedOpcode(int opcode) {
        return opcode == Opcodes.JSR || opcode == Opcodes.RET;
    }

    static ClassNode read(byte[] bytes) {
        ClassNode type = new ClassNode();
        new ClassReader(bytes).accept(type, 0);
        normalize(type);
        return type;
    }

    static void normalize(ClassNode type) {
        for (int i = 0; i < type.methods.size(); i++) {
            MethodNode method = type.methods.get(i);
            boolean legacy = false;
            for (AbstractInsnNode insn : method.instructions) legacy |= normalizedOpcode(insn.getOpcode());
            if (!legacy) continue;
            MethodNode normalized = new MethodNode(method.access, method.name, method.desc, method.signature,
                    method.exceptions.toArray(String[]::new));
            try {
                method.accept(new JSRInlinerAdapter(normalized, method.access, method.name, method.desc,
                        method.signature, method.exceptions.toArray(String[]::new)));
            } catch (IllegalArgumentException error) {
                throw BytecodeCompatibility.failure(type, method, null, "legacy subroutine",
                        "recompile structurally valid JSR/RET bytecode: " + error.getMessage());
            }
            type.methods.set(i, normalized);
        }
    }
}
