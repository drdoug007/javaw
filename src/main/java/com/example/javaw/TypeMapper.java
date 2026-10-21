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
        return switch (typeStr) {
            case "void" -> null;
            case "int", "boolean", "byte", "short", "char" -> "i32";
            case "long" -> "i64";
            case "float" -> "f32";
            case "double" -> "f64";
            default -> "i32";
        };
    }

    public static String mapType(Type type) {
        return mapType(type.asString());
    }
}
