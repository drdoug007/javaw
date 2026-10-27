package one.dastec.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import java.util.ArrayList;

/** Audits user input before generated library types enter the compilation unit. */
final class SourceRuntimeSupport {
    static void prepare(CompilationUnit cu) {
        for (DoStmt statement : cu.findAll(DoStmt.class))
            throw JavaSourcePreparation.error(statement, "Unsupported do-while statement: rewrite using a while loop with an explicit first iteration");
        for (SynchronizedStmt statement : cu.findAll(SynchronizedStmt.class))
            throw JavaSourcePreparation.error(statement, "Unsupported synchronized block: monitors require a worker-safe runtime; use explicit host coordination");
        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class))
            if (method.isSynchronized()) throw JavaSourcePreparation.error(method,
                    "Unsupported synchronized method: monitors require a worker-safe runtime; use explicit host coordination");
        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            if (!method.isNative()) continue;
            var owner = method.findAncestor(ClassOrInterfaceDeclaration.class);
            var js = one.dastec.javaw.js.JsAnnotationHelper.getJsAnnotation(method).isPresent()
                    || one.dastec.javaw.js.JsAnnotationHelper.getMethodImportAnnotation(method).isPresent()
                    || owner.map(t -> one.dastec.javaw.js.JsAnnotationHelper.getClassImportAnnotation(t).isPresent()).orElse(false);
            if (!js) throw JavaSourcePreparation.error(method,
                    "Unsupported native method: provide a @JS or @JS.Import host implementation");
        }
        for (FieldDeclaration field : cu.findAll(FieldDeclaration.class))
            if (field.isVolatile()) throw JavaSourcePreparation.error(field,
                    "Unsupported volatile field: Java Memory Model visibility is unavailable; use the explicit AtomicInteger facade");
        for (ObjectCreationExpr creation : cu.findAll(ObjectCreationExpr.class))
            if (creation.getAnonymousClassBody().isPresent()) throw JavaSourcePreparation.error(creation,
                    "Unsupported anonymous class: move the implementation to a named top-level class");
        var names = new java.util.HashSet<String>();
        for (TypeDeclaration<?> type : cu.findAll(TypeDeclaration.class)) {
            if (!names.add(type.getNameAsString())) throw JavaSourcePreparation.error(type,
                    "Unsupported duplicate flattened type name '" + type.getNameAsString() + "'; use unique simple names across the module");
            var parent = type.getParentNode().orElse(null);
            boolean staticMember = parent instanceof TypeDeclaration<?> && (type.isStatic()
                    || type.isEnumDeclaration() || type.isRecordDeclaration()
                    || type instanceof ClassOrInterfaceDeclaration declaration && declaration.isInterface()
                    || parent instanceof ClassOrInterfaceDeclaration declaration && declaration.isInterface());
            if (!(parent instanceof CompilationUnit) && !staticMember) throw JavaSourcePreparation.error(type,
                    "Unsupported non-static inner/local type: move '" + type.getNameAsString() + "' to a top-level declaration");
        }
        var assertions = new ArrayList<>(cu.findAll(AssertStmt.class));
        if (assertions.isEmpty()) return;
        if (cu.getTypes().stream().anyMatch(t -> t.getNameAsString().equals("__JavawAssertions")))
            throw JavaSourcePreparation.error(cu, "Reserved assertion helper type __JavawAssertions; rename the user type");
        for (AssertStmt statement : assertions) {
            var failure = new ObjectCreationExpr().setType("AssertionError");
            statement.getMessage().ifPresent(message -> failure.addArgument(new BinaryExpr(
                    new StringLiteralExpr(""), message.clone(), BinaryExpr.Operator.PLUS)));
            var enabled = new FieldAccessExpr(new NameExpr("__JavawAssertions"), "__javaw_assertions_enabled");
            var condition = new UnaryExpr(new EnclosedExpr(statement.getCheck().clone()), UnaryExpr.Operator.LOGICAL_COMPLEMENT);
            statement.replace(new IfStmt(new BinaryExpr(enabled, condition, BinaryExpr.Operator.AND),
                    new ThrowStmt(failure), null));
        }
        var helper = StaticJavaParser.parse("""
            class __JavawAssertions {
                public static boolean __javaw_assertions_enabled;
                public static void __javaw_set_assertions(boolean enabled) { __javaw_assertions_enabled = enabled; }
            }
            """);
        helper.getTypes().forEach(type -> cu.getTypes().add(type.clone()));
    }
}
