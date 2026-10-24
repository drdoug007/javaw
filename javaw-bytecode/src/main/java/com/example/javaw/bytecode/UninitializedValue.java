package com.example.javaw.bytecode;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;

/** A verifier identity, not an initialized reference; null allocation denotes uninitialized-this. */
final class UninitializedValue extends BasicValue {
    private final Type initializedType;
    final TypeInsnNode allocation;

    UninitializedValue(Type type, TypeInsnNode allocation) {
        // No admitted BasicValue has a method type. Keep BasicValue.equals from confusing this
        // identity with an ordinary reference or TOP, while exposing the reference type to lowering.
        super(Type.getMethodType("()V"));
        initializedType = type;
        this.allocation = allocation;
    }

    @Override public Type getType() { return initializedType; }
    @Override public boolean isReference() { return true; }
    @Override public boolean equals(Object other) { return this == other; }
    @Override public int hashCode() { return System.identityHashCode(this); }
    @Override public String toString() {
        return allocation == null ? "uninitialized-this" : "uninitialized NEW " + initializedType;
    }
}
