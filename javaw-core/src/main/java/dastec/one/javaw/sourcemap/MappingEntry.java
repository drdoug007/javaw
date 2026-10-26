package dastec.one.javaw.sourcemap;

import java.util.Objects;

/**
 * Represents a single source map mapping entry between generated output (WAT/WASM) and original source (Java).
 */
public class MappingEntry {
    private final int generatedLine;   // 0-based
    private final int generatedColumn; // 0-based
    private final String sourceFile;
    private final int originalLine;    // 0-based
    private final int originalColumn;  // 0-based
    private final String name;         // optional symbol name

    public MappingEntry(int generatedLine, int generatedColumn, String sourceFile, int originalLine, int originalColumn, String name) {
        this.generatedLine = generatedLine;
        this.generatedColumn = generatedColumn;
        this.sourceFile = sourceFile;
        this.originalLine = originalLine;
        this.originalColumn = originalColumn;
        this.name = name;
    }

    public int getGeneratedLine() {
        return generatedLine;
    }

    public int getGeneratedColumn() {
        return generatedColumn;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public int getOriginalLine() {
        return originalLine;
    }

    public int getOriginalColumn() {
        return originalColumn;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappingEntry that = (MappingEntry) o;
        return generatedLine == that.generatedLine &&
                generatedColumn == that.generatedColumn &&
                originalLine == that.originalLine &&
                originalColumn == that.originalColumn &&
                Objects.equals(sourceFile, that.sourceFile) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(generatedLine, generatedColumn, sourceFile, originalLine, originalColumn, name);
    }

    @Override
    public String toString() {
        return "MappingEntry{" +
                "gen=" + (generatedLine + 1) + ":" + generatedColumn +
                ", src=" + sourceFile +
                ", orig=" + (originalLine + 1) + ":" + originalColumn +
                (name != null ? ", name='" + name + '\'' : "") +
                '}';
    }
}
