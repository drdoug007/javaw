package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import java.util.*;

/** Synchronous REQUIRED boundaries and repository snapshots for the source Spring subset. */
final class SpringTransactions {
    private final List<MethodDeclaration> methods = new ArrayList<>();
    private boolean enabled;

    static SpringTransactions prepare(CompilationUnit cu) {
        SpringTransactions result = new SpringTransactions();
        for (var annotation : cu.findAll(AnnotationExpr.class)) {
            if (!annotation.getName().getIdentifier().equals("Transactional")) continue;
            result.enabled = true;
            if (!annotation.isMarkerAnnotationExpr()
                    && !(annotation.isNormalAnnotationExpr() && annotation.asNormalAnnotationExpr().getPairs().isEmpty()))
                throw error(annotation, "@Transactional options are unsupported; use default REQUIRED and rollback rules");
            Node target = annotation.getParentNode().orElseThrow();
            var owner = target instanceof ClassOrInterfaceDeclaration type ? type
                    : target.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
            if (owner == null || owner.isInterface() || owner.isAbstract()
                    || !(owner.isAnnotationPresent("Service") || owner.isAnnotationPresent("Component")
                    || owner.isAnnotationPresent("RestController") || owner.isAnnotationPresent("SpringBootApplication")))
                throw error(annotation, "@Transactional requires a concrete managed service/component/controller/application");
            if (!owner.getExtendedTypes().isEmpty())
                throw error(annotation, "Transactional managed classes with a superclass are unsupported; use interface composition");
            if (!(target instanceof ClassOrInterfaceDeclaration) && !(target instanceof MethodDeclaration))
                throw error(annotation, "@Transactional is supported only on classes and public instance methods");
            if (target instanceof MethodDeclaration method) validate(method);
        }
        for (var type : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            boolean all = type.isAnnotationPresent("Transactional");
            for (var method : type.getMethods()) {
                if (method.isAnnotationPresent("Transactional") || all && method.isPublic() && !method.isStatic() && !method.isAnnotationPresent("Bean")) {
                    validate(method);
                    result.methods.add(method);
                }
            }
        }
        return result;
    }

    boolean enabled() { return enabled; }

    private static void validate(MethodDeclaration method) {
        if (!method.isPublic() || method.isStatic() || method.isAbstract() || method.isNative() || method.getBody().isEmpty()
                || !method.getTypeParameters().isEmpty() || method.getParameters().stream().anyMatch(Parameter::isVarArgs)
                || method.isAnnotationPresent("Bean"))
            throw error(method, "Transactional methods must be public concrete non-generic instance methods without varargs or @Bean");
    }

    static void addSnapshot(CompilationUnit generated, ClassOrInterfaceDeclaration entity, String repository, String row) {
        if (!entity.getExtendedTypes().isEmpty())
            throw error(entity, "Transactional repository entities must declare their fields directly; inherited state is unsupported");
        String snapshot = "__JavawSnapshot_" + repository;
        StringBuilder fields = new StringBuilder(), capture = new StringBuilder(), restore = new StringBuilder();
        for (var field : entity.getFields()) {
            if (field.isStatic() || field.isFinal()) continue;
            for (var variable : field.getVariables()) {
                if (!Set.of("String", "int", "Integer", "BigDecimal").contains(variable.getTypeAsString()))
                    throw error(variable, "Unsupported transactional entity field " + variable.getTypeAsString());
                String name = variable.getNameAsString();
                fields.append(variable.getTypeAsString()).append(" __saved_").append(name).append(';');
                capture.append("this.__saved_").append(name).append(" = source.value.").append(name).append(';');
                restore.append("this.value.").append(name).append(" = this.__saved_").append(name).append(';');
            }
        }
        String type = "class " + snapshot + " { " + row + " row; " + row + " savedNext; " + entity.getNameAsString()
                + " value; " + snapshot + " next; " + fields
                + snapshot + "(" + row + " source," + snapshot + " next) { this.row=source; this.savedNext=source.next;"
                + "this.value=source.value; this.next=next;" + capture + "}"
                + "void restore() { this.row.next=this.savedNext; this.row.value=this.value;" + restore + "} }";
        generated.getTypes().add(StaticJavaParser.parseTypeDeclaration(type));
        var repo = generated.getType(1).asClassOrInterfaceDeclaration();
        repo.addField(row, "__tx_head");
        repo.addField(snapshot, "__tx_snapshot");
        repo.addMember(StaticJavaParser.parseBodyDeclaration("public void __tx_begin() { this.__tx_head=this.head; this.__tx_snapshot=null; "
                + row + " cursor=this.head; while(cursor!=null) { this.__tx_snapshot=new " + snapshot
                + "(cursor,this.__tx_snapshot); cursor=cursor.next; } }"));
        repo.addMember(StaticJavaParser.parseBodyDeclaration("public void __tx_commit() { this.__tx_head=null; this.__tx_snapshot=null; }"));
        repo.addMember(StaticJavaParser.parseBodyDeclaration("public void __tx_rollback() { while(this.__tx_snapshot!=null) {"
                + "this.__tx_snapshot.restore(); this.__tx_snapshot=this.__tx_snapshot.next; }"
                + "this.head=this.__tx_head; this.__tx_head=null; }"));
    }

    void lower(CompilationUnit cu, Set<String> repositories) {
        if (!enabled) return;
        StringBuilder begin = new StringBuilder(), commit = new StringBuilder(), rollback = new StringBuilder();
        for (String repository : repositories) {
            String receiver = "__JavawSpringContext.__repo_" + repository;
            begin.append(receiver).append(".__tx_begin();");
            commit.append(receiver).append(".__tx_commit();");
            rollback.append(receiver).append(".__tx_rollback();");
        }
        LibrarySupport.add(cu, "org.springframework.transaction", """
            class UnexpectedRollbackException extends RuntimeException {
                public UnexpectedRollbackException(String message) { super(message); }
            }
            """);
        String manager = """
            class __JavawTransactions {
                static int __tx_depth;
                static boolean __tx_rollback_only;
                public static void begin() {
                    if(!__JavawSpringContext.__tx_ready || __JavawSpringContext.__boot_state==3) throw new IllegalStateException("Transactions require initialized beans");
                    if(__tx_depth==0) { __tx_rollback_only=false; %s }
                    __tx_depth++;
                }
                public static void success() {
                    __tx_depth--;
                    if(__tx_depth==0) {
                        if(__tx_rollback_only) { %s __tx_rollback_only=false; throw new UnexpectedRollbackException("Joined transaction marked rollback-only"); }
                        %s
                    }
                }
                public static void failure(Throwable failure) {
                    if(failure instanceof RuntimeException || failure instanceof Error) __tx_rollback_only=true;
                    __tx_depth--;
                    if(__tx_depth==0) {
                        if(__tx_rollback_only) { %s } else { %s }
                        __tx_rollback_only=false;
                    }
                }
            }
            """.formatted(begin, rollback, commit, rollback, commit);
        cu.getTypes().add(StaticJavaParser.parseTypeDeclaration(manager));
        int index = 0;
        for (MethodDeclaration method : methods) {
            var owner = method.findAncestor(ClassOrInterfaceDeclaration.class).orElseThrow();
            String helperName = "__javaw_tx_body_" + method.getNameAsString() + "_" + index++;
            MethodDeclaration helper = method.clone().setName(helperName).setPublic(false).setProtected(false).setPrivate(true);
            helper.getAnnotations().removeIf(a -> a.getName().getIdentifier().equals("Transactional"));
            owner.addMember(helper);
            StringJoiner arguments = new StringJoiner(",");
            method.getParameters().forEach(p -> arguments.add(p.getNameAsString()));
            String invocation = "this." + helperName + "(" + arguments + ");";
            boolean isVoid = method.getType().isVoidType();
            String singleton = owner.isAnnotationPresent("RestController") ? "__controller_"
                    : owner.isAnnotationPresent("SpringBootApplication") ? "__application_" : "__bean_";
            String body = "{ if(this != __JavawSpringContext." + singleton + owner.getNameAsString()
                    + ") throw new IllegalStateException(\"Transactional methods require the managed singleton\"); __JavawTransactions.begin(); "
                    + (isVoid ? "" : method.getTypeAsString() + " __tx_result;")
                    + "try { " + (isVoid ? "" : "__tx_result = ") + invocation + "}"
                    + "catch(Throwable __tx_failure) { __JavawTransactions.failure(__tx_failure); throw __tx_failure; }"
                    + "__JavawTransactions.success();" + (isVoid ? "" : "return __tx_result;") + "}";
            method.setBody(StaticJavaParser.parseBlock(body));
        }
    }

    private static IllegalArgumentException error(Node node, String message) {
        return JavaSourcePreparation.error(node, "Unsupported Spring transaction: " + message);
    }
}
