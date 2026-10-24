package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.*;

/** Closed-world virtual dispatch with method access, override slots, and validated private nests. */
public final class BytecodeDispatch {
    private final Map<String, ClassNode> linked;
    private final String caller;
    private final BytecodeObjectLayouts layouts;
    private final Map<String, Integer> classIds = new LinkedHashMap<>();
    private record Declaration(String owner, MethodNode method) {}

    public BytecodeDispatch(Map<String, ClassNode> linked) {
        this(linked, null);
    }

    public BytecodeDispatch(Map<String, ClassNode> linked, String caller) {
        this.caller = caller;
        this.linked = linked;
        this.layouts = new BytecodeObjectLayouts(linked);
        int id = 1;
        for (String name : new TreeSet<>(linked.keySet())) classIds.put(name, id++);
    }

    private String packageName(String name) {
        int slash = name.lastIndexOf('/');
        return slash < 0 ? "" : name.substring(0, slash);
    }

    /** JVM class accessibility uses public/package access on the symbolic class name. */
    void checkClassAccess(String name) {
        ClassNode type = linked.get(name);
        if (type == null) throw new IllegalArgumentException("link the symbolic class/interface " + name);
        if ((type.access & Opcodes.ACC_PUBLIC) == 0 && caller != null
                && !packageName(name).equals(packageName(caller)))
            throw new IllegalArgumentException("inaccessible symbolic class/interface " + name);
    }

    private boolean nestMember(ClassNode host, ClassNode member) {
        if (!packageName(host.name).equals(packageName(member.name))) return false;
        if (member.name.equals(host.name)) return member.nestHostClass == null;
        return host.name.equals(member.nestHostClass) && host.nestMembers != null
                && host.nestMembers.contains(member.name);
    }

    void checkPrivateAccess(String declaringOwner) {
        if (declaringOwner.equals(caller)) return;
        ClassNode from = linked.get(caller), target = linked.get(declaringOwner);
        if (from != null && target != null) {
            String hostName = from.nestHostClass == null ? from.name : from.nestHostClass;
            ClassNode host = linked.get(hostName);
            if (host != null && host.nestHostClass == null && nestMember(host, from) && nestMember(host, target)) return;
        }
        throw new IllegalArgumentException("private member access requires the declaring class or a validated linked nest; "
                + "link the nest host and reciprocal same-package NestHost/NestMembers metadata");
    }

    public int classId(String name) {
        Integer id = classIds.get(name);
        if (id == null) throw new IllegalArgumentException("Unlinked class " + name);
        return id;
    }

    /** Concrete runtime IDs assignable to a linked class/interface type-test target. */
    public Set<Integer> typeTestIds(String target) {
        if (!target.equals("java/lang/Object")) {
            ClassNode type = linked.get(target);
            if (type == null || target.startsWith("["))
                throw new IllegalArgumentException("link the type-test class/interface; array type tests are unsupported");
            checkClassAccess(target);
        }
        Set<Integer> result = new LinkedHashSet<>();
        for (var entry : classIds.entrySet()) {
            ClassNode type = linked.get(entry.getKey());
            validateTypeHierarchy(type.name, new HashSet<>(), new HashSet<>());
            if ((type.access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT)) == 0
                    && layouts.contains(type.name) && subtype(type.name, target, new HashSet<>()))
                result.add(entry.getValue());
        }
        return result;
    }

    private void validateTypeHierarchy(String name, Set<String> visiting, Set<String> complete) {
        if ("java/lang/Object".equals(name) || complete.contains(name)) return;
        ClassNode type = linked.get(name);
        if (type == null || !visiting.add(name))
            throw new IllegalArgumentException("link a complete acyclic type hierarchy: " + name);
        if (type.superName != null) validateTypeHierarchy(type.superName, visiting, complete);
        for (String iface : type.interfaces) validateTypeHierarchy(iface, visiting, complete);
        visiting.remove(name);
        complete.add(name);
    }

    private record FieldDeclaration(String owner, FieldNode field) {}

    private FieldDeclaration resolveField(String name, String field, String descriptor, Set<String> visiting) {
        if ("java/lang/Object".equals(name) || name == null) return null;
        ClassNode type = linked.get(name);
        if (type == null || !visiting.add(name))
            throw new IllegalArgumentException("link a complete acyclic field hierarchy: " + name);
        for (FieldNode candidate : type.fields)
            if (candidate.name.equals(field) && candidate.desc.equals(descriptor))
                return new FieldDeclaration(name, candidate);
        // JVM field resolution searches direct superinterfaces before the superclass.
        for (String iface : type.interfaces) {
            FieldDeclaration found = resolveField(iface, field, descriptor, visiting);
            if (found != null) return found;
        }
        FieldDeclaration found = resolveField(type.superName, field, descriptor, visiting);
        visiting.remove(name);
        return found;
    }

    private FieldDeclaration fieldDeclaration(FieldInsnNode access) {
        ClassNode symbolic = linked.get(access.owner);
        if (symbolic == null) throw new IllegalArgumentException("link the field owner " + access.owner);
        checkClassAccess(access.owner);
        FieldDeclaration resolved = resolveField(access.owner, access.name, access.desc, new HashSet<>());
        if (resolved == null) throw new IllegalArgumentException("link a matching field declaration");
        boolean isStatic = access.getOpcode() == Opcodes.GETSTATIC || access.getOpcode() == Opcodes.PUTSTATIC;
        int flags = resolved.field.access;
        if (((flags & Opcodes.ACC_STATIC) != 0) != isStatic)
            throw new IllegalArgumentException("field opcode does not match static/instance declaration");
        if ((flags & Opcodes.ACC_PUBLIC) == 0) {
            if ((flags & Opcodes.ACC_PRIVATE) != 0) checkPrivateAccess(resolved.owner);
            else if (!packageName(resolved.owner).equals(packageName(caller))
                    && ((flags & Opcodes.ACC_PROTECTED) == 0 || !subtype(caller, resolved.owner, new HashSet<>())))
                throw new IllegalArgumentException("inaccessible protected/package field " + resolved.owner);
        }
        return resolved;
    }

    public String fieldOwner(FieldInsnNode access) {
        return fieldDeclaration(access).owner;
    }

    void validateField(FieldInsnNode access, MethodNode method) {
        FieldDeclaration resolved = fieldDeclaration(access);
        boolean write = access.getOpcode() == Opcodes.PUTFIELD || access.getOpcode() == Opcodes.PUTSTATIC;
        if (write && (resolved.field.access & Opcodes.ACC_FINAL) != 0
                && (!resolved.owner.equals(caller) || !method.name.equals(
                    access.getOpcode() == Opcodes.PUTFIELD ? "<init>" : "<clinit>")))
            throw new IllegalArgumentException("final field writes require the declaring class initializer/constructor");
    }

    /** Null means no extra receiver restriction; otherwise the permitted runtime class IDs. */
    public Set<Integer> fieldReceiverIds(FieldInsnNode access) {
        FieldDeclaration resolved = fieldDeclaration(access);
        if ((resolved.field.access & Opcodes.ACC_PROTECTED) == 0
                || packageName(resolved.owner).equals(packageName(caller))) return null;
        Set<Integer> result = new LinkedHashSet<>();
        for (var entry : classIds.entrySet())
            if (subtype(entry.getKey(), caller, new HashSet<>())) result.add(entry.getValue());
        return result;
    }

    private Declaration declaration(String name, String method, String descriptor, Set<String> visited) {
        if (!visited.add(name)) return null;
        ClassNode type = linked.get(name);
        if (type == null) return null;
        for (MethodNode candidate : type.methods)
            if (candidate.name.equals(method) && candidate.desc.equals(descriptor))
                return new Declaration(name, candidate);
        return declaration(type.superName, method, descriptor, visited);
    }

    private boolean subtype(String name, String target, Set<String> visited) {
        if (Objects.equals(name, target)) return true;
        if (!visited.add(name)) return false;
        ClassNode type = linked.get(name);
        if (type == null) return false;
        if (subtype(type.superName, target, visited)) return true;
        for (String iface : type.interfaces) if (subtype(iface, target, visited)) return true;
        return false;
    }

    private void interfaceDeclarations(String name, String method, String descriptor,
                                       Set<String> visited, List<Declaration> result) {
        if (name == null || name.equals("java/lang/Object") || !visited.add(name)) return;
        ClassNode type = linked.get(name);
        if (type == null) throw new IllegalArgumentException("link the complete interface hierarchy: " + name);
        if ((type.access & Opcodes.ACC_INTERFACE) != 0) {
            for (MethodNode candidate : type.methods)
                if (candidate.name.equals(method) && candidate.desc.equals(descriptor)
                        && (candidate.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) == 0)
                    result.add(new Declaration(name, candidate));
        }
        interfaceDeclarations(type.superName, method, descriptor, visited, result);
        for (String iface : type.interfaces) interfaceDeclarations(iface, method, descriptor, visited, result);
    }

    private Declaration interfaceMethod(String receiver, String method, String descriptor) {
        List<Declaration> declarations = new ArrayList<>();
        interfaceDeclarations(receiver, method, descriptor, new HashSet<>(), declarations);
        // An abstract redeclaration also suppresses an ancestor default (re-abstraction).
        List<Declaration> maximal = declarations.stream().filter(candidate -> declarations.stream().noneMatch(other ->
                !candidate.owner.equals(other.owner) && subtype(other.owner, candidate.owner, new HashSet<>())))
                .toList();
        List<Declaration> defaults = maximal.stream()
                .filter(candidate -> (candidate.method.access & Opcodes.ACC_ABSTRACT) == 0).toList();
        if (defaults.size() > 1)
            throw new IllegalArgumentException("conflicting maximally-specific interface defaults for " + receiver
                    + "." + method + descriptor + "; provide a public class override");
        if (defaults.size() == 1) return defaults.get(0);
        return maximal.isEmpty() ? null : maximal.get(0);
    }

    /** Resolve INVOKESTATIC without inheriting interface static methods. */
    public String staticTarget(MethodInsnNode call) {
        ClassNode symbolic = linked.get(call.owner);
        if (symbolic == null || ((symbolic.access & Opcodes.ACC_INTERFACE) != 0) != call.itf)
            throw new IllegalArgumentException("link the static owner with the matching class/interface reference kind");
        checkClassAccess(call.owner);
        if (call.name.startsWith("<"))
            throw new IllegalArgumentException("initializers cannot be invoked with INVOKESTATIC");
        Declaration resolved = null;
        Set<String> visited = new HashSet<>();
        String name = call.owner;
        while (name != null && !name.equals("java/lang/Object")) {
            ClassNode type = linked.get(name);
            if (type == null || !visited.add(name))
                throw new IllegalArgumentException("link a complete acyclic static method superclass chain: " + name);
            resolved = ownMethod(name, call.name, call.desc);
            if (resolved != null || call.itf) break;
            name = type.superName;
        }
        if (resolved == null)
            throw new IllegalArgumentException("link a matching static declaration; interface static methods are not inherited");
        if ((resolved.method.access & Opcodes.ACC_STATIC) == 0
                || (resolved.method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0)
            throw new IllegalArgumentException("static call requires a concrete non-native static declaration");
        checkMemberAccess(resolved); // Member visibility is identical; static calls have no receiver restriction.
        return TypeHelper.getMethodFunctionName(resolved.owner, call.name, call.desc);
    }

    private Declaration classSpecialDeclaration(String name, String method, String descriptor) {
        Set<String> visited = new HashSet<>();
        String current = name;
        while (current != null && !current.equals("java/lang/Object")) {
            ClassNode type = linked.get(current);
            if (type == null || !visited.add(current))
                throw new IllegalArgumentException("link a complete acyclic special method superclass chain: " + current);
            Declaration found = ownMethod(current, method, descriptor);
            if (found != null) return found;
            current = type.superName;
        }
        return interfaceMethod(name, method, descriptor);
    }

    /** Class INVOKESPECIAL resolution, including exact constructors and ACC_SUPER selection. */
    public String specialTarget(MethodInsnNode call) {
        ClassNode owner = linked.get(call.owner), from = linked.get(caller);
        if (call.itf || owner == null || (owner.access & Opcodes.ACC_INTERFACE) != 0 || from == null)
            throw new IllegalArgumentException("link the special-call class owner and caller with a class reference");
        checkClassAccess(call.owner);
        boolean constructor = call.name.equals("<init>");
        if (call.name.startsWith("<") && !constructor)
            throw new IllegalArgumentException("class initializers cannot be invoked with INVOKESPECIAL");
        Declaration resolved = constructor ? ownMethod(call.owner, call.name, call.desc)
                : classSpecialDeclaration(call.owner, call.name, call.desc);
        if (resolved == null)
            throw new IllegalArgumentException("link a matching special declaration; constructors are not inherited");
        checkMemberAccess(resolved);
        if ((resolved.method.access & Opcodes.ACC_STATIC) != 0)
            throw new IllegalArgumentException("special call cannot resolve to a static declaration");
        if (!constructor) {
            boolean superclass = !caller.equals(call.owner) && subtype(from.superName, call.owner, new HashSet<>());
            if (!caller.equals(call.owner) && !superclass)
                throw new IllegalArgumentException("non-constructor class special calls require the caller or a superclass owner");
            if (superclass && ((from.access & Opcodes.ACC_SUPER) != 0 || from.version >= Opcodes.V1_8))
                resolved = classSpecialDeclaration(from.superName, call.name, call.desc);
        }
        if (resolved == null || (resolved.method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0)
            throw new IllegalArgumentException("special call requires a concrete non-native instance implementation");
        return TypeHelper.getMethodFunctionName(resolved.owner, call.name, call.desc);
    }

    public String interfaceSuperTarget(MethodInsnNode call) {
        ClassNode owner = linked.get(call.owner), from = linked.get(caller);
        if (!call.itf || owner == null || (owner.access & Opcodes.ACC_INTERFACE) == 0 || from == null)
            throw new IllegalArgumentException("link the interface-special owner and caller with an interface reference");
        checkClassAccess(call.owner);
        if (call.name.startsWith("<"))
            throw new IllegalArgumentException("interface special calls cannot name initializers");
        if (!caller.equals(call.owner) && !from.interfaces.contains(call.owner))
            throw new IllegalArgumentException("interface-special owner must be the caller interface or a direct superinterface");
        validateTypeHierarchy(call.owner, new HashSet<>(), new HashSet<>());
        Declaration target = ownMethod(call.owner, call.name, call.desc);
        if (target == null) target = interfaceMethod(call.owner, call.name, call.desc);
        if (target != null && (target.method.access & Opcodes.ACC_PRIVATE) != 0) {
            checkPrivateAccess(target.owner);
        }
        if (target == null || (target.method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE)) == 0
                || (target.method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE)) != 0)
            throw new IllegalArgumentException("interface-special calls require an accessible concrete default/private implementation");
        return TypeHelper.getMethodFunctionName(target.owner, call.name, call.desc);
    }

    private boolean crossPackageProtected(Declaration method) {
        return (method.method.access & Opcodes.ACC_PROTECTED) != 0 && caller != null
                && !packageName(method.owner).equals(packageName(caller));
    }

    private void checkMemberAccess(Declaration method) {
        if ((method.method.access & Opcodes.ACC_PUBLIC) != 0) return;
        if ((method.method.access & Opcodes.ACC_PRIVATE) != 0) {
            checkPrivateAccess(method.owner);
            return;
        }
        if (caller != null && packageName(caller).equals(packageName(method.owner))) return;
        if (crossPackageProtected(method) && subtype(caller, method.owner, new HashSet<>())) return;
        throw new IllegalArgumentException("inaccessible protected/package method " + method.owner
                + "; call from the declaring package or an eligible protected subclass");
    }

    private Declaration ownMethod(String name, String method, String descriptor) {
        ClassNode type = linked.get(name);
        if (type == null) return null;
        for (MethodNode candidate : type.methods)
            if (candidate.name.equals(method) && candidate.desc.equals(descriptor)) return new Declaration(name, candidate);
        return null;
    }

    private Declaration virtualImplementation(String receiver, Declaration resolved) {
        if ((linked.get(resolved.owner).access & Opcodes.ACC_INTERFACE) != 0) {
            Declaration implementation = declaration(receiver, resolved.method.name, resolved.method.desc, new HashSet<>());
            return implementation == null ? interfaceMethod(receiver, resolved.method.name, resolved.method.desc) : implementation;
        }
        List<String> path = new ArrayList<>();
        String current = receiver;
        while (!current.equals(resolved.owner)) {
            ClassNode type = linked.get(current);
            if (type == null || path.contains(current))
                throw new IllegalArgumentException("link the complete receiver superclass chain through " + resolved.owner);
            path.add(current);
            current = type.superName;
            if (current == null) throw new IllegalArgumentException("receiver does not extend " + resolved.owner);
        }
        Declaration selected = resolved;
        Collections.reverse(path);
        for (String name : path) {
            Declaration candidate = ownMethod(name, resolved.method.name, resolved.method.desc);
            if (candidate == null || (candidate.method.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) != 0) continue;
            // A different-package same-named method does not override a package-private slot.
            // Once a same-package override widens access, subsequent overrides are transitive.
            if ((selected.method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0
                    || packageName(selected.owner).equals(packageName(candidate.owner))) {
                int access = candidate.method.access;
                if ((selected.method.access & Opcodes.ACC_PUBLIC) != 0 && (access & Opcodes.ACC_PUBLIC) == 0
                        || (selected.method.access & Opcodes.ACC_PROTECTED) != 0
                        && (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0)
                    throw new IllegalArgumentException("override reduces virtual method visibility in " + candidate.owner);
                selected = candidate;
            }
        }
        return selected;
    }

    public Map<Integer, String> targets(MethodInsnNode call) {
        ClassNode owner = linked.get(call.owner);
        boolean interfaceCall = call.getOpcode() == Opcodes.INVOKEINTERFACE;
        if (owner == null || ((owner.access & Opcodes.ACC_INTERFACE) != 0) != interfaceCall || call.itf != interfaceCall)
            throw new IllegalArgumentException("link the declaring class/interface with the matching invocation/reference kind");
        checkClassAccess(call.owner);
        if (call.name.startsWith("<"))
            throw new IllegalArgumentException("initializers cannot be invoked with virtual/interface instructions");
        Declaration declared = declaration(call.owner, call.name, call.desc, new HashSet<>());
        if (declared == null) declared = interfaceMethod(call.owner, call.name, call.desc);
        if (declared == null || (declared.method.access & Opcodes.ACC_STATIC) != 0)
            throw new IllegalArgumentException("link a matching instance method in the declaring hierarchy");
        checkMemberAccess(declared);
        boolean privateCall = (declared.method.access & Opcodes.ACC_PRIVATE) != 0;
        Map<Integer, String> result = new LinkedHashMap<>();
        for (var entry : classIds.entrySet()) {
            ClassNode type = linked.get(entry.getKey());
            if ((type.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0
                    || !subtype(type.name, call.owner, new HashSet<>())) continue;
            // The JVM protected receiver rule applies outside the declaring package.
            if (crossPackageProtected(declared) && !subtype(type.name, caller, new HashSet<>())) continue;
            // Private methods never participate in overriding, even on a subclass receiver.
            Declaration implementation = privateCall ? declared : virtualImplementation(type.name, declared);
            if (!layouts.contains(type.name) || implementation == null
                    || (!privateCall && (declared.method.access & Opcodes.ACC_PUBLIC) != 0
                        && (implementation.method.access & Opcodes.ACC_PUBLIC) == 0)
                    || (implementation.method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE)) != 0)
                throw new IllegalArgumentException("link a complete hierarchy and accessible concrete class/default implementation for "
                        + type.name + "." + call.name + call.desc + "; implement abstract or re-abstracted methods");
            result.put(entry.getValue(), TypeHelper.getMethodFunctionName(implementation.owner, call.name, call.desc));
        }
        return result;
    }
}
