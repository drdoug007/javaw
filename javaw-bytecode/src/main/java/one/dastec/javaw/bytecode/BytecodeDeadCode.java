package one.dastec.javaw.bytecode;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes module functions and named globals unreachable from root exports, exported globals and the other
 * non-function fields. Admission has already validated every linked declaration; this only drops code and state no
 * entry point can use. Dispatch lowers to direct calls, so the call graph is exact while the module has no tables.
 */
final class BytecodeDeadCode {
    private static final Pattern NAME = Pattern.compile("\\(func (\\$[^\\s()]+)");
    private static final Pattern GLOBAL = Pattern.compile("\\(global (\\$[^\\s()]+)( \\(export )?");
    private static final Pattern INLINE_EXPORT = Pattern.compile("\\(func \\$[^\\s()]+ \\(export \"([^\"]*)\"\\)");
    private static final Pattern EXPORT = Pattern.compile("\\(export \"([^\"]*)\" \\(func (\\$[^\\s()]+)\\)\\)");
    private static final Pattern REFERENCE = Pattern.compile("(?:call|return_call|ref\\.func) (\\$[^\\s()]+)");
    private static final Pattern GLOBAL_REFERENCE = Pattern.compile("(?:global\\.get|global\\.set|\\(global) (\\$[^\\s()]+)");
    /** Globals share the worklist under this prefix; WAT keeps functions and globals in separate namespaces. */
    private static final String GLOBAL_KEY = "global ";
    private record Field(int start, int end, String function) {}

    private BytecodeDeadCode() {}

    /** Export names are tested in their escaped WAT form; {@code retained} names extra root function ids. */
    static String prune(String wat, Predicate<String> rootExport, Set<String> retained) {
        int open = wat.indexOf("(module");
        if (open < 0) return wat;
        List<Field> fields = fields(wat, open + "(module".length());
        if (fields == null || wat.contains("call_indirect") || wat.contains("(table")) return wat;
        Map<String, Field> nodes = new HashMap<>();
        Map<Field, String> globals = new IdentityHashMap<>();
        Deque<String> pending = new ArrayDeque<>();
        for (Field field : fields) {
            if (field.function() != null) {
                nodes.put(field.function(), field);
                Matcher export = INLINE_EXPORT.matcher(wat).region(field.start(), field.end());
                if (export.lookingAt() && rootExport.test(export.group(1))) pending.add(field.function());
                continue;
            }
            Matcher global = GLOBAL.matcher(wat).region(field.start(), field.end());
            if (global.lookingAt()) {
                String key = GLOBAL_KEY + global.group(1);
                nodes.put(key, field);
                globals.put(field, key);
                if (global.group(2) != null) pending.add(key); // Exported state is visible to the host.
                continue;
            }
            Matcher export = EXPORT.matcher(wat).region(field.start(), field.end());
            if (export.lookingAt()) {
                if (rootExport.test(export.group(1))) pending.add(export.group(2));
                continue;
            }
            references(wat, field, pending);
        }
        pending.addAll(retained);
        Set<String> reachable = new HashSet<>();
        while (!pending.isEmpty()) {
            String name = pending.pop();
            Field field = nodes.get(name);
            if (field != null && reachable.add(name)) references(wat, field, pending);
        }
        StringBuilder result = new StringBuilder(wat.length() / 4);
        int copied = 0;
        for (Field field : fields) {
            String target = field.function() != null ? field.function() : globals.get(field);
            if (target == null) {
                Matcher export = EXPORT.matcher(wat).region(field.start(), field.end());
                if (export.lookingAt()) target = export.group(2);
            }
            if (target == null || reachable.contains(target)) continue;
            result.append(wat, copied, field.start());
            copied = field.end();
        }
        return result.append(wat, copied, wat.length()).toString();
    }

    private static void references(String wat, Field field, Deque<String> pending) {
        Matcher reference = REFERENCE.matcher(wat).region(field.start(), field.end());
        while (reference.find()) pending.add(reference.group(1));
        Matcher global = GLOBAL_REFERENCE.matcher(wat).region(field.start(), field.end());
        while (global.find()) pending.add(GLOBAL_KEY + global.group(1));
    }

    /** A top-level module field: its function id (null for other fields), its export name and target, and its text. */
    record Item(String function, String export, String exportTarget, String text) {}

    /** The top-level fields of module body text (fields without the {@code (module} wrapper); null if not plain WAT. */
    static List<Item> items(String body) {
        String wat = "(module\n" + body + ")";
        List<Field> fields = fields(wat, "(module".length());
        if (fields == null) return null;
        List<Item> result = new ArrayList<>(fields.size());
        for (Field field : fields) {
            String export = null, target = field.function();
            Matcher inline = INLINE_EXPORT.matcher(wat).region(field.start(), field.end());
            Matcher separate = EXPORT.matcher(wat).region(field.start(), field.end());
            if (field.function() != null) { if (inline.lookingAt()) export = inline.group(1); }
            else if (separate.lookingAt()) { export = separate.group(1); target = separate.group(2); }
            result.add(new Item(field.function(), export, target, wat.substring(field.start(), field.end())));
        }
        return result;
    }

    /** Function ids referenced by {@code call}, {@code return_call} or {@code ref.func} in {@code text}. */
    static void references(CharSequence text, Collection<String> result) {
        Matcher reference = REFERENCE.matcher(text);
        while (reference.find()) result.add(reference.group(1));
    }

    /** Top-level module fields with their trailing newline; null if the text is not plain WAT this pass understands. */
    private static List<Field> fields(String wat, int position) {
        List<Field> result = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = position; i < wat.length(); i++) {
            char c = wat.charAt(i);
            if (c == '"') {
                for (i++; i < wat.length() && wat.charAt(i) != '"'; i++) if (wat.charAt(i) == '\\') i++;
            } else if (c == ';' && i + 1 < wat.length() && wat.charAt(i + 1) == ';') {
                while (i < wat.length() && wat.charAt(i) != '\n') i++;
            } else if (c == '(' && i + 1 < wat.length() && wat.charAt(i + 1) == ';') {
                return null;
            } else if (c == '(') {
                if (depth++ == 0) start = i;
            } else if (c == ')') {
                if (depth == 0) return result;
                if (--depth == 0) {
                    int end = i + 1;
                    if (end < wat.length() && wat.charAt(end) == '\n') end++;
                    Matcher name = NAME.matcher(wat).region(start, end);
                    result.add(new Field(start, end, name.lookingAt() ? name.group(1) : null));
                }
            }
        }
        return null;
    }
}
