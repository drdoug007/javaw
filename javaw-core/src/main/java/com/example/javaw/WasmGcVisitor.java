package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import java.util.*;

/** Experimental standalone WasmGC backend. No linear-memory runtime is linked. */
public final class WasmGcVisitor {
    private final Map<String, ClassOrInterfaceDeclaration> classes = new LinkedHashMap<>();
    private final Map<String, String> locals = new LinkedHashMap<>();
    private String owner;

    public static String generateWat(String source) {
        return new WasmGcVisitor().generate(source);
    }

    private String generate(String source) {
        var cu = StaticJavaParser.parse(source);
        for (var declaration : cu.getTypes()) {
            if (!declaration.isClassOrInterfaceDeclaration()) throw unsupported(declaration);
            var c = declaration.asClassOrInterfaceDeclaration();
            if (c.isInterface() || !c.getExtendedTypes().isEmpty() || !c.getImplementedTypes().isEmpty()
                    || !c.getTypeParameters().isEmpty() || !c.getAnnotations().isEmpty()) throw unsupported(c);
            classes.put(c.getNameAsString(), c);
        }
        StringBuilder wat = new StringBuilder("(module\n (rec\n");
        for (var c : classes.values()) {
            wat.append("  (type $").append(c.getNameAsString()).append(" (struct");
            for (var member : c.getMembers()) {
                if (member.isFieldDeclaration()) {
                    var f = member.asFieldDeclaration();
                    if (f.isStatic() || f.isVolatile() || !f.getAnnotations().isEmpty()) throw unsupported(f);
                    for (var v : f.getVariables()) {
                        if (v.getInitializer().isPresent() && !v.getInitializer().get().isIntegerLiteralExpr()
                                && !v.getInitializer().get().isBooleanLiteralExpr()) throw unsupported(v);
                        wat.append(" (field $").append(v.getNameAsString())
                            .append(" (mut ").append(type(v.getTypeAsString(), v)).append("))");
                    }
                } else if (!member.isMethodDeclaration() && !member.isConstructorDeclaration()) throw unsupported(member);
            }
            wat.append("))\n");
        }
        wat.append(" )\n");
        for (var c : classes.values()) {
            owner = c.getNameAsString();
            if (c.getConstructors().size() > 1) throw unsupported(c.getConstructors().get(1));
            for (var m : c.getMethods()) {
                if (c.getMethodsByName(m.getNameAsString()).size() > 1 || m.isAbstract() || m.isNative()
                        || m.isSynchronized() || !m.getTypeParameters().isEmpty() || !m.getAnnotations().isEmpty()) throw unsupported(m);
                wat.append(function(m.getNameAsString(), m.getParameters(), m.getBody().orElseThrow(),
                        m.getTypeAsString(), m.isStatic(), true));
            }
            var constructor = c.getConstructors().stream().findFirst();
            constructor.ifPresent(k -> {
                if (!k.getTypeParameters().isEmpty() || !k.getAnnotations().isEmpty()) throw unsupported(k);
            });
            wat.append(function("new", constructor.map(ConstructorDeclaration::getParameters).orElseGet(com.github.javaparser.ast.NodeList::new),
                    constructor.map(ConstructorDeclaration::getBody).orElseGet(BlockStmt::new), owner, true, false));
        }
        return wat.append(")\n").toString();
    }

    private String function(String name, List<Parameter> parameters, BlockStmt body, String result, boolean isStatic, boolean method) {
        locals.clear();
        StringBuilder head = new StringBuilder(" (func $").append(owner).append('_').append(name);
        if (method) head.append(" (export \"").append(owner).append('.').append(name).append("\")");
        if (!isStatic || !method) locals.put("this", owner);
        if (!isStatic) head.append(" (param $this (ref null $").append(owner).append("))");
        for (var p : parameters) {
            if (p.isVarArgs()) throw unsupported(p);
            if (locals.putIfAbsent(p.getNameAsString(), p.getTypeAsString()) != null) throw unsupported(p);
            head.append(" (param $").append(p.getNameAsString()).append(' ').append(type(p.getTypeAsString(), p)).append(')');
        }
        if (!result.equals("void")) head.append(" (result ").append(type(result, body)).append(')');
        if (!method) head.append(" (local $this (ref null $").append(owner).append("))");
        for (var v : body.findAll(VariableDeclarator.class)) {
            if (locals.putIfAbsent(v.getNameAsString(), v.getTypeAsString()) != null) throw unsupported(v);
            head.append(" (local $").append(v.getNameAsString()).append(' ').append(type(v.getTypeAsString(), v)).append(')');
        }
        head.append('\n');
        if (!method) {
            for (var f : classes.get(owner).getFields()) for (var v : f.getVariables())
                head.append(v.getInitializer().map(this::expr).orElseGet(() -> zero(v.getTypeAsString(), v))).append('\n');
            head.append("struct.new $").append(owner).append("\nlocal.set $this\n");
        }
        head.append(statement(body));
        if (!method) head.append("local.get $this\n");
        return head.append(" )\n").toString();
    }

    private String statement(Statement s) {
        if (s.isBlockStmt()) {
            StringBuilder b = new StringBuilder();
            for (var child : s.asBlockStmt().getStatements()) b.append(statement(child));
            return b.toString();
        }
        if (s.isReturnStmt()) return s.asReturnStmt().getExpression().map(e -> expr(e) + "\n").orElse("") + "return\n";
        if (s.isEmptyStmt()) return "";
        if (s.isExpressionStmt()) {
            var e = s.asExpressionStmt().getExpression();
            if (e.isVariableDeclarationExpr()) {
                StringBuilder b = new StringBuilder();
                for (var v : e.asVariableDeclarationExpr().getVariables())
                    v.getInitializer().ifPresent(i -> b.append(expr(i)).append("\nlocal.set $").append(v.getNameAsString()).append('\n'));
                return b.toString();
            }
            if (e.isAssignExpr() && e.asAssignExpr().getOperator() == AssignExpr.Operator.ASSIGN) {
                var a = e.asAssignExpr();
                if (a.getTarget().isNameExpr() && locals.containsKey(a.getTarget().toString()))
                    return expr(a.getValue()) + "\nlocal.set $" + a.getTarget() + "\n";
                var target = field(a.getTarget());
                return expr(target.getScope()) + "\n" + expr(a.getValue()) + "\nstruct.set $" + expressionType(target.getScope()) + " $" + target.getNameAsString() + "\n";
            }
        }
        throw unsupported(s);
    }

    private FieldAccessExpr field(Expression e) {
        if (e.isFieldAccessExpr()) return e.asFieldAccessExpr();
        if (e.isNameExpr()) return new FieldAccessExpr(new ThisExpr(), e.asNameExpr().getNameAsString());
        throw unsupported(e);
    }

    private String expr(Expression e) {
        if (e.isEnclosedExpr()) return expr(e.asEnclosedExpr().getInner());
        if (e.isIntegerLiteralExpr()) return "i32.const " + e.asIntegerLiteralExpr().asNumber().intValue();
        if (e.isBooleanLiteralExpr()) return "i32.const " + (e.asBooleanLiteralExpr().getValue() ? 1 : 0);
        if (e.isThisExpr()) {
            if (!locals.containsKey("this")) throw unsupported(e);
            return "local.get $this";
        }
        if (e.isNameExpr() && locals.containsKey(e.toString())) return "local.get $" + e;
        if (e.isObjectCreationExpr()) {
            var n = e.asObjectCreationExpr();
            if (n.getAnonymousClassBody().isPresent() || !classes.containsKey(n.getTypeAsString())) throw unsupported(e);
            StringBuilder b = new StringBuilder();
            n.getArguments().forEach(a -> b.append(expr(a)).append('\n'));
            return b.append("call $").append(n.getTypeAsString()).append("_new").toString();
        }
        if (e.isNameExpr() || e.isFieldAccessExpr()) {
            var f = field(e);
            expressionType(f); // validate field before emitting
            return expr(f.getScope()) + "\nstruct.get $" + expressionType(f.getScope()) + " $" + f.getNameAsString();
        }
        if (e.isBinaryExpr()) {
            var b = e.asBinaryExpr();
            if (!expressionType(b.getLeft()).equals("int") || !expressionType(b.getRight()).equals("int")) throw unsupported(e);
            String op = switch (b.getOperator()) {
                case PLUS -> "add"; case MINUS -> "sub"; case MULTIPLY -> "mul";
                default -> throw unsupported(e);
            };
            return expr(b.getLeft()) + "\n" + expr(b.getRight()) + "\ni32." + op;
        }
        throw unsupported(e);
    }

    private String expressionType(Expression e) {
        if (e.isEnclosedExpr()) return expressionType(e.asEnclosedExpr().getInner());
        if (e.isThisExpr()) return owner;
        if (e.isIntegerLiteralExpr() || e.isBinaryExpr()) return "int";
        if (e.isNameExpr() && locals.containsKey(e.toString())) return locals.get(e.toString());
        var f = field(e);
        var c = classes.get(expressionType(f.getScope()));
        if (c == null) throw unsupported(e);
        for (var declaration : c.getFields()) for (var v : declaration.getVariables())
            if (v.getNameAsString().equals(f.getNameAsString())) return v.getTypeAsString();
        throw unsupported(e);
    }

    private String type(String t, Node n) {
        return switch (t) {
            case "int", "boolean", "byte", "short", "char" -> "i32";
            case "long" -> "i64"; case "float" -> "f32"; case "double" -> "f64";
            default -> {
                if (!classes.containsKey(t)) throw unsupported(n);
                yield "(ref null $" + t + ")";
            }
        };
    }

    private String zero(String t, Node n) {
        String storage = type(t, n);
        return storage.startsWith("(ref") ? "ref.null $" + t : storage + ".const 0";
    }

    private IllegalArgumentException unsupported(Node n) {
        return new IllegalArgumentException("WasmGC: unsupported " + n.getClass().getSimpleName()
                + n.getBegin().map(p -> " at " + p.line + ":" + p.column).orElse("")
                + "; use the default linear-memory backend or simplify to the documented GC subset.");
    }
}
