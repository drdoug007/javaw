package dastec.one.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import javax.tools.ToolProvider;
import static org.junit.jupiter.api.Assertions.*;

class BytecodeCompatibilityTest {
    @TempDir Path directory;

    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Fixture.java"); Files.writeString(file,source);
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-g","-d",directory.toString(),file.toString()));
        return Files.readAllBytes(directory.resolve("Fixture.class"));
    }

    private void execute(String wat,String assertions) throws Exception {
        Path input=directory.resolve("module.wat"), output=directory.resolve("module.wasm");
        Files.writeString(input,wat);
        run("wat2wasm",input.toString(),"-o",output.toString());
        Path script=directory.resolve("run.cjs");
        Files.writeString(script,"""
            const assert=require('node:assert/strict');
            const fs=require('node:fs');
            const memory=new WebAssembly.Memory({initial:2,maximum:20});
            let heap=1024;
            const instance=new WebAssembly.Instance(new WebAssembly.Module(fs.readFileSync(process.argv[2])),
                {env:{memory,__heap_alloc:(size)=>{
                    const ptr=heap; heap+=size;
                    new Uint8Array(memory.buffer,ptr,size).fill(0x7f);
                    return ptr;
                }}});
            const e=instance.exports;
            """+assertions);
        run("node",script.toString(),output.toString());
    }

    private void run(String... command) throws Exception {
        Path log=directory.resolve("command.log");
        Process process=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(20,TimeUnit.SECONDS)) { process.destroyForcibly(); fail("Timed out: "+String.join(" ",command)); }
        assertEquals(0,process.exitValue(),Files.readString(log));
    }

    @Test void executesReferenceArraysCovarianceAndStoreChecksInEitherJarOrder() throws Exception {
        fixture("""
            public class Fixture {
                static Base[] saved;
                public static int run(int n) {
                    Child[] children = new Child[n]; saved = children;
                    int sum = 0;
                    for (int i=0;i<n;i++) { if(saved[i] == null) sum++; saved[i] = new Child(i); }
                    for (Base item : saved) sum += item.value();
                    Object ref = saved;
                    if(ref instanceof Base[]) sum += 10;
                    if(ref instanceof Child[]) sum += 20;
                    if(ref instanceof Other[]) sum += 1000;
                    if(((Base[])ref) != saved) sum += 1000;
                    Object[] objects = new Object[3];
                    objects[0] = new int[]{7}; objects[1] = children; objects[2] = null;
                    if(objects[0] instanceof int[]) sum += 40;
                    if(objects[1] instanceof Object[]) sum += 80;
                    return sum + objects.length;
                }
                public static void badStore() { Base[] a = new Child[1]; a[0] = new Other(); }
                public static void badCast() { Object a = new Base[1]; Child[] b = (Child[])a; }
                public static void nullLoad() { Base[] a = null; Base b = a[0]; }
                public static void nullStore() { Base[] a = null; a[0] = null; }
                public static void bounds(int i) { Base[] a = new Base[1]; a[i] = null; }
                public static int size(int n) { return new Base[n].length; }
            }
            class Base { int value() { return -1; } }
            class Child extends Base { int n; Child(int n) {this.n=n;} int value(){return n;} }
            class Other extends Base {}
            """);
        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{directory.toUri().toURL()})) {
            Class<?> jvm = loader.loadClass("Fixture");
            for (String[] order : new String[][]{{"Fixture","Base","Child","Other"},{"Other","Child","Base","Fixture"}}) {
                StringBuilder checks = new StringBuilder();
                for (int n : new int[]{0,1,7}) checks.append("assert.equal(e['Fixture.run'](").append(n).append("),")
                        .append(jvm.getMethod("run",int.class).invoke(null,n)).append(");");
                for (String name : new String[]{"badStore","badCast","nullLoad","nullStore"}) {
                    assertThrows(java.lang.reflect.InvocationTargetException.class, () -> jvm.getMethod(name).invoke(null));
                    checks.append("assert.throws(()=>e['Fixture.").append(name).append("'](),WebAssembly.RuntimeError);");
                }
                for(int index : new int[]{-1,1,Integer.MAX_VALUE}) {
                    assertThrows(java.lang.reflect.InvocationTargetException.class, () -> jvm.getMethod("bounds",int.class).invoke(null,index));
                    checks.append("assert.throws(()=>e['Fixture.bounds'](").append(index).append("),WebAssembly.RuntimeError);");
                }
                checks.append("assert.throws(()=>e['Fixture.size'](-1),WebAssembly.RuntimeError);");
                String wat = linkJar(order);
                assertEquals(1,wat.split(java.util.regex.Pattern.quote("(func $__reference_array_store"),-1).length-1);
                execute(wat,checks.toString());
            }
        }
    }

    @Test void executesInterfaceArraysAndRejectsUnrelatedRuntimeStores() throws Exception {
        fixture("""
            public class Fixture {
                public static int run() {
                    Item[] a = new Item[3]; Object[] alias = a;
                    alias[0] = new Impl(); alias[1] = null;
                    Item[] b = new Impl[1]; b[0] = new Impl();
                    Object object = b;
                    return a[0].value() + b[0].value() + (a[1] == null ? 1 : 0)
                        + (object instanceof Item[] ? 10 : 0) + (object instanceof Impl[] ? 20 : 0);
                }
                public static void fail() { Object[] a = new Item[1]; a[0] = new Fixture(); }
            }
            interface Item { int value(); }
            class Impl implements Item { public int value() { return 7; } }
            """);
        try(var loader = new java.net.URLClassLoader(new java.net.URL[]{directory.toUri().toURL()})) {
            var jvm = loader.loadClass("Fixture");
            var failure = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> jvm.getMethod("fail").invoke(null));
            assertInstanceOf(ArrayStoreException.class, failure.getCause());
            execute(linkJar("Item","Fixture","Impl"),"assert.equal(e['Fixture.run'](),"+jvm.getMethod("run").invoke(null)
                    +");assert.throws(()=>e['Fixture.fail'](),WebAssembly.RuntimeError);");
        }
    }

    @Test void executesMultidimensionalPrimitiveAndReferenceArraysInEitherJarOrder() throws Exception {
        fixture("""
            public class Fixture {
                public static int run(int n) {
                    int[][][] a = new int[n][2][3];
                    long[][] longs = new long[n][2]; double[][] doubles = new double[n][2];
                    Item[][] items = new Child[n][2];
                    int total = a.length + longs.length + doubles.length;
                    for(int i=0;i<n;i++) {
                        a[i][1][2]=i+7; longs[i][1]=9007199254740993L+i; doubles[i][1]=i+0.5;
                        items[i][0]=new Child(i);
                        total += a[i][1][2]+items[i][0].value();
                        if(longs[i][1]==9007199254740993L+i) total++;
                        if(doubles[i][1]==i+0.5) total++;
                        if(items[i][1]==null && a[i][0][0]==0) total++;
                        if(i>0 && a[i]!=a[i-1]) total++;
                    }
                    Object object = a;
                    if(object instanceof Object[]) total+=10;
                    if(object instanceof Object[][]) total+=20;
                    if(object instanceof int[][][]) total+=40;
                    if(object instanceof long[][][]) total+=1000;
                    if(((Object[][])object).length!=n) total+=1000;
                    Object refs=items;
                    if(refs instanceof Item[][]) total+=80;
                    if(refs instanceof Child[][]) total+=160;
                    return total;
                }
                public static int partial() {
                    int[][][] a = new int[2][3][];
                    a[0][1]=new int[]{5};
                    int[][] b = new int[2][]; b[0]=new int[]{7};
                    return a[0][1][0]+b[0][0]+(a[1][0]==null?10:0)+(b[1]==null?20:0);
                }
                public static int ragged() {
                    Object[][] a=new Object[2][]; a[0]=new Item[3]; a[1]=new Object[1];
                    a[0][0]=new Child(9);
                    return ((Item)a[0][0]).value()+a[0].length+a[1].length;
                }
                public static int negative(int a,int b,int c) { return new int[a][b][c].length; }
                public static void badStore() { Object[] a=new int[1][]; a[0]=new long[1]; }
                public static void badNestedStore() { Object[][] a=new Child[1][1]; a[0]=new Item[1]; }
                public static void badCast() { Object a=new int[1][1]; long[][] b=(long[][])a; }
            }
            interface Item { int value(); }
            class Child implements Item { int n; Child(int n){this.n=n;} public int value(){return n;} }
            """);
        try(var loader=new java.net.URLClassLoader(new java.net.URL[]{directory.toUri().toURL()})) {
            Class<?> jvm=loader.loadClass("Fixture");
            for(String[] order:new String[][]{{"Fixture","Item","Child"},{"Child","Item","Fixture"}}) {
                StringBuilder checks=new StringBuilder();
                for(int n:new int[]{0,1,4}) checks.append("assert.equal(e['Fixture.run'](").append(n).append("),")
                        .append(jvm.getMethod("run",int.class).invoke(null,n)).append(");");
                for(String name:new String[]{"partial","ragged"}) checks.append("assert.equal(e['Fixture.")
                        .append(name).append("'](),").append(jvm.getMethod(name).invoke(null)).append(");");
                for(String name:new String[]{"badStore","badNestedStore","badCast"}) {
                    assertThrows(java.lang.reflect.InvocationTargetException.class,()->jvm.getMethod(name).invoke(null));
                    checks.append("assert.throws(()=>e['Fixture.").append(name).append("'](),WebAssembly.RuntimeError);");
                }
                for(int[] sizes:new int[][]{{-1,2,3},{0,-1,3},{0,0,-1},{1,2,-1}}) {
                    var error=assertThrows(java.lang.reflect.InvocationTargetException.class,()->jvm.getMethod("negative",int.class,int.class,int.class)
                            .invoke(null,sizes[0],sizes[1],sizes[2]));
                    assertInstanceOf(NegativeArraySizeException.class,error.getCause());
                    checks.append("assert.throws(()=>e['Fixture.negative'](").append(sizes[0]).append(',').append(sizes[1]).append(',')
                            .append(sizes[2]).append("),WebAssembly.RuntimeError);");
                }
                execute(linkJar(order),checks.toString());
            }
        }
    }

    @Test void executesEveryPrimitiveMultidimensionalLeafAndPrechecksAllSizes() throws Exception {
        byte[] bytes=fixture("""
            public class Fixture {
                public static int run() {
                    boolean[][] z=new boolean[2][2]; byte[][] b=new byte[2][2]; char[][] c=new char[2][2]; short[][] s=new short[2][2];
                    int[][] i=new int[2][2]; long[][] j=new long[2][2]; float[][] f=new float[2][2]; double[][] d=new double[2][2];
                    z[1][1]=true; b[1][1]=(byte)128; c[1][1]=(char)-1; s[1][1]=(short)32768;
                    i[1][1]=19; j[1][1]=9007199254740993L; f[1][1]=1.5f; d[1][1]=-0.5;
                    return (z[1][1]?1:0)+b[1][1]+c[1][1]+s[1][1]+i[1][1]+(j[1][1]==9007199254740993L?7:0)
                        +(f[1][1]==1.5f?11:0)+(d[1][1]==-0.5?13:0);
                }
                public static int size(int x,int y) { return new int[x][y].length; }
            }
            """);
        try(var loader=new java.net.URLClassLoader(new java.net.URL[]{directory.toUri().toURL()})) {
            Class<?> jvm=loader.loadClass("Fixture");
            execute(BytecodeCompiler.compileClass(bytes),"assert.equal(e['Fixture.run'](),"+jvm.getMethod("run").invoke(null)+");"
                +"const compiledModule = new WebAssembly.Module(fs.readFileSync(process.argv[2]));let allocations=0;"
                +"const checked=new WebAssembly.Instance(compiledModule,{env:{memory,__heap_alloc:()=>{allocations++;throw Error('allocated');}}}).exports;"
                +"assert.throws(()=>checked['Fixture.size'](0,-1),WebAssembly.RuntimeError);assert.equal(allocations,0);"
                +"assert.throws(()=>checked['Fixture.size'](2,-1),WebAssembly.RuntimeError);assert.equal(allocations,0);");
        }
    }

    @Test void executesEveryPrimitiveArrayCloneAndOverlappingCopyWithJvmComparison() throws Exception {
        StringBuilder source=new StringBuilder("public class Fixture {");
        String[] types={"boolean","byte","char","short","int","long","float","double"};
        String[] values={"true","(byte)128","(char)65535","(short)-32768","123456789","9007199254740993L","Float.NaN","Double.NEGATIVE_INFINITY"};
        for(int kind=0;kind<types.length;kind++) {
            String type=types[kind], value=values[kind];
            source.append("public static int kind").append(kind).append("(){")
                .append(type).append("[] a=new ").append(type).append("[5];a[0]=").append(value).append(';')
                .append(type).append("[] b=a.clone();")
                .append("System.arraycopy(a,0,b,1,4);System.arraycopy(b,0,b,1,4);System.arraycopy(b,1,b,0,4);")
                .append("int n=(a!=b?1:0)+b.length;");
            if(type.equals("float")) source.append("if(b[0]!=b[0]&&b[1]!=b[1])n+=10;");
            else source.append("if(b[0]==").append(value).append("&&b[1]==").append(value).append(")n+=10;");
            source.append("return n;}");
        }
        source.append("public static int empty(){int[] a=new int[0];int[] b=a.clone();System.arraycopy(a,0,b,0,0);return b.length+(a!=b?1:0);}}");
        byte[] bytes=fixture(source.toString());
        try(var loader=new java.net.URLClassLoader(new java.net.URL[]{directory.toUri().toURL()})) {
            Class<?> jvm=loader.loadClass("Fixture");StringBuilder checks=new StringBuilder();
            for(int kind=0;kind<types.length;kind++) checks.append("assert.equal(e['Fixture.kind").append(kind)
                    .append("'](),").append(jvm.getMethod("kind"+kind).invoke(null)).append(");");
            checks.append("assert.equal(e['Fixture.empty'](),").append(jvm.getMethod("empty").invoke(null)).append(");");
            execute(BytecodeCompiler.compileClass(bytes),checks.toString());
        }
    }

    @Test void executesShallowReferenceClonesAndCheckedCopiesInEitherJarOrder() throws Exception {
        fixture("""
            public class Fixture {
                static Base[] saved;
                public static int run() {
                    Child child=new Child(7); Child[] a={child,null,new Child(3)}; Child[] b=a.clone();
                    int[][] nested={{1,2},{3}}; int[][] copied=nested.clone(); copied[0][0]=9;
                    Object[] objects=new Object[3]; System.arraycopy(a,0,objects,0,3);
                    Base[] base=new Base[3]; System.arraycopy(objects,0,base,0,3);
                    Child[] children=new Child[3]; System.arraycopy(base,0,children,0,3);
                    System.arraycopy(children,0,children,1,2); System.arraycopy(children,1,children,0,2);
                    return (a!=b?1:0)+(a[0]==b[0]?2:0)+(nested!=copied?4:0)
                        +(nested[0]==copied[0]?8:0)+nested[0][0]+children[0].n+(children[1]==null?16:0)
                        +(objects instanceof Object[]?32:0)+(b instanceof Child[]?64:0);
                }
                public static void failCopy() {
                    Object[] source={new Child(1),null,new Other(),new Child(4)};
                    saved=new Child[]{new Child(9),new Child(9),new Child(9),new Child(9)};
                    System.arraycopy(source,0,saved,0,4);
                }
                public static int snapshot() {
                    return ((Child)saved[0]).n+(saved[1]==null?100:0)+((Child)saved[2]).n*1000+((Child)saved[3]).n*10000;
                }
            }
            class Base {}
            class Child extends Base {int n;Child(int n){this.n=n;}}
            class Other extends Base {}
            """);
        try(var loader=new java.net.URLClassLoader(new java.net.URL[]{directory.toUri().toURL()})) {
            Class<?> jvm=loader.loadClass("Fixture");Object expected=jvm.getMethod("run").invoke(null);
            var error=assertThrows(java.lang.reflect.InvocationTargetException.class,()->jvm.getMethod("failCopy").invoke(null));
            assertInstanceOf(ArrayStoreException.class,error.getCause());Object snapshot=jvm.getMethod("snapshot").invoke(null);
            for(String[] order:new String[][]{{"Fixture","Base","Child","Other"},{"Other","Child","Base","Fixture"}}) {
                String wat=linkJar(order);
                assertEquals(1,wat.split(java.util.regex.Pattern.quote("(func $__arraycopy"),-1).length-1);
                execute(wat,"assert.equal(e['Fixture.run'](),"+expected+");assert.throws(()=>e['Fixture.failCopy'](),WebAssembly.RuntimeError);"
                        +"assert.equal(e['Fixture.snapshot'](),"+snapshot+");");
            }
        }
    }

    @Test void trapsArrayTransferFailuresAndMatchesJvmZeroLengthRules() throws Exception {
        byte[] bytes=fixture("""
            public class Fixture {
                public static void copy(int kind,int from,int to,int count) {
                    Object a=kind==0?null:kind==1?new Fixture():kind==2?new byte[2]:kind==8?new Object[2]:new int[]{1,2};
                    Object b=kind==3?null:kind==4?new Fixture():kind==5?new long[2]:kind==7?new Object[2]:new int[2];
                    System.arraycopy(a,from,b,to,count);
                }
                public static void nullClone(){ int[] a=null; int[] b=a.clone(); }
                public static void refEmpty(){ System.arraycopy(new Fixture[0],0,new int[0][],0,0); }
            }
            """);
        try(var loader=new java.net.URLClassLoader(new java.net.URL[]{directory.toUri().toURL()})) {
            Class<?> jvm=loader.loadClass("Fixture");StringBuilder checks=new StringBuilder();
            for(int kind:new int[]{0,1,2,3,4,5,7,8}) for(int count:new int[]{0,1}) {
                int scenario=kind;
                var error=assertThrows(java.lang.reflect.InvocationTargetException.class,()->jvm.getMethod("copy",int.class,int.class,int.class,int.class)
                        .invoke(null,scenario,0,0,count));
                assertTrue(error.getCause() instanceof NullPointerException || error.getCause() instanceof ArrayStoreException);
                checks.append("assert.throws(()=>e['Fixture.copy'](").append(kind).append(",0,0,").append(count).append("),WebAssembly.RuntimeError);");
            }
            for(int[] args:new int[][]{{-1,0,0},{0,-1,0},{0,0,-1},{3,0,0},{0,3,0},{1,0,2},{0,1,2},{Integer.MAX_VALUE,0,1},{1,1,Integer.MAX_VALUE}}) {
                var error=assertThrows(java.lang.reflect.InvocationTargetException.class,()->jvm.getMethod("copy",int.class,int.class,int.class,int.class)
                        .invoke(null,6,args[0],args[1],args[2]));
                assertInstanceOf(IndexOutOfBoundsException.class,error.getCause());
                checks.append("assert.throws(()=>e['Fixture.copy'](6,").append(args[0]).append(',').append(args[1]).append(',')
                        .append(args[2]).append("),WebAssembly.RuntimeError);");
            }
            assertThrows(java.lang.reflect.InvocationTargetException.class,()->jvm.getMethod("nullClone").invoke(null));
            checks.append("assert.throws(()=>e['Fixture.nullClone'](),WebAssembly.RuntimeError);");
            jvm.getMethod("copy",int.class,int.class,int.class,int.class).invoke(null,6,2,2,0);
            jvm.getMethod("refEmpty").invoke(null);
            checks.append("e['Fixture.copy'](6,2,2,0);e['Fixture.refEmpty']();");
            execute(BytecodeCompiler.compileClass(bytes),checks.toString());
        }
    }

    @Test void preservesExplicitlyLinkedSystemDefinitionsInsteadOfReplacingTheirMethods() throws Exception {
        fixture("public class Fixture {public static void run(){System.arraycopy(null,0,null,0,0);}}");
        ClassNode system=new ClassNode();system.version=Opcodes.V17;system.access=Opcodes.ACC_PUBLIC;
        system.name="java/lang/System";system.superName="java/lang/Object";
        MethodNode copy=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"arraycopy","(Ljava/lang/Object;ILjava/lang/Object;II)V",null,null);
        copy.maxLocals=5;copy.instructions.add(new InsnNode(Opcodes.RETURN));system.methods.add(copy);
        execute(linkNodes(java.util.Map.of("Fixture",readFixture("Fixture"),system.name,system)),"e['Fixture.run']();");
    }

    @Test void executesPrimitiveNarrowArraysDefaultsLoopsAndAliasingWithJvmComparison() throws Exception {
        byte[] bytes = fixture("""
            public class Fixture {
                static int[] saved;
                private static int sum(int[] values) {
                    int sum = 0; for (int v : values) sum += v; return sum;
                }
                public static int ints(int n) {
                    int[] values = new int[n]; saved = values;
                    int defaults = sum(values);
                    for (int i = 0; i < n; i++) values[i] = i * 1000003 - 7;
                    return defaults + sum(saved);
                }
                public static int narrow(int n) {
                    boolean[] z = new boolean[n]; byte[] b = new byte[n];
                    short[] s = new short[n]; char[] c = new char[n];
                    if (n == 0) return z.length + b.length + s.length + c.length;
                    int defaults = (z[n-1] ? 1 : 0) + b[n-1] + s[n-1] + c[n-1];
                    z[0] = true; b[0] = (byte) 128; s[0] = (short) 32768; c[0] = (char) -1;
                    return defaults + (z[0] ? 1 : 0) + b[0] + s[0] + c[0];
                }
            }
            """);
        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{directory.toUri().toURL()})) {
            Class<?> jvm = loader.loadClass("Fixture"); StringBuilder assertions = new StringBuilder();
            for (String method : new String[]{"ints", "narrow"}) for (int n : new int[]{0, 1, 7, 40})
                assertions.append("assert.equal(e['Fixture.").append(method).append("'](").append(n).append("),")
                        .append(jvm.getMethod(method, int.class).invoke(null, n)).append(");");
            execute(BytecodeCompiler.compileClass(bytes), assertions.toString());
        }
    }

    @Test void executesWideAndFloatingPrimitiveArraysWithJvmComparison() throws Exception {
        byte[] bytes = fixture("""
            public class Fixture {
                private static long select(long[] values, int i) { return values[i]; }
                public static long longs(int i) {
                    long[] values = new long[4];
                    values[0] = Long.MIN_VALUE; values[1] = 9007199254740993L; values[2] = Long.MAX_VALUE;
                    return select(values, i);
                }
                public static float floats(int i) {
                    float[] values = {0.0f, -0.0f, 1.25f, Float.MIN_VALUE, Float.MAX_VALUE, Float.POSITIVE_INFINITY, Float.NaN};
                    return values[i];
                }
                public static double doubles(int i) {
                    double[] values = {0.0d, -0.0d, 1.25d, Double.MIN_VALUE, Double.MAX_VALUE, Double.NEGATIVE_INFINITY, Double.NaN};
                    return values[i];
                }
            }
            """);
        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{directory.toUri().toURL()})) {
            Class<?> jvm = loader.loadClass("Fixture"); StringBuilder assertions = new StringBuilder();
            for (String method : new String[]{"longs", "floats", "doubles"}) {
                for (int i = 0; i < (method.equals("longs") ? 4 : 7); i++) {
                    Object value = jvm.getMethod(method, int.class).invoke(null, i);
                    String expected = value instanceof Long ? value + "n" : Double.toString(((Number) value).doubleValue());
                    assertions.append("assert.equal(e['Fixture.").append(method).append("'](").append(i).append("),")
                            .append(expected).append(");");
                }
            }
            execute(BytecodeCompiler.compileClass(bytes), assertions.toString());
        }
    }

    @Test void executesPrimitiveArrayTypeTestsCastsAndObjectJoinsWithJvmComparison() throws Exception {
        byte[] bytes = fixture("""
            public class Fixture {
                public static Object make(int kind) {
                    return switch (kind) {
                        case 0 -> null; case 1 -> new boolean[0]; case 2 -> new byte[0];
                        case 3 -> new char[0]; case 4 -> new short[0]; case 5 -> new int[3];
                        case 6 -> new long[0]; case 7 -> new float[0]; case 8 -> new double[0];
                        default -> new Fixture();
                    };
                }
                public static int classify(Object value) {
                    return (value instanceof Object ? 1 : 0) + (value instanceof boolean[] ? 2 : 0)
                        + (value instanceof byte[] ? 4 : 0) + (value instanceof char[] ? 8 : 0)
                        + (value instanceof short[] ? 16 : 0) + (value instanceof int[] ? 32 : 0)
                        + (value instanceof long[] ? 64 : 0) + (value instanceof float[] ? 128 : 0)
                        + (value instanceof double[] ? 256 : 0);
                }
                public static int[] cast(Object value) { return (int[]) value; }
                public static int length(int[] value) { return value.length; }
            }
            """);
        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{directory.toUri().toURL()})) {
            Class<?> jvm = loader.loadClass("Fixture"); StringBuilder assertions = new StringBuilder();
            for (int i = 0; i <= 9; i++) {
                Object value = jvm.getMethod("make", int.class).invoke(null, i);
                assertions.append("assert.equal(e['Fixture.classify'](e['Fixture.make'](").append(i).append(")),")
                        .append(jvm.getMethod("classify", Object.class).invoke(null, value)).append(");");
            }
            assertions.append("const array=e['Fixture.make'](5); assert.equal(e['Fixture.cast'](array),array);")
                    .append("assert.equal(e['Fixture.length'](array),3); assert.equal(e['Fixture.cast'](0),0);")
                    .append("assert.throws(()=>e['Fixture.cast'](e['Fixture.make'](2)),WebAssembly.RuntimeError);")
                    .append("assert.throws(()=>e['Fixture.cast'](e['Fixture.make'](9)),WebAssembly.RuntimeError);");
            execute(BytecodeCompiler.compileClass(bytes), assertions.toString());
        }
    }

    @Test void trapsPrimitiveArrayNullBoundsNegativeAndOverflowFailures() throws Exception {
        byte[] bytes = fixture("""
            public class Fixture {
                public static int[] make(int n) { return new int[n]; }
                public static long[] wide(int n) { return new long[n]; }
                public static int length(int[] value) { return value.length; }
                public static int get(int[] value, int index) { return value[index]; }
                public static void put(int[] value, int index, int v) { value[index] = v; }
            }
            """);
        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{directory.toUri().toURL()})) {
            Class<?> jvm = loader.loadClass("Fixture");
            assertInstanceOf(NegativeArraySizeException.class, assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> jvm.getMethod("make", int.class).invoke(null, -1)).getCause());
            assertInstanceOf(NullPointerException.class, assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> jvm.getMethod("length", int[].class).invoke(null, (Object) null)).getCause());
            for (int index : new int[]{-1, 3, Integer.MAX_VALUE})
                assertInstanceOf(ArrayIndexOutOfBoundsException.class, assertThrows(java.lang.reflect.InvocationTargetException.class,
                        () -> jvm.getMethod("get", int[].class, int.class).invoke(null, new int[3], index)).getCause());
        }
        execute(BytecodeCompiler.compileClass(bytes), """
            const array=e['Fixture.make'](3), empty=e['Fixture.make'](0);
            assert.equal(e['Fixture.length'](empty),0);
            assert.equal(e['Fixture.get'](array,0),0);
            e['Fixture.put'](array,2,-123); assert.equal(e['Fixture.get'](array,2),-123);
            for(const index of [-1,3,2147483647]) {
                assert.throws(()=>e['Fixture.get'](array,index),WebAssembly.RuntimeError);
                assert.throws(()=>e['Fixture.put'](array,index,7),WebAssembly.RuntimeError);
            }
            assert.throws(()=>e['Fixture.get'](empty,0),WebAssembly.RuntimeError);
            assert.throws(()=>e['Fixture.length'](0),WebAssembly.RuntimeError);
            assert.throws(()=>e['Fixture.get'](0,0),WebAssembly.RuntimeError);
            assert.throws(()=>e['Fixture.put'](0,0,3),WebAssembly.RuntimeError);
            assert.throws(()=>e['Fixture.make'](-1),WebAssembly.RuntimeError);
            assert.throws(()=>e['Fixture.make'](2147483647),WebAssembly.RuntimeError);
            assert.throws(()=>e['Fixture.wide'](268435455),WebAssembly.RuntimeError);
            for(const alloc of [()=>0,()=>memory.buffer.byteLength-4]) {
                const bad=new WebAssembly.Instance(new WebAssembly.Module(fs.readFileSync(process.argv[2])),
                    {env:{memory,__heap_alloc:alloc}}).exports;
                assert.throws(()=>bad['Fixture.make'](0),WebAssembly.RuntimeError);
            }
            """);
    }

    @Test void matchesJvmNarrowArrayStoreConversionsIncludingBooleanLowBit() throws Exception {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "Stores", null, "java/lang/Object", null);
        int[] types = {Opcodes.T_BOOLEAN, Opcodes.T_BYTE, Opcodes.T_CHAR, Opcodes.T_SHORT};
        int[] stores = {Opcodes.BASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE};
        int[] loads = {Opcodes.BALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD};
        for (int i = 0; i < types.length; i++) {
            MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "kind"+i, "(I)I", null, null);
            method.visitCode(); method.visitInsn(Opcodes.ICONST_1); method.visitIntInsn(Opcodes.NEWARRAY, types[i]);
            method.visitVarInsn(Opcodes.ASTORE, 1); method.visitVarInsn(Opcodes.ALOAD, 1); method.visitInsn(Opcodes.ICONST_0);
            method.visitVarInsn(Opcodes.ILOAD, 0); method.visitInsn(stores[i]);
            method.visitVarInsn(Opcodes.ALOAD, 1); method.visitInsn(Opcodes.ICONST_0); method.visitInsn(loads[i]);
            method.visitInsn(Opcodes.IRETURN); method.visitMaxs(3, 2); method.visitEnd();
        }
        writer.visitEnd(); byte[] bytes = writer.toByteArray();
        Class<?> jvm = new ClassLoader() { Class<?> load() { return defineClass(null, bytes, 0, bytes.length); } }.load();
        StringBuilder assertions = new StringBuilder();
        for (int i = 0; i < types.length; i++) for (int value : new int[]{-65537,-32769,-3,-2,0,2,3,127,128,255,256,65535,65536})
            assertions.append("assert.equal(e['Stores.kind").append(i).append("'](").append(value).append("),")
                    .append(jvm.getMethod("kind"+i, int.class).invoke(null, value)).append(");");
        execute(BytecodeCompiler.compileClass(bytes), assertions.toString());
    }

    @Test void linksPrimitiveArrayHelpersOnceRegardlessOfJarOrder() throws Exception {
        fixture("""
            public class Fixture {
                public static int run(int n) {
                    int[] values = Numbers.make(n);
                    for (int i = 0; i < n; i++) values[i] += 10;
                    return Numbers.sum(values);
                }
            }
            class Numbers {
                static int[] make(int n) { int[] a = new int[n]; for(int i=0;i<n;i++) a[i]=i; return a; }
                static int sum(int[] a) { int sum=0; for(int value:a) sum+=value; return sum; }
            }
            """);
        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{directory.toUri().toURL()})) {
            var run = loader.loadClass("Fixture").getMethod("run", int.class);
            for (String[] order : new String[][]{{"Fixture", "Numbers"}, {"Numbers", "Fixture"}}) {
                String wat = linkJar(order);
                assertEquals(1, wat.split(java.util.regex.Pattern.quote("(func $__new_primitive_array"), -1).length - 1);
                execute(wat, "assert.equal(e['Fixture.run'](7)," + run.invoke(null, 7) + ");"
                        + "assert.equal(e['Fixture.run'](0)," + run.invoke(null, 0) + ");");
            }
        }
    }

    @Test void testsPrimitiveArraysAgainstLinkedMarkerInterfaceMetadata() throws Exception {
        fixture("""
            public class Fixture {
                public static int nested() {
                    Cloneable[] values = new int[1][]; values[0] = new int[2];
                    java.io.Serializable[] serial = new java.io.Serializable[1]; serial[0] = new int[1][1];
                    Object deep = new int[1][1][1];
                    return (values[0] instanceof int[] ? 1 : 0)
                        + (serial[0] instanceof int[][] ? 2 : 0) + (deep instanceof Cloneable[][] ? 4 : 0);
                }
                public static Object make() { return new int[1]; }
                public static int check(Object v) {
                    return (v instanceof Cloneable ? 1 : 0) + (v instanceof java.io.Serializable ? 2 : 0);
                }
            }
            """);
        var nodes = new java.util.LinkedHashMap<String,ClassNode>(); nodes.put("Fixture", readFixture("Fixture"));
        for (String name : new String[]{"java/lang/Cloneable", "java/io/Serializable"}) {
            ClassNode marker = new ClassNode(); marker.version = Opcodes.V17; marker.name = name;
            marker.superName = "java/lang/Object"; marker.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT;
            nodes.put(name, marker);
        }
        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{directory.toUri().toURL()})) {
            Class<?> jvm = loader.loadClass("Fixture"); Object array = jvm.getMethod("make").invoke(null);
            execute(linkNodes(nodes), "assert.equal(e['Fixture.check'](e['Fixture.make']()),"
                    + jvm.getMethod("check", Object.class).invoke(null, array) + ");assert.equal(e['Fixture.check'](0),0);assert.equal(e['Fixture.nested']()," + jvm.getMethod("nested").invoke(null) + ");");
        }
    }

    @Test void executesStackMappedConstructorChainingAndNestedNewWithJvmComparison() throws Exception {
        byte[] bytes = fixture("""
            public class Fixture {
                int n;
                Fixture(int n) { this(n > 0 ? n : 7, 5L); }
                private Fixture(int n, long extra) { this.n = n + (int) extra; }
                public static int run(int n) {
                    Fixture value = new Fixture(new Fixture(n > 2 ? n : 3).n);
                    return value.n;
                }
            }
            """);
        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{directory.toUri().toURL()})) {
            var run = loader.loadClass("Fixture").getMethod("run", int.class);
            execute(BytecodeCompiler.compileClass(bytes),
                    "assert.equal(e['Fixture.run'](9)," + run.invoke(null, 9) + ");"
                    + "assert.equal(e['Fixture.run'](-2)," + run.invoke(null, -2) + ");");
        }
    }

    @Test void executesPreSuperOwnFieldWriteWithJvmComparison() throws Exception {
        fixture("""
            public class Fixture {
                int n;
                Fixture() { n = 5; }
                public static int run() { return new Fixture().n; }
            }
            """);
        ClassNode owner = readFixture("Fixture");
        MethodNode init = owner.methods.stream().filter(m -> m.name.equals("<init>")).findFirst().orElseThrow();
        AbstractInsnNode load = null, call = null, put = null;
        for (AbstractInsnNode instruction : init.instructions) {
            if (instruction instanceof MethodInsnNode m && m.name.equals("<init>")) {
                call = instruction; load = instruction.getPrevious();
            }
            if (instruction.getOpcode() == Opcodes.PUTFIELD) put = instruction;
        }
        init.instructions.remove(load); init.instructions.remove(call);
        init.instructions.insert(put, call); init.instructions.insert(put, load);
        var jvm = linkedLoader(java.util.Map.of(owner.name, owner)).loadClass("Fixture");
        execute(BytecodeCompiler.compileClass(classBytes(owner)),
                "assert.equal(e['Fixture.run']()," + jvm.getMethod("run").invoke(null) + ");");
    }

    @Test void executesSealedClassesAndInterfacesInEitherJarOrder() throws Exception {
        fixture("""
            public class Fixture {
                public static int run(int n) { View v = new Leaf(n); return v.value(); }
            }
            sealed interface View permits Base { int value(); }
            abstract sealed class Base implements View permits Branch {}
            non-sealed class Branch extends Base { public int value() { return 3; } }
            final class Leaf extends Branch {
                int n;
                Leaf(int n) { this.n = n; }
                public int value() { return n + 7; }
            }
            """);
        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{directory.toUri().toURL()})) {
            var run = loader.loadClass("Fixture").getMethod("run", int.class);
            String assertions = "assert.equal(e['Fixture.run'](35)," + run.invoke(null, 35) + ");";
            execute(linkJar("Fixture", "View", "Base", "Branch", "Leaf"), assertions);
            execute(linkJar("Leaf", "Branch", "Base", "View", "Fixture"), assertions);
        }
    }

    @Test void executesMaximumParameterSlotsWithJvmComparison() throws Exception {
        String ints = java.util.stream.IntStream.range(0, 255)
                .mapToObj(i -> "int p" + i).collect(java.util.stream.Collectors.joining(","));
        String longs = java.util.stream.IntStream.range(0, 127)
                .mapToObj(i -> "long p" + i).collect(java.util.stream.Collectors.joining(","));
        String arguments = java.util.stream.IntStream.range(0, 127)
                .mapToObj(i -> i + "L").collect(java.util.stream.Collectors.joining(","));
        byte[] bytes = fixture("public class Fixture {"
                + " public static int ints(" + ints + ") { return p0 + p254; }"
                + " public static long wide(" + longs + ", int end) { return p126 + end; }"
                + " private long instance(" + longs + ") { return p126; }"
                + " public static long run() { return new Fixture().instance(" + arguments + "); } }");
        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{directory.toUri().toURL()})) {
            Class<?> jvm = loader.loadClass("Fixture");
            Class<?>[] intTypes = new Class<?>[255]; java.util.Arrays.fill(intTypes, int.class);
            Object[] intArgs = java.util.stream.IntStream.range(0, 255).boxed().toArray();
            Class<?>[] wideTypes = new Class<?>[128]; java.util.Arrays.fill(wideTypes, long.class); wideTypes[127] = int.class;
            Object[] wideArgs = new Object[128];
            for (int i = 0; i < 127; i++) wideArgs[i] = (long) i;
            wideArgs[127] = 5;
            execute(BytecodeCompiler.compileClass(bytes),
                    "assert.equal(e['Fixture.ints'](...Array.from({length:255},(_,i)=>i)),"
                    + jvm.getMethod("ints", intTypes).invoke(null, intArgs) + ");"
                    + "assert.equal(e['Fixture.wide'](...Array.from({length:127},(_,i)=>BigInt(i)),5),"
                    + jvm.getMethod("wide", wideTypes).invoke(null, wideArgs) + "n);"
                    + "assert.equal(e['Fixture.run']()," + jvm.getMethod("run").invoke(null) + "n);");
        }
    }

    @Test void matchesJvmNumericConstantValuesAndIgnoredInstanceConstants() throws Exception {
        String[] descriptors = {"Z", "B", "C", "S", "I", "J", "F", "F", "D", "D"};
        Object[] values = {1, -128, 65535, -32768, Integer.MIN_VALUE, Long.MAX_VALUE,
                -0.0f, Float.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "Constants", null, "java/lang/Object", null);
        for (int i = 0; i < descriptors.length; i++) {
            writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                    "constant" + i, descriptors[i], null, values[i]).visitEnd();
            MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                    "get" + i, "()" + descriptors[i], null, null);
            method.visitCode(); method.visitFieldInsn(Opcodes.GETSTATIC, "Constants", "constant" + i, descriptors[i]);
            method.visitInsn(Type.getType(descriptors[i]).getOpcode(Opcodes.IRETURN));
            method.visitMaxs(0, 0); method.visitEnd();
        }
        writer.visitField(Opcodes.ACC_PUBLIC, "ignored", "I", null, 99).visitEnd();
        MethodVisitor init = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode(); init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN); init.visitMaxs(0, 0); init.visitEnd();
        MethodVisitor read = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "instanceValue", "()I", null, null);
        read.visitCode(); read.visitTypeInsn(Opcodes.NEW, "Constants"); read.visitInsn(Opcodes.DUP);
        read.visitMethodInsn(Opcodes.INVOKESPECIAL, "Constants", "<init>", "()V", false);
        read.visitFieldInsn(Opcodes.GETFIELD, "Constants", "ignored", "I");
        read.visitInsn(Opcodes.IRETURN); read.visitMaxs(0, 0); read.visitEnd(); writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        Class<?> jvm = new ClassLoader() {
            Class<?> load() { return defineClass("Constants", bytes, 0, bytes.length); }
        }.load();
        StringBuilder assertions = new StringBuilder();
        for (int i = 0; i < descriptors.length; i++) {
            Object expected = jvm.getMethod("get" + i).invoke(null);
            String literal = expected instanceof Boolean b ? (b ? "1" : "0")
                    : expected instanceof Character c ? Integer.toString(c) : expected.toString();
            if (expected instanceof Long) literal += "n";
            assertions.append("assert.equal(e['Constants.get").append(i).append("'](),").append(literal).append(");");
        }
        assertEquals(0, jvm.getMethod("instanceValue").invoke(null));
        assertions.append("assert.equal(e['Constants.instanceValue'](),0);");
        execute(BytecodeCompiler.compileClass(bytes), assertions.toString());
    }

    @Test void rejectsConstantValueTypeMismatchesLikeJvm() {
        for (String descriptor : new String[]{"I", "J", "F", "D", "Ljava/lang/String;", "[I"}) {
            ClassWriter writer = new ClassWriter(0);
            writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "BadConstant", null, "java/lang/Object", null);
            writer.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "bad", descriptor, null,
                    descriptor.equals("I") ? (Object) Long.valueOf(1) : Integer.valueOf(1)).visitEnd();
            writer.visitEnd(); byte[] bytes = writer.toByteArray();
            assertThrows(ClassFormatError.class, () -> new ClassLoader() {
                Class<?> load() { return defineClass("BadConstant", bytes, 0, bytes.length); }
            }.load(), descriptor);
            String error = assertThrows(IllegalArgumentException.class,
                    () -> BytecodeCompiler.compileClass(bytes)).getMessage();
            assertTrue(error.contains("ConstantValue type does not match"), error);
        }
    }

    private byte[] classBytes(ClassNode type) {
        ClassWriter writer = new ClassWriter(0); type.accept(writer); return writer.toByteArray();
    }

    private ClassLoader linkedLoader(java.util.Map<String, ClassNode> linked) {
        return new ClassLoader(null) {
            @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
                ClassNode type = linked.get(name.replace('.', '/'));
                if (type == null) throw new ClassNotFoundException(name);
                byte[] bytes = classBytes(type); return defineClass(name, bytes, 0, bytes.length);
            }
        };
    }

    private ClassNode readFixture(String name) throws Exception {
        return BytecodeInput.read(Files.readAllBytes(directory.resolve(name + ".class")));
    }

    private String linkNodes(java.util.Map<String, ClassNode> nodes) throws Exception {
        var output = new java.io.ByteArrayOutputStream();
        try (var jar = new java.util.jar.JarOutputStream(output)) {
            for (ClassNode type : nodes.values()) {
                jar.putNextEntry(new java.util.jar.JarEntry(type.name + ".class"));
                jar.write(classBytes(type)); jar.closeEntry();
            }
        }
        return BytecodeCompiler.compileJar(new java.io.ByteArrayInputStream(output.toByteArray()));
    }

    @Test void preservesJvmStaticInstanceSlotsInsteadOfApplyingJavaSourceRestrictions() throws Exception {
        fixture("""
            public class Fixture {
                public static int run() { Base b = new Child(); return b.value() + Base.hidden(); }
            }
            class Base { public final int value() { return 10; } static int hidden() { return 20; } }
            class Child extends Base {}
            """);
        ClassNode entry = readFixture("Fixture"), base = readFixture("Base"), child = readFixture("Child");
        // These declarations are legal JVM bytecode although javac forbids them in Java source.
        MethodNode staticValue = integerDeclaration(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
        child.methods.add(staticValue);
        MethodNode instanceHidden = integerDeclaration(Opcodes.ACC_PUBLIC); instanceHidden.name = "hidden";
        child.methods.add(instanceHidden);
        MethodNode run = entry.methods.stream().filter(m -> m.name.equals("run")).findFirst().orElseThrow();
        var extra = new InsnList();
        extra.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Child", "value", "()I", false));
        extra.add(new InsnNode(Opcodes.IADD)); extra.add(new TypeInsnNode(Opcodes.NEW, "Child"));
        extra.add(new InsnNode(Opcodes.DUP));
        extra.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "Child", "<init>", "()V", false));
        extra.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "Child", "hidden", "()I", false));
        extra.add(new InsnNode(Opcodes.IADD)); run.instructions.insertBefore(java.util.Arrays.stream(run.instructions.toArray())
                .filter(insn -> insn.getOpcode() == Opcodes.IRETURN).findFirst().orElseThrow(), extra); run.maxStack = 4;
        var linked = new java.util.LinkedHashMap<String, ClassNode>();
        linked.put(entry.name, entry); linked.put(base.name, base); linked.put(child.name, child);
        Object expected = linkedLoader(linked).loadClass("Fixture").getMethod("run").invoke(null);
        assertEquals(32, expected);
        execute(linkNodes(linked), "assert.equal(e['Fixture.run']()," + expected + ");");
        var reversed = new java.util.LinkedHashMap<String, ClassNode>();
        reversed.put(child.name, child); reversed.put(base.name, base); reversed.put(entry.name, entry);
        execute(linkNodes(reversed), "assert.equal(e['Fixture.run'](),32);");
    }

    @Test void executesDistinctJvmIdentitiesAndEscapesUnusualExportNames() throws Exception {
        var linked = new java.util.LinkedHashMap<String, ClassNode>();
        ClassNode entry = new ClassNode(); entry.version = Opcodes.V17; entry.access = Opcodes.ACC_PUBLIC;
        entry.name = "Fixture"; entry.superName = "java/lang/Object";
        MethodNode run = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", "()I", null, null);
        run.instructions.add(new InsnNode(Opcodes.ICONST_0)); run.maxStack = 2; entry.methods.add(run); linked.put(entry.name, entry);
        int expected = 0;
        for (String name : new String[]{"A$B", "A_B", "Odd(Name)"}) {
            ClassNode type = new ClassNode(); type.version = Opcodes.V17; type.access = Opcodes.ACC_PUBLIC;
            type.name = name; type.superName = "java/lang/Object";
            for (String member : new String[]{"m$", "m_", "quote\"\\λ"}) {
                int number = ++expected;
                String descriptor = name.equals("Odd(Name)") ? "(LOdd(Name);)I" : "()I";
                MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, member, descriptor, null, null);
                method.instructions.add(new IntInsnNode(Opcodes.BIPUSH, number)); method.instructions.add(new InsnNode(Opcodes.IRETURN));
                method.maxStack = 1; method.maxLocals = name.equals("Odd(Name)") ? 1 : 0; type.methods.add(method);
                if (name.equals("Odd(Name)")) run.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
                run.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, name, member, descriptor, false));
                run.instructions.add(new InsnNode(Opcodes.IADD));
            }
            linked.put(name, type);
        }
        run.instructions.add(new InsnNode(Opcodes.IRETURN));
        Object result = linkedLoader(linked).loadClass("Fixture").getMethod("run").invoke(null);
        assertEquals(45, result);
        execute(linkNodes(linked), "assert.equal(e['Fixture.run'](),45);");
    }

    @Test void rejectsWrongSpecialReceiversLikeJvmForClassAndInterfaceCalls() throws Exception {
        for (boolean iface : new boolean[]{false, true}) {
            fixture(iface ? """
                public class Fixture implements View { public int call(View other) { return View.super.value(); } }
                interface View {default int value() {return 9;}}
                """ : """
                public class Fixture extends Base { public int call(Base other) { return super.value(); } }
                class Base {public int value() {return 9;}}
                """);
            ClassNode entry = readFixture("Fixture"), parent = readFixture(iface ? "View" : "Base");
            MethodNode method = entry.methods.stream().filter(m -> m.name.equals("call")).findFirst().orElseThrow();
            for (AbstractInsnNode insn : method.instructions)
                if (insn instanceof VarInsnNode local && local.getOpcode() == Opcodes.ALOAD) local.var = 1;
            var linked = java.util.Map.of(entry.name, entry, parent.name, parent);
            assertThrows(VerifyError.class, () -> linkedLoader(linked).loadClass("Fixture").getDeclaredMethods());
            String error = assertThrows(IllegalArgumentException.class, () -> new ClassBytecodeCompiler(entry).compileToWat(linked)).getMessage();
            assertTrue(error.contains("special receiver must be assignable to the caller"), error);
        }
    }

    @Test void matchesJvmAccSuperSelectionWithPrivateResolvedMethod() throws Exception {
        fixture("""
            public class Fixture {
                public static int run() { return new Child().call(); }
                static class Grand { private int value() { return 11; } }
                static class Parent extends Grand { public int value() { return 22; } }
                static class Child extends Parent { public int call() { return super.value(); } }
            }
            """);
        var linked = new java.util.LinkedHashMap<String, ClassNode>();
        for (String name : new String[]{"Fixture", "Fixture$Grand", "Fixture$Parent", "Fixture$Child"})
            linked.put(name, readFixture(name));
        for (MethodNode method : linked.get("Fixture$Child").methods)
            for (AbstractInsnNode instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call && call.name.equals("value")) call.owner = "Fixture$Grand";
        Object expected = linkedLoader(linked).loadClass("Fixture").getMethod("run").invoke(null);
        assertEquals(22, expected);
        execute(linkNodes(linked), "assert.equal(e['Fixture.run'](),22);");
    }

    @Test void preservesFieldIdentitiesContainingDescriptorSeparators() throws Exception {
        fixture("""
            public class Fixture {
                public static int run() {
                    Box box = new Box(); box.first = new C(); box.first.marker = 123;
                    box.second = null; return box.first.marker;
                }
            }
            class Box { C first; B second; }
            class C { int marker; }
            class B {}
            """);
        var names = java.util.Map.of("C", "c", "B", "b:Lc");
        var linked = new java.util.LinkedHashMap<String, ClassNode>();
        for (String name : new String[]{"Fixture", "Box", "C", "B"}) {
            ClassNode type = readFixture(name); type.name = names.getOrDefault(name, name);
            for (FieldNode field : type.fields) {
                if (type.name.equals("Box")) field.name = field.name.equals("first") ? "a:Lb" : "a";
                field.desc = field.desc.replace("LC;", "Lc;").replace("LB;", "Lb:Lc;");
            }
            for (MethodNode method : type.methods) {
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof TypeInsnNode reference) reference.desc = names.getOrDefault(reference.desc, reference.desc);
                    if (insn instanceof MethodInsnNode call) call.owner = names.getOrDefault(call.owner, call.owner);
                    if (insn instanceof FieldInsnNode field) {
                        if (field.owner.equals("Box")) field.name = field.name.equals("first") ? "a:Lb" : "a";
                        field.owner = names.getOrDefault(field.owner, field.owner);
                        field.desc = field.desc.replace("LC;", "Lc;").replace("LB;", "Lb:Lc;");
                    }
                }
            }
            linked.put(type.name, type);
        }
        Object expected = linkedLoader(linked).loadClass("Fixture").getMethod("run").invoke(null);
        assertEquals(123, expected);
        execute(linkNodes(linked), "assert.equal(e['Fixture.run'](),123);");
    }

    @Test void executesPrivateJvmSurrogateNamesAndDiagnosesUnrepresentableExports() throws Exception {
        ClassNode type = new ClassNode(); type.version = Opcodes.V17; type.access = Opcodes.ACC_PUBLIC;
        type.name = "Fixture"; type.superName = "java/lang/Object";
        MethodNode run = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", "()I", null, null);
        int number = 0;
        for (String name : new String[]{"\uD800", "\uD801"}) {
            MethodNode method = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, name, "()I", null, null);
            method.instructions.add(new IntInsnNode(Opcodes.BIPUSH, ++number));
            method.instructions.add(new InsnNode(Opcodes.IRETURN)); method.maxStack = 1; type.methods.add(method);
            run.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, type.name, name, "()I", false));
        }
        run.instructions.add(new InsnNode(Opcodes.IADD)); run.instructions.add(new InsnNode(Opcodes.IRETURN));
        run.maxStack = 2; type.methods.add(run);
        assertEquals(3, linkedLoader(java.util.Map.of(type.name, type)).loadClass("Fixture").getMethod("run").invoke(null));
        execute(new ClassBytecodeCompiler(type).compileToWat(), "assert.equal(e['Fixture.run'](),3);");
        type.methods.get(0).access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
        String error = assertThrows(IllegalArgumentException.class, () -> new ClassBytecodeCompiler(type).compileToWat()).getMessage();
        assertTrue(error.contains("public export name"), error);
        assertTrue(error.contains("well-formed Unicode"), error);
    }

    @Test void matchesJvmDescriptorDistinctStaticFields() throws Exception {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "FieldIdentity", null, "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_STATIC, "value", "I", null, null).visitEnd();
        writer.visitField(Opcodes.ACC_STATIC, "value", "J", null, null).visitEnd();
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", "()J", null, null);
        method.visitCode();
        method.visitLdcInsn(37); method.visitFieldInsn(Opcodes.PUTSTATIC, "FieldIdentity", "value", "I");
        method.visitLdcInsn(4294967299L); method.visitFieldInsn(Opcodes.PUTSTATIC, "FieldIdentity", "value", "J");
        method.visitFieldInsn(Opcodes.GETSTATIC, "FieldIdentity", "value", "I"); method.visitInsn(Opcodes.I2L);
        method.visitFieldInsn(Opcodes.GETSTATIC, "FieldIdentity", "value", "J"); method.visitInsn(Opcodes.LADD);
        method.visitInsn(Opcodes.LRETURN); method.visitMaxs(0, 0); method.visitEnd(); writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        Class<?> jvm = new ClassLoader() {
            Class<?> load() { return defineClass("FieldIdentity", bytes, 0, bytes.length); }
        }.load();
        long expected = (Long) jvm.getMethod("run").invoke(null);
        execute(BytecodeCompiler.compileClass(bytes),
                "assert.equal(e['FieldIdentity.run'](), " + expected + "n);");
    }

    @Test void executesJavacBranchesLoopsJoinsAndStaticCalls() throws Exception {
        String wat=BytecodeCompiler.compileClass(fixture("""
            public class Fixture {
                public static final int CONSTANT=42;
                private static int twice(int n) { return n*2; }
                private static int twice(boolean n) { return n ? 1 : 0; }
                public static int sum(int n) {
                    int sum=0; for(int i=0;i<=n;i++) { if((i&1)==0) sum+=twice(i); else sum-=i; }
                    return sum+CONSTANT;
                }
                public static int choose(int n) { return n<0 ? n*2 : n+3; }
                public static int countdown(int n) { while(n>0) n--; return n; }
                public static int divide(int a,int b) { return a/b; }
                public static int narrowing(int n) { return (byte)n+(short)n+(char)n; }
            }
            """));
        execute(wat,"""
            for(let n=-3;n<30;n++) {
                let sum=42; for(let i=0;i<=n;i++) sum+=(i&1)===0 ? i*2 : -i;
                assert.equal(e['Fixture.sum'](n),sum);
                assert.equal(e['Fixture.choose'](n),n<0?n*2:n+3);
                assert.equal(e['Fixture.countdown'](n),Math.min(n,0));
            }
            assert.equal(e['Fixture.narrowing'](65535),65533);
            assert.equal(e['Fixture.divide'](-2147483648,-1),-2147483648);
            assert.equal(e['Fixture.divide'](-7,2),-3);
            """);
    }

    @Test void executesDenseAndSparseSwitchesWithJoinsAndBackEdges() throws Exception {
        byte[] bytes = fixture("""
            public class Fixture {
                public static int dense(int n) {
                    int result=0;
                    switch(n) {
                        case -2: result=7;
                        case -1: result+=3; break;
                        case 0: case 1: result=11; break;
                        default: result=-9;
                    }
                    return result+1;
                }
                public static int sparse(int n) {
                    return 100 + switch(n) {
                        case -2147483648 -> 1;
                        case -10000, 10000 -> 2;
                        case 2147483647 -> 3;
                        default -> 4;
                    };
                }
                public static int loop(int n) {
                    int result=0;
                    while(n>0) {
                        switch(n%4) {
                            case 0: n--; continue;
                            case 1: result+=10; break;
                            case 2: result+=20; break;
                            default: result+=30;
                        }
                        n--;
                    }
                    return result;
                }
            }
            """);
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        assertTrue(node.methods.stream().flatMap(m -> java.util.Arrays.stream(m.instructions.toArray()))
                .anyMatch(i -> i instanceof TableSwitchInsnNode));
        assertTrue(node.methods.stream().flatMap(m -> java.util.Arrays.stream(m.instructions.toArray()))
                .anyMatch(i -> i instanceof LookupSwitchInsnNode));
        execute(BytecodeCompiler.compileClass(bytes), """
            for(let n=-5;n<=5;n++)
                assert.equal(e['Fixture.dense'](n),n===-2?11:n===-1?4:n===0||n===1?12:-8);
            for(const n of [-2147483648,-10000,10000,2147483647,-1,0,9999])
                assert.equal(e['Fixture.sparse'](n),100+(n===-2147483648?1:n===-10000||n===10000?2:n===2147483647?3:4));
            for(let n=0;n<40;n++) {
                let expected=0; for(let i=n;i>0;i--) expected+=(i%4)*10;
                assert.equal(e['Fixture.loop'](n),expected);
            }
            """);
    }

    @Test void executesObjectAllocationConstructorsAliasesAndNullGuards() throws Exception {
        execute(BytecodeCompiler.compileClass(fixture("""
            public class Fixture {
                int a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t;
                Fixture next;
                Fixture(int value) { a=value; t=3; }
                public static int calculate(int n) {
                    Fixture first=new Fixture(n), second=new Fixture(n+1);
                    first.next=second; second.next=first;
                    Fixture alias=first.next;
                    alias.t=8;
                    return first.a*100+first.t*10+second.a+second.t+first.b+second.s
                        +(second.next==first ? 1000 : 0);
                }
                public static int defaults() {
                    Fixture value=new Fixture(0);
                    return value.b+value.s+(value.next==null ? 1 : 0);
                }
                public static int readNull() { Fixture value=null; return value.t; }
                public static void writeNull() { Fixture value=null; value.a=9; }
            }
            """)), """
            assert.equal(e['Fixture.defaults'](),1);
            for(let n=-3;n<20;n++) assert.equal(e['Fixture.calculate'](n),101*n+1039);
            assert.throws(()=>e['Fixture.readNull'](),WebAssembly.RuntimeError);
            assert.throws(()=>e['Fixture.writeNull'](),WebAssembly.RuntimeError);
            assert.equal(new DataView(memory.buffer).getInt32(4,true),0);
            """);
    }

    @Test void executesInheritedLayoutsHiddenFieldsAndSuperclassConstructors() throws Exception {
        fixture("""
            public class Fixture {
                public static int calculate(int n) {
                    Leaf leaf=new Leaf(n);
                    Root root=leaf;
                    Middle middle=leaf;
                    leaf.inherited+=2;
                    leaf.same+=3;
                    root.same+=4;
                    middle.link=leaf;
                    root.tag=9; leaf.tag=leaf;
                    return root.inherited*1000+root.same*100+leaf.same*10+leaf.tail
                        +(middle.link==leaf ? 10000 : 0)+(root.empty==null ? 1 : 0)
                        +(root.tag==9 && leaf.tag==leaf ? 20 : 0);
                }
                public static int separate() {
                    Root root=new Root(2);
                    Leaf leaf=new Leaf(5);
                    root.inherited=8;
                    return leaf.inherited*100+root.inherited*10+leaf.tail;
                }
            }
            class Root {
                int inherited, same, tag;
                static int excluded;
                Root empty;
                Root(int n) { inherited=n; same=1; }
            }
            abstract class Middle extends Root {
                Root link;
                Middle(int n) { super(n); }
            }
            class Leaf extends Middle {
                int same, tail;
                Root tag;
                Leaf(int n) { super(n); same=2; tail=7; }
            }
            """);
        // Reverse hierarchy order exercises layout construction independently of JAR ordering.
        execute(linkJar("Leaf", "Middle", "Fixture", "Root"), """
            for(let n=-3;n<20;n++) assert.equal(e['Fixture.calculate'](n),1000*n+12578);
            assert.equal(e['Fixture.separate'](),587);
            """);
    }

    @Test void executesPublicVirtualAndInterfaceDispatch() throws Exception {
        fixture("""
            public class Fixture {
                static int ticks;
                static int tick() { ticks++; return 3; }
                public static int calculate(int n) {
                    View view=n<0 ? new Base(3) : new Leaf(5);
                    view.set(n);
                    return view.self().value(7)+view.value(2);
                }
                public static int constructorDispatch() { return new Leaf(5).initial; }
                public static int inheritedInterface() { ChildView view=new Leaf(3); return view.value(2); }
                public static int multi() { View view=new Leaf(4); return view.combine(view.value(1),3); }
                public static int classCall(int n) {
                    Base base=new Leaf(n);
                    return 100+base.value(base.value(1));
                }
                public static int inheritedOwner(int n) { return new Leaf(n).value(2); }
                public static int nullCall() { View view=null; return view.value(tick()); }
                public static int tickCount() { return ticks; }
                public static int external(Base base) { return base.value(2); }
                public static Unrelated unrelated() { return new Unrelated(); }
            }
            interface View {
                int value(int n);
                void set(int n);
                View self();
                int combine(int first, int second);
            }
            interface ChildView extends View {}
            class Base implements View {
                int state, initial;
                Base(int n) { state=n; initial=value(1); }
                public int value(int n) { return state+n; }
                public void set(int n) { state=n; }
                public View self() { return this; }
                public int combine(int first, int second) { return value(first)+second; }
            }
            abstract class Intermediate extends Base {
                Intermediate(int n) { super(n); }
                public int value(int n) { return super.value(n); }
                public abstract int hook();
            }
            class Derived extends Intermediate {
                Derived(int n) { super(n); }
                public int value(int n) { return super.value(n)*2; }
                public int hook() { return 0; }
            }
            class Leaf extends Derived implements ChildView {
                Leaf(int n) { super(n); }
            }
            class Unrelated {
                public int value(int n) { return 999; }
            }
            """);
        execute(linkJar("Leaf", "Unrelated", "Fixture", "Derived", "Intermediate", "ChildView", "View", "Base"), """
            for(let n=-5;n<10;n++) {
                assert.equal(e['Fixture.calculate'](n),n<0?2*n+9:4*n+18);
                assert.equal(e['Fixture.classCall'](n),104+6*n);
                assert.equal(e['Fixture.inheritedOwner'](n),2*(n+2));
            }
            assert.equal(e['Fixture.constructorDispatch'](),12);
            assert.equal(e['Fixture.inheritedInterface'](),10);
            assert.equal(e['Fixture.multi'](),31);
            assert.throws(()=>e['Fixture.external'](0),WebAssembly.RuntimeError);
            assert.throws(()=>e['Fixture.nullCall'](),WebAssembly.RuntimeError);
            assert.equal(e['Fixture.tickCount'](),1);
            assert.throws(()=>e['Fixture.external'](e['Fixture.unrelated']()),WebAssembly.RuntimeError);
            """);
    }

    @Test void diagnosesUnsupportedDynamicCalls() throws Exception {
        for (String source : new String[]{
                "public class Fixture { public static int call(Unlinked f){return f.value();} } class Unlinked { public int value(){return 1;} }",
                "public class Fixture { public static int call(View v){return v.value();} } interface View {default int value(){return 1;} }",
                "public class Fixture { public static int call(Missing v){return v.value();} } interface Missing {int value();}"}) {
            var bytes=fixture(source);
            // Only Fixture is linked; class and interface owners must be linked explicitly.
            var error=assertThrows(IllegalArgumentException.class, () -> BytecodeCompiler.compileClass(bytes));
            assertTrue(error.getMessage().contains("unsupported descriptor resolution"),error.getMessage());
            assertTrue(error.getMessage().contains("Fixture.java:"),error.getMessage());
        }
    }

    @Test void executesMostSpecificInterfaceDefaultsAndClassPrecedence() throws Exception {
        fixture("""
            public class Fixture {
                public static int run(int n) {
                    Root value=new Device();
                    value.set(n);
                    return 100+value.self().value(2)+value.pair(3,4);
                }
                public static int classOwner() { return new Device().value(2); }
                public static int inheritedInterface() { Diamond d=new Device(); return d.value(2); }
                public static int classWins() { Root r=new ClassChild(); return r.value(2); }
                public static int redeclared() { Root r=new Redeclared(); return r.value(2); }
                public static int inheritedSuper() { Root r=new ExplicitSuper(); return r.value(2); }
                public static int nullCall() { Root r=null; return r.value(2); }
            }
            interface Root {
                int seed();
                void write(int n);
                default int value(int n) { return seed()+n; }
                default int pair(int a,int b) { return value(a)+value(b); }
                default Root self() { return this; }
                default void set(int n) { write(n); }
            }
            interface Left extends Root {}
            interface Fancy extends Root {
                default int value(int n) { return Root.super.value(n)*2; }
            }
            interface Diamond extends Left, Fancy {}
            class Device implements Diamond {
                int state=3;
                public int seed() { return state; }
                public void write(int n) { state=n; }
            }
            class Parent {
                public int value(int n) { return 70+n; }
            }
            class ClassChild extends Parent implements Fancy {
                public int seed() { return 1; }
                public void write(int n) {}
            }
            class ExplicitSuper implements Left {
                public int seed() { return 3; }
                public void write(int n) {}
                public int value(int n) { return Left.super.value(n)+10; }
            }
            interface AbstractAgain extends Fancy { int value(int n); }
            class Redeclared implements AbstractAgain {
                public int value(int n) { return 90+n; }
                public int seed() { return 0; }
                public void write(int n) {}
            }
            """);
        execute(linkJar("ExplicitSuper", "Redeclared", "AbstractAgain", "ClassChild", "Parent", "Device", "Diamond",
                "Fancy", "Left", "Root", "Fixture"), """
            for(let n=-5;n<10;n++) assert.equal(e['Fixture.run'](n),6*n+118);
            assert.equal(e['Fixture.classOwner'](),10);
            assert.equal(e['Fixture.inheritedInterface'](),10);
            assert.equal(e['Fixture.classWins'](),72);
            assert.equal(e['Fixture.redeclared'](),92);
            assert.equal(e['Fixture.inheritedSuper'](),15);
            assert.throws(()=>e['Fixture.nullCall'](),WebAssembly.RuntimeError);
            """);
    }

    @Test void rejectsConflictingAndReabstractedDefaultsWithCallSiteDiagnostics() throws Exception {
        fixture("""
            public class Fixture { public static int call(Root r) { return r.value(); } }
            interface Root { default int value() { return 1; } }
            interface Other { default int value() { return 2; } }
            class Conflicted implements Root, Other { public int value() { return 3; } }
            interface Reabstract extends Root { int value(); }
            class Missing implements Reabstract { public int value() { return 4; } }
            abstract class AbstractBase implements Root { public abstract int value(); }
            class AbstractMissing extends AbstractBase { public int value() { return 5; } }
            """);
        // Model separately compiled binaries whose overrides have been removed.
        for(String name:new String[]{"Conflicted", "Missing", "AbstractMissing"}) {
            ClassNode node=new ClassNode();
            new ClassReader(Files.readAllBytes(directory.resolve(name+".class"))).accept(node,0);
            node.methods.removeIf(method -> method.name.equals("value"));
            ClassWriter writer=new ClassWriter(0); node.accept(writer);
            Files.write(directory.resolve(name+".class"),writer.toByteArray());
        }
        for(String[] names:new String[][]{
                {"Fixture", "Root", "Other", "Conflicted"},
                {"Fixture", "Root", "Reabstract", "Missing"},
                {"Fixture", "Root", "AbstractBase", "AbstractMissing"}}) {
            var error=assertThrows(IllegalArgumentException.class, () -> linkJar(names));
            assertTrue(error.getMessage().contains("Fixture.java:"),error.getMessage());
            assertTrue(error.getMessage().contains("Fixture.call(LRoot;)I"),error.getMessage());
            assertTrue(error.getMessage().contains(names.length==4 && names[2].equals("Other")
                    ? "conflicting maximally-specific" : "re-abstracted"),error.getMessage());
        }
    }

    @Test void executesPrivateClassAndInterfaceHelpersWithoutOverriding() throws Exception {
        byte[] bytes=fixture("""
            public class Fixture {
                int state;
                static int ticks;
                private int helper(int n) { return n+1; }
                private int helper(boolean b) { return b?5:6; }
                private void write(int n) { state=n; }
                private Fixture self() { return this; }
                public int evaluate(int n) { write(n); return self().helper(n)+helper(true)+state; }
                public static int run(int n) { Fixture value=new Child(); return value.evaluate(n); }
                static int tick() { ticks++; return 1; }
                public static int nullCall() { Fixture value=null; return value.helper(tick()); }
                public static int tickCount() { return ticks; }
                public static int interfaceCall(int n) { PrivateView value=new HelperDevice(); return value.result(n); }
            }
            class Child extends Fixture { public int helper(int n) { return 900; } }
            interface PrivateView {
                private int helper(int n) { return n+10; }
                private int helper(boolean b) { return b?2:3; }
                private static int twice(int n) { return n*2; }
                default int result(int n) { return helper(n)+helper(true)+twice(n); }
            }
            class HelperDevice implements PrivateView { public int helper(int n) { return 800; } }
            """);
        ClassNode node=new ClassNode(); new ClassReader(bytes).accept(node,0);
        assertTrue(node.methods.stream().flatMap(m -> java.util.Arrays.stream(m.instructions.toArray()))
                .anyMatch(i -> i instanceof MethodInsnNode call && call.name.equals("helper")
                        && call.getOpcode()==Opcodes.INVOKEVIRTUAL));
        execute(linkJar("HelperDevice", "PrivateView", "Child", "Fixture"), """
            for(let n=-5;n<20;n++) {
                assert.equal(e['Fixture.run'](n),2*n+6);
                assert.equal(e['Fixture.interfaceCall'](n),3*n+12);
            }
            assert.throws(()=>e['Fixture.nullCall'](),WebAssembly.RuntimeError);
            assert.equal(e['Fixture.tickCount'](),1);
            """);
    }

    @Test void executesPrivateNestmateCallsAndConstructors() throws Exception {
        fixture("""
            public class Fixture {
                private static int adjust() { return -1; }
                public static int run(int n) {
                    Secret value=new Child(n);
                    return value.helper(2)+Secret.twice(n)+Reader.read(value);
                }
                static class Secret {
                    int state;
                    private Secret(int n) { state=n; }
                    private int helper(int n) { return state+n; }
                    private static int twice(int n) { return n*2; }
                }
                static class Child extends Secret {
                    Child(int n) { super(n); }
                    public int helper(int n) { return 999; }
                }
                static class Reader {
                    static int read(Secret value) { return value.helper(3)+adjust(); }
                }
            }
            """);
        execute(linkJar("Fixture$Reader", "Fixture$Child", "Fixture$Secret", "Fixture"), """
            for(let n=-5;n<20;n++) assert.equal(e['Fixture.run'](n),4*n+4);
            """);
    }

    @Test void rejectsMissingOrNonreciprocalPrivateNests() throws Exception {
        fixture("""
            public class Fixture {
                static class Secret { private int helper() { return 2; } }
                static class Reader { public static int call(Secret value) { return value.helper(); } }
            }
            """);
        var missing=assertThrows(IllegalArgumentException.class, () -> linkJar("Fixture$Reader", "Fixture$Secret"));
        assertTrue(missing.getMessage().contains("validated linked nest"),missing.getMessage());
        ClassNode host=new ClassNode();
        new ClassReader(Files.readAllBytes(directory.resolve("Fixture.class"))).accept(host,0);
        host.nestMembers.remove("Fixture$Secret");
        ClassWriter writer=new ClassWriter(0); host.accept(writer);
        Files.write(directory.resolve("Fixture.class"),writer.toByteArray());
        var invalid=assertThrows(IllegalArgumentException.class,
                () -> linkJar("Fixture$Reader", "Fixture$Secret", "Fixture"));
        assertTrue(invalid.getMessage().contains("Fixture.java:"),invalid.getMessage());
        assertTrue(invalid.getMessage().contains("Fixture$Reader.call"),invalid.getMessage());
        assertTrue(invalid.getMessage().contains("reciprocal same-package"),invalid.getMessage());
    }

    @Test void executesLegacyPrivateSpecialCalls() throws Exception {
        Path source=directory.resolve("Fixture.java");
        Files.writeString(source,"""
            public class Fixture {
                private Fixture() {}
                private int helper(int n) { return n+2; }
                public static int run(int n) { return new Fixture().helper(n); }
            }
            """);
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"--release","8","-g",
                "-d",directory.toString(),source.toString()));
        byte[] bytes=Files.readAllBytes(directory.resolve("Fixture.class"));
        ClassNode node=new ClassNode(); new ClassReader(bytes).accept(node,0);
        assertTrue(node.methods.stream().flatMap(m -> java.util.Arrays.stream(m.instructions.toArray()))
                .anyMatch(i -> i instanceof MethodInsnNode call && call.name.equals("helper")
                        && call.getOpcode()==Opcodes.INVOKESPECIAL));
        execute(BytecodeCompiler.compileClass(bytes), "assert.equal(e['Fixture.run'](40),42);");
    }

    @Test void rejectsForgedCrossPackageNestsForEveryPrivateCallKind() {
        for(int opcode:new int[]{Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.INVOKESTATIC}) {
            ClassNode caller=new ClassNode(), target=new ClassNode(), host=new ClassNode();
            caller.name="a/Caller"; target.name="b/Secret"; host.name="a/Host";
            for(ClassNode node:new ClassNode[]{caller,target,host}) {
                node.version=Opcodes.V17; node.access=Opcodes.ACC_PUBLIC;
                node.superName="java/lang/Object";
            }
            caller.sourceFile="Caller.java";
            caller.nestHostClass=host.name; target.nestHostClass=host.name;
            host.nestMembers=new java.util.ArrayList<>(java.util.List.of(caller.name,target.name));
            MethodNode helper=new MethodNode(Opcodes.ACC_PRIVATE
                    | (opcode==Opcodes.INVOKESTATIC?Opcodes.ACC_STATIC:0),"helper","()I",null,null);
            helper.instructions.add(new InsnNode(Opcodes.ICONST_2));
            helper.instructions.add(new InsnNode(Opcodes.IRETURN));
            helper.maxLocals=1; helper.maxStack=1; target.methods.add(helper);
            MethodNode method=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"call","(Lb/Secret;)I",null,null);
            if(opcode!=Opcodes.INVOKESTATIC) method.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
            method.instructions.add(new MethodInsnNode(opcode,target.name,"helper","()I",false));
            method.instructions.add(new InsnNode(Opcodes.IRETURN));
            method.maxLocals=1; method.maxStack=1; caller.methods.add(method);
            var linked=java.util.Map.of(caller.name,caller,target.name,target,host.name,host);
            var error=assertThrows(IllegalArgumentException.class,
                    () -> new ClassBytecodeCompiler(caller).compileToWat(linked));
            assertTrue(error.getMessage().contains("Caller.java:"),error.getMessage());
            assertTrue(error.getMessage().contains("reciprocal same-package"),error.getMessage());
        }
    }

    private void compileAccessFixtures() throws Exception {
        var sources=java.util.Map.ofEntries(
            java.util.Map.entry("p/Base.java", """
                package p;
                public class Base {
                    int hidden() { return 1; }
                    protected int value() { return 2; }
                    public int invokeHidden() { return hidden(); }
                    public int invokeValue() { return value(); }
                }
                """),
            java.util.Map.entry("q/Outsider.java", """
                package q;
                public class Outsider extends p.Base {
                    public int hidden() { return 30; }
                    public int value() { return 40; }
                }
                """),
            java.util.Map.entry("p/Back.java", "package p; public class Back extends q.Outsider { public int hidden(){return 5;} }"),
            java.util.Map.entry("p/Bridge.java", "package p; public class Bridge extends Base { public int hidden(){return 6;} }"),
            java.util.Map.entry("q/BridgeChild.java", "package q; public class BridgeChild extends p.Bridge { public int hidden(){return 7;} }"),
            java.util.Map.entry("q/NoBridgeChild.java", "package q; public class NoBridgeChild extends Outsider { public int hidden(){return 50;} }"),
            java.util.Map.entry("r/Leaf.java", "package r; public class Leaf extends p.Back { public int hidden(){return 8;} }"),
            java.util.Map.entry("p/PackageCaller.java", """
                package p;
                public class PackageCaller {
                    public static int hidden(Base value) { return value.hidden(); }
                    public static int protectedCall(Base value) { return value.value(); }
                }
                """),
            java.util.Map.entry("q/ProtectedCaller.java", """
                package q;
                public class ProtectedCaller extends p.Base {
                    protected int value() { return 90; }
                    public static int call(ProtectedCaller receiver) { return receiver.value(); }
                }
                """),
            java.util.Map.entry("q/ProtectedLeaf.java", "package q; public class ProtectedLeaf extends ProtectedCaller { public int value(){return 91;} }"),
            java.util.Map.entry("Fixture.java", """
                public class Fixture {
                    public static p.Base make(int kind) {
                        if(kind==0) return new q.Outsider();
                        if(kind==1) return new p.Back();
                        if(kind==2) return new q.BridgeChild();
                        if(kind==3) return new q.NoBridgeChild();
                        if(kind==4) return new r.Leaf();
                        if(kind==5) return new q.ProtectedCaller();
                        if(kind==6) return new q.ProtectedLeaf();
                        return new p.Base();
                    }
                    public static int baseSlot(p.Base value) { return value.invokeHidden(); }
                    public static int otherSlot() { q.Outsider value=new q.NoBridgeChild(); return value.hidden(); }
                }
                """));
        var arguments=new java.util.ArrayList<String>(java.util.List.of("-g","-d",directory.toString()));
        for(var source:sources.entrySet()) {
            Path file=directory.resolve(source.getKey()); Files.createDirectories(file.getParent());
            Files.writeString(file,source.getValue()); arguments.add(file.toString());
        }
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,arguments.toArray(String[]::new)));
    }

    private String linkAccessFixtures() throws Exception {
        return linkJar("Fixture", "r/Leaf", "q/NoBridgeChild", "q/BridgeChild", "p/Bridge", "p/Back",
                "q/Outsider", "q/ProtectedLeaf", "q/ProtectedCaller", "p/PackageCaller", "p/Base");
    }

    @Test void executesPackageSlotsAcrossPackagesAndTransitiveOverrides() throws Exception {
        compileAccessFixtures();
        Path oracle=directory.resolve("Oracle.java");
        Files.writeString(oracle,"""
            public class Oracle {
                public static void main(String[] args) {
                    for(int kind=0;kind<8;kind++) {
                        p.Base value=Fixture.make(kind);
                        System.out.println(Fixture.baseSlot(value));
                        System.out.println(p.PackageCaller.hidden(value));
                    }
                }
            }
            """);
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-cp",directory.toString(),
                "-d",directory.toString(),oracle.toString()));
        run("java","-cp",directory.toString(),"Oracle");
        var results=Files.readAllLines(directory.resolve("command.log")).stream().map(Integer::parseInt)
                .map(String::valueOf).toList();
        assertEquals(16,results.size());
        execute(linkAccessFixtures(), "const expected=["+String.join(",",results)+"];\n"+"""
            for(let kind=0;kind<8;kind++) {
                const value=e['Fixture.make'](kind);
                assert.equal(e['Fixture.baseSlot'](value),expected[kind*2]);
                assert.equal(e['p.PackageCaller.hidden'](value),expected[kind*2+1]);
            }
            assert.equal(e['Fixture.otherSlot'](),50);
            assert.equal(e['p.PackageCaller.protectedCall'](e['Fixture.make'](0)),40);
            assert.equal(e['p.PackageCaller.protectedCall'](e['Fixture.make'](7)),2);
            assert.equal(e['q.ProtectedCaller.call'](e['Fixture.make'](5)),90);
            assert.equal(e['q.ProtectedCaller.call'](e['Fixture.make'](6)),91);
            """);
    }

    @Test void restrictsCrossPackageProtectedReceivers() throws Exception {
        compileAccessFixtures();
        ClassNode caller=new ClassNode();
        Path file=directory.resolve("q/ProtectedCaller.class");
        new ClassReader(Files.readAllBytes(file)).accept(caller,0);
        MethodNode method=caller.methods.stream().filter(m -> m.name.equals("call")).findFirst().orElseThrow();
        // Name the superclass symbol explicitly to expose the JVM receiver restriction.
        method.desc="(Lp/Base;)I";
        for(AbstractInsnNode instruction:method.instructions)
            if(instruction instanceof MethodInsnNode call && call.name.equals("value")) call.owner="p/Base";
        ClassWriter writer=new ClassWriter(0); caller.accept(writer); Files.write(file,writer.toByteArray());
        execute(linkAccessFixtures(), """
            assert.equal(e['q.ProtectedCaller.call'](e['Fixture.make'](5)),90);
            assert.equal(e['q.ProtectedCaller.call'](e['Fixture.make'](6)),91);
            for(const kind of [0,1,2,3,4,7])
                assert.throws(()=>e['q.ProtectedCaller.call'](e['Fixture.make'](kind)),WebAssembly.RuntimeError);
            assert.throws(()=>e['q.ProtectedCaller.call'](0),WebAssembly.RuntimeError);
            """);
    }

    @Test void diagnosesInaccessibleProtectedAndPackageVirtualCalls() throws Exception {
        compileAccessFixtures();
        byte[] bytes=Files.readAllBytes(directory.resolve("Fixture.class"));
        for(String methodName:new String[]{"hidden", "value"}) {
            ClassNode caller=new ClassNode(); new ClassReader(bytes).accept(caller,0);
            MethodNode method=caller.methods.stream().filter(m -> m.name.equals("baseSlot")).findFirst().orElseThrow();
            for(AbstractInsnNode instruction:method.instructions)
                if(instruction instanceof MethodInsnNode call && call.name.equals("invokeHidden")) call.name=methodName;
            ClassWriter writer=new ClassWriter(0); caller.accept(writer);
            Files.write(directory.resolve("Fixture.class"),writer.toByteArray());
            var error=assertThrows(IllegalArgumentException.class, this::linkAccessFixtures);
            assertTrue(error.getMessage().contains("Fixture.java:"),error.getMessage());
            assertTrue(error.getMessage().contains("Fixture.baseSlot(Lp/Base;)I"),error.getMessage());
            assertTrue(error.getMessage().contains("inaccessible protected/package"),error.getMessage());
        }
    }

    @Test void executesLongArithmeticLocalsJoinsConversionsAndCalls() throws Exception {
        execute(BytecodeCompiler.compileClass(fixture("""
            public class Fixture {
                private static long identity(long n) { return n; }
                private static int identity(int n) { return n; }
                public static long mixed(int a,long b,int c,long d) { return identity(b)+a+identity(c)+d; }
                public static long constant() { return -9007199254740993L; }
                public static long and(long a,long b) { return a&b; }
                public static long arithmetic(long a,long b) { return (a+b)*(a-b)^(-a|b); }
                public static long shifts(long a,int b) { return (a<<b)^(a>>b)^(a>>>b); }
                public static long divide(long a,long b) { return a/b; }
                public static long remainder(long a,long b) { return a%b; }
                public static int compare(long a,long b) { return a<b?-1:a==b?0:1; }
                public static long joined(boolean flag,long n) { return 100L+(flag?n:7L); }
                public static long sum(long n) { long result=0; for(long i=0;i<n;i++) result+=i; return result; }
                public static long widen(int n) { return n; }
                public static int narrow(long n) { return (int)n; }
                public static int reuse(long n) {
                    int result;
                    { long value=n+1; result=(int)value; }
                    { int value=3; result+=value; }
                    return result;
                }
            }
            """)), """
            const signed=n=>BigInt.asIntN(64,n);
            const values=[-9223372036854775808n,-9007199254740993n,-7n,-1n,0n,1n,7n,9007199254740993n,9223372036854775807n];
            for(const a of values) {
                for(const b of values) {
                    assert.equal(e['Fixture.mixed'](-3,a,7,b),signed(a+b+4n));
                    assert.equal(e['Fixture.arithmetic'](a,b),signed((a+b)*(a-b)^(-a|b)));
                    assert.equal(e['Fixture.and'](a,b),a&b);
                    assert.equal(e['Fixture.compare'](a,b),a<b?-1:a===b?0:1);
                    if(b!==0n) {
                        assert.equal(e['Fixture.divide'](a,b),signed(a/b));
                        assert.equal(e['Fixture.remainder'](a,b),a%b);
                    }
                }
                for(const distance of [-129,-65,-64,-1,0,1,31,32,63,64,65,129]) {
                    const b=BigInt(distance&63);
                    assert.equal(e['Fixture.shifts'](a,distance),signed((a<<b)^(a>>b)^(BigInt.asUintN(64,a)>>b)));
                }
                assert.equal(e['Fixture.joined'](1,a),signed(100n+a));
                assert.equal(e['Fixture.joined'](0,a),107n);
                assert.equal(e['Fixture.narrow'](a),Number(BigInt.asIntN(32,a)));
                assert.equal(e['Fixture.reuse'](a),Number(BigInt.asIntN(32,a+4n)));
            }
            for(let n=0n;n<30n;n++) assert.equal(e['Fixture.sum'](n),n*(n-1n)/2n);
            assert.equal(e['Fixture.widen'](-2147483648),-2147483648n);
            assert.equal(e['Fixture.constant'](),-9007199254740993n);
            assert.throws(()=>e['Fixture.divide'](1n,0n),WebAssembly.RuntimeError);
            assert.throws(()=>e['Fixture.remainder'](1n,0n),WebAssembly.RuntimeError);
            """);
    }

    @Test void executesLongFieldsAndMixedWidthVirtualInterfaceCalls() throws Exception {
        fixture("""
            public class Fixture {
                static long stored;
                public static long run(long n) {
                    Child value=new Child(n);
                    value.amount+=5;
                    stored=value.amount++;
                    return value.twice(2,n,3)+stored+value.before+value.after;
                }
                public static long bump() { return stored++; }
                private static long side() { return stored; }
                public static long discard() { side(); return 1; }
                public static long defaults() { Blank b=new Blank(); return b.a+b.b+b.c+b.d; }
            }
            interface Operation {
                long add(int first,long n,int last);
                default long twice(int first,long n,int last) { return add(first,n,last)+add(first,n,last); }
            }
            class Parent implements Operation {
                int before;
                long amount;
                int after;
                Parent(int first,long n,int last) { before=first; amount=n; after=last; }
                public long add(int first,long n,int last) { return amount+first+n+last; }
            }
            class Child extends Parent {
                long amount;
                Child(long n) { super(7,n,9); amount=n*2; }
                public long add(int first,long n,int last) { return super.add(first,n,last)+amount; }
            }
            class Blank { int a; long b; int c; long d; }
            """);
        Path oracle=directory.resolve("LongOracle.java");
        Files.writeString(oracle,"""
            public class LongOracle {
                public static void main(String[] args) {
                    for(long n:new long[]{-9007199254740993L,-3L,0L,5L,9007199254740993L,Long.MAX_VALUE})
                        System.out.println(Fixture.run(n));
                }
            }
            """);
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-cp",directory.toString(),
                "-d",directory.toString(),oracle.toString()));
        run("java","-cp",directory.toString(),"LongOracle");
        var expected=Files.readAllLines(directory.resolve("command.log")).stream().map(Long::parseLong)
                .map(value -> value+"n").toList();
        assertEquals(6,expected.size());
        execute(linkJar("Child", "Parent", "Operation", "Blank", "Fixture"),
                "const jvm=["+String.join(",",expected)+"];\n"+"""
            const values=[-9007199254740993n,-3n,0n,5n,9007199254740993n,9223372036854775807n];
            for(const [index,n] of values.entries()) {
                assert.equal(e['Fixture.run'](n),jvm[index]);
                assert.equal(e['Fixture.run'](n),BigInt.asIntN(64,10n*n+43n));
                assert.equal(e['Fixture.bump'](),BigInt.asIntN(64,2n*n+5n));
                assert.equal(e['Fixture.bump'](),BigInt.asIntN(64,2n*n+6n));
            }
            assert.equal(e['Fixture.defaults'](),0n);
            assert.equal(e['Fixture.discard'](),1n);
            """);
    }

    @Test void executesAllIntegralStackShuffleShapes() throws Exception {
        int[][] shapes={
            {Opcodes.DUP, 1, 0,0},
            {Opcodes.DUP_X1, 11, 1,0,1},
            {Opcodes.DUP_X2, 111, 2,0,1,2},
            {Opcodes.DUP_X2, 21, 1,0,1},
            {Opcodes.DUP2, 11, 0,1,0,1},
            {Opcodes.DUP2, 2, 0,0},
            {Opcodes.DUP2_X1, 111, 1,2,0,1,2},
            {Opcodes.DUP2_X1, 12, 1,0,1},
            {Opcodes.DUP2_X2, 1111, 2,3,0,1,2,3},
            {Opcodes.DUP2_X2, 211, 1,2,0,1,2},
            {Opcodes.DUP2_X2, 112, 2,0,1,2},
            {Opcodes.DUP2_X2, 22, 1,0,1},
            {Opcodes.SWAP, 11, 1,0}
        };
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,"Shuffle",null,"java/lang/Object",null);
        StringBuilder assertions=new StringBuilder();
        for(int index=0;index<shapes.length;index++) {
            int[] shape=shapes[index];
            String categories=Integer.toString(shape[1]);
            MethodVisitor method=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"shape"+index,"()J",null,null);
            method.visitCode();
            for(int i=0;i<categories.length();i++) method.visitLdcInsn(categories.charAt(i)=='2' ? (Object)Long.valueOf(i+1) : Integer.valueOf(i+1));
            method.visitInsn(shape[0]);
            for(int i=shape.length-1;i>=2;i--)
                method.visitVarInsn(categories.charAt(shape[i])=='2'?Opcodes.LSTORE:Opcodes.ISTORE,(i-2)*2);
            method.visitInsn(Opcodes.LCONST_0);
            long expected=0;
            for(int i=2;i<shape.length;i++) {
                method.visitLdcInsn(10L); method.visitInsn(Opcodes.LMUL);
                boolean wide=categories.charAt(shape[i])=='2';
                method.visitVarInsn(wide?Opcodes.LLOAD:Opcodes.ILOAD,(i-2)*2);
                if(!wide) method.visitInsn(Opcodes.I2L);
                method.visitInsn(Opcodes.LADD);
                expected=expected*10+shape[i]+1;
            }
            method.visitInsn(Opcodes.LRETURN); method.visitMaxs(0,0); method.visitEnd();
            assertions.append("assert.equal(e['Shuffle.shape").append(index).append("'](),").append(expected).append("n);\n");
        }
        writer.visitField(Opcodes.ACC_STATIC|Opcodes.ACC_FINAL,"MIN","J",null,Long.MIN_VALUE).visitEnd();
        MethodVisitor constant=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"constant","()J",null,null);
        constant.visitCode(); constant.visitFieldInsn(Opcodes.GETSTATIC,"Shuffle","MIN","J");
        constant.visitInsn(Opcodes.LRETURN); constant.visitMaxs(0,0); constant.visitEnd();
        assertions.append("assert.equal(e['Shuffle.constant'](),-9223372036854775808n);\n");
        for(boolean wide:new boolean[]{false,true}) {
            String name=wide?"popWide":"popPair";
            MethodVisitor method=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,name,"()J",null,null);
            method.visitCode(); method.visitLdcInsn(9L);
            if(wide) method.visitLdcInsn(7L);
            else { method.visitInsn(Opcodes.ICONST_2); method.visitInsn(Opcodes.ICONST_3); }
            method.visitInsn(Opcodes.POP2); method.visitInsn(Opcodes.LRETURN);
            method.visitMaxs(0,0); method.visitEnd();
            assertions.append("assert.equal(e['Shuffle.").append(name).append("'](),9n);\n");
        }
        writer.visitEnd();
        execute(BytecodeCompiler.compileClass(writer.toByteArray()),assertions.toString());
    }

    @Test void executesFloatingArithmeticComparisonsCastsAndJoins() throws Exception {
        execute(BytecodeCompiler.compileClass(fixture("""
            public class Fixture {
                double blankD; float blankF;
                public static double defaults() { Fixture f=new Fixture(); return f.blankD+f.blankF; }
                public static double arithmetic(double a,double b) { return (a+b)*(a-b)/(a*b); }
                public static float arithmeticF(float a,float b) { return (a+b)*(a-b)/(a*b); }
                public static double negate(double a) { return -a; }
                public static float negateF(float a) { return -a; }
                public static int relations(double a,double b) {
                    int r=0; if(a<b) r+=1; if(a>b) r+=2; if(a==b) r+=4;
                    if(a<=b) r+=8; if(a>=b) r+=16; if(a!=b) r+=32; return r;
                }
                public static int relationsF(float a,float b) {
                    int r=0; if(a<b) r+=1; if(a>b) r+=2; if(a==b) r+=4;
                    if(a<=b) r+=8; if(a>=b) r+=16; if(a!=b) r+=32; return r;
                }
                public static int toInt(double n) { return (int)n; }
                public static long toLong(double n) { return (long)n; }
                public static int floatInt(float n) { return (int)n; }
                public static long floatLong(float n) { return (long)n; }
                public static float intFloat(int n) { return n; }
                public static double intDouble(int n) { return n; }
                public static float narrow(double n) { return (float)n; }
                public static double widen(float n) { return n; }
                public static double joined(boolean b,double n) { return 100.0+(b?n:0.5); }
                public static double loop(int n) { double r=0; for(int i=0;i<n;i++) r+=0.125; return r; }
                public static float reuse(double n) {
                    float result; {double v=n+0.5; result=(float)v;} {float v=0.25f;result+=v;} return result;
                }
            }
            """)), """
            const values=[NaN,Infinity,-Infinity,-0,0,Number.MIN_VALUE,-Number.MIN_VALUE,1e-40,-1.5,0.1,2147483647,2147483648,-2147483649,9223372036854774784,-9223372036854774784,2**63,-(2**63),3.4028234663852886e38,Number.MAX_VALUE];
            const f=Math.fround;
            const relations=(a,b)=>(a<b?1:0)+(a>b?2:0)+(a===b?4:0)+(a<=b?8:0)+(a>=b?16:0)+(a!==b?32:0);
            const int=n=>Number.isNaN(n)?0:Math.max(-2147483648,Math.min(2147483647,Math.trunc(n)))||0;
            const long=n=>Number.isNaN(n)?0n:n>=2**63?9223372036854775807n:n<=-(2**63)?-9223372036854775808n:BigInt(Math.trunc(n));
            for(const a of values) {
                for(const b of values) {
                    assert.equal(e['Fixture.arithmetic'](a,b),(a+b)*(a-b)/(a*b));
                    assert.equal(e['Fixture.arithmeticF'](a,b),f(f(f(f(a)+f(b))*f(f(a)-f(b)))/f(f(a)*f(b))));
                    assert.equal(e['Fixture.relations'](a,b),relations(a,b));
                    assert.equal(e['Fixture.relationsF'](a,b),relations(f(a),f(b)));
                }
                assert.equal(e['Fixture.negate'](a),-a);
                assert.equal(e['Fixture.negateF'](a),f(-f(a)));
                assert.equal(e['Fixture.toInt'](a),int(a));
                assert.equal(e['Fixture.toLong'](a),long(a));
                assert.equal(e['Fixture.floatInt'](a),int(f(a)));
                assert.equal(e['Fixture.floatLong'](a),long(f(a)));
                assert.equal(e['Fixture.narrow'](a),f(a));
                assert.equal(e['Fixture.widen'](a),f(a));
                assert.equal(e['Fixture.joined'](1,a),100+a);
                assert.equal(e['Fixture.joined'](0,a),100.5);
                assert.equal(e['Fixture.reuse'](a),f(f(a+0.5)+0.25));
            }
            for(const a of [-2147483648,-16777217,-1,0,16777217,2147483647]) {
                assert.equal(e['Fixture.intFloat'](a),f(a));
                assert.equal(e['Fixture.intDouble'](a),a);
            }
            for(let i=0;i<30;i++) assert.equal(e['Fixture.loop'](i),i*0.125);
            assert.equal(e['Fixture.defaults'](),0);
            """);
    }

    @Test void matchesJvmFloatingFieldsMixedCallsAndLongConversions() throws Exception {
        fixture("""
            public class Fixture {
                static double savedD;
                static float savedF;
                public static double run(double n,float f,int i,long l) {
                    Child c=new Child(n,f);
                    savedD=c.amount++;
                    savedF=c.small++;
                    return c.twice(i,f,n,l)+savedD+savedF+c.before+c.after;
                }
                public static float longFloat(long n) { return n; }
                public static double longDouble(long n) { return n; }
                public static double savedDouble() { return savedD; }
                public static float savedFloat() { return savedF; }
            }
            interface Operation {
                double apply(int i,float f,double d,long l);
                default double twice(int i,float f,double d,long l) { return apply(i,f,d,l)+apply(i,f,d,l); }
            }
            class Parent implements Operation {
                int before; double amount; float small; int after;
                Parent(int i,double n,float f,int j) { before=i;amount=n;small=f;after=j; }
                public double apply(int i,float f,double d,long l) { return amount+small+i+f+d+l; }
            }
            class Child extends Parent {
                double amount; float small;
                Child(double n,float f) { super(7,n,f,9); amount=n*2;small=f*2; }
                public double apply(int i,float f,double d,long l) { return super.apply(i,f,d,l)+amount+small; }
            }
            """);
        Path oracle=directory.resolve("FloatOracle.java");
        Files.writeString(oracle,"""
            public class FloatOracle {
                public static void main(String[] args) {
                    double[] numbers={-0.0,0.1,1e20,-3.5,Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NaN};
                    float[] floats={0.0f,0.1f,-3.5f,Float.MIN_VALUE,Float.MAX_VALUE,Float.NEGATIVE_INFINITY,Float.NaN};
                    for(int i=0;i<numbers.length;i++) System.out.println(Fixture.run(numbers[i],floats[i],2,9007199254740993L));
                    for(long n:new long[]{Long.MIN_VALUE,-9007199254740993L,16777217L,Long.MAX_VALUE,4611686293305294849L}) {
                        System.out.println((double)Fixture.longFloat(n));
                        System.out.println(Fixture.longDouble(n));
                    }
                }
            }
            """);
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-cp",directory.toString(),
                "-d",directory.toString(),oracle.toString()));
        run("java","-cp",directory.toString(),"FloatOracle");
        var expected=Files.readAllLines(directory.resolve("command.log"));
        assertEquals(17,expected.size());
        // Java's numeric spellings (including NaN/Infinity) are valid JS numeric expressions.
        execute(linkJar("Child", "Parent", "Operation", "Fixture"),
                "const jvm=["+String.join(",",expected)+"];\n"+"""
            const numbers=[-0,0.1,1e20,-3.5,Number.MAX_VALUE,Infinity,NaN];
            const floats=[0,0.1,-3.5,2**-149,3.4028234663852886e38,-Infinity,NaN];
            for(let i=0;i<numbers.length;i++) {
                assert.equal(e['Fixture.run'](numbers[i],floats[i],2,9007199254740993n),jvm[i]);
                assert.equal(e['Fixture.savedDouble'](),numbers[i]*2);
                assert.equal(e['Fixture.savedFloat'](),Math.fround(Math.fround(floats[i])*2));
            }
            const longs=[-9223372036854775808n,-9007199254740993n,16777217n,9223372036854775807n,4611686293305294849n];
            for(let i=0;i<longs.length;i++) {
                assert.equal(e['Fixture.longFloat'](longs[i]),jvm[7+i*2]);
                assert.equal(e['Fixture.longDouble'](longs[i]),jvm[8+i*2]);
            }
            """);
    }

    @Test void executesFloatingConstantsAndStackShuffles() throws Exception {
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,"Floating",null,"java/lang/Object",null);
        Object[] values={Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,-0.0f,Float.MIN_VALUE,Float.MAX_VALUE,
                Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,-0.0,Double.MIN_VALUE,Double.MAX_VALUE};
        StringBuilder assertions=new StringBuilder();
        for(int i=0;i<values.length;i++) {
            Object value=values[i]; boolean wide=value instanceof Double;
            String descriptor=wide?"D":"F";
            writer.visitField(Opcodes.ACC_STATIC|Opcodes.ACC_FINAL,"field"+i,descriptor,null,value).visitEnd();
            for(boolean field:new boolean[]{false,true}) {
                String name=(field?"field":"literal")+i;
                MethodVisitor method=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,name,"()"+descriptor,null,null);
                method.visitCode();
                if(field) method.visitFieldInsn(Opcodes.GETSTATIC,"Floating","field"+i,descriptor);
                else method.visitLdcInsn(value);
                method.visitInsn(wide?Opcodes.DRETURN:Opcodes.FRETURN); method.visitMaxs(0,0); method.visitEnd();
                String expected=Double.toString(((Number)value).doubleValue());
                assertions.append("assert.equal(e['Floating.").append(name).append("'](),").append(expected).append(");\n");
            }
        }
        String[] categories={"D","FF","FD","DF","DD","FFD"};
        int[] opcodes={Opcodes.DUP2,Opcodes.DUP2,Opcodes.DUP2_X1,Opcodes.DUP_X2,Opcodes.DUP2_X2,Opcodes.DUP2_X2};
        int[][] outputs={{0,0},{0,1,0,1},{1,0,1},{1,0,1},{1,0,1},{2,0,1,2}};
        for(int i=0;i<categories.length;i++) {
            String types=categories[i]; int[] output=outputs[i];
            MethodVisitor method=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"shuffle"+i,"()D",null,null);
            method.visitCode();
            for(int j=0;j<types.length();j++) method.visitLdcInsn(types.charAt(j)=='D'?(Object)Double.valueOf(j+1):Float.valueOf(j+1));
            method.visitInsn(opcodes[i]);
            for(int j=output.length-1;j>=0;j--) method.visitVarInsn(types.charAt(output[j])=='D'?Opcodes.DSTORE:Opcodes.FSTORE,j*2);
            method.visitInsn(Opcodes.DCONST_0); int expected=0;
            for(int j=0;j<output.length;j++) {
                method.visitLdcInsn(10.0); method.visitInsn(Opcodes.DMUL);
                boolean wide=types.charAt(output[j])=='D';
                method.visitVarInsn(wide?Opcodes.DLOAD:Opcodes.FLOAD,j*2);
                if(!wide) method.visitInsn(Opcodes.F2D);
                method.visitInsn(Opcodes.DADD); expected=expected*10+output[j]+1;
            }
            method.visitInsn(Opcodes.DRETURN); method.visitMaxs(0,0); method.visitEnd();
            assertions.append("assert.equal(e['Floating.shuffle").append(i).append("'](),").append(expected).append(");\n");
        }
        writer.visitEnd(); execute(BytecodeCompiler.compileClass(writer.toByteArray()),assertions.toString());
    }

    @Test void matchesJvmFloatingRemaindersAcrossRawBitVectors() throws Exception {
        byte[] bytes=fixture("""
            public class Fixture {
                public static double rem(double a,double b) { return a%b; }
                public static float remF(float a,float b) { return a%b; }
                public static double joined(double a,double b,boolean flag) { return 100+(flag?a%b:b%a); }
                public static double chained(double a,double b,double c) { return (a%b)%c; }
            }
            """);
        Path oracle=directory.resolve("RemainderOracle.java");
        Files.writeString(oracle,"""
            public class RemainderOracle {
                static void check(double a,double b) {
                    System.out.println("D "+Long.toUnsignedString(Double.doubleToRawLongBits(a))+" "
                        +Long.toUnsignedString(Double.doubleToRawLongBits(b))+" "
                        +Long.toUnsignedString(Double.doubleToLongBits(Fixture.rem(a,b))));
                }
                static void check(float a,float b) {
                    System.out.println("F "+Integer.toUnsignedString(Float.floatToRawIntBits(a))+" "
                        +Integer.toUnsignedString(Float.floatToRawIntBits(b))+" "
                        +Integer.toUnsignedString(Float.floatToIntBits(Fixture.remF(a,b))));
                }
                public static void main(String[] args) {
                    double[] doubles={0,-0.0,1,-1,3,-3,5.5,-5.5,Double.MIN_VALUE,-Double.MIN_VALUE,
                        Double.MIN_NORMAL,Math.nextDown(Double.MIN_NORMAL),Double.MAX_VALUE,-Double.MAX_VALUE,
                        Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN,2.25,-2.25,0.1};
                    float[] floats={0,-0.0f,1,-1,3,-3,5.5f,-5.5f,Float.MIN_VALUE,-Float.MIN_VALUE,
                        Float.MIN_NORMAL,Math.nextDown(Float.MIN_NORMAL),Float.MAX_VALUE,-Float.MAX_VALUE,
                        Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NaN,2.25f,-2.25f,0.1f};
                    for(double a:doubles) for(double b:doubles) check(a,b);
                    for(float a:floats) for(float b:floats) check(a,b);
                    java.util.Random random=new java.util.Random(230947L);
                    for(int i=0;i<1024;i++) {
                        check(Double.longBitsToDouble(random.nextLong()),Double.longBitsToDouble(random.nextLong()));
                        check(Float.intBitsToFloat(random.nextInt()),Float.intBitsToFloat(random.nextInt()));
                    }
                }
            }
            """);
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-cp",directory.toString(),
                "-d",directory.toString(),oracle.toString()));
        run("java","-cp",directory.toString(),"RemainderOracle");
        Files.copy(directory.resolve("command.log"),directory.resolve("vectors.txt"));
        String wat=BytecodeCompiler.compileClass(bytes);
        execute(wat, """
            const view=new DataView(new ArrayBuffer(8));
            let count=0;
            for(const line of fs.readFileSync(__dirname+'/vectors.txt','utf8').trim().split('\\n')) {
                const [type,x,y,expected]=line.split(' ');
                const wide=type==='D';
                const decode=bits=>{
                    if(wide) {view.setBigUint64(0,BigInt(bits));return view.getFloat64(0);}
                    view.setUint32(0,Number(bits));return view.getFloat32(0);
                };
                const a=decode(x), b=decode(y);
                const actual=e[wide?'Fixture.rem':'Fixture.remF'](a,b);
                const wanted=decode(expected);
                if(Number.isNaN(wanted)) assert.ok(Number.isNaN(actual),line);
                else {
                    if(wide) {view.setFloat64(0,actual);assert.equal(view.getBigUint64(0),BigInt(expected),line);}
                    else {view.setFloat32(0,actual);assert.equal(view.getUint32(0),Number(expected),line);}
                }
                if(wide) {
                    assert.equal(e['Fixture.joined'](a,b,1),100+a%b);
                    assert.equal(e['Fixture.joined'](a,b,0),100+b%a);
                    assert.equal(e['Fixture.chained'](a,b,2.25),(a%b)%2.25);
                }
                count++;
            }
            assert.equal(count,2848);
            """);
        assertTrue(wat.contains("(func $__java_f64_rem "));
    }

    @Test void emitsOneFloatingRemainderRuntimePerLinkedModule() throws Exception {
        String ordinary=BytecodeCompiler.compileClass(fixture("public class Fixture {public static double add(double a,double b){return a+b;}}"));
        assertFalse(ordinary.contains("(func $__java_f64_rem "));
        fixture("""
            public class Fixture {
                public static double rem(double a,double b) { return Helper.rem(a,b)+a%b; }
                public static float remF(float a,float b) { return Helper.remF(a,b)+a%b; }
            }
            class Helper {
                static double rem(double a,double b) { return a%b; }
                static float remF(float a,float b) { return a%b; }
            }
            """);
        String wat=linkJar("Helper", "Fixture");
        for(String name:new String[]{"f32","f64"}) {
            String marker="(func $__java_"+name+"_rem ";
            assertTrue(wat.indexOf(marker)>=0);
            assertEquals(wat.indexOf(marker),wat.lastIndexOf(marker));
        }
        execute(wat,"""
            assert.equal(e['Fixture.rem'](5.5,2),3);
            assert.equal(e['Fixture.remF'](-5.5,2),-3);
            assert.ok(Number.isNaN(e['Fixture.rem'](1,0)));
            """);
    }

    @Test void executesLinkedClassAndInterfaceTypeTests() throws Exception {
        fixture("""
            public class Fixture {
                public static Object make(int n) {
                    if(n==0) return null;
                    if(n==1) return new Child();
                    if(n==2) return new Other();
                    return new Plain();
                }
                public static int classify(Object value) {
                    return (value instanceof Object?1:0)+(value instanceof Base?2:0)
                        +(value instanceof Child?4:0)+(value instanceof Root?8:0)
                        +(value instanceof Leaf?16:0)+(value instanceof Other?32:0)
                        +(value instanceof Empty?64:0);
                }
                public static int castClass(Object value) { return ((Base)value).value; }
                public static int castInterface(Object value) { return ((Root)value).get(); }
                public static Object castNull(Object value) { return (Child)value; }
                public static int joined(Object value,boolean flag) {
                    Base base=flag?(Base)value:new Child(); return base.value;
                }
                public static int nested(Object value) { return ((Root)(Base)value).get()+7; }
            }
            interface Empty {}
            interface Root { int get(); }
            interface Leaf extends Root {}
            abstract class Base implements Leaf { int value; Base(){value=23;} }
            class Child extends Base { public int get(){return value+1;} }
            class Other implements Root { public int get(){return 99;} }
            class Plain {}
            """);
        execute(linkJar("Empty","Plain","Other","Child","Base","Leaf","Root","Fixture"),"""
            const nil=e['Fixture.make'](0),child=e['Fixture.make'](1),other=e['Fixture.make'](2),plain=e['Fixture.make'](3);
            assert.equal(e['Fixture.classify'](nil),0);
            assert.equal(e['Fixture.classify'](child),31);
            assert.equal(e['Fixture.classify'](other),41);
            assert.equal(e['Fixture.classify'](plain),1);
            assert.equal(e['Fixture.castClass'](child),23);
            assert.equal(e['Fixture.castInterface'](child),24);
            assert.equal(e['Fixture.castInterface'](other),99);
            assert.equal(e['Fixture.castNull'](nil),0);
            assert.equal(e['Fixture.castNull'](child),child);
            assert.equal(e['Fixture.joined'](child,1),23);
            assert.equal(e['Fixture.joined'](other,0),23);
            assert.equal(e['Fixture.nested'](child),31);
            assert.throws(()=>e['Fixture.castNull'](other),WebAssembly.RuntimeError);
            assert.throws(()=>e['Fixture.castClass'](other),WebAssembly.RuntimeError);
            assert.throws(()=>e['Fixture.castInterface'](plain),WebAssembly.RuntimeError);
            assert.throws(()=>e['Fixture.castClass'](nil),WebAssembly.RuntimeError);
            """);
    }

    @Test void rejectsUnlinkedArrayInaccessibleAndIncompleteTypeTests() throws Exception {
        byte[] bytes=fixture("""
            public class Fixture {
                public static boolean test(Object value) { return value instanceof Fixture; }
            }
            """);
        for(String target:new String[]{"missing/Type","[[Lmissing/Type;","[Lmissing/Type;"}) {
            ClassNode type=new ClassNode(); new ClassReader(bytes).accept(type,0);
            for(MethodNode method:type.methods) for(AbstractInsnNode insn:method.instructions)
                if(insn instanceof TypeInsnNode test) test.desc=target;
            ClassWriter writer=new ClassWriter(0); type.accept(writer);
            String message=assertThrows(IllegalArgumentException.class,
                    ()->BytecodeCompiler.compileClass(writer.toByteArray())).getMessage();
            assertTrue(message.contains("type test "+target),message);
            assertTrue(message.contains("Fixture.java:"),message);
        }
        ClassNode caller=new ClassNode(); new ClassReader(bytes).accept(caller,0);
        ClassNode hidden=new ClassNode(); hidden.name="other/Hidden"; hidden.superName="java/lang/Object";
        var linked=new java.util.LinkedHashMap<String,ClassNode>();
        linked.put(caller.name,caller); linked.put(hidden.name,hidden);
        assertTrue(assertThrows(IllegalArgumentException.class,
                ()->new BytecodeDispatch(linked,caller.name).typeTestIds(hidden.name))
                .getMessage().contains("inaccessible"));
        hidden.access=Opcodes.ACC_PUBLIC; hidden.interfaces.add("missing/Interface");
        assertTrue(assertThrows(IllegalArgumentException.class,
                ()->new BytecodeDispatch(linked,caller.name).typeTestIds(caller.name))
                .getMessage().contains("complete acyclic"));
        hidden.interfaces.clear(); hidden.superName=hidden.name;
        assertTrue(assertThrows(IllegalArgumentException.class,
                ()->new BytecodeDispatch(linked,caller.name).typeTestIds(caller.name))
                .getMessage().contains("complete acyclic"));
    }

    @Test void executesInheritedStaticFieldsAndProtectedFieldReceivers() throws Exception {
        var sources=java.util.Map.of(
            "p/Base.java","""
                package p;
                public class Base {
                    protected static long shared;
                    protected int value;
                    public final int fixed;
                    public Base(){value=17;fixed=9;}
                    public int hidden;
                }
                """,
            "q/Sub.java","""
                package q;
                public class Sub extends p.Base {
                    public long hidden;
                    public static int read(Sub value){return value.value;}
                    public static void write(Sub value,int n){value.value=n;}
                    public static long sharedRead(){return Sub.shared;}
                    public static void sharedWrite(long n){Sub.shared=n;}
                }
                """,
            "q/Leaf.java","package q; public class Leaf extends Sub {}",
            "Fixture.java","""
                public class Fixture {
                    private static int secret;
                    private int own;
                    public static p.Base make(int n){return n==0?new p.Base():(n==1?new q.Sub():new q.Leaf());}
                    public static int fixed(p.Base b){return b.fixed;}
                    public static int hidden(q.Sub s){s.hidden=41;((p.Base)s).hidden=3;return (int)s.hidden+(int)s.hidden+((p.Base)s).hidden;}
                    public static int nested(){Fixture f=new Fixture();Nest.write(f);return secret+f.own;}
                    static class Nest {static void write(Fixture f){secret=20;f.own=22;}}
                }
                """);
        var args=new java.util.ArrayList<String>(java.util.List.of("-g","-d",directory.toString()));
        for(var source:sources.entrySet()) {
            Path file=directory.resolve(source.getKey());Files.createDirectories(file.getParent());
            Files.writeString(file,source.getValue());args.add(file.toString());
        }
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,args.toArray(String[]::new)));
        execute(linkJar("q/Leaf","Fixture$Nest","q/Sub","Fixture","p/Base"),"""
            const base=e['Fixture.make'](0),sub=e['Fixture.make'](1),leaf=e['Fixture.make'](2);
            assert.equal(e['q.Sub.read'](sub),17);
            assert.equal(e['q.Sub.read'](leaf),17);
            e['q.Sub.write'](leaf,123);
            assert.equal(e['q.Sub.read'](leaf),123);
            assert.equal(e['q.Sub.read'](sub),17);
            assert.throws(()=>e['q.Sub.read'](base),WebAssembly.RuntimeError);
            assert.throws(()=>e['q.Sub.write'](base,7),WebAssembly.RuntimeError);
            assert.throws(()=>e['q.Sub.read'](0),WebAssembly.RuntimeError);
            e['q.Sub.sharedWrite'](9007199254740993n);
            assert.equal(e['q.Sub.sharedRead'](),9007199254740993n);
            assert.equal(e['Fixture.fixed'](sub),9);
            assert.equal(e['Fixture.hidden'](sub),85);
            assert.equal(e['Fixture.nested'](),42);
            """);
    }

    @Test void diagnosesInvalidFieldAccessAndFinalWrites() {
        for(int flags:new int[]{Opcodes.ACC_PRIVATE,0,Opcodes.ACC_PROTECTED,
                Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,Opcodes.ACC_PUBLIC|Opcodes.ACC_FINAL}) {
            ClassNode base=new ClassNode();base.name="p/Base";base.superName="java/lang/Object";base.access=Opcodes.ACC_PUBLIC;
            base.fields.add(new FieldNode(flags,"value","I",null,null));
            ClassNode caller=new ClassNode();caller.name="q/Caller";caller.superName="java/lang/Object";caller.access=Opcodes.ACC_PUBLIC;
            MethodNode method=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"write","(Lp/Base;I)V",null,null);
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
            method.instructions.add(new VarInsnNode(Opcodes.ILOAD,1));
            method.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD,base.name,"value","I"));
            method.instructions.add(new InsnNode(Opcodes.RETURN));method.maxLocals=2;method.maxStack=2;caller.methods.add(method);
            var linked=java.util.Map.of(base.name,base,caller.name,caller);
            String error=assertThrows(IllegalArgumentException.class,
                    ()->new ClassBytecodeCompiler(caller).compileToWat(linked)).getMessage();
            assertTrue(error.contains("field access p/Base.value"),error);
            assertTrue(error.contains("q.Caller.write"),error);
        }
        ClassNode owner=new ClassNode();owner.name="Owner";owner.superName="java/lang/Object";
        owner.fields.add(new FieldNode(Opcodes.ACC_FINAL,"value","I",null,null));
        var dispatch=new BytecodeDispatch(java.util.Map.of(owner.name,owner),owner.name);
        var write=new FieldInsnNode(Opcodes.PUTFIELD,owner.name,"value","I");
        assertThrows(IllegalArgumentException.class,()->dispatch.validateField(write,
                new MethodNode(0,"ordinary","()V",null,null)));
        assertDoesNotThrow(()->dispatch.validateField(write,new MethodNode(0,"<init>","()V",null,null)));
        owner.fields.get(0).access|=Opcodes.ACC_STATIC;
        var staticWrite=new FieldInsnNode(Opcodes.PUTSTATIC,owner.name,"value","I");
        assertThrows(IllegalArgumentException.class,()->dispatch.validateField(staticWrite,
                new MethodNode(Opcodes.ACC_STATIC,"ordinary","()V",null,null)));
        assertDoesNotThrow(()->dispatch.validateField(staticWrite,
                new MethodNode(Opcodes.ACC_STATIC,"<clinit>","()V",null,null)));
    }

    @Test void resolvesInterfaceFieldsBeforeSuperclassFields() throws Exception {
        ClassNode iface=new ClassNode();iface.name="Values";iface.superName="java/lang/Object";
        iface.access=Opcodes.ACC_PUBLIC|Opcodes.ACC_INTERFACE|Opcodes.ACC_ABSTRACT;
        iface.fields.add(new FieldNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC|Opcodes.ACC_FINAL,"value","I",null,77));
        ClassNode parent=new ClassNode();parent.name="Parent";parent.superName="java/lang/Object";parent.access=Opcodes.ACC_PUBLIC;
        parent.fields.add(new FieldNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"value","I",null,11));
        ClassNode child=new ClassNode();child.name="Child";child.superName=parent.name;child.access=Opcodes.ACC_PUBLIC;
        child.interfaces.add(iface.name);
        MethodNode method=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"read","()I",null,null);
        method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,child.name,"value","I"));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));method.maxStack=1;child.methods.add(method);
        var bytes=new java.io.ByteArrayOutputStream();
        try(var jar=new java.util.jar.JarOutputStream(bytes)) {
            for(ClassNode type:new ClassNode[]{child,parent,iface}) {
                ClassWriter writer=new ClassWriter(0);type.version=Opcodes.V17;type.accept(writer);
                jar.putNextEntry(new java.util.jar.JarEntry(type.name+".class"));jar.write(writer.toByteArray());jar.closeEntry();
            }
        }
        execute(BytecodeCompiler.compileJar(new java.io.ByteArrayInputStream(bytes.toByteArray())),
                "assert.equal(e['Child.read'](),77);");
    }

    @Test void matchesJvmInheritedStaticCallsAndHiding() throws Exception {
        var sources=java.util.Map.of(
            "p/Base.java","""
                package p;
                public class Base {
                    public static double mixed(double a,long b,int c,float d){return a+b+c+d;}
                    public static int hidden(){return 10;}
                    protected static int protectedValue(){return 20;}
                    static int packageValue(){return 30;}
                }
                """,
            "p/Peer.java","package p; public class Peer {public static int read(){return Base.packageValue()+Base.protectedValue();}}",
            "q/Sub.java","""
                package q;
                public class Sub extends p.Base {
                    public static int hidden(){return 100;}
                    public static int protectedRead(){return Sub.protectedValue();}
                }
                """,
            "q/Leaf.java","package q; public class Leaf extends Sub {}",
            "Numbers.java","public interface Numbers {static double number(double n){return n*2;}}",
            "Fixture.java","""
                public class Fixture {
                    private static int counter;
                    private static int next(){return ++counter;}
                    public static double inherited(){return q.Leaf.mixed(0.5,9007199254740993L,7,0.25f);}
                    public static int hiding(){return p.Base.hidden()+q.Sub.hidden()+q.Leaf.hidden();}
                    public static int access(){return q.Sub.protectedRead()+p.Peer.read();}
                    public static double iface(){return Numbers.number(2.25);}
                    public static double nested(){counter=0;return q.Leaf.mixed(next(),next(),next(),next());}
                }
                """);
        var args=new java.util.ArrayList<String>(java.util.List.of("-g","-d",directory.toString()));
        for(var source:sources.entrySet()) {
            Path file=directory.resolve(source.getKey());Files.createDirectories(file.getParent());
            Files.writeString(file,source.getValue());args.add(file.toString());
        }
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,args.toArray(String[]::new)));
        Path oracle=directory.resolve("Oracle.java");
        Files.writeString(oracle,"""
            public class Oracle {public static void main(String[] args){
                System.out.println(Fixture.inherited());System.out.println(Fixture.hiding());
                System.out.println(Fixture.access());System.out.println(Fixture.iface());System.out.println(Fixture.nested());
            }}
            """);
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-cp",directory.toString(),
                "-d",directory.toString(),oracle.toString()));
        run("java","-cp",directory.toString(),"Oracle");
        var expected=Files.readAllLines(directory.resolve("command.log"));
        assertEquals(5,expected.size());
        execute(linkJar("q/Leaf","Numbers","Fixture","p/Peer","q/Sub","p/Base"),
                "const expected=["+String.join(",",expected)+"];\n"+"""
            for(const [i,name] of ['inherited','hiding','access','iface','nested'].entries())
                assert.equal(e['Fixture.'+name](),expected[i]);
            """);
    }

    @Test void diagnosesInvalidStaticMethodLinkage() {
        for(String scenario:new String[]{"private","package","protected","instance","native","abstract",
                "owner","kind","descriptor","interfaceInherited","missing","cycle","initializer"}) {
            ClassNode base=new ClassNode();base.name="p/Base";base.superName="java/lang/Object";base.access=Opcodes.ACC_PUBLIC;
            int flags=Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC;
            if(scenario.equals("private")) flags=Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC;
            if(scenario.equals("package")) flags=Opcodes.ACC_STATIC;
            if(scenario.equals("protected")) flags=Opcodes.ACC_PROTECTED|Opcodes.ACC_STATIC;
            if(scenario.equals("instance")) flags=Opcodes.ACC_PUBLIC;
            if(scenario.equals("native")) flags|=Opcodes.ACC_NATIVE;
            if(scenario.equals("abstract")) flags|=Opcodes.ACC_ABSTRACT;
            MethodNode target=integerDeclaration(flags);base.methods.add(target);
            ClassNode caller=new ClassNode();caller.name="q/Caller";caller.superName="java/lang/Object";caller.access=Opcodes.ACC_PUBLIC;
            MethodNode method=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"call","()I",null,null);
            MethodInsnNode call=new MethodInsnNode(Opcodes.INVOKESTATIC,base.name,"value","()I",false);
            method.instructions.add(call);method.instructions.add(new InsnNode(Opcodes.IRETURN));method.maxStack=2;caller.methods.add(method);
            var linked=new java.util.LinkedHashMap<String,ClassNode>();linked.put(base.name,base);linked.put(caller.name,caller);
            if(scenario.equals("owner")) base.access=0;
            if(scenario.equals("kind")) call.itf=true;
            if(scenario.equals("descriptor")) call.desc="(I)I";
            if(scenario.equals("missing")) {base.methods.clear();base.superName="missing/Parent";}
            if(scenario.equals("cycle")) {base.methods.clear();base.superName=base.name;}
            if(scenario.equals("initializer")) call.name="<clinit>";
            if(scenario.equals("interfaceInherited")) {
                base.access|=Opcodes.ACC_INTERFACE|Opcodes.ACC_ABSTRACT;
                ClassNode child=new ClassNode();child.name="p/Child";child.superName="java/lang/Object";
                child.access=Opcodes.ACC_PUBLIC|Opcodes.ACC_INTERFACE|Opcodes.ACC_ABSTRACT;child.interfaces.add(base.name);
                linked.put(child.name,child);call.owner=child.name;call.itf=true;
            }
            String error=assertThrows(IllegalArgumentException.class,
                    ()->new ClassBytecodeCompiler(caller).compileToWat(linked),scenario).getMessage();
            if (scenario.equals("abstract")) assertTrue(error.contains("incompatible abstract flags"), error);
            else if (scenario.equals("missing") || scenario.equals("cycle"))
                assertTrue(error.contains("hierarchy declaration"), error);
            else {
                assertTrue(error.contains("static call "),scenario+": "+error);
                assertTrue(error.contains("q.Caller.call()I"),error);
            }
        }
    }

    @Test void matchesJvmSpecialSuperclassSelectionAndInheritedDefaults() throws Exception {
        fixture("""
            public class Fixture {
                public static double run(int n) {
                    Leaf value=new Leaf();return value.direct(n)+value.inherited(n)+value.value();
                }
                public static int defaultCall(){return new DefaultChild().parent();}
            }
            class Base {
                protected int value(){return 10;}
                protected double mixed(int n,long big,float fraction,double tail){return n+big+fraction+tail;}
            }
            class Middle extends Base {protected int value(){return 20;}}
            class Leaf extends Middle {
                protected int value(){return 30;}
                public int direct(int n){return super.value()+n;}
                public double inherited(int n){return super.mixed(n,9007199254740993L,0.25f,0.5);}
            }
            interface DefaultValue {default int number(){return 73;}}
            class DefaultBase implements DefaultValue {}
            class DefaultChild extends DefaultBase {public int parent(){return super.number();}}
            """);
        // A legal grandparent symbolic reference still selects Middle under ACC_SUPER.
        Path leafFile=directory.resolve("Leaf.class");
        ClassNode leaf=new ClassNode();new ClassReader(Files.readAllBytes(leafFile)).accept(leaf,0);
        for(MethodNode method:leaf.methods) for(AbstractInsnNode insn:method.instructions)
            if(insn instanceof MethodInsnNode call && call.name.equals("value")) call.owner="Base";
        ClassWriter writer=new ClassWriter(0);leaf.accept(writer);Files.write(leafFile,writer.toByteArray());
        Path oracle=directory.resolve("Oracle.java");
        Files.writeString(oracle,"""
            public class Oracle {public static void main(String[] args){
                for(int n=-3;n<=3;n++)System.out.println(Fixture.run(n));
                System.out.println(Fixture.defaultCall());
            }}
            """);
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-cp",directory.toString(),
                "-d",directory.toString(),oracle.toString()));
        run("java","-cp",directory.toString(),"Oracle");
        var expected=Files.readAllLines(directory.resolve("command.log"));assertEquals(8,expected.size());
        execute(linkJar("DefaultChild","Leaf","Middle","DefaultBase","Base","DefaultValue","Fixture"),
                "const expected=["+String.join(",",expected)+"];\n"+"""
            for(let n=-3;n<=3;n++)assert.equal(e['Fixture.run'](n),expected[n+3]);
            assert.equal(e['Fixture.defaultCall'](),expected[7]);
            """);
        String wat=linkJar("Fixture","DefaultValue","Base","DefaultBase","Middle","Leaf","DefaultChild");
        execute(wat,"assert.equal(e['Fixture.defaultCall'](),73);");
    }

    @Test void diagnosesInvalidClassSpecialLinkage() {
        for(String scenario:new String[]{"private","package","protected","static","abstract","native",
                "owner","interface","unrelated","constructorInherited","missing","cycle","initializer"}) {
            ClassNode base=new ClassNode();base.name="p/Base";base.superName="java/lang/Object";base.access=Opcodes.ACC_PUBLIC;
            int flags=Opcodes.ACC_PUBLIC;
            if(scenario.equals("private"))flags=Opcodes.ACC_PRIVATE;
            if(scenario.equals("package"))flags=0;
            if(scenario.equals("protected"))flags=Opcodes.ACC_PROTECTED;
            if(scenario.equals("static"))flags|=Opcodes.ACC_STATIC;
            if(scenario.equals("abstract"))flags|=Opcodes.ACC_ABSTRACT;
            if(scenario.equals("native"))flags|=Opcodes.ACC_NATIVE;
            base.methods.add(integerDeclaration(flags));
            ClassNode caller=new ClassNode();caller.name="q/Caller";caller.superName=base.name;caller.access=Opcodes.ACC_PUBLIC|Opcodes.ACC_SUPER;
            MethodNode method=new MethodNode(Opcodes.ACC_PUBLIC,"call","()I",null,null);
            MethodInsnNode call=new MethodInsnNode(Opcodes.INVOKESPECIAL,base.name,"value","()I",false);
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));method.instructions.add(call);
            method.instructions.add(new InsnNode(Opcodes.IRETURN));method.maxLocals=1;method.maxStack=1;caller.methods.add(method);
            var linked=new java.util.LinkedHashMap<String,ClassNode>();linked.put(base.name,base);linked.put(caller.name,caller);
            if(scenario.equals("owner"))base.access=0;
            if(scenario.equals("interface"))base.access|=Opcodes.ACC_INTERFACE|Opcodes.ACC_ABSTRACT;
            if(scenario.equals("unrelated")||scenario.equals("protected"))caller.superName="java/lang/Object";
            if(scenario.equals("constructorInherited")) {
                base.methods.clear();
                MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);
                constructor.instructions.add(new InsnNode(Opcodes.RETURN)); constructor.maxLocals=1;
                base.methods.add(constructor);
                call.owner=caller.name;call.name="<init>";call.desc="()V";
            }
            if(scenario.equals("missing")){base.methods.clear();base.superName="missing/Parent";}
            if(scenario.equals("cycle")){base.methods.clear();base.superName=base.name;}
            if(scenario.equals("initializer"))call.name="<clinit>";
            String error=assertThrows(IllegalArgumentException.class,
                    ()->new ClassBytecodeCompiler(caller).compileToWat(linked),scenario).getMessage();
            if(java.util.Set.of("owner","interface","missing","cycle").contains(scenario))
                assertTrue(error.contains("hierarchy declaration"),scenario+": "+error);
            else {
                assertTrue(error.contains("special call "),scenario+": "+error);
                assertTrue(error.contains("q.Caller.call()I"),error);
            }
        }
    }

    @Test void matchesJvmInterfaceSpecialDefaultsAndPrivateHelpers() throws Exception {
        fixture("""
            public class Fixture {
                public static double run(int n){return new Device().decorated(n);}
            }
            interface Root {
                int seed(); void set(int n);
                default double base(double a,long b,float c){return seed()+a+b+c;}
                default void touch(int n){set(n);}
                default Root self(){return this;}
            }
            interface Left extends Root {}
            interface Fancy extends Left {
                private int helper(int n){return n*2;}
                default double decorated(int n){
                    Left.super.touch(n);
                    return Left.super.base(n,10L,0.5f)+helper(n)+Left.super.self().seed();
                }
            }
            class Device implements Fancy {
                int state;
                public int seed(){return state;}
                public void set(int n){state=n;}
                public double base(double a,long b,float c){return 999;}
                public void touch(int n){state=999;}
                public int helper(int n){return 999;}
            }
            """);
        // Exercise the same-interface private INVOKESPECIAL form, still accepted by the JVM.
        Path file=directory.resolve("Fancy.class");
        ClassNode type=new ClassNode();new ClassReader(Files.readAllBytes(file)).accept(type,0);
        for(MethodNode method:type.methods)for(AbstractInsnNode insn:method.instructions)
            if(insn instanceof MethodInsnNode call && call.name.equals("helper"))call.setOpcode(Opcodes.INVOKESPECIAL);
        ClassWriter writer=new ClassWriter(0);type.accept(writer);Files.write(file,writer.toByteArray());
        Path oracle=directory.resolve("Oracle.java");
        Files.writeString(oracle,"""
            public class Oracle {public static void main(String[] args){
                for(int n=-3;n<=3;n++)System.out.println(Fixture.run(n));
            }}
            """);
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-cp",directory.toString(),
                "-d",directory.toString(),oracle.toString()));
        run("java","-cp",directory.toString(),"Oracle");
        var expected=Files.readAllLines(directory.resolve("command.log"));assertEquals(7,expected.size());
        execute(linkJar("Device","Fancy","Left","Root","Fixture"),
                "const expected=["+String.join(",",expected)+"];\n"+"""
            for(let n=-3;n<=3;n++)assert.equal(e['Fixture.run'](n),expected[n+3]);
            """);
    }

    @Test void matchesJvmDirectSuperinterfacePrivateNestCall() throws Exception {
        fixture("""
            public class Fixture {
                interface Secret {private int value(){return 42;}}
                static class Reader implements Secret {int read(){return ((Secret)this).value();}}
                public static int run(){return new Reader().read();}
            }
            """);
        Path file=directory.resolve("Fixture$Reader.class");
        ClassNode type=new ClassNode();new ClassReader(Files.readAllBytes(file)).accept(type,0);
        for(MethodNode method:type.methods)for(AbstractInsnNode insn:method.instructions)
            if(insn instanceof MethodInsnNode call && call.name.equals("value"))call.setOpcode(Opcodes.INVOKESPECIAL);
        ClassWriter writer=new ClassWriter(0);type.accept(writer);Files.write(file,writer.toByteArray());
        Path oracle=directory.resolve("Oracle.java");
        Files.writeString(oracle,"public class Oracle {public static void main(String[] args){System.out.println(Fixture.run());}}");
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-cp",directory.toString(),
                "-d",directory.toString(),oracle.toString()));
        run("java","-cp",directory.toString(),"Oracle");
        assertEquals("42",Files.readString(directory.resolve("command.log")).trim());
        execute(linkJar("Fixture$Reader","Fixture$Secret","Fixture"),"assert.equal(e['Fixture.run'](),42);");
    }

    @Test void diagnosesInvalidInterfaceSpecialCallersAndTargets() {
        for(String scenario:new String[]{"unrelated","indirect","inaccessible","static","abstract","native",
                "classOwner","wrongReference","missing","cycle","initializer","noMethod","private"}) {
            ClassNode iface=new ClassNode();iface.name="p/Root";iface.superName="java/lang/Object";
            iface.access=Opcodes.ACC_PUBLIC|Opcodes.ACC_INTERFACE|Opcodes.ACC_ABSTRACT;
            int flags=Opcodes.ACC_PUBLIC;
            if(scenario.equals("static"))flags|=Opcodes.ACC_STATIC;
            if(scenario.equals("abstract"))flags|=Opcodes.ACC_ABSTRACT;
            if(scenario.equals("native"))flags|=Opcodes.ACC_NATIVE;
            if(scenario.equals("private"))flags=Opcodes.ACC_PRIVATE;
            iface.methods.add(integerDeclaration(flags));
            ClassNode caller=new ClassNode();caller.name="q/Caller";caller.superName="java/lang/Object";caller.access=Opcodes.ACC_PUBLIC;
            caller.interfaces.add(iface.name);
            MethodNode method=new MethodNode(Opcodes.ACC_PUBLIC,"call","()I",null,null);
            MethodInsnNode call=new MethodInsnNode(Opcodes.INVOKESPECIAL,iface.name,"value","()I",true);
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));method.instructions.add(call);
            method.instructions.add(new InsnNode(Opcodes.IRETURN));method.maxLocals=1;method.maxStack=1;caller.methods.add(method);
            var linked=new java.util.LinkedHashMap<String,ClassNode>();linked.put(iface.name,iface);linked.put(caller.name,caller);
            if(scenario.equals("unrelated"))caller.interfaces.clear();
            if(scenario.equals("indirect")) {
                ClassNode child=new ClassNode();child.name="p/Child";child.superName="java/lang/Object";
                child.access=iface.access;child.interfaces.add(iface.name);linked.put(child.name,child);
                caller.interfaces.clear();caller.interfaces.add(child.name);
            }
            if(scenario.equals("inaccessible"))iface.access&=~Opcodes.ACC_PUBLIC;
            if(scenario.equals("classOwner"))iface.access&=~Opcodes.ACC_INTERFACE;
            if(scenario.equals("wrongReference"))call.itf=false;
            if(scenario.equals("missing"))iface.interfaces.add("missing/Parent");
            if(scenario.equals("cycle"))iface.interfaces.add(iface.name);
            if(scenario.equals("initializer"))call.name="<init>";
            if(scenario.equals("noMethod"))iface.methods.clear();
            String error=assertThrows(IllegalArgumentException.class,
                    ()->new ClassBytecodeCompiler(caller).compileToWat(linked),scenario).getMessage();
            if(java.util.Set.of("inaccessible","classOwner","missing","cycle").contains(scenario))
                assertTrue(error.contains("hierarchy declaration"),scenario+": "+error);
            else if (scenario.equals("native")) assertTrue(error.contains("invalid interface method flags"), error);
            else if (scenario.equals("initializer")) assertTrue(error.contains("constructor references must return void"), error);
            else {
                assertTrue(error.contains(scenario.equals("wrongReference")?"special call ":"interface super call "),scenario+": "+error);
                assertTrue(error.contains("Caller.call()I"),error);
            }
        }
    }

    @Test void matchesJvmAccessibleSymbolicOwnersWithHiddenDeclarations() throws Exception {
        var sources=java.util.Map.of(
            "p/Open.java","""
                package p;
                interface HiddenView {default int defaultValue(){return 11;}}
                class Hidden implements HiddenView {public int value(){return 20;}}
                public class Open extends Hidden implements View {}
                """,
            "p/View.java","package p; public interface View extends HiddenView {}",
            "p/Peer.java","""
                package p;
                public class Peer {public static int local(){
                    Hidden value=new Hidden();return value.value()+((HiddenView)value).defaultValue();
                }}
                """,
            "Fixture.java","""
                public class Fixture {
                    public static int run(){
                        p.Open value=new p.Open();return value.value()+value.defaultValue()+through(value)+p.Peer.local();
                    }
                    public static int through(p.View value){return value.defaultValue();}
                }
                """);
        var args=new java.util.ArrayList<String>(java.util.List.of("-g","-d",directory.toString()));
        for(var source:sources.entrySet()) {
            Path file=directory.resolve(source.getKey());Files.createDirectories(file.getParent());
            Files.writeString(file,source.getValue());args.add(file.toString());
        }
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,args.toArray(String[]::new)));
        // Remove javac's accessibility bridge to exercise inherited JVM resolution directly.
        Path openFile=directory.resolve("p/Open.class");
        ClassNode open=new ClassNode();new ClassReader(Files.readAllBytes(openFile)).accept(open,0);
        open.methods.removeIf(method->method.name.equals("value"));
        ClassWriter writer=new ClassWriter(0);open.accept(writer);Files.write(openFile,writer.toByteArray());
        Path oracle=directory.resolve("Oracle.java");
        Files.writeString(oracle,"public class Oracle {public static void main(String[] args){System.out.println(Fixture.run());}}");
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-cp",directory.toString(),
                "-d",directory.toString(),oracle.toString()));
        run("java","-cp",directory.toString(),"Oracle");
        String expected=Files.readString(directory.resolve("command.log")).trim();assertEquals("73",expected);
        execute(linkJar("Fixture","p/Peer","p/Open","p/View","p/Hidden","p/HiddenView"),
                "assert.equal(e['Fixture.run'](),"+expected+");");
    }

    @Test void rejectsInaccessibleAllocationAndDynamicSymbolicOwners() {
        for(int opcode:new int[]{Opcodes.NEW,Opcodes.INVOKEVIRTUAL,Opcodes.INVOKEINTERFACE}) {
            ClassNode target=new ClassNode();target.name="p/Hidden";target.superName="java/lang/Object";
            if(opcode==Opcodes.INVOKEINTERFACE)target.access=Opcodes.ACC_INTERFACE|Opcodes.ACC_ABSTRACT;
            MethodNode value=new MethodNode(Opcodes.ACC_PUBLIC,"value","()I",null,null);
            value.instructions.add(new InsnNode(Opcodes.ICONST_1));value.instructions.add(new InsnNode(Opcodes.IRETURN));
            value.maxLocals=1;value.maxStack=1;target.methods.add(value);
            ClassNode caller=new ClassNode();caller.name="q/Caller";caller.superName="java/lang/Object";
            caller.sourceFile="Caller.java";
            MethodNode method=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"call",
                    opcode==Opcodes.NEW?"()Lp/Hidden;":"(Lp/Hidden;)I",null,null);
            if(opcode==Opcodes.NEW)method.instructions.add(new TypeInsnNode(Opcodes.NEW,target.name));
            else {
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
                method.instructions.add(new MethodInsnNode(opcode,target.name,"value","()I",opcode==Opcodes.INVOKEINTERFACE));
            }
            method.instructions.add(new InsnNode(opcode==Opcodes.NEW?Opcodes.ARETURN:Opcodes.IRETURN));
            method.maxLocals=1;method.maxStack=1;caller.methods.add(method);
            String error=assertThrows(IllegalArgumentException.class,()->new ClassBytecodeCompiler(caller)
                    .compileToWat(java.util.Map.of(caller.name,caller,target.name,target))).getMessage();
            assertTrue(error.contains("inaccessible symbolic class/interface p/Hidden"),error);
            assertTrue(error.contains("Caller.java:"),error);
            assertTrue(error.contains("q.Caller.call"),error);
        }
    }

    @Test void rejectsDynamicReferenceKindMismatchAndInitializerNames() {
        for(int opcode:new int[]{Opcodes.INVOKEVIRTUAL,Opcodes.INVOKEINTERFACE}) {
            for(String scenario:new String[]{"reference","constructor","initializer"}) {
                ClassNode owner=new ClassNode();owner.name="Owner";owner.superName="java/lang/Object";owner.access=Opcodes.ACC_PUBLIC;
                if(opcode==Opcodes.INVOKEINTERFACE)owner.access|=Opcodes.ACC_INTERFACE|Opcodes.ACC_ABSTRACT;
                owner.methods.add(new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_ABSTRACT,"value","()I",null,null));
                ClassNode caller=new ClassNode();caller.name="Caller";caller.superName="java/lang/Object";
                MethodNode method=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"call","(LOwner;)I",null,null);
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
                String name=scenario.equals("constructor")?"<init>":scenario.equals("initializer")?"<clinit>":"value";
                method.instructions.add(new MethodInsnNode(opcode,owner.name,name,"()I",
                        scenario.equals("reference")?opcode!=Opcodes.INVOKEINTERFACE:opcode==Opcodes.INVOKEINTERFACE));
                method.instructions.add(new InsnNode(Opcodes.IRETURN));method.maxLocals=1;method.maxStack=1;caller.methods.add(method);
                String error=assertThrows(IllegalArgumentException.class,()->new ClassBytecodeCompiler(caller)
                        .compileToWat(java.util.Map.of(owner.name,owner,caller.name,caller))).getMessage();
                if (scenario.equals("constructor")) assertTrue(error.contains("constructor references must return void"), error);
                else {
                    assertTrue(error.contains("dynamic call Owner."),error);
                    assertTrue(error.contains(scenario.equals("reference")?"reference kind":"initializers cannot"),error);
                }
            }
        }
    }

    @Test void executesSharedInterfaceDiamondsWithAbstractParentsAndFinalLeaves() throws Exception {
        fixture("""
            public class Fixture {public static int run(){return new Leaf().value();}}
            interface Root {int seed();default int value(){return seed()+1;}}
            interface Left extends Root {}
            interface Right extends Root {}
            interface Diamond extends Left,Right {}
            interface Other extends Right,Left {}
            abstract class Base implements Diamond {}
            final class Leaf extends Base implements Other {public int seed(){return 42;}}
            """);
        execute(linkJar("Leaf","Base","Other","Diamond","Right","Left","Root","Fixture"),
                "assert.equal(e['Fixture.run'](),43);");
        execute(linkJar("Fixture","Root","Left","Right","Diamond","Other","Base","Leaf"),
                "assert.equal(e['Fixture.run'](),43);");
    }

    @Test void matchesJvmFinalMethodsAcrossDistinctPackageAndPrivateSlots() throws Exception {
        var sources=java.util.Map.of(
            "p/Base.java","""
                package p;
                public class Base {
                    final int hidden(){return 10;}
                    private final int privateValue(){return 11;}
                    public final int fixed(){return 20;}
                    protected int open(){return 30;}
                    public int baseSlot(){return hidden()+privateValue();}
                }
                """,
            "p/Peer.java","package p; public class Peer {public static int read(Base value){return value.hidden();}}",
            "q/Middle.java","""
                package q;
                public class Middle extends p.Base {
                    public int hidden(){return 100;}
                    public int privateValue(){return 200;}
                    public int open(){return 300;}
                }
                """,
            "q/Leaf.java","""
                package q;
                public final class Leaf extends Middle {
                    public final int hidden(){return 1000;}
                    public final int privateValue(){return 201;}
                    public final int open(){return 400;}
                }
                """,
            "Fixture.java","""
                public class Fixture {public static int run(){
                    q.Middle value=new q.Leaf();
                    return value.hidden()+value.privateValue()+value.open()+value.baseSlot()+value.fixed()+p.Peer.read(value);
                }}
                """);
        var args=new java.util.ArrayList<String>(java.util.List.of("-g","-d",directory.toString()));
        for(var source:sources.entrySet()) {
            Path file=directory.resolve(source.getKey());Files.createDirectories(file.getParent());
            Files.writeString(file,source.getValue());args.add(file.toString());
        }
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,args.toArray(String[]::new)));
        Path oracle=directory.resolve("Oracle.java");
        Files.writeString(oracle,"public class Oracle {public static void main(String[] args){System.out.println(Fixture.run());}}");
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-cp",directory.toString(),
                "-d",directory.toString(),oracle.toString()));
        run("java","-cp",directory.toString(),"Oracle");
        String expected=Files.readString(directory.resolve("command.log")).trim();assertEquals("1652",expected);
        execute(linkJar("q/Leaf","q/Middle","p/Peer","p/Base","Fixture"),
                "assert.equal(e['Fixture.run'](),"+expected+");");
        execute(linkJar("Fixture","p/Base","p/Peer","q/Middle","q/Leaf"),
                "assert.equal(e['Fixture.run'](),"+expected+");");
    }

    @Test void matchesJvmLegacySharedNestedAndWideSubroutines() throws Exception {
        ClassWriter writer=new ClassWriter(0);
        writer.visit(Opcodes.V1_5,Opcodes.ACC_PUBLIC,"Legacy",null,"java/lang/Object",null);
        writer.visitSource("Legacy.java",null);
        MethodVisitor shared=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"shared","(I)I",null,null);
        shared.visitCode();Label negative=new Label(),exit=new Label(),sub=new Label();
        shared.visitInsn(Opcodes.ICONST_0);shared.visitVarInsn(Opcodes.ISTORE,1);
        shared.visitVarInsn(Opcodes.ILOAD,0);shared.visitJumpInsn(Opcodes.IFLT,negative);
        shared.visitJumpInsn(Opcodes.JSR,sub);shared.visitJumpInsn(Opcodes.JSR,sub);shared.visitJumpInsn(Opcodes.GOTO,exit);
        shared.visitLabel(negative);shared.visitJumpInsn(Opcodes.JSR,sub);
        shared.visitLabel(exit);shared.visitVarInsn(Opcodes.ILOAD,1);shared.visitInsn(Opcodes.IRETURN);
        shared.visitLabel(sub);shared.visitVarInsn(Opcodes.ASTORE,300);shared.visitIincInsn(1,5);shared.visitVarInsn(Opcodes.RET,300);
        shared.visitMaxs(1,301);shared.visitEnd();
        MethodVisitor nested=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"nested","(I)I",null,null);
        nested.visitCode();Label outer=new Label(),inner=new Label();
        nested.visitVarInsn(Opcodes.ILOAD,0);nested.visitVarInsn(Opcodes.ISTORE,1);nested.visitJumpInsn(Opcodes.JSR,outer);
        nested.visitVarInsn(Opcodes.ILOAD,1);nested.visitInsn(Opcodes.IRETURN);
        nested.visitLabel(outer);nested.visitVarInsn(Opcodes.ASTORE,2);nested.visitIincInsn(1,10);
        nested.visitJumpInsn(Opcodes.JSR,inner);nested.visitVarInsn(Opcodes.RET,2);
        nested.visitLabel(inner);nested.visitVarInsn(Opcodes.ASTORE,3);nested.visitIincInsn(1,1);nested.visitVarInsn(Opcodes.RET,3);
        nested.visitMaxs(1,4);nested.visitEnd();
        MethodVisitor stack=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"stack","(J)J",null,null);
        stack.visitCode();Label cleanup=new Label();
        stack.visitVarInsn(Opcodes.LLOAD,0);stack.visitJumpInsn(Opcodes.JSR,cleanup);
        stack.visitLdcInsn(2L);stack.visitInsn(Opcodes.LADD);stack.visitInsn(Opcodes.LRETURN);
        stack.visitLabel(cleanup);stack.visitVarInsn(Opcodes.ASTORE,2);stack.visitVarInsn(Opcodes.RET,2);
        stack.visitMaxs(4,3);stack.visitEnd();writer.visitEnd();
        byte[] bytes=writer.toByteArray();Files.write(directory.resolve("Legacy.class"),bytes);
        Path oracle=directory.resolve("Oracle.java");
        Files.writeString(oracle,"""
            public class Oracle {public static void main(String[] args){
                for(int n=-3;n<=3;n++){
                    System.out.println(Legacy.shared(n));System.out.println(Legacy.nested(n));
                    System.out.println(Legacy.stack(n));
                }
            }}
            """);
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-cp",directory.toString(),
                "-d",directory.toString(),oracle.toString()));
        run("java","-cp",directory.toString(),"Oracle");
        var expected=Files.readAllLines(directory.resolve("command.log"));assertEquals(21,expected.size());
        String assertions="const expected=["+String.join(",",expected)+"];\n"+"""
            for(let n=-3;n<=3;n++){
                const i=(n+3)*3;
                assert.equal(e['Legacy.shared'](n),expected[i]);
                assert.equal(e['Legacy.nested'](n),expected[i+1]);
                assert.equal(e['Legacy.stack'](BigInt(n)),BigInt(expected[i+2]));
            }
            """;
        execute(BytecodeCompiler.compileClass(bytes),assertions);
        execute(linkJar("Legacy"),assertions);
        ClassNode direct=new ClassNode();new ClassReader(bytes).accept(direct,0);
        execute(new ClassBytecodeCompiler(direct).compileToWat(),assertions);
    }

    @Test void legacyNormalizationPreservesHandlersAndSourceMetadataAndRejectsRecursion() {
        for(boolean recursive:new boolean[]{false,true}) {
            ClassNode type=new ClassNode();type.name="Legacy";type.superName="java/lang/Object";
            type.sourceFile="Legacy.java";type.version=Opcodes.V1_5;type.access=Opcodes.ACC_PUBLIC;
            MethodNode method=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"run","()V",null,null);
            LabelNode start=new LabelNode(),end=new LabelNode(),sub=new LabelNode(),handler=new LabelNode();
            method.instructions.add(start);method.instructions.add(new LineNumberNode(7,start));
            method.instructions.add(new JumpInsnNode(Opcodes.JSR,sub));method.instructions.add(end);
            method.instructions.add(new InsnNode(Opcodes.RETURN));
            method.instructions.add(sub);method.instructions.add(new VarInsnNode(Opcodes.ASTORE,0));
            if(recursive)method.instructions.add(new JumpInsnNode(Opcodes.JSR,sub));
            method.instructions.add(new VarInsnNode(Opcodes.RET,0));
            method.instructions.add(handler);method.instructions.add(new InsnNode(Opcodes.ATHROW));
            if(!recursive)method.tryCatchBlocks.add(new TryCatchBlockNode(start,end,handler,null));
            method.maxLocals=1;method.maxStack=1;type.methods.add(method);
            String error=assertThrows(IllegalArgumentException.class,
                    ()->new ClassBytecodeCompiler(type).compileToWat()).getMessage();
            assertTrue(error.contains("Legacy.java:7:"),error);
            assertTrue(error.contains(recursive?"legacy subroutine":"exception handler table"),error);
            if(!recursive){
                MethodNode normalized=type.methods.get(0);assertFalse(normalized.tryCatchBlocks.isEmpty());
                assertTrue(java.util.Arrays.stream(normalized.instructions.toArray())
                        .noneMatch(insn->insn.getOpcode()==Opcodes.JSR||insn.getOpcode()==Opcodes.RET));
            }
        }
    }

    private MethodNode integerDeclaration(int flags) {
        MethodNode method = new MethodNode(flags, "value", "()I", null, null);
        if ((flags & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
            method.instructions.add(new InsnNode(Opcodes.ICONST_1));
            method.instructions.add(new InsnNode(Opcodes.IRETURN));
            method.maxStack = 1; method.maxLocals = (flags & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
        }
        return method;
    }

    private String linkJar(String... names) throws Exception {
        var bytes=new java.io.ByteArrayOutputStream();
        try(var jar=new java.util.jar.JarOutputStream(bytes)) {
            for(String name:names) {
                jar.putNextEntry(new java.util.jar.JarEntry(name+".class"));
                jar.write(Files.readAllBytes(directory.resolve(name+".class"))); jar.closeEntry();
            }
        }
        return BytecodeCompiler.compileJar(new java.io.ByteArrayInputStream(bytes.toByteArray()));
    }

    @Test void linksSupportedClassesWithinOneJar() throws Exception {
        fixture("""
            public class Fixture {
                public static int calculate(int n) { Helper value=new Helper(n); value.extra=3; return Helper.doubleValue(value)+value.extra; }
            }
            class Helper {
                int unused, stored, extra;
                Helper(int n) { stored=n; }
                static int doubleValue(Helper value) { return value.stored*2+value.unused; }
            }
            """);
        var bytes=new java.io.ByteArrayOutputStream();
        try(var jar=new java.util.jar.JarOutputStream(bytes)) {
            for(String name:new String[]{"Fixture","Helper"}) {
                jar.putNextEntry(new java.util.jar.JarEntry(name+".class"));
                jar.write(Files.readAllBytes(directory.resolve(name+".class"))); jar.closeEntry();
            }
        }
        execute(BytecodeCompiler.compileJar(new java.io.ByteArrayInputStream(bytes.toByteArray())),
                "assert.equal(e['Fixture.calculate'](20),43); assert.equal(e['Fixture.calculate'](-3),-3);");
    }

    @Test void rejectsUnsupportedJavacConstructsWithSourceMethodAndRemediation() throws Exception {
        for(String body:new String[]{
                "public static final String TEXT=\"text\"; public static int f(){ return 1; }",
                "public static Runnable f() { return () -> {}; }",
                "public static String f(int n) { return \"value=\"+n; }",
                "public static int f() { try { return 1; } catch(RuntimeException e) { return 0; } }",
                "public static native int f();",
                "public static synchronized int f() { return 1; }",
                "public static int f() { return Math.abs(-4); }",
                "public static Class<?> f() { return Fixture.class; }",
                "public static Object f() { return java.util.Arrays.copyOf(new int[2],3); }",
                "public static Object f() { return new Object(); }",
                "public static class Child extends Fixture {} public static Object f(){ return new Child(); }",
                "static int x=compute(); static int compute(){ return 1; }",
                "public static int f(int n){ return n; } public static int f(){ return 1; }"}) {
            byte[] bytes=fixture("public class Fixture {\n"+body+"\n}");
            var error=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClass(bytes),body);
            assertTrue(error.getMessage().startsWith("Fixture.java:"),error.getMessage());
            assertTrue(error.getMessage().contains("Fixture"),error.getMessage());
            if (body.contains("try {")) assertTrue(error.getMessage().contains("Fixture.java:2:"),error.getMessage());
            assertTrue(error.getMessage().contains("unsupported"),error.getMessage());
            assertTrue(error.getMessage().contains("; "),error.getMessage());
        }
    }

    @Test void rejectsMissingAndCyclicAllocationHierarchies() {
        for (boolean cycle : new boolean[]{false, true}) {
            ClassNode owner = new ClassNode();
            owner.version=Opcodes.V17; owner.access=Opcodes.ACC_PUBLIC;
            owner.name="Broken"; owner.superName=cycle ? "Broken" : "Missing";
            owner.sourceFile="Broken.java";
            MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,
                    "create", "()LBroken;", null, null);
            method.instructions.add(new TypeInsnNode(Opcodes.NEW, "Broken"));
            method.instructions.add(new InsnNode(Opcodes.ARETURN));
            method.maxStack=1;
            owner.methods.add(method);
            var error=assertThrows(IllegalArgumentException.class,
                    () -> new ClassBytecodeCompiler(owner).compileToWat());
            assertTrue(error.getMessage().contains("Broken.java:line unavailable: Broken:"),error.getMessage());
            assertTrue(error.getMessage().contains("hierarchy declaration"),error.getMessage());
            assertTrue(error.getMessage().contains(cycle?"cyclic":"missing superclass"),error.getMessage());
        }
    }

    @Test void diagnosesOpcodeAndMissingDebugInformation() {
        ClassWriter writer=new ClassWriter(0);
        writer.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,"NoDebug",null,"java/lang/Object",null);
        MethodVisitor method=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"dynamic","()V",null,null);
        method.visitCode();
        method.visitInvokeDynamicInsn("run","()V",new Handle(Opcodes.H_INVOKESTATIC,"Bootstrap","bootstrap","()V",false));
        method.visitInsn(Opcodes.RETURN); method.visitMaxs(0,0); method.visitEnd(); writer.visitEnd();
        var error=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClass(writer.toByteArray()));
        assertTrue(error.getMessage().contains("INVOKEDYNAMIC"),error.getMessage());
        assertTrue(error.getMessage().contains("line unavailable"),error.getMessage());
        assertTrue(error.getMessage().contains("NoDebug.dynamic()V"),error.getMessage());
    }

    @Test void checksActualMavenDependencyJarsAndReportsUnsupportedFeatures() throws Exception {
        for(Class<?> library:new Class<?>[]{org.objectweb.asm.Type.class,org.junit.jupiter.api.Assertions.class}) {
            var jar=Path.of(library.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile();
            assertTrue(jar.getName().endsWith(".jar"));
            var error=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileJar(jar));
            assertTrue(error.getMessage().contains("unsupported"),error.getMessage());
            assertTrue(error.getMessage().contains("; "),error.getMessage());
        }
    }
}
