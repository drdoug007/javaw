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
    /** Each prefix ends where an id begins: the regex {@code (?:call|return_call|ref\.func) (\$[^\s()]+)}. */
    static final String[] REFERENCE = {"call $", "return_call $", "ref.func $"};
    /** {@code (?:global\.get|global\.set|\(global) (\$[^\s()]+)} */
    static final String[] GLOBAL_REFERENCE = {"global.get $", "global.set $", "(global $"};
    /** Globals share the worklist under this prefix; WAT keeps functions and globals in separate namespaces. */
    private static final String GLOBAL_KEY = "global ";
    record Field(int start, int end, String function) {}
    /** Text after which {@link #prune} returns the module unchanged (indirect calls and tables), or cannot parse it. */
    static final List<String> UNPRUNABLE = List.of("call_indirect", "(table", "(;");

    private BytecodeDeadCode() {}

    /** Export names are tested in their escaped WAT form; {@code retained} names extra root function ids. */
    static String prune(String wat, Predicate<String> rootExport, Set<String> retained) {
        int open = wat.indexOf("(module");
        if (open < 0) return wat;
        List<Field> fields = fields(wat, open + "(module".length());
        if (fields == null || wat.contains(UNPRUNABLE.get(0)) || wat.contains(UNPRUNABLE.get(1))) return wat;
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
        scan(wat, field.start(), field.end(), REFERENCE, "", pending);
        scan(wat, field.start(), field.end(), GLOBAL_REFERENCE, GLOBAL_KEY, pending);
    }

    /**
     * A top-level module field: its function id (null for other fields), its export name and target, its text, and its
     * span in {@code "(module\n" + body}, including a trailing newline, which is the span {@link #prune} removes.
     */
    record Item(String function, String export, String exportTarget, String text, int start, int end) {}

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
            result.add(new Item(field.function(), export, target, wat.substring(field.start(), field.end()), field.start(), field.end()));
        }
        return result;
    }

    /** Function ids referenced by {@code call}, {@code return_call} or {@code ref.func} in {@code text}. */
    static void references(String text, Collection<String> result) {
        scan(text, 0, text.length(), REFERENCE, "", result);
    }

    /**
     * Adds {@code key} plus each id that follows one of {@code prefixes} within {@code [from, to)}, in text order. This is
     * a regex {@code find} loop: matches are leftmost and do not overlap, and an id is one or more characters other than
     * whitespace and parentheses. Every prefix ends in its only {@code " $"}, so candidates are found by searching for
     * that pair rather than by regex matching over the multi-megabyte runtime text.
     */
    static void scan(String text, int from, int to, String[] prefixes, String key, Collection<String> result) {
        int next = from; // Earliest start of the next match; a match never starts inside the previous one.
        for (int mark = text.indexOf(" $", from); mark >= 0 && mark + 2 < to; mark = text.indexOf(" $", mark + 1)) {
            int id = mark + 2;
            boolean prefixed = false;
            for (String prefix : prefixes) {
                int start = id - prefix.length();
                if (start >= next && text.startsWith(prefix, start)) { prefixed = true; break; }
            }
            if (!prefixed) continue;
            int end = id;
            while (end < to && !separator(text.charAt(end))) end++;
            if (end == id) continue;
            result.add(key + text.substring(mark + 1, end));
            next = end;
            mark = end - 1;
        }
    }

    /** The regex classes {@code \s} (ASCII whitespace), {@code (} and {@code )}. */
    private static boolean separator(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r' || c == '(' || c == ')';
    }

    /**
     * Top-level module fields with their trailing newline; null if the text is not plain WAT this pass understands. Only
     * {@code "}, {@code ;}, {@code (} and {@code )} matter, and function bodies contain few of them, so the scan jumps
     * between their next occurrences ({@code indexOf}) instead of reading every character. A string ends at the first
     * quote not consumed by a backslash escape; {@code ;;} comments run to the end of the line; {@code (;} block
     * comments are not supported.
     */
    static List<Field> fields(String wat, int position) {
        List<Field> result = new ArrayList<>();
        int length = wat.length(), depth = 0, start = -1;
        int quote = wat.indexOf('"', position), semicolon = wat.indexOf(';', position),
                open = wat.indexOf('(', position), close = wat.indexOf(')', position);
        int escape = wat.indexOf('\\', position); // Cached like the others: a fresh search per string could run to the end.
        int i = position;
        while (true) {
            if (quote >= 0 && quote < i) quote = wat.indexOf('"', i);
            if (semicolon >= 0 && semicolon < i) semicolon = wat.indexOf(';', i);
            if (open >= 0 && open < i) open = wat.indexOf('(', i);
            if (close >= 0 && close < i) close = wat.indexOf(')', i);
            int next = Math.min(Math.min(earliest(quote), earliest(semicolon)), Math.min(earliest(open), earliest(close)));
            if (next == Integer.MAX_VALUE) return null;
            char c = wat.charAt(next);
            if (c == '"') {
                // The first quote after the opening one that no backslash escape consumes ends the string.
                int j = next + 1;
                while (true) {
                    int endQuote = wat.indexOf('"', j);
                    if (escape >= 0 && escape < j) escape = wat.indexOf('\\', j);
                    if (endQuote < 0) return null;
                    if (escape >= 0 && escape < endQuote) { j = escape + 2; continue; }
                    i = endQuote + 1;
                    break;
                }
            } else if (c == ';') {
                if (next + 1 < length && wat.charAt(next + 1) == ';') {
                    int newline = wat.indexOf('\n', next);
                    if (newline < 0) return null;
                    i = newline + 1;
                } else i = next + 1;
            } else if (c == '(') {
                if (next + 1 < length && wat.charAt(next + 1) == ';') return null;
                if (depth++ == 0) start = next;
                i = next + 1;
            } else {
                if (depth == 0) return result;
                if (--depth == 0) {
                    int end = next + 1;
                    if (end < length && wat.charAt(end) == '\n') end++;
                    Matcher name = NAME.matcher(wat).region(start, end);
                    result.add(new Field(start, end, name.lookingAt() ? name.group(1) : null));
                }
                i = next + 1;
            }
        }
    }

    /** An {@code indexOf} result, with "not found" ordered after every position. */
    private static int earliest(int index) { return index < 0 ? Integer.MAX_VALUE : index; }
}
