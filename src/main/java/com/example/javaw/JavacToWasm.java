package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JavacToWasm {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: javaw <JavaFile.java>");
            System.exit(1);
        }

        String javaFilePath = args[0];
        Path path = Paths.get(javaFilePath).toAbsolutePath().normalize();
        String stem = path.getFileName().toString().replace(".java", "");
        Path wasmFilePath = path.getParent().resolve(stem + ".wat");

        try {
            System.out.println("Compiling: " + path);
            System.out.println("Target WASM (WAT): " + wasmFilePath);

            CompilationUnit cu = StaticJavaParser.parse(path);
            StringBuilder watOutput = new StringBuilder();
            watOutput.append("(module\n");

            // Visit the AST and build the WAT content
            cu.accept(new WasmGeneratorVisitor(), watOutput);

            watOutput.append(")\n");

            try (FileWriter writer = new FileWriter(wasmFilePath.toFile())) {
                writer.write(watOutput.toString());
            }

            System.out.println("Compilation successful.");
        } catch (Exception e) {
            System.err.println("Error during compilation: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * A visitor that translates Java AST elements into WebAssembly Text format (WAT) snippets.
     */
    private static class WasmGeneratorVisitor extends VoidVisitorAdapter<StringBuilder> {

        @Override
        public void visit(ClassOrInterfaceDeclaration n, StringBuilder collector) {
            super.visit(n, collector);
            collector.append("  ;; Class/Interface: ").append(n.getNameAsString()).append("\n");
        }

        @Override
        public void visit(MethodDeclaration n, StringBuilder collector) {
            super.visit(n, collector);
            String methodName = n.getNameAsString();
            String returnType = n.getType().asString();

            collector.append("  (func $").append(methodName).append(" ");

            // Map Java types to WASM signature syntax
            if (returnType.equals("void")) {
                // no return type
            } else if (returnType.equals("int")) {
                collector.append("-> i32 ");
            } else if (returnType.equals("double")) {
                collector.append("-> f64 ");
            } else {
                // Default fallback
                collector.append("-> i32 ");
            }

            collector.append("\n    ;; Method Body for ").append(methodName).append("\n");
            // In a full implementation, we would add statements here.
            collector.append("    end\n  )\n");
        }
    }
}
