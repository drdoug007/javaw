package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.SimpleVerifier;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Reference-aware inference over linked metadata, never the compiler host's class loader. */
final class BytecodeVerifier extends SimpleVerifier {
    private static final Type OBJECT = Type.getObjectType("java/lang/Object");
    private final Map<String, ClassNode> linked;

    BytecodeVerifier(ClassNode owner, Map<String, ClassNode> linked) {
        super(Opcodes.ASM9, Type.getObjectType(owner.name),
                owner.superName == null ? null : Type.getObjectType(owner.superName),
                owner.interfaces.stream().map(Type::getObjectType).toList(),
                (owner.access & Opcodes.ACC_INTERFACE) != 0);
        this.linked = linked;
    }

    @Override protected boolean isInterface(Type type) {
        if (type.getSort() != Type.OBJECT) return false;
        String name = type.getInternalName();
        ClassNode node = linked.get(name);
        return node != null ? (node.access & Opcodes.ACC_INTERFACE) != 0
                : name.equals("java/lang/Cloneable") || name.equals("java/io/Serializable");
    }

    @Override protected Type getSuperClass(Type type) {
        if (type.equals(OBJECT)) return null;
        if (type.getSort() == Type.ARRAY) return OBJECT;
        ClassNode node = linked.get(type.getInternalName());
        return node == null || node.superName == null ? OBJECT : Type.getObjectType(node.superName);
    }

    @Override protected boolean isAssignableFrom(Type target, Type source) {
        if (target.equals(source)) return true;
        if (!reference(target) || !reference(source)) return false;
        if (source.getSort() == Type.OBJECT && source.getInternalName().equals("null")) return true;
        if (target.equals(OBJECT)) return true;
        if (source.getSort() == Type.ARRAY) {
            if (target.getSort() == Type.ARRAY)
                return isAssignableFrom(Type.getType(target.getDescriptor().substring(1)),
                        Type.getType(source.getDescriptor().substring(1)));
            return target.getInternalName().equals("java/lang/Cloneable")
                    || target.getInternalName().equals("java/io/Serializable");
        }
        return target.getSort() == Type.OBJECT && subtype(source.getInternalName(), target.getInternalName(), new HashSet<>());
    }

    private boolean subtype(String source, String target, Set<String> visited) {
        if (source.equals(target)) return true;
        if (!visited.add(source)) return false;
        ClassNode node = linked.get(source);
        if (node == null) return false;
        if (node.superName != null && subtype(node.superName, target, visited)) return true;
        for (String iface : node.interfaces) if (subtype(iface, target, visited)) return true;
        return false;
    }

    private boolean reference(Type type) { return type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY; }

    @Override protected Class<?> getClass(Type type) {
        throw new IllegalArgumentException("link type metadata instead of loading compiler-host class " + type);
    }
}
