package com.example.javaw.bytecode;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;
import static com.example.javaw.bytecode.BytecodeDynamic.*;

/** Original application declarations only: generated compiler helpers are never reflected. */
final class BytecodeReflection implements Opcodes {
    private static boolean discovery(String owner,String name) {
        return owner.equals("java/lang/Class") && (name.equals("getDeclaredMethods") || name.equals("getDeclaredMethod"));
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
        boolean needed=classes.stream().filter(c->!c.name.startsWith("java/") && !c.name.startsWith("javaw/runtime/"))
            .flatMap(c->c.methods.stream()).flatMap(m->Arrays.stream(m.instructions.toArray()))
            .anyMatch(BytecodeReflection::discoveryUse);
        if(!needed)return;
        Map<String,ClassNode> linked=new LinkedHashMap<>();for(ClassNode type:classes){BytecodeDeclarations.validate(type);linked.put(type.name,type);}
        ClassNode support=linked.get("javaw/runtime/ReflectionSupport");
        MethodNode discover=support.methods.stream().filter(m->m.name.equals("declaredMethods")).findFirst().orElseThrow();
        discover.instructions.clear();discover.tryCatchBlocks.clear();
        List<ClassNode> generated=new ArrayList<>();
        for(ClassNode owner:List.copyOf(classes)) {
            if(owner.name.startsWith("java/") || owner.name.startsWith("javaw/runtime/"))continue;
            String name=owner.name+"$$javaw$reflection";
            if(linked.containsKey(name))throw new IllegalArgumentException("reserved reflection adapter collision: "+name);
            ClassNode adapter=type(name);adapter.sourceFile=owner.sourceFile;
            ClassNode host=owner.nestHostClass==null?owner:linked.get(owner.nestHostClass);
            if(host!=null && (host.version&0xffff)>=V1_7)nest(adapter,owner,linked);
            generated.add(adapter);
            MethodNode metadata=method(adapter,ACC_PUBLIC|ACC_STATIC,"methods","()[Ljava/lang/reflect/Method;");
            List<MethodNode> declarations=owner.methods.stream().filter(m->!m.name.startsWith("<"))
                .sorted(Comparator.comparing((MethodNode m)->m.name).thenComparing(m->m.desc)).toList();
            metadata.visitLdcInsn(declarations.size());metadata.visitTypeInsn(ANEWARRAY,"java/lang/reflect/Method");
            for(int index=0;index<declarations.size();index++) {
                MethodNode declaration=declarations.get(index);
                metadata.visitInsn(DUP);metadata.visitLdcInsn(index);metadata.visitTypeInsn(NEW,"java/lang/reflect/Method");metadata.visitInsn(DUP);
                metadata.visitLdcInsn(Type.getObjectType(owner.name));metadata.visitLdcInsn(declaration.name);metadata.visitLdcInsn(Type.getMethodType(declaration.desc));
                metadata.visitLdcInsn(declaration.access&0xffff);metadata.visitLdcInsn(declaration.exceptions.size());metadata.visitTypeInsn(ANEWARRAY,"java/lang/Class");
                for(int i=0;i<declaration.exceptions.size();i++){metadata.visitInsn(DUP);metadata.visitLdcInsn(i);metadata.visitLdcInsn(Type.getObjectType(declaration.exceptions.get(i)));metadata.visitInsn(AASTORE);}
                call(metadata,INVOKESPECIAL,"java/lang/reflect/Method","<init>","(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;I[Ljava/lang/Class;)V");metadata.visitInsn(AASTORE);
            }
            metadata.visitInsn(ARETURN);finish(metadata,0);
            Label next=new Label();discover.visitVarInsn(ALOAD,0);call(discover,INVOKEVIRTUAL,"java/lang/Class","getName","()Ljava/lang/String;");
            discover.visitLdcInsn(owner.name.replace('/','.'));call(discover,INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");discover.visitJumpInsn(IFEQ,next);
            call(discover,INVOKESTATIC,name,"methods","()[Ljava/lang/reflect/Method;");discover.visitInsn(ARETURN);
            discover.visitLabel(next);discover.visitFrame(F_SAME,0,null,0,null);
        }
        discover.visitTypeInsn(NEW,"java/lang/UnsupportedOperationException");discover.visitInsn(DUP);discover.visitLdcInsn("declared-method metadata is available only for original linked application classes");
        call(discover,INVOKESPECIAL,"java/lang/UnsupportedOperationException","<init>","(Ljava/lang/String;)V");discover.visitInsn(ATHROW);finish(discover,1);
        classes.addAll(generated);
    }
}
