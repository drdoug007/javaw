package one.dastec.javaw;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import java.util.*;

/** Compile-time singleton graph for the linked Spring API subset; no runtime scanning. */
final class SpringBeanGraph {
    private record Bean(ClassOrInterfaceDeclaration type, String field) {}
    private final Map<String, ClassOrInterfaceDeclaration> types = new LinkedHashMap<>();
    private final Map<String, Bean> beans = new LinkedHashMap<>();
    private final Map<String, List<String>> dependencies = new LinkedHashMap<>();
    private final List<String> order = new ArrayList<>();
    private final Set<String> complete = new HashSet<>();
    private final List<String> active = new ArrayList<>();

    SpringBeanGraph(CompilationUnit cu) {
        for (var type : cu.findAll(ClassOrInterfaceDeclaration.class)) types.put(type.getNameAsString(), type);
    }

    void add(ClassOrInterfaceDeclaration type, String field) {
        String name = type.getNameAsString();
        if (type.isInterface() || type.isAbstract() || !type.getTypeParameters().isEmpty())
            throw error(type, "Bean " + name + " must be a concrete non-generic class");
        rejectUnsupportedInjection(type);
        if (type.getConstructors().size() > 1)
            throw error(type, "One constructor required on bean " + name);
        if (!type.getConstructors().isEmpty() && type.getConstructors().get(0).isPrivate())
            throw error(type.getConstructors().get(0), "Private bean constructor on " + name);
        // The source backend emits constructors from declarations; materialize Java's
        // implicit default constructor for beans before registry construction.
        if (type.getConstructors().isEmpty()) type.addConstructor();
        beans.putIfAbsent(name, new Bean(type, field));
    }

    private void rejectUnsupportedInjection(ClassOrInterfaceDeclaration type) {
        for (AnnotationExpr annotation : type.findAll(AnnotationExpr.class)) {
            String name = annotation.getName().getIdentifier();
            if (Set.of("Qualifier", "Primary", "Value", "Inject", "Resource", "Scope", "Lazy", "DependsOn", "PostConstruct", "PreDestroy", "Order", "Priority").contains(name))
                throw error(annotation, "@" + name + " is not supported; use a unique constructor dependency");
            if (name.equals("Bean") && !type.isAnnotationPresent("SpringBootApplication"))
                throw error(annotation, "Factory @Bean methods outside startup ApplicationRunner are not supported");
            if (name.equals("Autowired") && annotation.isNormalAnnotationExpr())
                throw error(annotation, "Constructor @Autowired options are not supported; use a single required constructor");
            if (name.equals("Autowired") && !(annotation.getParentNode().orElse(null) instanceof ConstructorDeclaration))
                throw error(annotation, "Field/setter @Autowired is not supported; use constructor injection");
        }
    }

    String declarations() {
        StringBuilder source = new StringBuilder();
        for (var entry : beans.entrySet()) source.append("static ").append(entry.getKey()).append(' ')
                .append(entry.getValue().field()).append(';');
        return source.toString();
    }

    String initialization() {
        for (String name : beans.keySet()) visit(name);
        StringBuilder source = new StringBuilder();
        for (String name : order) {
            Bean bean = beans.get(name);
            StringJoiner args = new StringJoiner(",");
            for (String dependency : dependencies.get(name)) args.add(beans.get(dependency).field());
            source.append(bean.field()).append(" = new ").append(name).append('(').append(args).append(");");
        }
        return source.toString();
    }

    private void visit(String name) {
        if (complete.contains(name)) return;
        Bean bean = beans.get(name);
        if (active.contains(name)) {
            var cycle = new ArrayList<>(active.subList(active.indexOf(name), active.size()));
            cycle.add(name);
            throw error(bean.type(), "Cyclic constructor dependencies: " + String.join(" -> ", cycle));
        }
        active.add(name);
        List<String> required = new ArrayList<>();
        if (!bean.type().getConstructors().isEmpty()) {
            for (Parameter parameter : bean.type().getConstructors().get(0).getParameters()) {
                String target = parameter.getTypeAsString();
                List<String> candidates = beans.keySet().stream()
                        .filter(candidate -> assignable(candidate, target, new HashSet<>())).toList();
                if (parameter.isVarArgs() || candidates.isEmpty())
                    throw error(parameter, "Missing bean for " + name + " dependency " + target
                            + "; link a concrete @Service/@Component or a supported repository");
                if (candidates.size() != 1)
                    throw error(parameter, "Ambiguous bean for " + name + " dependency " + target + ": " + String.join(", ", candidates)
                            + "; use a unique concrete constructor dependency");
                required.add(candidates.get(0));
                visit(candidates.get(0));
            }
        }
        dependencies.put(name, required);
        active.remove(active.size() - 1);
        complete.add(name);
        order.add(name);
    }

    private boolean assignable(String candidate, String target, Set<String> seen) {
        if (candidate.equals(target)) return true;
        if (!seen.add(candidate)) return false;
        var declaration = types.get(candidate);
        if (declaration == null) return false;
        for (var parent : declaration.getExtendedTypes())
            if (assignable(parent.asString(), target, seen)) return true;
        for (var parent : declaration.getImplementedTypes())
            if (assignable(parent.asString(), target, seen)) return true;
        return false;
    }

    private static IllegalArgumentException error(Node node, String message) {
        return JavaSourcePreparation.error(node, "Unsupported Spring WebAssembly construct: " + message);
    }
}
