package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.*;

/** Closed-world dispatch for public methods with class or maximally-specific interface implementations. */
public final class BytecodeDispatch {
    private final Map<String, ClassNode> linked;
    private final BytecodeObjectLayouts layouts;
    private final Map<String, Integer> classIds = new LinkedHashMap<>();
    private record Declaration(String owner, MethodNode method) {}

    public BytecodeDispatch(Map<String, ClassNode> linked) {
        this.linked = linked;
        this.layouts = new BytecodeObjectLayouts(linked);
        int id = 1;
        for (String name : new TreeSet<>(linked.keySet())) classIds.put(name, id++);
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
        if (target == null || (target.method.access & Opcodes.ACC_PUBLIC) == 0
                || (target.method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE)) != 0)
            throw new IllegalArgumentException("interface-super calls require a public concrete default implementation");
        return TypeHelper.getMethodFunctionName(target.owner, call.name, call.desc);
    }

    public Map<Integer, String> targets(MethodInsnNode call) {
        ClassNode owner = linked.get(call.owner);
        boolean interfaceCall = call.getOpcode() == Opcodes.INVOKEINTERFACE;
        if (owner == null || ((owner.access & Opcodes.ACC_INTERFACE) != 0) != interfaceCall)
            throw new IllegalArgumentException("link the declaring class/interface in the same module");
        Declaration declared = declaration(call.owner, call.name, call.desc, new HashSet<>());
        if (declared == null) declared = interfaceMethod(call.owner, call.name, call.desc);
        if (declared == null || (declared.method.access & Opcodes.ACC_PUBLIC) == 0
                || (declared.method.access & Opcodes.ACC_STATIC) != 0)
            throw new IllegalArgumentException("use public virtual/interface methods; non-public virtual calls are unavailable");
        Map<Integer, String> result = new LinkedHashMap<>();
        for (var entry : classIds.entrySet()) {
            ClassNode type = linked.get(entry.getKey());
            if ((type.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0
                    || !subtype(type.name, call.owner, new HashSet<>())) continue;
            Declaration implementation = declaration(type.name, call.name, call.desc, new HashSet<>());
            // Every class declaration takes precedence, even an abstract one.
            if (implementation == null) implementation = interfaceMethod(type.name, call.name, call.desc);
            if (!layouts.contains(type.name) || implementation == null
                    || (implementation.method.access & Opcodes.ACC_PUBLIC) == 0
                    || (implementation.method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE)) != 0)
                throw new IllegalArgumentException("link a complete hierarchy and public concrete class/default implementation for "
                        + type.name + "." + call.name + call.desc + "; implement abstract or re-abstracted methods");
            result.put(entry.getValue(), TypeHelper.getMethodFunctionName(implementation.owner, call.name, call.desc));
        }
        return result;
    }
}
