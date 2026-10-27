package one.dastec.javaw.js;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Metadata model representing exported Java classes, constructors, methods, and fields for JavaScript binding via @JS.Export.
 */
public class JsExportModel {
    private final String className;
    private final String exportName;
    private final int structSize;
    private final int vtableBase;
    private final boolean hasVtable;
    private final List<ConstructorModel> constructors = new ArrayList<>();
    private final List<MethodModel> methods = new ArrayList<>();
    private final List<FieldModel> fields = new ArrayList<>();

    public JsExportModel(String className, String exportName, int structSize, int vtableBase, boolean hasVtable) {
        this.className = className;
        this.exportName = exportName;
        this.structSize = structSize;
        this.vtableBase = vtableBase;
        this.hasVtable = hasVtable;
    }

    public void addConstructor(String exportFunctionName, List<JsSnippetModel.JsParam> params) {
        constructors.add(new ConstructorModel(exportFunctionName, params));
    }

    public void addMethod(String methodName, String exportName, String wasmFunctionName,
                          String returnType, String returnWasmType, boolean isStatic, boolean isCoerced,
                          List<JsSnippetModel.JsParam> params) {
        methods.add(new MethodModel(methodName, exportName, wasmFunctionName, returnType, returnWasmType, isStatic, isCoerced, params));
    }

    public void addField(String name, String javaType, String wasmType, int offset, int size, boolean isStatic) {
        fields.add(new FieldModel(name, javaType, wasmType, offset, size, isStatic));
    }

    public String getClassName() {
        return className;
    }

    public String getExportName() {
        return exportName;
    }

    public int getStructSize() {
        return structSize;
    }

    public int getVtableBase() {
        return vtableBase;
    }

    public boolean hasVtable() {
        return hasVtable;
    }

    public List<ConstructorModel> getConstructors() {
        return constructors;
    }

    public List<MethodModel> getMethods() {
        return methods;
    }

    public List<FieldModel> getFields() {
        return fields;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"className\":\"").append(escapeJson(className)).append("\",");
        sb.append("\"exportName\":\"").append(escapeJson(exportName)).append("\",");
        sb.append("\"structSize\":").append(structSize).append(",");
        sb.append("\"vtableBase\":").append(vtableBase).append(",");
        sb.append("\"hasVtable\":").append(hasVtable).append(",");

        sb.append("\"constructors\":[");
        for (int i = 0; i < constructors.size(); i++) {
            if (i > 0) sb.append(",");
            ConstructorModel c = constructors.get(i);
            sb.append("{");
            sb.append("\"exportFunctionName\":\"").append(escapeJson(c.getExportFunctionName())).append("\",");
            sb.append("\"params\":[");
            for (int j = 0; j < c.getParams().size(); j++) {
                if (j > 0) sb.append(",");
                JsSnippetModel.JsParam p = c.getParams().get(j);
                sb.append("{\"name\":\"").append(escapeJson(p.getName())).append("\",")
                        .append("\"javaType\":\"").append(escapeJson(p.getJavaType())).append("\",")
                        .append("\"wasmType\":\"").append(escapeJson(p.getWasmType())).append("\",")
                        .append("\"coerced\":").append(p.isCoerced()).append("}");
            }
            sb.append("]}");
        }
        sb.append("],");

        sb.append("\"methods\":[");
        for (int i = 0; i < methods.size(); i++) {
            if (i > 0) sb.append(",");
            MethodModel m = methods.get(i);
            sb.append("{");
            sb.append("\"methodName\":\"").append(escapeJson(m.getMethodName())).append("\",");
            sb.append("\"exportName\":\"").append(escapeJson(m.getExportName())).append("\",");
            sb.append("\"wasmFunctionName\":\"").append(escapeJson(m.getWasmFunctionName())).append("\",");
            sb.append("\"returnType\":\"").append(escapeJson(m.getReturnType())).append("\",");
            sb.append("\"returnWasmType\":\"").append(escapeJson(m.getReturnWasmType())).append("\",");
            sb.append("\"isStatic\":").append(m.isStatic()).append(",");
            sb.append("\"isCoerced\":").append(m.isCoerced()).append(",");
            sb.append("\"params\":[");
            for (int j = 0; j < m.getParams().size(); j++) {
                if (j > 0) sb.append(",");
                JsSnippetModel.JsParam p = m.getParams().get(j);
                sb.append("{\"name\":\"").append(escapeJson(p.getName())).append("\",")
                        .append("\"javaType\":\"").append(escapeJson(p.getJavaType())).append("\",")
                        .append("\"wasmType\":\"").append(escapeJson(p.getWasmType())).append("\",")
                        .append("\"coerced\":").append(p.isCoerced()).append("}");
            }
            sb.append("]}");
        }
        sb.append("],");

        sb.append("\"fields\":[");
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) sb.append(",");
            FieldModel f = fields.get(i);
            sb.append("{");
            sb.append("\"name\":\"").append(escapeJson(f.getName())).append("\",");
            sb.append("\"javaType\":\"").append(escapeJson(f.getJavaType())).append("\",");
            sb.append("\"wasmType\":\"").append(escapeJson(f.getWasmType())).append("\",");
            sb.append("\"offset\":").append(f.getOffset()).append(",");
            sb.append("\"size\":").append(f.getSize()).append(",");
            sb.append("\"isStatic\":").append(f.isStatic());
            sb.append("}");
        }
        sb.append("]");

        sb.append("}");
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

    public static class ConstructorModel {
        private final String exportFunctionName;
        private final List<JsSnippetModel.JsParam> params;

        public ConstructorModel(String exportFunctionName, List<JsSnippetModel.JsParam> params) {
            this.exportFunctionName = exportFunctionName;
            this.params = params != null ? params : Collections.emptyList();
        }

        public String getExportFunctionName() {
            return exportFunctionName;
        }

        public List<JsSnippetModel.JsParam> getParams() {
            return params;
        }
    }

    public static class MethodModel {
        private final String methodName;
        private final String exportName;
        private final String wasmFunctionName;
        private final String returnType;
        private final String returnWasmType;
        private final boolean isStatic;
        private final boolean isCoerced;
        private final List<JsSnippetModel.JsParam> params;

        public MethodModel(String methodName, String exportName, String wasmFunctionName,
                           String returnType, String returnWasmType, boolean isStatic, boolean isCoerced,
                           List<JsSnippetModel.JsParam> params) {
            this.methodName = methodName;
            this.exportName = exportName;
            this.wasmFunctionName = wasmFunctionName;
            this.returnType = returnType;
            this.returnWasmType = returnWasmType;
            this.isStatic = isStatic;
            this.isCoerced = isCoerced;
            this.params = params != null ? params : Collections.emptyList();
        }

        public String getMethodName() {
            return methodName;
        }

        public String getExportName() {
            return exportName;
        }

        public String getWasmFunctionName() {
            return wasmFunctionName;
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

        public List<JsSnippetModel.JsParam> getParams() {
            return params;
        }
    }

    public static class FieldModel {
        private final String name;
        private final String javaType;
        private final String wasmType;
        private final int offset;
        private final int size;
        private final boolean isStatic;

        public FieldModel(String name, String javaType, String wasmType, int offset, int size, boolean isStatic) {
            this.name = name;
            this.javaType = javaType;
            this.wasmType = wasmType;
            this.offset = offset;
            this.size = size;
            this.isStatic = isStatic;
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

        public int getOffset() {
            return offset;
        }

        public int getSize() {
            return size;
        }

        public boolean isStatic() {
            return isStatic;
        }
    }
}
