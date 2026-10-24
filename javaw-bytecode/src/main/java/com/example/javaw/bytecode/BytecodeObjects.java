package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Intrinsic Object declarations and stable identities for the isolated, nonmoving bytecode heap. */
final class BytecodeObjects {
    static final String OBJECT = "java/lang/Object";
    private BytecodeObjects() {}

    static boolean identityMethod(String name, String descriptor) {
        return name.equals("equals") && descriptor.equals("(Ljava/lang/Object;)Z")
                || name.equals("hashCode") && descriptor.equals("()I");
    }

    static ClassNode declaration() {
        ClassNode type = new ClassNode();
        type.name = OBJECT;
        type.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER;
        type.version = Opcodes.V17;
        type.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null));
        type.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "hashCode", "()I", null, null));
        return type;
    }

    static boolean identityHash(MethodInsnNode call) {
        return call.getOpcode() == Opcodes.INVOKESTATIC && !call.itf
                && call.owner.equals("java/lang/System") && call.name.equals("identityHashCode")
                && call.desc.equals("(Ljava/lang/Object;)I");
    }

    static String runtimeWat() {
        // Addresses remain stable for the lifetime of a live object. Hashes need not match host JVM values.
        return "(func " + TypeHelper.getMethodFunctionName(OBJECT, "equals", "(Ljava/lang/Object;)Z")
                + " (param $this i32) (param $other i32) (result i32) local.get $this local.get $other i32.eq)\n"
                + "(func " + TypeHelper.getMethodFunctionName(OBJECT, "hashCode", "()I")
                + " (param $this i32) (result i32) local.get $this)\n"
                + "(func $__identity_hash (param $object i32) (result i32) local.get $object)\n";
    }
}
