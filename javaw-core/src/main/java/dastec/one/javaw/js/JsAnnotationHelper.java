package dastec.one.javaw.js;

import dastec.one.javaw.TypeMapper;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility for discovering and extracting @JS annotations, @JS.Import bindings, and inline JavaScript methods.
 */
public class JsAnnotationHelper {

    public static Map<String, JsSnippetModel> collectJsSnippets(CompilationUnit cu) {
        Map<String, JsSnippetModel> snippets = new LinkedHashMap<>();
        addReferencedBuiltInSnippets(cu, snippets);
        List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);

        for (ClassOrInterfaceDeclaration cid : classes) {
            String className = cid.getNameAsString();
            Optional<AnnotationExpr> classImportOpt = getClassImportAnnotation(cid);
            String globalName = null;
            if (classImportOpt.isPresent()) {
                globalName = extractAnnotationValue(classImportOpt.get());
                if (globalName == null || globalName.trim().isEmpty()) {
                    globalName = className;
                    if (globalName.startsWith("JS") && globalName.length() > 2) {
                        globalName = globalName.substring(2);
                    }
                }
            }

            Map<String, Integer> methodCounts = new HashMap<>();

            for (MethodDeclaration md : cid.getMethods()) {
                Optional<AnnotationExpr> jsAnnoOpt = getJsAnnotation(md);
                Optional<AnnotationExpr> methodImportOpt = getMethodImportAnnotation(md);
                boolean isImported = classImportOpt.isPresent() || methodImportOpt.isPresent();

                if (jsAnnoOpt.isPresent() || isImported) {
                    String methodName = md.getNameAsString();
                    String baseKey = className + "_" + methodName;
                    int count = methodCounts.getOrDefault(baseKey, 0);
                    methodCounts.put(baseKey, count + 1);
                    String importName = count == 0 ? "js_inline_" + baseKey : "js_inline_" + baseKey + "_" + count;

                    String returnJavaType = md.getType().asString();
                    String returnWasmType = TypeMapper.mapType(md.getType());
                    boolean isStatic = md.isStatic();
                    boolean isCoerced = hasCoerceAnnotation(md) || hasCoerceAnnotation(cid);

                    String snippetCode;
                    if (jsAnnoOpt.isPresent()) {
                        snippetCode = extractAnnotationValue(jsAnnoOpt.get());
                    } else if (methodImportOpt.isPresent() && !extractAnnotationValue(methodImportOpt.get()).isEmpty()) {
                        String target = extractAnnotationValue(methodImportOpt.get());
                        snippetCode = generateDirectImportSnippet(target, md);
                    } else {
                        snippetCode = generateImportMethodSnippet(globalName != null ? globalName : className, md);
                    }

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

                    if (!isStatic) {
                        model.addParameter("this", className, "i32", false);
                    }

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

    public static String generateImportMethodSnippet(String globalName, MethodDeclaration md) {
        String mName = md.getNameAsString();
        List<String> paramNames = new ArrayList<>();
        for (Parameter p : md.getParameters()) {
            paramNames.add(p.getNameAsString());
        }
        String argsJoined = String.join(", ", paramNames);
        boolean isVoid = md.getType().isVoidType();

        if (md.isStatic()) {
            if (mName.equals("create") || mName.equals("newInstance") || mName.equals("ofInstance")) {
                return "return new " + globalName + "(" + argsJoined + ");";
            }
            if (paramNames.isEmpty() && (mName.equals("PI") || mName.equals("E") || mName.equals("LN2") ||
                    mName.equals("LN10") || mName.equals("LOG2E") || mName.equals("LOG10E") ||
                    mName.equals("SQRT1_2") || mName.equals("SQRT2") || mName.equals("EPSILON"))) {
                return "return " + globalName + "." + mName + ";";
            }
            if (isVoid) {
                return globalName + "." + mName + "(" + argsJoined + ");";
            }
            return "return " + globalName + "." + mName + "(" + argsJoined + ");";
        } else {
            if (isVoid) {
                return "this." + mName + "(" + argsJoined + ");";
            }
            return "return this." + mName + "(" + argsJoined + ");";
        }
    }

    public static String generateDirectImportSnippet(String target, MethodDeclaration md) {
        List<String> paramNames = new ArrayList<>();
        for (Parameter p : md.getParameters()) {
            paramNames.add(p.getNameAsString());
        }
        String argsJoined = String.join(", ", paramNames);
        boolean isVoid = md.getType().isVoidType();
        if (isVoid) {
            return target + "(" + argsJoined + ");";
        }
        return "return " + target + "(" + argsJoined + ");";
    }

    public static Optional<AnnotationExpr> getClassImportAnnotation(ClassOrInterfaceDeclaration cid) {
        for (AnnotationExpr anno : cid.getAnnotations()) {
            String name = anno.getNameAsString();
            if (name.equals("JS.Import") || name.equals("Import") || name.endsWith(".JS.Import") || name.endsWith(".Import")) {
                return Optional.of(anno);
            }
        }
        return Optional.empty();
    }

    public static Optional<AnnotationExpr> getMethodImportAnnotation(MethodDeclaration md) {
        for (AnnotationExpr anno : md.getAnnotations()) {
            String name = anno.getNameAsString();
            if (name.equals("JS.Import") || name.equals("Import") || name.endsWith(".JS.Import") || name.endsWith(".Import")) {
                return Optional.of(anno);
            }
        }
        return Optional.empty();
    }

    public static Optional<AnnotationExpr> getJsAnnotation(MethodDeclaration md) {
        for (AnnotationExpr anno : md.getAnnotations()) {
            String name = anno.getNameAsString();
            if (name.equals("JS") || name.equals("dastec.one.javaw.js.JS") ||
                name.equals("JS.Code") || name.equals("Code")) {
                return Optional.of(anno);
            }
        }
        return Optional.empty();
    }

    public static boolean hasCoerceAnnotation(ClassOrInterfaceDeclaration cid) {
        for (AnnotationExpr anno : cid.getAnnotations()) {
            String name = anno.getNameAsString();
            if (name.equals("Coerce") || name.equals("JS.Coerce") || name.endsWith(".Coerce")) {
                return true;
            }
        }
        return false;
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

    private static void addReferencedBuiltInSnippets(CompilationUnit cu, Map<String, JsSnippetModel> snippets) {
        if (cu == null) return;
        List<com.github.javaparser.ast.expr.MethodCallExpr> calls = cu.findAll(com.github.javaparser.ast.expr.MethodCallExpr.class);
        Map<String, String> varTypes = new HashMap<>();
        cu.findAll(com.github.javaparser.ast.body.VariableDeclarator.class).forEach(v -> {
            varTypes.put(v.getNameAsString(), v.getType().asString());
        });
        cu.findAll(com.github.javaparser.ast.body.Parameter.class).forEach(p -> {
            varTypes.put(p.getNameAsString(), p.getType().asString());
        });

        for (com.github.javaparser.ast.expr.MethodCallExpr call : calls) {
            String mName = call.getNameAsString();
            String scopeStr = call.getScope().isPresent() ? call.getScope().get().toString() : "";
            String shortTarget = scopeStr.contains(".") ? scopeStr.substring(scopeStr.lastIndexOf('.') + 1) : scopeStr;
            String inferredType = varTypes.get(scopeStr);
            if (inferredType != null && inferredType.contains(".")) {
                inferredType = inferredType.substring(inferredType.lastIndexOf('.') + 1);
            }

            if ("JSMath".equals(shortTarget) || "JSMath".equals(inferredType)) {
                if (mName.equals("hypot")) addMathSnippet(snippets, "hypot", "return Math.hypot(a, b);", "double", "f64", true, "a", "double", "f64", "b", "double", "f64");
                else if (mName.equals("sin")) addMathSnippet(snippets, "sin", "return Math.sin(x);", "double", "f64", true, "x", "double", "f64");
                else if (mName.equals("cos")) addMathSnippet(snippets, "cos", "return Math.cos(x);", "double", "f64", true, "x", "double", "f64");
                else if (mName.equals("tan")) addMathSnippet(snippets, "tan", "return Math.tan(x);", "double", "f64", true, "x", "double", "f64");
                else if (mName.equals("sqrt")) addMathSnippet(snippets, "sqrt", "return Math.sqrt(x);", "double", "f64", true, "x", "double", "f64");
                else if (mName.equals("pow")) addMathSnippet(snippets, "pow", "return Math.pow(base, exp);", "double", "f64", true, "base", "double", "f64", "exp", "double", "f64");
                else if (mName.equals("floor")) addMathSnippet(snippets, "floor", "return Math.floor(x);", "double", "f64", true, "x", "double", "f64");
                else if (mName.equals("ceil")) addMathSnippet(snippets, "ceil", "return Math.ceil(x);", "double", "f64", true, "x", "double", "f64");
                else if (mName.equals("round")) addMathSnippet(snippets, "round", "return Math.round(x);", "double", "f64", true, "x", "double", "f64");
                else if (mName.equals("abs")) addMathSnippet(snippets, "abs", "return Math.abs(x);", "double", "f64", true, "x", "double", "f64");
                else if (mName.equals("random")) addMathSnippet(snippets, "random", "return Math.random();", "double", "f64", true);
                else if (mName.equals("PI")) addMathSnippet(snippets, "PI", "return Math.PI;", "double", "f64", true);
                else if (mName.equals("E")) addMathSnippet(snippets, "E", "return Math.E;", "double", "f64", true);
            } else if ("JSDate".equals(shortTarget) || "JSDate".equals(inferredType)) {
                if (mName.equals("create")) {
                    if (call.getArguments().isEmpty()) {
                        addDateSnippet(snippets, "create", "return new Date();", "JSDate", "i32", true);
                    } else {
                        addDateSnippet(snippets, "create", "return new Date(ms);", "JSDate", "i32", true, "ms", "double", "f64");
                    }
                }
                else if (mName.equals("now")) addDateSnippet(snippets, "now", "return Date.now();", "double", "f64", true);
                else if (mName.equals("getTime")) addDateSnippet(snippets, "getTime", "return this.getTime();", "double", "f64", false);
                else if (mName.equals("getFullYear")) addDateSnippet(snippets, "getFullYear", "return this.getFullYear();", "int", "i32", false);
                else if (mName.equals("getMonth")) addDateSnippet(snippets, "getMonth", "return this.getMonth();", "int", "i32", false);
                else if (mName.equals("getDate")) addDateSnippet(snippets, "getDate", "return this.getDate();", "int", "i32", false);
                else if (mName.equals("toISOString")) addDateSnippet(snippets, "toISOString", "return this.toISOString();", "String", "i32", false);
                else if (mName.equals("toString")) addDateSnippet(snippets, "toString", "return this.toString();", "String", "i32", false);
            } else if ("JSJSON".equals(shortTarget) || "JSJSON".equals(inferredType)) {
                if (mName.equals("stringify")) addJsonSnippet(snippets, "stringify", "return JSON.stringify(val);", "String", "i32", true, "val", "JSValue", "i32");
                else if (mName.equals("parse")) addJsonSnippet(snippets, "parse", "return JSON.parse(text);", "JSValue", "i32", true, "text", "String", "i32");
            } else if ("JSConsole".equals(shortTarget) || "JSConsole".equals(inferredType)) {
                if (mName.equals("log")) addConsoleSnippet(snippets, "log", "console.log(msg);", "void", null, true, "msg", "String", "i32");
                else if (mName.equals("info")) addConsoleSnippet(snippets, "info", "console.info(msg);", "void", null, true, "msg", "String", "i32");
                else if (mName.equals("warn")) addConsoleSnippet(snippets, "warn", "console.warn(msg);", "void", null, true, "msg", "String", "i32");
                else if (mName.equals("error")) addConsoleSnippet(snippets, "error", "console.error(msg);", "void", null, true, "msg", "String", "i32");
            } else if ("JSStorage".equals(shortTarget) || "JSStorage".equals(inferredType)) {
                if (mName.equals("setItem")) addStorageSnippet(snippets, "setItem", "localStorage.setItem(k, v);", "void", null, true, "k", "String", "i32", "v", "String", "i32");
                else if (mName.equals("getItem")) addStorageSnippet(snippets, "getItem", "return localStorage.getItem(k);", "String", "i32", true, "k", "String", "i32");
                else if (mName.equals("removeItem")) addStorageSnippet(snippets, "removeItem", "localStorage.removeItem(k);", "void", null, true, "k", "String", "i32");
                else if (mName.equals("clear")) addStorageSnippet(snippets, "clear", "localStorage.clear();", "void", null, true);
            }
        }
    }

    private static void addMathSnippet(Map<String, JsSnippetModel> snippets, String name, String code, String retJava, String retWasm, boolean isStatic, String... params) {
        addGenericSnippet(snippets, "JSMath", name, code, retJava, retWasm, isStatic, params);
    }

    private static void addDateSnippet(Map<String, JsSnippetModel> snippets, String name, String code, String retJava, String retWasm, boolean isStatic, String... params) {
        addGenericSnippet(snippets, "JSDate", name, code, retJava, retWasm, isStatic, params);
    }

    private static void addJsonSnippet(Map<String, JsSnippetModel> snippets, String name, String code, String retJava, String retWasm, boolean isStatic, String... params) {
        addGenericSnippet(snippets, "JSJSON", name, code, retJava, retWasm, isStatic, params);
    }

    private static void addConsoleSnippet(Map<String, JsSnippetModel> snippets, String name, String code, String retJava, String retWasm, boolean isStatic, String... params) {
        addGenericSnippet(snippets, "JSConsole", name, code, retJava, retWasm, isStatic, params);
    }

    private static void addStorageSnippet(Map<String, JsSnippetModel> snippets, String name, String code, String retJava, String retWasm, boolean isStatic, String... params) {
        addGenericSnippet(snippets, "JSStorage", name, code, retJava, retWasm, isStatic, params);
    }

    private static void addGenericSnippet(Map<String, JsSnippetModel> snippets, String className, String methodName, String code, String retJava, String retWasm, boolean isStatic, String... params) {
        String importName = "js_inline_" + className + "_" + methodName;
        JsSnippetModel model = new JsSnippetModel(className, methodName, importName, code, retJava, retWasm, isStatic, false);
        if (!isStatic) {
            model.addParameter("this", className, "i32", false);
        }
        for (int i = 0; i < params.length; i += 3) {
            model.addParameter(params[i], params[i + 1], params[i + 2], false);
        }
        snippets.put(importName, model);
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

    public static Optional<AnnotationExpr> getClassExportAnnotation(ClassOrInterfaceDeclaration cid) {
        if (cid == null) return Optional.empty();
        for (AnnotationExpr a : cid.getAnnotations()) {
            String name = a.getNameAsString();
            if (name.equals("Export") || name.equals("JS.Export") || name.endsWith(".Export")) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    public static Optional<AnnotationExpr> getMethodExportAnnotation(MethodDeclaration md) {
        if (md == null) return Optional.empty();
        for (AnnotationExpr a : md.getAnnotations()) {
            String name = a.getNameAsString();
            if (name.equals("Export") || name.equals("JS.Export") || name.endsWith(".Export")) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    public static Map<String, JsExportModel> collectJsExports(CompilationUnit cu, dastec.one.javaw.ClassRegistry classRegistry) {
        Map<String, JsExportModel> exports = new LinkedHashMap<>();
        if (cu == null) return exports;
        List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);

        for (ClassOrInterfaceDeclaration cid : classes) {
            Optional<AnnotationExpr> exportAnnoOpt = getClassExportAnnotation(cid);
            if (!exportAnnoOpt.isPresent()) continue;

            String className = cid.getNameAsString();
            String exportName = extractAnnotationValue(exportAnnoOpt.get());
            if (exportName == null || exportName.trim().isEmpty()) {
                exportName = className;
            }

            dastec.one.javaw.ClassModel cm = classRegistry != null ? classRegistry.getClass(className) : null;
            int structSize = cm != null ? cm.getStructSize() : 8;
            int vtableBase = cm != null ? cm.getVtableBase() : 0;
            boolean hasVtable = cm != null && !cm.getVtableSlots().isEmpty();

            JsExportModel exportModel = new JsExportModel(className, exportName, structSize, vtableBase, hasVtable);
            boolean isClassCoerced = hasCoerceAnnotation(cid);

            // Collect constructors
            if (cid.getConstructors().isEmpty()) {
                String initFunc = className + "__init";
                exportModel.addConstructor(initFunc, Collections.emptyList());
            } else {
                int consIdx = 0;
                for (com.github.javaparser.ast.body.ConstructorDeclaration cd : cid.getConstructors()) {
                    String initFunc = className + "__init" + (consIdx == 0 ? "" : "_" + consIdx);
                    List<JsSnippetModel.JsParam> params = new ArrayList<>();
                    for (Parameter p : cd.getParameters()) {
                        String pName = p.getNameAsString();
                        String pJavaType = p.getType().asString();
                        String pWasmType = TypeMapper.mapType(p.getType());
                        if (pWasmType == null) pWasmType = "i32";
                        boolean coerced = hasCoerceAnnotation(p) || isClassCoerced;
                        params.add(new JsSnippetModel.JsParam(pName, pJavaType, pWasmType, coerced));
                    }
                    exportModel.addConstructor(initFunc, params);
                    consIdx++;
                }
            }

            // Collect methods
            for (MethodDeclaration md : cid.getMethods()) {
                if (md.isPrivate()) continue;
                String methodName = md.getNameAsString();
                Optional<AnnotationExpr> methodExportOpt = getMethodExportAnnotation(md);
                String methodExportName = methodName;
                if (methodExportOpt.isPresent()) {
                    String customName = extractAnnotationValue(methodExportOpt.get());
                    if (customName != null && !customName.trim().isEmpty()) {
                        methodExportName = customName;
                    }
                }

                boolean isStatic = md.isStatic();
                String wasmFuncName;
                if (isStatic) {
                    wasmFuncName = methodName;
                } else {
                    wasmFuncName = className + "_" + methodName;
                }

                String returnJavaType = md.getType().asString();
                String returnWasmType = TypeMapper.mapType(md.getType());
                boolean isMethodCoerced = isClassCoerced || hasCoerceAnnotation(md);

                List<JsSnippetModel.JsParam> params = new ArrayList<>();
                for (Parameter p : md.getParameters()) {
                    String pName = p.getNameAsString();
                    String pJavaType = p.getType().asString();
                    String pWasmType = TypeMapper.mapType(p.getType());
                    if (pWasmType == null) pWasmType = "i32";
                    boolean coerced = isMethodCoerced || hasCoerceAnnotation(p);
                    params.add(new JsSnippetModel.JsParam(pName, pJavaType, pWasmType, coerced));
                }

                exportModel.addMethod(methodName, methodExportName, wasmFuncName, returnJavaType, returnWasmType, isStatic, isMethodCoerced, params);
            }

            // Collect fields
            for (com.github.javaparser.ast.body.FieldDeclaration fd : cid.getFields()) {
                if (fd.isPrivate()) continue;
                boolean isStatic = fd.isStatic();
                for (com.github.javaparser.ast.body.VariableDeclarator vd : fd.getVariables()) {
                    String fName = vd.getNameAsString();
                    String fJavaType = vd.getType().asString();
                    String fWasmType = TypeMapper.mapType(vd.getType());
                    if (fWasmType == null) fWasmType = "i32";
                    dastec.one.javaw.FieldModel fm = cm != null ? cm.getFields().get(fName) : null;
                    int offset = fm != null ? fm.getOffset() : 0;
                    int size = fm != null ? fm.getSize() : 4;
                    exportModel.addField(fName, fJavaType, fWasmType, offset, size, isStatic);
                }
            }

            exports.put(exportName, exportModel);
        }

        return exports;
    }

    public static String exportsToJson(Map<String, JsExportModel> exports) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        int count = 0;
        for (JsExportModel exp : exports.values()) {
            if (count > 0) sb.append(",");
            sb.append(exp.toJson());
            count++;
        }
        sb.append("]");
        return sb.toString();
    }

    public static List<String> extractAnnotationStringArray(AnnotationExpr anno) {
        List<String> result = new ArrayList<>();
        if (anno.isSingleMemberAnnotationExpr()) {
            com.github.javaparser.ast.expr.Expression val = anno.asSingleMemberAnnotationExpr().getMemberValue();
            if (val.isStringLiteralExpr()) {
                result.add(val.asStringLiteralExpr().getValue());
            } else if (val.isArrayInitializerExpr()) {
                for (com.github.javaparser.ast.expr.Expression elem : val.asArrayInitializerExpr().getValues()) {
                    if (elem.isStringLiteralExpr()) {
                        result.add(elem.asStringLiteralExpr().getValue());
                    }
                }
            }
        } else if (anno.isNormalAnnotationExpr()) {
            for (MemberValuePair pair : anno.asNormalAnnotationExpr().getPairs()) {
                if (pair.getNameAsString().equals("value")) {
                    com.github.javaparser.ast.expr.Expression val = pair.getValue();
                    if (val.isStringLiteralExpr()) {
                        result.add(val.asStringLiteralExpr().getValue());
                    } else if (val.isArrayInitializerExpr()) {
                        for (com.github.javaparser.ast.expr.Expression elem : val.asArrayInitializerExpr().getValues()) {
                            if (elem.isStringLiteralExpr()) {
                                result.add(elem.asStringLiteralExpr().getValue());
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public static String extractAnnotationMember(AnnotationExpr anno, String memberName) {
        if (anno.isNormalAnnotationExpr()) {
            for (MemberValuePair pair : anno.asNormalAnnotationExpr().getPairs()) {
                if (pair.getNameAsString().equals(memberName)) {
                    if (pair.getValue().isStringLiteralExpr()) {
                        return pair.getValue().asStringLiteralExpr().getValue();
                    }
                    return pair.getValue().toString();
                }
            }
        } else if (anno.isSingleMemberAnnotationExpr() && "value".equals(memberName)) {
            return extractAnnotationValue(anno);
        }
        return "";
    }

    public static Optional<String> readFileContent(String relativePath, Path baseDir) {
        if (relativePath == null || relativePath.trim().isEmpty()) return Optional.empty();
        List<Path> searchDirs = new ArrayList<>();
        if (baseDir != null) {
            searchDirs.add(baseDir.resolve(relativePath));
            searchDirs.add(baseDir);
        }
        searchDirs.add(Paths.get(relativePath));
        searchDirs.add(Paths.get("src/main/resources").resolve(relativePath));
        searchDirs.add(Paths.get("src/test/resources").resolve(relativePath));
        searchDirs.add(Paths.get("src/test/java").resolve(relativePath));

        for (Path p : searchDirs) {
            try {
                if (Files.isRegularFile(p)) {
                    return Optional.of(Files.readString(p, StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {}
        }
        return Optional.empty();
    }

    public static List<String> collectJsCodeBlocks(CompilationUnit cu) {
        return collectJsCodeBlocks(cu, null);
    }

    public static List<String> collectJsCodeBlocks(CompilationUnit cu, Path baseDir) {
        List<String> codeBlocks = new ArrayList<>();
        if (cu == null) return codeBlocks;
        Path dir = baseDir;
        if (dir == null && cu.getStorage().isPresent()) {
            dir = cu.getStorage().get().getDirectory();
        }

        List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
        for (ClassOrInterfaceDeclaration cid : classes) {
            for (AnnotationExpr a : cid.getAnnotations()) {
                String name = a.getNameAsString();
                if (name.equals("Code") || name.equals("JS.Code") || name.endsWith(".JS.Code") || name.endsWith(".Code")) {
                    String code = extractAnnotationValue(a);
                    if (!code.isEmpty()) codeBlocks.add(code);
                } else if (name.equals("Include") || name.equals("JS.Code.Include") || name.equals("Code.Include") || name.endsWith(".Include")) {
                    List<String> paths = extractAnnotationStringArray(a);
                    for (String path : paths) {
                        Optional<String> content = readFileContent(path, dir);
                        if (content.isPresent()) {
                            codeBlocks.add(content.get());
                        } else {
                            codeBlocks.add(path);
                        }
                    }
                }
            }

            for (MethodDeclaration md : cid.getMethods()) {
                for (AnnotationExpr a : md.getAnnotations()) {
                    String name = a.getNameAsString();
                    if (name.equals("Code") || name.equals("JS.Code") || name.endsWith(".JS.Code") || name.endsWith(".Code")) {
                        String code = extractAnnotationValue(a);
                        if (!code.isEmpty()) codeBlocks.add(code);
                    } else if (name.equals("Include") || name.equals("JS.Code.Include") || name.equals("Code.Include") || name.endsWith(".Include")) {
                        List<String> paths = extractAnnotationStringArray(a);
                        for (String path : paths) {
                            Optional<String> content = readFileContent(path, dir);
                            if (content.isPresent()) {
                                codeBlocks.add(content.get());
                            } else {
                                codeBlocks.add(path);
                            }
                        }
                    }
                }
            }
        }

        return codeBlocks;
    }

    public static String codeBlocksToJson(List<String> codeBlocks) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < codeBlocks.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(codeBlocks.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    public static Map<String, String> collectJsResources(CompilationUnit cu) {
        return collectJsResources(cu, null);
    }

    public static Map<String, String> collectJsResources(CompilationUnit cu, Path baseDir) {
        Map<String, String> resources = new LinkedHashMap<>();
        if (cu == null) return resources;
        Path dir = baseDir;
        if (dir == null && cu.getStorage().isPresent()) {
            dir = cu.getStorage().get().getDirectory();
        }

        List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
        for (ClassOrInterfaceDeclaration cid : classes) {
            processResourceAnnotations(cid.getAnnotations(), dir, resources);

            for (com.github.javaparser.ast.body.FieldDeclaration fd : cid.getFields()) {
                processResourceAnnotations(fd.getAnnotations(), dir, resources);
            }

            for (MethodDeclaration md : cid.getMethods()) {
                processResourceAnnotations(md.getAnnotations(), dir, resources);
            }
        }

        // Also check JSResource method calls in AST with string literals
        List<com.github.javaparser.ast.expr.MethodCallExpr> calls = cu.findAll(com.github.javaparser.ast.expr.MethodCallExpr.class);
        for (com.github.javaparser.ast.expr.MethodCallExpr call : calls) {
            String scope = call.getScope().map(Object::toString).orElse("");
            if (scope.equals("JSResource") || scope.endsWith(".JSResource")) {
                if (call.getArguments().size() > 0 && call.getArgument(0).isStringLiteralExpr()) {
                    String pathArg = call.getArgument(0).asStringLiteralExpr().getValue();
                    if (!resources.containsKey(pathArg)) {
                        Optional<String> content = readFileContent(pathArg, dir);
                        if (content.isPresent()) {
                            resources.put(pathArg, content.get());
                        }
                    }
                }
            }
        }

        return resources;
    }

    private static void processResourceAnnotations(List<AnnotationExpr> annotations, Path dir, Map<String, String> resources) {
        for (AnnotationExpr a : annotations) {
            String name = a.getNameAsString();
            if (name.equals("Resource") || name.equals("JS.Resource") || name.equals("JSResource") ||
                name.endsWith(".Resource") || name.endsWith(".JSResource")) {
                String val = extractAnnotationValue(a);
                String path = extractAnnotationMember(a, "path");
                if (path.isEmpty()) path = val;

                Optional<String> fileContent = readFileContent(val, dir);
                if (fileContent.isPresent()) {
                    resources.put(path, fileContent.get());
                } else {
                    resources.put(path, val);
                }
            }
        }
    }

    public static String resourcesToJson(Map<String, String> resources) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        int count = 0;
        for (Map.Entry<String, String> entry : resources.entrySet()) {
            if (count > 0) sb.append(",");
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":\"")
              .append(escapeJson(entry.getValue())).append("\"");
            count++;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
