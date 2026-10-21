package com.example.javaw;

import java.util.*;

/**
 * Metadata model for a Java class or interface.
 */
public class ClassModel {
    private final String name;
    private final String superClassName;
    private final boolean isInterface;
    private final int classId;
    private final Map<String, FieldModel> fields = new LinkedHashMap<>();
    private final List<MethodModel> constructors = new ArrayList<>();
    private final Map<String, MethodModel> methods = new LinkedHashMap<>(); // signatureKey -> MethodModel
    private final List<MethodModel> vtableSlots = new ArrayList<>();
    private int structSize = 8;
    private int vtableBase = 0;
    private boolean isExported = false;

    public ClassModel(String name, String superClassName, boolean isInterface, int classId) {
        this.name = name;
        this.superClassName = superClassName;
        this.isInterface = isInterface;
        this.classId = classId;
    }

    public String getName() {
        return name;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public int getClassId() {
        return classId;
    }

    public Map<String, FieldModel> getFields() {
        return fields;
    }

    public void addField(FieldModel field) {
        fields.put(field.getName(), field);
    }

    public FieldModel getField(String fieldName) {
        return fields.get(fieldName);
    }

    public List<MethodModel> getConstructors() {
        return constructors;
    }

    public void addConstructor(MethodModel constructor) {
        constructors.add(constructor);
    }

    public Map<String, MethodModel> getMethods() {
        return methods;
    }

    public void addMethod(MethodModel method) {
        methods.put(method.getSignatureKey(), method);
    }

    public MethodModel getMethodByName(String methodName) {
        for (MethodModel m : methods.values()) {
            if (m.getName().equals(methodName)) {
                return m;
            }
        }
        return null;
    }

    public MethodModel getMethod(String signatureKey) {
        return methods.get(signatureKey);
    }

    public List<MethodModel> getVtableSlots() {
        return vtableSlots;
    }

    public int getStructSize() {
        return structSize;
    }

    public void setStructSize(int structSize) {
        this.structSize = structSize;
    }

    public int getVtableBase() {
        return vtableBase;
    }

    public void setVtableBase(int vtableBase) {
        this.vtableBase = vtableBase;
    }

    public boolean isExported() {
        return isExported;
    }

    public void setExported(boolean exported) {
        this.isExported = exported;
    }

    public boolean isOopClass() {
        boolean hasInstanceFields = false;
        for (FieldModel fm : fields.values()) {
            if (!fm.isStatic()) {
                hasInstanceFields = true;
                break;
            }
        }
        return hasInstanceFields || !constructors.isEmpty() || superClassName != null || isExported;
    }
}
