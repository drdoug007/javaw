package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.List;
import java.util.Set;

/** Explicit signature-polymorphic adapters for the selected single-threaded runtime. */
final class BytecodeInvokes {
    private static final Set<String> MODES=Set.of("withInvokeExactBehavior","withInvokeBehavior","hasInvokeExactBehavior","varType");
    static void normalize(List<ClassNode> classes) {
        for(ClassNode type:classes)for(MethodNode method:type.methods)for(AbstractInsnNode instruction:method.instructions) {
            if(!(instruction instanceof MethodInsnNode call))continue;
            if(!type.name.equals("java/lang/invoke/VarHandle") && call.owner.equals("java/lang/invoke/VarHandle") && !MODES.contains(call.name) && !call.name.startsWith("javaw") && !call.name.equals("<init>")) {
                if(!BytecodeDeclarations.validDescriptor(call.desc,true))
                    throw BytecodeCompatibility.failure(type,method,instruction,"VarHandle descriptor", "provide a valid method descriptor");
                boolean read=call.name.equals("get"),write=call.name.equals("set");
                String coordinate=call.desc.startsWith("([BI")?"[B":call.desc.startsWith("(Ljava/lang/Object;I")?"Ljava/lang/Object;":null;
                String primitive=read?call.desc.substring(call.desc.indexOf(')')+1):coordinate==null?"":call.desc.substring(1+coordinate.length()+1,call.desc.indexOf(')'));
                String names="SCIJFD";int index=names.indexOf(primitive);
                if(call.getOpcode()!=Opcodes.INVOKEVIRTUAL || call.itf || coordinate==null || primitive.length()!=1 || index<0 ||
                        !call.desc.equals("("+coordinate+"I"+(write?primitive:"")+")"+(write?"V":primitive)) || !read&&!write)
                    throw BytecodeCompatibility.failure(type,method,instruction,"VarHandle access "+call.name+call.desc,
                            "only plain byte-array-view get/set with int offsets and short/char/int/long/float/double values are supported; ordered/atomic modes require the concurrency runtime");
                call.name="javaw"+(read?"Get":"Set")+new String[]{"Short","Char","Int","Long","Float","Double"}[index];
            }
            if(call.owner.equals("java/lang/invoke/MethodHandles") && !call.name.equals("byteArrayViewVarHandle") && !call.name.equals("<init>") ||
                    call.owner.equals("java/lang/invoke/MethodHandle") || call.owner.equals("java/lang/invoke/MethodType") ||
                    call.owner.equals("java/lang/reflect/Proxy") || call.owner.equals("java/lang/invoke/MethodHandles$Lookup") ||
                    (call.name.equals("defineClass") || call.name.equals("defineHiddenClass") || call.name.equals("defineAnonymousClass")) &&
                            (call.owner.contains("ClassLoader") || call.owner.contains("Unsafe")))
                throw BytecodeCompatibility.failure(type,method,instruction,"dynamic invocation/generation "+call.owner+"."+call.name+call.desc,
                        "supply a tested closed-world AOT equivalent; general method-handle lookup/invocation, dynamic proxies and runtime class definition are not implemented");
        }
    }
}
