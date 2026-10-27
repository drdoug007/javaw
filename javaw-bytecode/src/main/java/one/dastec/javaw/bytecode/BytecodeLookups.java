package one.dastec.javaw.bytecode;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;
import static one.dastec.javaw.bytecode.BytecodeDynamic.*;

/** Lookup rights are compiled into per-caller closed-world resolvers, never a runtime class loader. */
final class BytecodeLookups implements Opcodes {
    private static final String LOOKUP="java/lang/invoke/MethodHandles$Lookup", HANDLE="Ljava/lang/invoke/MethodHandle;";
    private static final String FIND="(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;I)"+HANDLE;
    static void normalize(List<ClassNode> classes) {
        boolean needed=classes.stream().flatMap(c->c.methods.stream()).flatMap(m->Arrays.stream(m.instructions.toArray()))
            .anyMatch(i->i instanceof MethodInsnNode c && c.owner.equals("java/lang/invoke/MethodHandles") && (c.name.equals("lookup") || c.name.equals("privateLookupIn")));
        if(!needed)return;
        Map<String,ClassNode> linked=new LinkedHashMap<>();for(ClassNode c:classes){BytecodeDeclarations.validate(c);linked.put(c.name,c);}
        List<ClassNode> users=classes.stream().filter(c->!c.name.startsWith("java/") && !c.name.startsWith("javaw/runtime/") && !c.name.startsWith("javaw/aot/")).sorted(Comparator.comparing(c->c.name)).toList();
        Map<String,String> lookups=new LinkedHashMap<>();List<ClassNode> generated=new ArrayList<>();
        for(ClassNode caller:users) {
            String name=caller.name+"$$javaw$lookup";
            if(linked.containsKey(name))throw BytecodeCompatibility.failure(caller,null,null,"lookup adapter collision","rename reserved lookup adapter");
            lookups.put(caller.name,name);generated.add(resolver(caller,name,users,linked));
        }
        String providerName="javaw/runtime/PrivateLookups";
        if(linked.containsKey(providerName))throw new IllegalArgumentException("reserved private lookup provider collision");
        ClassNode provider=type(providerName);privateProvider(provider,lookups);generated.add(provider);
        for(ClassNode caller:classes)for(MethodNode method:List.copyOf(caller.methods))for(AbstractInsnNode instruction:method.instructions.toArray()) {
            if(!(instruction instanceof MethodInsnNode call) || !call.owner.equals("java/lang/invoke/MethodHandles"))continue;
            if(call.name.equals("lookup") && !caller.name.startsWith("java/")) {
                if(call.getOpcode()!=INVOKESTATIC || !call.desc.equals("()L"+LOOKUP+";"))throw BytecodeCompatibility.failure(caller,method,call,"lookup invocation","use the exact standard static descriptor");
                String adapter=lookups.get(caller.name);InsnList creation=new InsnList();
                creation.add(new TypeInsnNode(NEW,adapter));creation.add(new InsnNode(DUP));creation.add(new MethodInsnNode(INVOKESPECIAL,adapter,"<init>","()V",false));
                method.instructions.insertBefore(call,creation);method.instructions.remove(call);
            } else if(call.name.equals("privateLookupIn")) {
                if(call.getOpcode()!=INVOKESTATIC || !call.desc.equals("(Ljava/lang/Class;L"+LOOKUP+";)L"+LOOKUP+";"))throw BytecodeCompatibility.failure(caller,method,call,"private lookup invocation","use the exact standard static descriptor");
                call.owner=providerName;call.name="find";
            }
        }
        classes.addAll(generated);
    }
    private static ClassNode resolver(ClassNode caller,String name,List<ClassNode> users,Map<String,ClassNode> linked) {
        ClassNode adapter=type(name);adapter.superName=LOOKUP;adapter.sourceFile=caller.sourceFile;nest(adapter,caller,linked);
        MethodNode init=method(adapter,ACC_PUBLIC,"<init>","()V");init.visitVarInsn(ALOAD,0);init.visitLdcInsn(Type.getObjectType(caller.name));
        call(init,INVOKESPECIAL,LOOKUP,"<init>","(Ljava/lang/Class;)V");init.visitInsn(RETURN);finish(init,1);
        MethodNode special=method(adapter,ACC_PROTECTED,"javawSpecialLookup","(Ljava/lang/Class;)L"+LOOKUP+";");
        BytecodeDispatch specialAccess=new BytecodeDispatch(linked,caller.name);
        for(ClassNode target:users) {
            try{specialAccess.checkPrivateAccess(target.name);}catch(IllegalArgumentException denied){continue;}
            Label next=new Label();special.visitVarInsn(ALOAD,1);special.visitLdcInsn(Type.getObjectType(target.name));special.visitJumpInsn(IF_ACMPNE,next);
            special.visitTypeInsn(NEW,target.name+"$$javaw$lookup");special.visitInsn(DUP);call(special,INVOKESPECIAL,target.name+"$$javaw$lookup","<init>","()V");special.visitInsn(ARETURN);
            special.visitLabel(next);special.visitFrame(F_SAME,0,null,0,null);
        }
        fail(special,"java/lang/IllegalAccessException");finish(special,2);
        MethodNode find=method(adapter,ACC_PROTECTED,"javawFind",FIND);
        // Null arguments fail before search even when the closed-world registry has no matching class.
        for(int slot=1;slot<=3;slot++){find.visitVarInsn(ALOAD,slot);call(find,INVOKEVIRTUAL,"java/lang/Object","getClass","()Ljava/lang/Class;");find.visitInsn(POP);}
        BytecodeDispatch dispatch=new BytecodeDispatch(linked,caller.name);
        for(ClassNode target:users) {
            Map<String,MethodNode> candidates=new TreeMap<>();members(target,linked,candidates,new HashSet<>());
            for(MethodNode candidate:candidates.values())for(int kind:new int[]{H_INVOKESTATIC,H_INVOKEVIRTUAL,H_INVOKESPECIAL,H_NEWINVOKESPECIAL}) {
                boolean constructor=candidate.name.equals("<init>");
                if(candidate.name.equals("<clinit>") || constructor!=(kind==H_NEWINVOKESPECIAL))continue;
                if(constructor && !target.methods.contains(candidate))continue;
                boolean iface=(target.access&ACC_INTERFACE)!=0;
                int opcode=kind==H_INVOKESTATIC?INVOKESTATIC:kind==H_INVOKEVIRTUAL?(iface?INVOKEINTERFACE:INVOKEVIRTUAL):INVOKESPECIAL;
                boolean allowed=true;
                try {
                    MethodInsnNode call=new MethodInsnNode(opcode,target.name,candidate.name,candidate.desc,iface);
                    if(kind==H_INVOKESTATIC)dispatch.staticTarget(call);
                    else if(opcode==INVOKESPECIAL){if(iface)dispatch.interfaceSuperTarget(call);else dispatch.specialTarget(call);}
                    else {if((candidate.access&ACC_STATIC)!=0)throw new IllegalArgumentException();dispatch.targets(call);}
                }catch(IllegalArgumentException error){allowed=false;}
                Label next=new Label();
                find.visitVarInsn(ILOAD,4);find.visitLdcInsn(kind);find.visitJumpInsn(IF_ICMPNE,next);
                // Name comparison avoids granting the resolver access to a private symbolic owner.
                find.visitVarInsn(ALOAD,1);call(find,INVOKEVIRTUAL,"java/lang/Class","getName","()Ljava/lang/String;");find.visitLdcInsn(target.name.replace('/','.'));
                call(find,INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");find.visitJumpInsn(IFEQ,next);
                find.visitVarInsn(ALOAD,2);find.visitLdcInsn(candidate.name);call(find,INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");find.visitJumpInsn(IFEQ,next);
                if(allowed) {
                    find.visitVarInsn(ALOAD,3);find.visitLdcInsn(Type.getMethodType(candidate.desc));
                    call(find,INVOKEVIRTUAL,"java/lang/invoke/MethodType","equals","(Ljava/lang/Object;)Z");find.visitJumpInsn(IFEQ,next);
                    if((candidate.access&ACC_VARARGS)!=0 || constructor && (target.access&ACC_ABSTRACT)!=0) {
                        fail(find,"java/lang/UnsupportedOperationException");
                        find.visitLabel(next);find.visitFrame(F_SAME,0,null,0,null);continue;
                    }
                    String factory="$javaw$lookup$"+caller.methods.size();
                    if(caller.methods.stream().anyMatch(m->m.name.equals(factory)))throw new IllegalArgumentException("reserved lookup factory collision");
                    if((caller.access&ACC_INTERFACE)!=0 && (caller.version&0xffff)<V1_8)caller.version=V1_8;
                    MethodNode get=method(caller,ACC_PUBLIC|ACC_STATIC|ACC_SYNTHETIC,factory,"()"+HANDLE);
                    get.visitLdcInsn(new Handle(kind==H_INVOKEVIRTUAL && iface?H_INVOKEINTERFACE:kind,target.name,candidate.name,candidate.desc,iface));get.visitInsn(ARETURN);finish(get,0);
                    find.visitMethodInsn(INVOKESTATIC,caller.name,factory,"()"+HANDLE,(caller.access&ACC_INTERFACE)!=0);find.visitInsn(ARETURN);
                } else {
                    // Descriptor comparison uses text, so inaccessible signature classes are never resolved here.
                    find.visitVarInsn(ALOAD,3);call(find,INVOKEVIRTUAL,"java/lang/invoke/MethodType","toMethodDescriptorString","()Ljava/lang/String;");find.visitLdcInsn(candidate.desc);
                    call(find,INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");find.visitJumpInsn(IFEQ,next);fail(find,"java/lang/IllegalAccessException");
                }
                find.visitLabel(next);find.visitFrame(F_SAME,0,null,0,null);
            }
        }
        fail(find,"java/lang/NoSuchMethodException");finish(find,5);return adapter;
    }
    private static void members(ClassNode type,Map<String,ClassNode> linked,Map<String,MethodNode> out,Set<String> visited) {
        if(type==null || !visited.add(type.name))return;
        for(MethodNode method:List.copyOf(type.methods))if(!method.name.startsWith("$javaw$"))out.putIfAbsent(method.name+method.desc,method);
        members(linked.get(type.superName),linked,out,visited);for(String iface:type.interfaces)members(linked.get(iface),linked,out,visited);
    }
    private static void privateProvider(ClassNode owner,Map<String,String> lookups) {
        MethodNode code=method(owner,ACC_PUBLIC|ACC_STATIC,"find","(Ljava/lang/Class;L"+LOOKUP+";)L"+LOOKUP+";");
        code.visitVarInsn(ALOAD,1);call(code,INVOKEVIRTUAL,LOOKUP,"lookupClass","()Ljava/lang/Class;");code.visitInsn(POP);
        Label valid=new Label();code.visitVarInsn(ALOAD,0);call(code,INVOKEVIRTUAL,"java/lang/Class","isPrimitive","()Z");code.visitJumpInsn(IFNE,valid);
        code.visitVarInsn(ALOAD,0);call(code,INVOKEVIRTUAL,"java/lang/Class","isArray","()Z");Label search=new Label();code.visitJumpInsn(IFEQ,search);
        code.visitLabel(valid);code.visitFrame(F_SAME,0,null,0,null);fail(code,"java/lang/IllegalArgumentException");
        code.visitLabel(search);code.visitFrame(F_SAME,0,null,0,null);
        for(var entry:lookups.entrySet()) {
            Label next=new Label();code.visitVarInsn(ALOAD,0);call(code,INVOKEVIRTUAL,"java/lang/Class","getName","()Ljava/lang/String;");code.visitLdcInsn(entry.getKey().replace('/','.'));
            call(code,INVOKEVIRTUAL,"java/lang/String","equals","(Ljava/lang/Object;)Z");code.visitJumpInsn(IFEQ,next);
            code.visitTypeInsn(NEW,entry.getValue());code.visitInsn(DUP);call(code,INVOKESPECIAL,entry.getValue(),"<init>","()V");code.visitInsn(ARETURN);
            code.visitLabel(next);code.visitFrame(F_SAME,0,null,0,null);
        }
        fail(code,"java/lang/IllegalAccessException");finish(code,2);
    }
    private static void fail(MethodVisitor code,String type){code.visitTypeInsn(NEW,type);code.visitInsn(DUP);call(code,INVOKESPECIAL,type,"<init>","()V");code.visitInsn(ATHROW);}
}
