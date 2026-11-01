package one.dastec.javaw.bytecode;

import one.dastec.javaw.FloatingPointSupport;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class BytecodeCompiler {

    /**
     * Linear-memory configuration. Memory is imported as {@code env.memory}; without a maximum the host chooses how far
     * it may grow. {@code managedHeap} (bundled exception runtime only, and its default) uses a module-owned reclaiming
     * allocator exporting {@code __gc_collect}, {@code __gc_retain}, {@code __gc_release}, {@code __gc_live_bytes},
     * {@code __gc_heap_bytes} and the debug switch {@code __gc_scribble}. Otherwise the host supplies
     * {@code env.__heap_alloc}, as it always does without the bundled runtime.
     */
    public record Memory(int initialPages, Integer maximumPages, boolean managedHeap) {
        public Memory {
            if (initialPages < 1 || initialPages > 65536)
                throw new IllegalArgumentException("initial memory must be between 1 and 65536 pages");
            if (maximumPages != null && (maximumPages < initialPages || maximumPages > 65536))
                throw new IllegalArgumentException("maximum memory must be between the initial size and 65536 pages");
        }
        public static Memory managed() { return new Memory(2, null, true); }
        public static Memory hostAllocator() { return new Memory(2, null, false); }
        String importWat() {
            return "    (import \"env\" \"memory\" (memory " + initialPages + (maximumPages == null ? "" : " " + maximumPages) + "))\n";
        }
    }

    public static String compileClass(byte[] classBytes) {
        ClassBytecodeCompiler compiler = ClassBytecodeCompiler.fromBytes(classBytes);
        return compiler.compileToWat();
    }

    public static String compileClass(InputStream classInputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = classInputStream.read(buf)) != -1) {
            baos.write(buf, 0, r);
        }
        return compileClass(baos.toByteArray());
    }

    public static String compileJarWithExceptionRuntime(InputStream input) throws IOException {
        return compileJar(input, true,List.of());
    }

    public static String compileJarWithExceptionRuntime(InputStream input,List<List<String>> proxyShapes) throws IOException {
        return compileJar(input,true,proxyShapes);
    }

    /**
     * Retains the named WAT functions (e.g. {@code $method_...}) through dead-code pruning, for hosts that add
     * exports to the returned module text. Linked-class exports and host hooks are always retained.
     */
    public static String compileJarWithExceptionRuntime(InputStream input,Set<String> retainedFunctions) throws IOException {
        return compileJar(input,true,List.of(),retainedFunctions);
    }

    /** Selects the linear-memory configuration, e.g. {@link Memory#hostAllocator()} to import {@code env.__heap_alloc}. */
    public static String compileJarWithExceptionRuntime(InputStream input,Memory memory) throws IOException {
        return compileJar(input,true,List.of(),Set.of(),memory);
    }

    public static String compileJarWithExceptionRuntime(InputStream input,List<List<String>> proxyShapes,Set<String> retainedFunctions,Memory memory) throws IOException {
        return compileJar(input,true,proxyShapes,retainedFunctions,memory);
    }

    public static String compileClassWithExceptionRuntime(byte[] bytes,Memory memory) throws IOException {
        return compileClasses(new ArrayList<>(List.of(BytecodeInput.read(bytes))),true,List.of(),Set.of(),memory);
    }

    public static String compileClassWithExceptionRuntime(byte[] bytes) throws IOException {
        return compileClasses(new ArrayList<>(List.of(BytecodeInput.read(bytes))), true);
    }

    public static String compileClassWithExceptionRuntime(byte[] bytes,List<List<String>> proxyShapes) throws IOException {
        return compileClasses(new ArrayList<>(List.of(BytecodeInput.read(bytes))),true,proxyShapes);
    }

    public static String compileJar(InputStream jarInputStream) throws IOException { return compileJar(jarInputStream, false); }

    private static String compileJar(InputStream jarInputStream, boolean exceptionRuntime) throws IOException {return compileJar(jarInputStream,exceptionRuntime,List.of());}
    private static String compileJar(InputStream jarInputStream, boolean exceptionRuntime,List<List<String>> proxyShapes) throws IOException {
        return compileJar(jarInputStream,exceptionRuntime,proxyShapes,Set.of());
    }
    private static String compileJar(InputStream jarInputStream, boolean exceptionRuntime,List<List<String>> proxyShapes,Set<String> retainedFunctions) throws IOException {
        return compileJar(jarInputStream,exceptionRuntime,proxyShapes,retainedFunctions,exceptionRuntime ? Memory.managed() : Memory.hostAllocator());
    }
    private static String compileJar(InputStream jarInputStream, boolean exceptionRuntime,List<List<String>> proxyShapes,Set<String> retainedFunctions,Memory memory) throws IOException {
        JarInputStream jis = new JarInputStream(jarInputStream);
        JarEntry entry;
        List<ClassNode> classes = new ArrayList<>();

        while ((entry = jis.getNextJarEntry()) != null) {
            if (entry.getName().endsWith(".class") && !entry.getName().equals("module-info.class")) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int r;
                while ((r = jis.read(buf)) != -1) {
                    baos.write(buf, 0, r);
                }
                ClassNode cn = BytecodeInput.read(baos.toByteArray());
                classes.add(cn);
            }
        }

        return compileClasses(classes, exceptionRuntime,proxyShapes,retainedFunctions,memory);
    }

    private static String compileClasses(List<ClassNode> classes, boolean exceptionRuntime) throws IOException {return compileClasses(classes,exceptionRuntime,List.of());}
    private static String compileClasses(List<ClassNode> classes, boolean exceptionRuntime,List<List<String>> proxyShapes) throws IOException {
        return compileClasses(classes,exceptionRuntime,proxyShapes,Set.of());
    }
    private static String compileClasses(List<ClassNode> classes, boolean exceptionRuntime,List<List<String>> proxyShapes,Set<String> retainedFunctions) throws IOException {
        return compileClasses(classes,exceptionRuntime,proxyShapes,retainedFunctions,exceptionRuntime ? Memory.managed() : Memory.hostAllocator());
    }
    static String compileClasses(List<ClassNode> classes, boolean exceptionRuntime,List<List<String>> proxyShapes,Set<String> retainedFunctions,Memory memory) throws IOException {
        if (memory.managedHeap() && !exceptionRuntime)
            throw new IllegalArgumentException("the managed heap requires the bundled exception runtime");
        Set<String> roots = new HashSet<>();
        // Export names are emitted as escaped WAT strings; compare roots in the same form.
        for (ClassNode type : classes) roots.add(TypeHelper.watString(type.name.replace('/', '.') + "."));
        if (exceptionRuntime) {
            Map<String, ClassNode> runtime = BytecodeExceptionRuntime.definitions();
            for (ClassNode type : classes)
                if (runtime.containsKey(type.name))
                    throw BytecodeCompatibility.failure(type, null, null, "exception runtime redefinition",
                            "use the isolated bundled runtime or explicitly linked definitions, not both");
            classes.addAll(runtime.values());
        }
        if (exceptionRuntime) {
            BytecodeSystemProperties.normalize(classes);
            BytecodeReflection.normalize(classes);
            BytecodeProxies.normalize(classes,proxyShapes);
            BytecodeLookups.normalize(classes);
            BytecodeHandles.normalize(classes);
            BytecodeDynamic.normalize(classes);
            BytecodeInvokes.normalize(classes);
        }
        StringBuilder combinedWat = new StringBuilder();
        combinedWat.append("(module\n");
        combinedWat.append(memory.importWat());
        if (!memory.managedHeap())
            combinedWat.append("    (import \"env\" \"__heap_alloc\" (func $__heap_alloc (param i32) (result i32)))\n");

        if (classes.stream().anyMatch(ClassBytecodeCompiler::usesFloatingRemainder))
            combinedWat.append(FloatingPointSupport.remainderFunctionsWat());


        Map<String, ClassNode> linked = new LinkedHashMap<>();
        for (ClassNode cn : classes) {
            if (linked.putIfAbsent(cn.name, cn) != null)
                throw BytecodeCompatibility.failure(cn, null, null, "duplicate class declaration",
                        "provide exactly one definition of " + cn.name + " in the linked JAR");
        }
        // Bundled runtime code is kept only when reachable from linked-class exports, host hooks or Throwable inspection.
        Predicate<String> rootExport = export -> export.startsWith("__")
                || export.startsWith("java.lang.Throwable.") || roots.stream().anyMatch(export::startsWith);
        try (BytecodeAnalysisCache ignored = BytecodeAnalysisCache.open(linked)) {
            Set<String> instantiated = new HashSet<>();
            String wat = compileLinked(classes, linked, exceptionRuntime, combinedWat, rootExport, retainedFunctions, instantiated);
            if (!exceptionRuntime) return wat;
            wat = BytecodeDeadCode.prune(wat, rootExport, retainedFunctions);
            if (!memory.managedHeap()) return wat;
            // The collector reads only roots that survive pruning, so it is generated for the final module text.
            int end = wat.lastIndexOf(')');
            return wat.substring(0, end) + BytecodeHeap.runtimeWat(linked, new BytecodeDispatch(linked), instantiated, wat) + wat.substring(end);
        }
    }

    private static String compileLinked(List<ClassNode> classes, Map<String, ClassNode> linked, boolean exceptionRuntime,
                                        StringBuilder combinedWat, Predicate<String> rootExport, Set<String> retainedFunctions,
                                        Set<String> instantiated) {
        BytecodeDispatch runtimeDispatch = new BytecodeDispatch(linked);
        if (classes.stream().anyMatch(BytecodeArrays::usesRuntime)) combinedWat.append(BytecodeArrays.runtimeWat(runtimeDispatch));
        Set<BytecodeExceptions.Implicit> implicit = EnumSet.noneOf(BytecodeExceptions.Implicit.class);
        for (ClassNode type : classes) implicit.addAll(BytecodeExceptions.implicitRuntime(type, runtimeDispatch));
        if (!implicit.isEmpty() || classes.stream().anyMatch(BytecodeExceptions::usesRuntime)) combinedWat.append(BytecodeExceptions.TAG_WAT);
        for (var kind : implicit) combinedWat.append(runtimeDispatch.implicitExceptionRuntimeWat(kind));
        if(exceptionRuntime)combinedWat.append("""
            (func $__double_sqrt (param f64) (result f64) local.get 0 f64.sqrt)
            (func $__double_bits (param f64) (result i64) local.get 0 i64.reinterpret_f64)
            (func $__float_bits (param f32) (result i32) local.get 0 i32.reinterpret_f32)
            (func $__double_value (param i64) (result f64) local.get 0 f64.reinterpret_i64)
            (func $__float_value (param i32) (result f32) local.get 0 f32.reinterpret_i32)
            """);
        if (exceptionRuntime) combinedWat.append(BytecodeObjects.runtimeWat(linked,runtimeDispatch)).append(BytecodeClasses.runtimeWat(linked,runtimeDispatch));
        combinedWat.append(BytecodeStrings.runtimeWat(linked, runtimeDispatch));
        int classNamesEnd = combinedWat.length(), classNames = classNamesEnd
                - BytecodeStrings.classNameWat(linked, runtimeDispatch, name -> true).length();
        int traces = combinedWat.length();
        combinedWat.append(BytecodeTraces.runtimeWat(linked, runtimeDispatch));
        int tracesEnd = combinedWat.length();
        if (exceptionRuntime) {
            // Admit every class and method first, then lower only what the module can reach.
            List<ClassBytecodeCompiler> compilers = new ArrayList<>();
            for (ClassNode cn : classes) {
                ClassBytecodeCompiler compiler = new ClassBytecodeCompiler(cn);
                compiler.admit(linked);
                compilers.add(compiler);
            }
            String arrays = classes.stream().anyMatch(BytecodeArrays::usesRuntime)
                    ? runtimeDispatch.referenceArrayRuntimeWat() + runtimeDispatch.multiArrayRuntimeWat() : "";
            String runtime = combinedWat.substring("(module\n".length()) + arrays;
            var reachable = BytecodeReachability.lower(compilers, runtime, rootExport, retainedFunctions);
            instantiated.addAll(reachable.instantiated());
            // Analysis used the complete versions of these lookups; the emitted ones cover only what can occur.
            // Trace capture names sites whose method has a body; class names cover instantiated classes.
            combinedWat.replace(traces, tracesEnd,
                    BytecodeTraces.runtimeWat(linked, runtimeDispatch, reachable.lowered()::contains));
            combinedWat.replace(classNames, classNamesEnd,
                    BytecodeStrings.classNameWat(linked, runtimeDispatch, reachable.instantiated()::contains));
            combinedWat.append(reachable.classes()).append(arrays).append(")\n");
            return combinedWat.toString();
        }
        for (ClassNode cn : classes) {
            ClassBytecodeCompiler compiler = new ClassBytecodeCompiler(cn);
            String classWat = compiler.compileToWat(linked, false);
            // Strip outer (module ... )
            int start = classWat.indexOf("(import \"env\" \"__heap_alloc\" (func $__heap_alloc (param i32) (result i32)))");
            if (start != -1) {
                start += "(import \"env\" \"__heap_alloc\" (func $__heap_alloc (param i32) (result i32)))\n".length();
            } else {
                start = classWat.indexOf("(module\n") + "(module\n".length();
            }
            int end = classWat.lastIndexOf(")");
            if (start < end) {
                combinedWat.append(classWat.substring(start, end));
            }
        }

        if (classes.stream().anyMatch(BytecodeArrays::usesRuntime))
            combinedWat.append(new BytecodeDispatch(linked).referenceArrayRuntimeWat())
                    .append(new BytecodeDispatch(linked).multiArrayRuntimeWat());
        combinedWat.append(")\n");
        return combinedWat.toString();
    }

    public static String compileJar(File jarFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(jarFile)) {
            return compileJar(fis);
        }
    }

    public static void main(String[] args) {
        boolean exceptionRuntime=false,hostAllocator=false,managedHeap=false;Integer initialPages=null,maximumPages=null;List<List<String>> proxyShapes=new ArrayList<>();int option=0;
        while(option<args.length) {
            if(args[option].equals("--exception-runtime"))exceptionRuntime=true;
            else if(args[option].equals("--host-allocator"))hostAllocator=true;
            else if(args[option].equals("--managed-heap"))managedHeap=true; // the v257 opt-in, now the default
            else if(args[option].startsWith("--memory-pages=")) {
                String[] pages=args[option].substring("--memory-pages=".length()).split(":",-1);
                initialPages=Integer.valueOf(pages[0]);if(pages.length>1)maximumPages=Integer.valueOf(pages[1]);
            }
            else if(args[option].startsWith("--aot-proxy="))proxyShapes.add(List.of(args[option].substring("--aot-proxy=".length()).split(",",-1)));
            else break;
            option++;
        }
        args=Arrays.copyOfRange(args,option,args.length);
        if (args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
            System.out.println("Usage: java one.dastec.javaw.bytecode.BytecodeCompiler [--exception-runtime] [--host-allocator] [--memory-pages=initial[:maximum]] [--aot-proxy=Interface,Other] <jar-or-class-file> [output-wat-file]");
            return;
        }
        try {
            if(!exceptionRuntime && !proxyShapes.isEmpty())throw new IllegalArgumentException("AOT proxies require --exception-runtime");
            if(hostAllocator && managedHeap)throw new IllegalArgumentException("--host-allocator and --managed-heap conflict");
            if(!exceptionRuntime && (hostAllocator || managedHeap || initialPages!=null))throw new IllegalArgumentException("memory options require --exception-runtime");
            Memory memory=new Memory(initialPages==null?2:initialPages,maximumPages,exceptionRuntime && !hostAllocator);
            String path = args[0];
            if (path.startsWith("~" + File.separator) || path.equals("~")) {
                path = System.getProperty("user.home") + path.substring(1);
            }
            File file = new File(path);
            if (!file.exists()) {
                System.err.println("File not found: " + file.getAbsolutePath());
                System.exit(1);
            }
            String wat;
            if (file.getName().endsWith(".jar")) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    wat = compileJar(fis, exceptionRuntime,proxyShapes,Set.of(),memory);
                }
            } else if (file.getName().endsWith(".class")) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    wat = exceptionRuntime ? compileClasses(new ArrayList<>(List.of(BytecodeInput.read(fis.readAllBytes()))),true,proxyShapes,Set.of(),memory) : compileClass(fis);
                }
            } else {
                System.err.println("Unsupported file type: " + file.getName());
                System.exit(1);
                return;
            }
            if (args.length > 1) {
                String outPath = args[1];
                if (outPath.startsWith("~" + File.separator) || outPath.equals("~")) {
                    outPath = System.getProperty("user.home") + outPath.substring(1);
                }
                File outFile = new File(outPath);
                java.nio.file.Files.write(outFile.toPath(), wat.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                System.out.println("Wrote compiled WAT to " + outFile.getAbsolutePath());
            } else {
                System.out.println(wat);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
