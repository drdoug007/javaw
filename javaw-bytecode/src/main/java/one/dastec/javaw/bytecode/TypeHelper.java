package one.dastec.javaw.bytecode;

import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

public class TypeHelper {

    public static String watNumber(Object value) {
        if (value == null) return "0";
        String text = value.toString();
        return switch (text) {
            case "NaN" -> "nan";
            case "Infinity" -> "inf";
            case "-Infinity" -> "-inf";
            default -> text;
        };
    }

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

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private static String encode(String value) {
        // Encode UTF-16 code units individually, including any unpaired surrogates in JVM names.
        char[] result = new char[value.length() * 4];
        for (int i = 0, j = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            for (int shift = 12; shift >= 0; shift -= 4) result[j++] = HEX[c >>> shift & 15];
        }
        return new String(result);
    }

    public static String getMethodFunctionName(String ownerInternalName, String methodName, String methodDescriptor) {
        return "$method_" + encode(ownerInternalName) + "_" + encode(methodName) + "_" + encode(methodDescriptor);
    }

    public static String getGlobalFieldName(String ownerInternalName, String fieldName, String descriptor) {
        return "$global_" + encode(ownerInternalName) + "_" + encode(fieldName) + "_" + encode(descriptor);
    }

    /** Escape export names as UTF-8 WAT string literals, preserving ordinary readable names. */
    public static String watString(String value) {
        if (!java.nio.charset.StandardCharsets.UTF_8.newEncoder().canEncode(value))
            throw new IllegalArgumentException("public export names require well-formed Unicode; rename the class or method");
        StringBuilder result = new StringBuilder();
        for (byte octet : value.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            int b = octet & 255;
            if (b >= 32 && b <= 126 && b != '"' && b != '\\') result.append((char) b);
            else result.append('\\').append(Character.forDigit(b >>> 4, 16)).append(Character.forDigit(b & 15, 16));
        }
        return result.toString();
    }
}
