package com.example.javaw;

import com.github.javaparser.ast.type.Type;

public class TypeMapper {

    /**
     * Map Java type names to WebAssembly value types.
     */
    public static String mapType(String typeStr) {
        if (typeStr == null) {
            return "i32";
        }
        if (typeStr.endsWith("[]")) {
            return "i32";
        }
        if (typeStr.endsWith("Float32x4") || typeStr.endsWith("Int32x4") || typeStr.endsWith("Vector128") ||
                typeStr.endsWith("F32x4") || typeStr.endsWith("I32x4")) {
            return "v128";
        }
        return switch (typeStr) {
            case "void" -> null;
            case "int", "boolean", "byte", "short", "char" -> "i32";
            case "long" -> "i64";
            case "float" -> "f32";
            case "double" -> "f64";
            case "Float32x4", "Int32x4", "Vector128", "F32x4", "I32x4" -> "v128";
            default -> "i32";
        };
    }

    public static String mapType(Type type) {
        return mapType(type.asString());
    }
}
