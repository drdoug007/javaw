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
        throw new IllegalArgumentException("private method access requires the declaring class or a validated linked nest; "
                + "link the nest host and reciprocal same-package NestHost/NestMembers metadata");
    }

    public int classId(String name) {
        Integer id = classIds.get(name);
        if (id == null) throw new IllegalArgumentException("Unlinked class " + name);
        return id;
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

    public String interfaceSuperTarget(MethodInsnNode call) {
        ClassNode owner = linked.get(call.owner);
        if (owner == null || (owner.access & Opcodes.ACC_INTERFACE) == 0)
            throw new IllegalArgumentException("link the interface-super owner and its complete hierarchy");
        Declaration target = declaration(call.owner, call.name, call.desc, new HashSet<>());
        if (target == null) target = interfaceMethod(call.owner, call.name, call.desc);
        if (target != null && (target.method.access & Opcodes.ACC_PRIVATE) != 0) checkPrivateAccess(target.owner);
        if (target == null || (target.method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE)) == 0
                || (target.method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE)) != 0)
            throw new IllegalArgumentException("interface-special calls require an accessible concrete default/private implementation");
        return TypeHelper.getMethodFunctionName(target.owner, call.name, call.desc);
    }

    private boolean crossPackageProtected(Declaration method) {
        return (method.method.access & Opcodes.ACC_PROTECTED) != 0 && caller != null
                && !packageName(method.owner).equals(packageName(caller));
    }

    private void checkVirtualAccess(Declaration method) {
        if ((method.method.access & Opcodes.ACC_PUBLIC) != 0) return;
        if ((method.method.access & Opcodes.ACC_PRIVATE) != 0) {
            checkPrivateAccess(method.owner);
            return;
        }
        if (caller != null && packageName(caller).equals(packageName(method.owner))) return;
        if (crossPackageProtected(method) && subtype(caller, method.owner, new HashSet<>())) return;
        throw new IllegalArgumentException("inaccessible protected/package virtual method " + method.owner
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
        if (owner == null || ((owner.access & Opcodes.ACC_INTERFACE) != 0) != interfaceCall)
            throw new IllegalArgumentException("link the declaring class/interface in the same module");
        Declaration declared = declaration(call.owner, call.name, call.desc, new HashSet<>());
        if (declared == null) declared = interfaceMethod(call.owner, call.name, call.desc);
        if (declared == null || (declared.method.access & Opcodes.ACC_STATIC) != 0)
            throw new IllegalArgumentException("link a matching instance method in the declaring hierarchy");
        checkVirtualAccess(declared);
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
