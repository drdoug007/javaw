package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import java.util.function.BiPredicate;

/** Object representation for records that cross the reflective Object boundary. */
final class RecordReflectionSupport {
    static ClassOrInterfaceDeclaration lower(RecordDeclaration record,BiPredicate<AnnotationExpr,String> applicable) {
        String name=record.getNameAsString();
        var owner=new ClassOrInterfaceDeclaration().setName(name).setFinal(true);
        record.getModifiers().forEach(m -> owner.getModifiers().add(m.clone()));
        owner.setFinal(true);
        record.getTypeParameters().forEach(p -> owner.getTypeParameters().add(p.clone()));
        record.getImplementedTypes().forEach(t -> owner.getImplementedTypes().add(t.clone()));
        record.getAnnotations().forEach(a -> owner.getAnnotations().add(a.clone()));
        if(record.containsData(JavaSourcePreparation.PACKAGE))
            owner.setData(JavaSourcePreparation.PACKAGE,record.getData(JavaSourcePreparation.PACKAGE));
        boolean canonical=false;
        for(var member:record.getMembers()) {
            if(member instanceof CompactConstructorDeclaration compact) {
                var constructor=new ConstructorDeclaration().setName(name);
                compact.getModifiers().forEach(m -> constructor.getModifiers().add(m.clone()));
                compact.getAnnotations().forEach(a -> constructor.getAnnotations().add(a.clone()));
                record.getParameters().forEach(p -> constructor.getParameters().add(parameter(p,applicable)));
                constructor.setBody(compact.getBody().clone());
                assignments(record,constructor.getBody());
                owner.addMember(constructor); canonical=true;
            } else {
                owner.addMember(member.clone());
                if(member instanceof ConstructorDeclaration constructor &&
                        constructor.getParameters().stream().map(RecordReflectionSupport::componentType).toList()
                        .equals(record.getParameters().stream().map(RecordReflectionSupport::componentType).toList()))
                    canonical=true;
            }
        }
        if(!canonical) {
            var constructor=new ConstructorDeclaration().setName(name).setPublic(record.isPublic());
            record.getParameters().forEach(p -> constructor.getParameters().add(parameter(p,applicable)));
            constructor.setBody(new BlockStmt()); assignments(record,constructor.getBody());
            owner.addMember(constructor);
        }
        for(var component:record.getParameters()) {
            String type=componentType(component), field=component.getNameAsString();
            var backing=StaticJavaParser.parseBodyDeclaration("private final "+type+" "+field+";").asFieldDeclaration();
            component.getAnnotations().stream().filter(a -> applicable.test(a,"FIELD")).forEach(a -> backing.getAnnotations().add(a.clone()));
            owner.addMember(backing);
            if(owner.getMethodsByName(field).stream().noneMatch(m -> m.getParameters().isEmpty())) {
                var accessor=StaticJavaParser.parseBodyDeclaration("public "+type+" "+field+"() { return this."+field+"; }").asMethodDeclaration();
                component.getAnnotations().stream().filter(a -> applicable.test(a,"METHOD")).forEach(a -> accessor.getAnnotations().add(a.clone()));
                owner.addMember(accessor);
            }
        }
        if(owner.getMethodsByName("equals").stream().noneMatch(m -> m.getParameters().size()==1 && m.getParameter(0).getTypeAsString().equals("Object"))) {
            StringBuilder equals=new StringBuilder("public final boolean equals(Object other) { if(this==other) return true; if(!(other instanceof "+name+")) return false; "+name+" that=("+name+")other; return true");
            for(var component:record.getParameters()) {
                String field=component.getNameAsString(),type=componentType(component),a="this."+field,b="that."+field;
                String test=type.endsWith("[]")?a+"=="+b:BoxingSupport.wrapper(type)!=null?
                        ReflectionSupport.annotationEqual(a,b,type):"("+a+"=="+b+" || ("+a+"!=null && "+a+".equals("+b+")))";
                equals.append(" && (").append(test).append(")");
            }
            owner.addMember(StaticJavaParser.parseBodyDeclaration(equals+"; }"));
        }
        if(owner.getMethodsByName("hashCode").stream().noneMatch(m -> m.getParameters().isEmpty())) {
            StringBuilder hash=new StringBuilder("public final int hashCode() { int result=0;");
            for(var component:record.getParameters()) {
                String field="this."+component.getNameAsString(),type=componentType(component);
                String value=type.endsWith("[]")?"System.identityHashCode("+field+")":BoxingSupport.wrapper(type)!=null?
                        BoxingSupport.wrapper(type)+".valueOf("+field+").hashCode()":"("+field+"==null?0:"+field+".hashCode())";
                hash.append("result=31*result+").append(value).append(";");
            }
            owner.addMember(StaticJavaParser.parseBodyDeclaration(hash+"return result; }"));
        }
        if(owner.getMethodsByName("toString").stream().noneMatch(m -> m.getParameters().isEmpty())) {
            StringBuilder text=new StringBuilder("public final String toString() { return ").append(new StringLiteralExpr(name+"["));
            for(int i=0;i<record.getParameters().size();i++) {
                String field=record.getParameter(i).getNameAsString();
                text.append("+").append(new StringLiteralExpr((i==0?"":", ")+field+"=")).append("+this.").append(field);
            }
            owner.addMember(StaticJavaParser.parseBodyDeclaration(text+"+\"]\"; }"));
        }
        return owner;
    }
    private static Parameter parameter(Parameter source,BiPredicate<AnnotationExpr,String> applicable) {
        var parameter=source.clone();
        parameter.getAnnotations().removeIf(a -> !applicable.test(a,"PARAMETER"));
        return parameter;
    }
    private static void assignments(RecordDeclaration record,BlockStmt body) {
        for(var component:record.getParameters()) {
            String name=component.getNameAsString();
            body.addStatement(StaticJavaParser.parseStatement("this."+name+"="+name+";"));
        }
    }
    static String componentType(Parameter parameter) {
        return parameter.getTypeAsString()+(parameter.isVarArgs()?"[]":"");
    }
}
