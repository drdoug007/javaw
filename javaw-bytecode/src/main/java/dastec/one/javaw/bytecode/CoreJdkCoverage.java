package dastec.one.javaw.bytecode;

import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Signature inventory only: declarations do not prove execution or runtime reachability. */
final class CoreJdkCoverage {
    private static final Set<String> LANG = Set.of(
            "String", "StringBuilder", "StringBuffer", "CharSequence", "Boolean", "Byte", "Short",
            "Integer", "Long", "Float", "Double", "Character", "Number", "Math", "StrictMath",
            "Comparable", "Iterable");
    private static final Set<String> UTIL = Set.of(
            "AbstractCollection", "AbstractList", "AbstractMap", "AbstractQueue", "AbstractSet",
            "ArrayDeque", "ArrayList", "Arrays", "BitSet", "Collection", "Collections", "Comparator",
            "Deque", "EmptyStackException", "EnumMap", "EnumSet", "Enumeration", "HashMap", "HashSet",
            "Hashtable", "IdentityHashMap", "Iterator", "LinkedHashMap", "LinkedHashSet", "LinkedList",
            "List", "ListIterator", "Map", "NavigableMap", "NavigableSet", "Objects", "Optional",
            "OptionalInt", "OptionalLong", "OptionalDouble", "PriorityQueue", "Queue", "Set",
            "SortedMap", "SortedSet", "Spliterator", "Spliterators", "Stack", "StringJoiner",
            "StringTokenizer", "TreeMap", "TreeSet", "Vector", "WeakHashMap");

    private CoreJdkCoverage() {}

    static boolean includes(String owner) {
        if (owner.startsWith("java/math/") || owner.startsWith("java/util/function/")
                || owner.startsWith("java/util/stream/")) return true;
        int slash = owner.lastIndexOf('/');
        String base = owner.substring(slash + 1).split("\\$", 2)[0];
        return slash == 9 && (owner.startsWith("java/lang/") && LANG.contains(base)
                || owner.startsWith("java/util/") && UTIL.contains(base));
    }

    static String status(Map<String, ClassNode> types, String owner, String name, String descriptor) {
        if (!types.containsKey(owner)) return "missing-owner";
        return declares(types, owner, name, descriptor, new HashSet<>())
                ? "declared-signature" : "missing-member";
    }

    private static boolean declares(Map<String, ClassNode> types, String owner, String name,
                                    String descriptor, Set<String> visited) {
        if (owner == null || !visited.add(owner)) return false;
        ClassNode type = types.get(owner);
        if (type == null && owner.equals(BytecodeObjects.OBJECT)) type = BytecodeObjects.declaration();
        if (type == null) return false;
        if (descriptor.startsWith("(")) {
            if (type.methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.equals(descriptor))) return true;
        } else if (type.fields.stream().anyMatch(f -> f.name.equals(name) && f.desc.equals(descriptor))) {
            return true;
        }
        if (name.startsWith("<")) return false;
        if (declares(types, type.superName, name, descriptor, visited)) return true;
        for (String iface : type.interfaces) {
            if (declares(types, iface, name, descriptor, visited)) return true;
        }
        return false;
    }

    static void write(Path directory) throws IOException {
        Map<String, ClassNode> types = BytecodeExceptionRuntime.definitions();
        List<String> result = new ArrayList<>();
        result.add("artifact\topcodeOrNegativeHandleTag\towner\tmember\tdescriptor\tcount\tfirstSite\tstatus");
        Map<String, Integer> counts = new TreeMap<>();
        Set<String> distinct = new HashSet<>();
        List<String> lines = Files.readAllLines(directory.resolve("member-references.tsv"));
        for (String line : lines.subList(1, lines.size())) {
            if (line.isEmpty()) continue;
            String[] parts = line.split("\t", -1);
            if (!includes(parts[2])) continue;
            String status = status(types, parts[2], parts[3], parts[4]);
            result.add(line + "\t" + status);
            if (distinct.add(parts[2] + "." + parts[3] + parts[4])) counts.merge(status, 1, Integer::sum);
        }
        Files.write(directory.resolve("core-jdk-linkage.tsv"), result);
        List<String> summary = new ArrayList<>(List.of("status\tdistinctSignatures"));
        counts.forEach((key, value) -> summary.add(key + "\t" + value));
        Files.write(directory.resolve("core-jdk-linkage-summary.tsv"), summary);
    }
}
