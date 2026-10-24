package com.example.javaw.bytecode;

import org.objectweb.asm.tree.ClassNode;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Explicit, isolated exception runtime. Requires a JDK to compile its bundled Java sources once. */
public final class BytecodeExceptionRuntime {
    private static Map<String,byte[]> compiled;
    private static final String[] NAMES = ("AutoCloseable Cloneable CloneNotSupportedException Class Record Comparable Number Boolean Byte Character Short Integer Long Float Double Void Throwable String StackTraceElement Exception RuntimeException Error LinkageError "
            + "VirtualMachineError InternalError OutOfMemoryError StackOverflowError NullPointerException ClassCastException ArithmeticException "
            + "IndexOutOfBoundsException ArrayIndexOutOfBoundsException ArrayStoreException NegativeArraySizeException "
            + "IllegalArgumentException IllegalStateException UnsupportedOperationException IncompatibleClassChangeError AbstractMethodError IllegalAccessError "
            + "BootstrapMethodError ExceptionInInitializerError NoSuchMethodError NoSuchFieldError NoClassDefFoundError InstantiationError UnsatisfiedLinkError").split(" ");

    private BytecodeExceptionRuntime() {}

    static boolean enabled(Map<String,ClassNode> linked) { return linked.containsKey("javaw/runtime/ExceptionSupport"); }

    /** Fresh ASM nodes prevent normalization or caller edits from mutating the cached runtime. */
    static Map<String,ClassNode> definitions() throws IOException {
        Map<String,ClassNode> result=new LinkedHashMap<>();
        for(var entry:classBytes().entrySet())result.put(entry.getKey(),BytecodeInput.read(entry.getValue()));
        return result;
    }

    private static synchronized Map<String,byte[]> classBytes() throws IOException {
        if(compiled!=null)return compiled;
        var compiler=ToolProvider.getSystemJavaCompiler();
        if(compiler==null)throw new IOException("The bundled bytecode exception runtime requires a JDK compiler");
        Path temporary=Files.createTempDirectory("javaw-exception-runtime-");
        try {
            Path source=temporary.resolve("source"),output=temporary.resolve("classes");
            List<String> paths=new ArrayList<>();
            for(String name:NAMES)paths.add("java/lang/"+name+".java");
            paths.add("java/io/Serializable.java");
            paths.add("java/util/Objects.java");
            paths.add("java/util/Arrays.java");
            paths.add("java/util/UUID.java");
            paths.add("java/lang/invoke/StringConcatException.java");
            paths.add("java/lang/invoke/LambdaConversionException.java");
            paths.add("java/lang/invoke/WrongMethodTypeException.java");
            paths.add("java/lang/invoke/SerializedLambda.java");
            paths.add("java/lang/invoke/MethodHandles.java");
            paths.add("java/lang/invoke/VarHandle.java");
            paths.add("java/nio/ByteOrder.java");
            paths.add("javaw/runtime/ExceptionSupport.java");
            paths.add("javaw/runtime/Decimal.java");
            List<String> arguments=new ArrayList<>(List.of("-g","-source","17","-target","17","-Xlint:-options",
                    "--patch-module","java.base="+source,"-d",output.toString()));
            for(String path:paths) {
                Path file=source.resolve(path);Files.createDirectories(file.getParent());
                try(InputStream input=BytecodeExceptionRuntime.class.getResourceAsStream("/exception-runtime/"+path)) {
                    if(input==null)throw new IOException("Missing exception runtime source: "+path);
                    Files.copy(input,file);
                }
                arguments.add(file.toString());
            }
            ByteArrayOutputStream errors=new ByteArrayOutputStream();
            if(compiler.run(null,errors,errors,arguments.toArray(String[]::new))!=0)
                throw new IOException("Cannot build bytecode exception runtime: "+errors);
            Map<String,byte[]> bytes=new LinkedHashMap<>();
            for(String path:paths) {
                String name=path.substring(0,path.length()-5);
                bytes.put(name,Files.readAllBytes(output.resolve(name+".class")));
            }
            compiled=Collections.unmodifiableMap(bytes);return compiled;
        } finally {
            try(var paths=Files.walk(temporary)) {
                for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);
            }
        }
    }
}
