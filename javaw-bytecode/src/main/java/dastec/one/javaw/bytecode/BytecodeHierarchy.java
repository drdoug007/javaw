package dastec.one.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Declaration checks for the closed-world class/interface inheritance graph. */
final class BytecodeHierarchy {
    static void validate(ClassNode owner, Map<String, ClassNode> linked) {
        Set<String> complete = new HashSet<>();
        visit(owner, linked, new HashSet<>(), complete);
        for (ClassNode declaration : linked.values()) visit(declaration, linked, new HashSet<>(), complete);
    }

    private static void visit(ClassNode type, Map<String, ClassNode> linked,
                              Set<String> visiting, Set<String> complete) {
        if (complete.contains(type.name)) return;
        if (!visiting.add(type.name)) throw failure(type, "cyclic superclass/superinterface hierarchy");
        if ("java/lang/Object".equals(type.name) && type.superName != null)
            throw failure(type, "java/lang/Object must not declare a superclass");
        boolean isInterface = (type.access & Opcodes.ACC_INTERFACE) != 0;
        if (isInterface && !"java/lang/Object".equals(type.superName))
            throw failure(type, "an interface must name java/lang/Object as its superclass");
        if (!"java/lang/Object".equals(type.superName)) {
            if (type.superName == null && !"java/lang/Object".equals(type.name))
                throw failure(type, "link a complete superclass chain through java/lang/Object");
            if (type.superName != null) {
                ClassNode parent = parent(type, type.superName, false, linked);
                if ((parent.access & Opcodes.ACC_FINAL) != 0)
                    throw failure(type, "cannot extend final superclass " + parent.name);
                visit(parent, linked, visiting, complete);
            }
        }
        Set<String> direct = new HashSet<>();
        for (String name : type.interfaces) {
            if (!direct.add(name)) throw failure(type, "duplicate direct superinterface " + name);
            visit(parent(type, name, true, linked), linked, visiting, complete);
        }
        visiting.remove(type.name);
        complete.add(type.name);
    }

    private static ClassNode parent(ClassNode child, String name, boolean interfaceExpected,
                                    Map<String, ClassNode> linked) {
        ClassNode parent = linked.get(name);
        if (parent == null) throw failure(child, "link missing "
                + (interfaceExpected ? "superinterface " : "superclass ") + name);
        if (((parent.access & Opcodes.ACC_INTERFACE) != 0) != interfaceExpected)
            throw failure(child, "wrong superclass/superinterface kind for " + name);
        if ((parent.access & Opcodes.ACC_PUBLIC) == 0 && !packageName(child.name).equals(packageName(name)))
            throw failure(child, "inaccessible superclass/superinterface " + name);
        if (parent.permittedSubclasses != null && !parent.permittedSubclasses.contains(child.name))
            throw failure(child, "sealed parent " + name + " does not permit " + child.name);
        // The compiler links a single unnamed module, so sealed children must share a runtime package.
        if (parent.permittedSubclasses != null && !packageName(child.name).equals(packageName(name)))
            throw failure(child, "sealed parent and child must share a package in the unnamed module: " + name);
        return parent;
    }

    private static String packageName(String name) {
        int slash = name.lastIndexOf('/');
        return slash < 0 ? "" : name.substring(0, slash);
    }

    private static IllegalArgumentException failure(ClassNode owner, String reason) {
        return BytecodeCompatibility.failure(owner, null, null, "hierarchy declaration", reason);
    }
}
