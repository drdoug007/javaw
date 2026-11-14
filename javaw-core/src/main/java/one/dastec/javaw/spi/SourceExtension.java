package one.dastec.javaw.spi;

import com.github.javaparser.ast.CompilationUnit;

/**
 * A source-level compiler extension discovered with {@link java.util.ServiceLoader}.
 *
 * <p>{@link #prepare} runs once per module, on the merged compilation unit, after source names are
 * normalized and before library types are added, callbacks are lowered and the class registry is built.
 * An extension may add types, rewrite expressions and report diagnostics by throwing
 * {@link IllegalArgumentException} (use {@link #error} for a source position), and may contribute
 * WebAssembly custom sections through {@link Output}.
 */
public interface SourceExtension {
    /** Package prefixes this extension owns; programs importing them fail clearly when it is absent. */
    java.util.Set<String> packages();

    void prepare(CompilationUnit cu, Output output);

    interface Output {
        /** Adds a custom section whose payload is UTF-8 text, typically JSON. */
        void customSection(String name, String payload);
    }

    /** A diagnostic carrying the node's source file and position, in the compiler's usual format. */
    static IllegalArgumentException error(com.github.javaparser.ast.Node node, String message) {
        return one.dastec.javaw.JavacToWasm.sourceError(node, message);
    }
}
