package one.dastec.javaw.components.compiler;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import one.dastec.javaw.spi.SourceExtension;

import java.util.*;

/**
 * M5 styles (docs/web-components.md, "Styles"): validates {@code css(...)} calls and finds each component's
 * style fields.
 *
 * <p>Every {@code css(literal)} call site gets an id; its processed text goes into the
 * {@code javaw_wc_styles} section, and the runtime builds one shared {@code CSSStyleSheet} per id. The
 * calls themselves are not rewritten: {@code css} returns a {@code Css} value holding the text in Wasm.
 * A component's style list is the call sites of the {@code static final Css} fields of its class and its
 * superclasses, superclass first and then in declaration order; a call site listed twice is kept at its
 * last position, as Lit's {@code finalizeStyles} does, so it keeps its place in the cascade.
 */
final class ComponentStyles {
    static final String SECTION = "javaw_wc_styles";

    private final CompilationUnit cu;
    private final IdentityHashMap<MethodCallExpr, Integer> ids = new IdentityHashMap<>();
    private final List<String> texts = new ArrayList<>();
    private final Set<Integer> used = new TreeSet<>();

    private ComponentStyles(CompilationUnit cu) {
        this.cu = cu;
    }

    /** Validates every css(...) call of {@code cu}. */
    static ComponentStyles scan(CompilationUnit cu) {
        ComponentStyles styles = new ComponentStyles(cu);
        for (MethodCallExpr call : cu.findAll(MethodCallExpr.class))
            if (call.getNameAsString().equals("css") && TemplateRewrite.resolvesToTemplates(call, call, "css"))
                styles.ids.put(call, styles.register(call));
        return styles;
    }

    /**
     * Qualifies every css call as {@code Templates.css}, which the source compiler resolves without static
     * imports; returns whether there were any.
     */
    boolean qualifyCalls() {
        for (MethodCallExpr call : ids.keySet()) call.setScope(new NameExpr("Templates"));
        if (!ids.isEmpty() && cu.getImports().stream().noneMatch(i -> !i.isStatic()
                && (i.getNameAsString().equals(ComponentExtension.PACKAGE + ".Templates")
                    || (i.isAsterisk() && i.getNameAsString().equals(ComponentExtension.PACKAGE)))))
            cu.addImport(ComponentExtension.PACKAGE + ".Templates");
        return !ids.isEmpty();
    }

    private int register(MethodCallExpr call) {
        if (call.getArguments().size() != 1)
            throw SourceExtension.error(call, "css(...) takes exactly one string literal or text block");
        Expression argument = call.getArgument(0);
        boolean textBlock = argument.isTextBlockLiteralExpr();
        if (!textBlock && !argument.isStringLiteralExpr())
            throw SourceExtension.error(argument, "css(...) requires a string literal or text block, not an expression;"
                    + " there is no unsafeCSS, so style text must be a compile-time literal");
        String raw = textBlock ? argument.asTextBlockLiteralExpr().getValue() : argument.asStringLiteralExpr().getValue();
        if (raw.contains("${"))
            throw SourceExtension.error(argument, "css(...) does not support ${...} bindings; use a separate style field,"
                    + " or CSS custom properties set from the template");
        String text;
        if (textBlock) {
            // JavaParser keeps whitespace after the opening delimiter and that line's terminator; javac drops them.
            int nl = raw.indexOf('\n');
            if (nl >= 0 && raw.substring(0, nl).isBlank()) raw = raw.substring(nl + 1);
            text = raw.stripIndent().translateEscapes();
        } else {
            text = raw.translateEscapes();
        }
        texts.add(text);
        return texts.size() - 1;
    }

    /** The style call-site ids of {@code component}: superclass fields first, then declaration order, duplicates kept last. */
    List<Integer> stylesOf(ClassOrInterfaceDeclaration component) {
        Deque<ClassOrInterfaceDeclaration> chain = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        for (ClassOrInterfaceDeclaration c = component; c != null && seen.add(c.getNameAsString()); c = superclass(c))
            chain.addFirst(c);
        List<Integer> all = new ArrayList<>();
        for (ClassOrInterfaceDeclaration c : chain)
            for (FieldDeclaration field : c.getFields()) {
                if (!isCss(field)) continue;
                for (VariableDeclarator v : field.getVariables()) {
                    if (v.getType().isArrayType())
                        throw SourceExtension.error(field, "Style field '" + v.getNameAsString() + "' must have type Css; declare one field per style sheet");
                    if (!field.isStatic() || !field.isFinal())
                        throw SourceExtension.error(field, "Style field '" + v.getNameAsString() + "' of " + c.getNameAsString()
                                + " must be static final");
                    all.add(resolve(c, v, new LinkedHashSet<>()));
                }
            }
        LinkedHashSet<Integer> last = new LinkedHashSet<>();
        for (int i = all.size() - 1; i >= 0; i--) last.add(all.get(i));
        List<Integer> result = new ArrayList<>(last);
        Collections.reverse(result);
        used.addAll(result);
        return result;
    }

    private static boolean isCss(FieldDeclaration field) {
        String type = field.getElementType().asString();
        return type.equals("Css") || type.equals(ComponentExtension.PACKAGE + ".Css");
    }

    /** The css call site that {@code variable} of {@code owner} is initialized with, following field references. */
    private int resolve(ClassOrInterfaceDeclaration owner, VariableDeclarator variable, Set<String> path) {
        String name = owner.getNameAsString() + "." + variable.getNameAsString();
        if (!path.add(name)) throw SourceExtension.error(variable, "Style field " + name + " refers to itself");
        Expression init = variable.getInitializer().orElseThrow(() -> SourceExtension.error(variable,
                "Style field '" + variable.getNameAsString() + "' needs an initializer: css(\"...\") or another style field"));
        while (init.isEnclosedExpr()) init = init.asEnclosedExpr().getInner();
        if (init instanceof MethodCallExpr call && ids.containsKey(call)) return ids.get(call);
        ClassOrInterfaceDeclaration target = null;
        String field = null;
        if (init instanceof NameExpr ref) {
            field = ref.getNameAsString();
            // A field of the declaring class or, as Java resolves simple names, of one of its superclasses.
            for (ClassOrInterfaceDeclaration c = owner; c != null && target == null; c = superclass(c))
                if (c.getFieldByName(field).isPresent()) target = c;
        } else if (init instanceof FieldAccessExpr access && access.getScope() instanceof NameExpr scope) {
            field = access.getNameAsString();
            target = type(scope.getNameAsString());
        }
        if (target != null) {
            FieldDeclaration declaration = target.getFieldByName(field).orElse(null);
            if (declaration != null && isCss(declaration) && declaration.isStatic() && declaration.isFinal())
                for (VariableDeclarator v : declaration.getVariables())
                    if (v.getNameAsString().equals(field)) return resolve(target, v, path);
        }
        throw SourceExtension.error(init, "Style field '" + variable.getNameAsString() + "' must be initialized with css(\"...\")"
                + " or a reference to another static final Css field of this module");
    }

    private ClassOrInterfaceDeclaration superclass(ClassOrInterfaceDeclaration c) {
        return c.getExtendedTypes().isEmpty() ? null : type(c.getExtendedTypes().get(0).getNameAsString());
    }

    private ClassOrInterfaceDeclaration type(String name) {
        for (ClassOrInterfaceDeclaration c : cu.findAll(ClassOrInterfaceDeclaration.class))
            if (c.getNameAsString().equals(name) && !c.isInterface()) return c;
        return null;
    }

    boolean isEmpty() { return used.isEmpty(); }

    /** {@code [{"id":n,"css":"..."}]} for the call sites some component uses. */
    String json() {
        StringBuilder out = new StringBuilder("[");
        for (int id : used) {
            if (out.length() > 1) out.append(',');
            out.append("{\"id\":").append(id).append(",\"css\":").append(ComponentExtension.quoteJson(texts.get(id))).append('}');
        }
        return out.append(']').toString();
    }
}
