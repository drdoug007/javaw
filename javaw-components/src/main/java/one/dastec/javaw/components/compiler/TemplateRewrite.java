package one.dastec.javaw.components.compiler;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.*;
import one.dastec.javaw.spi.SourceExtension;

import java.util.ArrayList;
import java.util.List;

/**
 * M3 template rewrite: {@code html("...")} becomes {@code Template.of(id, Parts.kind(expr), ...)}, and
 * each template's marked HTML and part list goes into the {@code javaw_wc_templates} section.
 *
 * <p>Binding expressions are parsed from the raw literal text and their nodes get the source positions
 * of that text, so later diagnostics (for example a binding of the wrong type for its position) point at
 * the binding itself.
 */
final class TemplateRewrite {
    static final String SECTION = "javaw_wc_templates";

    private record Definition(int id, HtmlTemplate.Result result) {}

    private final List<Definition> definitions = new ArrayList<>();

    /** A Java component known to the module: its class and its @Property fields (name to declared type). */
    record Component(String className, java.util.Map<String, Field> properties) {}

    /** A @Property field: its declared type and the component class that declares it. */
    record Field(String type, String owner) {}

    /** A property of a known component bound with {@code .name=${...}}; {@code id} is its key in the module. */
    record PropertyKey(int id, String className, String field, String type) {}

    private final java.util.Map<String, Component> components;
    private final java.util.Map<String, PropertyKey> keys = new java.util.LinkedHashMap<>();

    TemplateRewrite(java.util.Map<String, Component> componentsByTag) {
        this.components = componentsByTag;
    }

    /** The component properties that templates bind, in key order. */
    List<PropertyKey> propertyKeys() { return List.copyOf(keys.values()); }
    private final JavaParser parser = new JavaParser(new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));

    boolean isEmpty() { return definitions.isEmpty(); }

    /** Rewrites every {@code html(...)} call in classes that extend {@code WebComponent}. */
    void rewrite(CompilationUnit cu) {
        for (MethodCallExpr call : new ArrayList<>(cu.findAll(MethodCallExpr.class)))
            if (isHtmlCall(call) && call.getParentNode().isPresent()) call.replace(lower(call));
        // The rewritten source also compiles with javac against the public API.
        if (!definitions.isEmpty())
            for (String type : List.of("Template", "Parts"))
                if (cu.getImports().stream().noneMatch(i -> i.getNameAsString().equals(ComponentExtension.PACKAGE + "." + type)
                        || (i.isAsterisk() && !i.isStatic() && i.getNameAsString().equals(ComponentExtension.PACKAGE))))
                    cu.addImport(ComponentExtension.PACKAGE + "." + type);
    }

    private static boolean isHtmlCall(MethodCallExpr call) {
        if (!call.getNameAsString().equals("html")) return false;
        if (call.getScope().isPresent()) return isTemplateScope(call.getScope().get());
        // An html method declared by an enclosing class wins over the inherited or imported one, as in Java.
        for (Node n = call.getParentNode().orElse(null); n != null; n = n.getParentNode().orElse(null))
            if (n instanceof ClassOrInterfaceDeclaration c && !c.getMethodsByName("html").isEmpty()) return false;
        // html is inherited (also through other classes of the module) and visible in nested classes.
        CompilationUnit cu = call.findCompilationUnit().orElse(null);
        for (Node n = call.getParentNode().orElse(null); n != null; n = n.getParentNode().orElse(null))
            if (n instanceof ClassOrInterfaceDeclaration c && extendsComponent(cu, c, new java.util.HashSet<>())) return true;
        // Or statically imported from Templates.
        return cu != null && cu.getImports().stream().anyMatch(i -> i.isStatic()
                && (i.isAsterisk() ? i.getNameAsString().equals(ComponentExtension.PACKAGE + ".Templates")
                                   : i.getNameAsString().equals(ComponentExtension.PACKAGE + ".Templates.html")));
    }

    private static boolean isTemplateScope(Expression scope) {
        String name = scope.toString();
        return name.equals("WebComponent") || name.equals("Templates");
    }

    private static boolean extendsComponent(CompilationUnit cu, ClassOrInterfaceDeclaration type, java.util.Set<String> seen) {
        if (!seen.add(type.getNameAsString())) return false;
        for (var extended : type.getExtendedTypes()) {
            String name = extended.getNameAsString();
            if (name.equals("WebComponent")) return true;
            if (cu != null)
                for (ClassOrInterfaceDeclaration other : cu.findAll(ClassOrInterfaceDeclaration.class))
                    if (other.getNameAsString().equals(name) && extendsComponent(cu, other, seen)) return true;
        }
        return false;
    }

    /** html("...") inside a binding: unqualified or WebComponent.html. */
    private static boolean isNestedHtmlCall(MethodCallExpr call) {
        return call.getNameAsString().equals("html")
                && (call.getScope().isEmpty() || isTemplateScope(call.getScope().get()));
    }

    private Expression lower(MethodCallExpr call) {
        if (call.getArguments().size() != 1)
            throw SourceExtension.error(call, "html(...) takes exactly one string literal or text block");
        Expression argument = call.getArgument(0);
        boolean textBlock = argument.isTextBlockLiteralExpr();
        if (!textBlock && !argument.isStringLiteralExpr())
            throw SourceExtension.error(argument, "html(...) requires a string literal or text block, not an expression");
        String raw = textBlock ? argument.asTextBlockLiteralExpr().getValue() : argument.asStringLiteralExpr().getValue();
        if (textBlock) {
            // JavaParser keeps whitespace after the opening delimiter and that line's terminator; javac drops them.
            int nl = raw.indexOf('\n');
            if (nl >= 0 && raw.substring(0, nl).isBlank()) raw = raw.substring(nl + 1);
        }
        Position begin = argument.getBegin().orElse(null);
        TemplateScanner.Scan scan;
        try {
            scan = TemplateScanner.scan(raw, textBlock);
        } catch (TemplateScanner.Problem p) {
            throw p.offset < 0 ? SourceExtension.error(argument, p.getMessage()) : errorAt(argument, position(begin, raw, p.offset, textBlock), p.getMessage());
        }
        HtmlTemplate.Result result;
        try {
            result = HtmlTemplate.compile(scan.statics());
        } catch (HtmlTemplate.Problem p) {
            if (p.binding < 0) throw SourceExtension.error(argument, p.getMessage());
            throw errorAt(argument, position(begin, raw, scan.bindings().get(p.binding).offset() - 2, textBlock), p.getMessage());
        }
        int id = definitions.size() + 1;
        definitions.add(new Definition(id, result));

        NodeList<Expression> arguments = new NodeList<>(new IntegerLiteralExpr(String.valueOf(id)));
        for (int i = 0; i < scan.bindings().size(); i++) {
            TemplateScanner.Binding binding = scan.bindings().get(i);
            Position at = position(begin, raw, binding.offset(), textBlock);
            Position start = position(begin, raw, binding.offset() - 2, textBlock);
            Expression expression = parse(argument, binding.expression(), at, start);
            // A binding may itself contain one-line html("...") calls.
            for (MethodCallExpr inner : new ArrayList<>(expression.findAll(MethodCallExpr.class)))
                if (isNestedHtmlCall(inner) && inner != expression && inner.getParentNode().isPresent())
                    inner.replace(lower(inner));
            if (expression instanceof MethodCallExpr m && isNestedHtmlCall(m))
                expression = lower(m);
            Component target = result.bindingKinds().get(i) == HtmlTemplate.Kind.PROP ? components.get(result.bindingTags().get(i)) : null;
            arguments.add(target != null ? componentProperty(target, result.bindingTags().get(i), result.bindingNames().get(i), expression, argument, start)
                    : wrap(result.bindingKinds().get(i), result.bindingNames().get(i), expression, argument, start));
        }
        MethodCallExpr of = new MethodCallExpr(new NameExpr("Template"), "of", arguments);
        call.getRange().ifPresent(of::setRange);
        return of;
    }

    /** {@code .name=${x}} on a Java component of this module: a typed call that sets the field through its setter. */
    private Expression componentProperty(Component target, String tag, String name, Expression expression, Node anchor, Position at) {
        Field field = target.properties().get(name);
        if (field == null)
            throw errorAt(anchor, at, "<" + tag + "> (" + target.className() + ") has no @Property '" + name + "'"
                    + (target.properties().isEmpty() ? "" : "; its properties are " + String.join(", ", target.properties().keySet())));
        // The key and its generated methods belong to the declaring class, so a subclass's tag shares its base's key.
        keys.computeIfAbsent(field.owner() + "#" + name, k -> new PropertyKey(keys.size() + 1, field.owner(), name, field.type()));
        MethodCallExpr call = new MethodCallExpr(new NameExpr(field.owner()), "__wc_prop_" + name, new NodeList<>(expression));
        expression.getRange().ifPresent(call::setRange);
        return call;
    }

    private static Expression wrap(HtmlTemplate.Kind kind, String name, Expression expression, Node anchor, Position at) {
        String method = switch (kind) {
            case CHILD -> "child";
            case ATTR -> "attr";
            case BOOL -> "bool";
            case PROP -> "prop";
            case EVENT -> eventMethod(name);
        };
        if (expression.isNullLiteralExpr()) {
            if (kind == HtmlTemplate.Kind.BOOL) throw errorAt(anchor, at, "A ?attribute binding needs a boolean, not null");
            MethodCallExpr nothing = new MethodCallExpr(new NameExpr("Parts"), kind == HtmlTemplate.Kind.EVENT ? "eventNothing" : method + "Nothing");
            expression.getRange().ifPresent(nothing::setRange);
            return nothing;
        }
        MethodCallExpr call = new MethodCallExpr(new NameExpr("Parts"), method, new NodeList<>(expression));
        expression.getRange().ifPresent(call::setRange);
        return call;
    }

    /** The Parts method for an event binding, chosen by event name (docs/web-components.md, "Binding kinds"). */
    static String eventMethod(String event) {
        return switch (event) {
            case "click", "dblclick", "auxclick", "contextmenu", "mousedown", "mouseup", "mousemove",
                 "mouseover", "mouseout", "mouseenter", "mouseleave" -> "onMouse";
            case "keydown", "keyup" -> "onKeyboard";
            case "focus", "blur", "focusin", "focusout" -> "onFocus";
            case "beforeinput" -> "onInput";
            case "submit" -> "onSubmit";
            default -> event.startsWith("pointer") ? "onPointer" : "on";
        };
    }

    private Expression parse(Node anchor, String source, Position at, Position start) {
        ParseResult<Expression> parsed = parser.parseExpression(source);
        if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) {
            String detail = parsed.getProblems().isEmpty() ? "" : ": " + parsed.getProblems().get(0).getMessage().lines().findFirst().orElse("");
            throw errorAt(anchor, start, "Invalid binding expression '" + source.strip() + "'" + detail);
        }
        Expression expression = parsed.getResult().get();
        // Parsed positions are relative to the binding text; move them to the binding's place in the file.
        expression.walk(node -> node.getRange().ifPresent(r -> node.setRange(new Range(shift(r.begin, at), shift(r.end, at)))));
        return expression;
    }

    private static Position shift(Position p, Position at) {
        return p.line == 1 ? new Position(at.line, at.column + p.column - 1) : new Position(at.line + p.line - 1, p.column);
    }

    /** Source position of raw offset {@code offset}. Text block content starts on the line after the opening delimiter. */
    private static Position position(Position begin, String raw, int offset, boolean textBlock) {
        if (begin == null) return new Position(1, 1);
        int line = textBlock ? begin.line + 1 : begin.line;
        int column = textBlock ? 1 : begin.column + 1;
        for (int i = 0; i < offset && i < raw.length(); i++) {
            if (raw.charAt(i) == '\n') { line++; column = 1; } else column++;
        }
        return new Position(line, column);
    }

    /** An error at {@code at}, with the file name taken from {@code anchor}'s compilation unit. */
    static IllegalArgumentException errorAt(Node anchor, Position at, String message) {
        NameExpr marker = new NameExpr("html");
        marker.setRange(new Range(at, at));
        marker.setParentNode(anchor);
        try {
            return SourceExtension.error(marker, message);
        } finally {
            marker.setParentNode(null);
        }
    }

    String json() {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < definitions.size(); i++) {
            Definition d = definitions.get(i);
            if (i > 0) out.append(',');
            out.append("{\"id\":").append(d.id).append(",\"html\":").append(ComponentExtension.quoteJson(d.result.html())).append(",\"parts\":[");
            List<HtmlTemplate.Part> parts = d.result.parts();
            for (int j = 0; j < parts.size(); j++) {
                HtmlTemplate.Part p = parts.get(j);
                if (j > 0) out.append(',');
                out.append("{\"kind\":\"").append(p.kind().name().toLowerCase(java.util.Locale.ROOT)).append('"');
                if (p.name() != null) out.append(",\"name\":").append(ComponentExtension.quoteJson(p.name()));
                out.append(",\"bindings\":").append(p.bindings()).append('}');
            }
            out.append("]}");
        }
        return out.append(']').toString();
    }
}
