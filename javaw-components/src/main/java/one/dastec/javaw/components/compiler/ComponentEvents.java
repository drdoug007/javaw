package one.dastec.javaw.components.compiler;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import one.dastec.javaw.spi.SourceExtension;

import java.util.*;

/**
 * M5 events (docs/web-components.md, "Events"): {@code ComponentEvent} records, their names and options,
 * the events each component declares, and the typed {@code @name=${...}} bindings on Java components.
 *
 * <p>The source compiler represents records as flattened values, which cannot be held as {@code Object}
 * (Todo stage 1). An event record has to be held by reference while it is dispatched, so each
 * {@code ComponentEvent} record is lowered to a final class with the record's fields, canonical
 * constructor, accessors, {@code equals}, {@code hashCode} and {@code toString}, plus three generated
 * methods: the event name, the option flags and the code that builds the JS {@code detail} object.
 */
final class ComponentEvents {
    static final int BUBBLES = 1, COMPOSED = 2, CANCELABLE = 4;

    /** An event record: its key (1-based), source declaration, simple and qualified name, DOM name and flags. */
    record Event(int key, RecordDeclaration declaration, String simpleName, String qualifiedName, String name, int flags) {}

    private static final Set<String> PRIMITIVES = Set.of("boolean", "byte", "short", "int", "long", "char", "float", "double");
    private static final Map<String, String> BOXES = Map.of("Boolean", "boolean", "Byte", "byte", "Short", "short",
            "Integer", "int", "Long", "long", "Character", "char", "Float", "float", "Double", "double");

    private static final java.util.regex.Pattern DISPATCH_NEW =
            java.util.regex.Pattern.compile("(?<![\\w.])dispatch\\s*\\(\\s*new\\s+([A-Za-z_$][\\w$.]*)\\s*\\(");

    private final CompilationUnit cu;
    private final Map<String, Event> bySimpleName = new LinkedHashMap<>();
    private final Set<String> enums = new HashSet<>();

    private ComponentEvents(CompilationUnit cu) {
        this.cu = cu;
    }

    boolean isEmpty() { return bySimpleName.isEmpty(); }

    Collection<Event> events() { return bySimpleName.values(); }

    /** Finds and validates every ComponentEvent record of {@code cu}. */
    static ComponentEvents scan(CompilationUnit cu) {
        ComponentEvents events = new ComponentEvents(cu);
        cu.findAll(EnumDeclaration.class).forEach(e -> events.enums.add(e.getNameAsString()));
        for (TypeDeclaration<?> type : cu.findAll(TypeDeclaration.class)) {
            boolean implementsEvent = implementedTypes(type).stream().anyMatch(t -> t.getNameAsString().equals("ComponentEvent"));
            if (!implementsEvent) {
                if (ComponentExtension.annotationOf(type, "EventName") != null || ComponentExtension.annotationOf(type, "EventOptions") != null)
                    throw SourceExtension.error(type, "@EventName and @EventOptions belong on a record that implements ComponentEvent");
                continue;
            }
            if (!(type instanceof RecordDeclaration record))
                throw SourceExtension.error(type, "ComponentEvent must be implemented by a record: " + type.getNameAsString());
            if (!record.getTypeParameters().isEmpty())
                throw SourceExtension.error(record, "Event record " + record.getNameAsString() + " must not be generic");
            String simple = record.getNameAsString();
            if (events.bySimpleName.containsKey(simple))
                throw SourceExtension.error(record, "Two event records are named " + simple + "; give them different names");
            events.bySimpleName.put(simple, new Event(events.bySimpleName.size() + 1, record, simple, qualifiedName(record),
                    eventName(record), flags(record)));
        }
        for (Event e : events.bySimpleName.values())
            for (Parameter p : e.declaration().getParameters()) events.checkComponent(e, p);
        return events;
    }

    private static List<ClassOrInterfaceType> implementedTypes(TypeDeclaration<?> type) {
        if (type instanceof RecordDeclaration r) return r.getImplementedTypes();
        if (type instanceof EnumDeclaration e) return e.getImplementedTypes();
        if (type instanceof ClassOrInterfaceDeclaration c) {
            List<ClassOrInterfaceType> all = new ArrayList<>(c.getImplementedTypes());
            if (c.isInterface()) all.addAll(c.getExtendedTypes());
            return all;
        }
        return List.of();
    }

    private static String qualifiedName(TypeDeclaration<?> type) {
        StringBuilder name = new StringBuilder(type.getNameAsString());
        for (Node n = type.getParentNode().orElse(null); n != null; n = n.getParentNode().orElse(null))
            if (n instanceof TypeDeclaration<?> outer) name.insert(0, outer.getNameAsString() + ".");
        return name.toString();
    }

    private static String eventName(RecordDeclaration record) {
        AnnotationExpr annotation = ComponentExtension.annotationOf(record, "EventName");
        if (annotation != null) {
            String name = ComponentExtension.stringMemberOf(annotation, "value");
            if (name == null) throw SourceExtension.error(annotation, "@EventName requires a string literal");
            if (name.isEmpty() || name.chars().anyMatch(Character::isWhitespace))
                throw SourceExtension.error(annotation, "@EventName must be non-empty and without whitespace: \"" + name + "\"");
            return name;
        }
        return kebab(record.getNameAsString());
    }

    /** {@code RemainingChanged} is {@code remaining-changed}; an {@code Event} suffix is dropped ({@code ItemSelectedEvent}). */
    static String kebab(String simpleName) {
        String name = simpleName.endsWith("Event") && simpleName.length() > "Event".length()
                ? simpleName.substring(0, simpleName.length() - "Event".length()) : simpleName;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                char before = name.charAt(i - 1);
                boolean nextLower = i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1));
                // A word starts after a lowercase letter or digit, and at the last capital of an acronym (URLChanged).
                if (Character.isLowerCase(before) || Character.isDigit(before) || (Character.isUpperCase(before) && nextLower))
                    out.append('-');
            }
            out.append(Character.toLowerCase(c));
        }
        return out.toString();
    }

    private static int flags(RecordDeclaration record) {
        AnnotationExpr options = ComponentExtension.annotationOf(record, "EventOptions");
        boolean bubbles = true, composed = true, cancelable = false;
        if (options != null) {
            if (options instanceof SingleMemberAnnotationExpr)
                throw SourceExtension.error(options, "@EventOptions takes named members: bubbles, composed, cancelable");
            if (options instanceof NormalAnnotationExpr normal)
                for (MemberValuePair pair : normal.getPairs()) {
                    if (!(pair.getValue() instanceof BooleanLiteralExpr literal))
                        throw SourceExtension.error(pair.getValue(), "@EventOptions." + pair.getNameAsString() + " must be a boolean literal");
                    switch (pair.getNameAsString()) {
                        case "bubbles" -> bubbles = literal.getValue();
                        case "composed" -> composed = literal.getValue();
                        case "cancelable" -> cancelable = literal.getValue();
                        default -> throw SourceExtension.error(pair, "@EventOptions has no member " + pair.getNameAsString());
                    }
                }
        }
        return (bubbles ? BUBBLES : 0) | (composed ? COMPOSED : 0) | (cancelable ? CANCELABLE : 0);
    }

    private static String simpleTypeName(String type) {
        int dot = type.lastIndexOf('.');
        return dot < 0 ? type : type.substring(dot + 1);
    }

    private void checkComponent(Event event, Parameter p) {
        if (p.isVarArgs())
            throw SourceExtension.error(p, "Event record " + event.simpleName() + " must not have a varargs component");
        String type = p.getTypeAsString();
        String simple = simpleTypeName(type);
        if (PRIMITIVES.contains(type) || BOXES.containsKey(simple) || simple.equals("String") || simple.equals("Element")
                || enums.contains(simple) || bySimpleName.containsKey(simple))
            return;
        throw SourceExtension.error(p, "Component '" + p.getNameAsString() + "' of event record " + event.simpleName() + " has type " + type
                + ", which event detail does not support yet; use primitives, their boxes, String, enums, Element or other event records");
    }

    /** The event record named by a type written in source ({@code RemainingChanged}, {@code TodoList.RemainingChanged}), or null. */
    Event find(String type) {
        return bySimpleName.get(simpleTypeName(type));
    }

    /**
     * The events {@code component} declares: the records of its {@code dispatch(new X(...))} calls and its
     * {@code @Fires} list, also from component superclasses, by DOM name.
     */
    Map<String, Event> declaredBy(ClassOrInterfaceDeclaration component, Map<String, ClassOrInterfaceDeclaration> componentsByName) {
        Map<String, Event> declared = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (ClassOrInterfaceDeclaration c = component; c != null && seen.add(c.getNameAsString());
             c = c.getExtendedTypes().isEmpty() ? null : componentsByName.get(c.getExtendedTypes().get(0).getNameAsString())) {
            AnnotationExpr fires = ComponentExtension.annotationOf(c, "Fires");
            if (fires != null) {
                Expression value = fires instanceof SingleMemberAnnotationExpr single ? single.getMemberValue()
                        : fires instanceof NormalAnnotationExpr normal ? normal.getPairs().stream().filter(pair -> pair.getNameAsString().equals("value"))
                            .map(MemberValuePair::getValue).findFirst().orElse(null) : null;
                if (value == null) throw SourceExtension.error(fires, "@Fires lists event record classes, as in @Fires(Changed.class)");
                List<Expression> items = value instanceof ArrayInitializerExpr array ? array.getValues() : List.of(value);
                for (Expression item : items) {
                    Event event = item instanceof ClassExpr classExpr ? find(classExpr.getType().asString()) : null;
                    if (event == null)
                        throw SourceExtension.error(item, "@Fires lists records of this module that implement ComponentEvent; " + item + " is not one");
                    declare(declared, component, event, item);
                }
            }
            for (MethodCallExpr call : c.findAll(MethodCallExpr.class)) {
                if (!call.getNameAsString().equals("dispatch") || call.getArguments().size() != 1) continue;
                if (call.getScope().isPresent() && !call.getScope().get().isThisExpr()) continue;
                Event event = null;
                if (call.getArgument(0) instanceof ObjectCreationExpr creation) event = find(creation.getType().asString());
                else if (call.getArgument(0) instanceof NameExpr variable) event = localType(call, variable.getNameAsString());
                if (event != null) declare(declared, component, event, call);
            }
            // Calls written in template bindings are still text when the declared events are collected.
            for (LiteralStringValueExpr literal : c.findAll(LiteralStringValueExpr.class)) {
                if (!(literal instanceof StringLiteralExpr) && !(literal instanceof TextBlockLiteralExpr)) continue;
                java.util.regex.Matcher m = DISPATCH_NEW.matcher(literal.getValue());
                while (m.find()) {
                    Event event = find(m.group(1));
                    if (event != null) declare(declared, component, event, literal);
                }
            }
        }
        return declared;
    }

    /** The event record type of the local variable or parameter {@code name} visible at {@code call}'s method, or null. */
    private Event localType(MethodCallExpr call, String name) {
        CallableDeclaration<?> callable = call.findAncestor(CallableDeclaration.class).orElse(null);
        if (callable == null) return null;
        for (Parameter p : callable.getParameters()) if (p.getNameAsString().equals(name)) return find(p.getTypeAsString());
        for (VariableDeclarator v : callable.findAll(VariableDeclarator.class))
            if (v.getNameAsString().equals(name)) return find(v.getTypeAsString());
        return null;
    }

    private static void declare(Map<String, Event> declared, ClassOrInterfaceDeclaration component, Event event, Node at) {
        Event previous = declared.putIfAbsent(event.name(), event);
        if (previous != null && previous != event)
            throw SourceExtension.error(at, component.getNameAsString() + " declares two events named '" + event.name() + "': "
                    + previous.simpleName() + " and " + event.simpleName());
    }

    /** Replaces each event record with an equivalent final class (see the class comment). */
    void lower() {
        for (Event event : bySimpleName.values()) event.declaration().replace(lowered(event));
    }

    private ClassOrInterfaceDeclaration lowered(Event event) {
        RecordDeclaration record = event.declaration();
        String name = record.getNameAsString();
        NodeList<Modifier> modifiers = new NodeList<>();
        for (Modifier m : record.getModifiers()) if (m.getKeyword() != Modifier.Keyword.STATIC && m.getKeyword() != Modifier.Keyword.FINAL) modifiers.add(m.clone());
        if (record.isNestedType()) modifiers.add(Modifier.staticModifier());
        modifiers.add(Modifier.finalModifier());
        ClassOrInterfaceDeclaration type = new ClassOrInterfaceDeclaration(modifiers, false, name);
        record.getRange().ifPresent(type::setRange);
        for (AnnotationExpr a : record.getAnnotations())
            if (!Set.of("EventName", "EventOptions").contains(simpleTypeName(a.getNameAsString()))) type.addAnnotation(a.clone());
        for (ClassOrInterfaceType t : record.getImplementedTypes()) type.addImplementedType(t.clone());

        List<Parameter> components = record.getParameters();
        for (Parameter p : components) {
            FieldDeclaration field = new FieldDeclaration(new NodeList<>(Modifier.privateModifier(), Modifier.finalModifier()),
                    p.getType().clone(), p.getNameAsString());
            p.getRange().ifPresent(field::setRange);
            type.addMember(field);
        }
        boolean canonical = false;
        for (BodyDeclaration<?> member : record.getMembers()) {
            if (member instanceof CompactConstructorDeclaration compact) {
                type.addMember(constructor(name, components, compact.getBody().clone(), compact));
                canonical = true;
            } else {
                if (member instanceof ConstructorDeclaration c && sameParameters(c.getParameters(), components)) canonical = true;
                type.addMember(member.clone());
            }
        }
        if (!canonical) type.addMember(constructor(name, components, new BlockStmt(), record));

        for (Parameter p : components)
            if (record.getMethodsBySignature(p.getNameAsString()).isEmpty())
                type.addMember(StaticJavaParser.parseBodyDeclaration("public " + p.getTypeAsString() + " " + p.getNameAsString()
                        + "() { return this." + p.getNameAsString() + "; }"));
        if (record.getMethodsBySignature("equals", "Object").isEmpty())
            type.addMember(StaticJavaParser.parseBodyDeclaration(equalsMethod(name, components)));
        if (record.getMethodsBySignature("hashCode").isEmpty())
            type.addMember(StaticJavaParser.parseBodyDeclaration(hashCodeMethod(components)));
        if (record.getMethodsBySignature("toString").isEmpty())
            type.addMember(StaticJavaParser.parseBodyDeclaration(toStringMethod(name, components)));

        type.addMember(StaticJavaParser.parseBodyDeclaration("public String __wc_type() { return " + ComponentExtension.quoteJson(event.name()) + "; }"));
        type.addMember(StaticJavaParser.parseBodyDeclaration("public int __wc_flags() { return " + event.flags() + "; }"));
        StringBuilder fill = new StringBuilder("public void __wc_fill() { ");
        for (Parameter p : components) fill.append(detail(p)).append(' ');
        type.addMember(StaticJavaParser.parseBodyDeclaration(fill.append('}').toString()));
        return type;
    }

    private static boolean sameParameters(List<Parameter> a, List<Parameter> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (!a.get(i).getType().asString().equals(b.get(i).getType().asString())) return false;
        return true;
    }

    private static ConstructorDeclaration constructor(String name, List<Parameter> components, BlockStmt body, Node at) {
        ConstructorDeclaration constructor = new ConstructorDeclaration(new NodeList<>(Modifier.publicModifier()), name);
        for (Parameter p : components) constructor.addParameter(new Parameter(p.getType().clone(), p.getNameAsString()));
        // A compact constructor's body runs first and may reassign the parameters; then the fields are assigned.
        NodeList<Statement> statements = new NodeList<>(body.getStatements());
        for (Parameter p : components)
            statements.add(StaticJavaParser.parseStatement("this." + p.getNameAsString() + " = " + p.getNameAsString() + ";"));
        constructor.setBody(new BlockStmt(statements));
        at.getRange().ifPresent(constructor::setRange);
        return constructor;
    }

    private static String equalsMethod(String name, List<Parameter> components) {
        StringBuilder out = new StringBuilder("public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof ")
                .append(name).append(")) return false; ").append(name).append(" other = (").append(name).append(") o; ");
        for (Parameter p : components) {
            String f = p.getNameAsString(), type = p.getTypeAsString();
            String same;
            if (type.equals("double") || type.equals("float"))
                // Double.compare == 0, as records compare floating-point components: NaN equals NaN, 0.0 differs from -0.0.
                same = "(" + f + " == other." + f + " ? (" + f + " != 0.0 || 1.0 / " + f + " == 1.0 / other." + f + ") : ("
                        + f + " != " + f + " && other." + f + " != other." + f + "))";
            else if (PRIMITIVES.contains(type)) same = f + " == other." + f;
            else same = "(" + f + " == null ? other." + f + " == null : " + f + ".equals(other." + f + "))";
            out.append("if (!").append(same.startsWith("(") ? same : "(" + same + ")").append(") return false; ");
        }
        return out.append("return true; }").toString();
    }

    private static String hashCodeMethod(List<Parameter> components) {
        StringBuilder out = new StringBuilder("public int hashCode() { int h = 0; ");
        for (Parameter p : components) {
            String f = p.getNameAsString(), type = p.getTypeAsString();
            String hash = switch (type) {
                case "boolean" -> "(" + f + " ? 1231 : 1237)";
                case "long" -> "(int) (" + f + " ^ (" + f + " >>> 32))";
                // Consistent with equals: NaN and both zeros hash to 0.
                case "float", "double" -> "(int) " + f;
                case "int", "short", "byte", "char" -> "(int) " + f;
                default -> "(" + f + " == null ? 0 : " + f + ".hashCode())";
            };
            out.append("h = 31 * h + ").append(hash).append("; ");
        }
        return out.append("return h; }").toString();
    }

    private static String toStringMethod(String name, List<Parameter> components) {
        StringBuilder out = new StringBuilder("public String toString() { return \"").append(name).append("[");
        for (int i = 0; i < components.size(); i++) {
            String f = components.get(i).getNameAsString();
            out.append(i > 0 ? ", " : "").append(f).append("=\" + ").append(f).append(" + \"");
        }
        return out.append("]\"; }").toString();
    }

    /** Statements that add component {@code p} to the detail object being built (docs/web-components.md, "Property marshalling"). */
    private String detail(Parameter p) {
        String f = p.getNameAsString(), key = ComponentExtension.quoteJson(f), type = p.getTypeAsString();
        String simple = simpleTypeName(type);
        String primitive = PRIMITIVES.contains(type) ? type : BOXES.get(simple);
        if (primitive != null) {
            String value = BOXES.containsKey(simple) ? f + "." + primitive + "Value()" : f;
            String set = switch (primitive) {
                case "boolean" -> "WebComponent.__wc_detailBool(" + key + ", " + value + ");";
                case "long" -> "WebComponent.__wc_detailBigInt(" + key + ", String.valueOf(" + value + "));";
                case "char" -> "WebComponent.__wc_detailString(" + key + ", String.valueOf(" + value + "), true);";
                default -> "WebComponent.__wc_detailNumber(" + key + ", (double) " + value + ");";
            };
            return PRIMITIVES.contains(type) ? set : "if (" + f + " == null) WebComponent.__wc_detailNull(" + key + "); else " + set;
        }
        if (simple.equals("String"))
            return "WebComponent.__wc_detailString(" + key + ", " + f + " == null ? \"\" : " + f + ", " + f + " != null);";
        if (simple.equals("Element")) return "WebComponent.__wc_detailElement(" + key + ", " + f + ");";
        if (enums.contains(simple))
            return "if (" + f + " == null) WebComponent.__wc_detailNull(" + key + "); else WebComponent.__wc_detailString(" + key
                    + ", WebComponent.__wc_kebab(" + f + ".name()), true);";
        // Another event record: a nested plain object.
        return "if (" + f + " == null) WebComponent.__wc_detailNull(" + key + "); else { WebComponent.__wc_detailEnter(); "
                + f + ".__wc_fill(); WebComponent.__wc_detailExit(" + key + "); }";
    }

    static final String INTERFACE = """
        interface ComponentEvent {
            public String __wc_type();
            public int __wc_flags();
            public void __wc_fill();
        }
        """;

    /** Members of the generated WebComponent for dispatch and listen. */
    static final String COMPONENT_MEMBERS = """
            private ArrayList<__WcListen> __listens;
            private static Object[] __wc_firing;
            private static int __wc_depth;
            protected boolean dispatch(ComponentEvent event) {
                if (event == null) throw new NullPointerException("dispatch(null)");
                if (__host == null) throw new IllegalStateException("dispatch is not available in the constructor");
                __wc_detailBegin();
                boolean filled = false;
                try { event.__wc_fill(); filled = true; } finally { if (!filled) __wc_detailAbort(); }
                // The record stays reachable while it is in flight; Java listeners find it through the event's token.
                if (__wc_firing == null) __wc_firing = new Object[8];
                if (__wc_depth == __wc_firing.length) {
                    Object[] grown = new Object[__wc_depth * 2];
                    for (int i = 0; i < __wc_depth; i++) grown[i] = __wc_firing[i];
                    __wc_firing = grown;
                }
                int token = __wc_depth;
                __wc_firing[token] = event;
                __wc_depth = token + 1;
                try {
                    return __wc_fire(__host, event.__wc_type(), event.__wc_flags(), token) != 0;
                } finally {
                    __wc_firing[token] = null;
                    __wc_depth = token;
                }
            }
            public static Object __wc_eventRecord(JSObject event) {
                int token = __wc_eventToken(event);
                return token < 0 || token >= __wc_depth ? null : __wc_firing[token];
            }
            public static String __wc_kebab(String name) { return name.toLowerCase().replace('_', '-'); }
            // The window and document of the runtime's document, which is also where components are defined.
            @JS("return runtime.doc ? runtime.doc.defaultView : globalThis;")
            protected static native JSObject window();
            @JS("return runtime.doc || globalThis.document;")
            protected static native JSObject document();
            protected void listen(JSObject target, String type, EventListener<Event> listener) { __wc_addListen(new __WcListen(target, null, type, listener, null)); }
            protected void listen(JSObject target, String type, __JavawDomRunnable listener) { __wc_addListen(new __WcListen(target, null, type, null, listener)); }
            protected void listen(Element target, String type, EventListener<Event> listener) { __wc_addListen(new __WcListen(null, target, type, listener, null)); }
            protected void listen(Element target, String type, __JavawDomRunnable listener) { __wc_addListen(new __WcListen(null, target, type, null, listener)); }
            private void __wc_addListen(__WcListen l) {
                if (l.type == null) throw new NullPointerException("listen: type is null");
                if (l.object == null && l.element == null) throw new NullPointerException("listen: target is null");
                if (l.handler == null && l.run == null) return;
                if (__listens == null) __listens = new ArrayList<__WcListen>();
                __listens.add(l);
                if (__isConnected) __wc_listenOn(__listens.size() - 1, l, true);
            }
            private void __wc_listenAll(boolean on) {
                if (__listens == null) return;
                for (int i = 0; i < __listens.size(); i++) __wc_listenOn(i, __listens.get(i), on);
            }
            private void __wc_listenOn(int index, __WcListen l, boolean on) {
                if (l.element != null) __wc_listenElement(__host, l.element, l.type, index, on);
                else __wc_listenObject(__host, l.object, l.type, index, on);
            }
            public static void __wc_listenDispatch(WebComponent c, int index, JSObject event) {
                __WcListen l = c.__listens.get(index);
                if (l.run != null) l.run.run();
                else l.handler.handleEvent(new Event(event));
            }
            @JS("runtime.wcDetailBegin();")
            public static native void __wc_detailBegin();
            @JS("runtime.wcDetailAbort();")
            public static native void __wc_detailAbort();
            @JS("runtime.wcDetailSet(name, present ? value : null);")
            public static native void __wc_detailString(String name, String value, boolean present);
            @JS("runtime.wcDetailSet(name, value);")
            public static native void __wc_detailNumber(String name, double value);
            @JS("runtime.wcDetailSet(name, value);")
            public static native void __wc_detailBool(String name, boolean value);
            @JS("runtime.wcDetailSet(name, BigInt(value));")
            public static native void __wc_detailBigInt(String name, String value);
            @JS("runtime.wcDetailSet(name, value);")
            public static native void __wc_detailElement(String name, Element value);
            @JS("runtime.wcDetailSet(name, null);")
            public static native void __wc_detailNull(String name);
            @JS("runtime.wcDetailEnter();")
            public static native void __wc_detailEnter();
            @JS("runtime.wcDetailExit(name);")
            public static native void __wc_detailExit(String name);
            @JS("return runtime.wcFire(host, type, flags, token) ? 1 : 0;")
            public static native int __wc_fire(Element host, String type, int flags, int token);
            @JS("return runtime.wcEventToken(event);")
            public static native int __wc_eventToken(JSObject event);
            @JS("runtime.wcListen(host, target, type, index, on);")
            public static native void __wc_listenElement(Element host, Element target, String type, int index, boolean on);
            @JS("runtime.wcListen(host, target, type, index, on);")
            public static native void __wc_listenObject(Element host, JSObject target, String type, int index, boolean on);
        """;

    static final String LISTEN_TYPE = """
        final class __WcListen {
            JSObject object;
            Element element;
            String type;
            EventListener<Event> handler;
            __JavawDomRunnable run;
            __WcListen(JSObject object, Element element, String type, EventListener<Event> handler, __JavawDomRunnable run) {
                this.object = object;
                this.element = element;
                this.type = type;
                this.handler = handler;
                this.run = run;
            }
        }
        """;
}
