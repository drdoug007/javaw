package one.dastec.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.*;

/** Bounded module-local shadow stack: no allocation on call entry or exceptional unwind. */
final class BytecodeTraces {
    static final int CAPACITY=256;
    private record Site(ClassNode owner,MethodNode method) {}
    private BytecodeTraces() {}

    static boolean instrument(ClassNode type,MethodNode method,Map<String,ClassNode> linked) {
        if(!BytecodeExceptionRuntime.enabled(linked) || type.name.startsWith("java/lang/") || type.name.startsWith("javaw/runtime/")
                || (!method.name.equals("<clinit>") && (method.access & (Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))!=0))return false;
        if(method.name.equals("<init>")) {
            ClassNode parent=type;Set<String> visited=new HashSet<>();
            while(parent!=null && visited.add(parent.name)) {
                if(parent.name.equals("java/lang/Throwable"))return false;
                parent=linked.get(parent.superName);
            }
        }
        return true;
    }

    private static List<Site> sites(Map<String,ClassNode> linked) {
        return BytecodeAnalysisCache.get(linked,"trace-sites",()->collectSites(linked));
    }

    private static List<Site> collectSites(Map<String,ClassNode> linked) {
        List<Site> sites=new ArrayList<>();
        for(String name:new TreeSet<>(linked.keySet())) {
            ClassNode owner=linked.get(name);
            owner.methods.stream().filter(method->instrument(owner,method,linked))
                    .sorted(Comparator.comparing(method->method.name+method.desc)).forEach(method->sites.add(new Site(owner,method)));
        }
        return sites;
    }

    static int id(ClassNode owner,MethodNode method,Map<String,ClassNode> linked) {
        Map<MethodNode,Integer> ids=BytecodeAnalysisCache.get(linked,"trace-ids",()->{
            Map<MethodNode,Integer> result=new IdentityHashMap<>();List<Site> sites=sites(linked);
            for(int i=0;i<sites.size();i++)result.putIfAbsent(sites.get(i).method,i);
            return result;
        });
        Integer id=ids.get(method);
        if(id==null || sites(linked).get(id).owner!=owner)throw new IllegalArgumentException("Unregistered trace method");
        return id;
    }

    static int emergencyAddress(BytecodeExceptions.Implicit kind,Map<String,ClassNode> linked) {
        if(kind==BytecodeExceptions.Implicit.OUT_OF_MEMORY)return 8;
        return 8+BytecodeObjectLayouts.of(linked).sizes().getOrDefault("java/lang/OutOfMemoryError",0);
    }

    static int stackBase(Map<String,ClassNode> linked) {
        var sizes=BytecodeObjectLayouts.of(linked).sizes();
        int end=8+sizes.getOrDefault("java/lang/OutOfMemoryError",0)+sizes.getOrDefault("java/lang/StackOverflowError",0);
        return (end+7)&~7;
    }

    static String runtimeWat(Map<String,ClassNode> linked,BytecodeDispatch dispatch) {
        if(!BytecodeExceptionRuntime.enabled(linked))return "";
        int base=stackBase(linked),size=BytecodeObjectLayouts.of(linked).sizes().get("java/lang/StackTraceElement");
        StringBuilder wat=new StringBuilder();
        wat.append("(global $__trace_depth (mut i32) (i32.const 0))\n")
           .append("(func (export \"__reset_exception_trace\") i32.const 0 global.set $__trace_depth)\n")
           .append("(func $__trace_enter (param $id i32) global.get $__trace_depth i32.const ").append(CAPACITY)
           .append(" i32.ge_u if ").append(dispatch.implicitFailureWat(BytecodeExceptions.Implicit.STACK_OVERFLOW)).append(" end\n")
           .append("i32.const ").append(base).append(" global.get $__trace_depth i32.const 8 i32.mul i32.add local.get $id i32.store\n")
           .append("i32.const ").append(base+4).append(" global.get $__trace_depth i32.const 8 i32.mul i32.add i32.const -1 i32.store\n")
           .append("global.get $__trace_depth i32.const 1 i32.add global.set $__trace_depth)\n")
           .append("(func $__trace_exit global.get $__trace_depth i32.const 1 i32.sub global.set $__trace_depth)\n")
           .append("(func $__trace_line (param $line i32) i32.const ").append(base+4)
           .append(" global.get $__trace_depth i32.const 1 i32.sub i32.const 8 i32.mul i32.add local.get $line i32.store)\n")
           .append("(func $__trace_capture (result i32) (local $array i32) (local $i i32) (local $frame i32) (local $entry i32)\n")
           .append("global.get $__trace_depth i32.const ").append(dispatch.arrayId("[Ljava/lang/StackTraceElement;"))
           .append(" i32.const 4 call $__new_primitive_array local.set $array\n")
           .append("block $done loop $next local.get $i global.get $__trace_depth i32.ge_u br_if $done\n")
           .append("i32.const ").append(base).append(" global.get $__trace_depth i32.const 1 i32.sub local.get $i i32.sub i32.const 8 i32.mul i32.add local.set $frame\n")
           .append("i32.const ").append(size).append(" call $__heap_alloc local.tee $entry i32.eqz if ")
           .append(dispatch.implicitFailureWat(BytecodeExceptions.Implicit.OUT_OF_MEMORY)).append(" end\n")
           .append("local.get $entry i32.const 0 i32.const ").append(size).append(" memory.fill\nlocal.get $entry i32.const ")
           .append(dispatch.classId("java/lang/StackTraceElement")).append(" i32.store\n");
        List<Site> sites=sites(linked);
        for(int i=0;i<sites.size();i++) {
            Site site=sites.get(i);
            wat.append("local.get $frame i32.load i32.const ").append(i).append(" i32.eq if\nlocal.get $entry call ")
               .append(BytecodeStrings.symbol(site.owner.name.replace('/','.'))).append(" call ")
               .append(BytecodeStrings.symbol(site.method.name)).append(' ');
            if(site.owner.sourceFile==null)wat.append("i32.const 0 ");
            else wat.append("call ").append(BytecodeStrings.symbol(site.owner.sourceFile)).append(' ');
            wat.append("local.get $frame i32.load offset=4 call ")
               .append(TypeHelper.getMethodFunctionName("java/lang/StackTraceElement","<init>","(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V"))
               .append(" end\n");
        }
        return wat.append("local.get $array i32.const 8 i32.add local.get $i i32.const 4 i32.mul i32.add local.get $entry i32.store\n")
                .append("local.get $i i32.const 1 i32.add local.set $i br $next end end local.get $array)\n").toString();
    }
}
