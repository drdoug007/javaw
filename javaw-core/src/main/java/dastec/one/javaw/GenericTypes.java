package dastec.one.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeParameters;
import com.github.javaparser.ast.type.*;
import java.util.*;

/** Reference-type substitution and invocation constraints for the source compiler. */
final class GenericTypes {
    private GenericTypes() {}
    static String raw(String type) {
        if (type == null) return null;
        int bracket = type.indexOf('<');
        return bracket < 0 ? type : type.substring(0, bracket);
    }
    static List<String> arguments(String type) {
        if (type == null || !type.contains("<")) return List.of();
        return StaticJavaParser.parseClassOrInterfaceType(type).getTypeArguments()
                .map(a -> a.stream().map(Type::asString).toList()).orElse(List.of());
    }
    private static Type parse(String type) {
        return type.startsWith("?") ? StaticJavaParser.parseClassOrInterfaceType("Capture<" + type + ">")
                .getTypeArguments().orElseThrow().get(0).clone() : StaticJavaParser.parseType(type);
    }
    static String substitute(String type, Map<String, String> bindings) {
        if (type == null || bindings.isEmpty()) return type;
        Type parsed = parse(type);
        if (parsed.isClassOrInterfaceType() && bindings.containsKey(parsed.asString())) return bindings.get(parsed.asString());
        for (ClassOrInterfaceType variable : new ArrayList<>(parsed.findAll(ClassOrInterfaceType.class)))
            if (variable.getScope().isEmpty() && variable.getTypeArguments().isEmpty() && bindings.containsKey(variable.getNameAsString()))
                variable.replace(parse(bindings.get(variable.getNameAsString())));
        return parsed.asString();
    }
    static List<TypeParameter> parameters(Node node) {
        return node instanceof NodeWithTypeParameters<?> p ? new ArrayList<>(p.getTypeParameters()) : List.of();
    }
    static Map<String, String> erasedBindings(Node context) {
        Map<String, String> result = new LinkedHashMap<>();
        context.findAncestor(TypeDeclaration.class).ifPresent(t -> parameters(t).forEach(p -> result.put(p.getNameAsString(),
                p.getTypeBound().isEmpty() ? "Object" : raw(p.getTypeBound().get(0).asString()))));
        parameters(context).forEach(p -> result.put(p.getNameAsString(),
                p.getTypeBound().isEmpty() ? "Object" : raw(p.getTypeBound().get(0).asString())));
        return result;
    }
    static String eraseVariables(String type, Node context) { return substitute(type, erasedBindings(context)); }

    static void constrain(String formal, String actual, Set<String> variables, Map<String, String> bindings, ClassRegistry registry) {
        if (actual == null || actual.equals("null")) return;
        if (variables.contains(formal)) {
            String boxed = BoxingSupport.wrapper(actual);
            String inferred = boxed == null ? actual : boxed;
            String prior = bindings.get(formal);
            if (prior == null) bindings.put(formal, inferred);
            else bindings.put(formal, commonType(prior, inferred, registry));
            return;
        }
        if (formal.endsWith("[]") && actual.endsWith("[]")) {
            constrain(formal.substring(0, formal.length() - 2), actual.substring(0, actual.length() - 2), variables, bindings, registry);
            return;
        }
        if (formal.contains("<")) {
            String aligned = registry.asSuperType(actual, raw(formal));
            if (aligned == null) return;
            List<String> f = arguments(formal), a = arguments(aligned);
            if (f.size() != a.size()) return;
            for (int i = 0; i < f.size(); i++) {
                String pattern = f.get(i).replace("? extends ", "").replace("? super ", "");
                constrain(pattern, a.get(i), variables, bindings, registry);
            }
        }
    }
    static void validateSourceTypes(com.github.javaparser.ast.CompilationUnit cu, ClassRegistry registry) {
        for (ClassOrInterfaceType type : cu.findAll(ClassOrInterfaceType.class)) {
            if (type.getTypeArguments().isEmpty() || type.getTypeArguments().get().isEmpty()) continue;
            Node context = type.findAncestor(CallableDeclaration.class).map(n -> (Node)n)
                    .orElseGet(() -> type.findAncestor(TypeDeclaration.class).map(n -> (Node)n).orElse(type));
            try { registry.classBindings(eraseVariables(type.asString(), context)); }
            catch (IllegalArgumentException error) { throw JavaSourcePreparation.error(type, error.getMessage()); }
        }
        for (var creation : cu.findAll(com.github.javaparser.ast.expr.ObjectCreationExpr.class)) {
            Node context = creation.findAncestor(CallableDeclaration.class).map(n -> (Node)n).orElse(creation);
            if (erasedBindings(context).containsKey(creation.getType().getNameAsString()))
                throw JavaSourcePreparation.error(creation, "Cannot instantiate a type variable; pass an explicit factory or concrete class");
        }
        for (var creation : cu.findAll(com.github.javaparser.ast.expr.ArrayCreationExpr.class)) {
            Node context = creation.findAncestor(CallableDeclaration.class).map(n -> (Node)n).orElse(creation);
            if (erasedBindings(context).containsKey(creation.getElementType().asString()))
                throw JavaSourcePreparation.error(creation, "Generic array creation is not supported; pass an array supplied by the caller");
        }
    }

    static String readable(String type) {
        if (type == null) return null;
        if (type.startsWith("? extends ")) return type.substring(10);
        if (type.startsWith("?")) return "Object";
        return type;
    }

    static String commonType(String left, String right, ClassRegistry registry) {
        if (registry.canWiden(left, right)) return right;
        if (registry.canWiden(right, left)) return left;
        List<String> shared = registry.sourceSupertypes(left).stream().filter(t -> registry.canWiden(right, t)).toList();
        List<String> minimal = shared.stream().filter(t -> shared.stream().noneMatch(other -> !other.equals(t)
                && registry.canWiden(other, t))).toList();
        if (minimal.size() == 1) return minimal.get(0);
        if (minimal.size() > 1) throw new IllegalArgumentException("Intersection generic inference requires an explicit common type argument: " + minimal);
        return "Object";
    }

    static void validateBounds(List<TypeParameter> variables, Map<String, String> bindings, ClassRegistry registry) {
        for (TypeParameter variable : variables) {
            String actual = bindings.get(variable.getNameAsString());
            if (actual != null && actual.startsWith("?")) {
                actual = actual.equals("?") ? variable.getTypeBound().isEmpty() ? "Object" : raw(variable.getTypeBound().get(0).asString())
                        : actual.replace("? extends ", "").replace("? super ", "");
            }
            if (actual == null || BoxingSupport.wrapper(actual) != null)
                throw new IllegalArgumentException("Generic type argument must be a reference type for " + variable.getNameAsString());
            for (ClassOrInterfaceType bound : variable.getTypeBound()) {
                String expected = substitute(bound.asString(), bindings);
                if (!registry.canWiden(actual, expected))
                    throw new IllegalArgumentException("Generic type argument " + actual + " violates bound " + expected + " for " + variable.getNameAsString());
            }
        }
    }
    static Map<String, String> bindings(List<TypeParameter> parameters, List<String> arguments, ClassRegistry registry) {
        if (!arguments.isEmpty() && parameters.size() != arguments.size()) throw new IllegalArgumentException("Wrong number of generic type arguments");
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < parameters.size(); i++) {
            TypeParameter p = parameters.get(i);
            String actual = arguments.isEmpty() ? p.getTypeBound().isEmpty() ? "Object" : raw(p.getTypeBound().get(0).asString()) : arguments.get(i);
            result.put(p.getNameAsString(), actual);
        }
        validateBounds(parameters, result, registry);
        return result;
    }
}
