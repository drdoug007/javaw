package com.example.javaw.js;

import com.example.javaw.TypeMapper;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility for discovering and extracting @JS annotations and inline JavaScript methods.
 */
public class JsAnnotationHelper {

    public static Map<String, JsSnippetModel> collectJsSnippets(CompilationUnit cu) {
        Map<String, JsSnippetModel> snippets = new LinkedHashMap<>();
        List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);

        for (ClassOrInterfaceDeclaration cid : classes) {
            String className = cid.getNameAsString();
            for (MethodDeclaration md : cid.getMethods()) {
                Optional<AnnotationExpr> jsAnnoOpt = getJsAnnotation(md);
                if (jsAnnoOpt.isPresent()) {
                    AnnotationExpr jsAnno = jsAnnoOpt.get();
                    String snippetCode = extractAnnotationValue(jsAnno);
                    String methodName = md.getNameAsString();
                    String importName = "js_inline_" + className + "_" + methodName;
                    String returnJavaType = md.getType().asString();
                    String returnWasmType = TypeMapper.mapType(md.getType());
                    boolean isStatic = md.isStatic();
                    boolean isCoerced = hasCoerceAnnotation(md);

                    JsSnippetModel model = new JsSnippetModel(
                            className,
                            methodName,
                            importName,
                            snippetCode,
                            returnJavaType,
                            returnWasmType,
                            isStatic,
                            isCoerced
                    );

                    for (Parameter p : md.getParameters()) {
                        String pName = p.getNameAsString();
                        String pJavaType = p.getType().asString();
                        String pWasmType = TypeMapper.mapType(p.getType());
                        if (pWasmType == null) pWasmType = "i32";
                        boolean pCoerced = isCoerced || hasCoerceAnnotation(p);
                        model.addParameter(pName, pJavaType, pWasmType, pCoerced);
                    }

                    snippets.put(importName, model);
                }
            }
        }
        return snippets;
    }

    public static Optional<AnnotationExpr> getJsAnnotation(MethodDeclaration md) {
        for (AnnotationExpr anno : md.getAnnotations()) {
            String name = anno.getNameAsString();
            if (name.equals("JS") || name.equals("com.example.javaw.js.JS") ||
                name.equals("JS.Code") || name.equals("Code")) {
                return Optional.of(anno);
            }
        }
        return Optional.empty();
    }

    public static boolean hasCoerceAnnotation(MethodDeclaration md) {
        for (AnnotationExpr anno : md.getAnnotations()) {
            String name = anno.getNameAsString();
            if (name.equals("Coerce") || name.equals("JS.Coerce") || name.endsWith(".Coerce")) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasCoerceAnnotation(Parameter p) {
        for (AnnotationExpr anno : p.getAnnotations()) {
            String name = anno.getNameAsString();
            if (name.equals("Coerce") || name.equals("JS.Coerce") || name.endsWith(".Coerce")) {
                return true;
            }
        }
        return false;
    }

    public static String extractAnnotationValue(AnnotationExpr anno) {
        if (anno.isSingleMemberAnnotationExpr()) {
            SingleMemberAnnotationExpr sm = anno.asSingleMemberAnnotationExpr();
            if (sm.getMemberValue().isStringLiteralExpr()) {
                return sm.getMemberValue().asStringLiteralExpr().getValue();
            }
            return sm.getMemberValue().toString();
        } else if (anno.isNormalAnnotationExpr()) {
            NormalAnnotationExpr norm = anno.asNormalAnnotationExpr();
            for (MemberValuePair pair : norm.getPairs()) {
                if (pair.getNameAsString().equals("value")) {
                    if (pair.getValue().isStringLiteralExpr()) {
                        return pair.getValue().asStringLiteralExpr().getValue();
                    }
                    return pair.getValue().toString();
                }
            }
        }
        return "";
    }

    public static String snippetsToJson(Map<String, JsSnippetModel> snippets) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        int count = 0;
        for (JsSnippetModel snippet : snippets.values()) {
            if (count > 0) sb.append(",");
            sb.append(snippet.toJson());
            count++;
        }
        sb.append("]");
        return sb.toString();
    }
}
