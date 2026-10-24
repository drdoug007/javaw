package com.example.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeHierarchyTest {
    private ClassNode type(String name, boolean isInterface) {
        ClassNode type = new ClassNode();
        type.name = name;
        type.superName = "java/lang/Object";
        type.access = Opcodes.ACC_PUBLIC | (isInterface ? Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT : 0);
        type.sourceFile = name.substring(name.lastIndexOf('/') + 1) + ".java";
        return type;
    }

    @Test void rejectsInvalidDeclarationsEvenWithoutExecutableMethods() {
        for (String scenario : new String[]{"missingSuperclass", "missingInterface", "classCycle", "interfaceCycle",
                "interfaceSuperclass", "extendsInterface", "implementsClass", "duplicateInterface", "finalSuperclass",
                "hiddenSuperclass", "hiddenInterface", "noSuperclass", "indirectMissing"}) {
            ClassNode owner = type("q/Owner", false), parent = type("p/Parent", false), iface = type("p/View", true);
            var linked = new LinkedHashMap<String, ClassNode>();
            linked.put(owner.name, owner); linked.put(parent.name, parent); linked.put(iface.name, iface);
            switch (scenario) {
                case "missingSuperclass" -> owner.superName = "missing/Base";
                case "missingInterface" -> owner.interfaces.add("missing/View");
                case "classCycle" -> { owner.superName = parent.name; parent.superName = owner.name; }
                case "interfaceCycle" -> { owner.interfaces.add(iface.name); iface.interfaces.add(iface.name); }
                case "interfaceSuperclass" -> { owner.access = iface.access; owner.superName = parent.name; }
                case "extendsInterface" -> owner.superName = iface.name;
                case "implementsClass" -> owner.interfaces.add(parent.name);
                case "duplicateInterface" -> { owner.interfaces.add(iface.name); owner.interfaces.add(iface.name); }
                case "finalSuperclass" -> { owner.superName = parent.name; parent.access |= Opcodes.ACC_FINAL; }
                case "hiddenSuperclass" -> { owner.superName = parent.name; parent.access = 0; }
                case "hiddenInterface" -> { owner.interfaces.add(iface.name); iface.access &= ~Opcodes.ACC_PUBLIC; }
                case "noSuperclass" -> owner.superName = null;
                case "indirectMissing" -> { owner.superName = parent.name; parent.interfaces.add("missing/View"); }
            }
            String error = assertThrows(IllegalArgumentException.class,
                    () -> new ClassBytecodeCompiler(owner).compileToWat(linked), scenario).getMessage();
            assertTrue(error.contains("unsupported hierarchy declaration"), scenario + ": " + error);
            assertTrue(error.contains(".java:line unavailable:"), error);
            assertTrue(error.contains("; "), error);
        }
    }

    @Test void checksAccessFromEachDeclaringChildRatherThanTheEntryClass() {
        ClassNode owner = type("q/Owner", false), parent = type("p/Parent", false), hidden = type("p/Hidden", false);
        ClassNode view = type("p/View", true), hiddenView = type("p/HiddenView", true);
        hidden.access = 0; hiddenView.access &= ~Opcodes.ACC_PUBLIC;
        owner.superName = parent.name; parent.superName = hidden.name;
        owner.interfaces.add(view.name); view.interfaces.add(hiddenView.name);
        var linked = java.util.Map.of(owner.name, owner, parent.name, parent, hidden.name, hidden,
                view.name, view, hiddenView.name, hiddenView);
        assertDoesNotThrow(() -> new ClassBytecodeCompiler(owner).compileToWat(linked));
        // A transitive edge must still be checked when the entry class could access the parent.
        hidden.name = "q/Hidden"; parent.superName = hidden.name;
        var invalid = java.util.Map.of(owner.name, owner, parent.name, parent, hidden.name, hidden,
                view.name, view, hiddenView.name, hiddenView);
        String error = assertThrows(IllegalArgumentException.class,
                () -> new ClassBytecodeCompiler(owner).compileToWat(invalid)).getMessage();
        assertTrue(error.contains("Parent.java:line unavailable: p.Parent:"), error);
        assertTrue(error.contains("inaccessible superclass/superinterface q/Hidden"), error);
    }
}
