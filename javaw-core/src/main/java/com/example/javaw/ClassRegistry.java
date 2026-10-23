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
            for (com.github.javaparser.ast.type.ClassOrInterfaceType it : cd.getImplementedTypes()) {
                cm.addImplementedInterface(it.getNameAsString());
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

                if (cd.getMethodsByName(mName).size() > 1) {
                    funcName = "$" + className + "_" + mName + "__" + methodSuffix(paramTypeNames);
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

        // Pre-register CharSequence and StringBuilder
        if (!classes.containsKey("CharSequence")) {
            ClassModel cm = new ClassModel("CharSequence", null, true, classIdCounter++);
            classes.put("CharSequence", cm);
        }
        if (!classes.containsKey("StringBuilder")) {
            ClassModel cm = new ClassModel("StringBuilder", null, false, classIdCounter++);
            classes.put("StringBuilder", cm);
        }

        // Pre-register AutoCloseable, Comparable, Iterable, Iterator
        if (!classes.containsKey("AutoCloseable")) {
            ClassModel cm = new ClassModel("AutoCloseable", null, true, classIdCounter++);
            MethodModel mm = new MethodModel("close", "AutoCloseable", "$AutoCloseable_close", "void", null,
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
            mm.setVirtualSlotIndex(3);
            cm.addMethod(mm);
            classes.put("AutoCloseable", cm);
        }
        if (!classes.containsKey("Comparable")) {
            ClassModel cm = new ClassModel("Comparable", null, true, classIdCounter++);
            MethodModel mm = new MethodModel("compareTo", "Comparable", "$Comparable_compareTo", "int", "i32",
                    Collections.singletonList("other"), Collections.singletonList("Object"), Collections.singletonList("i32"), false, false);
            mm.setVirtualSlotIndex(4);
            cm.addMethod(mm);
            classes.put("Comparable", cm);
        }
        if (!classes.containsKey("Iterable")) {
            ClassModel cm = new ClassModel("Iterable", null, true, classIdCounter++);
            MethodModel mm = new MethodModel("iterator", "Iterable", "$Iterable_iterator", "Iterator", "i32",
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
            mm.setVirtualSlotIndex(5);
            cm.addMethod(mm);
            classes.put("Iterable", cm);
        }
        if (!classes.containsKey("Iterator")) {
            ClassModel cm = new ClassModel("Iterator", null, true, classIdCounter++);
            MethodModel mm1 = new MethodModel("hasNext", "Iterator", "$Iterator_hasNext", "boolean", "i32",
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
            mm1.setVirtualSlotIndex(6);
            cm.addMethod(mm1);
            MethodModel mm2 = new MethodModel("next", "Iterator", "$Iterator_next", "Object", "i32",
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
            mm2.setVirtualSlotIndex(7);
            cm.addMethod(mm2);
            classes.put("Iterator", cm);
        }

        // Pre-register Runnable, Thread, Class
        if (!classes.containsKey("Runnable")) {
            ClassModel cm = new ClassModel("Runnable", null, true, classIdCounter++);
            MethodModel mm = new MethodModel("run", "Runnable", "$Runnable_run", "void", null,
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
            mm.setVirtualSlotIndex(8);
            cm.addMethod(mm);
            classes.put("Runnable", cm);
        }
        if (!classes.containsKey("Thread")) {
            ClassModel cm = new ClassModel("Thread", null, false, classIdCounter++);
            cm.addImplementedInterface("Runnable");
            cm.addField(new FieldModel("target", "Runnable", "i32", 4, 4, false));
            cm.addField(new FieldModel("name", "String", "i32", 8, 4, false));
            cm.addField(new FieldModel("id", "int", "i32", 12, 4, false));
            cm.addField(new FieldModel("alive", "boolean", "i32", 16, 4, false));
            cm.setStructSize(24);
            MethodModel c0 = new MethodModel("<init>", "Thread", "$Thread__init", "void", null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, true);
            cm.addConstructor(c0);
            MethodModel c1 = new MethodModel("<init>", "Thread", "$Thread__init_1", "void", null, Collections.singletonList("target"), Collections.singletonList("Runnable"), Collections.singletonList("i32"), false, true);
            cm.addConstructor(c1);
            MethodModel c2 = new MethodModel("<init>", "Thread", "$Thread__init_2", "void", null, Arrays.asList("target", "name"), Arrays.asList("Runnable", "String"), Arrays.asList("i32", "i32"), false, true);
            cm.addConstructor(c2);
            classes.put("Thread", cm);
        }
        if (!classes.containsKey("Class")) {
            ClassModel cm = new ClassModel("Class", null, false, classIdCounter++);
            cm.addField(new FieldModel("name", "String", "i32", 4, 4, false));
            cm.addField(new FieldModel("simpleName", "String", "i32", 8, 4, false));
            cm.addField(new FieldModel("classId", "int", "i32", 12, 4, false));
            cm.addField(new FieldModel("isInterface", "boolean", "i32", 16, 4, false));
            cm.addField(new FieldModel("isEnum", "boolean", "i32", 20, 4, false));
            cm.addField(new FieldModel("superclass", "Class", "i32", 24, 4, false));
            cm.setStructSize(32);
            classes.put("Class", cm);
        }
        String[] primTypes = {"int", "long", "double", "float", "boolean", "char", "byte", "short", "void"};
        for (String pt : primTypes) {
            if (!classes.containsKey(pt)) {
                ClassModel cm = new ClassModel(pt, null, false, classIdCounter++);
                classes.put(pt, cm);
            }
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

                // Slot 3: close()
                MethodModel closeMm = cm.getMethodByName("close");
                if (closeMm != null && closeMm.getParamNames().isEmpty() && !closeMm.isStatic()) {
                    closeMm.setVirtualSlotIndex(3);
                    slots.add(closeMm);
                } else {
                    MethodModel defClose = new MethodModel("close", cm.getName(), "$AutoCloseable_close", "void", null,
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                    defClose.setVirtualSlotIndex(3);
                    defClose.setSynthetic(true);
                    slots.add(defClose);
                    cm.addMethod(defClose);
                }

                // Slot 4: compareTo(Object)
                MethodModel compareToMm = cm.getMethodByName("compareTo");
                if (compareToMm != null && compareToMm.getParamNames().size() == 1 && !compareToMm.isStatic()) {
                    compareToMm.setVirtualSlotIndex(4);
                    slots.add(compareToMm);
                } else {
                    MethodModel defCompareTo = new MethodModel("compareTo", cm.getName(), "$Comparable_compareTo", "int", "i32",
                            Collections.singletonList("other"), Collections.singletonList("Object"), Collections.singletonList("i32"), false, false);
                    defCompareTo.setVirtualSlotIndex(4);
                    defCompareTo.setSynthetic(true);
                    slots.add(defCompareTo);
                    cm.addMethod(defCompareTo);
                }

                // Slot 5: iterator()
                MethodModel iteratorMm = cm.getMethodByName("iterator");
                if (iteratorMm != null && iteratorMm.getParamNames().isEmpty() && !iteratorMm.isStatic()) {
                    iteratorMm.setVirtualSlotIndex(5);
                    slots.add(iteratorMm);
                } else {
                    MethodModel defIterator = new MethodModel("iterator", cm.getName(), "$Iterable_iterator", "Iterator", "i32",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                    defIterator.setVirtualSlotIndex(5);
                    defIterator.setSynthetic(true);
                    slots.add(defIterator);
                    cm.addMethod(defIterator);
                }

                // Slot 6: hasNext()
                MethodModel hasNextMm = cm.getMethodByName("hasNext");
                if (hasNextMm != null && hasNextMm.getParamNames().isEmpty() && !hasNextMm.isStatic()) {
                    hasNextMm.setVirtualSlotIndex(6);
                    slots.add(hasNextMm);
                } else {
                    MethodModel defHasNext = new MethodModel("hasNext", cm.getName(), "$Iterator_hasNext", "boolean", "i32",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                    defHasNext.setVirtualSlotIndex(6);
                    defHasNext.setSynthetic(true);
                    slots.add(defHasNext);
                    cm.addMethod(defHasNext);
                }

                // Slot 7: next()
                MethodModel nextMm = cm.getMethodByName("next");
                if (nextMm != null && nextMm.getParamNames().isEmpty() && !nextMm.isStatic()) {
                    nextMm.setVirtualSlotIndex(7);
                    slots.add(nextMm);
                } else {
                    MethodModel defNext = new MethodModel("next", cm.getName(), "$Iterator_next", "Object", "i32",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                    defNext.setVirtualSlotIndex(7);
                    defNext.setSynthetic(true);
                    slots.add(defNext);
                    cm.addMethod(defNext);
                }

                // Slot 8: run()
                MethodModel runMm = cm.getMethodByName("run");
                if (runMm != null && runMm.getParamNames().isEmpty() && !runMm.isStatic()) {
                    runMm.setVirtualSlotIndex(8);
                    slots.add(runMm);
                } else {
                    MethodModel defRun = new MethodModel("run", cm.getName(), "$" + cm.getName() + "_run", "void", null,
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                    defRun.setVirtualSlotIndex(8);
                    defRun.setSynthetic(true);
                    slots.add(defRun);
                    cm.addMethod(defRun);
                }

                // Slot 9: getClass()
                MethodModel getClassMm = cm.getMethodByName("getClass");
                if (getClassMm != null && getClassMm.getParamNames().isEmpty() && !getClassMm.isStatic()) {
                    getClassMm.setVirtualSlotIndex(9);
                    slots.add(getClassMm);
                } else {
                    MethodModel defGetClass = new MethodModel("getClass", cm.getName(), "$" + cm.getName() + "_getClass", "Class", "i32",
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                    defGetClass.setVirtualSlotIndex(9);
                    defGetClass.setSynthetic(true);
                    slots.add(defGetClass);
                    cm.addMethod(defGetClass);
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

    static String methodSuffix(List<String> types) {
        if (types.isEmpty()) return "noargs";
        return types.stream().map(t -> t.chars().mapToObj(c -> Integer.toHexString(c) + "_")
                .collect(java.util.stream.Collectors.joining())).collect(java.util.stream.Collectors.joining("__"));
    }

    public List<MethodModel> methodCandidates(String className, String methodName) {
        Map<String, MethodModel> methods = new LinkedHashMap<>();
        collectMethods(className, methodName, methods, new HashSet<>());
        return new ArrayList<>(methods.values());
    }

    private void collectMethods(String name, String methodName, Map<String, MethodModel> methods, Set<String> seen) {
        if (name == null || !seen.add(name)) return;
        ClassModel model = classes.get(name);
        if (model == null) return;
        model.getMethods().values().stream().filter(m -> m.getName().equals(methodName))
                .forEach(m -> methods.putIfAbsent(m.getSignatureKey(), m));
        collectMethods(model.getSuperClassName(), methodName, methods, seen);
        for (String iface : model.getImplementedInterfaces()) collectMethods(iface, methodName, methods, seen);
    }

    /** Strict invocation conversions: identity, primitive widening, and reference widening. */
    public MethodModel resolveOverload(List<MethodModel> candidates, List<String> arguments) {
        List<MethodModel> applicable = candidates.stream().filter(m -> {
            if (m.getParamTypeNames().size() != arguments.size()) return false;
            for (int i = 0; i < arguments.size(); i++)
                if (!canWiden(arguments.get(i), m.getParamTypeNames().get(i))) return false;
            return true;
        }).toList();
        List<MethodModel> best = applicable.stream().filter(m -> applicable.stream().noneMatch(other ->
                other != m && moreSpecific(other, m))).toList();
        if (best.size() == 1) return best.get(0);
        String signatures = candidates.stream().map(MethodModel::getSignatureKey)
                .collect(java.util.stream.Collectors.joining(", "));
        throw new IllegalArgumentException((best.isEmpty() ? "No applicable overload" : "Ambiguous overload")
                + " for arguments " + arguments + "; candidates: " + signatures);
    }

    private boolean moreSpecific(MethodModel first, MethodModel second) {
        boolean strict = false;
        for (int i = 0; i < first.getParamTypeNames().size(); i++) {
            String a = first.getParamTypeNames().get(i), b = second.getParamTypeNames().get(i);
            if (!canWiden(a, b)) return false;
            strict |= !a.equals(b);
        }
        return strict;
    }

    private boolean canWiden(String from, String to) {
        if (from == null || to == null) return false;
        if (from.equals(to)) return true;
        Set<String> primitives = Set.of("boolean", "byte", "short", "char", "int", "long", "float", "double", "void");
        if (from.equals("null")) return !primitives.contains(to);
        if (primitives.contains(from) || primitives.contains(to)) {
            return switch (from) {
                case "byte" -> Set.of("short", "int", "long", "float", "double").contains(to);
                case "short", "char" -> Set.of("int", "long", "float", "double").contains(to);
                case "int" -> Set.of("long", "float", "double").contains(to);
                case "long" -> Set.of("float", "double").contains(to);
                case "float" -> to.equals("double");
                default -> false;
            };
        }
        if (to.equals("Object")) return true;
        Set<String> visited = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        pending.add(from);
        while (!pending.isEmpty()) {
            String name = pending.removeFirst();
            if (!visited.add(name)) continue;
            if (name.equals(to)) return true;
            ClassModel model = classes.get(name);
            if (model == null) continue;
            if (model.getSuperClassName() != null) pending.add(model.getSuperClassName());
            pending.addAll(model.getImplementedInterfaces());
        }
        return false;
    }

    public MethodModel findMethod(String className, String methodName) {
        if (className == null) return null;
        if (className.contains("<")) {
            className = className.substring(0, className.indexOf('<')).trim();
        }
        if (className.contains(".")) {
            className = className.substring(className.lastIndexOf('.') + 1);
        }

        if ("Object".equals(className)) {
            if ("toString".equals(methodName)) {
                MethodModel mm = new MethodModel("toString", "Object", "$Object_toString", "String", "i32",
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                mm.setVirtualSlotIndex(0);
                mm.setTypeSignatureName(getOrCreateTypeSignature("(func (param i32) (result i32))"));
                return mm;
            } else if ("equals".equals(methodName)) {
                MethodModel mm = new MethodModel("equals", "Object", "$Object_equals", "boolean", "i32",
                        Collections.singletonList("other"), Collections.singletonList("Object"), Collections.singletonList("i32"), false, false);
                mm.setVirtualSlotIndex(1);
                mm.setTypeSignatureName(getOrCreateTypeSignature("(func (param i32 i32) (result i32))"));
                return mm;
            } else if ("hashCode".equals(methodName)) {
                MethodModel mm = new MethodModel("hashCode", "Object", "$Object_hashCode", "int", "i32",
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                mm.setVirtualSlotIndex(2);
                mm.setTypeSignatureName(getOrCreateTypeSignature("(func (param i32) (result i32))"));
                return mm;
            } else if ("getClass".equals(methodName)) {
                MethodModel mm = new MethodModel("getClass", "Object", "$Object_getClass", "Class", "i32",
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                mm.setVirtualSlotIndex(9);
                mm.setTypeSignatureName(getOrCreateTypeSignature("(func (param i32) (result i32))"));
                return mm;
            }
        } else if ("AutoCloseable".equals(className)) {
            if ("close".equals(methodName)) {
                MethodModel mm = new MethodModel("close", "AutoCloseable", "$AutoCloseable_close", "void", null,
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                mm.setVirtualSlotIndex(3);
                mm.setTypeSignatureName(getOrCreateTypeSignature("(func (param i32) (result i32))"));
                return mm;
            }
        } else if ("Comparable".equals(className)) {
            if ("compareTo".equals(methodName)) {
                MethodModel mm = new MethodModel("compareTo", "Comparable", "$Comparable_compareTo", "int", "i32",
                        Collections.singletonList("other"), Collections.singletonList("Object"), Collections.singletonList("i32"), false, false);
                mm.setVirtualSlotIndex(4);
                mm.setTypeSignatureName(getOrCreateTypeSignature("(func (param i32 i32) (result i32))"));
                return mm;
            }
        } else if ("Iterable".equals(className)) {
            if ("iterator".equals(methodName)) {
                MethodModel mm = new MethodModel("iterator", "Iterable", "$Iterable_iterator", "Iterator", "i32",
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                mm.setVirtualSlotIndex(5);
                mm.setTypeSignatureName(getOrCreateTypeSignature("(func (param i32) (result i32))"));
                return mm;
            }
        } else if ("Iterator".equals(className)) {
            if ("hasNext".equals(methodName)) {
                MethodModel mm = new MethodModel("hasNext", "Iterator", "$Iterator_hasNext", "boolean", "i32",
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                mm.setVirtualSlotIndex(6);
                mm.setTypeSignatureName(getOrCreateTypeSignature("(func (param i32) (result i32))"));
                return mm;
            } else if ("next".equals(methodName)) {
                MethodModel mm = new MethodModel("next", "Iterator", "$Iterator_next", "Object", "i32",
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                mm.setVirtualSlotIndex(7);
                mm.setTypeSignatureName(getOrCreateTypeSignature("(func (param i32) (result i32))"));
                return mm;
            }
        } else if ("Runnable".equals(className)) {
            if ("run".equals(methodName)) {
                MethodModel mm = new MethodModel("run", "Runnable", "$Runnable_run", "void", null,
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                mm.setVirtualSlotIndex(8);
                mm.setTypeSignatureName(getOrCreateTypeSignature("(func (param i32) (result i32))"));
                return mm;
            }
        } else if ("Class".equals(className)) {
            if ("getName".equals(methodName)) {
                return new MethodModel("getName", "Class", "$Class_getName", "String", "i32", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
            } else if ("getSimpleName".equals(methodName)) {
                return new MethodModel("getSimpleName", "Class", "$Class_getSimpleName", "String", "i32", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
            } else if ("getId".equals(methodName)) {
                return new MethodModel("getId", "Class", "$Class_getId", "int", "i32", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
            } else if ("isInterface".equals(methodName)) {
                return new MethodModel("isInterface", "Class", "$Class_isInterface", "boolean", "i32", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
            } else if ("isEnum".equals(methodName)) {
                return new MethodModel("isEnum", "Class", "$Class_isEnum", "boolean", "i32", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
            } else if ("isArray".equals(methodName)) {
                return new MethodModel("isArray", "Class", "$Class_isArray", "boolean", "i32", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
            } else if ("getSuperclass".equals(methodName)) {
                return new MethodModel("getSuperclass", "Class", "$Class_getSuperclass", "Class", "i32", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
            } else if ("isInstance".equals(methodName)) {
                return new MethodModel("isInstance", "Class", "$Class_isInstance", "boolean", "i32", Collections.singletonList("obj"), Collections.singletonList("Object"), Collections.singletonList("i32"), false, false);
            } else if ("forName".equals(methodName)) {
                return new MethodModel("forName", "Class", "$Class_forName", "Class", "i32", Collections.singletonList("name"), Collections.singletonList("String"), Collections.singletonList("i32"), true, false);
            }
        } else if ("Thread".equals(className)) {
            if ("start".equals(methodName)) {
                return new MethodModel("start", "Thread", "$Thread_start", "void", null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
            } else if ("run".equals(methodName)) {
                MethodModel mm = new MethodModel("run", "Thread", "$Thread_run", "void", null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
                mm.setVirtualSlotIndex(8);
                mm.setTypeSignatureName(getOrCreateTypeSignature("(func (param i32) (result i32))"));
                return mm;
            } else if ("getId".equals(methodName)) {
                return new MethodModel("getId", "Thread", "$Thread_getId", "int", "i32", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
            } else if ("getName".equals(methodName)) {
                return new MethodModel("getName", "Thread", "$Thread_getName", "String", "i32", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
            } else if ("setName".equals(methodName)) {
                return new MethodModel("setName", "Thread", "$Thread_setName", "void", null, Collections.singletonList("name"), Collections.singletonList("String"), Collections.singletonList("i32"), false, false);
            } else if ("isAlive".equals(methodName)) {
                return new MethodModel("isAlive", "Thread", "$Thread_isAlive", "boolean", "i32", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, false);
            } else if ("sleep".equals(methodName)) {
                return new MethodModel("sleep", "Thread", "$dom_thread_sleep", "void", null, Collections.singletonList("millis"), Collections.singletonList("int"), Collections.singletonList("i32"), true, false);
            } else if ("currentThread".equals(methodName)) {
                return new MethodModel("currentThread", "Thread", "$Thread_currentThread", "Thread", "i32", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), true, false);
            } else if ("activeCount".equals(methodName)) {
                return new MethodModel("activeCount", "Thread", "$dom_thread_hardware_concurrency", "int", "i32", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), true, false);
            } else if ("yield".equals(methodName) || "onSpinWait".equals(methodName)) {
                return new MethodModel(methodName, "Thread", "$Thread_" + methodName, "void", null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), true, false);
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
        } else if ("close".equals(methodName)) {
            return findMethod("AutoCloseable", "close");
        } else if ("compareTo".equals(methodName)) {
            return findMethod("Comparable", "compareTo");
        } else if ("iterator".equals(methodName)) {
            return findMethod("Iterable", "iterator");
        } else if ("hasNext".equals(methodName)) {
            return findMethod("Iterator", "hasNext");
        } else if ("next".equals(methodName)) {
            return findMethod("Iterator", "next");
        } else if ("run".equals(methodName)) {
            return findMethod("Runnable", "run");
        } else if ("getClass".equals(methodName)) {
            return findMethod("Object", "getClass");
        }
        return null;
    }

    public String getOrCreateTypeSignature(String sig) {
        if (!typeSignatures.containsKey(sig)) {
            String sigName = "$t_sig_" + (++typeSigCounter);
            typeSignatures.put(sig, sigName);
        }
        return typeSignatures.get(sig);
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
        if ("Object".equals(superClass) || "java.lang.Object".equals(superClass)) return true;
        ClassModel cm = classes.get(subClass);
        if (cm != null && cm.isEnum() && ("Enum".equals(superClass) || "java.lang.Enum".equals(superClass))) return true;
        while (cm != null) {
            for (String iface : cm.getImplementedInterfaces()) {
                if (iface.equals(superClass) || iface.endsWith("." + superClass) || superClass.endsWith("." + iface)) {
                    return true;
                }
            }
            if (cm.getSuperClassName() != null && (cm.getSuperClassName().equals(superClass) ||
                    cm.getSuperClassName().endsWith("." + superClass) || superClass.endsWith("." + cm.getSuperClassName()))) {
                return true;
            }
            cm = cm.getSuperClassName() != null ? classes.get(cm.getSuperClassName()) : null;
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
