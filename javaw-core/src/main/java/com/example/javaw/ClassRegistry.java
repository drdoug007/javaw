package com.example.javaw;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import java.util.*;

/**
 * Registry and layout calculator for Java classes, fields, vtables, and function table slots.
 */
public class ClassRegistry {
    private final Map<String, ClassModel> classes = new LinkedHashMap<>();
    private final Map<String, RecordModel> records = new LinkedHashMap<>();
    private final List<String> tableFunctions = new ArrayList<>();
    private final Map<String, String> typeSignatures = new LinkedHashMap<>(); // sigWat -> sigName
    private int typeSigCounter = 0;

    public static ClassRegistry build(CompilationUnit cu) {
        ClassRegistry registry = new ClassRegistry();
        registry.initFromCu(cu);
        return registry;
    }

    private void initFromCu(CompilationUnit cu) {
        List<ClassOrInterfaceDeclaration> classDecls = cu.findAll(ClassOrInterfaceDeclaration.class);
        int classIdCounter = 1;

        // Phase 1: Register classes and basic definitions
        for (ClassOrInterfaceDeclaration cd : classDecls) {
            String className = cd.getNameAsString();
            String superClassName = null;
            if (!cd.getExtendedTypes().isEmpty()) {
                superClassName = cd.getExtendedTypes(0).getNameAsString();
            }
            ClassModel cm = new ClassModel(className, superClassName, cd.isInterface(), classIdCounter++);
            if (com.example.javaw.js.JsAnnotationHelper.getClassExportAnnotation(cd).isPresent()) {
                cm.setExported(true);
            }
            classes.put(className, cm);

            // Collect static and instance fields
            for (FieldDeclaration fd : cd.getFields()) {
                boolean isStatic = fd.isStatic();
                for (VariableDeclarator vd : fd.getVariables()) {
                    String fName = vd.getNameAsString();
                    String fType = vd.getType().asString();
                    String wasmType = TypeMapper.mapType(vd.getType());
                    if (wasmType == null) wasmType = "i32";
                    int size = "v128".equals(wasmType) ? 16 : (("i64".equals(wasmType) || "f64".equals(wasmType)) ? 8 : 4);
                    // Offset will be calculated in Phase 2 for instance fields
                    cm.addField(new FieldModel(fName, fType, wasmType, 0, size, isStatic));
                }
            }

            // Collect constructors
            for (ConstructorDeclaration cons : cd.getConstructors()) {
                List<String> paramNames = new ArrayList<>();
                List<String> paramTypeNames = new ArrayList<>();
                List<String> paramWasmTypes = new ArrayList<>();
                for (Parameter p : cons.getParameters()) {
                    paramNames.add(p.getNameAsString());
                    paramTypeNames.add(p.getType().asString());
                    String pWasm = TypeMapper.mapType(p.getType());
                    paramWasmTypes.add(pWasm != null ? pWasm : "i32");
                }
                String funcName = "$" + className + "__init";
                if (!cm.getConstructors().isEmpty()) {
                    funcName += "_" + cm.getConstructors().size();
                }
                MethodModel mm = new MethodModel("<init>", className, funcName, "void", null,
                        paramNames, paramTypeNames, paramWasmTypes, false, true);
                cm.addConstructor(mm);
            }

            // Collect methods
            for (MethodDeclaration md : cd.getMethods()) {
                String mName = md.getNameAsString();
                String retTypeName = md.getType().asString();
                String retWasmType = TypeMapper.mapType(md.getType());
                boolean isStatic = md.isStatic();
                List<String> paramNames = new ArrayList<>();
                List<String> paramTypeNames = new ArrayList<>();
                List<String> paramWasmTypes = new ArrayList<>();
                for (Parameter p : md.getParameters()) {
                    paramNames.add(p.getNameAsString());
                    paramTypeNames.add(p.getType().asString());
                    String pWasm = TypeMapper.mapType(p.getType());
                    paramWasmTypes.add(pWasm != null ? pWasm : "i32");
                }

                String funcName;
                if (isStatic) {
                    funcName = "$" + mName;
                } else if (!cm.isOopClass() && !com.example.javaw.js.JsAnnotationHelper.getClassImportAnnotation(cd).isPresent()) {
                    funcName = "$" + mName;
                } else {
                    funcName = "$" + className + "_" + mName;
                }

                MethodModel mm = new MethodModel(mName, className, funcName, retTypeName, retWasmType,
                        paramNames, paramTypeNames, paramWasmTypes, isStatic, false);
                cm.addMethod(mm);
            }
        }

        // Register Java records
        List<RecordDeclaration> recordDecls = cu.findAll(RecordDeclaration.class);
        for (RecordDeclaration rd : recordDecls) {
            String recName = rd.getNameAsString();
            RecordModel rm = new RecordModel(recName);
            records.put(recName, rm);

            for (Parameter p : rd.getParameters()) {
                String cName = p.getNameAsString();
                String cType = p.getType().asString();
                String wasmType = TypeMapper.mapType(p.getType());
                if (wasmType == null) wasmType = "i32";
                int size = "v128".equals(wasmType) ? 16 : (("i64".equals(wasmType) || "f64".equals(wasmType)) ? 8 : 4);
                rm.addComponent(new FieldModel(cName, cType, wasmType, 0, size, false));
            }

            for (MethodDeclaration md : rd.getMethods()) {
                String mName = md.getNameAsString();
                String retTypeName = md.getType().asString();
                String retWasmType = TypeMapper.mapType(md.getType());
                boolean isStatic = md.isStatic();
                List<String> paramNames = new ArrayList<>();
                List<String> paramTypeNames = new ArrayList<>();
                List<String> paramWasmTypes = new ArrayList<>();
                for (Parameter p : md.getParameters()) {
                    paramNames.add(p.getNameAsString());
                    paramTypeNames.add(p.getType().asString());
                    String pWasm = TypeMapper.mapType(p.getType());
                    paramWasmTypes.add(pWasm != null ? pWasm : "i32");
                }
                String funcName = "$" + recName + "_" + mName;
                MethodModel mm = new MethodModel(mName, recName, funcName, retTypeName, retWasmType,
                        paramNames, paramTypeNames, paramWasmTypes, isStatic, false);
                rm.addMethod(mm);
            }
        }

        // Phase 1b: Register Java enums
        List<EnumDeclaration> enumDecls = cu.findAll(EnumDeclaration.class);
        for (EnumDeclaration ed : enumDecls) {
            String enumName = ed.getNameAsString();
            ClassModel cm = new ClassModel(enumName, "Enum", false, classIdCounter++);
            cm.setEnum(true);
            classes.put(enumName, cm);

            // Add standard enum instance fields: name at offset 4, ordinal at offset 8
            cm.addField(new FieldModel("name", "String", "i32", 4, 4, false));
            cm.addField(new FieldModel("ordinal", "int", "i32", 8, 4, false));

            // Register enum constants
            for (EnumConstantDeclaration ecd : ed.getEntries()) {
                String cName = ecd.getNameAsString();
                cm.addEnumConstant(cName);
                if (!ecd.getArguments().isEmpty()) {
                    cm.getEnumConstantArgs().put(cName, new ArrayList<>(ecd.getArguments()));
                }
            }

            // Collect custom fields
            for (FieldDeclaration fd : ed.getFields()) {
                boolean isStatic = fd.isStatic();
                for (VariableDeclarator vd : fd.getVariables()) {
                    String fName = vd.getNameAsString();
                    String fType = vd.getType().asString();
                    String wasmType = TypeMapper.mapType(vd.getType());
                    if (wasmType == null) wasmType = "i32";
                    int size = "v128".equals(wasmType) ? 16 : (("i64".equals(wasmType) || "f64".equals(wasmType)) ? 8 : 4);
                    cm.addField(new FieldModel(fName, fType, wasmType, 0, size, isStatic));
                }
            }

            // Collect constructors
            for (ConstructorDeclaration cons : ed.getConstructors()) {
                List<String> paramNames = new ArrayList<>();
                List<String> paramTypeNames = new ArrayList<>();
                List<String> paramWasmTypes = new ArrayList<>();
                for (Parameter p : cons.getParameters()) {
                    paramNames.add(p.getNameAsString());
                    paramTypeNames.add(p.getType().asString());
                    String pWasm = TypeMapper.mapType(p.getType());
                    paramWasmTypes.add(pWasm != null ? pWasm : "i32");
                }
                String funcName = "$" + enumName + "__init";
                if (!cm.getConstructors().isEmpty()) {
                    funcName += "_" + cm.getConstructors().size();
                }
                MethodModel mm = new MethodModel("<init>", enumName, funcName, "void", null,
                        paramNames, paramTypeNames, paramWasmTypes, false, true);
                cm.addConstructor(mm);
            }

            // Collect methods
            for (MethodDeclaration md : ed.getMethods()) {
                String mName = md.getNameAsString();
                String retTypeName = md.getType().asString();
                String retWasmType = TypeMapper.mapType(md.getType());
                boolean isStatic = md.isStatic();
                List<String> paramNames = new ArrayList<>();
                List<String> paramTypeNames = new ArrayList<>();
                List<String> paramWasmTypes = new ArrayList<>();
                for (Parameter p : md.getParameters()) {
                    paramNames.add(p.getNameAsString());
                    paramTypeNames.add(p.getType().asString());
                    String pWasm = TypeMapper.mapType(p.getType());
                    paramWasmTypes.add(pWasm != null ? pWasm : "i32");
                }

                String funcName = "$" + enumName + "_" + mName;
                MethodModel mm = new MethodModel(mName, enumName, funcName, retTypeName, retWasmType,
                        paramNames, paramTypeNames, paramWasmTypes, isStatic, false);
                cm.addMethod(mm);
            }

            // Pre-register standard enum instance and static methods if not explicitly provided
            if (cm.getMethodByName("name") == null) {
                cm.addMethod(new MethodModel("name", enumName, "$" + enumName + "_name", "String", "i32",
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false));
            }
            if (cm.getMethodByName("ordinal") == null) {
                cm.addMethod(new MethodModel("ordinal", enumName, "$" + enumName + "_ordinal", "int", "i32",
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false));
            }
            if (cm.getMethodByName("compareTo") == null) {
                cm.addMethod(new MethodModel("compareTo", enumName, "$" + enumName + "_compareTo", "int", "i32",
                        Collections.singletonList("other"), Collections.singletonList(enumName), Collections.singletonList("i32"), false, false));
            }
            if (cm.getMethodByName("values") == null) {
                cm.addMethod(new MethodModel("values", enumName, "$" + enumName + "_values", enumName + "[]", "i32",
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), true, false));
            }
            if (cm.getMethodByName("valueOf") == null) {
                cm.addMethod(new MethodModel("valueOf", enumName, "$" + enumName + "_valueOf", enumName, "i32",
                        Collections.singletonList("name"), Collections.singletonList("String"), Collections.singletonList("i32"), true, false));
            }
        }

        // Pre-register built-in exception classes if not defined in source
        String[][] builtins = {
                {"Throwable", null},
                {"Exception", "Throwable"},
                {"RuntimeException", "Exception"},
                {"IllegalArgumentException", "RuntimeException"},
                {"IllegalStateException", "RuntimeException"},
                {"NullPointerException", "RuntimeException"}
        };
        for (String[] def : builtins) {
            String cName = def[0];
            String superName = def[1];
            if (!classes.containsKey(cName)) {
                ClassModel cm = new ClassModel(cName, superName, false, classIdCounter++);
                cm.addField(new FieldModel("message", "String", "i32", 4, 4, false));

                MethodModel init0 = new MethodModel("<init>", cName, "$" + cName + "__init", "void", null,
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, true);
                cm.addConstructor(init0);

                MethodModel init1 = new MethodModel("<init>", cName, "$" + cName + "__init_1", "void", null,
                        Collections.singletonList("message"), Collections.singletonList("String"), Collections.singletonList("i32"), false, true);
                cm.addConstructor(init1);

                MethodModel getMsg = new MethodModel("getMessage", cName, "$" + cName + "_getMessage", "String", "i32",
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                cm.addMethod(getMsg);

                classes.put(cName, cm);
            }
        }

        // Pre-register WASIFile class
        if (!classes.containsKey("WASIFile")) {
            ClassModel cm = new ClassModel("WASIFile", null, false, classIdCounter++);
            cm.addField(new FieldModel("path", "String", "i32", 4, 4, false));
            MethodModel init = new MethodModel("<init>", "WASIFile", "$WASIFile__init", "void", null,
                    Collections.singletonList("path"), Collections.singletonList("String"), Collections.singletonList("i32"), false, true);
            cm.addConstructor(init);
            classes.put("WASIFile", cm);
        }

        // Phase 2: Compute Field Layouts & Struct Sizes in inheritance order
        for (ClassModel cm : getClassesInHierarchyOrder()) {
            int currentOffset = 4; // Offset 0 is reserved for vtable/classId header (4 bytes)
            if (cm.getSuperClassName() != null && classes.containsKey(cm.getSuperClassName())) {
                ClassModel superCm = classes.get(cm.getSuperClassName());
                for (FieldModel sf : superCm.getFields().values()) {
                    if (!sf.isStatic()) {
                        cm.addField(new FieldModel(sf.getName(), sf.getTypeName(), sf.getWasmType(),
                                sf.getOffset(), sf.getSize(), false));
                        int end = sf.getOffset() + sf.getSize();
                        if (end > currentOffset) {
                            currentOffset = end;
                        }
                    }
                }
            }

            // Allocate offsets for own instance fields
            for (Map.Entry<String, FieldModel> entry : new ArrayList<>(cm.getFields().entrySet())) {
                FieldModel f = entry.getValue();
                if (!f.isStatic() && f.getOffset() == 0) {
                    if (f.getSize() == 16 && (currentOffset % 16 != 0)) {
                        currentOffset = ((currentOffset + 15) / 16) * 16; // 16-byte alignment
                    } else if (f.getSize() == 8 && (currentOffset % 8 != 0)) {
                        currentOffset = ((currentOffset + 7) / 8) * 8; // 8-byte alignment
                    }
                    FieldModel updatedField = new FieldModel(f.getName(), f.getTypeName(), f.getWasmType(),
                            currentOffset, f.getSize(), false);
                    cm.getFields().put(f.getName(), updatedField);
                    currentOffset += f.getSize();
                }
            }
            int totalAligned = ((currentOffset + 7) / 8) * 8;
            cm.setStructSize(Math.max(8, totalAligned));
        }

        // Phase 3: Compute VTable slots and function table layout
        for (ClassModel cm : getClassesInHierarchyOrder()) {
            if (cm.isInterface() || !cm.isOopClass()) continue;

            List<MethodModel> slots = cm.getVtableSlots();
            if (cm.getSuperClassName() != null && classes.containsKey(cm.getSuperClassName())) {
                ClassModel superCm = classes.get(cm.getSuperClassName());
                slots.addAll(superCm.getVtableSlots());
            } else {
                // Universal root Object virtual method slots:
                // Slot 0: toString()
                MethodModel toStringMm = cm.getMethodByName("toString");
                if (toStringMm != null && toStringMm.getParamNames().isEmpty() && !toStringMm.isStatic()) {
                    toStringMm.setVirtualSlotIndex(0);
                    slots.add(toStringMm);
                } else {
                    MethodModel defToString = new MethodModel("toString", cm.getName(), "$" + cm.getName() + "_toString", "String", "i32",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                    defToString.setVirtualSlotIndex(0);
                    defToString.setSynthetic(true);
                    slots.add(defToString);
                    cm.addMethod(defToString);
                }

                // Slot 1: equals(Object)
                MethodModel equalsMm = cm.getMethodByName("equals");
                if (equalsMm != null && equalsMm.getParamNames().size() == 1 && !equalsMm.isStatic()) {
                    equalsMm.setVirtualSlotIndex(1);
                    slots.add(equalsMm);
                } else {
                    MethodModel defEquals = new MethodModel("equals", cm.getName(), "$" + cm.getName() + "_equals", "boolean", "i32",
                            Collections.singletonList("other"), Collections.singletonList("Object"), Collections.singletonList("i32"), false, false);
                    defEquals.setVirtualSlotIndex(1);
                    defEquals.setSynthetic(true);
                    slots.add(defEquals);
                    cm.addMethod(defEquals);
                }

                // Slot 2: hashCode()
                MethodModel hashCodeMm = cm.getMethodByName("hashCode");
                if (hashCodeMm != null && hashCodeMm.getParamNames().isEmpty() && !hashCodeMm.isStatic()) {
                    hashCodeMm.setVirtualSlotIndex(2);
                    slots.add(hashCodeMm);
                } else {
                    MethodModel defHashCode = new MethodModel("hashCode", cm.getName(), "$" + cm.getName() + "_hashCode", "int", "i32",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                    defHashCode.setVirtualSlotIndex(2);
                    defHashCode.setSynthetic(true);
                    slots.add(defHashCode);
                    cm.addMethod(defHashCode);
                }
            }

            for (MethodModel mm : cm.getMethods().values()) {
                if (mm.isVirtual()) {
                    boolean overridden = false;
                    for (int i = 0; i < slots.size(); i++) {
                        MethodModel existing = slots.get(i);
                        if (existing.getName().equals(mm.getName()) &&
                                (existing.getParamTypeNames().equals(mm.getParamTypeNames()) ||
                                 (existing.getName().equals("equals") && mm.getParamTypeNames().size() == 1))) {
                            slots.set(i, mm);
                            mm.setVirtualSlotIndex(i);
                            overridden = true;
                            break;
                        }
                    }
                    if (!overridden) {
                        mm.setVirtualSlotIndex(slots.size());
                        slots.add(mm);
                    }
                }
            }

            if (!slots.isEmpty()) {
                cm.setVtableBase(tableFunctions.size());
                for (MethodModel vMethod : slots) {
                    tableFunctions.add(vMethod.getFunctionName());

                    String sig = vMethod.buildWasmTypeSignature();
                    if (!typeSignatures.containsKey(sig)) {
                        String sigName = "$t_sig_" + (++typeSigCounter);
                        typeSignatures.put(sig, sigName);
                    }
                    vMethod.setTypeSignatureName(typeSignatures.get(sig));
                }
            }
        }
    }

    private List<ClassModel> getClassesInHierarchyOrder() {
        List<ClassModel> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (ClassModel cm : classes.values()) {
            visitClassHierarchy(cm, visited, result);
        }
        return result;
    }

    private void visitClassHierarchy(ClassModel cm, Set<String> visited, List<ClassModel> result) {
        if (visited.contains(cm.getName())) return;
        if (cm.getSuperClassName() != null && classes.containsKey(cm.getSuperClassName())) {
            visitClassHierarchy(classes.get(cm.getSuperClassName()), visited, result);
        }
        visited.add(cm.getName());
        result.add(cm);
    }

    public Map<String, ClassModel> getClasses() {
        return classes;
    }

    public ClassModel getClass(String name) {
        return classes.get(name);
    }

    public boolean hasClass(String name) {
        return classes.containsKey(name);
    }

    public MethodModel findMethod(String className, String methodName) {
        if ("Object".equals(className)) {
            if ("toString".equals(methodName)) {
                MethodModel mm = new MethodModel("toString", "Object", "$Object_toString", "String", "i32",
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                mm.setVirtualSlotIndex(0);
                mm.setTypeSignatureName(typeSignatures.get("(func (param i32) (result i32))"));
                return mm;
            } else if ("equals".equals(methodName)) {
                MethodModel mm = new MethodModel("equals", "Object", "$Object_equals", "boolean", "i32",
                        Collections.singletonList("other"), Collections.singletonList("Object"), Collections.singletonList("i32"), false, false);
                mm.setVirtualSlotIndex(1);
                mm.setTypeSignatureName(typeSignatures.get("(func (param i32 i32) (result i32))"));
                return mm;
            } else if ("hashCode".equals(methodName)) {
                MethodModel mm = new MethodModel("hashCode", "Object", "$Object_hashCode", "int", "i32",
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                mm.setVirtualSlotIndex(2);
                mm.setTypeSignatureName(typeSignatures.get("(func (param i32) (result i32))"));
                return mm;
            }
        }
        String curr = className;
        while (curr != null && classes.containsKey(curr)) {
            ClassModel cm = classes.get(curr);
            MethodModel mm = cm.getMethodByName(methodName);
            if (mm != null) return mm;
            curr = cm.getSuperClassName();
        }
        if ("toString".equals(methodName)) {
            return findMethod("Object", "toString");
        } else if ("equals".equals(methodName)) {
            return findMethod("Object", "equals");
        } else if ("hashCode".equals(methodName)) {
            return findMethod("Object", "hashCode");
        }
        return null;
    }

    public List<String> getTableFunctions() {
        return tableFunctions;
    }

    public Map<String, String> getTypeSignatures() {
        return typeSignatures;
    }

    public boolean isRecord(String name) {
        return name != null && records.containsKey(name);
    }

    public RecordModel getRecord(String name) {
        return records.get(name);
    }

    public Map<String, RecordModel> getRecords() {
        return records;
    }

    public boolean isSubclassOf(String subClass, String superClass) {
        if (subClass == null || superClass == null) return false;
        if (subClass.equals(superClass)) return true;
        if ("Object".equals(superClass)) return true;
        ClassModel cm = classes.get(subClass);
        if (cm != null && cm.isEnum() && "Enum".equals(superClass)) return true;
        while (cm != null && cm.getSuperClassName() != null) {
            if (cm.getSuperClassName().equals(superClass)) {
                return true;
            }
            cm = classes.get(cm.getSuperClassName());
        }
        return false;
    }

    public void emitTypeSignaturesAndTable(StringBuilder watOutput) {
        if (!typeSignatures.isEmpty()) {
            watOutput.append("  ;; --- WebAssembly Function Type Signatures ---\n");
            for (Map.Entry<String, String> entry : typeSignatures.entrySet()) {
                watOutput.append("  (type ").append(entry.getValue()).append(" ").append(entry.getKey()).append(")\n");
            }
        }

        if (!tableFunctions.isEmpty()) {
            watOutput.append("  ;; --- WebAssembly Function Table (VTable) ---\n");
            watOutput.append("  (table (export \"__vtable\") ").append(tableFunctions.size()).append(" funcref)\n");
            watOutput.append("  (elem (i32.const 0)");
            for (String func : tableFunctions) {
                watOutput.append(" ").append(func);
            }
            watOutput.append(")\n");
        }
    }
}
