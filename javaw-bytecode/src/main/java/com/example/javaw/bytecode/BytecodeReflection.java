package com.example.javaw.bytecode;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;
import static com.example.javaw.bytecode.BytecodeDynamic.*;

/** Original application declarations only: generated compiler helpers are never reflected. */
final class BytecodeReflection implements Opcodes {
    private static boolean discovery(String owner,String name) {
        return name.equals("toArray") || owner.equals("java/lang/reflect/Array") || (owner.equals("java/lang/Class") || owner.startsWith("java/lang/reflect/")) && (name.equals("getDeclaredMethods") || name.equals("getDeclaredMethod") || name.equals("getMethods") || name.equals("getMethod") || name.equals("getDeclaredConstructors") || name.equals("getDeclaredConstructor") || name.equals("getConstructors") || name.equals("getConstructor") || name.equals("getDeclaredFields") || name.equals("getDeclaredField") || name.equals("getFields") || name.equals("getField") || name.contains("Annotation") || name.equals("getEnumConstants") || name.equals("newInstance") || name.equals("getDefaultValue") || name.equals("getParameters") || name.startsWith("getAnnotated") || name.startsWith("getGeneric") || name.equals("getTypeParameters") || name.equals("getDeclaringClass") || name.equals("getEnclosingClass") || name.equals("getEnclosingMethod") || name.equals("getEnclosingConstructor") || name.equals("getDeclaredClasses") || name.equals("getClasses") || name.equals("isMemberClass") || name.equals("isLocalClass") || name.equals("isAnonymousClass"));
    }
    private static boolean discoveryConstant(Object value) {
        if(value instanceof Handle handle)return discovery(handle.getOwner(),handle.getName());
        if(value instanceof ConstantDynamic dynamic)for(int i=0;i<dynamic.getBootstrapMethodArgumentCount();i++)if(discoveryConstant(dynamic.getBootstrapMethodArgument(i)))return true;
        return false;
    }
    private static boolean discoveryUse(AbstractInsnNode instruction) {
        if(instruction instanceof MethodInsnNode call)return discovery(call.owner,call.name);
        if(instruction instanceof LdcInsnNode ldc)return discoveryConstant(ldc.cst);
        if(instruction instanceof InvokeDynamicInsnNode indy)for(Object argument:indy.bsmArgs)if(discoveryConstant(argument))return true;
        return false;
    }
    static void normalize(List<ClassNode> classes) {
        Map<String,List<MethodNode>> originalMethods=new HashMap<>();for(ClassNode type:classes)originalMethods.put(type.name,List.copyOf(type.methods));
        boolean needed=classes.stream().filter(c->!c.name.startsWith("java/") && !c.name.startsWith("javaw/runtime/"))
            .flatMap(c->c.methods.stream()).flatMap(m->Arrays.stream(m.instructions.toArray()))
            .anyMatch(BytecodeReflection::discoveryUse);
        // Caller identity is attached at the original call site, before helper generation.
        for(ClassNode caller:classes)if(!caller.name.startsWith("java/") && !caller.name.startsWith("javaw/runtime/"))
            for(MethodNode method:List.copyOf(caller.methods))for(AbstractInsnNode instruction:method.instructions.toArray())
                if(instruction instanceof MethodInsnNode call && callerSensitive(call.owner,call.name)) {
                    method.maxStack++;method.instructions.insertBefore(call,new LdcInsnNode(Type.getObjectType(caller.name)));
                    call.name=callerName(call.name);
                    call.desc=call.desc.substring(0,call.desc.indexOf(')'))+"Ljava/lang/Class;"+call.desc.substring(call.desc.indexOf(')'));
                } else if(instruction instanceof LdcInsnNode ldc)ldc.cst=callerConstant(caller,ldc.cst);
                else if(instruction instanceof InvokeDynamicInsnNode indy)for(int i=0;i<indy.bsmArgs.length;i++)indy.bsmArgs[i]=callerConstant(caller,indy.bsmArgs[i]);
        if(!needed && classes.stream().noneMatch(c->!c.name.startsWith("java/") && (c.access&ACC_ENUM)!=0))return;
        BytecodeAnnotations.normalize(classes);
        Map<String,ClassNode> linked=new LinkedHashMap<>();for(ClassNode type:classes){BytecodeDeclarations.validate(type);linked.put(type.name,type);}
        ClassNode support=linked.get("javaw/runtime/ReflectionSupport");
        MethodNode discover=support.methods.stream().filter(m->m.name.equals("declaredMethods")).findFirst().orElseThrow();
        discover.instructions.clear();discover.tryCatchBlocks.clear();
        MethodNode constructors=support.methods.stream().filter(m->m.name.equals("declaredConstructors")).findFirst().orElseThrow();constructors.instructions.clear();
        MethodNode fields=support.methods.stream().filter(m->m.name.equals("declaredFields")).findFirst().orElseThrow();fields.instructions.clear();
        MethodNode nest=support.methods.stream().filter(m->m.name.equals("nestHost")).findFirst().orElseThrow();nest.instructions.clear();
        MethodNode initialize=support.methods.stream().filter(m->m.name.equals("initialize")).findFirst().orElseThrow();initialize.instructions.clear();
        MethodNode arrays=support.methods.stream().filter(m->m.name.equals("newArray")).findFirst().orElseThrow();arrays.instructions.clear();
        MethodNode resolve=support.methods.stream().filter(m->m.name.equals("resolve")).findFirst().orElseThrow();resolve.instructions.clear();
        for(String publicType:List.of("java/lang/Object"))resolveRoute(resolve,publicType,null);
        for(ClassNode runtime:classes)if(runtime.name.startsWith("java/") && (runtime.access&ACC_PUBLIC)!=0)resolveRoute(resolve,runtime.name,null);
        MethodNode members=support.methods.stream().filter(m->m.name.equals("declaredClasses")).findFirst().orElseThrow();members.instructions.clear();
        MethodNode enclosing=support.methods.stream().filter(m->m.name.equals("enclosing")).findFirst().orElseThrow();enclosing.instructions.clear();
        MethodNode declaring=support.methods.stream().filter(m->m.name.equals("declaringClass")).findFirst().orElseThrow();declaring.instructions.clear();
        List<ClassNode> generated=new ArrayList<>();
        for(ClassNode component:classes)if((component.access&ACC_PUBLIC)!=0 && component.name.startsWith("java/"))for(int rank=0;rank<=1;rank++)arrayRoute(arrays,component.name,rank);
        for(String primitive:List.of("Z","B","C","S","I","J","F","D"))for(int rank=1;rank<=2;rank++)arrayRoute(arrays,primitive,rank);
        ClassNode object=type("javaw/runtime/ObjectReflection");generated.add(object);objectMetadata(object);objectConstructor(object);
        Label notObjectConstructor=new Label();constructors.visitVarInsn(ALOAD,0);constructors.visitLdcInsn(Type.getType(Object.class));constructors.visitJumpInsn(IF_ACMPNE,notObjectConstructor);
        call(constructors,INVOKESTATIC,object.name,"constructors","()[Ljava/lang/reflect/Constructor;");constructors.visitInsn(ARETURN);constructors.visitLabel(notObjectConstructor);constructors.visitFrame(F_SAME,0,null,0,null);
        Label notObject=new Label();discover.visitVarInsn(ALOAD,0);discover.visitLdcInsn(Type.getType(Object.class));discover.visitJumpInsn(IF_ACMPNE,notObject);
        call(discover,INVOKESTATIC,object.name,"methods","()[Ljava/lang/reflect/Method;");discover.visitInsn(ARETURN);discover.visitLabel(notObject);discover.visitFrame(F_SAME,0,null,0,null);
        for(ClassNode owner:List.copyOf(classes)) {
            if(owner.name.startsWith("java/") && !owner.name.startsWith("java/lang/annotation/") && (owner.access&ACC_ANNOTATION)==0 || owner.name.startsWith("javaw/runtime/") || owner.name.contains("$$javaw$annotation"))continue;
            String name=owner.name+"$$javaw$reflection";
            if(linked.containsKey(name))throw new IllegalArgumentException("reserved reflection adapter collision: "+name);
            ClassNode adapter=type(name);adapter.sourceFile=owner.sourceFile;
            ClassNode host=owner.nestHostClass==null?owner:linked.get(owner.nestHostClass);
            if(host!=null && (host.version&0xffff)>=V1_7)nest(adapter,owner,linked);
            generated.add(adapter);
            MethodNode mirror=method(adapter,ACC_PUBLIC|ACC_STATIC,"mirror","()Ljava/lang/Class;");mirror.visitLdcInsn(Type.getObjectType(owner.name));mirror.visitInsn(ARETURN);finish(mirror,0);resolveRoute(resolve,owner.name,adapter.name);
            MethodNode memberClasses=method(adapter,ACC_PUBLIC|ACC_STATIC,"memberClasses","()[Ljava/lang/Class;");List<String> nested=new ArrayList<>();
            if(owner.innerClasses!=null)for(InnerClassNode inner:owner.innerClasses)if(owner.name.equals(inner.outerName) && inner.innerName!=null)nested.add(inner.name);
            memberClasses.visitLdcInsn(nested.size());memberClasses.visitTypeInsn(ANEWARRAY,"java/lang/Class");for(int i=0;i<nested.size();i++){memberClasses.visitInsn(DUP);memberClasses.visitLdcInsn(i);memberClasses.visitLdcInsn(nested.get(i).replace('/','.'));call(memberClasses,INVOKESTATIC,"javaw/runtime/ReflectionSupport","resolve","(Ljava/lang/String;)Ljava/lang/Class;");memberClasses.visitInsn(AASTORE);}memberClasses.visitInsn(ARETURN);finish(memberClasses,0);
            Label nextMembers=new Label();members.visitVarInsn(ALOAD,0);call(members,INVOKEVIRTUAL,"java/lang/Class","getName","()Ljava/lang/String;");members.visitLdcInsn(owner.name.replace('/','.'));call(members,INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");members.visitJumpInsn(IFEQ,nextMembers);call(members,INVOKESTATIC,adapter.name,"memberClasses","()[Ljava/lang/Class;");members.visitInsn(ARETURN);members.visitLabel(nextMembers);members.visitFrame(F_SAME,0,null,0,null);
            if(owner.outerClass!=null){Label next=new Label();enclosing.visitVarInsn(ALOAD,0);call(enclosing,INVOKEVIRTUAL,"java/lang/Class","getName","()Ljava/lang/String;");enclosing.visitLdcInsn(owner.name.replace('/','.'));call(enclosing,INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");enclosing.visitJumpInsn(IFEQ,next);
                enclosing.visitInsn(ICONST_3);enclosing.visitTypeInsn(ANEWARRAY,"java/lang/String");String[] values={owner.outerClass.replace('/','.'),owner.outerMethod,owner.outerMethodDesc};for(int i=0;i<3;i++){enclosing.visitInsn(DUP);enclosing.visitLdcInsn(i);if(values[i]==null)enclosing.visitInsn(ACONST_NULL);else enclosing.visitLdcInsn(values[i]);enclosing.visitInsn(AASTORE);}enclosing.visitInsn(ARETURN);enclosing.visitLabel(next);enclosing.visitFrame(F_SAME,0,null,0,null);
            }
            String outer=null;if(owner.innerClasses!=null)for(InnerClassNode inner:owner.innerClasses)if(inner.name.equals(owner.name) && inner.outerName!=null)outer=inner.outerName;
            if(outer!=null){
                MethodNode outerMethod=method(adapter,ACC_PUBLIC|ACC_STATIC,"outer","()Ljava/lang/Class;");outerMethod.visitLdcInsn(outer.replace('/','.'));call(outerMethod,INVOKESTATIC,"javaw/runtime/ReflectionSupport","resolve","(Ljava/lang/String;)Ljava/lang/Class;");outerMethod.visitInsn(ARETURN);finish(outerMethod,0);
                Label next=new Label();declaring.visitVarInsn(ALOAD,0);call(declaring,INVOKEVIRTUAL,"java/lang/Class","getName","()Ljava/lang/String;");declaring.visitLdcInsn(owner.name.replace('/','.'));call(declaring,INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");declaring.visitJumpInsn(IFEQ,next);
                call(declaring,INVOKESTATIC,adapter.name,"outer","()Ljava/lang/Class;");declaring.visitInsn(ARETURN);declaring.visitLabel(next);declaring.visitFrame(F_SAME,0,null,0,null);
            }
            for(int rank=0;rank<=1;rank++) {
                MethodNode array=method(adapter,ACC_PUBLIC|ACC_STATIC,"array"+rank,"(I)Ljava/lang/Object;");array.visitVarInsn(ILOAD,0);array.visitTypeInsn(ANEWARRAY,rank==0?owner.name:"[L"+owner.name+";");array.visitInsn(ARETURN);finish(array,1);
                Label next=new Label();arrays.visitVarInsn(ALOAD,0);call(arrays,INVOKEVIRTUAL,"java/lang/Class","getName","()Ljava/lang/String;");arrays.visitLdcInsn(rank==0?owner.name.replace('/','.'):("[L"+owner.name.replace('/','.')+";"));call(arrays,INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");arrays.visitJumpInsn(IFEQ,next);
                arrays.visitVarInsn(ILOAD,1);call(arrays,INVOKESTATIC,adapter.name,"array"+rank,"(I)Ljava/lang/Object;");arrays.visitInsn(ARETURN);arrays.visitLabel(next);arrays.visitFrame(F_SAME,0,null,0,null);
            }
            String initializer="$javaw$reflectInit";
            if(owner.methods.stream().anyMatch(m->m.name.equals(initializer)))throw new IllegalArgumentException("reserved reflective initializer collision");
            // Snapshot declarations before adding any bridges.
            List<MethodNode> originals=originalMethods.get(owner.name);
            MethodNode trigger=method(owner,ACC_PUBLIC|ACC_STATIC|ACC_SYNTHETIC,initializer,"()V");trigger.visitInsn(RETURN);finish(trigger,0);
            MethodNode initAdapter=method(adapter,ACC_PUBLIC|ACC_STATIC,"initialize","()V");initAdapter.visitMethodInsn(INVOKESTATIC,owner.name,initializer,"()V",(owner.access&ACC_INTERFACE)!=0);initAdapter.visitInsn(RETURN);finish(initAdapter,0);
            Label nextInit=new Label();initialize.visitVarInsn(ALOAD,0);call(initialize,INVOKEVIRTUAL,"java/lang/Class","getName","()Ljava/lang/String;");initialize.visitLdcInsn(owner.name.replace('/','.'));call(initialize,INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");initialize.visitJumpInsn(IFEQ,nextInit);
            call(initialize,INVOKESTATIC,name,"initialize","()V");initialize.visitInsn(RETURN);initialize.visitLabel(nextInit);initialize.visitFrame(F_SAME,0,null,0,null);
            Label nextNest=new Label();nest.visitVarInsn(ALOAD,0);call(nest,INVOKEVIRTUAL,"java/lang/Class","getName","()Ljava/lang/String;");nest.visitLdcInsn(owner.name.replace('/','.'));call(nest,INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");nest.visitJumpInsn(IFEQ,nextNest);
            MethodNode hostMethod=method(adapter,ACC_PUBLIC|ACC_STATIC,"nestHost","()Ljava/lang/Class;");hostMethod.visitLdcInsn(Type.getObjectType(host==null?owner.name:host.name));hostMethod.visitInsn(ARETURN);finish(hostMethod,0);
            call(nest,INVOKESTATIC,name,"nestHost","()Ljava/lang/Class;");nest.visitInsn(ARETURN);nest.visitLabel(nextNest);nest.visitFrame(F_SAME,0,null,0,null);
            MethodNode metadata=method(adapter,ACC_PUBLIC|ACC_STATIC,"methods","()[Ljava/lang/reflect/Method;");
            List<MethodNode> declarations=originals.stream().filter(m->!m.name.startsWith("<"))
                .sorted(Comparator.comparing((MethodNode m)->m.name).thenComparing(m->m.desc)).toList();
            metadata.visitLdcInsn(declarations.size());metadata.visitTypeInsn(ANEWARRAY,"java/lang/reflect/Method");
            for(int index=0;index<declarations.size();index++) {
                MethodNode declaration=declarations.get(index);
                metadata.visitInsn(DUP);metadata.visitLdcInsn(index);metadata.visitTypeInsn(NEW,"java/lang/reflect/Method");metadata.visitInsn(DUP);
                metadata.visitLdcInsn(Type.getObjectType(owner.name));metadata.visitLdcInsn(declaration.name);metadata.visitLdcInsn(Type.getMethodType(declaration.desc));
                String bridge=bridge(owner,declaration);
                metadata.visitLdcInsn(new Handle(H_INVOKESTATIC,owner.name,bridge,bridgeDescriptor(owner,declaration),(owner.access&ACC_INTERFACE)!=0));
                metadata.visitLdcInsn(declaration.access&0xffff);metadata.visitLdcInsn(declaration.exceptions.size());metadata.visitTypeInsn(ANEWARRAY,"java/lang/Class");
                for(int i=0;i<declaration.exceptions.size();i++){metadata.visitInsn(DUP);metadata.visitLdcInsn(i);metadata.visitLdcInsn(Type.getObjectType(declaration.exceptions.get(i)));metadata.visitInsn(AASTORE);}
                call(metadata,INVOKESPECIAL,"java/lang/reflect/Method","<init>","(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;I[Ljava/lang/Class;)V");metadata.visitInsn(AASTORE);
            }
            metadata.visitInsn(ARETURN);finish(metadata,0);
            constructors(adapter,owner,originals);fields(adapter,owner);
            route(constructors,owner.name,adapter.name,"constructors","Constructor");
            route(fields,owner.name,adapter.name,"fields","Field");
            Label next=new Label();discover.visitVarInsn(ALOAD,0);call(discover,INVOKEVIRTUAL,"java/lang/Class","getName","()Ljava/lang/String;");
            discover.visitLdcInsn(owner.name.replace('/','.'));call(discover,INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");discover.visitJumpInsn(IFEQ,next);
            call(discover,INVOKESTATIC,name,"methods","()[Ljava/lang/reflect/Method;");discover.visitInsn(ARETURN);
            discover.visitLabel(next);discover.visitFrame(F_SAME,0,null,0,null);
        }
        discover.visitTypeInsn(NEW,"java/lang/UnsupportedOperationException");discover.visitInsn(DUP);discover.visitLdcInsn("declared-method metadata is available only for original linked application classes");
        call(discover,INVOKESPECIAL,"java/lang/UnsupportedOperationException","<init>","(Ljava/lang/String;)V");discover.visitInsn(ATHROW);finish(discover,1);
        for(MethodNode table:List.of(constructors,fields)){table.visitTypeInsn(NEW,"java/lang/UnsupportedOperationException");table.visitInsn(DUP);call(table,INVOKESPECIAL,"java/lang/UnsupportedOperationException","<init>","()V");table.visitInsn(ATHROW);finish(table,1);}
        resolve.visitTypeInsn(NEW,"java/lang/TypeNotPresentException");resolve.visitInsn(DUP);resolve.visitVarInsn(ALOAD,0);resolve.visitInsn(ACONST_NULL);call(resolve,INVOKESPECIAL,"java/lang/TypeNotPresentException","<init>","(Ljava/lang/String;Ljava/lang/Throwable;)V");resolve.visitInsn(ATHROW);finish(resolve,1);
        enclosing.visitInsn(ACONST_NULL);enclosing.visitInsn(ARETURN);finish(enclosing,1);
        members.visitTypeInsn(NEW,"java/lang/UnsupportedOperationException");members.visitInsn(DUP);call(members,INVOKESPECIAL,"java/lang/UnsupportedOperationException","<init>","()V");members.visitInsn(ATHROW);finish(members,1);
        declaring.visitInsn(ACONST_NULL);declaring.visitInsn(ARETURN);finish(declaring,1);
        arrays.visitTypeInsn(NEW,"java/lang/UnsupportedOperationException");arrays.visitInsn(DUP);arrays.visitLdcInsn("reflective array component/rank is outside the linked array closure");call(arrays,INVOKESPECIAL,"java/lang/UnsupportedOperationException","<init>","(Ljava/lang/String;)V");arrays.visitInsn(ATHROW);finish(arrays,2);
        initialize.visitInsn(RETURN);finish(initialize,1);
        nest.visitVarInsn(ALOAD,0);nest.visitInsn(ARETURN);finish(nest,1);
        classes.addAll(generated);
    }
    private static String bridgeDescriptor(ClassNode owner,MethodNode declaration) {
        return (declaration.access&ACC_STATIC)!=0?declaration.desc:"(L"+owner.name+";"+declaration.desc.substring(1);
    }
    private static String bridge(ClassNode owner,MethodNode declaration) {
        String name="$javaw$reflect$"+owner.methods.size();
        if(owner.methods.stream().anyMatch(m->m.name.equals(name)))throw new IllegalArgumentException("reserved reflective bridge collision");
        MethodNode body=method(owner,ACC_PUBLIC|ACC_STATIC|ACC_SYNTHETIC,name,bridgeDescriptor(owner,declaration));
        Type[] parameters=Type.getArgumentTypes(body.desc);int slot=0;
        for(Type parameter:parameters){body.visitVarInsn(parameter.getOpcode(ILOAD),slot);slot+=parameter.getSize();}
        int opcode=(declaration.access&ACC_STATIC)!=0?INVOKESTATIC:(owner.access&ACC_INTERFACE)!=0?((declaration.access&ACC_PRIVATE)!=0?INVOKESPECIAL:INVOKEINTERFACE):INVOKEVIRTUAL;
        body.visitMethodInsn(opcode,owner.name,declaration.name,declaration.desc,(owner.access&ACC_INTERFACE)!=0);
        body.visitInsn(Type.getReturnType(declaration.desc).getOpcode(IRETURN));finish(body,slot);return name;
    }

    private static void objectMetadata(ClassNode object) {
        MethodNode code=method(object,ACC_PUBLIC|ACC_STATIC,"methods","()[Ljava/lang/reflect/Method;");
        String[] names={"getClass","hashCode","equals","toString","clone","finalize","wait","wait","wait","notify","notifyAll"};
        String[] descriptors={"()Ljava/lang/Class;","()I","(Ljava/lang/Object;)Z","()Ljava/lang/String;","()Ljava/lang/Object;","()V","()V","(J)V","(JI)V","()V","()V"};
        int[] flags={273,257,1,1,260,4,17,273,17,273,273};
        code.visitLdcInsn(names.length);code.visitTypeInsn(ANEWARRAY,"java/lang/reflect/Method");
        for(int i=0;i<names.length;i++) {
            code.visitInsn(DUP);code.visitLdcInsn(i);code.visitTypeInsn(NEW,"java/lang/reflect/Method");code.visitInsn(DUP);
            code.visitLdcInsn(Type.getType(Object.class));code.visitLdcInsn(names[i]);code.visitLdcInsn(Type.getMethodType(descriptors[i]));
            if(i<4)code.visitLdcInsn(new Handle(H_INVOKEVIRTUAL,"java/lang/Object",names[i],descriptors[i],false));else code.visitInsn(ACONST_NULL);
            code.visitLdcInsn(flags[i]);code.visitInsn(ICONST_0);code.visitTypeInsn(ANEWARRAY,"java/lang/Class");
            call(code,INVOKESPECIAL,"java/lang/reflect/Method","<init>","(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;I[Ljava/lang/Class;)V");code.visitInsn(AASTORE);
        }
        code.visitInsn(ARETURN);finish(code,0);
    }

    private static void route(MethodNode table,String owner,String adapter,String name,String result) {
        Label next=new Label();table.visitVarInsn(ALOAD,0);call(table,INVOKEVIRTUAL,"java/lang/Class","getName","()Ljava/lang/String;");table.visitLdcInsn(owner.replace('/','.'));
        call(table,INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");table.visitJumpInsn(IFEQ,next);
        call(table,INVOKESTATIC,adapter,name,"()[Ljava/lang/reflect/"+result+";");table.visitInsn(ARETURN);table.visitLabel(next);table.visitFrame(F_SAME,0,null,0,null);
    }
    private static void constructors(ClassNode adapter,ClassNode owner,List<MethodNode> originals) {
        MethodNode table=method(adapter,ACC_PUBLIC|ACC_STATIC,"constructors","()[Ljava/lang/reflect/Constructor;");
        List<MethodNode> selected=originals.stream().filter(m->m.name.equals("<init>")).toList();table.visitLdcInsn(selected.size());table.visitTypeInsn(ANEWARRAY,"java/lang/reflect/Constructor");
        for(int i=0;i<selected.size();i++) {
            MethodNode original=selected.get(i);table.visitInsn(DUP);table.visitLdcInsn(i);table.visitTypeInsn(NEW,"java/lang/reflect/Constructor");table.visitInsn(DUP);
            table.visitLdcInsn(Type.getObjectType(owner.name));table.visitLdcInsn(Type.getMethodType(original.desc));
            if((owner.access&ACC_ABSTRACT)!=0)table.visitInsn(ACONST_NULL);
            else {
                String name="$javaw$construct$"+owner.methods.size(),descriptor=Type.getMethodDescriptor(Type.getObjectType(owner.name),Type.getArgumentTypes(original.desc));
                MethodNode body=method(owner,ACC_PUBLIC|ACC_STATIC|ACC_SYNTHETIC,name,descriptor);body.visitTypeInsn(NEW,owner.name);body.visitInsn(DUP);int slot=0;
                for(Type argument:Type.getArgumentTypes(original.desc)){body.visitVarInsn(argument.getOpcode(ILOAD),slot);slot+=argument.getSize();}
                call(body,INVOKESPECIAL,owner.name,"<init>",original.desc);body.visitInsn(ARETURN);finish(body,slot);
                table.visitLdcInsn(new Handle(H_INVOKESTATIC,owner.name,name,descriptor,false));
            }
            table.visitLdcInsn(original.access&0xffff);exceptions(table,original.exceptions);
            call(table,INVOKESPECIAL,"java/lang/reflect/Constructor","<init>","(Ljava/lang/Class;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;I[Ljava/lang/Class;)V");table.visitInsn(AASTORE);
        }
        table.visitInsn(ARETURN);finish(table,0);
    }
    private static void exceptions(MethodNode code,List<String> names) {
        code.visitLdcInsn(names.size());code.visitTypeInsn(ANEWARRAY,"java/lang/Class");
        for(int i=0;i<names.size();i++){code.visitInsn(DUP);code.visitLdcInsn(i);code.visitLdcInsn(Type.getObjectType(names.get(i)));code.visitInsn(AASTORE);}
    }
    private static void fields(ClassNode adapter,ClassNode owner) {
        MethodNode table=method(adapter,ACC_PUBLIC|ACC_STATIC,"fields","()[Ljava/lang/reflect/Field;");table.visitLdcInsn(owner.fields.size());table.visitTypeInsn(ANEWARRAY,"java/lang/reflect/Field");
        for(int i=0;i<owner.fields.size();i++) {
            FieldNode original=owner.fields.get(i);table.visitInsn(DUP);table.visitLdcInsn(i);table.visitTypeInsn(NEW,"java/lang/reflect/Field");table.visitInsn(DUP);
            table.visitLdcInsn(Type.getObjectType(owner.name));table.visitLdcInsn(original.name);classLiteral(table,Type.getType(original.desc));table.visitLdcInsn(original.access&0xffff);
            for(boolean write:new boolean[]{false,true}) {
                if(write && (original.access&ACC_FINAL)!=0){table.visitInsn(ACONST_NULL);continue;}
                boolean statik=(original.access&ACC_STATIC)!=0;
                String name="$javaw$field$"+owner.methods.size();String descriptor="("+(statik?"":"L"+owner.name+";")+(write?original.desc:"")+")"+(write?"V":original.desc);
                MethodNode body=method(owner,ACC_PUBLIC|ACC_STATIC|ACC_SYNTHETIC,name,descriptor);int slot=0;
                if(!statik){body.visitVarInsn(ALOAD,slot++);}if(write){body.visitVarInsn(Type.getType(original.desc).getOpcode(ILOAD),slot);slot+=Type.getType(original.desc).getSize();}
                body.visitFieldInsn(write?(statik?PUTSTATIC:PUTFIELD):(statik?GETSTATIC:GETFIELD),owner.name,original.name,original.desc);
                body.visitInsn(write?RETURN:Type.getType(original.desc).getOpcode(IRETURN));finish(body,slot);
                table.visitLdcInsn(new Handle(H_INVOKESTATIC,owner.name,name,descriptor,(owner.access&ACC_INTERFACE)!=0));
            }
            call(table,INVOKESPECIAL,"java/lang/reflect/Field","<init>","(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;ILjava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;)V");table.visitInsn(AASTORE);
        }
        table.visitInsn(ARETURN);finish(table,0);
    }
    static void classLiteral(MethodNode code,Type type){
        if(type.getSort()==Type.OBJECT || type.getSort()==Type.ARRAY)code.visitLdcInsn(type);
        else {code.visitLdcInsn(BytecodeClasses.primitiveTokens().get(type.getDescriptor()));call(code,INVOKESTATIC,"javaw/runtime/ExceptionSupport","classMirror","(I)Ljava/lang/Class;");}
    }

    private static void arrayRoute(MethodNode code,String component,int rank){
        String descriptor=rank==0?component:"[".repeat(rank)+(component.length()==1?component:"L"+component+";");
        Label next=new Label();code.visitVarInsn(ALOAD,0);call(code,INVOKEVIRTUAL,"java/lang/Class","getName","()Ljava/lang/String;");code.visitLdcInsn(descriptor.replace('/','.'));call(code,INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");code.visitJumpInsn(IFEQ,next);
        code.visitVarInsn(ILOAD,1);code.visitTypeInsn(ANEWARRAY,descriptor);code.visitInsn(ARETURN);code.visitLabel(next);code.visitFrame(F_SAME,0,null,0,null);
    }

    private static void resolveRoute(MethodNode code,String name,String adapter){
        Label next=new Label();code.visitVarInsn(ALOAD,0);code.visitLdcInsn(name.replace('/','.'));call(code,INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");code.visitJumpInsn(IFEQ,next);
        if(adapter==null)code.visitLdcInsn(Type.getObjectType(name));else call(code,INVOKESTATIC,adapter,"mirror","()Ljava/lang/Class;");code.visitInsn(ARETURN);code.visitLabel(next);code.visitFrame(F_SAME,0,null,0,null);
    }

    private static void objectConstructor(ClassNode object){
        MethodNode create=method(object,ACC_PUBLIC|ACC_STATIC,"construct","()Ljava/lang/Object;");create.visitTypeInsn(NEW,"java/lang/Object");create.visitInsn(DUP);call(create,INVOKESPECIAL,"java/lang/Object","<init>","()V");create.visitInsn(ARETURN);finish(create,0);
        MethodNode table=method(object,ACC_PUBLIC|ACC_STATIC,"constructors","()[Ljava/lang/reflect/Constructor;");table.visitInsn(ICONST_1);table.visitTypeInsn(ANEWARRAY,"java/lang/reflect/Constructor");table.visitInsn(DUP);table.visitInsn(ICONST_0);table.visitTypeInsn(NEW,"java/lang/reflect/Constructor");table.visitInsn(DUP);
        table.visitLdcInsn(Type.getType(Object.class));table.visitLdcInsn(Type.getMethodType("()V"));table.visitLdcInsn(new Handle(H_INVOKESTATIC,object.name,"construct","()Ljava/lang/Object;",false));table.visitInsn(ICONST_1);table.visitInsn(ICONST_0);table.visitTypeInsn(ANEWARRAY,"java/lang/Class");
        call(table,INVOKESPECIAL,"java/lang/reflect/Constructor","<init>","(Ljava/lang/Class;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;I[Ljava/lang/Class;)V");table.visitInsn(AASTORE);table.visitInsn(ARETURN);finish(table,0);
    }

    private static boolean callerSensitive(String owner,String name){
        return owner.equals("java/lang/reflect/Method") && (name.equals("invoke") || name.equals("canAccess")) ||
            (owner.equals("java/lang/reflect/Constructor") || owner.equals("java/lang/Class")) && name.equals("newInstance") ||
            Set.of("java/lang/reflect/AccessibleObject","java/lang/reflect/Field","java/lang/reflect/Constructor").contains(owner) && name.equals("canAccess") ||
            owner.equals("java/lang/reflect/Field") && Set.of("get","set","getBoolean","getByte","getChar","getShort","getInt","getLong","getFloat","getDouble","setBoolean","setByte","setChar","setShort","setInt","setLong","setFloat","setDouble").contains(name);
    }
    private static String callerName(String name){return "javaw"+Character.toUpperCase(name.charAt(0))+name.substring(1);}
    private static Object callerConstant(ClassNode caller,Object constant){
        if(constant instanceof Handle target && callerSensitive(target.getOwner(),target.getName())){
            String name="$javaw$caller$"+caller.methods.size(),descriptor="(L"+target.getOwner()+";"+target.getDesc().substring(1);
            if(caller.methods.stream().anyMatch(m->m.name.equals(name)))throw new IllegalArgumentException("reserved caller bridge collision");
            MethodNode bridge=method(caller,ACC_PUBLIC|ACC_STATIC|ACC_SYNTHETIC,name,descriptor);int slot=0;
            for(Type parameter:Type.getArgumentTypes(descriptor)){bridge.visitVarInsn(parameter.getOpcode(ILOAD),slot);slot+=parameter.getSize();}
            bridge.visitLdcInsn(Type.getObjectType(caller.name));String forwarded=target.getDesc().substring(0,target.getDesc().indexOf(')'))+"Ljava/lang/Class;"+target.getDesc().substring(target.getDesc().indexOf(')'));
            call(bridge,INVOKEVIRTUAL,target.getOwner(),callerName(target.getName()),forwarded);bridge.visitInsn(Type.getReturnType(descriptor).getOpcode(IRETURN));finish(bridge,slot);
            return new Handle(H_INVOKESTATIC,caller.name,name,descriptor,(caller.access&ACC_INTERFACE)!=0);
        }
        if(constant instanceof ConstantDynamic dynamic){Object[] arguments=new Object[dynamic.getBootstrapMethodArgumentCount()];for(int i=0;i<arguments.length;i++)arguments[i]=callerConstant(caller,dynamic.getBootstrapMethodArgument(i));return new ConstantDynamic(dynamic.getName(),dynamic.getDescriptor(),dynamic.getBootstrapMethod(),arguments);}return constant;
    }

}
