package com.example.javaw;

import java.util.List;

/**
 * Metadata model for a Java method or constructor.
 */
public class MethodModel {
    private final String name;
    private final String className;
    private final String functionName;
    private final String returnTypeName;
    private final String returnWasmType;
    private final List<String> paramNames;
    private final List<String> paramTypeNames;
    private final List<String> paramWasmTypes;
    private final boolean isStatic;
    private final boolean isConstructor;
    private boolean isVirtual;
    private int virtualSlotIndex = -1;
    private String typeSignatureName;
    private boolean isSynthetic = false;

    public MethodModel(String name, String className, String functionName, String returnTypeName,
                       String returnWasmType, List<String> paramNames, List<String> paramTypeNames,
                       List<String> paramWasmTypes, boolean isStatic, boolean isConstructor) {
        this.name = name;
        this.className = className;
        this.functionName = functionName;
        this.returnTypeName = returnTypeName;
        this.returnWasmType = returnWasmType;
        this.paramNames = paramNames;
        this.paramTypeNames = paramTypeNames;
        this.paramWasmTypes = paramWasmTypes;
        this.isStatic = isStatic;
        this.isConstructor = isConstructor;
        this.isVirtual = !isStatic && !isConstructor;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getReturnTypeName() {
        return returnTypeName;
    }

    public String getReturnWasmType() {
        return returnWasmType;
    }

    public List<String> getParamNames() {
        return paramNames;
    }

    public List<String> getParamTypeNames() {
        return paramTypeNames;
    }

    public List<String> getParamWasmTypes() {
        return paramWasmTypes;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isConstructor() {
        return isConstructor;
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public void setVirtual(boolean virtual) {
        isVirtual = virtual;
    }

    public int getVirtualSlotIndex() {
        return virtualSlotIndex;
    }

    public void setVirtualSlotIndex(int virtualSlotIndex) {
        this.virtualSlotIndex = virtualSlotIndex;
    }

    public String getTypeSignatureName() {
        return typeSignatureName;
    }

    public void setTypeSignatureName(String typeSignatureName) {
        this.typeSignatureName = typeSignatureName;
    }

    public boolean isSynthetic() {
        return isSynthetic;
    }

    public void setSynthetic(boolean synthetic) {
        isSynthetic = synthetic;
    }

    public String getSignatureKey() {
        return name + "(" + String.join(",", paramTypeNames) + ")";
    }

    public String buildWasmTypeSignature() {
        StringBuilder sb = new StringBuilder("(func");
        if (!paramWasmTypes.isEmpty() || !isStatic) {
            sb.append(" (param");
            if (!isStatic) {
                sb.append(" i32"); // $this
            }
            for (String pType : paramWasmTypes) {
                sb.append(" ").append(pType);
            }
            sb.append(")");
        }
        if (returnWasmType != null) {
            sb.append(" (result ").append(returnWasmType).append(")");
        }
        sb.append(")");
        return sb.toString();
    }
}
