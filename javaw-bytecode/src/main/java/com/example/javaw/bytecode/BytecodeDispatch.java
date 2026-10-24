package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.*;

/** Closed-world dispatch for public methods with class-provided implementations. */
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

    private Declaration declaration(String name, String method, String descriptor, Set<String> visited,
                                    boolean interfaces) {
        if (!visited.add(name)) return null;
        ClassNode type = linked.get(name);
        if (type == null) return null;
        for (MethodNode candidate : type.methods)
            if (candidate.name.equals(method) && candidate.desc.equals(descriptor))
                return new Declaration(name, candidate);
        Declaration parent = declaration(type.superName, method, descriptor, visited, interfaces);
        if (parent != null) return parent;
        if (interfaces) for (String iface : type.interfaces) {
            Declaration result = declaration(iface, method, descriptor, visited, true);
            if (result != null) return result;
        }
        return null;
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

    public Map<Integer, String> targets(MethodInsnNode call) {
        ClassNode owner = linked.get(call.owner);
        boolean interfaceCall = call.getOpcode() == Opcodes.INVOKEINTERFACE;
        if (owner == null || ((owner.access & Opcodes.ACC_INTERFACE) != 0) != interfaceCall)
            throw new IllegalArgumentException("link the declaring class/interface in the same module");
        Declaration declared = declaration(call.owner, call.name, call.desc, new HashSet<>(), interfaceCall);
        if (declared == null || (declared.method.access & Opcodes.ACC_PUBLIC) == 0
                || (declared.method.access & Opcodes.ACC_STATIC) != 0
                || (interfaceCall && (declared.method.access & Opcodes.ACC_ABSTRACT) == 0))
            throw new IllegalArgumentException("use public virtual methods or abstract interface declarations; default methods are unavailable");
        Map<Integer, String> result = new LinkedHashMap<>();
        for (var entry : classIds.entrySet()) {
            ClassNode type = linked.get(entry.getKey());
            if ((type.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0
                    || !subtype(type.name, call.owner, new HashSet<>())) continue;
            Declaration implementation = declaration(type.name, call.name, call.desc, new HashSet<>(), false);
            if (!layouts.contains(type.name) || implementation == null
                    || (implementation.method.access & Opcodes.ACC_PUBLIC) == 0
                    || (implementation.method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE)) != 0)
                throw new IllegalArgumentException("link a complete hierarchy and public class implementation for " + type.name
                        + "." + call.name + call.desc + "; interface defaults are unavailable");
            result.put(entry.getValue(), TypeHelper.getMethodFunctionName(implementation.owner, call.name, call.desc));
        }
        return result;
    }
}
