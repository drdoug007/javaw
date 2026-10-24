package com.example.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.jar.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeLinkageTest implements Opcodes {
    private ClassNode type(String name) {
        ClassNode node = new ClassNode(); node.name = name; node.superName = "java/lang/Object";
        node.version = V17; node.access = ACC_PUBLIC; node.sourceFile = "Declarations.java";
        return node;
    }

    private MethodNode value(int access) {
        MethodNode method = new MethodNode(access, "value", "()I", null, null);
        if ((access & (ACC_ABSTRACT | ACC_NATIVE)) == 0) {
            method.instructions.add(new InsnNode(ICONST_1)); method.instructions.add(new InsnNode(IRETURN));
            method.maxStack = 1; method.maxLocals = (access & ACC_STATIC) == 0 ? 1 : 0;
        }
        return method;
    }

    private byte[] bytes(ClassNode node) {
        ClassWriter writer = new ClassWriter(0); node.accept(writer); return writer.toByteArray();
    }

    private Class<?> define(byte[] bytes) {
        return new ClassLoader() { Class<?> load() { return defineClass(null, bytes, 0, bytes.length); } }.load();
    }

    private String rejectsUnused(Consumer<ClassNode> change, String reason) {
        ClassNode entry = type("Entry"), unused = type("Unused"); change.accept(unused);
        String error = assertThrows(IllegalArgumentException.class, () -> new ClassBytecodeCompiler(entry)
                .compileToWat(Map.of(entry.name, entry, unused.name, unused))).getMessage();
        assertTrue(error.contains(reason), error); assertTrue(error.contains("Declarations.java:"), error);
        return error;
    }

    @Test void checksUnusedClassInterfaceAndCodeDeclarationsLikeJvm() {
        List<Consumer<ClassNode>> invalid = List.of(
                n -> n.access |= ACC_INTERFACE | ACC_ABSTRACT | ACC_SUPER,
                n -> n.access |= ACC_INTERFACE | ACC_ABSTRACT | ACC_ENUM,
                n -> n.access |= ACC_ANNOTATION,
                n -> { MethodNode m = value(ACC_PUBLIC | ACC_ABSTRACT); m.instructions.add(new InsnNode(RETURN)); n.methods.add(m); },
                n -> { MethodNode m = value(ACC_PUBLIC | ACC_NATIVE); m.instructions.add(new InsnNode(RETURN)); n.methods.add(m); },
                n -> n.methods.add(new MethodNode(ACC_PUBLIC, "empty", "()I", null, null)),
                n -> { n.nestHostClass = "Host"; n.nestMembers = new ArrayList<>(List.of("Member")); }
        );
        for (Consumer<ClassNode> change : invalid) {
            ClassNode node = type("Invalid"); change.accept(node);
            assertThrows(ClassFormatError.class, () -> define(bytes(node)), node.access + "");
            rejectsUnused(change, "member declaration");
        }
        rejectsUnused(n -> n.access |= ACC_MODULE, "unnamed closed-world target");
        rejectsUnused(n -> n.name = "bad.name", "invalid internal class name");
        rejectsUnused(n -> { n.version = V10; n.nestHostClass = "Host"; }, "nest attributes require Java 11");
    }

    @Test void treatsStrictAndClassInitializerFlagsAccordingToClassVersion() {
        for (int version : new int[]{V1_1, V1_2, V16, V17}) {
            ClassNode node = type("Strict"); node.version = version; node.access |= ACC_ABSTRACT;
            node.methods.add(value(ACC_PUBLIC | ACC_ABSTRACT | ACC_STRICT));
            boolean strictApplies = version == V1_2 || version == V16;
            if (strictApplies) {
                if (version == V16) assertThrows(ClassFormatError.class, () -> define(bytes(node)), "version=" + version);
                assertThrows(IllegalArgumentException.class, () -> BytecodeDeclarations.validate(node));
            } else {
                assertDoesNotThrow(() -> define(bytes(node)));
                assertDoesNotThrow(() -> BytecodeDeclarations.validate(node));
            }
        }
        for (int version : new int[]{V1_6, V17}) {
            ClassNode node = type("Initializer"); node.version = version;
            // <clinit> ignores access flags other than the version-dependent static requirement.
            MethodNode init = new MethodNode(ACC_STATIC | ACC_PRIVATE | ACC_PUBLIC | ACC_ABSTRACT | ACC_NATIVE,
                    "<clinit>", "()V", null, null);
            init.instructions.add(new InsnNode(RETURN)); node.methods.add(init);
            assertDoesNotThrow(() -> define(bytes(node)));
            assertDoesNotThrow(() -> BytecodeDeclarations.validate(node));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> new ClassBytecodeCompiler(node).compileToWat())
                    .getMessage().contains("class initializer"));
        }
    }

    @Test void resolvesUnusedDescriptorComponentsWithoutHostClassLoading() {
        for (String descriptor : new String[]{"Lexternal/Missing;", "[[Lexternal/Missing;"})
            rejectsUnused(n -> n.fields.add(new FieldNode(0, "field", descriptor, null, null)), "descriptor resolution");
        rejectsUnused(n -> {
            MethodNode method = value(ACC_PUBLIC | ACC_ABSTRACT); method.desc = "(Lexternal/Argument;)V"; n.methods.add(method);
        }, "external/Argument");
        rejectsUnused(n -> {
            MethodNode method = value(ACC_PUBLIC | ACC_ABSTRACT); method.desc = "()Lexternal/Result;"; n.methods.add(method);
        }, "external/Result");
        rejectsUnused(n -> {
            MethodNode method = value(ACC_PUBLIC | ACC_ABSTRACT); method.exceptions.add("external/Failure"); n.methods.add(method);
        }, "external/Failure");
        rejectsUnused(n -> n.fields.add(new FieldNode(0, "jdk", "Ljava/lang/String;", null, null)), "compiler-host JDK");
        ClassNode owner = type("q/Owner"), hidden = type("p/Hidden"); hidden.access = 0;
        // A descriptor may name an inaccessible class; it does not confer access to its members.
        owner.fields.add(new FieldNode(0, "field", "[[Lp/Hidden;", null, null));
        owner.fields.add(new FieldNode(0, "root", "Ljava/lang/Object;", null, null));
        assertDoesNotThrow(() -> new ClassBytecodeCompiler(owner).compileToWat(Map.of(owner.name, owner, hidden.name, hidden)));
    }

    @Test void validatesUnusedHierarchiesOverridesAndUnnamedSealedPackages() {
        rejectsUnused(n -> n.superName = "Missing", "missing superclass");
        rejectsUnused(n -> n.interfaces.add(n.name), "wrong superclass/superinterface kind");
        ClassNode entry = type("Entry"), parent = type("Parent"), unused = type("Unused");
        parent.methods.add(value(ACC_PUBLIC | ACC_FINAL)); unused.superName = parent.name; unused.methods.add(value(ACC_PUBLIC));
        var linked = Map.of(entry.name, entry, parent.name, parent, unused.name, unused);
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new ClassBytecodeCompiler(entry).compileToWat(linked)).getMessage().contains("final instance method"));
        ClassNode sealed = type("p/Sealed"), child = type("q/Child");
        sealed.permittedSubclasses = new ArrayList<>(List.of(child.name)); child.superName = sealed.name;
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new ClassBytecodeCompiler(child)
                .compileToWat(Map.of(sealed.name, sealed, child.name, child))).getMessage().contains("unnamed module"));
    }

    @Test void rejectsDuplicateJarClassIdentitiesAndMismatchedMetadataKeys() throws Exception {
        ClassNode node = type("Duplicate"); var output = new ByteArrayOutputStream();
        try (var jar = new JarOutputStream(output)) {
            for (String path : List.of("Duplicate.class", "other/Copy.class")) {
                jar.putNextEntry(new JarEntry(path)); jar.write(bytes(node)); jar.closeEntry();
            }
        }
        assertTrue(assertThrows(IllegalArgumentException.class, () -> BytecodeCompiler.compileJar(
                new ByteArrayInputStream(output.toByteArray()))).getMessage().contains("duplicate class declaration"));
        assertThrows(IllegalArgumentException.class, () -> new ClassBytecodeCompiler(node).compileToWat(Map.of("Wrong", node)));
    }

    @Test void rejectsAllStaticInstanceOpcodeConflictsAtLinkage() {
        ClassNode owner = type("Owner"), caller = type("Caller");
        owner.fields.add(new FieldNode(ACC_PUBLIC, "instance", "I", null, null));
        owner.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, "statik", "I", null, null));
        owner.methods.add(value(ACC_PUBLIC));
        MethodNode statik = value(ACC_PUBLIC | ACC_STATIC); statik.name = "statik"; owner.methods.add(statik);
        var linked = Map.of(owner.name, owner, caller.name, caller);
        BytecodeDispatch dispatch = new BytecodeDispatch(linked, caller.name);
        for (int opcode : new int[]{GETFIELD, PUTFIELD, GETSTATIC, PUTSTATIC}) {
            boolean staticAccess = opcode == GETSTATIC || opcode == PUTSTATIC;
            var field = new FieldInsnNode(opcode, owner.name, staticAccess ? "instance" : "statik", "I");
            assertTrue(assertThrows(IllegalArgumentException.class, () -> dispatch.fieldOwner(field))
                    .getMessage().contains("static/instance"));
        }
        assertThrows(IllegalArgumentException.class,
                () -> dispatch.staticTarget(new MethodInsnNode(INVOKESTATIC, owner.name, "value", "()I", false)));
        assertThrows(IllegalArgumentException.class,
                () -> dispatch.targets(new MethodInsnNode(INVOKEVIRTUAL, owner.name, "statik", "()I", false)));
        assertThrows(IllegalArgumentException.class,
                () -> dispatch.specialTarget(new MethodInsnNode(INVOKESPECIAL, owner.name, "statik", "()I", false)));
    }

    @Test void rejectsImplicitObjectFinalOverridesInClassesAndInterfacesLikeJvm() {
        for (boolean iface : new boolean[]{false, true}) {
            for (String signature : List.of("notify()V", "notifyAll()V", "wait()V", "wait(J)V", "wait(JI)V",
                    "getClass()Ljava/lang/Class;")) {
                ClassNode node = type("FinalObjectOverride");
                if (iface) node.access |= ACC_INTERFACE | ACC_ABSTRACT;
                int split = signature.indexOf('(');
                MethodNode method = new MethodNode(ACC_PUBLIC, signature.substring(0, split), signature.substring(split), null, null);
                boolean reference = method.desc.endsWith(";");
                if (reference) method.instructions.add(new InsnNode(ACONST_NULL));
                method.instructions.add(new InsnNode(reference ? ARETURN : RETURN)); method.maxStack = 1; method.maxLocals = 4;
                node.methods.add(method);
                assertThrows(IncompatibleClassChangeError.class, () -> define(bytes(node)), signature);
                String error = assertThrows(IllegalArgumentException.class,
                        () -> new ClassBytecodeCompiler(node).compileToWat()).getMessage();
                assertTrue(error.contains("final java/lang/Object method"), error);
            }
        }
        for (int access : new int[]{ACC_PUBLIC | ACC_STATIC, ACC_PRIVATE}) {
            ClassNode legal = type("SeparateObjectSlot");
            MethodNode method = new MethodNode(access, "notify", "()V", null, null);
            method.instructions.add(new InsnNode(RETURN)); method.maxLocals = 1; legal.methods.add(method);
            assertDoesNotThrow(() -> define(bytes(legal)));
            assertDoesNotThrow(() -> new ClassBytecodeCompiler(legal).compileToWat());
        }
    }

    @Test void rejectsObjectConstructorWithInterfaceReferenceInsteadOfSkippingIt() {
        ClassNode node = type("BadObjectReference");
        MethodNode init = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        init.instructions.add(new VarInsnNode(ALOAD, 0));
        init.instructions.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", true));
        init.instructions.add(new InsnNode(RETURN)); init.maxLocals = 1; init.maxStack = 1; node.methods.add(init);
        var jvmError = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> define(bytes(node)).getConstructor().newInstance());
        assertInstanceOf(IncompatibleClassChangeError.class, jvmError.getCause());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new ClassBytecodeCompiler(node).compileToWat())
                .getMessage().contains("interface super call"));
    }

    @Test void checksMemberReferenceSyntaxAndInterfaceVersionBeforeLookup() {
        ClassNode owner = type("Caller"); MethodNode method = value(ACC_PUBLIC);
        for (String descriptor : List.of("I", "(V)V", "()VI")) {
            var call = new MethodInsnNode(INVOKESPECIAL, owner.name, "value", descriptor, false);
            assertThrows(IllegalArgumentException.class, () -> BytecodeLinkage.validateReference(owner, method, call));
        }
        var call = new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", true);
        assertFalse(BytecodeDispatch.isObjectConstructor(call));
        owner.version = V1_7;
        assertThrows(IllegalArgumentException.class, () -> BytecodeLinkage.validateReference(owner, method, call));
        owner.version = V1_8;
        assertDoesNotThrow(() -> BytecodeLinkage.validateReference(owner, method, call));
        assertThrows(IllegalArgumentException.class, () -> BytecodeLinkage.validateReference(owner, method,
                new FieldInsnNode(GETSTATIC, "Caller", "field", "()I")));
    }
}
