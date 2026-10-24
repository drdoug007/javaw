package com.example.javaw.bytecode;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;
import static com.example.javaw.bytecode.BytecodeDynamic.*;

/** Runtime-visible class-file annotations become typed immutable AOT values. */
final class BytecodeAnnotations implements Opcodes {
    private final Map<String,ClassNode> linked=new LinkedHashMap<>();
    private final List<ClassNode> generated=new ArrayList<>();
    static void normalize(List<ClassNode> classes){new BytecodeAnnotations().run(classes);}
    private void run(List<ClassNode> classes){
        for(ClassNode type:classes)linked.put(type.name,type);
        ClassNode support=linked.get("javaw/runtime/ReflectionSupport");
        MethodNode annotations=support.methods.stream().filter(m->m.name.equals("annotations")).findFirst().orElseThrow();annotations.instructions.clear();
        MethodNode defaults=support.methods.stream().filter(m->m.name.equals("defaultValue")).findFirst().orElseThrow();defaults.instructions.clear();
        MethodNode parameterNames=support.methods.stream().filter(m->m.name.equals("parameterNames")).findFirst().orElseThrow();parameterNames.instructions.clear();
        MethodNode parameterModifiers=support.methods.stream().filter(m->m.name.equals("parameterModifiers")).findFirst().orElseThrow();parameterModifiers.instructions.clear();
        MethodNode signatures=support.methods.stream().filter(m->m.name.equals("signature")).findFirst().orElseThrow();signatures.instructions.clear();
        for(ClassNode owner:List.copyOf(classes)) {
            if(owner.name.startsWith("java/") && !owner.name.startsWith("java/lang/annotation/") && (owner.access&ACC_ANNOTATION)==0 || owner.name.startsWith("javaw/runtime/"))continue;
            signature(signatures,owner,"",owner.signature);
            table(annotations,owner,"",owner.visibleAnnotations);typeAnnotations(annotations,owner,"",owner.visibleTypeAnnotations);
            for(FieldNode field:owner.fields){table(annotations,owner,"F"+field.name+field.desc,field.visibleAnnotations);typeAnnotations(annotations,owner,"F"+field.name+field.desc,field.visibleTypeAnnotations);signature(signatures,owner,"F"+field.name+field.desc,field.signature);}
            for(MethodNode method:owner.methods){
                String key=(method.name.equals("<init>")?"C":"M"+method.name)+method.desc;
                signature(signatures,owner,key,method.signature);
                table(annotations,owner,key,method.visibleAnnotations);typeAnnotations(annotations,owner,key,method.visibleTypeAnnotations);
                if(method.parameters!=null)for(MethodNode table:List.of(parameterNames,parameterModifiers)){
                    Label next=match(table,owner.name,key);boolean names=table==parameterNames;table.visitLdcInsn(method.parameters.size());
                    if(names)table.visitTypeInsn(ANEWARRAY,"java/lang/String");else table.visitIntInsn(NEWARRAY,T_INT);
                    for(int i=0;i<method.parameters.size();i++){ParameterNode parameter=method.parameters.get(i);table.visitInsn(DUP);table.visitLdcInsn(i);if(names){if(parameter.name==null)table.visitInsn(ACONST_NULL);else table.visitLdcInsn(parameter.name);}else table.visitLdcInsn(parameter.access);table.visitInsn(names?AASTORE:IASTORE);}
                    table.visitInsn(ARETURN);table.visitLabel(next);table.visitFrame(F_SAME,0,null,0,null);
                }
                if(method.visibleParameterAnnotations!=null){
                    int offset=Type.getArgumentTypes(method.desc).length-method.visibleAnnotableParameterCount;
                    for(int i=0;i<method.visibleAnnotableParameterCount;i++)table(annotations,owner,"P"+key+"#"+(i+offset),method.visibleParameterAnnotations[i]);
                }
                if(method.annotationDefault!=null){
                    Label next=match(defaults,owner.name,method.name+method.desc);
                    ClassNode factory=factory(owner,method.annotationDefault,Type.getReturnType(method.desc));
                    call(defaults,INVOKESTATIC,factory.name,"value","()Ljava/lang/Object;");defaults.visitInsn(ARETURN);defaults.visitLabel(next);defaults.visitFrame(F_SAME,0,null,0,null);
                }
            }
        }
        for(MethodNode table:List.of(parameterNames,parameterModifiers)){table.visitInsn(ACONST_NULL);table.visitInsn(ARETURN);finish(table,2);}
        annotations.visitInsn(ICONST_0);annotations.visitTypeInsn(ANEWARRAY,"java/lang/annotation/Annotation");annotations.visitInsn(ARETURN);finish(annotations,2);
        signatures.visitInsn(ACONST_NULL);signatures.visitInsn(ARETURN);finish(signatures,2);
        defaults.visitInsn(ACONST_NULL);defaults.visitInsn(ARETURN);finish(defaults,2);classes.addAll(generated);
    }
    private void typeAnnotations(MethodNode code,ClassNode owner,String key,List<TypeAnnotationNode> annotations){
        if(annotations==null)return;Map<String,List<AnnotationNode>> groups=new LinkedHashMap<>();
        for(TypeAnnotationNode annotation:annotations){String path=annotation.typePath==null?"":annotation.typePath.toString();groups.computeIfAbsent("Y"+key+"#"+annotation.typeRef+"#"+path,k->new ArrayList<>()).add(annotation);}
        for(var group:groups.entrySet())table(code,owner,group.getKey(),group.getValue());
    }
    private void signature(MethodNode code,ClassNode owner,String key,String signature){if(signature==null)return;Label next=match(code,owner.name,key);code.visitLdcInsn(signature);code.visitInsn(ARETURN);code.visitLabel(next);code.visitFrame(F_SAME,0,null,0,null);}
    private Label match(MethodNode code,String owner,String key){
        Label next=new Label();code.visitVarInsn(ALOAD,0);call(code,INVOKEVIRTUAL,"java/lang/Class","getName","()Ljava/lang/String;");code.visitLdcInsn(owner.replace('/','.'));call(code,INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");code.visitJumpInsn(IFEQ,next);
        code.visitVarInsn(ALOAD,1);code.visitLdcInsn(key);call(code,INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");code.visitJumpInsn(IFEQ,next);return next;
    }
    private void table(MethodNode code,ClassNode owner,String key,List<AnnotationNode> entries){
        if(entries==null)return;
        entries=entries.stream().filter(a->linked.containsKey(Type.getType(a.desc).getInternalName())).toList();
        if(entries.isEmpty())return;Label next=match(code,owner.name,key);
        Set<String> annotationTypes=new HashSet<>();boolean duplicate=false;for(AnnotationNode entry:entries)if(!annotationTypes.add(entry.desc))duplicate=true;
        if(duplicate){code.visitTypeInsn(NEW,"java/lang/annotation/AnnotationFormatError");code.visitInsn(DUP);code.visitLdcInsn("duplicate runtime annotation type");call(code,INVOKESPECIAL,"java/lang/annotation/AnnotationFormatError","<init>","(Ljava/lang/String;)V");code.visitInsn(ATHROW);code.visitLabel(next);code.visitFrame(F_SAME,0,null,0,null);return;}
        code.visitLdcInsn(entries.size());code.visitTypeInsn(ANEWARRAY,"java/lang/annotation/Annotation");
        for(int i=0;i<entries.size();i++){code.visitInsn(DUP);code.visitLdcInsn(i);ClassNode factory=factory(owner,entries.get(i),Type.getType(entries.get(i).desc));call(code,INVOKESTATIC,factory.name,"value","()Ljava/lang/Object;");code.visitTypeInsn(CHECKCAST,"java/lang/annotation/Annotation");code.visitInsn(AASTORE);}
        code.visitInsn(ARETURN);code.visitLabel(next);code.visitFrame(F_SAME,0,null,0,null);
    }
    private ClassNode factory(ClassNode owner,Object value,Type type){
        String name=owner.name+"$$javaw$annotationFactory$"+generated.size();if(linked.containsKey(name))throw new IllegalArgumentException("reserved annotation factory collision");
        ClassNode factory=type(name);generated.add(factory);factory.sourceFile=owner.sourceFile;
        ClassNode host=owner.nestHostClass==null?owner:linked.get(owner.nestHostClass);if(host!=null)nest(factory,owner,linked);
        MethodNode method=method(factory,ACC_PUBLIC|ACC_STATIC,"value","()Ljava/lang/Object;");
        if(!(value instanceof AnnotationNode) && invalid(type,value)!=null){method.visitTypeInsn(NEW,"java/lang/annotation/AnnotationFormatError");method.visitInsn(DUP);method.visitLdcInsn("invalid annotation default");call(method,INVOKESPECIAL,"java/lang/annotation/AnnotationFormatError","<init>","(Ljava/lang/String;)V");method.visitInsn(ATHROW);finish(method,0);return factory;}
        emit(method,type,value,owner);adapt(method,type,Type.getType(Object.class));method.visitInsn(ARETURN);finish(method,0);return factory;
    }
    private ClassNode annotation(AnnotationNode value,ClassNode lexicalOwner){
        ClassNode declaration=linked.get(Type.getType(value.desc).getInternalName());
        if(declaration==null || (declaration.access&ACC_ANNOTATION)==0)throw new IllegalArgumentException("link runtime annotation declaration "+value.desc);
        String name=lexicalOwner.name+"$$javaw$annotation$"+generated.size();if(linked.containsKey(name))throw new IllegalArgumentException("reserved annotation value collision");
        ClassNode result=type(name);generated.add(result);result.superName="javaw/runtime/AnnotationValue";result.interfaces.add(declaration.name);result.sourceFile=declaration.sourceFile;
        ClassNode host=lexicalOwner.nestHostClass==null?lexicalOwner:linked.get(lexicalOwner.nestHostClass);if(host!=null)nest(result,lexicalOwner,linked);
        Map<String,Object> values=new LinkedHashMap<>();if(value.values!=null)for(int i=0;i<value.values.size();i+=2)values.put((String)value.values.get(i),value.values.get(i+1));
        List<MethodNode> members=declaration.methods.stream().filter(m->!m.name.startsWith("<") && (m.access&ACC_STATIC)==0).toList();
        MethodNode init=method(result,ACC_PUBLIC,"<init>","()V");init.visitVarInsn(ALOAD,0);call(init,INVOKESPECIAL,result.superName,"<init>","()V");
        for(MethodNode member:members){
            Type type=Type.getReturnType(member.desc);Object memberValue=values.containsKey(member.name)?values.get(member.name):member.annotationDefault;
            MethodNode getter=method(result,ACC_PUBLIC|ACC_FINAL,member.name,member.desc);
            if(memberValue==null){getter.visitTypeInsn(NEW,"java/lang/annotation/IncompleteAnnotationException");getter.visitInsn(DUP);getter.visitLdcInsn(Type.getObjectType(declaration.name));getter.visitLdcInsn(member.name);call(getter,INVOKESPECIAL,"java/lang/annotation/IncompleteAnnotationException","<init>","(Ljava/lang/Class;Ljava/lang/String;)V");getter.visitInsn(ATHROW);finish(getter,1);continue;}
            String failure=invalid(type,memberValue);
            if(failure!=null){
                if(failure.equals("enum")){
                    String[] enumValue=(String[])memberValue;getter.visitTypeInsn(NEW,"java/lang/EnumConstantNotPresentException");getter.visitInsn(DUP);getter.visitLdcInsn(Type.getType(enumValue[0]));getter.visitLdcInsn(enumValue[1]);call(getter,INVOKESPECIAL,"java/lang/EnumConstantNotPresentException","<init>","(Ljava/lang/Class;Ljava/lang/String;)V");
                }else if(failure.equals("class")){
                    getter.visitTypeInsn(NEW,"java/lang/TypeNotPresentException");getter.visitInsn(DUP);getter.visitLdcInsn(((Type)memberValue).getClassName());getter.visitInsn(ACONST_NULL);call(getter,INVOKESPECIAL,"java/lang/TypeNotPresentException","<init>","(Ljava/lang/String;Ljava/lang/Throwable;)V");
                }else {
                    getter.visitTypeInsn(NEW,"java/lang/annotation/AnnotationTypeMismatchException");getter.visitInsn(DUP);getter.visitLdcInsn(Type.getObjectType(declaration.name));getter.visitLdcInsn(member.name);getter.visitInsn(ICONST_0);getter.visitTypeInsn(ANEWARRAY,"java/lang/Class");call(getter,INVOKEVIRTUAL,"java/lang/Class","getDeclaredMethod","(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");getter.visitLdcInsn(String.valueOf(memberValue));call(getter,INVOKESPECIAL,"java/lang/annotation/AnnotationTypeMismatchException","<init>","(Ljava/lang/reflect/Method;Ljava/lang/String;)V");
                }
                getter.visitInsn(ATHROW);finish(getter,1);continue;
            }
            result.fields.add(new FieldNode(ACC_PRIVATE|ACC_FINAL,member.name,type.getDescriptor(),null,null));
            init.visitVarInsn(ALOAD,0);emit(init,type,memberValue,lexicalOwner);init.visitFieldInsn(PUTFIELD,name,member.name,type.getDescriptor());
            getter.visitVarInsn(ALOAD,0);getter.visitFieldInsn(GETFIELD,name,member.name,type.getDescriptor());
            if(type.getSort()==Type.ARRAY){call(getter,INVOKESTATIC,"javaw/runtime/AnnotationValue","copy","(Ljava/lang/Object;)Ljava/lang/Object;");getter.visitTypeInsn(CHECKCAST,type.getDescriptor());}
            getter.visitInsn(type.getOpcode(IRETURN));finish(getter,1);
        }
        init.visitInsn(RETURN);finish(init,1);
        MethodNode annotationType=method(result,ACC_PUBLIC,"annotationType","()Ljava/lang/Class;");annotationType.visitLdcInsn(Type.getObjectType(declaration.name));annotationType.visitInsn(ARETURN);finish(annotationType,1);
        MethodNode names=method(result,ACC_PROTECTED,"names","()[Ljava/lang/String;");names.visitLdcInsn(members.size());names.visitTypeInsn(ANEWARRAY,"java/lang/String");
        MethodNode memberValues=method(result,ACC_PROTECTED,"values","()[Ljava/lang/Object;");memberValues.visitLdcInsn(members.size());memberValues.visitTypeInsn(ANEWARRAY,"java/lang/Object");
        for(int i=0;i<members.size();i++){MethodNode member=members.get(i);names.visitInsn(DUP);names.visitLdcInsn(i);names.visitLdcInsn(member.name);names.visitInsn(AASTORE);
            memberValues.visitInsn(DUP);memberValues.visitLdcInsn(i);memberValues.visitVarInsn(ALOAD,0);call(memberValues,INVOKEVIRTUAL,name,member.name,member.desc);adapt(memberValues,Type.getReturnType(member.desc),Type.getType(Object.class));memberValues.visitInsn(AASTORE);}
        names.visitInsn(ARETURN);finish(names,1);memberValues.visitInsn(ARETURN);finish(memberValues,1);return result;
    }
    private String invalid(Type type,Object value){
        if(type.getSort()==Type.ARRAY){if(!(value instanceof List<?> values))return "mismatch";Type component=Type.getType(type.getDescriptor().substring(1));for(Object entry:values)if(invalid(component,entry)!=null)return "mismatch";return null;}
        if(value instanceof Type literal){if(!type.getDescriptor().equals("Ljava/lang/Class;"))return "mismatch";Type base=literal;while(base.getSort()==Type.ARRAY)base=Type.getType(base.getDescriptor().substring(1));return base.getSort()==Type.OBJECT && !base.getInternalName().equals("java/lang/Object") && !linked.containsKey(base.getInternalName())?"class":null;}
        if(value instanceof AnnotationNode annotation)return annotation.desc.equals(type.getDescriptor())?null:"mismatch";
        if(value instanceof String[] enumeration){if(!enumeration[0].equals(type.getDescriptor()))return "mismatch";ClassNode declaration=linked.get(type.getInternalName());return declaration==null || declaration.fields.stream().noneMatch(f->f.name.equals(enumeration[1]) && f.desc.equals(enumeration[0]))?"enum":null;}
        return switch(type.getSort()){case Type.BOOLEAN->value instanceof Boolean?null:"mismatch";case Type.BYTE->value instanceof Byte?null:"mismatch";case Type.CHAR->value instanceof Character?null:"mismatch";case Type.SHORT->value instanceof Short?null:"mismatch";case Type.INT->value instanceof Integer?null:"mismatch";case Type.LONG->value instanceof Long?null:"mismatch";case Type.FLOAT->value instanceof Float?null:"mismatch";case Type.DOUBLE->value instanceof Double?null:"mismatch";case Type.OBJECT->type.getDescriptor().equals("Ljava/lang/String;") && value instanceof String?null:"mismatch";default->"mismatch";};
    }
    private void emit(MethodNode code,Type type,Object value,ClassNode owner){
        if(value instanceof AnnotationNode annotation){ClassNode implementation=annotation(annotation,owner);code.visitTypeInsn(NEW,implementation.name);code.visitInsn(DUP);call(code,INVOKESPECIAL,implementation.name,"<init>","()V");}
        else if(value instanceof Type literal)BytecodeReflection.classLiteral(code,literal);
        else if(value instanceof String[] enumeration)code.visitFieldInsn(GETSTATIC,Type.getType(enumeration[0]).getInternalName(),enumeration[1],enumeration[0]);
        else if(value instanceof List<?> entries){
            Type component=Type.getType(type.getDescriptor().substring(1));code.visitLdcInsn(entries.size());
            if(component.getSort()==Type.OBJECT || component.getSort()==Type.ARRAY)code.visitTypeInsn(ANEWARRAY,component.getInternalName());
            else code.visitIntInsn(NEWARRAY,switch(component.getSort()){case Type.BOOLEAN->T_BOOLEAN;case Type.BYTE->T_BYTE;case Type.CHAR->T_CHAR;case Type.SHORT->T_SHORT;case Type.INT->T_INT;case Type.LONG->T_LONG;case Type.FLOAT->T_FLOAT;default->T_DOUBLE;});
            for(int i=0;i<entries.size();i++){code.visitInsn(DUP);code.visitLdcInsn(i);emit(code,component,entries.get(i),owner);code.visitInsn(component.getOpcode(IASTORE));}
        }else if(value instanceof Boolean bool)code.visitInsn(bool?ICONST_1:ICONST_0);
        else if(value instanceof Character character)code.visitLdcInsn((int)character);
        else if(value instanceof Byte || value instanceof Short)code.visitLdcInsn(((Number)value).intValue());
        else code.visitLdcInsn(value);
    }
}
