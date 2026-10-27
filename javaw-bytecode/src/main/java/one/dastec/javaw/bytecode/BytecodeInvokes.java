package one.dastec.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

/** Explicit signature-polymorphic adapters for the selected single-threaded runtime. */
final class BytecodeInvokes {
    private static final Set<String> MODES=Set.of("withInvokeExactBehavior","withInvokeBehavior","hasInvokeExactBehavior","varType");
    static void normalize(List<ClassNode> classes) {
        Map<String,ClassNode> linked=new HashMap<>();for(ClassNode type:classes)linked.put(type.name,type);
        for(ClassNode type:classes)for(MethodNode method:type.methods)for(AbstractInsnNode instruction:method.instructions) {
            if(!(instruction instanceof MethodInsnNode call))continue;
            String generation=generation(call,linked);
            if(generation!=null)throw BytecodeCompatibility.failure(type,method,instruction,generation+" "+call.owner+"."+call.name+call.desc,
                    "generate and package classes on the build JVM, then link the resulting class files; runtime generation/loading is excluded from the Wasm target");
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
            if(!type.name.startsWith("java/lang/invoke/") && (call.owner.equals("java/lang/invoke/MethodHandle") && !Set.of("<init>","type","bindTo","asType","invokeWithArguments","isVarargsCollector","asFixedArity","toString").contains(call.name) && !call.name.startsWith("javaw") ||
                    call.owner.equals("java/lang/invoke/MethodHandles$Lookup") && !Set.of("<init>","lookupClass","hasFullPrivilegeAccess","findStatic","findVirtual","findSpecial","findConstructor").contains(call.name) && !call.name.startsWith("javaw")))
                throw BytecodeCompatibility.failure(type,method,instruction,"closed-world handle API "+call.owner+"."+call.name+call.desc,
                        "use a linked fixed-arity target and a supported lookup/invocation adapter; arbitrary combinators, field lookup, unreflect and lookup modes require an explicit AOT wrapper");
            if(call.owner.equals("java/lang/invoke/MethodHandles") && !call.name.equals("byteArrayViewVarHandle") && !call.name.equals("<init>") ||
                    (call.name.equals("defineClass") || call.name.equals("defineHiddenClass") || call.name.equals("defineAnonymousClass")) &&
                            (call.owner.contains("ClassLoader") || call.owner.contains("Unsafe")))
                throw BytecodeCompatibility.failure(type,method,instruction,"dynamic invocation/generation "+call.owner+"."+call.name+call.desc,
                        "supply a tested closed-world AOT equivalent; this handle factory is outside the supported closed-world API");
        }
    }
    private static String generation(MethodInsnNode call,Map<String,ClassNode> linked) {
        if(call.name.startsWith("define") && (call.owner.contains("Unsafe") || call.owner.equals("java/lang/invoke/MethodHandles$Lookup")))return "runtime class definition";
        if(call.name.equals("defineClass")) {
            String current=call.owner;Set<String> visited=new HashSet<>();
            while(current!=null && visited.add(current)) {
                if(current.equals("java/lang/ClassLoader") || current.equals("java/security/SecureClassLoader"))return "runtime class definition";
                ClassNode type=linked.get(current);current=type==null?null:type.superName;
            }
        }
        if(call.owner.contains("cglib/") && (call.name.startsWith("create") || call.name.startsWith("generate") || call.name.equals("defineClass")))return "runtime CGLIB generation";
        if(call.owner.startsWith("net/bytebuddy/dynamic/DynamicType$Builder") && call.name.equals("make") ||
                call.owner.startsWith("net/bytebuddy/dynamic/DynamicType$Unloaded") && call.name.equals("load") ||
                call.owner.startsWith("net/bytebuddy/dynamic/loading/") && (call.name.equals("load") || call.name.equals("inject") || call.name.startsWith("defineClass")))return "runtime Byte Buddy generation";
        if(call.owner.startsWith("org/hibernate/bytecode/internal/bytebuddy/ByteBuddyState") && (call.name.startsWith("load") || call.name.equals("make")) ||
                call.owner.equals("org/hibernate/proxy/pojo/bytebuddy/ByteBuddyProxyHelper") && call.name.startsWith("build"))return "runtime Hibernate proxy generation";
        return null;
    }

}
