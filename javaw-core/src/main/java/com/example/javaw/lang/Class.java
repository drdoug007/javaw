package com.example.javaw.lang;

/**
 * Instances of the class {@code Class} represent classes and interfaces in a running
 * WebAssembly application.
 *
 * @param <T> the type of the class modeled by this {@code Class} object
 */
public class Class<T> {

    private String name;
    private String simpleName;
    private int classId;
    private boolean isInterface;
    private boolean isEnum;
    private Class<?> superclass;

    public Class(String name, String simpleName, int classId, boolean isInterface, boolean isEnum, Class<?> superclass) {
        this.name = name;
        this.simpleName = simpleName;
        this.classId = classId;
        this.isInterface = isInterface;
        this.isEnum = isEnum;
        this.superclass = superclass;
    }

    public String getName() {
        return name;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public int getId() {
        return classId;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public boolean isArray() {
        return name != null && name.startsWith("[");
    }

    public Class<?> getSuperclass() {
        return superclass;
    }

    public boolean isInstance(Object obj) {
        return false;
    }

    @Override
    public String toString() {
        return (isInterface ? "interface " : "class ") + name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Class) {
            Class<?> other = (Class<?>) obj;
            return this.classId == other.classId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return classId;
    }

    public static Class<?> forName(String className) {
        return null;
    }
}
