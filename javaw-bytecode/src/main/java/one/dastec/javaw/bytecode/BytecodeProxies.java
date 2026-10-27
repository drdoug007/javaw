package one.dastec.javaw.bytecode;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;
import static one.dastec.javaw.bytecode.BytecodeDynamic.*;

/** Explicit interface shapes are generated at build time; a closed registry replaces Proxy factories. */
final class BytecodeProxies implements Opcodes {
    private static final String PROXY="java/lang/reflect/Proxy", METHOD="java/lang/reflect/Method", HANDLER="java/lang/reflect/InvocationHandler";
    private record Member(String owner,MethodNode method){}
    static void normalize(List<ClassNode> classes,List<List<String>> shapes) {
        Map<String,ClassNode> linked=new LinkedHashMap<>();for(ClassNode c:classes){BytecodeDeclarations.validate(c);linked.put(c.name,c);}
        if(shapes.isEmpty())for(ClassNode c:classes)if(!c.name.equals(PROXY))for(MethodNode m:c.methods)for(AbstractInsnNode i:m.instructions)
            if(i instanceof MethodInsnNode call && call.owner.equals(PROXY) && (call.name.equals("newProxyInstance") || call.name.equals("getProxyClass")))
                throw BytecodeCompatibility.failure(c,m,i,"runtime proxy generation","register an ordered public-interface shape through the AOT proxy compiler API; Wasm cannot define proxy classes at runtime");
        List<String> names=new ArrayList<>();List<List<String>> selected=new ArrayList<>();
        Set<String> seen=new HashSet<>();
        for(List<String> shape:shapes) {
            List<String> interfaces=shape.stream().map(n->n.replace('.','/')).toList();
            if(new HashSet<>(interfaces).size()!=interfaces.size() || interfaces.isEmpty())throw new IllegalArgumentException("AOT proxy shapes require distinct public interfaces");
            if(!seen.add(String.join(",",interfaces)))continue;
            for(String name:interfaces){ClassNode type=linked.get(name);if(type==null || (type.access&(ACC_PUBLIC|ACC_INTERFACE))!=(ACC_PUBLIC|ACC_INTERFACE) || type.permittedSubclasses!=null && !type.permittedSubclasses.isEmpty())throw new IllegalArgumentException("AOT proxy interface must be linked, public and unsealed: "+name);}
            String name="javaw/aot/Proxy"+names.size();if(linked.containsKey(name))throw new IllegalArgumentException("reserved proxy class collision");
            names.add(name);selected.add(interfaces);classes.add(proxy(name,interfaces,linked));
        }
        ClassNode runtime=linked.get(PROXY);
        MethodNode isProxy=runtime.methods.stream().filter(m->m.name.equals("isProxyClass")).findFirst().orElseThrow();isProxy.instructions.clear();isProxy.tryCatchBlocks.clear();
        isProxy.visitVarInsn(ALOAD,0);call(isProxy,INVOKEVIRTUAL,"java/lang/Object","getClass","()Ljava/lang/Class;");isProxy.visitInsn(POP);
        for(String name:names){Label next=new Label();isProxy.visitVarInsn(ALOAD,0);isProxy.visitLdcInsn(Type.getObjectType(name));isProxy.visitJumpInsn(IF_ACMPNE,next);isProxy.visitInsn(ICONST_1);isProxy.visitInsn(IRETURN);isProxy.visitLabel(next);isProxy.visitFrame(F_SAME,0,null,0,null);}
        isProxy.visitInsn(ICONST_0);isProxy.visitInsn(IRETURN);finish(isProxy,1);
        factory(runtime,"newProxyInstance",names,selected,true);
        factory(runtime,"getProxyClass",names,selected,false);
    }
    private static void factory(ClassNode runtime,String method,List<String> names,List<List<String>> selected,boolean instance) {
        MethodNode create=runtime.methods.stream().filter(m->m.name.equals(method)).findFirst().orElseThrow();create.instructions.clear();create.tryCatchBlocks.clear();
        // The selected target has one application loader plus the bootstrap/null identity.
        Label loaderOk=new Label();create.visitVarInsn(ALOAD,0);create.visitJumpInsn(IFNULL,loaderOk);
        create.visitVarInsn(ALOAD,0);call(create,INVOKESTATIC,"java/lang/ClassLoader","javawApplication","()Ljava/lang/ClassLoader;");create.visitJumpInsn(IF_ACMPEQ,loaderOk);
        fail(create,"java/lang/IllegalArgumentException");create.visitLabel(loaderOk);create.visitFrame(F_SAME,0,null,0,null);
        create.visitVarInsn(ALOAD,1);call(create,INVOKESTATIC,PROXY,"javawValidate","([Ljava/lang/Class;)V");
        if(instance){create.visitVarInsn(ALOAD,2);call(create,INVOKEVIRTUAL,"java/lang/Object","getClass","()Ljava/lang/Class;");create.visitInsn(POP);}
        for(int index=0;index<names.size();index++) {
            Label next=new Label();List<String> shape=selected.get(index);
            if(shape.stream().anyMatch(n->!n.startsWith("java/"))){create.visitVarInsn(ALOAD,0);create.visitJumpInsn(IFNULL,next);}
            create.visitVarInsn(ALOAD,1);create.visitInsn(ARRAYLENGTH);create.visitLdcInsn(shape.size());create.visitJumpInsn(IF_ICMPNE,next);
            for(int i=0;i<shape.size();i++){create.visitVarInsn(ALOAD,1);create.visitLdcInsn(i);create.visitInsn(AALOAD);create.visitLdcInsn(Type.getObjectType(shape.get(i)));create.visitJumpInsn(IF_ACMPNE,next);}
            if(instance){create.visitTypeInsn(NEW,names.get(index));create.visitInsn(DUP);create.visitVarInsn(ALOAD,2);call(create,INVOKESPECIAL,names.get(index),"<init>","(L"+HANDLER+";)V");}
            else create.visitLdcInsn(Type.getObjectType(names.get(index)));
            create.visitInsn(ARETURN);
            create.visitLabel(next);create.visitFrame(F_SAME,0,null,0,null);
        }
        fail(create,"java/lang/IllegalArgumentException");finish(create,instance?3:2);
    }
    private static ClassNode proxy(String name,List<String> interfaces,Map<String,ClassNode> linked) {
        ClassNode type=type(name);type.superName=PROXY;type.interfaces.addAll(interfaces);
        MethodNode init=method(type,ACC_PUBLIC,"<init>","(L"+HANDLER+";)V");init.visitVarInsn(ALOAD,0);init.visitVarInsn(ALOAD,1);call(init,INVOKESPECIAL,PROXY,"<init>","(L"+HANDLER+";)V");init.visitInsn(RETURN);finish(init,2);
        Map<String,List<Member>> members=new LinkedHashMap<>();
        for(String[] object:new String[][]{{"equals","(Ljava/lang/Object;)Z"},{"hashCode","()I"},{"toString","()Ljava/lang/String;"}})
            members.put(object[0]+object[1],new ArrayList<>(List.of(new Member("java/lang/Object",new MethodNode(ACC_PUBLIC,object[0],object[1],null,null)))));
        for(String iface:interfaces)collect(iface,linked,members,new HashSet<>());
        Map<String,String> returns=new HashMap<>();
        for(List<Member> declarations:members.values()) {
            MethodNode method=declarations.get(0).method;
            String key=method.name+method.desc.substring(0,method.desc.indexOf(')')+1);
            String previous=returns.putIfAbsent(key,Type.getReturnType(method.desc).getDescriptor());
            if(previous!=null && !previous.equals(Type.getReturnType(method.desc).getDescriptor()))
                throw new IllegalArgumentException("AOT proxy duplicate method return descriptors require an explicit adapter: "+key);
        }
        int ordinal=0;
        for(List<Member> declarations:members.values()) {
            Member first=declarations.get(0);MethodNode declared=first.method;String getter="$method$"+ordinal++,field=getter+"Value";
            type.fields.add(new FieldNode(ACC_PRIVATE|ACC_STATIC,field,"L"+METHOD+";",null,null));
            MethodNode metadata=method(type,ACC_PRIVATE|ACC_STATIC,getter,"()L"+METHOD+";");Label ready=new Label();metadata.visitFieldInsn(GETSTATIC,name,field,"L"+METHOD+";");metadata.visitInsn(DUP);metadata.visitJumpInsn(IFNONNULL,ready);metadata.visitInsn(POP);
            metadata.visitTypeInsn(NEW,METHOD);metadata.visitInsn(DUP);metadata.visitLdcInsn(Type.getObjectType(first.owner));metadata.visitLdcInsn(declared.name);metadata.visitLdcInsn(Type.getMethodType(declared.desc));
            metadata.visitLdcInsn(new Handle(first.owner.equals("java/lang/Object")?H_INVOKEVIRTUAL:H_INVOKEINTERFACE,first.owner,declared.name,declared.desc,!first.owner.equals("java/lang/Object")));
            metadata.visitLdcInsn(declared.access);metadata.visitLdcInsn(declared.exceptions.size());metadata.visitTypeInsn(ANEWARRAY,"java/lang/Class");
            for(int i=0;i<declared.exceptions.size();i++){metadata.visitInsn(DUP);metadata.visitLdcInsn(i);metadata.visitLdcInsn(Type.getObjectType(declared.exceptions.get(i)));metadata.visitInsn(AASTORE);}
            call(metadata,INVOKESPECIAL,METHOD,"<init>","(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;I[Ljava/lang/Class;)V");
            metadata.visitInsn(DUP);metadata.visitFieldInsn(PUTSTATIC,name,field,"L"+METHOD+";");metadata.visitLabel(ready);metadata.visitFrame(F_SAME1,0,null,1,new Object[]{METHOD});metadata.visitInsn(ARETURN);finish(metadata,0);
            MethodNode invoke=method(type,ACC_PUBLIC|ACC_FINAL,declared.name,declared.desc);Type[] parameters=Type.getArgumentTypes(declared.desc);
            Label start=new Label(),end=new Label(),handler=new Label(),rethrow=new Label(),wrap=new Label();invoke.visitTryCatchBlock(start,end,handler,"java/lang/Throwable");
            invoke.visitLabel(start);invoke.visitVarInsn(ALOAD,0);invoke.visitFieldInsn(GETFIELD,PROXY,"h","L"+HANDLER+";");invoke.visitVarInsn(ALOAD,0);call(invoke,INVOKESTATIC,name,getter,"()L"+METHOD+";");
            int locals=1;
            if(parameters.length==0)invoke.visitInsn(ACONST_NULL);
            else {
                invoke.visitLdcInsn(parameters.length);invoke.visitTypeInsn(ANEWARRAY,"java/lang/Object");
                for(int i=0;i<parameters.length;i++){invoke.visitInsn(DUP);invoke.visitLdcInsn(i);invoke.visitVarInsn(parameters[i].getOpcode(ILOAD),locals);locals+=parameters[i].getSize();adapt(invoke,parameters[i],Type.getType(Object.class));invoke.visitInsn(AASTORE);}
            }
            invoke.visitMethodInsn(INVOKEINTERFACE,HANDLER,"invoke","(Ljava/lang/Object;L"+METHOD+";[Ljava/lang/Object;)Ljava/lang/Object;",true);
            Type result=Type.getReturnType(declared.desc);
            if(result.equals(Type.VOID_TYPE))invoke.visitInsn(POP);else BytecodeHandles.unbox(invoke,result);
            invoke.visitLabel(end);invoke.visitInsn(result.getOpcode(IRETURN));
            List<Object> frames=new ArrayList<>();frames.add(name);for(Type p:parameters)frames.add(frame(p));
            invoke.visitLabel(handler);invoke.visitFrame(F_FULL,frames.size(),frames.toArray(),1,new Object[]{"java/lang/Throwable"});invoke.visitVarInsn(ASTORE,locals);
            invoke.visitVarInsn(ALOAD,locals);invoke.visitTypeInsn(INSTANCEOF,"java/lang/RuntimeException");invoke.visitJumpInsn(IFNE,rethrow);
            invoke.visitVarInsn(ALOAD,locals);invoke.visitTypeInsn(INSTANCEOF,"java/lang/Error");invoke.visitJumpInsn(IFNE,rethrow);
            frames.add("java/lang/Throwable");
            for(Member declaration:declarations) {
                Label permitted=new Label();
                for(String exception:declaration.method.exceptions){invoke.visitVarInsn(ALOAD,locals);invoke.visitTypeInsn(INSTANCEOF,exception);invoke.visitJumpInsn(IFNE,permitted);}
                invoke.visitJumpInsn(GOTO,wrap);invoke.visitLabel(permitted);invoke.visitFrame(F_FULL,frames.size(),frames.toArray(),0,null);
            }
            invoke.visitJumpInsn(GOTO,rethrow);
            invoke.visitLabel(wrap);invoke.visitFrame(F_FULL,frames.size(),frames.toArray(),0,null);invoke.visitTypeInsn(NEW,"java/lang/reflect/UndeclaredThrowableException");invoke.visitInsn(DUP);invoke.visitVarInsn(ALOAD,locals);call(invoke,INVOKESPECIAL,"java/lang/reflect/UndeclaredThrowableException","<init>","(Ljava/lang/Throwable;)V");invoke.visitInsn(ATHROW);
            invoke.visitLabel(rethrow);invoke.visitFrame(F_FULL,frames.size(),frames.toArray(),0,null);invoke.visitVarInsn(ALOAD,locals);invoke.visitInsn(ATHROW);finish(invoke,locals+1);
        }
        return type;
    }
    private static Object frame(Type type){return switch(type.getSort()){case Type.OBJECT->type.getInternalName();case Type.ARRAY->type.getDescriptor();case Type.LONG->LONG;case Type.FLOAT->FLOAT;case Type.DOUBLE->DOUBLE;default->INTEGER;};}
    private static void collect(String owner,Map<String,ClassNode> linked,Map<String,List<Member>> members,Set<String> visited) {
        if(!visited.add(owner))return;ClassNode iface=linked.get(owner);
        if(iface==null)throw new IllegalArgumentException("link proxy parent interface "+owner);
        for(MethodNode method:iface.methods)if(!method.name.startsWith("<") && (method.access&(ACC_STATIC|ACC_PRIVATE))==0)members.computeIfAbsent(method.name+method.desc,k->new ArrayList<>()).add(new Member(owner,method));
        for(String parent:iface.interfaces)collect(parent,linked,members,visited);
    }
    private static void fail(MethodVisitor code,String type){code.visitTypeInsn(NEW,type);code.visitInsn(DUP);call(code,INVOKESPECIAL,type,"<init>","()V");code.visitInsn(ATHROW);}
}
