package com.example.javaw.bytecode;

import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

public class TypeHelper {

    public static String getWasmType(Type type) {
        switch (type.getSort()) {
            case Type.VOID:
                return null;
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
            case Type.OBJECT:
            case Type.ARRAY:
                return "i32";
            case Type.LONG:
                return "i64";
            case Type.FLOAT:
                return "f32";
            case Type.DOUBLE:
                return "f64";
            default:
                return "i32";
        }
    }

    public static String getWasmType(String descriptor) {
        return getWasmType(Type.getType(descriptor));
    }

    public static List<String> getParamWasmTypes(String methodDescriptor) {
        Type[] argumentTypes = Type.getArgumentTypes(methodDescriptor);
        List<String> list = new ArrayList<>();
        for (Type t : argumentTypes) {
            String wasmType = getWasmType(t);
            if (wasmType != null) {
                list.add(wasmType);
            }
        }
        return list;
    }

    public static String getReturnWasmType(String methodDescriptor) {
        Type returnType = Type.getReturnType(methodDescriptor);
        return getWasmType(returnType);
    }

    public static String sanitizeName(String name) {
        return name.replace('/', '_').replace('.', '_').replace('$', '_').replace('<', '_').replace('>', '_');
    }

    public static String getMethodFunctionName(String ownerInternalName, String methodName, String methodDescriptor) {
        String owner = sanitizeName(ownerInternalName);
        String name = sanitizeName(methodName);
        if ("_init_".equals(name)) {
            name = "_init";
        } else if ("_clinit_".equals(name)) {
            name = "_clinit";
        }
        return "$" + owner + "_" + name;
    }

    public static String getGlobalFieldName(String ownerInternalName, String fieldName) {
        return "$global_" + sanitizeName(ownerInternalName) + "_" + sanitizeName(fieldName);
    }
}
