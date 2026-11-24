package one.dastec.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.*;

/**
 * Field initializers and initializer blocks (JLS 12.5, 12.4.2), lowered to ordinary methods before the
 * generator sees them.
 *
 * <ul>
 *   <li>Instance field initializers and instance initializer blocks move, in declaration order, into a
 *       per-class method that every constructor not delegating with {@code this(...)} calls right after
 *       the superclass constructor. A class without constructors gets an explicit no-argument one.</li>
 *   <li>Static field initializers and static blocks move, in declaration order, into a per-class static
 *       method. Only {@code static final} primitive literals stay folded into the global's initial value
 *       (they are constant variables, initialized first by the JVM too). The module start function runs
 *       every class's static initialization eagerly, dependencies first, then source order.</li>
 * </ul>
 */
final class FieldInitializerSupport {
    static final String RUNNER_CLASS = "__JavawStaticInit";
    static final String RUNNER_METHOD = "__javaw_run_static_init";

    private FieldInitializerSupport() {}

    /** Static initializers folded into the global's initial value instead of running in class initialization. */
    static boolean folded(FieldDeclaration field, VariableDeclarator variable) {
        if (!field.isStatic() || variable.getInitializer().isEmpty()) return false;
        boolean constant = field.isFinal() || field.findAncestor(ClassOrInterfaceDeclaration.class).map(ClassOrInterfaceDeclaration::isInterface).orElse(false);
        if (!constant || BoxingSupport.boxed(variable.getTypeAsString())) return false;
        Expression init = variable.getInitializer().get();
        return init.isIntegerLiteralExpr() || init.isDoubleLiteralExpr() || init.isBooleanLiteralExpr();
    }

    /** Logger facades (LogManager.getLogger) are resolved from the declaration at compile time; they have no runtime value. */
    static boolean compileTimeIntrinsic(VariableDeclarator variable) {
        return variable.getInitializer().filter(Expression::isMethodCallExpr).map(Expression::asMethodCallExpr)
                .flatMap(MethodCallExpr::getScope).map(Object::toString)
                .filter(scope -> scope.equals("LogManager") || scope.endsWith(".LogManager")).isPresent();
    }

    /** Returns whether a static initialization runner was added. */
    static boolean lower(CompilationUnit cu) {
        Map<String, BlockStmt> statics = new LinkedHashMap<>();
        List<TypeDeclaration<?>> types = new ArrayList<>();
        for (TypeDeclaration<?> type : cu.findAll(TypeDeclaration.class)) types.add(type);
        for (TypeDeclaration<?> type : types) {
            if (type.getNameAsString().startsWith("__JavawLambda")) continue;
            if (type instanceof ClassOrInterfaceDeclaration c && !c.isInterface()) lowerInstance(c, c.getConstructors());
            else if (type instanceof EnumDeclaration e) lowerInstance(e, e.getConstructors());
            BlockStmt body = staticBody(type);
            if (body == null) continue;
            if (type instanceof RecordDeclaration || (type instanceof ClassOrInterfaceDeclaration c && c.isInterface()))
                throw JavaSourcePreparation.error(body.getStatements().isEmpty() ? type : body.getStatement(0),
                        "Static initialization in " + (type instanceof RecordDeclaration ? "records" : "interfaces")
                                + " is limited to final primitive literals for now; move it to a class");
            statics.put(type.getNameAsString(), body);
        }
        if (statics.isEmpty()) return false;
        for (Map.Entry<String, BlockStmt> entry : statics.entrySet()) {
            TypeDeclaration<?> owner = types.stream().filter(t -> t.getNameAsString().equals(entry.getKey())).findFirst().orElseThrow();
            owner.addMember(new MethodDeclaration(new NodeList<>(Modifier.publicModifier(), Modifier.staticModifier()),
                    StaticJavaParser.parseType("void"), clinitName(entry.getKey())).setBody(entry.getValue()));
        }
        ClassOrInterfaceDeclaration runner = new ClassOrInterfaceDeclaration().setName(RUNNER_CLASS);
        BlockStmt run = new BlockStmt();
        for (String name : initializationOrder(statics))
            run.addStatement(new MethodCallExpr(new NameExpr(name), clinitName(name)));
        runner.addMethod(RUNNER_METHOD, Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC).setBody(run);
        cu.getTypes().add(runner);
        return true;
    }

    private static String clinitName(String type) {
        return "__javaw_clinit_" + type;
    }

    private static void lowerInstance(TypeDeclaration<?> type, List<ConstructorDeclaration> constructors) {
        BlockStmt body = new BlockStmt();
        for (BodyDeclaration<?> member : new ArrayList<>(type.getMembers())) {
            if (member instanceof FieldDeclaration field && !field.isStatic()) {
                for (VariableDeclarator variable : field.getVariables()) {
                    if (variable.getInitializer().isEmpty()) continue;
                    body.addStatement(assignment(new FieldAccessExpr(new ThisExpr(), variable.getNameAsString()), variable));
                    variable.removeInitializer();
                }
            } else if (member instanceof InitializerDeclaration block && !block.isStatic()) {
                body.addStatement(block.getBody().clone());
                block.remove();
            }
        }
        if (body.isEmpty()) return;
        String method = "__javaw_fields_" + type.getNameAsString();
        type.addMember(new MethodDeclaration(new NodeList<>(Modifier.privateModifier()), StaticJavaParser.parseType("void"), method).setBody(body));
        List<ConstructorDeclaration> targets = new ArrayList<>(constructors);
        if (targets.isEmpty()) {
            ConstructorDeclaration implicit = new ConstructorDeclaration(new NodeList<>(type instanceof EnumDeclaration
                    ? Modifier.privateModifier() : Modifier.publicModifier()), type.getNameAsString());
            type.getMembers().add(0, implicit);
            targets.add(implicit);
        }
        for (ConstructorDeclaration constructor : targets) {
            NodeList<Statement> statements = constructor.getBody().getStatements();
            boolean explicit = !statements.isEmpty() && statements.get(0) instanceof ExplicitConstructorInvocationStmt;
            if (explicit && ((ExplicitConstructorInvocationStmt) statements.get(0)).isThis()) continue; // the delegate runs them
            statements.add(explicit ? 1 : 0, new ExpressionStmt(new MethodCallExpr(new ThisExpr(), method)));
        }
    }

    private static BlockStmt staticBody(TypeDeclaration<?> type) {
        BlockStmt body = new BlockStmt();
        for (BodyDeclaration<?> member : new ArrayList<>(type.getMembers())) {
            if (member instanceof FieldDeclaration field && field.isStatic()) {
                for (VariableDeclarator variable : field.getVariables()) {
                    if (variable.getInitializer().isEmpty() || folded(field, variable) || compileTimeIntrinsic(variable)) continue;
                    body.addStatement(assignment(new NameExpr(variable.getNameAsString()), variable));
                    variable.removeInitializer();
                }
            } else if (member instanceof InitializerDeclaration block && block.isStatic()) {
                body.addStatement(block.getBody().clone());
                block.remove();
            }
        }
        return body.isEmpty() ? null : body;
    }

    /**
     * {@code { T tmp = init; target = tmp; }}: the initializer keeps its declaration context, so diamonds,
     * lambdas, array initializers and generic factories infer exactly as they did in the field declaration.
     */
    private static Statement assignment(Expression target, VariableDeclarator variable) {
        String local = "__javaw_init_" + variable.getNameAsString();
        BlockStmt block = new BlockStmt();
        block.addStatement(new ExpressionStmt(new VariableDeclarationExpr(
                new VariableDeclarator(variable.getType().clone(), local, variable.getInitializer().get().clone()))));
        block.addStatement(new ExpressionStmt(new AssignExpr(target, new NameExpr(local), AssignExpr.Operator.ASSIGN)));
        return block;
    }

    /** Classes whose static initialization another class's initializers use run first; otherwise source order. */
    private static List<String> initializationOrder(Map<String, BlockStmt> statics) {
        Map<String, Set<String>> dependencies = new LinkedHashMap<>();
        for (Map.Entry<String, BlockStmt> entry : statics.entrySet()) {
            Set<String> uses = new LinkedHashSet<>();
            for (Node node : entry.getValue().findAll(Node.class)) {
                String name = node instanceof NameExpr n ? n.getNameAsString()
                        : node instanceof ClassOrInterfaceType t ? t.getNameAsString() : null;
                if (name != null && !name.equals(entry.getKey()) && statics.containsKey(name)) uses.add(name);
            }
            dependencies.put(entry.getKey(), uses);
        }
        List<String> order = new ArrayList<>();
        Set<String> visiting = new HashSet<>();
        for (String name : statics.keySet()) visit(name, dependencies, visiting, order);
        return order;
    }

    private static void visit(String name, Map<String, Set<String>> dependencies, Set<String> visiting, List<String> order) {
        if (order.contains(name) || !visiting.add(name)) return; // cycles fall back to source order
        for (String dependency : dependencies.get(name)) visit(dependency, dependencies, visiting, order);
        order.add(name);
    }
}
