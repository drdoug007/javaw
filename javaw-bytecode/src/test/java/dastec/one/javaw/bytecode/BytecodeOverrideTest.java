package dastec.one.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeOverrideTest {
    private ClassNode type(String name, String parent) {
        ClassNode type = new ClassNode(); type.name = name; type.superName = parent;
        type.access = Opcodes.ACC_PUBLIC; type.sourceFile = "Child.java";
        return type;
    }

    private MethodNode method(int access) {
        MethodNode method = new MethodNode(access, "value", "()I", null, null);
        method.instructions.add(new InsnNode(Opcodes.ICONST_1));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        method.maxLocals = 1; method.maxStack = 1;
        return method;
    }

    @Test void rejectsFinalOverridesAndReducedVisibilityWithoutCallSites() {
        for (String scenario : new String[]{"publicFinal", "protectedFinal", "packageFinal", "publicVisibility",
                "protectedVisibility", "finalBridge", "visibilityBridge", "returnToPackage"}) {
            ClassNode base = type("p/Base", "java/lang/Object");
            ClassNode middle = type("q/Middle", base.name);
            ClassNode child = type("q/Child", base.name);
            int inherited = Opcodes.ACC_PUBLIC, declared = Opcodes.ACC_PUBLIC;
            switch (scenario) {
                case "publicFinal" -> inherited |= Opcodes.ACC_FINAL;
                case "protectedFinal" -> inherited = Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL;
                case "packageFinal" -> { inherited = Opcodes.ACC_FINAL; child.name = "p/Child"; }
                case "publicVisibility" -> declared = Opcodes.ACC_PROTECTED;
                case "protectedVisibility" -> { inherited = Opcodes.ACC_PROTECTED; declared = 0; }
                case "finalBridge", "visibilityBridge" -> {
                    inherited = 0; middle.name = "p/Bridge"; child.superName = middle.name;
                    middle.methods.add(method(Opcodes.ACC_PUBLIC | (scenario.equals("finalBridge") ? Opcodes.ACC_FINAL : 0)));
                    if (scenario.equals("visibilityBridge")) declared = Opcodes.ACC_PROTECTED;
                }
                case "returnToPackage" -> {
                    inherited = Opcodes.ACC_FINAL; child.name = "p/Child"; child.superName = middle.name;
                    middle.methods.add(method(Opcodes.ACC_PUBLIC));
                }
            }
            base.methods.add(method(inherited)); child.methods.add(method(declared));
            var linked = java.util.Map.of(base.name, base, middle.name, middle, child.name, child);
            String error = assertThrows(IllegalArgumentException.class,
                    () -> new ClassBytecodeCompiler(child).compileToWat(linked), scenario).getMessage();
            assertTrue(error.contains("unsupported method override value()I"), scenario + ": " + error);
            assertTrue(error.contains("Child.java:line unavailable:"), error);
            assertTrue(error.contains(scenario.contains("Visibility") || scenario.equals("visibilityBridge")
                    ? "reduces virtual method visibility" : "cannot override final instance method"), error);
        }
    }
}
