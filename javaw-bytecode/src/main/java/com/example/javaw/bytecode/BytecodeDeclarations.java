package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Set;

/** Structural declaration checks, independent of executable call sites. */
final class BytecodeDeclarations implements Opcodes {
    private static final int VISIBILITY = ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED;

    static void validate(ClassNode owner) {
        boolean iface = (owner.access & ACC_INTERFACE) != 0;
        if ((owner.access & (ACC_FINAL | ACC_ABSTRACT)) == (ACC_FINAL | ACC_ABSTRACT))
            fail(owner, "a class cannot be both final and abstract");
        if (iface && ((owner.access & ACC_ABSTRACT) == 0 || (owner.access & ACC_FINAL) != 0))
            fail(owner, "interfaces must be abstract and non-final");
        if ((owner.access & ACC_ANNOTATION) != 0 && !iface)
            fail(owner, "annotation types must be interfaces");
        if (owner.permittedSubclasses != null) {
            int version = owner.version & 0xffff;
            boolean preview = (owner.version & 0xffff0000) == V_PREVIEW;
            if (version != 0 && version < V17 && !(preview && (version == V15 || version == V16)))
                fail(owner, "PermittedSubclasses requires Java 17 or Java 15/16 preview");
            if ((owner.access & ACC_FINAL) != 0)
                fail(owner, "a final class cannot declare PermittedSubclasses");
        }
        Set<String> fields = new HashSet<>();
        for (FieldNode field : owner.fields) {
            String label = "field " + field.name + field.desc;
            visibility(owner, field.access, label);
            if (!fields.add(field.name + ":" + field.desc)) fail(owner, "duplicate " + label);
            if (!validDescriptor(field.desc, false)) fail(owner, "invalid descriptor for " + label);
            if ((field.access & (ACC_FINAL | ACC_VOLATILE)) == (ACC_FINAL | ACC_VOLATILE))
                fail(owner, "final volatile " + label);
            if (iface && ((field.access & (ACC_PUBLIC | ACC_STATIC | ACC_FINAL)) != (ACC_PUBLIC | ACC_STATIC | ACC_FINAL)
                    || (field.access & (ACC_PRIVATE | ACC_PROTECTED | ACC_VOLATILE | ACC_TRANSIENT)) != 0))
                fail(owner, "interface fields must be public static final: " + label);
        }
        Set<String> methods = new HashSet<>();
        for (MethodNode method : owner.methods) {
            String label = "method " + method.name + method.desc;
            visibility(owner, method.access, label);
            if (!methods.add(method.name + method.desc)) fail(owner, "duplicate " + label);
            if (!validDescriptor(method.desc, true)) fail(owner, "invalid descriptor for " + label);
            if ((method.access & ACC_ABSTRACT) != 0
                    && (method.access & (ACC_PRIVATE | ACC_STATIC | ACC_FINAL | ACC_SYNCHRONIZED | ACC_NATIVE | ACC_STRICT)) != 0)
                fail(owner, "incompatible abstract flags for " + label);
            if ("<init>".equals(method.name)) {
                if (iface || !method.desc.endsWith(")V")
                        || (method.access & (ACC_STATIC | ACC_FINAL | ACC_SYNCHRONIZED | ACC_NATIVE | ACC_ABSTRACT | ACC_BRIDGE)) != 0)
                    fail(owner, "invalid constructor declaration " + label);
            } else if ("<clinit>".equals(method.name)) {
                if (!"()V".equals(method.desc) || (method.access & ACC_STATIC) == 0)
                    fail(owner, "invalid class initializer declaration " + label);
            } else if (iface) {
                if ((method.access & (ACC_PROTECTED | ACC_FINAL | ACC_SYNCHRONIZED | ACC_NATIVE)) != 0
                        || (method.access & (ACC_PUBLIC | ACC_PRIVATE)) == 0)
                    fail(owner, "invalid interface method flags for " + label);
                int version = owner.version & 0xffff;
                if (version != 0 && version < V1_8 && ((method.access & (ACC_PUBLIC | ACC_ABSTRACT)) != (ACC_PUBLIC | ACC_ABSTRACT)
                        || (method.access & ACC_STATIC) != 0))
                    fail(owner, "pre-Java-8 interface methods must be public abstract: " + label);
                if (version != 0 && version < V9 && (method.access & ACC_PRIVATE) != 0)
                    fail(owner, "private interface methods require Java 9: " + label);
            }
        }
    }

    private static void visibility(ClassNode owner, int access, String label) {
        if (Integer.bitCount(access & VISIBILITY) > 1) fail(owner, "conflicting visibility flags for " + label);
    }

    // ASM Type is intentionally permissive; consume the entire JVMS descriptor grammar here.
    private static boolean validDescriptor(String descriptor, boolean method) {
        if (descriptor == null || descriptor.isEmpty()) return false;
        int position = 0;
        if (method) {
            if (descriptor.charAt(position++) != '(') return false;
            while (position < descriptor.length() && descriptor.charAt(position) != ')') {
                int end = valueEnd(descriptor, position);
                if (end < 0) return false;
                position = end;
            }
            if (position >= descriptor.length() || descriptor.charAt(position++) != ')') return false;
            if (position < descriptor.length() && descriptor.charAt(position) == 'V') return position + 1 == descriptor.length();
        }
        return valueEnd(descriptor, position) == descriptor.length();
    }

    private static int valueEnd(String descriptor, int position) {
        int dimensions = 0;
        while (position < descriptor.length() && descriptor.charAt(position) == '[') { position++; dimensions++; }
        if (dimensions > 255 || position >= descriptor.length()) return -1;
        char kind = descriptor.charAt(position++);
        if ("ZBCSIJFD".indexOf(kind) >= 0) return position;
        if (kind != 'L') return -1;
        int start = position;
        while (position < descriptor.length() && descriptor.charAt(position) != ';') {
            char c = descriptor.charAt(position);
            if (c == '.' || c == '[' || c == '(' || c == ')' || (c == '/' && (position == start || descriptor.charAt(position - 1) == '/'))) return -1;
            position++;
        }
        return position > start && position < descriptor.length() && descriptor.charAt(position - 1) != '/' ? position + 1 : -1;
    }

    private static void fail(ClassNode owner, String reason) {
        throw BytecodeCompatibility.failure(owner, null, null, "member declaration", reason);
    }
}
