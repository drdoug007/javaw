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
                } else if (scopeStr.equals("WASI") || scopeStr.endsWith(".WASI") ||
                           scopeStr.equals("WASIFile") || scopeStr.endsWith(".WASIFile") ||
                           scopeStr.equals("WASITerminal") || scopeStr.endsWith(".WASITerminal") ||
                           scopeStr.equals("Terminal") || scopeStr.endsWith(".Terminal")) {
                    imports.add("wasi_all");
                }
            }
        }
        return imports;
    }

    public static void emitWasiImports(Set<String> wasiImports, StringBuilder watOutput) {
        if (wasiImports.isEmpty()) return;

        watOutput.append("  ;; --- WASI Host Imports (wasi_snapshot_preview1) ---\n");
        watOutput.append("  (import \"wasi_snapshot_preview1\" \"fd_write\" (func $__wasi_fd_write (param i32 i32 i32 i32) (result i32)))\n");
        watOutput.append("  (import \"wasi_snapshot_preview1\" \"fd_read\" (func $__wasi_fd_read (param i32 i32 i32 i32) (result i32)))\n");
        watOutput.append("  (import \"wasi_snapshot_preview1\" \"proc_exit\" (func $__wasi_proc_exit (param i32)))\n");
        watOutput.append("  (import \"wasi_snapshot_preview1\" \"args_sizes_get\" (func $__wasi_args_sizes_get (param i32 i32) (result i32)))\n");
        watOutput.append("  (import \"wasi_snapshot_preview1\" \"args_get\" (func $__wasi_args_get (param i32 i32) (result i32)))\n");
        watOutput.append("  (import \"wasi_snapshot_preview1\" \"environ_sizes_get\" (func $__wasi_environ_sizes_get (param i32 i32) (result i32)))\n");
        watOutput.append("  (import \"wasi_snapshot_preview1\" \"environ_get\" (func $__wasi_environ_get (param i32 i32) (result i32)))\n");
        watOutput.append("  (import \"wasi_snapshot_preview1\" \"clock_time_get\" (func $__wasi_clock_time_get (param i32 i64 i32) (result i32)))\n");
        watOutput.append("  (import \"wasi_snapshot_preview1\" \"random_get\" (func $__wasi_random_get (param i32 i32) (result i32)))\n");
        watOutput.append("  (import \"wasi_snapshot_preview1\" \"path_open\" (func $__wasi_path_open (param i32 i32 i32 i32 i32 i64 i64 i32 i32) (result i32)))\n");
        watOutput.append("  (import \"wasi_snapshot_preview1\" \"fd_close\" (func $__wasi_fd_close (param i32) (result i32)))\n");
        watOutput.append("  (import \"wasi_snapshot_preview1\" \"fd_seek\" (func $__wasi_fd_seek (param i32 i64 i32 i32) (result i32)))\n");
        watOutput.append("  (import \"wasi_snapshot_preview1\" \"fd_filestat_get\" (func $__wasi_fd_filestat_get (param i32 i32) (result i32)))\n");
        watOutput.append("  (import \"wasi_snapshot_preview1\" \"path_filestat_get\" (func $__wasi_path_filestat_get (param i32 i32 i32 i32 i32) (result i32)))\n");
        watOutput.append("  (import \"wasi_snapshot_preview1\" \"path_create_directory\" (func $__wasi_path_create_directory (param i32 i32 i32) (result i32)))\n");
        watOutput.append("  (import \"wasi_snapshot_preview1\" \"path_unlink_file\" (func $__wasi_path_unlink_file (param i32 i32 i32) (result i32)))\n");
    }
}
