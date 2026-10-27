package one.dastec.javaw.bytecode;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;

/** Exact System property entry points backed by isolated module state. */
final class BytecodeSystemProperties implements Opcodes {
    private static final String SYSTEM="java/lang/System", STORE="javaw/runtime/SystemProperties";
    private static final Map<String,Set<String>> SIGNATURES=Map.of(
        "getProperty",Set.of("(Ljava/lang/String;)Ljava/lang/String;","(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
        "setProperty",Set.of("(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
        "clearProperty",Set.of("(Ljava/lang/String;)Ljava/lang/String;"));

    static void normalize(List<ClassNode> classes) {
        if(classes.stream().anyMatch(c->c.name.equals(SYSTEM)))return;
        for(ClassNode type:classes)for(MethodNode method:type.methods)for(AbstractInsnNode insn:method.instructions.toArray()) {
            if(insn instanceof MethodInsnNode call && selected(call.owner,call.name)) {
                require(type,method,insn,call.name,call.desc,call.getOpcode()==INVOKESTATIC && !call.itf);
                call.owner=STORE;
            } else if(insn instanceof LdcInsnNode constant)constant.cst=rewrite(type,method,insn,constant.cst);
            else if(insn instanceof InvokeDynamicInsnNode dynamic) {
                dynamic.bsm=(Handle)rewrite(type,method,insn,dynamic.bsm);
                for(int i=0;i<dynamic.bsmArgs.length;i++)dynamic.bsmArgs[i]=rewrite(type,method,insn,dynamic.bsmArgs[i]);
            }
        }
    }
    private static boolean selected(String owner,String name){return owner.equals(SYSTEM) && SIGNATURES.containsKey(name);}
    private static void require(ClassNode type,MethodNode method,AbstractInsnNode insn,String name,String desc,boolean validKind) {
        if(!validKind || !SIGNATURES.get(name).contains(desc))throw BytecodeCompatibility.failure(type,method,insn,
            "System property invocation","use an exact standard static System."+name+" descriptor; the isolated runtime supports string property reads, writes and removal");
    }
    private static Object rewrite(ClassNode type,MethodNode method,AbstractInsnNode insn,Object value) {
        if(value instanceof Handle handle && selected(handle.getOwner(),handle.getName())) {
            require(type,method,insn,handle.getName(),handle.getDesc(),handle.getTag()==H_INVOKESTATIC && !handle.isInterface());
            return new Handle(H_INVOKESTATIC,STORE,handle.getName(),handle.getDesc(),false);
        }
        if(value instanceof ConstantDynamic dynamic) {
            Object[] args=new Object[dynamic.getBootstrapMethodArgumentCount()];
            for(int i=0;i<args.length;i++)args[i]=rewrite(type,method,insn,dynamic.getBootstrapMethodArgument(i));
            return new ConstantDynamic(dynamic.getName(),dynamic.getDescriptor(),(Handle)rewrite(type,method,insn,dynamic.getBootstrapMethod()),args);
        }
        return value;
    }
}
