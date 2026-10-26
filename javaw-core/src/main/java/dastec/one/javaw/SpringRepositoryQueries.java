package dastec.one.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.*;
import java.util.*;

/** Admission and source lowering for a deliberately bounded derived-query grammar. */
final class SpringRepositoryQueries {
    private record Property(String name, String type) {}

    static void add(ClassOrInterfaceDeclaration repository, ClassOrInterfaceDeclaration entity,
                    ClassOrInterfaceDeclaration generated, String rowType) {
        Map<String, Property> properties = new LinkedHashMap<>();
        for (var field : entity.getFields()) {
            if (field.isStatic()) continue;
            for (var variable : field.getVariables()) {
                String name = variable.getNameAsString();
                properties.put(Character.toUpperCase(name.charAt(0)) + name.substring(1),
                        new Property(name, variable.getTypeAsString()));
            }
        }
        for (var method : repository.getMethods()) {
            String result = method.getTypeAsString().replace("java.util.List<", "List<").replace("java.util.Optional<", "Optional<");
            String entityName = entity.getNameAsString();
            boolean array = result.equals(entityName + "[]") || result.equals("List<" + entityName + ">");
            if (method.getNameAsString().equals("findAll") && method.getParameters().isEmpty() && array
                    && method.getBody().isEmpty() && method.getAnnotations().isEmpty()) continue;
            generated.addMember(lower(method, properties, entityName, rowType, array));
        }
    }

    private static MethodDeclaration lower(MethodDeclaration method, Map<String, Property> properties,
                                           String entity, String row, boolean array) {
        if (method.getBody().isPresent() || method.isStatic() || !method.getTypeParameters().isEmpty()
                || !method.getAnnotations().isEmpty()) throw error(method, "Annotations, bodies, static and generic query methods are unsupported");
        String name = method.getNameAsString();
        if (Set.of("findById", "save", "deleteById").contains(name))
            throw error(method, "Redeclaring built-in CRUD methods is unsupported");
        String kind = null;
        for (String prefix : List.of("findBy", "countBy", "existsBy")) if (name.startsWith(prefix)) kind = prefix;
        if (kind == null) throw error(method, "Use findBy, countBy or existsBy with equality properties");
        String predicate = name.substring(kind.length());
        String result = method.getTypeAsString().replace("java.util.List<", "List<").replace("java.util.Optional<", "Optional<");
        boolean optional = result.equals("Optional<" + entity + ">");
        if (kind.equals("findBy") && !(array || optional || result.equals(entity))
                || kind.equals("countBy") && !result.equals("long")
                || kind.equals("existsBy") && !result.equals("boolean"))
            throw error(method, "Result must be entity/List<entity>/entity[]/Optional<entity>, long for countBy, or boolean for existsBy");

        Parsed parsed = parse(predicate, properties);
        if (parsed == null) throw error(method, "Unknown or unsupported query property/operator " + predicate);
        if (parsed.properties().size() != method.getParameters().size())
            throw error(method, "Exactly one parameter per equality property is required");
        StringBuilder condition = new StringBuilder();
        MethodDeclaration implementation = method.clone();
        if (optional) implementation.setType("Optional<" + entity + ">");
        for (int i = 0; i < parsed.properties().size(); i++) {
            Property property = parsed.properties().get(i);
            Parameter parameter = method.getParameter(i);
            if (parameter.isVarArgs() || !parameter.getAnnotations().isEmpty()
                    || !parameter.getTypeAsString().equals(property.type()))
                throw error(method, "Parameter " + parameter.getNameAsString() + " must have property type " + property.type());
            String argument = "__q_arg" + i;
            implementation.getParameter(i).setName(argument);
            if (i > 0) condition.append(parsed.connectors().get(i - 1).equals("And") ? " && " : " || ");
            condition.append('(').append(equality(method, property, "__q_row.value." + property.name(), argument)).append(')');
        }
        if (condition.isEmpty()) throw error(method, "At least one equality property is required");
        String scan = row + " __q_row = this.head; while (__q_row != null) { if (" + condition + ") {";
        String advance = "} __q_row = __q_row.next; }";
        String body;
        if (kind.equals("existsBy")) body = scan + "return true;" + advance + "return false;";
        else if (kind.equals("countBy")) body = "long __q_count=0L;" + scan + "__q_count++;" + advance + "return __q_count;";
        else if (array) {
            implementation.setType(entity + "[]");
            body = "int __q_count=0;" + scan + "__q_count++;" + advance
                    + entity + "[] __q_results=new " + entity + "[__q_count]; int __q_index=0;"
                    + "__q_row=this.head; while(__q_row!=null) { if(" + condition + ") { __q_results[__q_index++]=__q_row.value; }"
                    + "__q_row=__q_row.next; } return __q_results;";
        } else {
            body = entity + " __q_result=null; int __q_count=0;" + scan
                    + "__q_count++; __q_result=__q_row.value;" + advance
                    + "if(__q_count>1) throw new IncorrectResultSizeDataAccessException(1,__q_count);"
                    + (optional ? "return Optional.<" + entity + ">ofNullable(__q_result);" : "return __q_result;");
        }
        implementation.setBody(StaticJavaParser.parseBlock("{" + body + "}"));
        implementation.setPublic(true);
        implementation.setAbstract(false);
        return implementation;
    }

    private record Parsed(List<Property> properties, List<String> connectors) {}

    private static Parsed parse(String predicate, Map<String, Property> properties) {
        Map<String, Property> tokens = new LinkedHashMap<>(properties);
        for (var entry : properties.entrySet()) {
            tokens.putIfAbsent(entry.getKey() + "Equals", entry.getValue());
            tokens.putIfAbsent(entry.getKey() + "Is", entry.getValue());
        }
        // Prefer a complete/longer property over splitting a name containing And/Or.
        List<String> sorted = new ArrayList<>(tokens.keySet());
        sorted.sort(Comparator.comparingInt(String::length).reversed());
        return parse(predicate, sorted, tokens);
    }

    private static Parsed parse(String text, List<String> sorted, Map<String, Property> tokens) {
        for (String token : sorted) {
            if (!text.startsWith(token)) continue;
            String rest = text.substring(token.length());
            if (rest.isEmpty()) return new Parsed(List.of(tokens.get(token)), List.of());
            for (String connector : List.of("And", "Or")) {
                if (!rest.startsWith(connector)) continue;
                String tail = rest.substring(connector.length());
                if (tail.isEmpty() || !Character.isUpperCase(tail.charAt(0))) continue;
                Parsed following = parse(tail, sorted, tokens);
                if (following != null) {
                    List<Property> fields = new ArrayList<>(); fields.add(tokens.get(token)); fields.addAll(following.properties());
                    List<String> joins = new ArrayList<>(); joins.add(connector); joins.addAll(following.connectors());
                    return new Parsed(fields, joins);
                }
            }
        }
        return null;
    }

    private static String equality(MethodDeclaration method, Property property, String field, String arg) {
        return switch (property.type()) {
            case "int" -> field + " == " + arg;
            case "Integer" -> "(" + field + " == null && " + arg + " == null) || (" + field + " != null && " + arg
                    + " != null && (int)" + field + " == (int)" + arg + ")";
            case "String" -> "(" + field + " == null && " + arg + " == null) || (" + field + " != null && " + arg
                    + " != null && " + field + ".equals(" + arg + "))";
            case "BigDecimal" -> "(" + field + " == null && " + arg + " == null) || (" + field + " != null && " + arg
                    + " != null && " + field + ".compareTo(" + arg + ") == 0)";
            default -> throw error(method, "Unsupported query field type " + property.type());
        };
    }

    private static IllegalArgumentException error(MethodDeclaration method, String message) {
        return JavaSourcePreparation.error(method, "Repository method "
                + method.findAncestor(ClassOrInterfaceDeclaration.class).map(t -> t.getNameAsString() + ".").orElse("")
                + method.getNameAsString() + ": " + message
                + "; use supported equality queries or implement filtering in a source service");
    }
}
