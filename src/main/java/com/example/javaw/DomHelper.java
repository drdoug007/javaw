package com.example.javaw;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DomHelper {

    public static Set<String> collectDomImports(CompilationUnit cu) {
        Set<String> imports = new LinkedHashSet<>();
        List<MethodCallExpr> calls = cu.findAll(MethodCallExpr.class);
        for (MethodCallExpr call : calls) {
            String mName = call.getNameAsString();
            if (isDomStaticMethod(mName) || isDomInstanceMethod(mName)) {
                imports.add(mName);
            }
        }
        return imports;
    }

    public static boolean isDomStaticMethod(String mName) {
        return mName.equals("getElementById") ||
                mName.equals("createElement") ||
                mName.equals("getBody");
    }

    public static boolean isDomInstanceMethod(String mName) {
        return mName.equals("setInnerText") ||
                mName.equals("setAttribute") ||
                mName.equals("appendChild") ||
                mName.equals("addEventListener");
    }

    public static void emitDomImports(Set<String> domImports, StringBuilder watOutput) {
        if (domImports.contains("getElementById")) {
            watOutput.append("  (import \"dom\" \"getElementById\" (func $dom_getElementById (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("createElement")) {
            watOutput.append("  (import \"dom\" \"createElement\" (func $dom_createElement (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("getBody")) {
            watOutput.append("  (import \"dom\" \"getBody\" (func $dom_getBody (result i32)))\n");
        }
        if (domImports.contains("setInnerText")) {
            watOutput.append("  (import \"dom\" \"setInnerText\" (func $dom_setInnerText (param i32 i32 i32)))\n");
        }
        if (domImports.contains("setAttribute")) {
            watOutput.append("  (import \"dom\" \"setAttribute\" (func $dom_setAttribute (param i32 i32 i32 i32 i32)))\n");
        }
        if (domImports.contains("appendChild")) {
            watOutput.append("  (import \"dom\" \"appendChild\" (func $dom_appendChild (param i32 i32)))\n");
        }
        if (domImports.contains("addEventListener")) {
            watOutput.append("  (import \"dom\" \"addEventListener\" (func $dom_addEventListener (param i32 i32 i32 i32 i32)))\n");
        }
    }
}
