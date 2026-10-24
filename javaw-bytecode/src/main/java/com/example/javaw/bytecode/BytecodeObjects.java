package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import java.util.Map;

/** Intrinsic Object declarations and stable identities for the isolated, nonmoving bytecode heap. */
final class BytecodeObjects {
    static final String OBJECT = "java/lang/Object";
    private BytecodeObjects() {}

    static boolean objectMethod(String name, String descriptor) {
        return name.equals("equals") && descriptor.equals("(Ljava/lang/Object;)Z")
                || name.equals("hashCode") && descriptor.equals("()I")
                || name.equals("getClass") && descriptor.equals("()Ljava/lang/Class;")
                || name.equals("toString") && descriptor.equals("()Ljava/lang/String;")
                || name.equals("clone") && descriptor.equals("()Ljava/lang/Object;")
                || name.equals("finalize") && descriptor.equals("()V");
    }

    static ClassNode declaration() {
        ClassNode type = new ClassNode();
        type.name = OBJECT;
        type.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER;
        type.version = Opcodes.V17;
        type.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null));
        type.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "hashCode", "()I", null, null));
        type.methods.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, "getClass", "()Ljava/lang/Class;", null, null));
        type.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null));
        type.methods.add(new MethodNode(Opcodes.ACC_PROTECTED, "clone", "()Ljava/lang/Object;", null, null));
        type.methods.add(new MethodNode(Opcodes.ACC_PROTECTED, "finalize", "()V", null, null));
        return type;
    }

    static boolean identityHash(MethodInsnNode call) {
        return call.getOpcode() == Opcodes.INVOKESTATIC && !call.itf
                && call.owner.equals("java/lang/System") && call.name.equals("identityHashCode")
                && call.desc.equals("(Ljava/lang/Object;)I");
    }

    static String runtimeWat(Map<String,org.objectweb.asm.tree.ClassNode> linked, BytecodeDispatch dispatch) {
        // Addresses remain stable for the lifetime of a live object. Hashes need not match host JVM values.
        return cloneWat(linked,dispatch) + "(func " + TypeHelper.getMethodFunctionName(OBJECT, "equals", "(Ljava/lang/Object;)Z")
                + " (param $this i32) (param $other i32) (result i32) local.get $this local.get $other i32.eq)\n"
                + "(func " + TypeHelper.getMethodFunctionName(OBJECT, "hashCode", "()I")
                + " (param $this i32) (result i32) local.get $this)\n"
                + "(func " + TypeHelper.getMethodFunctionName(OBJECT, "getClass", "()Ljava/lang/Class;")
                + " (param $this i32) (result i32) local.get $this i32.load call $__class_mirror)\n"
                + "(func " + TypeHelper.getMethodFunctionName(OBJECT, "toString", "()Ljava/lang/String;")
                + " (param $this i32) (result i32) local.get $this call "
                + TypeHelper.getMethodFunctionName("javaw/runtime/ExceptionSupport", "objectText", "(Ljava/lang/Object;)Ljava/lang/String;") + ")\n"
                + "(func " + TypeHelper.getMethodFunctionName(OBJECT, "finalize", "()V") + " (param $this i32))\n"
                + "(func $__identity_hash (param $object i32) (result i32) local.get $object)\n";
    }
    private static String cloneWat(Map<String,org.objectweb.asm.tree.ClassNode> linked,BytecodeDispatch dispatch) {
        StringBuilder out=new StringBuilder("(func ").append(TypeHelper.getMethodFunctionName(OBJECT,"clone","()Ljava/lang/Object;"))
                .append(" (param $this i32) (result i32) (local $copy i32) (local $size i32)\n")
                .append("local.get $this i32.load i32.const 0 i32.lt_s if local.get $this call $__array_clone return end\n")
                .append("i32.const 0\n");
        for(int id:dispatch.typeTestIds("java/lang/Cloneable"))
            out.append("local.get $this i32.load i32.const ").append(id).append(" i32.eq i32.or\n");
        out.append("i32.eqz if call ").append(TypeHelper.getMethodFunctionName("javaw/runtime/ExceptionSupport","cloneFailure","()V"))
                .append(" unreachable end\n");
        for(var entry:new BytecodeObjectLayouts(linked).sizes().entrySet())
            out.append("local.get $this i32.load i32.const ").append(dispatch.classId(entry.getKey()))
                    .append(" i32.eq if i32.const ").append(entry.getValue()).append(" local.set $size end\n");
        out.append("local.get $size call $__heap_alloc local.tee $copy i32.eqz if ")
                .append(dispatch.implicitFailureWat(BytecodeExceptions.Implicit.OUT_OF_MEMORY)).append(" end\n")
                .append("local.get $copy local.get $this local.get $size memory.copy local.get $copy)\n");
        return out.toString();
    }

}
