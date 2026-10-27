package one.dastec.javaw;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import java.util.*;

/** Java int constant expressions used by conditional numeric narrowing. */
final class IntegralConstants {
    private IntegralConstants() {}

    static OptionalInt evaluate(Expression expression) {
        try { return OptionalInt.of(value(expression, Collections.newSetFromMap(new IdentityHashMap<>()))); }
        catch (IllegalArgumentException | ArithmeticException error) { return OptionalInt.empty(); }
    }

    private static int value(Expression expression, Set<Node> evaluating) {
        if (!evaluating.add(expression)) throw new IllegalArgumentException("Cyclic constant");
        try {
            if (expression.isEnclosedExpr()) return value(expression.asEnclosedExpr().getInner(), evaluating);
            if (expression.isIntegerLiteralExpr()) return expression.asIntegerLiteralExpr().asNumber().intValue();
            if (expression.isCharLiteralExpr()) return expression.asCharLiteralExpr().asChar();
            if (expression.isCastExpr()) {
                CastExpr cast = expression.asCastExpr();
                int operand = value(cast.getExpression(), evaluating);
                return switch (cast.getType().asString()) {
                    case "byte" -> (byte) operand;
                    case "short" -> (short) operand;
                    case "char" -> (char) operand;
                    case "int" -> operand;
                    default -> throw new IllegalArgumentException("Not an int constant");
                };
            }
            if (expression.isUnaryExpr()) {
                UnaryExpr unary = expression.asUnaryExpr();
                int operand = value(unary.getExpression(), evaluating);
                return switch (unary.getOperator()) {
                    case MINUS -> -operand;
                    case PLUS -> operand;
                    case BITWISE_COMPLEMENT -> ~operand;
                    default -> throw new IllegalArgumentException("Not a constant operator");
                };
            }
            if (expression.isBinaryExpr()) {
                BinaryExpr binary = expression.asBinaryExpr();
                int left = value(binary.getLeft(), evaluating), right = value(binary.getRight(), evaluating);
                return switch (binary.getOperator()) {
                    case PLUS -> left + right;
                    case MINUS -> left - right;
                    case MULTIPLY -> left * right;
                    case DIVIDE -> left / right;
                    case REMAINDER -> left % right;
                    case LEFT_SHIFT -> left << right;
                    case SIGNED_RIGHT_SHIFT -> left >> right;
                    case UNSIGNED_RIGHT_SHIFT -> left >>> right;
                    case BINARY_AND -> left & right;
                    case BINARY_OR -> left | right;
                    case XOR -> left ^ right;
                    default -> throw new IllegalArgumentException("Not an integral operator");
                };
            }
            if (expression.isNameExpr() || expression.isFieldAccessExpr()) {
                String name = expression.isNameExpr() ? expression.asNameExpr().getNameAsString() : expression.asFieldAccessExpr().getNameAsString();
                Node root = expression.findCompilationUnit().orElseThrow();
                List<VariableDeclarator> declarations = root.findAll(VariableDeclarator.class).stream().filter(v -> v.getNameAsString().equals(name))
                        .filter(v -> v.getParentNode().map(p -> p instanceof FieldDeclaration f ? f.isFinal()
                                : p instanceof VariableDeclarationExpr d && d.isFinal()).orElse(false))
                        .filter(v -> Set.of("int", "byte", "short", "char").contains(v.getTypeAsString())).toList();
                if (expression.isFieldAccessExpr()) {
                    String owner = expression.asFieldAccessExpr().getScope().toString();
                    declarations = declarations.stream().filter(v -> v.findAncestor(TypeDeclaration.class)
                            .map(t -> t.getNameAsString().equals(owner)).orElse(false)).toList();
                } else {
                    var callable = expression.findAncestor(CallableDeclaration.class);
                    var type = expression.findAncestor(TypeDeclaration.class);
                    declarations = declarations.stream().filter(v -> v.findAncestor(CallableDeclaration.class).equals(callable)
                            || v.findAncestor(CallableDeclaration.class).isEmpty() && v.findAncestor(TypeDeclaration.class).equals(type)).toList();
                }
                if (declarations.size() == 1) return value(declarations.get(0).getInitializer().orElseThrow(), evaluating);
            }
            throw new IllegalArgumentException("Not an int constant expression");
        } finally { evaluating.remove(expression); }
    }
}
