package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.util.*;

/** Ahead-of-time lowering for constructor-injected CRUD controllers. No JVM framework is loaded. */
final class SpringWebSupport {
    private static final Set<String> VALUE_TYPES = Set.of("String", "BigDecimal", "int", "Integer");
    private final Map<String, String> repositories = new LinkedHashMap<>();
    private final List<ClassOrInterfaceDeclaration> controllers = new ArrayList<>();
    private final Map<ClassOrInterfaceDeclaration, List<MethodDeclaration>> runners = new LinkedHashMap<>();
    private final Set<String> entities = new LinkedHashSet<>();
    private final List<Route> routes = new ArrayList<>();
    private record Route(String verb, String path, String controller, MethodDeclaration method) {}

    static SpringWebSupport lower(CompilationUnit cu) {
        SpringWebSupport support = new SpringWebSupport();
        boolean enabled = cu.getImports().stream().anyMatch(i -> i.getNameAsString().startsWith("org.springframework."));
        if (!enabled) return support;
        for (ClassOrInterfaceDeclaration type : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            if (type.isAnnotationPresent("Entity")) support.entities.add(type.getNameAsString());
            if (type.isAnnotationPresent("RestController")) support.controllers.add(type);
            for (ClassOrInterfaceType parent : type.getExtendedTypes()) {
                if (parent.getNameAsString().equals("CrudRepository")) {
                    var args = parent.getTypeArguments().orElseThrow(() -> unsupported("Raw CrudRepository"));
                    if (args.size() != 2 || !Set.of("Integer", "int").contains(args.get(1).asString()))
                        throw unsupported("CrudRepository requires an Integer ID");
                    support.repositories.put(type.getNameAsString(), args.get(0).asString());
                }
            }
        }
        boolean bootRequested = cu.findAll(MethodCallExpr.class).stream().anyMatch(call ->
                call.getScope().map(Object::toString).orElse("").equals("SpringApplication") && call.getNameAsString().equals("run"));
        if (!bootRequested && support.controllers.isEmpty()) return support;
        for (var entry : support.repositories.entrySet()) support.lowerRepository(cu, entry.getKey(), entry.getValue());
        support.lowerOptional(cu);
        for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
            if (call.getScope().map(Object::toString).orElse("").equals("SpringApplication") && call.getNameAsString().equals("run")) {
                call.replace(new MethodCallExpr("__javaw_boot"));
            }
        }
        for (FieldAccessExpr field : cu.findAll(FieldAccessExpr.class)) {
            if (field.getScope().toString().equals("HttpStatus")) {
                int status = switch (field.getNameAsString()) {
                    case "NOT_FOUND" -> 404;
                    case "BAD_REQUEST" -> 400;
                    case "CONFLICT" -> 409;
                    case "INTERNAL_SERVER_ERROR" -> 500;
                    default -> throw unsupported("HttpStatus." + field.getNameAsString());
                };
                field.replace(new IntegerLiteralExpr(status));
            }
        }
        cu.getTypes().add(StaticJavaParser.parseTypeDeclaration("""
            class ResponseStatusException extends RuntimeException {
                public int status;
                public String reason;
                public ResponseStatusException(int status, String reason) {
                    super(reason); this.status = status; this.reason = reason;
                }
            }
            """));
        LibrarySupport.add(cu, "org.springframework.boot", "interface ApplicationRunner { public void run(ApplicationArguments args); }");
        support.lowerStartup(cu);

        support.lowerLists(cu);
        support.addBootstrap(cu);
        return support;
    }

    private void lowerRepository(CompilationUnit cu, String name, String entity) {
        ClassOrInterfaceDeclaration model = cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(c -> c.getNameAsString().equals(entity)).findFirst().orElseThrow(() -> unsupported("Missing entity " + entity));
        if (!entities.contains(entity)) throw unsupported(entity + " must have @Entity");
        var ids = model.getFields().stream().filter(f -> f.isAnnotationPresent("Id")).toList();
        if (ids.size() != 1 || ids.get(0).getVariables().size() != 1) throw unsupported("One @Id field is required on " + entity);
        VariableDeclarator id = ids.get(0).getVariable(0);
        if (!Set.of("int", "Integer").contains(id.getTypeAsString())) throw unsupported("Integer entity ID required");
        if (!ids.get(0).isAnnotationPresent("GeneratedValue")) throw unsupported("@GeneratedValue is required on " + entity);
        String key = id.getNameAsString();
        ClassOrInterfaceDeclaration repo = cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(c -> c.getNameAsString().equals(name)).findFirst().orElseThrow();
        for (MethodDeclaration method : repo.getMethods()) {
            if (!method.getNameAsString().equals("findAll") || !method.getParameters().isEmpty())
                throw unsupported("Repository method " + name + "." + method.getNameAsString());
        }
        String node = "__JavawRow_" + name;
        String source = """
            class %s {
                public %s value;
                public %s next;
                %s(%s value, %s next) { this.value = value; this.next = next; }
            }
            class %s {
                private %s head;
                private int nextId;
                %s() { this.head = null; this.nextId = 1; }
                public %s findById(int id) {
                    %s row = this.head;
                    while (row != null) {
                        if (row.value.%s == id) return row.value;
                        row = row.next;
                    }
                    return null;
                }
                public %s[] findAll() {
                    int size = 0;
                    %s row = this.head;
                    while (row != null) { size++; row = row.next; }
                    %s[] values = new %s[size];
                    row = this.head;
                    int index = 0;
                    while (row != null) { values[index] = row.value; index++; row = row.next; }
                    return values;
                }
                public %s save(%s value) {
                    if (value.%s == 0) { value.%s = this.nextId; this.nextId++; }
                    %s row = this.head;
                    %s previous = null;
                    while (row != null) {
                        if (row.value.%s == value.%s) { row.value = value; return value; }
                        previous = row; row = row.next;
                    }
                    %s added = new %s(value, null);
                    if (previous == null) { this.head = added; } else { previous.next = added; }
                    if (value.%s >= this.nextId) this.nextId = value.%s + 1;
                    return value;
                }
                public void deleteById(int id) {
                    %s row = this.head;
                    %s previous = null;
                    while (row != null) {
                        if (row.value.%s == id) {
                            if (previous == null) { this.head = row.next; } else { previous.next = row.next; }
                            return;
                        }
                        previous = row; row = row.next;
                    }
                }
            }
            """.formatted(node, entity, node, node, entity, node, name, node, name,
                entity, node, key, entity, node, entity, entity, entity, entity, key, key,
                node, node, key, key, node, node, key, key, node, node, key);
        if (id.getTypeAsString().equals("Integer")) {
            source = source.replace("if (value." + key + " == 0)", "if (value." + key + " == null || value." + key + " == 0)")
                    .replace("row.value." + key + " == value." + key, "(int)row.value." + key + " == (int)value." + key);
        }
        CompilationUnit generated = StaticJavaParser.parse(source);
        String packageName = repo.containsData(JavaSourcePreparation.PACKAGE) ? repo.getData(JavaSourcePreparation.PACKAGE) : "";
        generated.getTypes().forEach(type -> type.setData(JavaSourcePreparation.PACKAGE, packageName));
        repo.replace(generated.getType(1).clone());
        cu.getTypes().add(generated.getType(0).clone());
    }

    private void lowerOptional(CompilationUnit cu) {
        int count = 0;
        for (MethodCallExpr call : new ArrayList<>(cu.findAll(MethodCallExpr.class))) {
            if (!call.getNameAsString().equals("orElseThrow")) continue;
            if (call.getArguments().size() != 1 || !call.getArgument(0).isLambdaExpr() || call.getScope().isEmpty()
                    || !call.getScope().get().isMethodCallExpr()) continue;
            MethodCallExpr lookup = call.getScope().get().asMethodCallExpr();
            MethodDeclaration outer = call.findAncestor(MethodDeclaration.class).orElseThrow();
            ClassOrInterfaceDeclaration owner = outer.findAncestor(ClassOrInterfaceDeclaration.class).orElseThrow();
            String repository = lookup.getScope().map(Object::toString).orElse("");
            String repoType = owner.getFields().stream().flatMap(f -> f.getVariables().stream())
                    .filter(v -> v.getNameAsString().equals(repository)).map(VariableDeclarator::getTypeAsString).findFirst().orElse("");
            String entity = repositories.get(repoType);
            if (!lookup.getNameAsString().equals("findById") || entity == null) continue;
            LambdaExpr lambda = call.getArgument(0).asLambdaExpr();
            Expression error = lambda.getExpressionBody().orElseThrow(() -> unsupported("Expression exception supplier required"));
            String helperName = "__javaw_require_" + count++;
            MethodDeclaration helper = new MethodDeclaration().setName(helperName).setType(entity).setStatic(true);
            helper.addParameter(entity, "__value");
            MethodCallExpr replacement = new MethodCallExpr(helperName).addArgument(lookup.clone());
            for (Parameter parameter : outer.getParameters()) {
                helper.addParameter(parameter.clone());
                replacement.addArgument(parameter.getNameAsString());
            }
            helper.setBody(StaticJavaParser.parseBlock("{ if (__value == null) throw " + error + "; return __value; }"));
            owner.addMember(helper);
            call.replace(replacement);
        }
    }

    private void lowerLists(CompilationUnit cu) {
        for (ClassOrInterfaceType type : new ArrayList<>(cu.findAll(ClassOrInterfaceType.class))) {
            if (type.getNameAsString().equals("List") && type.getTypeArguments().isPresent()
                    && type.getTypeArguments().get().size() == 1 && entities.contains(type.getTypeArguments().get().get(0).asString())) {
                type.replace(StaticJavaParser.parseType(type.getTypeArguments().get().get(0) + "[]"));
            }
        }
    }

    private void lowerStartup(CompilationUnit cu) {
        for (ClassOrInterfaceDeclaration application : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            if (!application.isAnnotationPresent("SpringBootApplication")) continue;
            List<MethodDeclaration> callbacks = new ArrayList<>();
            for (MethodDeclaration bean : new ArrayList<>(application.getMethods())) {
                if (!bean.isAnnotationPresent("Bean")) continue;
                if (!bean.getTypeAsString().equals("ApplicationRunner") || !bean.getParameters().isEmpty()
                        || bean.getBody().isEmpty() || bean.getBody().get().getStatements().size() != 1
                        || !bean.getBody().get().getStatement(0).isReturnStmt())
                    throw unsupported("@Bean requires an ApplicationRunner expression lambda: " + bean.getNameAsString());
                Expression returned = bean.getBody().get().getStatement(0).asReturnStmt().getExpression()
                        .orElseThrow(() -> unsupported("ApplicationRunner return value"));
                if (!returned.isLambdaExpr() || returned.asLambdaExpr().getParameters().size() != 1)
                    throw unsupported("ApplicationRunner requires a one-argument lambda");
                MethodDeclaration callback = new MethodDeclaration().setName("__javaw_runner_" + application.getNameAsString() + "_" + bean.getNameAsString()).setType("void");
                callback.addParameter("ApplicationArguments", "args");
                callback.setBody(StaticJavaParser.parseBlock("{ " + bean.getNameAsString() + "().run(args); }"));
                callbacks.add(callback);
            }
            if (!callbacks.isEmpty()) { callbacks.forEach(application::addMember); runners.put(application, callbacks); }
        }
    }


    private void addBootstrap(CompilationUnit cu) {
        SpringBeanGraph graph = new SpringBeanGraph(cu);
        for (String repo : repositories.keySet()) {
            var type = cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                    .filter(c -> c.getNameAsString().equals(repo)).findFirst().orElseThrow();
            graph.add(type, "__repo_" + repo);
        }
        for (var controller : controllers) graph.add(controller, "__controller_" + controller.getNameAsString());
        for (var application : runners.keySet()) graph.add(application, "__application_" + application.getNameAsString());
        for (var type : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            if (type.isAnnotationPresent("SpringBootApplication"))
                graph.add(type, "__application_" + type.getNameAsString());
            if (type.isAnnotationPresent("Service") || type.isAnnotationPresent("Component"))
                graph.add(type, "__bean_" + type.getNameAsString());
        }
        StringBuilder source = new StringBuilder("class __JavawSpringContext { private int marker; static int __boot_state;");
        source.append(graph.declarations());
        source.append("static void __javaw_boot() {")
                .append("if (__boot_state == 2) return;")
                .append("if (__boot_state != 0) throw new IllegalStateException(\"Spring context is initializing or failed\");")
                .append("__boot_state = 1; try {")
                .append(graph.initialization());
        for (var controller : controllers) {
            String name = controller.getNameAsString();
            String base = controller.getAnnotationByName("RequestMapping").map(SpringWebSupport::annotationValue).orElse("");
            for (MethodDeclaration method : controller.getMethods()) {
                for (String verb : List.of("Get", "Post", "Put", "Delete")) {
                    method.getAnnotationByName(verb + "Mapping").ifPresent(a -> routes.add(new Route(verb.toUpperCase(Locale.ROOT),
                            base + annotationValue(a), name, method)));
                }
            }
        }
        for (var entry : runners.entrySet()) {
            String name = entry.getKey().getNameAsString();
            for (MethodDeclaration callback : entry.getValue()) source.append("__application_").append(name).append(".")
                    .append(callback.getNameAsString()).append("(null);");
        }
        source.append("__boot_state = 2; } catch (Throwable failure) { __boot_state = 3; throw failure; } }");
        for (var controller : controllers) {
            String name = controller.getNameAsString();
            source.append("static ").append(name).append(" __javaw_controller_").append(name).append("() { return __controller_").append(name).append("; }");
        }
        source.append("}");
        var context = StaticJavaParser.parseTypeDeclaration(source.toString());
        context.setData(JavaSourcePreparation.PACKAGE, cu.getType(0).containsData(JavaSourcePreparation.PACKAGE)
                ? cu.getType(0).getData(JavaSourcePreparation.PACKAGE) : "");
        cu.getTypes().add(context);
    }

    private static String annotationValue(AnnotationExpr annotation) {
        if (annotation.isMarkerAnnotationExpr()) return "";
        Expression value = annotation.isSingleMemberAnnotationExpr() ? annotation.asSingleMemberAnnotationExpr().getMemberValue()
                : annotation.asNormalAnnotationExpr().getPairs().stream().filter(p -> Set.of("value", "path").contains(p.getNameAsString()))
                .map(MemberValuePair::getValue).findFirst().orElseThrow(() -> unsupported("Mapping path"));
        if (!value.isStringLiteralExpr()) throw unsupported("Single string mapping path required");
        return value.asStringLiteralExpr().asString();
    }

    void emitMetadata(ClassRegistry registry, StringBuilder wat) {
        if (routes.isEmpty()) return;
        StringBuilder json = new StringBuilder("{\"routes\":[");
        StringJoiner routeJson = new StringJoiner(",");
        for (Route route : routes) {
            MethodDeclaration method = route.method();
            StringJoiner params = new StringJoiner(",");
            for (Parameter parameter : method.getParameters()) {
                if (!VALUE_TYPES.contains(parameter.getTypeAsString())) throw unsupported("HTTP parameter " + parameter);
                String parameterName = parameter.getNameAsString();
                if (parameter.isAnnotationPresent("PathVariable")) {
                    String specified = annotationValue(parameter.getAnnotationByName("PathVariable").orElseThrow());
                    if (!specified.isEmpty()) parameterName = specified;
                }
                params.add("{\"name\":" + quote(parameterName) + ",\"type\":" + quote(parameter.getTypeAsString())
                        + ",\"path\":" + parameter.isAnnotationPresent("PathVariable") + "}");
            }
            String resultType = method.getTypeAsString();
            if (!resultType.equals("void") && !entities.contains(resultType)
                    && !(resultType.endsWith("[]") && entities.contains(resultType.substring(0, resultType.length() - 2))))
                throw unsupported("HTTP result " + resultType);
            routeJson.add("{\"verb\":" + quote(route.verb()) + ",\"path\":" + quote(route.path()) + ",\"controller\":" + quote(route.controller())
                    + ",\"export\":" + quote(route.controller() + "_" + method.getNameAsString()) + ",\"result\":" + quote(method.getTypeAsString())
                    + ",\"params\":[" + params + "]}");
        }
        json.append(routeJson).append("],\"entities\":{");
        StringJoiner models = new StringJoiner(",");
        for (String name : entities) {
            ClassModel model = registry.getClass(name);
            StringJoiner fields = new StringJoiner(",");
            for (FieldModel field : model.getFields().values()) {
                if (!field.isStatic() && !VALUE_TYPES.contains(field.getTypeName()))
                    throw unsupported("Entity field " + name + "." + field.getName() + ": " + field.getTypeName());
                if (!field.isStatic()) fields.add("{\"name\":" + quote(field.getName()) + ",\"type\":" + quote(field.getTypeName())
                        + ",\"offset\":" + field.getOffset() + (field.getTypeName().equals("Integer")
                            ? ",\"boxedOffset\":" + registry.getClass("Integer").getField("__boxedValue").getOffset() : "") + "}");
            }
            models.add(quote(name) + ":[" + fields + "]");
        }
        ClassModel exception = registry.getClass("ResponseStatusException");
        json.append(models).append("},\"parseDecimal\":").append(registry.hasClass("BigDecimal") ? quote(registry.findMethod("BigDecimal","__javaw_parse").getFunctionName().substring(1)) : "null")
                .append(",\"formatDecimal\":").append(registry.hasClass("BigDecimal") ? quote(registry.findMethod("BigDecimal","__javaw_format").getFunctionName().substring(1)) : "null")
                .append(",\"boxInteger\":").append(quote(registry.boxingFactory("Integer").getFunctionName().substring(1))).append(",\"exception\":{\"vtable\":").append(exception.getVtableBase())
                .append(",\"status\":").append(exception.getField("status").getOffset())
                .append(",\"reason\":").append(exception.getField("reason").getOffset()).append("}}");
        wat.append("  (@custom \"javaw_spring_web\" \"")
                .append(com.example.javaw.sourcemap.SourceMapGenerator.escapeRawCustomSectionPayload(json.toString())).append("\")\n");
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }
    private static IllegalArgumentException unsupported(String detail) {
        return new IllegalArgumentException("Unsupported Spring WebAssembly construct: " + detail);
    }
}
