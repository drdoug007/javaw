package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Map;

/** Eager descriptor linkage for the single-loader, unnamed, closed-world target. */
final class BytecodeLinkage {
    static void validateDescriptors(Map<String, ClassNode> linked) {
        for (ClassNode owner : linked.values()) {
            for (FieldNode field : owner.fields)
                resolve(owner, null, Type.getType(field.desc), linked);
            for (MethodNode method : owner.methods) {
                for (Type parameter : Type.getArgumentTypes(method.desc)) resolve(owner, method, parameter, linked);
                resolve(owner, method, Type.getReturnType(method.desc), linked);
                for (String exception : method.exceptions) {
                    if (!BytecodeDeclarations.validClassName(exception))
                        throw BytecodeCompatibility.failure(owner, method, null, "exception declaration", "use a valid internal exception class name");
                    resolve(owner, method, Type.getObjectType(exception), linked);
                }
            }
        }
    }

    private static void resolve(ClassNode owner, MethodNode method, Type type, Map<String, ClassNode> linked) {
        if (type.getSort() == Type.ARRAY) type = type.getElementType();
        if (type.getSort() != Type.OBJECT || type.getInternalName().equals("java/lang/Object")) return;
        if (!linked.containsKey(type.getInternalName()))
            throw BytecodeCompatibility.failure(owner, method, null, "descriptor resolution " + type.getDescriptor(),
                    "link the descriptor class/interface " + type.getInternalName()
                            + "; compiler-host JDK classes are not runtime definitions");
        // Descriptor types need not be accessible from the declaring class. Access is checked on
        // symbolic owners at actual field/method/type instructions, as on the JVM.
    }

    static void validateReference(ClassNode owner, MethodNode method, AbstractInsnNode instruction) {
        String reason = null;
        if (instruction instanceof FieldInsnNode field) {
            if (!BytecodeDeclarations.validClassName(field.owner)
                    || !BytecodeDeclarations.validMemberName(field.name, false)
                    || !BytecodeDeclarations.validDescriptor(field.desc, false))
                reason = "use a valid field owner, name and value descriptor";
        } else if (instruction instanceof MethodInsnNode call) {
            if (!(BytecodeDeclarations.validClassName(call.owner)
                    || BytecodeDeclarations.validDescriptor(call.owner, false) && call.owner.startsWith("["))
                    || !BytecodeDeclarations.validMemberName(call.name, true)
                    || !BytecodeDeclarations.validDescriptor(call.desc, true))
                reason = "use a valid method owner, name and method descriptor";
            else {
                int slots = call.getOpcode() == Opcodes.INVOKESTATIC ? 0 : 1;
                for (Type parameter : Type.getArgumentTypes(call.desc)) slots += parameter.getSize();
                if (slots > 255) reason = "method reference exceeds 255 parameter slots";
                else if (call.name.equals("<init>") && !call.desc.endsWith(")V"))
                    reason = "constructor references must return void";
                else if ((owner.version & 0xffff) != 0 && (owner.version & 0xffff) < Opcodes.V1_8 && call.itf
                        && (call.getOpcode() == Opcodes.INVOKESTATIC || call.getOpcode() == Opcodes.INVOKESPECIAL))
                    reason = "interface static/special references require Java 8";
            }
        }
        if (reason != null)
            throw BytecodeCompatibility.failure(owner, method, instruction, "member reference", reason);
    }
}
