package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.SimpleVerifier;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reference-aware inference over linked metadata, never the compiler host's class loader. */
final class BytecodeVerifier extends SimpleVerifier {
    private static final Type OBJECT = Type.getObjectType("java/lang/Object");
    private final Map<String, ClassNode> linked;
    private final Type callerType;
    private final ClassNode owner;
    final UninitializedValue uninitializedThis;
    private final Map<TypeInsnNode, UninitializedValue> allocations = new IdentityHashMap<>();

    BytecodeVerifier(ClassNode owner, Map<String, ClassNode> linked) {
        super(Opcodes.ASM9, Type.getObjectType(owner.name),
                owner.superName == null ? null : Type.getObjectType(owner.superName),
                owner.interfaces.stream().map(Type::getObjectType).toList(),
                (owner.access & Opcodes.ACC_INTERFACE) != 0);
        this.linked = linked;
        this.owner = owner;
        this.uninitializedThis = new UninitializedValue(Type.getObjectType(owner.name), null);
        this.callerType = Type.getObjectType(owner.name);
    }

    UninitializedValue allocation(TypeInsnNode instruction) {
        return allocations.computeIfAbsent(instruction,
                node -> new UninitializedValue(Type.getObjectType(node.desc), node));
    }

    @Override public BasicValue newOperation(AbstractInsnNode instruction) throws AnalyzerException {
        if (instruction.getOpcode() == Opcodes.NEW) return allocation((TypeInsnNode) instruction);
        return super.newOperation(instruction);
    }

    private void initialized(AbstractInsnNode instruction, BasicValue value) throws AnalyzerException {
        if (value instanceof UninitializedValue)
            throw new AnalyzerException(instruction, "cannot use or escape an uninitialized object: " + value);
    }

    @Override public BasicValue unaryOperation(AbstractInsnNode instruction, BasicValue value) throws AnalyzerException {
        if ((instruction.getOpcode() == Opcodes.IFNULL || instruction.getOpcode() == Opcodes.IFNONNULL)
                && value instanceof UninitializedValue)
            return super.unaryOperation(instruction, newValue(value.getType()));
        initialized(instruction, value);
        return super.unaryOperation(instruction, value);
    }

    @Override public BasicValue binaryOperation(AbstractInsnNode instruction, BasicValue left, BasicValue right)
            throws AnalyzerException {
        if (instruction.getOpcode() == Opcodes.IF_ACMPEQ || instruction.getOpcode() == Opcodes.IF_ACMPNE)
            return super.binaryOperation(instruction,
                    left instanceof UninitializedValue ? newValue(left.getType()) : left,
                    right instanceof UninitializedValue ? newValue(right.getType()) : right);
        initialized(instruction, right);
        if (instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.PUTFIELD
                && left == uninitializedThis && field.owner.equals(owner.name)
                && owner.fields.stream().anyMatch(f -> f.name.equals(field.name) && f.desc.equals(field.desc))) {
            // The JVM permits writes to fields declared in this class before the super/this call.
            return super.binaryOperation(instruction, newValue(callerType), right);
        }
        initialized(instruction, left);
        return super.binaryOperation(instruction, left, right);
    }

    @Override public BasicValue ternaryOperation(AbstractInsnNode instruction, BasicValue first,
            BasicValue second, BasicValue third) throws AnalyzerException {
        initialized(instruction, first); initialized(instruction, second); initialized(instruction, third);
        if (instruction.getOpcode() == Opcodes.AASTORE && first.getType() != null
                && first.getType().getSort() == Type.ARRAY) {
            String descriptor = first.getType().getDescriptor();
            if (descriptor.charAt(1) != 'L' && descriptor.charAt(1) != '[')
                throw new AnalyzerException(instruction, "AASTORE requires a reference array");
        }
        return super.ternaryOperation(instruction, first, second, third);
    }

    @Override public void returnOperation(AbstractInsnNode instruction, BasicValue value, BasicValue expected)
            throws AnalyzerException {
        initialized(instruction, value);
        super.returnOperation(instruction, value, expected);
    }

    @Override public BasicValue naryOperation(AbstractInsnNode instruction, List<? extends BasicValue> values)
            throws AnalyzerException {
        if (instruction instanceof MethodInsnNode call && call.name.equals("<init>")) {
            if (call.getOpcode() != Opcodes.INVOKESPECIAL || call.itf || values.isEmpty()
                    || !(values.get(0) instanceof UninitializedValue receiver))
                throw new AnalyzerException(instruction, "constructor requires an uninitialized receiver and INVOKESPECIAL");
            for (int i = 1; i < values.size(); i++) initialized(instruction, values.get(i));
            if (receiver.allocation == null) {
                if (!call.owner.equals(owner.name) && !call.owner.equals(owner.superName))
                    throw new AnalyzerException(instruction, "uninitialized-this must invoke this class or its direct superclass constructor");
            } else {
                if (!call.owner.equals(receiver.getType().getInternalName()))
                    throw new AnalyzerException(instruction, "NEW must invoke the constructor of its exact allocated class");
                ClassNode target = linked.get(call.owner);
                if (target != null && !packageName(call.owner).equals(packageName(owner.name))) {
                    for (MethodNode method : target.methods)
                        if (method.name.equals("<init>") && method.desc.equals(call.desc)
                                && (method.access & Opcodes.ACC_PROTECTED) != 0)
                            throw new AnalyzerException(instruction, "cross-package protected constructor requires uninitialized-this, not a NEW receiver");
                }
            }
            List<BasicValue> initializedValues = new ArrayList<>(values);
            initializedValues.set(0, newValue(receiver.getType()));
            return super.naryOperation(instruction, initializedValues);
        }
        for (BasicValue value : values) initialized(instruction, value);
        BasicValue result = super.naryOperation(instruction, values);
        if (instruction instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESPECIAL
                && !values.isEmpty()) {
            Type receiver = values.get(0).getType();
            if (receiver == null || !isAssignableFrom(callerType, receiver))
                throw new AnalyzerException(instruction, "non-constructor special receiver must be assignable to the caller");
        }
        return result;
    }

    private String packageName(String name) {
        int slash = name.lastIndexOf('/');
        return slash < 0 ? "" : name.substring(0, slash);
    }

    @Override public BasicValue merge(BasicValue left, BasicValue right) {
        if (left instanceof UninitializedValue || right instanceof UninitializedValue)
            return left == right ? left : BasicValue.UNINITIALIZED_VALUE;
        return super.merge(left, right);
    }

    /** JVM verification treats an interface target as Object; runtime dispatch checks membership. */
    @Override protected boolean isSubTypeOf(BasicValue value, BasicValue expected) {
        if (value instanceof UninitializedValue || expected instanceof UninitializedValue) return value == expected;
        Type source = value.getType(), target = expected.getType();
        if (source == null || target == null) return value.equals(expected);
        if (!reference(target)) return source.equals(target);
        return reference(source) && (isAssignableFrom(target, source) || isInterface(target));
    }

    boolean frameAssignable(BasicValue value, BasicValue expected) {
        return expected.equals(BasicValue.UNINITIALIZED_VALUE) || isSubTypeOf(value, expected);
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
