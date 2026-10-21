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
            classes.put(className, cm);

            // Collect static and instance fields
            for (FieldDeclaration fd : cd.getFields()) {
                boolean isStatic = fd.isStatic();
                for (VariableDeclarator vd : fd.getVariables()) {
                    String fName = vd.getNameAsString();
                    String fType = vd.getType().asString();
                    String wasmType = TypeMapper.mapType(vd.getType());
                    if (wasmType == null) wasmType = "i32";
                    int size = ("i64".equals(wasmType) || "f64".equals(wasmType)) ? 8 : 4;
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
                    funcName = "$" + className + "_" + mName;
                } else if (!cm.isOopClass()) {
                    funcName = "$" + mName;
                } else {
                    funcName = "$" + className + "_" + mName;
                }

                MethodModel mm = new MethodModel(mName, className, funcName, retTypeName, retWasmType,
                        paramNames, paramTypeNames, paramWasmTypes, isStatic, false);
                cm.addMethod(mm);
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
                    if (f.getSize() == 8 && (currentOffset % 8 != 0)) {
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
            if (cm.isInterface()) continue;

            List<MethodModel> slots = cm.getVtableSlots();
            if (cm.getSuperClassName() != null && classes.containsKey(cm.getSuperClassName())) {
                ClassModel superCm = classes.get(cm.getSuperClassName());
                slots.addAll(superCm.getVtableSlots());
            }

            for (MethodModel mm : cm.getMethods().values()) {
                if (mm.isVirtual()) {
                    boolean overridden = false;
                    for (int i = 0; i < slots.size(); i++) {
                        MethodModel existing = slots.get(i);
                        if (existing.getName().equals(mm.getName()) &&
                                existing.getParamTypeNames().equals(mm.getParamTypeNames())) {
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

    public List<String> getTableFunctions() {
        return tableFunctions;
    }

    public Map<String, String> getTypeSignatures() {
        return typeSignatures;
    }

    public boolean isSubclassOf(String subClass, String superClass) {
        if (subClass == null || superClass == null) return false;
        if (subClass.equals(superClass)) return true;
        ClassModel cm = classes.get(subClass);
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
