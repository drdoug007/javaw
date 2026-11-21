package one.dastec.javaw.components.compiler;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits the raw source text of an {@code html(...)} literal into static parts and {@code ${...}} bindings.
 *
 * <p>Bindings are found in the raw characters between the delimiters, so a binding expression is the
 * verbatim source text. The static parts get the literal's Java processing (for text blocks, incidental
 * indentation stripping over the whole raw block, then escape translation) exactly as javac applies it.
 * To get that, each binding is masked with private-use characters that keep its whitespace layout, the
 * masked text is processed with {@link String#stripIndent()} and {@link String#translateEscapes()}, and
 * the processed text is cut at the masks.
 */
final class TemplateScanner {
    private TemplateScanner() {}

    private static final char START = '', BODY = '';

    /** One binding: the expression source and the raw offset of its first character. */
    record Binding(String expression, int offset) {}

    /** {@code statics.size() == bindings.size() + 1}. */
    record Scan(List<String> statics, List<Binding> bindings) {}

    /** A problem at a raw offset (or -1 for the whole literal). */
    static final class Problem extends RuntimeException {
        final int offset;
        Problem(int offset, String message) { super(message); this.offset = offset; }
    }

    static Scan scan(String raw, boolean textBlock) {
        for (int i = 0; i < raw.length(); i++)
            if (raw.charAt(i) == START || raw.charAt(i) == BODY)
                throw new Problem(i, "Templates may not contain the characters U+E000 or U+E001");
        List<Binding> bindings = new ArrayList<>();
        StringBuilder masked = new StringBuilder(raw);
        int i = 0;
        while ((i = raw.indexOf("${", i)) >= 0) {
            int end = closing(raw, i + 2);
            String expression = raw.substring(i + 2, end);
            if (expression.isBlank()) throw new Problem(i, "Empty template binding '${}'");
            bindings.add(new Binding(expression, i + 2));
            masked.setCharAt(i, START);
            for (int j = i + 1; j <= end; j++) if (!Character.isWhitespace(raw.charAt(j))) masked.setCharAt(j, BODY);
            i = end + 1;
        }
        String processed;
        try {
            processed = (textBlock ? masked.toString().stripIndent() : masked.toString()).translateEscapes();
        } catch (IllegalArgumentException e) {
            throw new Problem(-1, "Invalid escape sequence in template: " + e.getMessage());
        }
        List<String> statics = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int k = 0; k < processed.length(); ) {
            char c = processed.charAt(k);
            if (c != START) { current.append(c); k++; continue; }
            statics.add(current.toString());
            current.setLength(0);
            // The binding ends at its last masked character ('}'); whitespace after it is static again.
            int last = k, j = k + 1;
            while (j < processed.length() && processed.charAt(j) != START
                    && (processed.charAt(j) == BODY || Character.isWhitespace(processed.charAt(j)))) {
                if (processed.charAt(j) == BODY) last = j;
                j++;
            }
            k = last + 1;
        }
        statics.add(current.toString());
        if (statics.size() != bindings.size() + 1) throw new Problem(-1, "Could not split the template into bindings");
        return new Scan(statics, bindings);
    }

    /** Index of the '}' closing the binding whose body starts at {@code from}. */
    private static int closing(String raw, int from) {
        int depth = 1;
        for (int j = from; j < raw.length(); j++) {
            char c = raw.charAt(j);
            if (c == '"' || c == '\'') {
                j = skipQuoted(raw, j, c);
            } else if (c == '/' && j + 1 < raw.length() && raw.charAt(j + 1) == '/') {
                int nl = raw.indexOf('\n', j);
                if (nl < 0) break;
                j = nl;
            } else if (c == '/' && j + 1 < raw.length() && raw.charAt(j + 1) == '*') {
                int close = raw.indexOf("*/", j + 2);
                if (close < 0) break;
                j = close + 1;
            } else if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return j;
            }
        }
        throw new Problem(from - 2, "Unterminated template binding: '${' has no matching '}'");
    }

    private static int skipQuoted(String raw, int open, char quote) {
        for (int j = open + 1; j < raw.length(); j++) {
            char c = raw.charAt(j);
            if (c == '\\') j++;
            else if (c == quote) return j;
            else if (c == '\n') break;
        }
        throw new Problem(open, "Unterminated " + (quote == '"' ? "string" : "character") + " literal in template binding");
    }
}
