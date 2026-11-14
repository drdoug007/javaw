package one.dastec.javaw.components.compiler;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import one.dastec.javaw.spi.SourceExtension;

import java.util.*;

/**
 * Lowers {@code @CustomElement} classes for the javaw source compiler (docs/web-components.md, milestone M1).
 *
 * <ul>
 *   <li>validates component classes and tag names;</li>
 *   <li>adds the {@code WebComponent} implementation and one exported factory per component;</li>
 *   <li>rewrites {@code new X()} for component classes to creation through the custom element registry;</li>
 *   <li>rewrites reads of {@code @Query} fields to {@code renderRoot().querySelector(selector)};</li>
 *   <li>emits the {@code javaw_wc_components} custom section the runtime registers elements from.</li>
 * </ul>
 */
public final class ComponentExtension implements SourceExtension {
    static final String PACKAGE = "one.dastec.javaw.components";
    static final String SECTION = "javaw_wc_components";
    private static final String FACTORIES = "__JavawComponentFactories";
    private static final Set<String> RESERVED = Set.of("annotation-xml", "color-profile", "font-face", "font-face-src",
            "font-face-uri", "font-face-format", "font-face-name", "missing-glyph");

    private static final String WEB_COMPONENT = """
        abstract class WebComponent {
            private Element __host;
            private ShadowRoot __root;
            public WebComponent() {}
            protected void connected() {}
            protected void disconnected() {}
            protected void adopted() {}
            protected void attributeChanged(String name, String oldValue, String newValue) {}
            public Element host() { return __host; }
            public ShadowRoot renderRoot() { return __root; }
            public static void __wc_attach(WebComponent component, Element host, ShadowRoot root) {
                component.__host = host;
                component.__root = root;
            }
            public static void __wc_connected(WebComponent component) { component.connected(); }
            public static void __wc_disconnected(WebComponent component) { component.disconnected(); }
            public static void __wc_adopted(WebComponent component) { component.adopted(); }
            public static void __wc_attributeChanged(WebComponent component, String name, String oldValue, String newValue) {
                component.attributeChanged(name, oldValue, newValue);
            }
            protected Element createElement(String tag) { return __wc_createElement(__host, tag); }
            @JS("return runtime.createScopedElement(host, tag);")
            public static native Element __wc_createElement(Element host, String tag);
            @JS("return runtime.createComponent(tag);")
            public static native WebComponent __wc_create(String tag);
        }
        """;

    @Override
    public Set<String> packages() {
        return Set.of(PACKAGE);
    }

    @Override
    public void prepare(CompilationUnit cu, Output output) {
        boolean imported = cu.getImports().stream().anyMatch(i -> i.getNameAsString().equals(PACKAGE) || i.getNameAsString().startsWith(PACKAGE + "."));
        boolean qualified = normalizeQualifiedNames(cu);
        // A program with its own annotation type named CustomElement and no components import is not ours.
        boolean ownAnnotation = cu.findAll(AnnotationDeclaration.class).stream().anyMatch(a -> a.getNameAsString().equals("CustomElement"));
        if (!imported && !qualified) return;
        if (ownAnnotation)
            throw SourceExtension.error(cu.findAll(AnnotationDeclaration.class).get(0), "Type name 'CustomElement' is reserved by " + PACKAGE);
        List<ClassOrInterfaceDeclaration> components = new ArrayList<>();
        for (ClassOrInterfaceDeclaration type : cu.findAll(ClassOrInterfaceDeclaration.class))
            if (annotation(type, "CustomElement") != null) components.add(type);

        for (TypeDeclaration<?> type : cu.findAll(TypeDeclaration.class))
            if (type.getNameAsString().equals("WebComponent") || type.getNameAsString().equals(FACTORIES))
                throw SourceExtension.error(type, "Type name '" + type.getNameAsString() + "' is reserved by " + PACKAGE);

        Map<String, ClassOrInterfaceDeclaration> byTag = new LinkedHashMap<>();
        List<Definition> definitions = new ArrayList<>();
        for (ClassOrInterfaceDeclaration component : components) {
            Definition definition = validate(cu, component);
            ClassOrInterfaceDeclaration previous = byTag.putIfAbsent(definition.tag, component);
            if (previous != null)
                throw SourceExtension.error(component, "Custom element <" + definition.tag + "> is already defined by " + previous.getNameAsString());
            definitions.add(definition);
        }
        for (ClassOrInterfaceDeclaration component : components) lowerQueries(component);
        rewriteConstruction(cu, definitions);

        cu.getTypes().add(StaticJavaParser.parseTypeDeclaration(WEB_COMPONENT));
        if (!definitions.isEmpty()) {
            ClassOrInterfaceDeclaration factories = new ClassOrInterfaceDeclaration().setName(FACTORIES);
            for (Definition d : definitions)
                factories.addMethod(d.factory, com.github.javaparser.ast.Modifier.Keyword.PUBLIC, com.github.javaparser.ast.Modifier.Keyword.STATIC)
                        .setType("WebComponent")
                        .setBody(new BlockStmt().addStatement(StaticJavaParser.parseStatement("return new " + d.className + "();")));
            cu.getTypes().add(factories);
        }
        output.customSection(SECTION, json(definitions));
    }

    /** Rewrites one.dastec.javaw.components.X references to X; returns whether any existed. */
    private static boolean normalizeQualifiedNames(CompilationUnit cu) {
        boolean found = false;
        for (ClassOrInterfaceType type : new ArrayList<>(cu.findAll(ClassOrInterfaceType.class)))
            if (type.getScope().map(Object::toString).filter(PACKAGE::equals).isPresent()) { type.removeScope(); found = true; }
        for (AnnotationExpr annotation : cu.findAll(AnnotationExpr.class))
            if (annotation.getNameAsString().startsWith(PACKAGE + ".")) {
                annotation.setName(annotation.getNameAsString().substring(PACKAGE.length() + 1));
                found = true;
            }
        for (FieldAccessExpr access : new ArrayList<>(cu.findAll(FieldAccessExpr.class)))
            if (access.toString().startsWith(PACKAGE + ".") && access.getScope().toString().equals(PACKAGE)) {
                access.replace(new NameExpr(access.getNameAsString()));
                found = true;
            }
        return found;
    }

    private record Definition(String tag, String className, String factory, List<String> observedAttributes) {}

    private Definition validate(CompilationUnit cu, ClassOrInterfaceDeclaration component) {
        AnnotationExpr annotation = annotation(component, "CustomElement");
        String tag = stringMember(annotation, "value");
        if (tag == null) throw SourceExtension.error(annotation, "@CustomElement requires a tag name string literal");
        String problem = tagProblem(tag);
        if (problem != null) throw SourceExtension.error(annotation, "Invalid custom element name \"" + tag + "\": " + problem);
        if (component.isInterface()) throw SourceExtension.error(component, "@CustomElement requires a class");
        if (component.isAbstract()) throw SourceExtension.error(component, "@CustomElement class " + component.getNameAsString() + " must not be abstract");
        if (component.isNestedType())
            throw SourceExtension.error(component, "@CustomElement classes must be top-level for now: " + component.getNameAsString());
        if (!component.getTypeParameters().isEmpty())
            throw SourceExtension.error(component, "@CustomElement class " + component.getNameAsString() + " must not be generic");
        if (!extendsWebComponent(cu, component, new HashSet<>()))
            throw SourceExtension.error(component, "@CustomElement class " + component.getNameAsString() + " must extend WebComponent");
        boolean noArg = component.getConstructors().isEmpty()
                || component.getConstructors().stream().anyMatch(c -> c.getParameters().isEmpty() && !c.isPrivate());
        if (!noArg)
            throw SourceExtension.error(component, "@CustomElement class " + component.getNameAsString() + " needs a non-private no-argument constructor");
        List<String> observed = stringArrayMember(annotation, "observedAttributes");
        for (String name : observed)
            if (name.isEmpty() || !name.equals(name.toLowerCase(Locale.ROOT)) || name.chars().anyMatch(Character::isWhitespace))
                throw SourceExtension.error(annotation, "Observed attribute names must be non-empty, lowercase and without whitespace: \"" + name + "\"");
        return new Definition(tag, component.getNameAsString(), "__wc_new_" + component.getNameAsString(), List.copyOf(new LinkedHashSet<>(observed)));
    }

    /** Null when {@code name} is a valid custom element name (HTML "valid custom element name"). */
    static String tagProblem(String name) {
        if (name.isEmpty() || name.charAt(0) < 'a' || name.charAt(0) > 'z') return "it must start with an ASCII lowercase letter";
        if (name.indexOf('-') < 0) return "it must contain a hyphen";
        if (RESERVED.contains(name)) return "it is a reserved name";
        for (int i = 0; i < name.length(); ) {
            int c = name.codePointAt(i);
            i += Character.charCount(c);
            if (c >= 'A' && c <= 'Z') return "it must not contain ASCII uppercase letters";
            if (!pcenChar(c)) return "it contains the character U+" + String.format("%04X", c) + ", which is not allowed";
        }
        return null;
    }

    private static boolean pcenChar(int c) {
        return c == '-' || c == '.' || (c >= '0' && c <= '9') || c == '_' || (c >= 'a' && c <= 'z') || c == 0xB7
                || (c >= 0xC0 && c <= 0xD6) || (c >= 0xD8 && c <= 0xF6) || (c >= 0xF8 && c <= 0x37D)
                || (c >= 0x37F && c <= 0x1FFF) || (c >= 0x200C && c <= 0x200D) || (c >= 0x203F && c <= 0x2040)
                || (c >= 0x2070 && c <= 0x218F) || (c >= 0x2C00 && c <= 0x2FEF) || (c >= 0x3001 && c <= 0xD7FF)
                || (c >= 0xF900 && c <= 0xFDCF) || (c >= 0xFDF0 && c <= 0xFFFD) || (c >= 0x10000 && c <= 0xEFFFF);
    }

    private static boolean extendsWebComponent(CompilationUnit cu, ClassOrInterfaceDeclaration type, Set<String> seen) {
        if (!seen.add(type.getNameAsString())) return false;
        for (ClassOrInterfaceType parent : type.getExtendedTypes()) {
            String name = parent.getNameAsString();
            if (name.equals("WebComponent")) return true;
            for (ClassOrInterfaceDeclaration candidate : cu.findAll(ClassOrInterfaceDeclaration.class))
                if (candidate.getNameAsString().equals(name) && extendsWebComponent(cu, candidate, seen)) return true;
        }
        return false;
    }

    /** Reads of a {@code @Query} field become {@code this.renderRoot().querySelector(selector)}. */
    private static void lowerQueries(ClassOrInterfaceDeclaration component) {
        for (FieldDeclaration field : new ArrayList<>(component.getFields())) {
            AnnotationExpr query = annotation(field, "Query");
            if (query == null) continue;
            String selector = stringMember(query, "value");
            if (selector == null) throw SourceExtension.error(query, "@Query requires a selector string literal");
            if (field.getVariables().size() != 1) throw SourceExtension.error(field, "@Query must annotate a single field");
            VariableDeclarator variable = field.getVariable(0);
            String name = variable.getNameAsString();
            if (!field.isPrivate() || field.isStatic())
                throw SourceExtension.error(field, "@Query field '" + name + "' must be a private instance field");
            if (!variable.getTypeAsString().equals("Element") && !variable.getTypeAsString().endsWith(".Element"))
                throw SourceExtension.error(field, "@Query field '" + name + "' must have type Element");
            if (variable.getInitializer().isPresent())
                throw SourceExtension.error(field, "@Query field '" + name + "' must not have an initializer");

            for (AssignExpr assign : component.findAll(AssignExpr.class))
                if (refersToField(assign.getTarget(), name, component))
                    throw SourceExtension.error(assign, "@Query field '" + name + "' is read-only");
            List<Expression> reads = new ArrayList<>();
            for (NameExpr use : component.findAll(NameExpr.class)) if (refersToField(use, name, component)) reads.add(use);
            for (FieldAccessExpr use : component.findAll(FieldAccessExpr.class)) {
                if (!use.getNameAsString().equals(name)) continue;
                if (refersToField(use, name, component)) reads.add(use);
                else if (!use.getScope().isThisExpr() && !(use.getScope() instanceof NameExpr scope && isTypeName(scope, component)))
                    throw SourceExtension.error(use, "@Query field '" + name + "' can only be read as '" + name + "' or 'this." + name + "'");
            }
            for (Expression read : reads) {
                if (inStaticContext(read, component))
                    throw SourceExtension.error(read, "@Query field '" + name + "' cannot be read from a static context");
                read.replace(new MethodCallExpr(new MethodCallExpr(new ThisExpr(), "renderRoot"), "querySelector",
                        new NodeList<>(new StringLiteralExpr().setString(selector))));
            }
            field.remove();
        }
    }

    private static boolean isTypeName(NameExpr scope, ClassOrInterfaceDeclaration owner) {
        return Character.isUpperCase(scope.getNameAsString().charAt(0)) && owner.getFieldByName(scope.getNameAsString()).isEmpty();
    }

    /** True when {@code expression} reads the component's field {@code name}; throws for reads the lowering cannot handle. */
    private static boolean refersToField(Expression expression, String name, ClassOrInterfaceDeclaration owner) {
        if (expression instanceof FieldAccessExpr access) {
            if (!access.getNameAsString().equals(name) || !access.getScope().isThisExpr()) return false;
            ClassOrInterfaceDeclaration enclosing = access.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
            if (enclosing != owner) throw SourceExtension.error(access, "@Query field '" + name + "' cannot be read from a nested class");
            return true;
        }
        if (!(expression instanceof NameExpr use) || !use.getNameAsString().equals(name)) return false;
        ClassOrInterfaceDeclaration enclosing = use.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
        if (shadowed(use, name, owner)) return false;
        if (enclosing != owner) {
            if (enclosing != null && enclosing.getFieldByName(name).isPresent()) return false; // the nested class's own field
            throw SourceExtension.error(use, "@Query field '" + name + "' cannot be read from a nested class");
        }
        return true;
    }

    /** Java scoping for locals: declarations before the use in an enclosing block, plus loop, resource, catch and lambda variables. */
    private static boolean shadowed(NameExpr use, String name, ClassOrInterfaceDeclaration owner) {
        Node child = use;
        for (Node scope = use.getParentNode().orElse(null); scope != null && scope != owner; child = scope, scope = scope.getParentNode().orElse(null)) {
            if (scope instanceof CallableDeclaration<?> callable && callable.getParameters().stream().anyMatch(p -> p.getNameAsString().equals(name))) return true;
            if (scope instanceof LambdaExpr lambda && lambda.getParameters().stream().anyMatch(p -> p.getNameAsString().equals(name))) return true;
            if (scope instanceof com.github.javaparser.ast.stmt.CatchClause clause && clause.getParameter().getNameAsString().equals(name)) return true;
            if (scope instanceof com.github.javaparser.ast.stmt.ForEachStmt loop && loop.getVariableDeclarator().getNameAsString().equals(name)
                    && child != loop.getIterable()) return true;
            if (scope instanceof com.github.javaparser.ast.stmt.ForStmt loop && declares(loop.getInitialization(), name)) return true;
            if (scope instanceof com.github.javaparser.ast.stmt.TryStmt attempt) {
                if (attempt.getTryBlock().isAncestorOf(use) && declares(attempt.getResources(), name)) return true;
                int index = attempt.getResources().indexOf(child);
                if (index > 0 && declares(new NodeList<>(attempt.getResources().subList(0, index)), name)) return true;
            }
            if (scope instanceof VariableDeclarationExpr declaration && child instanceof VariableDeclarator current)
                // `Element q = null, b = q;`: earlier declarators (and the current one) are in scope in later initializers.
                for (VariableDeclarator v : declaration.getVariables()) {
                    if (v.getNameAsString().equals(name)) return true;
                    if (v == current) break;
                }
            if (scope instanceof com.github.javaparser.ast.stmt.SwitchStmt sw && child instanceof com.github.javaparser.ast.stmt.SwitchEntry entry
                    && entry.getType() == com.github.javaparser.ast.stmt.SwitchEntry.Type.STATEMENT_GROUP)
                // Old-style case groups share one block: declarations in earlier groups are in scope.
                for (com.github.javaparser.ast.stmt.SwitchEntry earlier : sw.getEntries()) {
                    if (earlier == entry) break;
                    for (com.github.javaparser.ast.stmt.Statement statement : earlier.getStatements())
                        if (statement instanceof com.github.javaparser.ast.stmt.ExpressionStmt expression
                                && expression.getExpression() instanceof VariableDeclarationExpr declaration
                                && declaration.getVariables().stream().anyMatch(v -> v.getNameAsString().equals(name))) return true;
                }
            NodeList<com.github.javaparser.ast.stmt.Statement> statements =
                    scope instanceof BlockStmt block ? block.getStatements()
                    : scope instanceof com.github.javaparser.ast.stmt.SwitchEntry entry ? entry.getStatements() : null;
            if (statements != null)
                for (com.github.javaparser.ast.stmt.Statement statement : statements) {
                    if (statement == child) break;
                    if (statement instanceof com.github.javaparser.ast.stmt.ExpressionStmt expression
                            && expression.getExpression() instanceof VariableDeclarationExpr declaration
                            && declaration.getVariables().stream().anyMatch(v -> v.getNameAsString().equals(name))) return true;
                }
            if (scope instanceof BodyDeclaration<?> member && member.getParentNode().orElse(null) == owner) {
                // Pattern variables are flow-scoped; do not guess (methods, constructors, initializers, field initializers).
                boolean pattern = member.findAll(PatternExpr.class).stream().anyMatch(p -> p.getNameAsString().equals(name));
                if (pattern) throw SourceExtension.error(use, "Pattern variable '" + name + "' has the same name as a @Query field; rename one of them");
            }
        }
        return false;
    }

    private static boolean declares(NodeList<Expression> expressions, String name) {
        for (Expression e : expressions)
            if (e instanceof VariableDeclarationExpr declaration && declaration.getVariables().stream().anyMatch(v -> v.getNameAsString().equals(name)))
                return true;
        return false;
    }

    private static boolean inStaticContext(Node node, ClassOrInterfaceDeclaration owner) {
        for (Node scope = node.getParentNode().orElse(null); scope != null && scope != owner; scope = scope.getParentNode().orElse(null)) {
            if (scope instanceof MethodDeclaration method) return method.isStatic();
            if (scope instanceof InitializerDeclaration initializer) return initializer.isStatic();
            if (scope instanceof FieldDeclaration field) return field.isStatic();
        }
        return false;
    }

    /** {@code new X()} for a component class creates the element, whose constructor creates the Java object. */
    private static void rewriteConstruction(CompilationUnit cu, List<Definition> definitions) {
        Map<String, Definition> byClass = new HashMap<>();
        for (Definition d : definitions) byClass.put(d.className, d);
        for (MethodReferenceExpr reference : cu.findAll(MethodReferenceExpr.class))
            if (reference.getIdentifier().equals("new") && reference.getScope() instanceof TypeExpr type
                    && type.getType() instanceof ClassOrInterfaceType named
                    && (byClass.containsKey(named.getNameAsString()) || webComponentSubclass(cu, named.getNameAsString())))
                throw SourceExtension.error(reference, "Create components with new " + named.getNameAsString() + "() (for example () -> new "
                        + named.getNameAsString() + "()), not a constructor reference");
        for (ObjectCreationExpr creation : new ArrayList<>(cu.findAll(ObjectCreationExpr.class))) {
            Definition d = byClass.get(creation.getType().getNameAsString());
            if (d == null) {
                if (webComponentSubclass(cu, creation.getType().getNameAsString()))
                    throw SourceExtension.error(creation, creation.getType().getNameAsString()
                            + " extends WebComponent but has no @CustomElement, so it has no element; annotate it or create a component class");
                continue;
            }
            if (!creation.getArguments().isEmpty() || creation.getAnonymousClassBody().isPresent())
                throw SourceExtension.error(creation, "Components are created without arguments: new " + d.className + "()");
            MethodCallExpr create = new MethodCallExpr(new NameExpr("WebComponent"), "__wc_create", new NodeList<>(new StringLiteralExpr(d.tag)));
            // As a statement, `new X();` only needs the element created; a cast expression would leave a value behind.
            if (creation.getParentNode().orElse(null) instanceof com.github.javaparser.ast.stmt.ExpressionStmt) creation.replace(create);
            else creation.replace(new EnclosedExpr(new CastExpr(StaticJavaParser.parseClassOrInterfaceType(d.className), create)));
        }
    }

    private static boolean webComponentSubclass(CompilationUnit cu, String name) {
        for (ClassOrInterfaceDeclaration type : cu.findAll(ClassOrInterfaceDeclaration.class))
            if (type.getNameAsString().equals(name)) return extendsWebComponent(cu, type, new HashSet<>());
        return false;
    }

    private static AnnotationExpr annotation(com.github.javaparser.ast.nodeTypes.NodeWithAnnotations<?> node, String simpleName) {
        for (AnnotationExpr a : node.getAnnotations()) {
            String name = a.getNameAsString();
            if (name.equals(simpleName) || name.equals(PACKAGE + "." + simpleName)) return a;
        }
        return null;
    }

    private static String stringMember(AnnotationExpr annotation, String member) {
        Expression value = member(annotation, member);
        return value instanceof StringLiteralExpr literal ? literal.asString() : null;
    }

    private static List<String> stringArrayMember(AnnotationExpr annotation, String member) {
        Expression value = member(annotation, member);
        if (value == null) return List.of();
        List<Expression> items = value instanceof ArrayInitializerExpr array ? array.getValues() : List.of(value);
        List<String> result = new ArrayList<>();
        for (Expression item : items) {
            if (!(item instanceof StringLiteralExpr literal))
                throw SourceExtension.error(item, "@CustomElement." + member + " must list string literals");
            result.add(literal.asString());
        }
        return result;
    }

    private static Expression member(AnnotationExpr annotation, String member) {
        if (annotation instanceof SingleMemberAnnotationExpr single) return member.equals("value") ? single.getMemberValue() : null;
        if (annotation instanceof NormalAnnotationExpr normal)
            for (MemberValuePair pair : normal.getPairs()) if (pair.getNameAsString().equals(member)) return pair.getValue();
        return null;
    }

    private static String json(List<Definition> definitions) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < definitions.size(); i++) {
            Definition d = definitions.get(i);
            if (i > 0) out.append(',');
            out.append("{\"tag\":").append(quote(d.tag)).append(",\"className\":").append(quote(d.className))
                    .append(",\"factory\":").append(quote(d.factory)).append(",\"observedAttributes\":[");
            for (int j = 0; j < d.observedAttributes.size(); j++) out.append(j > 0 ? "," : "").append(quote(d.observedAttributes.get(j)));
            out.append("]}");
        }
        return out.append(']').toString();
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
}
