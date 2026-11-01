package one.dastec.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.*;

/** Shared parent-prefix layouts, independent of JAR entry order. */
final class BytecodeObjectLayouts {
    private record FieldKey(String name, String descriptor) {}
    /** {@code fields} resolves names, so hiding replaces entries; {@code references} keeps every reference slot. */
    private record Layout(int size, Map<FieldKey, Integer> fields, List<Integer> references) {}
    private final Map<String, Layout> layouts = new HashMap<>();
    private final Map<String, ClassNode> linked;
    private Map<String, Integer> sizes, offsets;

    /** Layouts are shared within one compilation; see {@link BytecodeAnalysisCache}. */
    static BytecodeObjectLayouts of(Map<String, ClassNode> linked) {
        return BytecodeAnalysisCache.get(linked, "layouts", () -> new BytecodeObjectLayouts(linked));
    }

    private BytecodeObjectLayouts(Map<String, ClassNode> linked) {
        this.linked = linked;
        if (BytecodeExceptionRuntime.enabled(linked)) layouts.put("java/lang/Object", new Layout(4, Map.of(), List.of()));
        for (String name : linked.keySet()) build(name, new HashSet<>());
    }

    private Layout build(String name, Set<String> visiting) {
        if (layouts.containsKey(name)) return layouts.get(name);
        if (!visiting.add(name)) return null;
        ClassNode type = linked.get(name);
        if (type == null || (type.access & Opcodes.ACC_INTERFACE) != 0) return null;
        Layout parent = "java/lang/Object".equals(type.superName)
                ? new Layout(4, Map.of(), List.of()) : build(type.superName, visiting);
        if (parent == null) return null;
        int size = parent.size();
        Map<FieldKey, Integer> fields = new LinkedHashMap<>(parent.fields());
        List<Integer> references = new ArrayList<>(parent.references());
        for (FieldNode field : type.fields) {
            FieldKey signature = new FieldKey(field.name, field.desc);
            if ((field.access & Opcodes.ACC_STATIC) == 0) {
                fields.put(signature, size);
                if (field.desc.startsWith("L") || field.desc.startsWith("[")) references.add(size);
                size += (field.desc.equals("J") || field.desc.equals("D")) ? 8 : 4;
            } else {
                // A declared static field must not resolve to a hidden parent instance field.
                fields.remove(signature);
            }
        }
        Layout result = new Layout(size, fields, List.copyOf(references));
        layouts.put(name, result);
        visiting.remove(name);
        return result;
    }

    boolean contains(String name) { return layouts.containsKey(name); }

    /** Payload offsets of every reference-typed instance field slot of {@code name}, including inherited and hidden ones. */
    List<Integer> referenceOffsets(String name) {
        Layout layout = layouts.get(name);
        return layout == null ? List.of() : layout.references();
    }

    Map<String, Integer> sizes() {
        if (sizes != null) return sizes;
        Map<String, Integer> result = new LinkedHashMap<>();
        layouts.forEach((name, layout) -> result.put(name, layout.size()));
        return sizes = Collections.unmodifiableMap(result);
    }

    Map<String, Integer> offsets() {
        if (offsets != null) return offsets;
        Map<String, Integer> result = new LinkedHashMap<>();
        layouts.forEach((name, layout) -> layout.fields().forEach((field, offset) ->
                result.put(TypeHelper.getGlobalFieldName(name, field.name, field.descriptor), offset)));
        return offsets = Collections.unmodifiableMap(result);
    }
}
