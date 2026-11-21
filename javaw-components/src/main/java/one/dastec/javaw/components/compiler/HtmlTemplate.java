package one.dastec.javaw.components.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Build-time HTML tokenizer for templates (docs/web-components.md, "Binding kinds").
 *
 * <p>Walks the static parts with a small subset of the HTML tokenizer's states and classifies each
 * binding by its position. The output HTML marks each part: a child part becomes the comment
 * {@code <!--wc$N-->}; a bound attribute is replaced by the attribute {@code wc$N} whose value is the
 * original value with each binding replaced by {@link #MARKER}. The runtime finds the markers after
 * the browser has parsed the HTML, so parser fix-ups (implied {@code <tbody>}, foster parenting) cannot
 * misplace a part.
 */
final class HtmlTemplate {
    private HtmlTemplate() {}

    /** Separates the static strings of a bound attribute value; never appears in authored HTML. */
    static final String MARKER = "\u0001wc\u0001";

    // Elements whose content the parser does not tokenize as markup, plus <template>, whose content is diagnosed.
    private static final Set<String> RAW_TEXT = Set.of("script", "style", "textarea", "title", "xmp", "iframe",
            "noembed", "noframes", "noscript", "plaintext", "template");

    enum Kind { CHILD, ATTR, BOOL, PROP, EVENT }

    /** One part: a child position or an attribute with one or more bindings. */
    record Part(Kind kind, String name, int bindings) {}

    /** Per binding: the attribute, property or event name and the lowercase element tag (both null for child bindings). */
    record Result(String html, List<Part> parts, List<Kind> bindingKinds, List<String> bindingNames, List<String> bindingTags) {}

    /** A problem at binding {@code binding}, or for the whole template when it is -1. */
    static final class Problem extends RuntimeException {
        final int binding;
        Problem(int binding, String message) { super(message); this.binding = binding; }
    }

    private enum State {
        DATA, TAG_OPEN, END_TAG_OPEN, TAG_NAME, BEFORE_ATTR_NAME, ATTR_NAME, AFTER_ATTR_NAME,
        BEFORE_ATTR_VALUE, ATTR_VALUE_DQ, ATTR_VALUE_SQ, ATTR_VALUE_UNQ, SELF_CLOSING, COMMENT, BOGUS, RAW_TEXT
    }

    private static final class Tokenizer {
        final StringBuilder out = new StringBuilder();
        final List<Part> parts = new ArrayList<>();
        final List<Kind> bindingKinds = new ArrayList<>();
        final List<String> bindingNames = new ArrayList<>();
        final List<String> bindingTags = new ArrayList<>();
        State state = State.DATA;
        final StringBuilder tagName = new StringBuilder();
        boolean endTag;
        String rawTextTag;
        // The attribute being read.
        int attrStart;
        final StringBuilder attrName = new StringBuilder();
        char quote;
        final List<String> pieces = new ArrayList<>();
        final StringBuilder piece = new StringBuilder();
        final List<Integer> attrBindings = new ArrayList<>();
        // Set right after a binding in an unquoted value: the next character must end the value.
        int unquotedBinding = -1;

        void text(String s) {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (unquotedBinding >= 0) {
                    if (state == State.ATTR_VALUE_UNQ && !isHtmlSpace(c) && c != '>')
                        throw new Problem(unquotedBinding, "An unquoted attribute value cannot mix a binding with static text; quote the value");
                    unquotedBinding = -1;
                }
                step(c);
            }
        }

        void step(char c) {
            switch (state) {
                case DATA -> { out.append(c); if (c == '<') state = State.TAG_OPEN; }
                case TAG_OPEN -> {
                    out.append(c);
                    if (isAsciiLetter(c)) { state = State.TAG_NAME; endTag = false; tagName.setLength(0); tagName.append(c); }
                    else if (c == '/') state = State.END_TAG_OPEN;
                    else if (c == '!' || c == '?') state = State.BOGUS;
                    else state = State.DATA;
                    if (c == '<') state = State.TAG_OPEN;
                }
                case END_TAG_OPEN -> {
                    out.append(c);
                    if (isAsciiLetter(c)) { state = State.TAG_NAME; endTag = true; tagName.setLength(0); tagName.append(c); }
                    else state = c == '>' ? State.DATA : State.BOGUS;
                }
                case TAG_NAME -> {
                    if (isHtmlSpace(c)) { out.append(c); state = State.BEFORE_ATTR_NAME; }
                    else if (c == '/') { out.append(c); state = State.SELF_CLOSING; }
                    else if (c == '>') { out.append(c); endOfTag(); }
                    else { out.append(c); tagName.append(c); }
                }
                case BEFORE_ATTR_NAME -> {
                    if (isHtmlSpace(c)) out.append(c);
                    else if (c == '/') { out.append(c); state = State.SELF_CLOSING; }
                    else if (c == '>') { out.append(c); endOfTag(); }
                    else startAttribute(c);
                }
                case ATTR_NAME -> {
                    if (isHtmlSpace(c)) { out.append(c); state = State.AFTER_ATTR_NAME; }
                    else if (c == '=') { out.append(c); state = State.BEFORE_ATTR_VALUE; }
                    else if (c == '/') { endAttribute(); out.append(c); state = State.SELF_CLOSING; }
                    else if (c == '>') { endAttribute(); out.append(c); endOfTag(); }
                    else { out.append(c); attrName.append(c); }
                }
                case AFTER_ATTR_NAME -> {
                    if (isHtmlSpace(c)) out.append(c);
                    else if (c == '=') { out.append(c); state = State.BEFORE_ATTR_VALUE; }
                    else if (c == '/') { endAttribute(); out.append(c); state = State.SELF_CLOSING; }
                    else if (c == '>') { endAttribute(); out.append(c); endOfTag(); }
                    else { endAttribute(); startAttribute(c); }
                }
                case BEFORE_ATTR_VALUE -> {
                    if (isHtmlSpace(c)) out.append(c);
                    else if (c == '"' || c == '\'') { out.append(c); quote = c; state = c == '"' ? State.ATTR_VALUE_DQ : State.ATTR_VALUE_SQ; }
                    else if (c == '>') { endAttribute(); out.append(c); endOfTag(); }
                    else { quote = 0; state = State.ATTR_VALUE_UNQ; out.append(c); piece.append(c); }
                }
                case ATTR_VALUE_DQ, ATTR_VALUE_SQ -> {
                    out.append(c);
                    if (c == quote) { endAttribute(); state = State.BEFORE_ATTR_NAME; }
                    else piece.append(c);
                }
                case ATTR_VALUE_UNQ -> {
                    if (isHtmlSpace(c)) { endAttribute(); out.append(c); state = State.BEFORE_ATTR_NAME; }
                    else if (c == '>') { endAttribute(); out.append(c); endOfTag(); }
                    else { out.append(c); piece.append(c); }
                }
                case SELF_CLOSING -> {
                    // The slash is ignored on HTML elements, so <textarea/> still opens raw text.
                    if (c == '>') { out.append(c); endOfTag(); }
                    else { state = State.BEFORE_ATTR_NAME; step(c); }
                }
                case COMMENT -> { out.append(c); if (endsWith("-->") || endsWith("--!>")) state = State.DATA; }
                case BOGUS -> {
                    out.append(c);
                    if (endsWith("<!--")) state = State.COMMENT;
                    else if (c == '>') state = State.DATA;
                }
                case RAW_TEXT -> {
                    out.append(c);
                    if (endsWithIgnoreCase("</" + rawTextTag)) { state = State.TAG_NAME; endTag = true; tagName.setLength(0); tagName.append(rawTextTag); }
                }
            }
        }

        void binding(int index) {
            switch (state) {
                case DATA -> {
                    out.append("<!--wc$").append(parts.size()).append("-->");
                    parts.add(new Part(Kind.CHILD, null, 1));
                    bindingKinds.add(Kind.CHILD);
                    bindingNames.add(null);
                    bindingTags.add(null);
                }
                case BEFORE_ATTR_VALUE -> {
                    quote = 0;
                    state = State.ATTR_VALUE_UNQ;
                    attributeBinding(index);
                    unquotedBinding = index;
                }
                case ATTR_VALUE_UNQ -> throw new Problem(index, "An unquoted attribute value cannot mix a binding with static text; quote the value");
                case ATTR_VALUE_DQ, ATTR_VALUE_SQ -> attributeBinding(index);
                case BEFORE_ATTR_NAME, AFTER_ATTR_NAME, SELF_CLOSING ->
                        throw new Problem(index, "Bindings in element position (ref) are not supported yet; bind an attribute value instead");
                case ATTR_NAME -> throw new Problem(index, "Bindings are not allowed in attribute names");
                case TAG_OPEN, TAG_NAME, END_TAG_OPEN -> throw new Problem(index, "Bindings are not allowed in tag names or closing tags");
                case COMMENT, BOGUS -> throw new Problem(index, "Bindings are not allowed inside HTML comments");
                case RAW_TEXT -> throw new Problem(index, "Bindings are not allowed inside <" + rawTextTag + "> content");
            }
        }

        void startAttribute(char c) {
            attrStart = out.length();
            attrName.setLength(0);
            attrName.append(c);
            pieces.clear();
            piece.setLength(0);
            attrBindings.clear();
            out.append(c);
            state = State.ATTR_NAME;
        }

        void attributeBinding(int index) {
            pieces.add(piece.toString());
            piece.setLength(0);
            attrBindings.add(index);
        }

        void endAttribute() {
            if (attrBindings.isEmpty()) { piece.setLength(0); return; }
            pieces.add(piece.toString());
            piece.setLength(0);
            String name = attrName.toString();
            Kind kind = switch (name.isEmpty() ? ' ' : name.charAt(0)) {
                case '.' -> Kind.PROP;
                case '?' -> Kind.BOOL;
                case '@' -> Kind.EVENT;
                default -> Kind.ATTR;
            };
            if (kind != Kind.ATTR) {
                name = name.substring(1);
                if (name.isEmpty()) throw new Problem(attrBindings.get(0), "Missing name after '" + attrName.charAt(0) + "'");
                if (attrBindings.size() != 1 || pieces.stream().anyMatch(p -> !p.isEmpty()))
                    throw new Problem(attrBindings.get(0), "A " + attrName.charAt(0) + "name binding must be exactly one binding with no static text");
            }
            int index = parts.size();
            parts.add(new Part(kind, name, attrBindings.size()));
            String tag = tagName.toString().toLowerCase(Locale.ROOT);
            for (int ignored : attrBindings) { bindingKinds.add(kind); bindingNames.add(name); bindingTags.add(tag); }
            char q = quote == 0 ? '"' : quote;
            String value = String.join(MARKER, pieces);
            if (value.indexOf(q) >= 0) throw new Problem(attrBindings.get(0), "Attribute value contains its own quote character");
            out.setLength(attrStart);
            out.append("wc$").append(index).append('=').append(q).append(value).append(q);
            attrBindings.clear();
        }

        void endOfTag() {
            String name = tagName.toString().toLowerCase(Locale.ROOT);
            if (!endTag && RAW_TEXT.contains(name)) { state = State.RAW_TEXT; rawTextTag = name; }
            else state = State.DATA;
        }

        boolean endsWith(String s) {
            return out.length() >= s.length() && out.substring(out.length() - s.length()).equals(s);
        }

        boolean endsWithIgnoreCase(String s) {
            return out.length() >= s.length() && out.substring(out.length() - s.length()).equalsIgnoreCase(s);
        }
    }

    static Result compile(List<String> statics) {
        Tokenizer t = new Tokenizer();
        for (int i = 0; i < statics.size(); i++) {
            if (statics.get(i).contains(MARKER) || statics.get(i).matches("(?s).*wc\\$\\d.*"))
                throw new Problem(-1, "Templates may not contain the reserved text 'wc$<digit>' or U+0001");
            t.text(statics.get(i));
            if (i < statics.size() - 1) t.binding(i);
        }
        if (t.state == State.ATTR_VALUE_UNQ) t.endAttribute();
        if (t.state != State.DATA && t.state != State.TAG_OPEN)
            throw new Problem(-1, "Template ends inside " + switch (t.state) {
                case COMMENT, BOGUS -> "a comment";
                case RAW_TEXT -> "<" + t.rawTextTag + "> content";
                default -> "a tag";
            });
        return new Result(t.out.toString(), List.copyOf(t.parts), List.copyOf(t.bindingKinds), java.util.Collections.unmodifiableList(new ArrayList<>(t.bindingNames)),
                java.util.Collections.unmodifiableList(new ArrayList<>(t.bindingTags)));
    }

    /** HTML whitespace: tab, line feed, form feed, carriage return and space. */
    private static boolean isHtmlSpace(char c) {
        return c == '\t' || c == '\n' || c == '\f' || c == '\r' || c == ' ';
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
}
