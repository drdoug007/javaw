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
        int size=BytecodeObjectLayouts.of(linked).sizes().get("java/lang/String");
        for(String value:constants) {
            String function=symbol(value),global=function+"_cached";
            wat.append("(global ").append(global).append(" (mut i32) (i32.const 0))\n");
            wat.append("(func ").append(function).append(" (result i32) (local $chars i32) (local $text i32)\n");
            wat.append("global.get ").append(global).append(" if global.get ").append(global).append(" return end\n");
            // The character stores come later, from charactersWat, and only for constants that survive pruning.
            wat.append("i32.const ").append(value.length()).append(CHARS);
            wat.append("i32.const ").append(size).append(" call $__heap_alloc local.tee $text i32.eqz if ")
                    .append(dispatch.implicitFailureWat(BytecodeExceptions.Implicit.OUT_OF_MEMORY)).append(" end\n");
            wat.append("local.get $text i32.const 0 i32.const ").append(size).append(" memory.fill\nlocal.get $text i32.const ")
                    .append(dispatch.classId("java/lang/String")).append(" i32.store\nlocal.get $text local.get $chars call ")
                    .append(TypeHelper.getMethodFunctionName("java/lang/String","<init>","([C)V")).append('\n');
            wat.append("local.get $text call ").append(TypeHelper.getMethodFunctionName("java/lang/String","intern","()Ljava/lang/String;"))
                    .append(" local.set $text\n");
            wat.append("local.get $text global.set ").append(global).append(" local.get $text)\n");
        }
        return wat.append(classNameWat(linked,dispatch,name->true)).toString();
    }

    /** Ends the line that allocates a constant's char array; its {@code i32.store16} lines follow it. */
    private static final String CHARS=" i32.const -5 i32.const 2 call $__new_primitive_array local.set $chars\n";
    private static final String FUNCTION="(func $__text";

    /**
     * Adds the character stores to every {@code $__text} constant in the module. {@link #runtimeWat} leaves them out:
     * they are most of the constants' text but refer to no function or global, so reachability and pruning do not need
     * them. Each value is decoded from its function name, the inverse of {@link #symbol}; only {@link #runtimeWat}
     * defines functions with that prefix, and each one must have the header it writes.
     */
    static String charactersWat(String wat) {
        StringBuilder out=null;
        int copied=0;
        for(int at=wat.indexOf(FUNCTION);at>=0;at=wat.indexOf(FUNCTION,at+1)) {
            int nameStart=at+"(func ".length(),nameEnd=wat.indexOf(' ',nameStart);
            String name=wat.substring(nameStart,nameEnd),value=value(name),global=name+"_cached";
            String header=value==null?null:" (result i32) (local $chars i32) (local $text i32)\nglobal.get "+global
                    +" if global.get "+global+" return end\ni32.const "+value.length()+CHARS;
            if(header==null || !wat.startsWith(header,nameEnd))
                throw new IllegalStateException("unexpected text constant function "+name);
            int line=nameEnd+header.length();
            if(out==null)out=new StringBuilder(wat.length()+wat.length()/2);
            out.append(wat,copied,line);
            for(int i=0;i<value.length();i++)
                out.append("local.get $chars i32.const ").append(value.charAt(i)+0).append(" i32.store16 offset=").append(8+i*2).append('\n');
            copied=line;
        }
        return out==null?wat:out.append(wat,copied,wat.length()).toString();
    }

    /** The value whose {@link #symbol} is {@code name}: {@code _}-separated lowercase hex UTF-16 units; otherwise null. */
    private static String value(String name) {
        if(!name.startsWith("$__text"))return null;
        StringBuilder value=new StringBuilder();
        int i="$__text".length();
        while(i<name.length()) {
            if(name.charAt(i)!='_')return null;
            int end=++i;
            while(end<name.length() && (name.charAt(end)>='0' && name.charAt(end)<='9' || name.charAt(end)>='a' && name.charAt(end)<='f'))end++;
            if(end==i || end-i>4)return null;
            value.append((char)Integer.parseInt(name,i,end,16));
            i=end;
        }
        return symbol(value.toString()).equals(name)?value.toString():null;
    }

    /** {@code $__exception_class_name} over the classes accepted by {@code instantiated}; no other class tags a value. */
    static String classNameWat(Map<String,ClassNode> linked,BytecodeDispatch dispatch,java.util.function.Predicate<String> instantiated) {
        if(!BytecodeExceptionRuntime.enabled(linked))return "";
        StringBuilder wat=new StringBuilder("(func $__exception_class_name (param $value i32) (result i32)\n");
        for(String name:new TreeSet<>(linked.keySet()))
            if(instantiated.test(name))
                wat.append("local.get $value i32.load i32.const ").append(dispatch.classId(name))
                        .append(" i32.eq if call ").append(symbol(name.replace('/','.'))).append(" return end\n");
        return wat.append("unreachable)\n").toString();
    }
}
