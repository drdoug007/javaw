package com.example.javaw;

import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a Java record, its component fields, and WebAssembly multi-value return signature.
 */
public class RecordModel {
    private final String name;
    private final List<FieldModel> components = new ArrayList<>();
    private final List<MethodModel> methods = new ArrayList<>();

    public RecordModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addComponent(FieldModel component) {
        components.add(component);
    }

    public List<FieldModel> getComponents() {
        return components;
    }

    public FieldModel getComponent(String componentName) {
        for (FieldModel f : components) {
            if (f.getName().equals(componentName)) {
                return f;
            }
        }
        return null;
    }

    public int getComponentIndex(String componentName) {
        for (int i = 0; i < components.size(); i++) {
            if (components.get(i).getName().equals(componentName)) {
                return i;
            }
        }
        return -1;
    }

    public void addMethod(MethodModel method) {
        methods.add(method);
    }

    public List<MethodModel> getMethods() {
        return methods;
    }

    public MethodModel getMethodByName(String methodName) {
        for (MethodModel mm : methods) {
            if (mm.getName().equals(methodName)) {
                return mm;
            }
        }
        return null;
    }

    /**
     * Emits the WebAssembly multi-value result signature, e.g. "i32 i32" or "i32 i32 f64".
     */
    public String getWasmResultSignature() {
        StringBuilder sb = new StringBuilder();
        for (FieldModel f : components) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(f.getWasmType());
        }
        return sb.toString();
    }
}
