package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import java.util.*;

/** Canonical lazy Class mirrors and closed-world metadata; never initializes the represented type. */
public final class BytecodeClasses {
    private BytecodeClasses() {}
    private static final String[] PRIMITIVES={"Z","B","C","S","I","J","F","D","V"};
    private static final String[] NAMES={"boolean","byte","char","short","int","long","float","double","void"};

    static Map<String,Integer> primitiveTokens() {
        Map<String,Integer> result=new LinkedHashMap<>();
        for(int i=0;i<PRIMITIVES.length;i++)result.put(PRIMITIVES[i],-20-i);
        return result;
    }

    public static String symbol(String descriptor) { return "$__mirror"+BytecodeStrings.symbol(descriptor).substring("$__text".length()); }

    static void validateLiteral(Type type,BytecodeDispatch dispatch) {
        if(type.getSort()==Type.ARRAY) dispatch.typeTestIds(type.getDescriptor());
        else if(type.getSort()==Type.OBJECT) dispatch.checkClassAccess(type.getInternalName());
        else throw new IllegalArgumentException("class literals require an object or array constant; primitive literals use wrapper TYPE fields");
    }

    static String name(String descriptor) {
        for(int i=0;i<PRIMITIVES.length;i++)if(descriptor.equals(PRIMITIVES[i]))return NAMES[i];
        return descriptor.startsWith("[")?descriptor.replace('/','.')
                :Type.getType(descriptor).getClassName();
    }

    private static ClassNode node(String descriptor,Map<String,ClassNode> linked) {
        if(!descriptor.startsWith("L"))return null;
        String name=Type.getType(descriptor).getInternalName();
        return name.equals(BytecodeObjects.OBJECT)?BytecodeObjects.declaration():linked.get(name);
    }

    private static int modifiers(String descriptor,Map<String,ClassNode> linked) {
        if(descriptor.startsWith("["))return (modifiers(Type.getType(descriptor).getElementType().getDescriptor(),linked)&7)|1040;
        ClassNode node=node(descriptor,linked);
        if(node==null)return 1041;
        int flags=node.access;
        if(node.innerClasses!=null)for(InnerClassNode inner:node.innerClasses)
            if(node.name.equals(inner.name)){flags=inner.access;break;}
        return flags & (Opcodes.ACC_PUBLIC|Opcodes.ACC_PRIVATE|Opcodes.ACC_PROTECTED|Opcodes.ACC_STATIC
                |Opcodes.ACC_FINAL|Opcodes.ACC_INTERFACE|Opcodes.ACC_ABSTRACT|Opcodes.ACC_SYNTHETIC|Opcodes.ACC_ANNOTATION|Opcodes.ACC_ENUM);
    }

    private static String simpleName(String descriptor,Map<String,ClassNode> linked) {
        if(descriptor.startsWith("["))return simpleName(descriptor.substring(1),linked)+"[]";
        ClassNode node=node(descriptor,linked);
        if(node==null)return name(descriptor);
        for(InnerClassNode inner:node.innerClasses)if(node.name.equals(inner.name))return inner.innerName==null?"":inner.innerName;
        return node.name.substring(node.name.lastIndexOf('/')+1);
    }

    private static String canonicalName(String descriptor,Map<String,ClassNode> linked,Set<String> visited) {
        if(!visited.add(descriptor))return null;
        if(descriptor.startsWith("[")) {
            String component=canonicalName(descriptor.substring(1),linked,visited);
            return component==null?null:component+"[]";
        }
        ClassNode node=node(descriptor,linked);
        if(node==null)return name(descriptor);
        if(node.outerClass!=null)return null;
        for(InnerClassNode inner:node.innerClasses)if(node.name.equals(inner.name)) {
            if(inner.innerName==null)return null;
            if(inner.outerName!=null) {
                String outer=canonicalName("L"+inner.outerName+";",linked,visited);
                return outer==null?null:outer+"."+inner.innerName;
            }
        }
        return name(descriptor);
    }

    static Set<String> strings(Map<String,ClassNode> linked,BytecodeDispatch dispatch) {
        Set<String> strings=new TreeSet<>();
        for(String descriptor:dispatch.runtimeTypes().keySet()) {
            strings.add(name(descriptor));strings.add(simpleName(descriptor,linked));
            String canonical=canonicalName(descriptor,linked,new HashSet<>());
            if(canonical!=null)strings.add(canonical);
        }
        return strings;
    }

    static String runtimeWat(Map<String,ClassNode> linked,BytecodeDispatch dispatch) {
        Map<String,Integer> types=dispatch.runtimeTypes();
        var layouts=new BytecodeObjectLayouts(linked);
        int size=layouts.sizes().get("java/lang/Class");
        int tokenOffset=layouts.offsets().get(TypeHelper.getGlobalFieldName("java/lang/Class","token","I"));
        StringBuilder out=new StringBuilder();
        for(var entry:types.entrySet()) {
            String mirror=symbol(entry.getKey()),cached=mirror+"_cached";
            out.append("(global ").append(cached).append(" (mut i32) (i32.const 0))\n(func ").append(mirror)
                    .append(" (result i32) (local $mirror i32)\nglobal.get ").append(cached)
                    .append(" if global.get ").append(cached).append(" return end\ni32.const ").append(size)
                    .append(" call $__heap_alloc local.tee $mirror i32.eqz if ")
                    .append(dispatch.implicitFailureWat(BytecodeExceptions.Implicit.OUT_OF_MEMORY)).append(" end\n")
                    .append("local.get $mirror i32.const 0 i32.const ").append(size).append(" memory.fill\n")
                    .append("local.get $mirror i32.const ").append(dispatch.classId("java/lang/Class")).append(" i32.store\n")
                    .append("local.get $mirror i32.const ").append(entry.getValue()).append(" i32.store offset=").append(tokenOffset)
                    .append("\nlocal.get $mirror global.set ").append(cached).append(" local.get $mirror)\n");
        }
        out.append("(func $__class_mirror (param $token i32) (result i32)\n");
        for(var entry:types.entrySet())out.append(test(entry.getValue())).append("call ").append(symbol(entry.getKey())).append(" return end\n");
        out.append("unreachable)\n");
        for(String property:List.of("name","simple","canonical","modifiers","enum","component","super","interfaces")) {
            out.append("(func $__type_").append(property).append(" (param $token i32) (result i32) (local $array i32)\n");
            for(var entry:types.entrySet()) {
                String descriptor=entry.getKey();ClassNode node=node(descriptor,linked);
                out.append(test(entry.getValue()));
                switch(property) {
                    case "name" -> text(out,name(descriptor));
                    case "simple" -> text(out,simpleName(descriptor,linked));
                    case "canonical" -> text(out,canonicalName(descriptor,linked,new HashSet<>()));
                    case "enum" -> out.append("i32.const ").append(node!=null && (node.access&Opcodes.ACC_ENUM)!=0
                            && "java/lang/Enum".equals(node.superName)?1:0).append('\n');
                    case "modifiers" -> out.append("i32.const ").append(modifiers(descriptor,linked)).append('\n');
                    case "component" -> mirror(out,descriptor.startsWith("[")?descriptor.substring(1):null);
                    case "super" -> mirror(out,descriptor.startsWith("[")?"Ljava/lang/Object;":
                            node!=null && (node.access&Opcodes.ACC_INTERFACE)==0 && node.superName!=null?"L"+node.superName+";":null);
                    case "interfaces" -> {
                        List<String> interfaces=descriptor.startsWith("[")?List.of("java/lang/Cloneable","java/io/Serializable"):
                                node==null?List.of():node.interfaces;
                        out.append("i32.const ").append(interfaces.size()).append(" i32.const ")
                                .append(dispatch.referenceArrayId("java/lang/Class")).append(" i32.const 4 call $__new_primitive_array local.set $array\n");
                        for(int i=0;i<interfaces.size();i++) {
                            out.append("local.get $array\n");mirror(out,"L"+interfaces.get(i)+";");
                            out.append("i32.store offset=").append(8+4*i).append('\n');
                        }
                        out.append("local.get $array\n");
                    }
                }
                out.append("return end\n");
            }
            out.append("unreachable)\n");
        }
        out.append("(func $__type_assignable (param $from i32) (param $to i32) (result i32)\n");
        for(var to:types.entrySet()) {
            out.append("local.get $to i32.const ").append(to.getValue()).append(" i32.eq if\ni32.const 0\n");
            for(var from:types.entrySet())if(dispatch.descriptorAssignable(from.getKey(),to.getKey()))
                out.append("local.get $from i32.const ").append(from.getValue()).append(" i32.eq i32.or\n");
            out.append("return end\n");
        }
        return out.append("unreachable)\n").toString();
    }

    private static String test(int token) { return "local.get $token i32.const "+token+" i32.eq if\n"; }
    private static void mirror(StringBuilder out,String descriptor) {
        out.append(descriptor==null?"i32.const 0\n":"call "+symbol(descriptor)+"\n");
    }
    private static void text(StringBuilder out,String text) {
        out.append(text==null?"i32.const 0\n":"call "+BytecodeStrings.symbol(text)+"\n");
    }
}
