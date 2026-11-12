package one.dastec.javaw.bytecode;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

/**
 * Static {@code native} methods bound to named Wasm imports ({@link BytecodeCompiler.NativeImport}). A bound method
 * compiles to a wrapper function: it initializes its class, as any static method does, calls the import with the
 * Java arguments, and narrows a sub-int result. Only primitive values cross, so a host can satisfy the import with
 * another module's export directly. See docs/foreign-wasm-modules.md.
 */
final class BytecodeNatives {
    private BytecodeNatives() {}

    /**
     * Marks a bound method. A class-file attribute is read as a plain {@link Attribute}, never as this class, so input
     * bytecode cannot bind itself; only {@link #bind} does.
     */
    static final class Binding extends Attribute {
        final String module, field, function;
        private Binding(String module, String field, String function) {
            super("javaw.NativeImport");
            this.module = module; this.field = field; this.function = function;
        }
    }

    static Binding binding(MethodNode method) {
        if ((method.access & Opcodes.ACC_NATIVE) == 0 || method.attrs == null) return null;
        for (Attribute attribute : method.attrs) if (attribute instanceof Binding binding) return binding;
        return null;
    }

    /** Validates {@code imports} against the input classes (before the bundled runtime is added) and marks each method. */
    static void bind(List<ClassNode> inputs, List<BytecodeCompiler.NativeImport> imports, boolean exceptionRuntime, boolean managedHeap) {
        if (imports.isEmpty()) return;
        if (!exceptionRuntime)
            throw new IllegalArgumentException("native imports require the bundled exception runtime (--exception-runtime)");
        Map<String, ClassNode> classes = new HashMap<>();
        for (ClassNode type : inputs) classes.putIfAbsent(type.name, type);
        Set<MethodNode> bound = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<String, BytecodeCompiler.NativeImport> names = new HashMap<>();
        for (BytecodeCompiler.NativeImport binding : imports) {
            if (binding.module().equals("env") && (binding.field().equals("memory") || !managedHeap && binding.field().equals("__heap_alloc")))
                throw new IllegalArgumentException("native import " + binding + " names env." + binding.field()
                        + ", which the module already imports for its own runtime; choose another module or field name");
            ClassNode owner = classes.get(binding.owner());
            if (owner == null) {
                // A nested class's binary name uses '$'; suggest it when the dotted canonical name was given.
                String hint = "bind methods of the compiled JAR or class only";
                for (String candidate = binding.owner(); candidate.lastIndexOf('/') > 0; ) {
                    int slash = candidate.lastIndexOf('/');
                    candidate = candidate.substring(0, slash) + '$' + candidate.substring(slash + 1);
                    if (classes.containsKey(candidate)) { hint = "for a nested class use the binary name " + candidate.replace('/', '.'); break; }
                }
                throw new IllegalArgumentException("native import " + binding + " names " + binding.owner().replace('/', '.')
                        + ", which is not an input class; " + hint);
            }
            MethodNode method = null;
            for (MethodNode candidate : owner.methods)
                if (candidate.name.equals(binding.method()) && candidate.desc.equals(binding.descriptor())) { method = candidate; break; }
            if (method == null)
                throw new IllegalArgumentException("native import " + binding + " names no declared method; give the exact name and descriptor");
            if ((method.access & Opcodes.ACC_NATIVE) == 0)
                throw failure(owner, method, "is not native; only native methods can be bound to imports");
            if ((method.access & Opcodes.ACC_STATIC) == 0)
                throw failure(owner, method, "is an instance method; only static native methods can be bound, because a receiver cannot cross a Wasm import");
            Type[] parameters = Type.getArgumentTypes(method.desc);
            for (int i = 0; i < parameters.length; i++)
                if (!primitive(parameters[i]))
                    throw failure(owner, method, "parameter " + (i + 1) + " has type " + parameters[i].getClassName()
                            + ", which cannot cross a Wasm import; only boolean, byte, char, short, int, long, float and double values can");
            Type result = Type.getReturnType(method.desc);
            if (result.getSort() != Type.VOID && !primitive(result))
                throw failure(owner, method, "returns " + result.getClassName()
                        + ", which cannot cross a Wasm import; return void or a boolean, byte, char, short, int, long, float or double value");
            if (!bound.add(method))
                throw failure(owner, method, "is bound by more than one native import");
            BytecodeCompiler.NativeImport previous = names.putIfAbsent(binding.module() + '\0' + binding.field(), binding);
            if (previous != null)
                throw new IllegalArgumentException("native imports " + previous + " and " + binding
                        + " name the same import; bind each import to one method");
            if (method.attrs == null) method.attrs = new ArrayList<>();
            method.attrs.add(new Binding(binding.module(), binding.field(),
                    importFunction(owner.name, method.name, method.desc)));
        }
    }

    /** One import per bound method, in input-class then method order; every binding is imported even if never called. */
    static String importsWat(List<ClassNode> classes) {
        StringBuilder wat = new StringBuilder();
        for (ClassNode type : classes)
            for (MethodNode method : type.methods) {
                Binding binding = binding(method);
                if (binding == null) continue;
                wat.append("    (import \"").append(importName(binding.module)).append("\" \"")
                        .append(importName(binding.field)).append("\" (func ").append(binding.function);
                for (Type parameter : Type.getArgumentTypes(method.desc))
                    wat.append(" (param ").append(TypeHelper.getWasmType(parameter)).append(')');
                Type result = Type.getReturnType(method.desc);
                if (result.getSort() != Type.VOID) wat.append(" (result ").append(TypeHelper.getWasmType(result)).append(')');
                wat.append("))\n");
            }
        return wat.toString();
    }

    /**
     * A quoted import name's text. Dead-code pruning gives up on a module whose text contains one of its markers
     * ({@code call_indirect}, {@code (table}, {@code (;}), so a name containing one is written with {@code \hh} escapes,
     * which decode to the same bytes.
     */
    private static String importName(String value) {
        String text = TypeHelper.watString(value);
        if (BytecodeDeadCode.UNPRUNABLE.stream().noneMatch(text::contains)) return text;
        return text.replace("_", "\\5f").replace("(", "\\28").replace(";", "\\3b");
    }

    /** The wrapper body after the function header: arguments, the import call, and JNI-style narrowing of the result. */
    static String bodyWat(Binding binding, MethodNode method, String initialization) {
        StringBuilder wat = new StringBuilder();
        if (!initialization.isEmpty()) wat.append("        ").append(initialization).append('\n');
        // Verified bytecode may pass any int for a sub-int parameter, and a foreign function may return any i32. Both
        // directions are read as a JNI implementation reads jboolean, jbyte, jchar and jshort.
        Type[] parameters = Type.getArgumentTypes(method.desc);
        for (int i = 0; i < parameters.length; i++) {
            wat.append("        local.get $p").append(i).append('\n');
            narrow(wat, parameters[i]);
        }
        wat.append("        call ").append(binding.function).append('\n');
        narrow(wat, Type.getReturnType(method.desc));
        return wat.toString();
    }

    private static void narrow(StringBuilder wat, Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN -> wat.append("        i32.const 255\n        i32.and\n        i32.const 0\n        i32.ne\n");
            case Type.BYTE -> wat.append("        i32.extend8_s\n");
            case Type.SHORT -> wat.append("        i32.extend16_s\n");
            case Type.CHAR -> wat.append("        i32.const 65535\n        i32.and\n");
            default -> { }
        }
    }

    /** The WAT id of a bound method's import: the method function's id with {@code $__native_} for {@code $method_}. */
    static String importFunction(String owner, String method, String descriptor) {
        return "$__native_" + TypeHelper.getMethodFunctionName(owner, method, descriptor).substring("$method_".length());
    }

    private static boolean primitive(Type type) {
        return type.getSort() >= Type.BOOLEAN && type.getSort() <= Type.DOUBLE;
    }

    private static IllegalArgumentException failure(ClassNode owner, MethodNode method, String problem) {
        return BytecodeCompatibility.failure(owner, method, null, "native import binding",
                owner.name.replace('/', '.') + "." + method.name + method.desc + " " + problem);
    }
}
