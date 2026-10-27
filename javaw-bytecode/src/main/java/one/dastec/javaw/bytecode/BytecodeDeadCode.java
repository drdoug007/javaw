package one.dastec.javaw.bytecode;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes module functions unreachable from root exports, the start function and non-function fields.
 * Admission has already validated every linked declaration; this only drops code no entry point can call.
 * Dispatch lowers to direct calls, so the call graph is exact while the module has no tables.
 */
final class BytecodeDeadCode {
    private static final Pattern NAME = Pattern.compile("\\(func (\\$[^\\s()]+)");
    private static final Pattern INLINE_EXPORT = Pattern.compile("\\(func \\$[^\\s()]+ \\(export \"([^\"]*)\"\\)");
    private static final Pattern EXPORT = Pattern.compile("\\(export \"([^\"]*)\" \\(func (\\$[^\\s()]+)\\)\\)");
    private static final Pattern REFERENCE = Pattern.compile("(?:call|return_call|ref\\.func) (\\$[^\\s()]+)");
    private record Field(int start, int end, String function) {}

    private BytecodeDeadCode() {}

    /** Export names are tested in their escaped WAT form; {@code retained} names extra root function ids. */
    static String prune(String wat, Predicate<String> rootExport, Set<String> retained) {
        int open = wat.indexOf("(module");
        if (open < 0) return wat;
        List<Field> fields = fields(wat, open + "(module".length());
        if (fields == null || wat.contains("call_indirect") || wat.contains("(table")) return wat;
        Map<String, Field> functions = new HashMap<>();
        Deque<String> pending = new ArrayDeque<>();
        for (Field field : fields) {
            if (field.function() != null) {
                functions.put(field.function(), field);
                Matcher export = INLINE_EXPORT.matcher(wat).region(field.start(), field.end());
                if (export.lookingAt() && rootExport.test(export.group(1))) pending.add(field.function());
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
            Field field = functions.get(name);
            if (field != null && reachable.add(name)) references(wat, field, pending);
        }
        StringBuilder result = new StringBuilder(wat.length() / 4);
        int copied = 0;
        for (Field field : fields) {
            String target = field.function();
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
