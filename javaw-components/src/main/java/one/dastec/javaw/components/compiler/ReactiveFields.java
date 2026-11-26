package one.dastec.javaw.components.compiler;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import one.dastec.javaw.spi.SourceExtension;

import java.util.*;

/**
 * M2 reactive fields: {@code @Property} and {@code @State} on private instance fields of a component.
 *
 * <p>Writes outside constructors and initializers become calls to generated setters that record the old
 * value and schedule an update. Each component also gets generated overrides for its initial changes,
 * attribute conversion, reflection, and static accessors that back the element's JS properties.
 */
final class ReactiveFields {
    private ReactiveFields() {}

    /** One reactive field. {@code kind} is the attribute/JS conversion, or null for {@code @State}. */
    record Field(String name, String type, boolean property, String kind, String attribute, boolean reflect,
                 String getter, String setter, boolean nullable, List<String> values, String nullGetter) {}

    /** JS kinds of @Property types (docs/web-components.md, "Property marshalling"); enums of the module are "enum". */
    private static final Map<String, String> PROPERTY_KINDS = Map.ofEntries(
            Map.entry("String", "string"), Map.entry("boolean", "boolean"), Map.entry("Boolean", "boolean"),
            Map.entry("int", "int"), Map.entry("Integer", "int"), Map.entry("short", "short"), Map.entry("Short", "short"),
            Map.entry("byte", "byte"), Map.entry("Byte", "byte"), Map.entry("char", "char"), Map.entry("Character", "char"),
            Map.entry("long", "long"), Map.entry("Long", "long"), Map.entry("float", "float"), Map.entry("Float", "float"),
            Map.entry("double", "double"), Map.entry("Double", "double"));
    /** Kinds without an attribute converter: JS properties only. */
    private static final Set<String> NO_ATTRIBUTE = Set.of("char", "long");
    private static final Map<String, String> BOXES = Map.of("int", "Integer", "long", "Long", "short", "Short",
            "byte", "Byte", "char", "Character", "boolean", "Boolean", "double", "Double", "float", "Float");

    static List<Field> lower(ClassOrInterfaceDeclaration component) {
        String owner = component.getNameAsString();
        List<Field> fields = new ArrayList<>();
        for (FieldDeclaration declaration : component.getFields()) {
            AnnotationExpr property = ComponentExtension.annotationOf(declaration, "Property");
            AnnotationExpr state = ComponentExtension.annotationOf(declaration, "State");
            if (property == null && state == null) continue;
            if (property != null && state != null)
                throw SourceExtension.error(declaration, "A field cannot be both @Property and @State");
            if (declaration.getVariables().size() != 1)
                throw SourceExtension.error(declaration, "Reactive fields must be declared one per declaration");
            String name = declaration.getVariable(0).getNameAsString();
            if (!declaration.isPrivate() || declaration.isStatic() || declaration.isFinal())
                throw SourceExtension.error(declaration, "Reactive field '" + name + "' must be a private, non-static, non-final instance field");
            String type = declaration.getVariable(0).getTypeAsString();
            String kind = null, attribute = null;
            boolean reflect = false, nullable = false;
            List<String> values = new ArrayList<>();
            if (property != null) {
                if (name.equals("updateComplete"))
                    throw SourceExtension.error(declaration, "@Property name 'updateComplete' is reserved for the element's update promise");
                String simpleType = type.startsWith("java.lang.") ? type.substring("java.lang.".length()) : type;
                kind = PROPERTY_KINDS.get(simpleType);
                if (kind != null) type = simpleType;
                EnumDeclaration enumType = kind == null ? enumOf(component, type) : null;
                if (enumType != null && enumType.getEntries().isEmpty())
                    throw SourceExtension.error(declaration, "@Property '" + name + "' has type " + type + ", an enum with no constants");
                if (enumType != null) {
                    kind = "enum";
                    for (EnumConstantDeclaration c : enumType.getEntries()) values.add(c.getNameAsString().toLowerCase(Locale.ROOT).replace('_', '-'));
                }
                if (kind == null)
                    throw SourceExtension.error(declaration, "@Property '" + name + "' has type " + type
                            + "; this milestone supports String, primitives, their boxes and enums of the module (use @State for other types)");
                nullable = BOXES.containsValue(type);
                String explicit = ComponentExtension.stringMemberOf(property, "attribute");
                if (NO_ATTRIBUTE.contains(kind)) {
                    if (explicit != null)
                        throw SourceExtension.error(property, "@Property '" + name + "' of type " + type + " has no attribute converter; remove attribute = \"" + explicit + "\"");
                } else {
                    attribute = explicit == null || explicit.isEmpty() ? kebab(name) : explicit;
                    if (!attribute.equals(attribute.toLowerCase(Locale.ROOT)) || attribute.chars().anyMatch(Character::isWhitespace))
                        throw SourceExtension.error(property, "Attribute names must be lowercase and without whitespace: \"" + attribute + "\"");
                }
                reflect = ComponentExtension.booleanMemberOf(property, "reflect");
                if (reflect && attribute == null)
                    throw SourceExtension.error(property, "@Property '" + name + "' of type " + type + " has no attribute to reflect to");
            }
            fields.add(new Field(name, type, property != null, kind, attribute, reflect,
                    "__wc_get_" + owner + "_" + name, "__wc_set_" + owner + "_" + name, nullable, List.copyOf(values),
                    nullable ? "__wc_null_" + owner + "_" + name : null));
        }
        if (fields.isEmpty()) return fields;
        Set<String> attributes = new HashSet<>();
        for (Field f : fields)
            if (f.attribute != null && !attributes.add(f.attribute))
                throw SourceExtension.error(component, "Two @Property fields of " + owner + " use the attribute \"" + f.attribute + "\"");

        for (Field f : fields) rewriteWrites(component, f);
        for (Field f : fields) addHelpers(component, f);
        addOverrides(component, fields);
        return fields;
    }

    static String kebab(String name) {
        StringBuilder out = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isUpperCase(c)) out.append('-').append(Character.toLowerCase(c));
            else out.append(c);
        }
        return out.toString();
    }

    /** Assignments, compound assignments and ++/-- on the field, outside constructors and initializers. */
    private static void rewriteWrites(ClassOrInterfaceDeclaration component, Field f) {
        String owner = component.getNameAsString();
        // Innermost first: an outer rewrite clones its value, so nested writes must already be rewritten.
        List<AssignExpr> assigns = new ArrayList<>(component.findAll(AssignExpr.class));
        Collections.reverse(assigns);
        for (AssignExpr assign : assigns) {
            if (!targets(assign.getTarget(), f.name, component)) continue;
            if (initialization(assign, component)) continue;
            Expression value = assign.getOperator() == AssignExpr.Operator.ASSIGN ? assign.getValue().clone()
                    : cast(f.type, new BinaryExpr(read(f.name), new EnclosedExpr(assign.getValue().clone()),
                    assign.getOperator().toBinaryOperator().orElseThrow()));
            assign.replace(setter(owner, "__set$" + f.name, value));
        }
        for (UnaryExpr unary : new ArrayList<>(component.findAll(UnaryExpr.class))) {
            UnaryExpr.Operator op = unary.getOperator();
            boolean increment = op == UnaryExpr.Operator.PREFIX_INCREMENT || op == UnaryExpr.Operator.POSTFIX_INCREMENT;
            boolean decrement = op == UnaryExpr.Operator.PREFIX_DECREMENT || op == UnaryExpr.Operator.POSTFIX_DECREMENT;
            if ((!increment && !decrement) || !targets(unary.getExpression(), f.name, component) || initialization(unary, component)) continue;
            Expression next = cast(f.type, new BinaryExpr(read(f.name), new IntegerLiteralExpr("1"),
                    increment ? BinaryExpr.Operator.PLUS : BinaryExpr.Operator.MINUS));
            boolean postfix = op == UnaryExpr.Operator.POSTFIX_INCREMENT || op == UnaryExpr.Operator.POSTFIX_DECREMENT;
            unary.replace(setter(owner, (postfix ? "__post$" : "__set$") + f.name, next));
        }
    }

    private static boolean targets(Expression target, String name, ClassOrInterfaceDeclaration component) {
        if (target instanceof FieldAccessExpr access && access.getNameAsString().equals(name)) {
            if (access.getScope() instanceof ThisExpr self) {
                boolean qualified = self.getTypeName().isPresent();
                if (qualified && !self.getTypeName().get().getIdentifier().equals(component.getNameAsString())) return false;
                if (thisOwner(access) == component) return true;
                // An unqualified `this` in a nested or anonymous class is that class's own object.
                if (!qualified) return false;
                throw SourceExtension.error(access, "Reactive field '" + name + "' cannot be written from a nested class");
            }
            // Without type resolution, only receivers declared with the component's own type are known to be components.
            if (access.getScope() instanceof NameExpr receiver && declaredAsComponent(receiver.getNameAsString(), component))
                throw SourceExtension.error(access, "Reactive field '" + name + "' can only be written as '" + name + "' or 'this." + name + "'");
            return false;
        }
        if (!(target instanceof NameExpr use) || !use.getNameAsString().equals(name)) return false;
        if (ComponentExtension.shadowedLocal(use, name, component)) return false;
        // A field of an enclosing nested or anonymous class shadows the component's field.
        for (Node owner = thisOwner(use); owner != null && owner != component; owner = thisOwner(owner))
            if (declaresField(owner, name)) return false;
        if (thisOwner(use) != component)
            throw SourceExtension.error(use, "Reactive field '" + name + "' cannot be written from a nested class");
        return true;
    }

    /** The class or anonymous class body that {@code this} refers to at {@code node}. */
    private static Node thisOwner(Node node) {
        Node child = node;
        for (Node scope = node.getParentNode().orElse(null); scope != null; child = scope, scope = scope.getParentNode().orElse(null)) {
            if (scope instanceof TypeDeclaration<?>) return scope;
            if (scope instanceof ObjectCreationExpr creation && child instanceof BodyDeclaration<?> member
                    && creation.getAnonymousClassBody().map(b -> b.stream().anyMatch(m -> m == member)).orElse(false)) return scope;
        }
        return null;
    }

    private static boolean declaresField(Node owner, String name) {
        List<BodyDeclaration<?>> members = owner instanceof TypeDeclaration<?> type ? type.getMembers()
                : owner instanceof ObjectCreationExpr creation ? creation.getAnonymousClassBody().map(List::copyOf).orElse(List.of()) : List.of();
        for (BodyDeclaration<?> member : members)
            if (member instanceof FieldDeclaration field && field.getVariables().stream().anyMatch(v -> v.getNameAsString().equals(name))) return true;
        return false;
    }

    private static boolean declaredAsComponent(String variable, ClassOrInterfaceDeclaration component) {
        String owner = component.getNameAsString();
        for (Parameter p : component.findAll(Parameter.class))
            if (p.getNameAsString().equals(variable) && p.getTypeAsString().equals(owner)) return true;
        for (VariableDeclarator v : component.findAll(VariableDeclarator.class))
            if (v.getNameAsString().equals(variable) && v.getTypeAsString().equals(owner)) return true;
        return false;
    }

    /**
     * Writes in constructors, initializer blocks and field initializers set the initial value and are not changes.
     * Writes in lambdas declared there run later, so they are changes.
     */
    private static boolean initialization(Node node, ClassOrInterfaceDeclaration component) {
        boolean deferred = false;
        for (Node scope = node.getParentNode().orElse(null); scope != null && scope != component; scope = scope.getParentNode().orElse(null)) {
            if (scope instanceof LambdaExpr) deferred = true;
            if (scope instanceof ConstructorDeclaration || scope instanceof InitializerDeclaration || scope instanceof FieldDeclaration) return !deferred;
            if (scope instanceof MethodDeclaration method) {
                if (method.isStatic()) throw SourceExtension.error(node, "Reactive fields cannot be written from a static method");
                return false;
            }
        }
        return false;
    }

    private static Expression read(String name) {
        return new FieldAccessExpr(new ThisExpr(), name);
    }

    private static Expression cast(String type, Expression value) {
        return new CastExpr(StaticJavaParser.parseType(type), new EnclosedExpr(value));
    }

    private static Expression setter(String owner, String helper, Expression value) {
        return new MethodCallExpr(new NameExpr(owner), helper, new com.github.javaparser.ast.NodeList<>(new ThisExpr(), value));
    }

    private static String box(String type, String expression) {
        String wrapper = BOXES.get(type);
        return wrapper == null ? expression : wrapper + ".valueOf(" + expression + ")";
    }

    private static String changed(String type) {
        if (type.equals("double") || type.equals("float")) return "WebComponent.__wc_doubleChanged(old, value)";
        if (BOXES.containsKey(type)) return "old != value";
        if (type.equals("String") || BOXES.containsValue(type)) return "(old == null ? value != null : !old.equals(value))";
        return "old != value";
    }

    private static String defaultValue(String type) {
        return switch (type) {
            case "boolean" -> "false";
            case "char" -> "(char) 0";
            case "double" -> "0.0";
            case "float" -> "0.0f";
            case "long" -> "0L";
            case "int" -> "0";
            case "short" -> "(short) 0";
            case "byte" -> "(byte) 0";
            default -> null;
        };
    }

    /** The module enum a field type names, by simple name (nested enums included), or null. */
    private static EnumDeclaration enumOf(ClassOrInterfaceDeclaration component, String type) {
        // A multi-file build is merged into one compilation unit before extensions run, so this sees every module enum.
        // A qualified type (Outer.Mode) must match the enum's nesting; a simple name must be unambiguous.
        List<EnumDeclaration> matches = component.findCompilationUnit().map(cu -> cu.findAll(EnumDeclaration.class).stream()
                .filter(e -> nestedName(e).equals(type) || nestedName(e).endsWith("." + type)).toList()).orElse(List.of());
        if (matches.size() > 1)
            throw SourceExtension.error(component, "@Property type " + type + " matches several enums ("
                    + String.join(", ", matches.stream().map(ReactiveFields::nestedName).toList()) + "); qualify it");
        return matches.isEmpty() ? null : matches.get(0);
    }

    private static String nestedName(TypeDeclaration<?> type) {
        String name = type.getNameAsString();
        for (Node n = type.getParentNode().orElse(null); n != null; n = n.getParentNode().orElse(null))
            if (n instanceof TypeDeclaration<?> outer) name = outer.getNameAsString() + "." + name;
        return name;
    }

    /** The primitive a JS bridge passes for a kind (boxes pass their primitive plus a null flag). */
    private static String bridgeType(Field f) {
        return switch (f.kind) {
            case "string", "enum" -> "String";
            case "boolean" -> "boolean";
            default -> f.kind; // int, short, byte, char, long, float, double
        };
    }

    private static void addHelpers(ClassOrInterfaceDeclaration component, Field f) {
        String owner = component.getNameAsString(), t = f.type;
        add(component, "static " + t + " __set$" + f.name + "(" + owner + " component, " + t + " value) { "
                + t + " old = component." + f.name + "; component." + f.name + " = value; "
                + "if (" + changed(t) + ") component.__wc_change(\"" + f.name + "\", " + box(t, "old") + "); return value; }");
        add(component, "static " + t + " __post$" + f.name + "(" + owner + " component, " + t + " value) { "
                + t + " old = component." + f.name + "; __set$" + f.name + "(component, value); return old; }");
        if (!f.property) return;
        if (f.kind.equals("enum") && f.attribute != null)
            // Matched by kebab-case name, like the JS setter and reflection, so every advertised value round-trips.
            // (Enum.valueOf of an unknown name does not throw in the source compiler either.)
            add(component, "static " + t + " __parse$" + f.name + "(String value) { String name = WebComponent.__wc_constantName(value); "
                    + "for (" + t + " c : " + t + ".values()) if (WebComponent.__wc_kebab(c.name()).equals(value)) return c; "
                    + "throw new IllegalArgumentException(\"No enum constant " + t + ".\" + name); }");
        // JS accessors call these exports with primitives and strings only (boxes and enums cannot be built in JS).
        String b = bridgeType(f), v = "component." + f.name;
        if (f.kind.equals("enum")) {
            add(component, "public static String " + f.getter + "(" + owner + " component) { return " + v + " == null ? null : WebComponent.__wc_kebab(" + v + ".name()); }");
            add(component, "public static void " + f.setter + "(" + owner + " component, String value) { " + t + " converted = null; "
                    + "if (value != null) { for (" + t + " c : " + t + ".values()) if (WebComponent.__wc_kebab(c.name()).equals(value)) converted = c; "
                    + "if (converted == null) throw new IllegalArgumentException(\"No constant '\" + value + \"' in " + t + "\"); } "
                    + "__set$" + f.name + "(component, converted); }");
        } else if (f.nullable) {
            String unbox = v + "." + b + "Value()";
            add(component, "public static " + b + " " + f.getter + "(" + owner + " component) { return " + v + " == null ? " + defaultValue(b) + " : " + unbox + "; }");
            add(component, "public static boolean " + f.nullGetter + "(" + owner + " component) { return " + v + " == null; }");
            add(component, "public static void " + f.setter + "(" + owner + " component, boolean isNull, " + b + " value) { "
                    + "__set$" + f.name + "(component, isNull ? null : " + t + ".valueOf(value)); }");
        } else {
            add(component, "public static " + t + " " + f.getter + "(" + owner + " component) { return " + v + "; }");
            add(component, "public static void " + f.setter + "(" + owner + " component, " + t + " value) { __set$" + f.name + "(component, value); }");
        }
    }

    private static void addOverrides(ClassOrInterfaceDeclaration component, List<Field> fields) {
        StringBuilder initial = new StringBuilder("protected void __wc_initial() { super.__wc_initial(); ");
        StringBuilder fromAttribute = new StringBuilder("protected void __wc_fromAttribute(String name, String value) { super.__wc_fromAttribute(name, value); ");
        StringBuilder reflect = new StringBuilder("protected void __wc_reflect(Changes changes) { super.__wc_reflect(changes); ");
        for (Field f : fields) {
            String def = defaultValue(f.type);
            initial.append("__wc_change(\"").append(f.name).append("\", ").append(def == null ? "null" : box(f.type, def)).append("); ");
            if (f.attribute == null) continue;
            // docs/web-components.md, "Attribute conversion": removal gives the default, or null for boxes and enums.
            String parse = switch (f.kind) {
                case "string" -> "value";
                case "boolean" -> "true";
                case "int" -> "Integer.parseInt(value)";
                case "short" -> "WebComponent.__wc_parseShort(value)";
                case "byte" -> "WebComponent.__wc_parseByte(value)";
                case "float" -> "Float.parseFloat(value)";
                case "enum" -> "__parse$" + f.name + "(value)";
                default -> "Double.parseDouble(value)";
            };
            if (f.nullable && !f.kind.equals("boolean")) parse = f.type + ".valueOf(" + parse + ")";
            String removed = f.kind.equals("string") || f.kind.equals("enum") || f.nullable ? "null"
                    : f.kind.equals("boolean") ? "false" : defaultValue(f.type);
            String convert = f.kind.equals("string") ? "value"
                    : f.kind.equals("boolean") ? (f.nullable ? "value == null ? null : Boolean.TRUE" : "value != null")
                    : "value == null ? " + removed + " : " + parse;
            fromAttribute.append("if (name.equals(\"").append(f.attribute).append("\")) { ").append(f.type).append(" converted = ")
                    .append(convert).append("; __set$").append(f.name).append("(this, converted); } ");
            if (f.reflect) {
                String me = "this." + f.name;
                String text = switch (f.kind) {
                    case "string" -> me;
                    case "boolean" -> f.nullable ? me + " == null || !" + me + ".booleanValue() ? null : \"\"" : me + " ? \"\" : null";
                    case "enum" -> me + " == null ? null : WebComponent.__wc_kebab(" + me + ".name())";
                    default -> f.nullable ? me + " == null ? null : \"\" + " + me : "\"\" + " + me;
                };
                reflect.append("if (changes.has(\"").append(f.name).append("\")) { String text = ").append(text)
                        .append("; __wc_reflectAttribute(\"").append(f.attribute).append("\", text); } ");
            }
        }
        add(component, initial.append("}").toString());
        add(component, fromAttribute.append("}").toString());
        add(component, reflect.append("}").toString());
    }

    private static void add(ClassOrInterfaceDeclaration component, String member) {
        component.addMember(StaticJavaParser.parseBodyDeclaration(member));
    }

    /** Observed attributes contributed by @Property fields. */
    static List<String> attributes(List<Field> fields) {
        List<String> names = new ArrayList<>();
        for (Field f : fields) if (f.attribute != null) names.add(f.attribute);
        return names;
    }
}
