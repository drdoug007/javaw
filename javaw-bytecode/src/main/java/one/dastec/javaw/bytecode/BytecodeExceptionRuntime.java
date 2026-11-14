package one.dastec.javaw.bytecode;

import org.objectweb.asm.tree.ClassNode;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Explicit, isolated exception runtime. Requires a JDK to compile its bundled Java sources once. */
public final class BytecodeExceptionRuntime {
    private static Map<String,byte[]> compiled;
    private static final String[] NAMES = ("FdLibm Math StrictMath Runnable Iterable NumberFormatException ReflectiveOperationException InstantiationException NoSuchFieldException NoSuchMethodException IllegalAccessException Deprecated FunctionalInterface SafeVarargs ClassFormatError TypeNotPresentException EnumConstantNotPresentException Enum ClassLoader AutoCloseable Cloneable CloneNotSupportedException Class Record Comparable Number Boolean Byte Character Short Integer Long Float Double Void Throwable CharSequence StringIndexOutOfBoundsException StringBuilder StringBuffer String StackTraceElement Exception RuntimeException Error LinkageError "
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
            paths.add("java/io/IOException.java");
            paths.add("java/io/UnsupportedEncodingException.java");
            for(String name:List.of("Closeable","Flushable","OutputStream","Writer"))paths.add("java/io/"+name+".java");
            paths.add("java/lang/Appendable.java");
            paths.add("javaw/runtime/TextCharsets.java");
            paths.add("javaw/runtime/SystemProperties.java");
            paths.add("javaw/runtime/CharacterStreams.java");
            for(String name:List.of("Pattern","Matcher","MatchResult","PatternSyntaxException"))paths.add("java/util/regex/"+name+".java");
            paths.add("java/util/Objects.java");
            paths.add("java/util/Locale.java");
            for(String name:"IllegalFormatException UnknownFormatConversionException UnknownFormatFlagsException DuplicateFormatFlagsException MissingFormatArgumentException MissingFormatWidthException IllegalFormatFlagsException IllegalFormatCodePointException IllegalFormatPrecisionException IllegalFormatWidthException IllegalFormatArgumentIndexException IllegalFormatConversionException FormatFlagsConversionMismatchException".split(" "))paths.add("java/util/"+name+".java");
            paths.add("javaw/runtime/TextFormats.java");
            paths.add("java/util/Arrays.java");
            paths.add("java/util/UUID.java");
            paths.add("java/lang/invoke/StringConcatException.java");
            paths.add("java/lang/invoke/LambdaConversionException.java");
            paths.add("java/lang/invoke/WrongMethodTypeException.java");
            paths.add("java/lang/invoke/SerializedLambda.java");
            paths.add("java/lang/invoke/MethodHandles.java");
            paths.add("java/lang/reflect/InvocationHandler.java");
            paths.add("java/lang/reflect/Method.java");
            paths.add("java/lang/reflect/AccessibleObject.java");
            paths.add("java/lang/reflect/Member.java");
            paths.add("java/lang/reflect/Modifier.java");
            paths.add("java/lang/reflect/Constructor.java");
            paths.add("java/lang/reflect/Field.java");
            paths.add("java/lang/reflect/Array.java");
            paths.add("java/lang/reflect/Executable.java");
            paths.add("java/lang/reflect/Parameter.java");
            for(String name:List.of("Type","GenericDeclaration","ParameterizedType","TypeVariable","GenericArrayType","WildcardType","GenericSignatureFormatError","AnnotatedType","AnnotatedArrayType","AnnotatedParameterizedType","AnnotatedWildcardType","AnnotatedTypeVariable"))paths.add("java/lang/reflect/"+name+".java");
            for(String name:List.of("Iterator","ListIterator","Collection","List","Queue","Deque","RandomAccess","AbstractCollection","AbstractList","AbstractSequentialList","ArrayList","LinkedList","Collections","Spliterator","Spliterators","stream/StreamSupport","IdentityHashMap", "SortedMap","NavigableMap","SortedSet","NavigableSet","TreeMap","TreeKeySet","TreeSet","SortedViews", "UnmodifiableViews","ImmutableCollections","Enumeration","ConcurrentModificationException","NoSuchElementException","StringJoiner","StringTokenizer","ArrayDeque","AbstractQueue","PriorityQueue","Vector","EmptyStackException","Stack","SynchronizedViews","BitSet","HexFormat","Base64"))paths.add("java/util/"+name+".java");
            for(String name:"BiConsumer BiFunction BiPredicate BinaryOperator BooleanSupplier Consumer DoubleBinaryOperator DoubleConsumer DoubleFunction DoublePredicate DoubleSupplier DoubleToIntFunction DoubleToLongFunction DoubleUnaryOperator Function IntBinaryOperator IntConsumer IntFunction IntPredicate IntSupplier IntToDoubleFunction IntToLongFunction IntUnaryOperator LongBinaryOperator LongConsumer LongFunction LongPredicate LongSupplier LongToDoubleFunction LongToIntFunction LongUnaryOperator ObjDoubleConsumer ObjIntConsumer ObjLongConsumer Predicate Supplier ToDoubleBiFunction ToDoubleFunction ToIntBiFunction ToIntFunction ToLongBiFunction ToLongFunction UnaryOperator".split(" "))paths.add("java/util/function/"+name+".java");
            for(String name:List.of("BaseStream","Stream","ReferenceStream","IntStream","IntPipeline","Collector","Collectors"))paths.add("java/util/stream/"+name+".java");
            paths.add("java/util/Comparator.java");
            paths.add("java/util/concurrent/atomic/AtomicReferenceArray.java");
            paths.add("java/util/PrimitiveIterator.java");
            paths.add("java/util/IntSummaryStatistics.java");
            for(String name:List.of("Optional","OptionalInt","OptionalLong","OptionalDouble","Map","AbstractMap","HashMap","LinkedHashMap","Set","AbstractSet","HashSet","LinkedHashSet"))paths.add("java/util/"+name+".java");
            paths.add("javaw/runtime/IntegralNumbers.java");
            paths.add("javaw/runtime/UnicodeCase.java");
            paths.add("javaw/runtime/StringUpperData.java");
            paths.add("javaw/runtime/StringLowerData.java");
            paths.add("javaw/runtime/WordBreaks.java");
            paths.add("javaw/runtime/UnicodeProperties.java");
            paths.add("javaw/runtime/FloatingNumbers.java");
            paths.add("javaw/runtime/FusedMath.java");
            paths.add("java/math/BigInteger.java");
            paths.add("java/math/BigDecimal.java");
            paths.add("java/math/RoundingMode.java");
            paths.add("java/math/MathContext.java");
            paths.add("javaw/runtime/GenericSignatures.java");
            paths.add("javaw/runtime/AnnotatedTypes.java");
            paths.add("java/lang/reflect/AnnotatedElement.java");
            for(String name:List.of("Annotation","RetentionPolicy","ElementType","Retention","Target","Inherited","Documented","Repeatable","IncompleteAnnotationException","AnnotationTypeMismatchException","AnnotationFormatError"))paths.add("java/lang/annotation/"+name+".java");
            paths.add("javaw/runtime/AnnotationValue.java");
            paths.add("java/lang/reflect/Proxy.java");
            paths.add("java/lang/reflect/UndeclaredThrowableException.java");
            paths.add("java/lang/reflect/InvocationTargetException.java");
            paths.add("java/lang/invoke/MethodType.java");
            paths.add("java/lang/invoke/MethodHandle.java");
            paths.add("java/lang/invoke/VarHandle.java");
            paths.add("java/nio/ByteOrder.java");
            for(String name:List.of("Charset","StandardCharsets","IllegalCharsetNameException","UnsupportedCharsetException"))paths.add("java/nio/charset/"+name+".java");
            paths.add("java/security/GeneralSecurityException.java");
            paths.add("java/security/NoSuchAlgorithmException.java");
            paths.add("java/security/DigestException.java");
            paths.add("java/security/MessageDigestSpi.java");
            paths.add("java/security/MessageDigest.java");
            paths.add("javaw/runtime/Digests.java");
            paths.add("javaw/runtime/BlockDigest.java");
            paths.add("javaw/runtime/Sha256.java");
            paths.add("javaw/runtime/Sha1.java");
            paths.add("javaw/runtime/Md5.java");
            paths.add("javaw/runtime/Sha512.java");
            paths.add("java/security/Key.java");
            paths.add("java/security/KeyException.java");
            paths.add("java/security/InvalidKeyException.java");
            paths.add("java/security/spec/KeySpec.java");
            paths.add("javax/security/auth/Destroyable.java");
            paths.add("javax/security/auth/DestroyFailedException.java");
            paths.add("javax/crypto/SecretKey.java");
            paths.add("javax/crypto/spec/SecretKeySpec.java");
            paths.add("javax/crypto/Mac.java");
            paths.add("java/util/zip/Checksum.java");
            paths.add("java/util/zip/CRC32.java");
            paths.add("java/util/zip/Adler32.java");
            paths.add("java/io/ByteArrayOutputStream.java");
            paths.add("javaw/runtime/ExceptionSupport.java");
            paths.add("javaw/runtime/ReflectionSupport.java");
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
            try(var files=Files.walk(output)) {
                for(Path file:files.filter(f->f.toString().endsWith(".class")).sorted().toList()) {
                    String name=output.relativize(file).toString().replace(File.separatorChar,'/');
                    bytes.put(name.substring(0,name.length()-6),Files.readAllBytes(file));
                }
            }
            compiled=Collections.unmodifiableMap(bytes);return compiled;
        } finally {
            try(var paths=Files.walk(temporary)) {
                for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);
            }
        }
    }
}
