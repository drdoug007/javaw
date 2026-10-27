package one.dastec.javaw.bytecode;

import org.objectweb.asm.tree.*;
import java.util.*;

/** Lazy, interned UTF-16 constants for the isolated exception runtime. No fixed data/heap overlap. */
public final class BytecodeStrings {
    private BytecodeStrings() {}
    public static String symbol(String value) {
        StringBuilder name=new StringBuilder("$__text");
        for(int i=0;i<value.length();i++)name.append('_').append(Integer.toHexString(value.charAt(i)));
        return name.toString();
    }

    static String runtimeWat(Map<String,ClassNode> linked,BytecodeDispatch dispatch) {
        if(!BytecodeExceptionRuntime.enabled(linked))return "";
        Set<String> constants=new TreeSet<>();
        constants.add("/ by zero");
        constants.addAll(BytecodeClasses.strings(linked,dispatch));
        for(ClassNode type:linked.values()) {
            constants.add(type.name.replace('/','.'));
            if(type.sourceFile!=null)constants.add(type.sourceFile);
            for(FieldNode field:type.fields) if(field.value instanceof String text)constants.add(text);
            for(MethodNode method:type.methods) {
                constants.add(method.name);
                for(AbstractInsnNode instruction:method.instructions)
                    if(instruction instanceof LdcInsnNode ldc && ldc.cst instanceof String text)constants.add(text);
            }
        }
        StringBuilder wat=new StringBuilder();
        int size=new BytecodeObjectLayouts(linked).sizes().get("java/lang/String");
        for(String value:constants) {
            String function=symbol(value),global=function+"_cached";
            wat.append("(global ").append(global).append(" (mut i32) (i32.const 0))\n");
            wat.append("(func ").append(function).append(" (result i32) (local $chars i32) (local $text i32)\n");
            wat.append("global.get ").append(global).append(" if global.get ").append(global).append(" return end\n");
            wat.append("i32.const ").append(value.length()).append(" i32.const -5 i32.const 2 call $__new_primitive_array local.set $chars\n");
            for(int i=0;i<value.length();i++)
                wat.append("local.get $chars i32.const ").append(value.charAt(i)+0).append(" i32.store16 offset=").append(8+i*2).append('\n');
            wat.append("i32.const ").append(size).append(" call $__heap_alloc local.tee $text i32.eqz if ")
                    .append(dispatch.implicitFailureWat(BytecodeExceptions.Implicit.OUT_OF_MEMORY)).append(" end\n");
            wat.append("local.get $text i32.const 0 i32.const ").append(size).append(" memory.fill\nlocal.get $text i32.const ")
                    .append(dispatch.classId("java/lang/String")).append(" i32.store\nlocal.get $text local.get $chars call ")
                    .append(TypeHelper.getMethodFunctionName("java/lang/String","<init>","([C)V")).append('\n');
            wat.append("local.get $text call ").append(TypeHelper.getMethodFunctionName("java/lang/String","intern","()Ljava/lang/String;"))
                    .append(" local.set $text\n");
            wat.append("local.get $text global.set ").append(global).append(" local.get $text)\n");
        }
        wat.append("(func $__exception_class_name (param $value i32) (result i32)\n");
        for(String name:new TreeSet<>(linked.keySet()))
            wat.append("local.get $value i32.load i32.const ").append(dispatch.classId(name))
                    .append(" i32.eq if call ").append(symbol(name.replace('/','.'))).append(" return end\n");
        return wat.append("unreachable)\n").toString();
    }
}
