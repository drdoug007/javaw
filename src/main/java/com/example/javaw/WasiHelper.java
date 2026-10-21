package com.example.javaw;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class WasiHelper {

    public static Set<String> collectWasiImports(CompilationUnit cu) {
        Set<String> imports = new LinkedHashSet<>();
        List<MethodCallExpr> calls = cu.findAll(MethodCallExpr.class);
        for (MethodCallExpr call : calls) {
            String mName = call.getNameAsString();
            if (call.getScope().isPresent()) {
                Expression scope = call.getScope().get();
                String scopeStr = scope.toString();

                if (scopeStr.equals("System.out") || scopeStr.equals("System.err")) {
                    if (mName.equals("println") || mName.equals("print")) {
                        imports.add("fd_write");
                    }
                } else if (scopeStr.equals("System")) {
                    if (mName.equals("exit")) {
                        imports.add("proc_exit");
                    }
                } else if (scopeStr.equals("WASI") || scopeStr.endsWith(".WASI")) {
                    if (mName.equals("print") || mName.equals("println") || mName.equals("eprint") || mName.equals("eprintln") || mName.equals("fdWrite")) {
                        imports.add("fd_write");
                    } else if (mName.equals("exit") || mName.equals("procExit")) {
                        imports.add("proc_exit");
                    } else if (mName.equals("argsSizesGet") || mName.equals("argsGet")) {
                        imports.add("args_sizes_get");
                        imports.add("args_get");
                    } else if (mName.equals("environSizesGet") || mName.equals("environGet")) {
                        imports.add("environ_sizes_get");
                        imports.add("environ_get");
                    } else if (mName.equals("randomGet")) {
                        imports.add("random_get");
                    } else if (mName.equals("clockTimeGet")) {
                        imports.add("clock_time_get");
                    }
                }
            }
        }
        return imports;
    }

    public static void emitWasiImports(Set<String> wasiImports, StringBuilder watOutput) {
        if (wasiImports.isEmpty()) return;

        watOutput.append("  ;; --- WASI Host Imports (wasi_snapshot_preview1) ---\n");
        if (wasiImports.contains("fd_write")) {
            watOutput.append("  (import \"wasi_snapshot_preview1\" \"fd_write\" (func $__wasi_fd_write (param i32 i32 i32 i32) (result i32)))\n");
        }
        if (wasiImports.contains("fd_read")) {
            watOutput.append("  (import \"wasi_snapshot_preview1\" \"fd_read\" (func $__wasi_fd_read (param i32 i32 i32 i32) (result i32)))\n");
        }
        if (wasiImports.contains("proc_exit")) {
            watOutput.append("  (import \"wasi_snapshot_preview1\" \"proc_exit\" (func $__wasi_proc_exit (param i32)))\n");
        }
        if (wasiImports.contains("args_sizes_get")) {
            watOutput.append("  (import \"wasi_snapshot_preview1\" \"args_sizes_get\" (func $__wasi_args_sizes_get (param i32 i32) (result i32)))\n");
        }
        if (wasiImports.contains("args_get")) {
            watOutput.append("  (import \"wasi_snapshot_preview1\" \"args_get\" (func $__wasi_args_get (param i32 i32) (result i32)))\n");
        }
        if (wasiImports.contains("environ_sizes_get")) {
            watOutput.append("  (import \"wasi_snapshot_preview1\" \"environ_sizes_get\" (func $__wasi_environ_sizes_get (param i32 i32) (result i32)))\n");
        }
        if (wasiImports.contains("environ_get")) {
            watOutput.append("  (import \"wasi_snapshot_preview1\" \"environ_get\" (func $__wasi_environ_get (param i32 i32) (result i32)))\n");
        }
        if (wasiImports.contains("clock_time_get")) {
            watOutput.append("  (import \"wasi_snapshot_preview1\" \"clock_time_get\" (func $__wasi_clock_time_get (param i32 i64 i32) (result i32)))\n");
        }
        if (wasiImports.contains("random_get")) {
            watOutput.append("  (import \"wasi_snapshot_preview1\" \"random_get\" (func $__wasi_random_get (param i32 i32) (result i32)))\n");
        }
    }
}
