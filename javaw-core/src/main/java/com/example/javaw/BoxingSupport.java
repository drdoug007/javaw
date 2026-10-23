package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import java.util.*;

/** Nullable primitive wrappers implemented as ordinary objects in linear memory. */
final class BoxingSupport {
    static final Map<String, String> PRIMITIVES = Map.of(
            "Integer", "int", "Long", "long", "Float", "float", "Double", "double",
            "Byte", "byte", "Short", "short", "Character", "char", "Boolean", "boolean");
    static String primitive(String type) { return type == null ? null : PRIMITIVES.getOrDefault(type, type); }
    static String wrapper(String type) {
        return PRIMITIVES.entrySet().stream().filter(e -> e.getValue().equals(type)).map(Map.Entry::getKey).findFirst().orElse(null);
    }
    static boolean boxed(String type) { return type != null && PRIMITIVES.containsKey(type); }

    static void addWrappers(CompilationUnit cu) {
        if (cu.getTypes().stream().noneMatch(t -> t.getNameAsString().equals("Number"))) {
            cu.getTypes().add(StaticJavaParser.parseTypeDeclaration("""
                abstract class Number {
                    public Number() {}
                    public int intValue() { throw new IllegalStateException(); }
                    public long longValue() { throw new IllegalStateException(); }
                    public float floatValue() { throw new IllegalStateException(); }
                    public double doubleValue() { throw new IllegalStateException(); }
                }
                """));
        }
        for (String wrapper : new TreeSet<>(PRIMITIVES.keySet())) {
            if (cu.getTypes().stream().anyMatch(t -> t.getNameAsString().equals(wrapper))) continue;
            String primitive = PRIMITIVES.get(wrapper);
            boolean logical = primitive.equals("boolean"), character = primitive.equals("char");
            StringBuilder body = new StringBuilder("class " + wrapper + (logical || character ? "" : " extends Number") + " {");
            body.append("public int __boxMarker; public ").append(primitive).append(" __boxedValue;");
            body.append("public ").append(wrapper).append("(").append(primitive).append(" value) { this.__boxedValue = value; }");
            if (!logical && !primitive.equals("float") && !primitive.equals("double")) {
                String low = character ? "0" : "-128", high = character ? "127" : "127";
                body.append("private static ").append(wrapper).append("[] __cache_").append(wrapper).append(";");
                body.append("public static ").append(wrapper).append(" valueOf(").append(primitive).append(" value) {")
                    .append("if(value >= ").append(low).append(" && value <= ").append(high).append(") {")
                    .append("if (__cache_").append(wrapper).append(" == null) __cache_").append(wrapper).append(" = new ").append(wrapper).append("[256];")
                    .append("int index = (int)value + ").append(character ? "0" : "128").append(";")
                    .append(wrapper).append(" result = __cache_").append(wrapper).append("[index];")
                    .append("if (result == null) { result = new ").append(wrapper).append("(value); __cache_").append(wrapper).append("[index] = result; }")
                    .append("return result; } return new ").append(wrapper).append("(value); }");
            } else if (logical) {
                body.append("private static Boolean __trueBoolean; private static Boolean __falseBoolean;")
                    .append("public static Boolean valueOf(boolean value) { if (value) { if (__trueBoolean == null) __trueBoolean = new Boolean(true); return __trueBoolean; }")
                    .append("if (__falseBoolean == null) __falseBoolean = new Boolean(false); return __falseBoolean; }");
            } else body.append("public static ").append(wrapper).append(" valueOf(").append(primitive).append(" value) { return new ").append(wrapper).append("(value); }");
            if (!character) {
                String parser = "parse" + (wrapper.equals("Integer") ? "Int" : wrapper);
                body.append("public static ").append(wrapper).append(" valueOf(String text) { return valueOf((")
                        .append(primitive).append(")").append(wrapper).append('.').append(parser).append("(text)); }");
                if (Set.of("Integer", "Byte", "Short").contains(wrapper))
                    body.append("public static ").append(wrapper).append(" valueOf(String text, int radix) { return valueOf((")
                            .append(primitive).append(")").append(wrapper).append('.').append(parser).append("(text, radix)); }");
            }
            if (logical || character) body.append("public ").append(primitive).append(' ').append(logical ? "booleanValue" : "charValue").append("() { return __boxedValue; }");
            else for (String type : List.of("byte", "short", "int", "long", "float", "double"))
                body.append("public ").append(type).append(' ').append(type).append("Value() { return (").append(type).append(")__boxedValue; }");
            body.append("public boolean equals(Object other) { if (!(other instanceof ").append(wrapper)
                    .append(")) return false; ").append(wrapper).append(" that = (").append(wrapper).append(")other; ");
            if (primitive.equals("float") || primitive.equals("double")) {
                String bits = primitive.equals("float") ? "Float.floatToIntBits" : "Double.doubleToLongBits";
                body.append("return (").append("__boxedValue != __boxedValue && that.__boxedValue != that.__boxedValue) || ")
                    .append(bits).append("(__boxedValue) == ").append(bits).append("(that.__boxedValue); }");
            } else body.append("return __boxedValue == that.__boxedValue; }");
            if (logical) body.append("public int hashCode() { return __boxedValue ? 1231 : 1237; }");
            else if (primitive.equals("long")) body.append("public int hashCode() { return (int)(__boxedValue ^ (__boxedValue >>> 32)); }");
            else if (primitive.equals("double")) body.append("public int hashCode() { long bits = Double.doubleToLongBits(__boxedValue); return (int)(bits ^ (bits >>> 32)); }");
            else if (primitive.equals("float")) body.append("public int hashCode() { return Float.floatToIntBits(__boxedValue); }");
            else body.append("public int hashCode() { return (int)__boxedValue; }");
            body.append("}");
            var type = StaticJavaParser.parseTypeDeclaration(body.toString());
            type.setData(JavaSourcePreparation.PACKAGE, "java.lang");
            cu.getTypes().add(type);
        }
        for (ClassOrInterfaceDeclaration owner : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            List<ExpressionStmt> statics = new ArrayList<>(), instances = new ArrayList<>();
            for (FieldDeclaration field : owner.getFields()) for (VariableDeclarator variable : field.getVariables()) {
                if (!boxed(variable.getTypeAsString()) || variable.getInitializer().isEmpty()) continue;
                Expression target = field.isStatic() ? new NameExpr(variable.getNameAsString()) : new FieldAccessExpr(new ThisExpr(), variable.getNameAsString());
                ExpressionStmt assignment = new ExpressionStmt(new AssignExpr(target, variable.getInitializer().get().clone(), AssignExpr.Operator.ASSIGN));
                (field.isStatic() ? statics : instances).add(assignment);
                variable.removeInitializer();
            }
            if (!statics.isEmpty()) {
                MethodDeclaration initializer = new MethodDeclaration().setName("__initialize_wrappers_" + owner.getNameAsString()).setType("void").setStatic(true).setPublic(true);
                BlockStmt body = new BlockStmt(); statics.forEach(body::addStatement); initializer.setBody(body); owner.addMember(initializer);
            }
            if (!instances.isEmpty()) {
                if (owner.getConstructors().isEmpty()) owner.addConstructor().setBody(new BlockStmt());
                for (ConstructorDeclaration constructor : owner.getConstructors()) {
                    var statements = constructor.getBody().getStatements();
                    boolean explicit = !statements.isEmpty() && statements.get(0).isExplicitConstructorInvocationStmt();
                    if (explicit && statements.get(0).asExplicitConstructorInvocationStmt().isThis()) continue;
                    int position = explicit ? 1 : 0;
                    for (ExpressionStmt assignment : instances) statements.add(position++, assignment.clone());
                }
            }
        }
    }
}
