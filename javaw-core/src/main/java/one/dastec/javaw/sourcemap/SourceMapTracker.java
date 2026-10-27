package one.dastec.javaw.sourcemap;

import com.github.javaparser.ast.Node;

/**
 * Helper to record line and column mappings from JavaParser AST nodes
 * into a SourceMapGenerator while emitting WebAssembly Text (WAT).
 */
public class SourceMapTracker {
    private final SourceMapGenerator generator;
    private String sourceFile = "Source.java";
    private int currentGenLine = 0;
    private int lastLength = 0;

    public SourceMapTracker(SourceMapGenerator generator) {
        this.generator = generator;
    }

    public SourceMapTracker(SourceMapGenerator generator, String sourceFile) {
        this.generator = generator;
        this.sourceFile = sourceFile != null ? sourceFile : "Source.java";
    }

    public SourceMapGenerator getGenerator() {
        return generator;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile != null ? sourceFile : "Source.java";
    }

    public int getCurrentLine(StringBuilder sb) {
        if (sb == null) return 0;
        int len = sb.length();
        if (len < lastLength) {
            // Buffer was reset or modified backwards
            currentGenLine = 0;
            lastLength = 0;
        }
        for (int i = lastLength; i < len; i++) {
            if (sb.charAt(i) == '\n') {
                currentGenLine++;
            }
        }
        lastLength = len;
        return currentGenLine;
    }

    public void track(StringBuilder sb, Node node) {
        track(sb, node, null);
    }

    public void track(StringBuilder sb, Node node, String symbolName) {
        if (generator == null || node == null || sb == null) return;
        node.getRange().ifPresent(range -> {
            int genLine = getCurrentLine(sb);
            int origLine = Math.max(0, range.begin.line - 1);
            int origCol = Math.max(0, range.begin.column - 1);
            generator.addMapping(genLine, 0, sourceFile, origLine, origCol, symbolName);
        });
    }
}
