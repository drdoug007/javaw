package one.dastec.javaw;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Arbitrary precision arithmetic is compiled from source, without a host numeric bridge. */
final class DecimalSupport {
    static void addTypes(CompilationUnit cu) {
        boolean needed=cu.findAll(com.github.javaparser.ast.type.ClassOrInterfaceType.class).stream()
                .anyMatch(t->t.getNameAsString().equals("BigDecimal") || t.getNameAsString().equals("BigInteger"))
                || cu.findAll(NameExpr.class).stream().anyMatch(n->n.getNameAsString().equals("BigDecimal") || n.getNameAsString().equals("BigInteger"));
        if(!needed) return;
        // A constant double factory uses the JDK's shortest decimal spelling at compile time.
        for(MethodCallExpr call:cu.findAll(MethodCallExpr.class)) {
            if(!call.getScope().map(Object::toString).orElse("").equals("BigDecimal") || !call.getNameAsString().equals("valueOf") || call.getArguments().size()!=1) continue;
            Expression value=call.getArgument(0);
            if(value.isDoubleLiteralExpr() || value.isUnaryExpr() && value.asUnaryExpr().getExpression().isDoubleLiteralExpr()) {
                String literal=value.toString().replace("_","");
                boolean single=literal.endsWith("f") || literal.endsWith("F");
                literal=literal.replaceAll("[dDfF]$","");
                double number=single ? (double)Float.parseFloat(literal) : Double.parseDouble(literal);
                String text=java.math.BigDecimal.valueOf(number).toString();
                call.replace(new ObjectCreationExpr().setType("BigDecimal").addArgument(new StringLiteralExpr(text)));
            }
        }
        try(var input=DecimalSupport.class.getResourceAsStream("/javaw/decimal.java")) {
            if(input==null) throw new IllegalStateException("Missing decimal library source");
            LibrarySupport.add(cu,"java.math",new String(input.readAllBytes(),StandardCharsets.UTF_8));
            for(String owner:java.util.List.of("BigInteger","BigDecimal")) {
                var model=cu.getClassByName(owner).orElseThrow();
                for(String name:java.util.List.of("ZERO","ONE","TEN")) {
                    String symbol="__javaw_"+owner+"_"+name;
                    model.getFieldByName(name).orElseThrow().getVariable(0).setName(symbol);
                    for(NameExpr use:model.findAll(NameExpr.class)) if(use.getNameAsString().equals(name)) use.setName(symbol);
                    for(FieldAccessExpr use:cu.findAll(FieldAccessExpr.class))
                        if(use.getScope().toString().equals(owner) && use.getNameAsString().equals(name)) use.setName(symbol);
                }
            }
        } catch(IOException error) { throw new IllegalStateException(error); }
    }
}
