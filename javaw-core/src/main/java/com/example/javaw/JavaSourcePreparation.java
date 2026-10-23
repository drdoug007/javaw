package com.example.javaw;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.*;

/** Resolves source names before function-wide Wasm local allocation. */
final class JavaSourcePreparation {
    private static final DataKey<String> SOURCE = new DataKey<>() {};

    static void markSource(CompilationUnit cu, String source) {
        cu.getTypes().forEach(type -> { if (!type.containsData(SOURCE)) type.setData(SOURCE, source); });
    }

    static Map<String, String> typeNames(List<CompilationUnit> units) {
        Map<String, String> names = new LinkedHashMap<>();
        Set<String> simpleNames = new HashSet<>();
        for (CompilationUnit cu : units) {
            String prefix = cu.getPackageDeclaration().map(p -> p.getNameAsString() + ".").orElse("");
            for (TypeDeclaration<?> type : cu.getTypes()) {
                String simple = type.getNameAsString();
                if (!simpleNames.add(simple)) throw error(type, "Duplicate simple type name '" + simple
                        + "'; separate namespaces in one Wasm module are not supported yet");
                names.put(prefix + simple, simple);
            }
        }
        for (String name : List.of("String", "Object", "Class", "Integer", "Long", "Double", "Float", "Boolean",
                "Byte", "Short", "Character", "Math", "StrictMath", "RuntimeException", "Exception", "Throwable"))
            names.put("java.lang." + name, name);
        return names;
    }

    static void normalizeTypes(CompilationUnit cu, Map<String, String> names) {
        for (ClassOrInterfaceType type : new ArrayList<>(cu.findAll(ClassOrInterfaceType.class))) {
            String name = names.get(type.getNameWithScope());
            if (name != null && type.getScope().isPresent()) { type.removeScope(); type.setName(name); }
        }
        for (FieldAccessExpr access : new ArrayList<>(cu.findAll(FieldAccessExpr.class))) {
            String name = names.get(access.toString());
            if (name != null) access.replace(new NameExpr(name));
        }
    }

    static IllegalArgumentException error(Node node, String message) {
        String source = node.stream(Node.TreeTraversal.PARENTS).filter(n -> n.containsData(SOURCE))
                .findFirst().map(n -> n.getData(SOURCE)).orElseGet(() -> node.findCompilationUnit()
                        .flatMap(CompilationUnit::getStorage).map(storage -> storage.getPath().getFileName().toString()).orElse("Source.java"));
        if (node.containsData(SOURCE)) source = node.getData(SOURCE);
        String location = node.getBegin().map(p -> ":" + p.line + ":" + p.column).orElse("");
        return new IllegalArgumentException(source + location + ": " + message);
    }

    static void resolveLocalScopes(CompilationUnit cu) {
        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class))
            method.getBody().ifPresent(body -> new Locals(method, body).resolve(body));
        for (ConstructorDeclaration constructor : cu.findAll(ConstructorDeclaration.class))
            new Locals(constructor, constructor.getBody()).resolve(constructor.getBody());
    }

    private static final class Locals extends VoidVisitorAdapter<Void> {
        private final Deque<Map<String, String>> scopes = new ArrayDeque<>();
        private final Set<String> used = new HashSet<>();
        private final Set<String> locals = new HashSet<>();
        private final Set<String> fields = new HashSet<>();
        private int sequence;
        Locals(CallableDeclaration<?> callable, BlockStmt body) {
            scopes.push(new HashMap<>());
            for (Parameter p : callable.getParameters()) { scopes.peek().put(p.getNameAsString(), p.getNameAsString()); used.add(p.getNameAsString()); }
            body.findAll(VariableDeclarator.class).forEach(v -> locals.add(v.getNameAsString()));
            Map<String, ClassOrInterfaceDeclaration> types = new HashMap<>();
            callable.findCompilationUnit().ifPresent(cu -> cu.findAll(ClassOrInterfaceDeclaration.class).forEach(c -> types.put(c.getNameAsString(), c)));
            ClassOrInterfaceDeclaration owner = callable.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
            Set<String> visited = new HashSet<>();
            while (owner != null && visited.add(owner.getNameAsString())) {
                owner.getFields().forEach(f -> f.getVariables().forEach(v -> fields.add(v.getNameAsString())));
                owner = owner.getExtendedTypes().isEmpty() ? null : types.get(owner.getExtendedTypes(0).getNameAsString());
            }
        }
        void resolve(BlockStmt body) { body.accept(this, null); }
        private void scoped(Node node) { scopes.push(new HashMap<>()); node.getChildNodes().forEach(n -> n.accept(this, null)); scopes.pop(); }
        private String lookup(String name) { for (Map<String, String> scope : scopes) if (scope.containsKey(name)) return scope.get(name); return null; }
        private void declare(SimpleName name) {
            String original = name.asString();
            if (lookup(original) != null) throw error(name, "Local '" + original + "' is already declared in an enclosing scope");
            String resolved = original;
            while (used.contains(resolved) || fields.contains(resolved)) resolved = original + "$local" + ++sequence;
            used.add(resolved); scopes.peek().put(original, resolved); name.setIdentifier(resolved);
        }
        @Override public void visit(BlockStmt n, Void unused) { scoped(n); }
        @Override public void visit(ForStmt n, Void unused) {
            scopes.push(new HashMap<>());
            n.getInitialization().forEach(e -> e.accept(this, null));
            n.getCompare().ifPresent(e -> e.accept(this, null));
            n.getBody().accept(this, null); n.getUpdate().forEach(e -> e.accept(this, null)); scopes.pop();
        }
        @Override public void visit(ForEachStmt n, Void unused) {
            n.getIterable().accept(this, null); scopes.push(new HashMap<>());
            n.getVariable().accept(this, null); n.getBody().accept(this, null); scopes.pop();
        }
        @Override public void visit(CatchClause n, Void unused) {
            scopes.push(new HashMap<>()); declare(n.getParameter().getName()); n.getBody().accept(this, null); scopes.pop();
        }
        @Override public void visit(TryStmt n, Void unused) {
            scopes.push(new HashMap<>()); n.getResources().forEach(e -> e.accept(this, null)); n.getTryBlock().accept(this, null); scopes.pop();
            n.getCatchClauses().forEach(c -> c.accept(this, null)); n.getFinallyBlock().ifPresent(b -> b.accept(this, null));
        }
        @Override public void visit(SwitchStmt n, Void unused) { scoped(n); }
        @Override public void visit(SwitchExpr n, Void unused) { scoped(n); }
        @Override public void visit(VariableDeclarator n, Void unused) {
            if (n.getInitializer().stream().flatMap(e -> e.findAll(NameExpr.class).stream())
                    .anyMatch(e -> e.getNameAsString().equals(n.getNameAsString())))
                throw error(n, "Local '" + n.getNameAsString() + "' refers to itself before initialization; use a different name or an explicit field reference");
            n.getInitializer().ifPresent(e -> e.accept(this, null)); declare(n.getName());
        }
        @Override public void visit(NameExpr n, Void unused) {
            String name = n.getNameAsString(), resolved = lookup(name);
            if (resolved != null) n.setName(resolved);
            else if (locals.contains(name) && !fields.contains(name)) throw error(n, "Local '" + name + "' is not in scope; declare it before use in this block");
        }
        // Bodies of nested callables have their own local namespace.
        @Override public void visit(ClassOrInterfaceDeclaration n, Void unused) { }
        @Override public void visit(LambdaExpr n, Void unused) { }
    }
}
