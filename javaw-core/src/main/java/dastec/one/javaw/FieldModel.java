package dastec.one.javaw;

/**
 * Metadata model for a Java class field.
 */
public class FieldModel {
    private final String name;
    private final String typeName;
    private final String wasmType;
    private final int offset;
    private final int size;
    private final boolean isStatic;

    public FieldModel(String name, String typeName, String wasmType, int offset, int size, boolean isStatic) {
        this.name = name;
        this.typeName = typeName;
        this.wasmType = wasmType;
        this.offset = offset;
        this.size = size;
        this.isStatic = isStatic;
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getJavaType() {
        return typeName;
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
