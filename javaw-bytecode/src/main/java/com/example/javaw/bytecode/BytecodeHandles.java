package com.example.javaw.bytecode;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;
import static com.example.javaw.bytecode.BytecodeDynamic.*;

/** Direct constant handles and signature-polymorphic calls become ordinary verified AOT bytecode. */
final class BytecodeHandles implements Opcodes {
    private static final String HANDLE="java/lang/invoke/MethodHandle", OBJECTS="[Ljava/lang/Object;";
    static void normalize(List<ClassNode> classes) {
        Map<String,ClassNode> linked=new LinkedHashMap<>();
        for(ClassNode type:classes){BytecodeDeclarations.validate(type);linked.put(type.name,type);}
        List<ClassNode> generated=new ArrayList<>();
        for(ClassNode owner:List.copyOf(classes)) {
            Map<Handle,String> constants=new HashMap<>();int ordinal=0;
            for(MethodNode method:List.copyOf(owner.methods))for(AbstractInsnNode instruction:method.instructions.toArray()) {
                try {
                    if(instruction instanceof LdcInsnNode ldc && ldc.cst instanceof Handle target) {
                        if((owner.version&0xffff)<V1_7)throw new IllegalArgumentException("method handles require Java 7+ class files");
                        String factory=constants.get(target);
                        if(factory==null) {
                            factory=owner.name+"$$javaw$handle$"+ordinal++;
                            if(linked.containsKey(factory))throw new IllegalArgumentException("reserved handle class collision");
                            generated.add(direct(owner,target,factory,linked));constants.put(target,factory);
                        }
                        method.instructions.set(instruction,new MethodInsnNode(INVOKESTATIC,factory,"constant","()L"+HANDLE+";",false));
                    } else if(instruction instanceof MethodInsnNode call && call.owner.equals(HANDLE) && (call.name.equals("invoke") || call.name.equals("invokeExact"))) {
                        if(call.getOpcode()!=INVOKEVIRTUAL || call.itf || !BytecodeDeclarations.validDescriptor(call.desc,true))throw new IllegalArgumentException("invalid signature-polymorphic invocation");
                        String name="$javaw$invoke$"+owner.methods.size();
                        if(owner.methods.stream().anyMatch(m->m.name.equals(name)))throw new IllegalArgumentException("reserved invoke bridge collision");
                        String descriptor="(L"+HANDLE+";"+call.desc.substring(1);
                        invocation(owner,name,descriptor,call.desc,call.name.equals("invokeExact"));
                        method.instructions.set(call,new MethodInsnNode(INVOKESTATIC,owner.name,name,descriptor,(owner.access&ACC_INTERFACE)!=0));
                    }
                }catch(IllegalArgumentException error){throw BytecodeCompatibility.failure(owner,method,instruction,"method handle",error.getMessage());}
            }
        }
        classes.addAll(generated);
    }
    private static ClassNode direct(ClassNode caller,Handle target,String name,Map<String,ClassNode> linked) {
        boolean field=target.getTag()>=H_GETFIELD && target.getTag()<=H_PUTSTATIC;
        List<Type> parameters=new ArrayList<>();Type result;
        int opcode= switch(target.getTag()) {
            case H_GETFIELD->GETFIELD;case H_GETSTATIC->GETSTATIC;case H_PUTFIELD->PUTFIELD;case H_PUTSTATIC->PUTSTATIC;
            case H_INVOKESTATIC->INVOKESTATIC;case H_INVOKEVIRTUAL->INVOKEVIRTUAL;case H_INVOKEINTERFACE->INVOKEINTERFACE;
            case H_INVOKESPECIAL,H_NEWINVOKESPECIAL->INVOKESPECIAL;default->throw new IllegalArgumentException("unsupported handle kind");};
        BytecodeDispatch dispatch=new BytecodeDispatch(linked,caller.name);
        if(field) {
            if(!BytecodeDeclarations.validDescriptor(target.getDesc(),false))throw new IllegalArgumentException("invalid field handle descriptor");
            String declaring=dispatch.fieldOwner(new FieldInsnNode(opcode,target.getOwner(),target.getName(),target.getDesc()));
            boolean write=opcode==PUTFIELD || opcode==PUTSTATIC;
            if(write && linked.get(declaring).fields.stream().anyMatch(f->f.name.equals(target.getName()) && f.desc.equals(target.getDesc()) && (f.access&ACC_FINAL)!=0))
                throw new IllegalArgumentException("method handle cannot write a final field");
            if(opcode==GETFIELD || opcode==PUTFIELD)parameters.add(Type.getObjectType(dispatch.fieldReceiverIds(new FieldInsnNode(opcode,target.getOwner(),target.getName(),target.getDesc()))==null?target.getOwner():caller.name));
            if(write)parameters.add(Type.getType(target.getDesc()));result=write?Type.VOID_TYPE:Type.getType(target.getDesc());
        } else {
            if(!BytecodeDeclarations.validDescriptor(target.getDesc(),true))throw new IllegalArgumentException("invalid method handle descriptor");
            MethodInsnNode call=new MethodInsnNode(opcode,target.getOwner(),target.getName(),target.getDesc(),target.isInterface());
            if(target.getTag()==H_NEWINVOKESPECIAL && (!target.getName().equals("<init>") || !Type.getReturnType(target.getDesc()).equals(Type.VOID_TYPE)))throw new IllegalArgumentException("constructor handle requires <init> returning void");
            if(target.getTag()!=H_NEWINVOKESPECIAL && target.getName().startsWith("<"))throw new IllegalArgumentException("non-constructor handle cannot name an initializer");
            MethodNode declaration=handleDeclaration(linked,target.getOwner(),target.getName(),target.getDesc(),new HashSet<>());
            if(declaration!=null && (declaration.access&ACC_VARARGS)!=0)
                throw new IllegalArgumentException("variable-arity method handles require an explicit fixed-arity AOT wrapper");
            if(target.getTag()==H_NEWINVOKESPECIAL && (linked.get(target.getOwner()).access&ACC_ABSTRACT)!=0)
                throw new IllegalArgumentException("constructor handle for abstract class requires an explicit AOT factory");
            if(opcode==INVOKESTATIC)dispatch.staticTarget(call);
            else if(opcode==INVOKESPECIAL){if(call.itf)dispatch.interfaceSuperTarget(call);else dispatch.specialTarget(call);}
            else dispatch.targets(call);
            if(opcode!=INVOKESTATIC && target.getTag()!=H_NEWINVOKESPECIAL)parameters.add(Type.getObjectType(target.getTag()==H_INVOKESPECIAL?caller.name:dispatch.handleReceiver(call)));
            parameters.addAll(List.of(Type.getArgumentTypes(target.getDesc())));
            result=target.getTag()==H_NEWINVOKESPECIAL?Type.getObjectType(target.getOwner()):Type.getReturnType(target.getDesc());
        }
        String bridge="$javaw$handle$"+caller.methods.size();
        if(caller.methods.stream().anyMatch(m->m.name.equals(bridge)))throw new IllegalArgumentException("reserved handle bridge collision");
        if((caller.access&ACC_INTERFACE)!=0 && (caller.version&0xffff)<V1_8)caller.version=V1_8;
        MethodNode body=method(caller,ACC_PUBLIC|ACC_STATIC|ACC_SYNTHETIC,bridge,"("+OBJECTS+")Ljava/lang/Object;");
        if(target.getTag()==H_NEWINVOKESPECIAL){body.visitTypeInsn(NEW,target.getOwner());body.visitInsn(DUP);}
        for(int i=0;i<parameters.size();i++) {
            body.visitVarInsn(ALOAD,0);body.visitLdcInsn(i);body.visitInsn(AALOAD);
            unbox(body,parameters.get(i));
        }
        if(field)body.visitFieldInsn(opcode,target.getOwner(),target.getName(),target.getDesc());
        else body.visitMethodInsn(opcode,target.getOwner(),target.getName(),target.getDesc(),target.isInterface());
        if(result.equals(Type.VOID_TYPE))body.visitInsn(ACONST_NULL);else adapt(body,result,Type.getType(Object.class));
        body.visitInsn(ARETURN);finish(body,1);
        ClassNode adapter=type(name);adapter.superName=HANDLE;adapter.sourceFile=caller.sourceFile;nest(adapter,caller,linked);
        MethodNode init=method(adapter,ACC_PRIVATE,"<init>","()V");init.visitVarInsn(ALOAD,0);
        init.visitLdcInsn(Type.getMethodType(Type.getMethodDescriptor(result,parameters.toArray(Type[]::new))));
        call(init,INVOKESPECIAL,HANDLE,"<init>","(Ljava/lang/invoke/MethodType;)V");init.visitInsn(RETURN);finish(init,1);
        MethodNode invoke=method(adapter,ACC_PROTECTED,"javawInvoke","("+OBJECTS+")Ljava/lang/Object;");
        invoke.visitVarInsn(ALOAD,1);invoke.visitMethodInsn(INVOKESTATIC,caller.name,bridge,"("+OBJECTS+")Ljava/lang/Object;",(caller.access&ACC_INTERFACE)!=0);
        invoke.visitInsn(ARETURN);finish(invoke,2);
        adapter.fields.add(new FieldNode(ACC_PRIVATE|ACC_STATIC,"value","L"+HANDLE+";",null,null));
        MethodNode factory=method(adapter,ACC_PUBLIC|ACC_STATIC,"constant","()L"+HANDLE+";");Label ready=new Label();
        factory.visitFieldInsn(GETSTATIC,name,"value","L"+HANDLE+";");factory.visitInsn(DUP);factory.visitJumpInsn(IFNONNULL,ready);factory.visitInsn(POP);
        factory.visitTypeInsn(NEW,name);factory.visitInsn(DUP);call(factory,INVOKESPECIAL,name,"<init>","()V");
        factory.visitInsn(DUP);factory.visitFieldInsn(PUTSTATIC,name,"value","L"+HANDLE+";");
        factory.visitLabel(ready);factory.visitFrame(F_SAME1,0,null,1,new Object[]{HANDLE});factory.visitInsn(ARETURN);finish(factory,0);
        return adapter;
    }
    private static MethodNode handleDeclaration(Map<String,ClassNode> linked,String owner,String name,String desc,Set<String> visited) {
        ClassNode type=linked.get(owner);if(type==null || !visited.add(owner))return null;
        for(MethodNode method:type.methods)if(method.name.equals(name) && method.desc.equals(desc))return method;
        MethodNode inherited=handleDeclaration(linked,type.superName,name,desc,visited);if(inherited!=null)return inherited;
        for(String parent:type.interfaces){inherited=handleDeclaration(linked,parent,name,desc,visited);if(inherited!=null)return inherited;}return null;
    }
    static void unbox(MethodVisitor code,Type type) {
        if(type.getSort()==Type.OBJECT || type.getSort()==Type.ARRAY){adapt(code,Type.getType(Object.class),type);return;}
        String wrapper=switch(type.getSort()){case Type.BOOLEAN->"Boolean";case Type.BYTE->"Byte";case Type.CHAR->"Character";case Type.SHORT->"Short";case Type.INT->"Integer";case Type.LONG->"Long";case Type.FLOAT->"Float";default->"Double";};
        code.visitTypeInsn(CHECKCAST,"java/lang/"+wrapper);call(code,INVOKEVIRTUAL,"java/lang/"+wrapper,type.getClassName()+"Value","()"+type.getDescriptor());
    }
    private static void invocation(ClassNode owner,String name,String descriptor,String signature,boolean exact) {
        if((owner.access&ACC_INTERFACE)!=0 && (owner.version&0xffff)<V1_8)owner.version=V1_8;
        MethodNode code=method(owner,ACC_PUBLIC|ACC_STATIC|ACC_SYNTHETIC,name,descriptor);
        code.visitVarInsn(ALOAD,0);call(code,INVOKEVIRTUAL,HANDLE,"type","()Ljava/lang/invoke/MethodType;");code.visitInsn(POP);
        code.visitVarInsn(ALOAD,0);code.visitLdcInsn(Type.getMethodType(signature));
        Type[] parameters=Type.getArgumentTypes(signature);code.visitLdcInsn(parameters.length);code.visitTypeInsn(ANEWARRAY,"java/lang/Object");
        int slot=1;
        for(int i=0;i<parameters.length;i++) {
            code.visitInsn(DUP);code.visitLdcInsn(i);code.visitVarInsn(parameters[i].getOpcode(ILOAD),slot);slot+=parameters[i].getSize();
            adapt(code,parameters[i],Type.getType(Object.class));code.visitInsn(AASTORE);
        }
        code.visitInsn(exact?ICONST_1:ICONST_0);call(code,INVOKEVIRTUAL,HANDLE,"javawCall","(Ljava/lang/invoke/MethodType;"+OBJECTS+"Z)Ljava/lang/Object;");
        Type result=Type.getReturnType(signature);
        if(result.equals(Type.VOID_TYPE))code.visitInsn(POP);else unbox(code,result);
        code.visitInsn(result.getOpcode(IRETURN));finish(code,slot);
    }
}
