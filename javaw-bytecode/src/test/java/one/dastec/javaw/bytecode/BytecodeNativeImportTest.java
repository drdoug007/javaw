package one.dastec.javaw.bytecode;

import one.dastec.javaw.Wat2Wasm;
import one.dastec.javaw.bytecode.BytecodeCompiler.NativeImport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;

import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

/** Static native methods bound to Wasm imports and satisfied directly by another module's exports. */
class BytecodeNativeImportTest implements Opcodes {
    @TempDir Path directory;

    private static final String NATIVES = """
            package lib;
            public class Natives {
                static native int add(int a, int b);
                public static native long fib(int n);
                static native double hypot(double a, double b);
                static native float half(float value);
                static native boolean flag(int raw);
                static native byte toByte(int raw);
                static native char toChar(int raw);
                static native short toShort(int raw);
                static native void tick();
                static native int ticks();
                static native double choose(boolean first, long a, float b, double c);
                static native void boom();
                static native int unused(int value);
                static native int echoBoolean(boolean value);
                static native int echoByte(byte value);
                static native int echoChar(char value);
                static native int echoShort(short value);
                static native int seed();
                static { Log.events = Log.events * 10 + 1; }
            }
            """;
    private static final String LOG = "package lib; public class Log { public static int events; }";
    private static final String CALLS = """
            package lib;
            public class Calls {
                public static int initialization() { int before = Log.events; int sum = Natives.add(1, 2); return before * 100 + Log.events * 10 + sum; }
                public static int sum() { return Natives.add(40, 2); }
                public static long fibonacci() { return Natives.fib(90); }
                public static double hypot() { return Natives.hypot(3, 4); }
                public static float half() { return Natives.half(5f); }
                public static String narrowing() {
                    return Natives.flag(0x1FF) + "," + Natives.flag(0x100) + "," + Natives.toByte(0x1F0) + ","
                            + (int) Natives.toChar(0x12345) + "," + Natives.toShort(0x18000);
                }
                public static int ticks() { Natives.tick(); Natives.tick(); return Natives.ticks(); }
                public static double choose() { return Natives.choose(true, 5L, 1.5f, 2.25) * 100 + Natives.choose(false, 5L, 1.5f, 2.25); }
                public static int lambda() { java.util.function.IntBinaryOperator op = Natives::add; return op.applyAsInt(20, 22); }
                public static int boom() { try { Natives.boom(); return 1; } catch (Throwable error) { return 2; } }
                public static int trapInInitializer() { return Seeded.value; }
            }
            """;
    /** A class initializer that calls a binding whose foreign function traps. */
    private static final String SEEDED = "package lib; public class Seeded { public static int value = 7; static { value = Natives.seed(); } }";
    /** Stands in for a Go or Rust library: plain exports, no imports and no JavaScript. */
    private static final String FOREIGN = """
            (module
              (global $ticks (mut i32) (i32.const 0))
              (func (export "add") (param i32 i32) (result i32) local.get 0 local.get 1 i32.add)
              (func (export "fib") (param $n i32) (result i64) (local $a i64) (local $b i64) (local $t i64)
                i64.const 1 local.set $b
                block $done loop $next
                  local.get $n i32.eqz br_if $done
                  local.get $a local.get $b i64.add local.set $t
                  local.get $b local.set $a local.get $t local.set $b
                  local.get $n i32.const 1 i32.sub local.set $n br $next
                end end
                local.get $a)
              (func (export "hypot") (param f64 f64) (result f64) local.get 0 local.get 0 f64.mul local.get 1 local.get 1 f64.mul f64.add f64.sqrt)
              (func (export "half") (param f32) (result f32) local.get 0 f32.const 0.5 f32.mul)
              (func (export "raw_flag") (param i32) (result i32) local.get 0)
              (func (export "raw_byte") (param i32) (result i32) local.get 0)
              (func (export "raw_char") (param i32) (result i32) local.get 0)
              (func (export "raw_short") (param i32) (result i32) local.get 0)
              (func (export "tick") global.get $ticks i32.const 1 i32.add global.set $ticks)
              (func (export "ticks") (result i32) global.get $ticks)
              (func (export "choose") (param i32 i64 f32 f64) (result f64)
                local.get 0 if (result f64) local.get 1 f64.convert_i64_s else local.get 2 f64.promote_f32 local.get 3 f64.add end)
              (func (export "boom") unreachable)
              (func (export "seed") (result i32) unreachable)
              (func (export "echo_boolean") (param i32) (result i32) local.get 0)
              (func (export "echo_byte") (param i32) (result i32) local.get 0)
              (func (export "echo_char") (param i32) (result i32) local.get 0)
              (func (export "echo_short") (param i32) (result i32) local.get 0)
              (func (export "unused") (param i32) (result i32) local.get 0))
            """;

    private static List<NativeImport> bindings() {
        List<NativeImport> result = new ArrayList<>();
        for (String name : List.of("add(II)I", "fib(I)J", "hypot(DD)D", "half(F)F", "tick()V", "ticks()I", "choose(ZJFD)D", "boom()V", "unused(I)I", "seed()I"))
            result.add(NativeImport.parse("lib.Natives." + name + "=foreign"));
        result.add(NativeImport.parse("lib.Natives.flag(I)Z=foreign:raw_flag"));
        result.add(NativeImport.parse("lib/Natives.toByte(I)B=foreign:raw_byte"));
        result.add(new NativeImport("lib/Natives", "toChar", "(I)C", "foreign", "raw_char"));
        result.add(new NativeImport("lib.Natives", "toShort", "(I)S", "foreign", "raw_short"));
        for (String type : List.of("Boolean:Z", "Byte:B", "Char:C", "Short:S")) {
            String[] parts = type.split(":");
            result.add(NativeImport.parse("lib.Natives.echo" + parts[0] + "(" + parts[1] + ")I=foreign:echo_" + parts[0].toLowerCase(Locale.ROOT)));
        }
        return result;
    }

    @Test void callsForeignModuleExportsDirectly() throws Exception {
        String wat = BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(List.of(arguments()), NATIVES, LOG, CALLS, SEEDED)),
                List.of(), Set.of(), BytecodeCompiler.Memory.managed(), bindings());
        assertTrue(wat.contains("(import \"foreign\" \"raw_flag\" (func " + BytecodeNatives.importFunction("lib/Natives", "flag", "(I)Z")
                + " (param i32) (result i32)))"), wat.substring(0, 2000));
        // Every binding is imported, even one no reachable code calls; its wrapper is pruned.
        assertTrue(wat.contains("(import \"foreign\" \"unused\""));
        assertFalse(wat.contains("(func " + TypeHelper.getMethodFunctionName("lib/Natives", "unused", "(I)I")));
        assertTrue(wat.contains("(export \"lib.Natives.fib\")"), "a public bound native is exported like any public method");
        // Expected values follow Java casts of the same bits, as a JNI implementation reads jboolean/jbyte/jchar/jshort.
        String narrowing = ((0x1FF & 0xFF) != 0) + "," + ((0x100 & 0xFF) != 0) + "," + (byte) 0x1F0 + "," + (int) (char) 0x12345 + "," + (short) 0x18000;
        long fibonacci = 0, next = 1;
        for (int i = 0; i < 90; i++) { long sum = fibonacci + next; fibonacci = next; next = sum; }
        execute(wat, """
            const imports=WebAssembly.Module.imports(compiled).map(i=>i.module+'.'+i.name+':'+i.kind).sort();
            assert.deepEqual(imports,['env.memory:memory',...%s.map(n=>'foreign.'+n+':function')].sort());
            let {e,text}=instance();
            // Invoking a static native initializes its class first (JVMS 5.5).
            assert.equal(e['lib.Calls.initialization'](),13);
            assert.equal(e['lib.Calls.sum'](),42);
            assert.equal(e['lib.Calls.fibonacci'](),%dn);
            assert.equal(e['lib.Natives.fib'](10),55n);
            assert.equal(e['lib.Calls.hypot'](),5);
            assert.equal(e['lib.Calls.half'](),2.5);
            assert.equal(text(e['lib.Calls.narrowing']()),'%s');
            assert.equal(e['lib.Calls.ticks'](),2);
            assert.equal(e['lib.Calls.choose'](),503.75);
            assert.equal(e['lib.Calls.lambda'](),42);
            // A foreign trap is not a Java exception: Java catch blocks do not see it.
            assert.throws(()=>e['lib.Calls.boom'](),WebAssembly.RuntimeError);
            // Verified bytecode may pass any int for a sub-int parameter; the foreign function sees the JNI-narrowed value.
            assert.equal(e['lib.Arguments.booleanValue'](),1);
            assert.equal(e['lib.Arguments.byteValue'](),%d);
            assert.equal(e['lib.Arguments.charValue'](),%d);
            assert.equal(e['lib.Arguments.shortValue'](),%d);
            // A trap cannot be caught in Wasm, so it skips class-initialization bookkeeping: the contract is to discard
            // the instance. Only the first call's trap is asserted.
            ({e}=instance());
            assert.throws(()=>e['lib.Calls.trapInInitializer'](),WebAssembly.RuntimeError);
            """.formatted("['add','fib','hypot','half','tick','ticks','choose','boom','unused','seed','raw_flag','raw_byte','raw_char','raw_short',"
                + "'echo_boolean','echo_byte','echo_char','echo_short']", fibonacci, narrowing, (byte) 300, (int) (char) 0x12345, (short) 0x18000));
    }

    @Test void diagnosesBindingsThatCannotCross() throws Exception {
        byte[] jar = jar("""
                package lib;
                public class Bad {
                    native int instance(int value);
                    static native int length(String text);
                    static native int[] values();
                    static native Object object(int value);
                    static native synchronized int locked(int value);
                    static native int plain(int value);
                    static native int other(int value);
                    public static int run() { return 1; }
                }
                """);
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("lib.Bad.instance(I)I=m:f", "lib.Bad.instance(I)I is an instance method; only static native methods can be bound");
        expected.put("lib.Bad.length(Ljava/lang/String;)I=m:f", "parameter 1 has type java.lang.String, which cannot cross a Wasm import");
        expected.put("lib.Bad.values()[I=m:f", "returns int[], which cannot cross a Wasm import");
        expected.put("lib.Bad.object(I)Ljava/lang/Object;=m:f", "returns java.lang.Object, which cannot cross a Wasm import");
        expected.put("lib.Bad.run()I=m:f", "lib.Bad.run()I is not native");
        expected.put("lib.Bad.missing()I=m:f", "names no declared method");
        expected.put("java.lang.Math.sqrt(D)D=m:f", "names java.lang.Math, which is not an input class");
        for (var entry : expected.entrySet()) {
            var failure = assertThrows(IllegalArgumentException.class, () -> compile(jar, NativeImport.parse(entry.getKey())), entry.getKey());
            assertTrue(failure.getMessage().contains(entry.getValue()), failure.getMessage());
        }
        // Bindings are checked before admission; admission still rejects a bound synchronized native and any unbound one.
        byte[] sync = jar("package lib; public class Sync { static native synchronized int locked(int value); }");
        var locked = assertThrows(IllegalArgumentException.class, () -> compile(sync, NativeImport.parse("lib.Sync.locked(I)I=m:f")));
        assertTrue(locked.getMessage().contains("lib.Sync.locked(I)I: unsupported native/synchronized method; provide a supported source"), locked.getMessage());
        byte[] loose = jar("package lib; public class Loose { static native int other(int value); native int instance(); }");
        var unbound = assertThrows(IllegalArgumentException.class, () -> compile(loose));
        assertTrue(unbound.getMessage().contains("lib.Loose.other(I)I: unsupported native/synchronized method; bind a static native method with primitive values to a Wasm import (--native-import)"),
                unbound.getMessage());
        var instance = assertThrows(IllegalArgumentException.class, () -> compile(loose, NativeImport.parse("lib.Loose.other(I)I=m:f")));
        assertTrue(instance.getMessage().contains("lib.Loose.instance()I: unsupported native/synchronized method; provide a supported source"), instance.getMessage());
        var twice = assertThrows(IllegalArgumentException.class, () -> compile(jar, NativeImport.parse("lib.Bad.plain(I)I=m:a"), NativeImport.parse("lib.Bad.plain(I)I=m:b")));
        assertTrue(twice.getMessage().contains("lib.Bad.plain(I)I is bound by more than one native import"), twice.getMessage());
        var shared = assertThrows(IllegalArgumentException.class, () -> compile(jar, NativeImport.parse("lib.Bad.plain(I)I=m:f"), NativeImport.parse("lib.Bad.other(I)I=m:f")));
        assertTrue(shared.getMessage().contains("name the same import"), shared.getMessage());
        for (String reserved : List.of("lib.Bad.plain(I)I=env:memory"))
            assertTrue(assertThrows(IllegalArgumentException.class, () -> compile(jar, NativeImport.parse(reserved))).getMessage()
                    .contains("names env.memory, which the module already imports"));
        var heap = assertThrows(IllegalArgumentException.class, () -> BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar),
                List.of(), Set.of(), BytecodeCompiler.Memory.hostAllocator(), List.of(NativeImport.parse("lib.Bad.plain(I)I=env:__heap_alloc"))));
        assertTrue(heap.getMessage().contains("names env.__heap_alloc"), heap.getMessage());
        var unbundled = assertThrows(IllegalArgumentException.class, () -> BytecodeCompiler.compileClasses(classes(jar), false, List.of(), Set.of(),
                BytecodeCompiler.Memory.hostAllocator(), List.of(NativeImport.parse("lib.Bad.plain(I)I=m:f"))));
        assertTrue(unbundled.getMessage().contains("native imports require the bundled exception runtime"), unbundled.getMessage());
    }

    @Test void importNamesDoNotDisablePruning() throws Exception {
        byte[] jar = jar("package lib; public class Pruned { static native int add(int a, int b); public static int run() { return add(1, 2); } }");
        String plain = compile(jar, NativeImport.parse("lib.Pruned.add(II)I=m:add"));
        String marked = compile(jar, NativeImport.parse("lib.Pruned.add(II)I=m(;:call_indirect(table"));
        assertTrue(marked.contains("(import \"m\\28\\3b\" \"call\\5findirect\\28table\""), marked.substring(0, 1000));
        assertEquals(plain.length() + "\\28\\3b".length() + "call\\5findirect\\28table".length() - "add".length(), marked.length(),
                "only the escaped import names differ; the rest of the module is pruned the same way");
    }

    @Test void nestedClassesUseTheirBinaryName() throws Exception {
        byte[] jar = jar("package lib; public class Outer { static class Inner { static native int add(int a, int b); } public static int run() { return Inner.add(1, 2); } }");
        var failure = assertThrows(IllegalArgumentException.class, () -> compile(jar, NativeImport.parse("lib.Outer.Inner.add(II)I=m:add")));
        assertTrue(failure.getMessage().contains("for a nested class use the binary name lib.Outer$Inner"), failure.getMessage());
        assertTrue(compile(jar, NativeImport.parse("lib.Outer$Inner.add(II)I=m:add")).contains("(import \"m\" \"add\""));
    }

    @Test void classFileAttributesCannotBindAMethod() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(V17, ACC_PUBLIC, "Forged", null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(ACC_STATIC | ACC_NATIVE, "add", "(II)I", null, null);
        method.visitAttribute(new Attribute("javaw.NativeImport") {
            @Override protected ByteVector write(ClassWriter classWriter, byte[] code, int codeLength, int maxStack, int maxLocals) {
                return new ByteVector().putShort(0);
            }
        });
        method.visitEnd();
        writer.visitEnd();
        var failure = assertThrows(IllegalArgumentException.class, () -> BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()));
        assertTrue(failure.getMessage().contains("Forged.add(II)I: unsupported native/synchronized method"), failure.getMessage());
    }

    @Test void parsesAndValidatesSpecifications() {
        NativeImport parsed = NativeImport.parse("pkg.Outer$Inner.run(IJ)V=go:run_it");
        assertEquals(new NativeImport("pkg/Outer$Inner", "run", "(IJ)V", "go", "run_it"), parsed);
        assertEquals("pkg.Outer$Inner.run(IJ)V=go:run_it", parsed.toString());
        assertEquals("run", NativeImport.parse("pkg.A.run()V=go").field());
        assertEquals("a:b", NativeImport.parse("A.run()V=go:a:b").field());
        for (String bad : List.of("A.run()V", "A.run=go", "run()V=go", "A.run(I=go", "A.<init>()V=go", "A.run()V=", "A.run()V=go:", "A..run()V=go",
                "A.m;()V=go", "A.m/x()V=go", "A.r<n()V=go"))
            assertThrows(IllegalArgumentException.class, () -> NativeImport.parse(bad), bad);
        assertThrows(IllegalArgumentException.class, () -> new NativeImport("A", "run", "()V", "go", "\ud800"));
        assertThrows(IllegalArgumentException.class, () -> new NativeImport("A", "run", "()V", "go:x", "f"));
        assertThrows(IllegalArgumentException.class, () -> new NativeImport("A=B", "run", "()V", "go", "f"));
        assertThrows(IllegalArgumentException.class, () -> new NativeImport("A", "m.x", "()V", "go", "f"));
        NativeImport odd = new NativeImport("p.A$1", "run", "(Ljava/lang/String;)V", "go", "f:g=h");
        assertEquals(odd, NativeImport.parse(odd.toString()), "toString is the command-line form");
    }

    @Test void commandLineBindsImports() throws Exception {
        Path jarFile = directory.resolve("lib.jar"), output = directory.resolve("out.wat");
        Files.write(jarFile, jar(List.of(arguments()), NATIVES, LOG, CALLS, SEEDED));
        List<String> arguments = new ArrayList<>(List.of("--exception-runtime"));
        for (NativeImport binding : bindings()) arguments.add("--native-import=" + binding);
        arguments.addAll(List.of(jarFile.toString(), output.toString()));
        BytecodeCompiler.main(arguments.toArray(String[]::new));
        assertEquals(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(Files.readAllBytes(jarFile)),
                bindings().toArray(NativeImport[]::new)), Files.readString(output));
    }

    /** javac never passes an out-of-range int for a sub-int parameter, but verified bytecode may. */
    private static byte[] arguments() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(V17, ACC_PUBLIC, "lib/Arguments", null, "java/lang/Object", null);
        String[][] cases = {{"booleanValue", "echoBoolean", "Z", "2"}, {"byteValue", "echoByte", "B", "300"},
                {"charValue", "echoChar", "C", String.valueOf(0x12345)}, {"shortValue", "echoShort", "S", String.valueOf(0x18000)}};
        for (String[] c : cases) {
            MethodVisitor method = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, c[0], "()I", null, null);
            method.visitCode();
            method.visitLdcInsn(Integer.valueOf(c[3]));
            method.visitMethodInsn(INVOKESTATIC, "lib/Natives", c[1], "(" + c[2] + ")I", false);
            method.visitInsn(IRETURN);
            method.visitMaxs(0, 0);
            method.visitEnd();
        }
        writer.visitEnd();
        return writer.toByteArray();
    }

    private String compile(byte[] jar, NativeImport... bindings) throws IOException {
        return BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar), bindings);
    }

    private List<org.objectweb.asm.tree.ClassNode> classes(byte[] jar) throws IOException {
        List<org.objectweb.asm.tree.ClassNode> result = new ArrayList<>();
        try (JarInputStream input = new JarInputStream(new ByteArrayInputStream(jar))) {
            for (JarEntry entry; (entry = input.getNextJarEntry()) != null; )
                if (entry.getName().endsWith(".class")) result.add(BytecodeInput.read(input.readAllBytes()));
        }
        return result;
    }

    private byte[] jar(String... sources) throws Exception { return jar(List.of(), sources); }

    /** javac output for {@code sources}, plus {@code extra} class files named by their internal names. */
    private byte[] jar(List<byte[]> extra, String... sources) throws Exception {
        Path build = Files.createTempDirectory(directory, "jar");
        Path source = Files.createDirectories(build.resolve("src")), classes = Files.createDirectories(build.resolve("classes"));
        List<String> arguments = new ArrayList<>(List.of("-g", "--release", "17", "-d", classes.toString()));
        for (int i = 0; i < sources.length; i++) {
            String name = sources[i].replaceAll("(?s).*?public class (\\w+).*", "$1");
            Path file = source.resolve(name + ".java");
            Files.writeString(file, sources[i]);
            arguments.add(file.toString());
        }
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, arguments.toArray(String[]::new)));
        List<Path> files;
        try (var paths = Files.walk(classes)) { files = paths.filter(path -> path.toString().endsWith(".class")).sorted().toList(); }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(bytes)) {
            for (Path path : files) {
                jar.putNextEntry(new JarEntry(classes.relativize(path).toString().replace(File.separatorChar, '/')));
                jar.write(Files.readAllBytes(path));
                jar.closeEntry();
            }
            for (byte[] type : extra) {
                jar.putNextEntry(new JarEntry(new ClassReader(type).getClassName() + ".class"));
                jar.write(type);
                jar.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private void execute(String wat, String assertions) throws Exception {
        Path javaWat = directory.resolve("java.wat"), javaWasm = directory.resolve("java.wasm");
        Path foreignWat = directory.resolve("foreign.wat"), foreignWasm = directory.resolve("foreign.wasm"), script = directory.resolve("run.cjs");
        Files.writeString(javaWat, wat);
        Files.writeString(foreignWat, FOREIGN);
        WasmTestSupport.assemble(javaWat, javaWasm, Wat2Wasm.EXCEPTIONS);
        WasmTestSupport.assemble(foreignWat, foreignWasm);
        Files.writeString(script, """
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            const foreignModule=new WebAssembly.Module(fs.readFileSync(process.argv[3]));
            function instance(){
                // The foreign module's exports object is the import namespace: no JavaScript sits on the call path.
                const foreign=new WebAssembly.Instance(foreignModule,{}).exports;
                const memory=new WebAssembly.Memory({initial:2});
                const e=new WebAssembly.Instance(compiled,{env:{memory},foreign}).exports;
                if(e.__gc_scribble)e.__gc_scribble.value=1;
                function text(pointer){if(!pointer)return null;const view=new DataView(memory.buffer);
                    const chars=view.getInt32(pointer+4,true),length=view.getInt32(chars+4,true);
                    let result='';for(let i=0;i<length;i++)result+=String.fromCharCode(view.getUint16(chars+8+2*i,true));return result;}
                return {e,text};
            }
            """ + assertions);
        WasmTestSupport.node(script, javaWasm, foreignWasm);
    }
}
