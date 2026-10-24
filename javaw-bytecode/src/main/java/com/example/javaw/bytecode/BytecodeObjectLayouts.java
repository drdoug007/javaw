package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.*;

/** Shared parent-prefix layouts, independent of JAR entry order. */
final class BytecodeObjectLayouts {
    private record FieldKey(String name, String descriptor) {}
    private record Layout(int size, Map<FieldKey, Integer> fields) {}
    private final Map<String, Layout> layouts = new HashMap<>();
    private final Map<String, ClassNode> linked;

    BytecodeObjectLayouts(Map<String, ClassNode> linked) {
        this.linked = linked;
        if (BytecodeExceptionRuntime.enabled(linked)) layouts.put("java/lang/Object", new Layout(4, Map.of()));
        for (String name : linked.keySet()) build(name, new HashSet<>());
    }

    private Layout build(String name, Set<String> visiting) {
        if (layouts.containsKey(name)) return layouts.get(name);
        if (!visiting.add(name)) return null;
        ClassNode type = linked.get(name);
        if (type == null || (type.access & Opcodes.ACC_INTERFACE) != 0) return null;
        Layout parent = "java/lang/Object".equals(type.superName)
                ? new Layout(4, Map.of()) : build(type.superName, visiting);
        if (parent == null) return null;
        int size = parent.size();
        Map<FieldKey, Integer> fields = new LinkedHashMap<>(parent.fields());
        for (FieldNode field : type.fields) {
            FieldKey signature = new FieldKey(field.name, field.desc);
            if ((field.access & Opcodes.ACC_STATIC) == 0) {
                fields.put(signature, size);
                size += (field.desc.equals("J") || field.desc.equals("D")) ? 8 : 4;
            } else {
                // A declared static field must not resolve to a hidden parent instance field.
                fields.remove(signature);
            }
        }
        Layout result = new Layout(size, fields);
        layouts.put(name, result);
        visiting.remove(name);
        return result;
    }

    boolean contains(String name) { return layouts.containsKey(name); }

    Map<String, Integer> sizes() {
        Map<String, Integer> result = new LinkedHashMap<>();
        layouts.forEach((name, layout) -> result.put(name, layout.size()));
        return result;
    }

    Map<String, Integer> offsets() {
        Map<String, Integer> result = new LinkedHashMap<>();
        layouts.forEach((name, layout) -> layout.fields().forEach((field, offset) ->
                result.put(TypeHelper.getGlobalFieldName(name, field.name, field.descriptor), offset)));
        return result;
    }
}
