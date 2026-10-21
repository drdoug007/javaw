package com.example.javaw.js;

import java.util.ArrayList;
import java.util.List;

/**
 * Model representing an extracted @JS inline JavaScript snippet method.
 */
public class JsSnippetModel {
    private final String className;
    private final String methodName;
    private final String importName;
    private final String jsCode;
    private final List<JsParam> parameters = new ArrayList<>();
    private final String returnType;
    private final String returnWasmType;
    private final boolean isStatic;
    private final boolean isCoerced;

    public JsSnippetModel(String className, String methodName, String importName, String jsCode,
                          String returnType, String returnWasmType, boolean isStatic, boolean isCoerced) {
        this.className = className;
        this.methodName = methodName;
        this.importName = importName;
        this.jsCode = jsCode;
        this.returnType = returnType;
        this.returnWasmType = returnWasmType;
        this.isStatic = isStatic;
        this.isCoerced = isCoerced;
    }

    public void addParameter(String name, String javaType, String wasmType, boolean coerced) {
        parameters.add(new JsParam(name, javaType, wasmType, coerced));
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getImportName() {
        return importName;
    }

    public String getJsCode() {
        return jsCode;
    }

    public List<JsParam> getParameters() {
        return parameters;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getReturnWasmType() {
        return returnWasmType;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isCoerced() {
        return isCoerced;
    }

    public String getWasmImportSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append("(import \"dom\" \"").append(importName).append("\" (func $dom_").append(importName);
        StringBuilder params = new StringBuilder();
        for (JsParam p : parameters) {
            if ("String".equals(p.getJavaType()) || "java.lang.String".equals(p.getJavaType())) {
                params.append(" i32 i32");
            } else {
                params.append(" ").append(p.getWasmType());
            }
        }
        if (params.length() > 0) {
            sb.append(" (param").append(params).append(")");
        }
        if (returnWasmType != null) {
            sb.append(" (result ").append(returnWasmType).append(")");
        }
        sb.append("))");
        return sb.toString();
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"className\":\"").append(escapeJson(className)).append("\",");
        sb.append("\"methodName\":\"").append(escapeJson(methodName)).append("\",");
        sb.append("\"importName\":\"").append(escapeJson(importName)).append("\",");
        sb.append("\"jsCode\":\"").append(escapeJson(jsCode)).append("\",");
        sb.append("\"returnType\":\"").append(escapeJson(returnType)).append("\",");
        sb.append("\"returnWasmType\":").append(returnWasmType == null ? "null" : "\"" + escapeJson(returnWasmType) + "\"").append(",");
        sb.append("\"isStatic\":").append(isStatic).append(",");
        sb.append("\"isCoerced\":").append(isCoerced).append(",");
        sb.append("\"parameters\":[");
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(",");
            JsParam p = parameters.get(i);
            sb.append("{");
            sb.append("\"name\":\"").append(escapeJson(p.getName())).append("\",");
            sb.append("\"javaType\":\"").append(escapeJson(p.getJavaType())).append("\",");
            sb.append("\"wasmType\":\"").append(escapeJson(p.getWasmType())).append("\",");
            sb.append("\"coerced\":").append(p.isCoerced());
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static class JsParam {
        private final String name;
        private final String javaType;
        private final String wasmType;
        private final boolean coerced;

        public JsParam(String name, String javaType, String wasmType, boolean coerced) {
            this.name = name;
            this.javaType = javaType;
            this.wasmType = wasmType;
            this.coerced = coerced;
        }

        public String getName() {
            return name;
        }

        public String getJavaType() {
            return javaType;
        }

        public String getWasmType() {
            return wasmType;
        }

        public boolean isCoerced() {
            return coerced;
        }
    }
}
