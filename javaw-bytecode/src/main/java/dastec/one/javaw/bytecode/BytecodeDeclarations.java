package dastec.one.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Set;

/** Structural declaration checks, independent of executable call sites. */
final class BytecodeDeclarations implements Opcodes {
    private record MemberKey(String name, String descriptor) {}
    private static final int VISIBILITY = ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED;

    static void validate(ClassNode owner) {
        if (!validClassName(owner.name)) fail(owner, "invalid internal class name " + owner.name);
        if ((owner.access & ACC_MODULE) != 0 || owner.module != null)
            fail(owner, "module declarations are not executable classes; use the unnamed closed-world target");
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
        if (iface && (owner.access & (ACC_SUPER | ACC_ENUM)) != 0)
            fail(owner, "interfaces cannot carry ACC_SUPER or ACC_ENUM");
        if (owner.permittedSubclasses != null)
            for (String name : owner.permittedSubclasses)
                if (!validClassName(name)) fail(owner, "invalid permitted subclass name " + name);
        if (owner.nestHostClass != null && owner.nestMembers != null)
            fail(owner, "a class cannot declare both NestHost and NestMembers");
        int major = owner.version & 0xffff;
        if (major != 0 && major < V11 && (owner.nestHostClass != null || owner.nestMembers != null))
            fail(owner, "nest attributes require Java 11");
        if (owner.nestHostClass != null && !validClassName(owner.nestHostClass))
            fail(owner, "invalid nest host name");
        if (owner.nestMembers != null) for (String name : owner.nestMembers)
            if (!validClassName(name)) fail(owner, "invalid nest member name " + name);
        Set<MemberKey> fields = new HashSet<>();
        for (FieldNode field : owner.fields) {
            String label = "field " + field.name + field.desc;
            if (!validMemberName(field.name, false)) fail(owner, "invalid name for " + label);
            visibility(owner, field.access, label);
            if (!fields.add(new MemberKey(field.name, field.desc))) fail(owner, "duplicate " + label);
            if (!validDescriptor(field.desc, false)) fail(owner, "invalid descriptor for " + label);
            if (field.value != null && !validConstantValue(field.desc, field.value))
                fail(owner, "ConstantValue type does not match " + label);
            if ((field.access & (ACC_FINAL | ACC_VOLATILE)) == (ACC_FINAL | ACC_VOLATILE))
                fail(owner, "final volatile " + label);
            if (iface && ((field.access & (ACC_PUBLIC | ACC_STATIC | ACC_FINAL)) != (ACC_PUBLIC | ACC_STATIC | ACC_FINAL)
                    || (field.access & (ACC_PRIVATE | ACC_PROTECTED | ACC_VOLATILE | ACC_TRANSIENT)) != 0))
                fail(owner, "interface fields must be public static final: " + label);
        }
        Set<MemberKey> methods = new HashSet<>();
        for (MethodNode method : owner.methods) {
            String label = "method " + method.name + method.desc;
            if (!validMemberName(method.name, true)) fail(owner, "invalid name for " + label);
            if (!method.name.equals("<clinit>")) visibility(owner, method.access, label);
            if (!methods.add(new MemberKey(method.name, method.desc))) fail(owner, "duplicate " + label);
            if (!validDescriptor(method.desc, true)) fail(owner, "invalid descriptor for " + label);
            int parameterSlots = (method.access & ACC_STATIC) == 0 ? 1 : 0;
            for (Type parameter : Type.getArgumentTypes(method.desc))
                parameterSlots += parameter.getSize();
            if (parameterSlots > 255) fail(owner, "method parameter length exceeds 255 slots: " + label);
            if (!method.name.equals("<clinit>") && (method.access & ACC_ABSTRACT) != 0
                    && (method.access & (ACC_PRIVATE | ACC_STATIC | ACC_FINAL | ACC_SYNCHRONIZED | ACC_NATIVE
                        | (strictFlagApplies(owner) ? ACC_STRICT : 0))) != 0)
                fail(owner, "incompatible abstract flags for " + label);
            if ("<init>".equals(method.name)) {
                if (iface || !method.desc.endsWith(")V")
                        || (method.access & (ACC_STATIC | ACC_FINAL | ACC_SYNCHRONIZED | ACC_NATIVE | ACC_ABSTRACT | ACC_BRIDGE)) != 0)
                    fail(owner, "invalid constructor declaration " + label);
            } else if ("<clinit>".equals(method.name)) {
                if (!method.desc.endsWith(")V") || ((major == 0 || major >= V1_7)
                        && (!"()V".equals(method.desc) || (method.access & ACC_STATIC) == 0)))
                    fail(owner, "invalid class initializer declaration " + label);
            } else if (iface) {
                if ((method.access & (ACC_PROTECTED | ACC_FINAL | ACC_SYNCHRONIZED | ACC_NATIVE)) != 0
                        || (method.access & (ACC_PUBLIC | ACC_PRIVATE)) == 0)
                    fail(owner, "invalid interface method flags for " + label);
                int version = owner.version & 0xffff;
                if (version != 0 && version < V1_8 && ((method.access & (ACC_PUBLIC | ACC_ABSTRACT)) != (ACC_PUBLIC | ACC_ABSTRACT)
                        || (method.access & ACC_STATIC) != 0))
                    fail(owner, "pre-Java-8 interface methods must be public abstract: " + label);
                // Class-file version 52 permits private concrete interface methods, including
                // javac 8 lambda helpers. Java source did not expose private methods until Java 9.
            }
        }
        for (MethodNode method : owner.methods) {
            boolean noCode = !method.name.equals("<clinit>") && (method.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0;
            if (noCode && (method.instructions.size() != 0 || !method.tryCatchBlocks.isEmpty()))
                fail(owner, "abstract/native method cannot declare Code: " + method.name + method.desc);
            if (!noCode && java.util.Arrays.stream(method.instructions.toArray()).noneMatch(insn -> insn.getOpcode() >= 0))
                fail(owner, "concrete method requires a Code body: " + method.name + method.desc);
        }
    }

    private static boolean strictFlagApplies(ClassNode owner) {
        int major = owner.version & 0xffff;
        return major == 0 || major >= V1_2 && major <= V16;
    }

    static boolean validClassName(String name) {
        if (name == null || name.isEmpty()) return false;
        for (String component : name.split("/", -1))
            if (!validMemberName(component, false)) return false;
        return true;
    }

    private static boolean validConstantValue(String descriptor, Object value) {
        return switch (descriptor) {
            case "Z", "B", "C", "S", "I" -> value instanceof Integer;
            case "J" -> value instanceof Long;
            case "F" -> value instanceof Float;
            case "D" -> value instanceof Double;
            case "Ljava/lang/String;" -> value instanceof String;
            default -> false;
        };
    }

    static boolean validMemberName(String name, boolean method) {
        if (name == null || name.isEmpty()) return false;
        if (method && (name.equals("<init>") || name.equals("<clinit>"))) return true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (".;[/".indexOf(c) >= 0 || method && (c == '<' || c == '>')) return false;
        }
        return true;
    }

    private static void visibility(ClassNode owner, int access, String label) {
        if (Integer.bitCount(access & VISIBILITY) > 1) fail(owner, "conflicting visibility flags for " + label);
    }

    // ASM Type is intentionally permissive; consume the entire JVMS descriptor grammar here.
    static boolean validDescriptor(String descriptor, boolean method) {
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
            if (c == '.' || c == '[' || (c == '/' && (position == start || descriptor.charAt(position - 1) == '/'))) return -1;
            position++;
        }
        return position > start && position < descriptor.length() && descriptor.charAt(position - 1) != '/' ? position + 1 : -1;
    }

    private static void fail(ClassNode owner, String reason) {
        throw BytecodeCompatibility.failure(owner, null, null, "member declaration", reason);
    }
}
