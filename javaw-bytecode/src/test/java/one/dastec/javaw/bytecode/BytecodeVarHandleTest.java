package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeVarHandleTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClasspath;

    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        List<String> options=new ArrayList<>(List.of("-g","--release",release,"-d",directory.toString()));
        if(fixtureClasspath!=null)options.addAll(List.of("-classpath",fixtureClasspath));
        options.add(file.toString());
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(String[]::new)));
        return jar();
    }

    private byte[] jar() throws Exception {
        List<Path> files;
        try(var paths=Files.walk(directory)) {
            files=new ArrayList<>(paths.filter(path->path.toString().endsWith(".class")).sorted().toList());
        }
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(bytes)) {
            for(Path file:files){jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(Files.readAllBytes(file));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    @Test void executesPlainViewsAndPreservesByteOrderAndRawFloatingBits() throws Exception {
        fixture("""
            import java.lang.invoke.*;
            import java.nio.ByteOrder;
            public class Metadata {
                public static long run(int kind,int endian,int offset,long bits) {
                    Class<?> type=kind==0?short[].class:kind==1?char[].class:kind==2?int[].class:kind==3?long[].class:kind==4?float[].class:double[].class;
                    VarHandle handle=MethodHandles.byteArrayViewVarHandle(type,endian==0?ByteOrder.BIG_ENDIAN:ByteOrder.LITTLE_ENDIAN);
                    byte[] bytes=new byte[24];
                    switch(kind){
                        case 0:handle.set(bytes,offset,(short)bits);return (short)handle.get(bytes,offset);
                        case 1:handle.set(bytes,offset,(char)bits);return (char)handle.get(bytes,offset);
                        case 2:handle.set(bytes,offset,(int)bits);return (int)handle.get(bytes,offset);
                        case 3:handle.set(bytes,offset,bits);return (long)handle.get(bytes,offset);
                        case 4:handle.set(bytes,offset,Float.intBitsToFloat((int)bits));return Float.floatToRawIntBits((float)handle.get(bytes,offset));
                        default:handle.set(bytes,offset,Double.longBitsToDouble(bits));return Double.doubleToRawLongBits((double)handle.get(bytes,offset));
                    }
                }
                public static long order(int endian){
                    VarHandle handle=MethodHandles.byteArrayViewVarHandle(long[].class,endian==0?ByteOrder.BIG_ENDIAN:ByteOrder.LITTLE_ENDIAN);
                    byte[] bytes={1,2,3,4,5,6,7,8,9};return (long)handle.get(bytes,1);
                }
            }
            """);
        StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            long[] values={0,-1,Long.MIN_VALUE,0x7ff8000000000123L,0x80000000L,0x123456789abcdefL};
            for(int kind=0;kind<6;kind++)for(int endian=0;endian<2;endian++)for(int offset:new int[]{0,1,7,16})for(long bits:values)
                checks.append("assert.equal(e['Metadata.run'](").append(kind).append(',').append(endian).append(',').append(offset).append(',').append(bits).append("n),")
                    .append(jvm.getMethod("run",int.class,int.class,int.class,long.class).invoke(null,kind,endian,offset,bits)).append("n);\n");
            for(int endian=0;endian<2;endian++)checks.append("assert.equal(e['Metadata.order'](").append(endian).append("),")
                .append(jvm.getMethod("order",int.class).invoke(null,endian)).append("n);\n");
        }
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar())),checks.toString());
    }

    @Test void matchesJvmAdaptationModesAndFailurePriorityWithoutPartialStores() throws Exception {
        fixture("""
            import java.lang.invoke.*;
            import java.nio.ByteOrder;
            public class Metadata {
                static VarHandle view(Class<?> type){return MethodHandles.byteArrayViewVarHandle(type,ByteOrder.BIG_ENDIAN);}
                public static int failure(int scenario){
                    VarHandle h=view(int[].class);byte[] bytes={1,2,3,4,5,6,7,8};
                    try {
                        switch(scenario){
                            case 0:return (int)h.get((byte[])null,-1);
                            case 1:h.set(bytes,6,42);break;
                            case 2:return (int)h.get(bytes,Integer.MAX_VALUE);
                            case 3:return (int)h.get(bytes,-1);
                            case 4:return (int)h.withInvokeExactBehavior().get((Object)bytes,0);
                            case 5:return (int)h.withInvokeExactBehavior().get((byte[])null,0);
                            case 6:return (int)h.get((Object)new int[2],0);
                            case 7:return (int)h.withInvokeExactBehavior().get((Object)null,-1);
                            case 8:return (short)h.get(bytes,0);
                            case 9:h.set(bytes,0,1L);break;
                            case 10:return (int)view(null).get(bytes,0);
                            case 11:return (int)MethodHandles.byteArrayViewVarHandle(int[].class,null).get(bytes,0);
                            case 12:return (int)view(byte[].class).get(bytes,0);
                            case 13:return (int)view(Object[].class).get(bytes,0);
                            case 14:VarHandle absent=null;return (int)absent.get((byte[])null,-1);
                        }
                        return 0;
                    }catch(WrongMethodTypeException e){return 1;}
                    catch(NullPointerException e){return 2;}
                    catch(ClassCastException e){return 3;}
                    catch(IndexOutOfBoundsException e){return bytes[6]==7 && bytes[7]==8?4:9;}
                    catch(UnsupportedOperationException e){return 5;}
                }
                public static double widening(){
                    byte[] bytes=new byte[16];VarHandle i=view(int[].class),d=view(double[].class);
                    i.set((Object)bytes,1,123);long value=(long)i.get((Object)bytes,1);
                    d.set(bytes,3,(int)value);return (double)d.get(bytes,3)+(float)i.get(bytes,1);
                }
                public static int modes(){
                    VarHandle h=view(long[].class),exact=h.withInvokeExactBehavior();
                    return (!h.hasInvokeExactBehavior()?1:0)+(exact.hasInvokeExactBehavior()?2:0)
                        +(!exact.withInvokeBehavior().hasInvokeExactBehavior()?4:0)
                        +(h.withInvokeBehavior()==h?8:0)+(exact.withInvokeExactBehavior()==exact?16:0)
                        +(exact.varType()==long.class?32:0);
                }
            }
            """);
        StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            for(int scenario=0;scenario<15;scenario++)checks.append("assert.equal(e['Metadata.failure'](").append(scenario).append("),")
                .append(jvm.getMethod("failure",int.class).invoke(null,scenario)).append(");\n");
            for(String method:List.of("widening","modes"))checks.append("assert.equal(e['Metadata.").append(method).append("'](),")
                .append(jvm.getMethod(method).invoke(null)).append(");\n");
        }
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar())),checks.toString());
    }

    @Test void diagnosesOrderedAtomicsAndRuntimeGenerationAtOriginalSites() throws Exception {
        for(String expression:List.of(
                "return (int)java.lang.invoke.MethodHandles.byteArrayViewVarHandle(int[].class,java.nio.ByteOrder.BIG_ENDIAN).getVolatile(new byte[8],0);",
                "return java.lang.invoke.MethodHandles.publicLookup()==null?0:1;",
                "return java.lang.invoke.MethodHandles.arrayElementVarHandle(int[].class)==null?0:1;")) {
            byte[] jar=fixture("public class Metadata {public static int run(){"+expression+"}}");
            String error=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar))).getMessage();
            assertTrue(error.contains("Metadata.java:1"),error);assertTrue(error.contains("Metadata.run()I"),error);
            assertTrue(error.contains("VarHandle access") || error.contains("dynamic invocation/generation"),error);
        }
    }

    @Test void executesUnchangedPinnedJava17H2BitsBytecode() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path h2=repository.resolve("com/h2database/h2/2.2.224/h2-2.2.224.jar");
        assertEquals("b9d8f19358ada82a4f6eb5b174c6cfe320a375b5a9cb5a4fe456d623e6e55497",
            java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(h2))));
        fixtureClasspath=h2.toString();
        fixture("""
            import org.h2.util.Bits;
            public class Metadata {
                public static long integral(int little,int offset,long value){
                    byte[] bytes=new byte[24];
                    if(little==0){Bits.writeLong(bytes,offset,value);return Bits.readLong(bytes,offset);}
                    Bits.writeLongLE(bytes,offset,value);return Bits.readLongLE(bytes,offset);
                }
                public static long floating(int little,int offset,long bits){
                    byte[] bytes=new byte[24];double value=Double.longBitsToDouble(bits);
                    if(little==0){Bits.writeDouble(bytes,offset,value);return Double.doubleToRawLongBits(Bits.readDouble(bytes,offset));}
                    Bits.writeDoubleLE(bytes,offset,value);return Double.doubleToRawLongBits(Bits.readDoubleLE(bytes,offset));
                }
                public static int order(){byte[] bytes=new byte[9];Bits.writeInt(bytes,1,0x12345678);return Bits.readIntLE(bytes,1);}
                public static int comparison(){return Bits.compareNotNull(new char[]{1,255},new char[]{1,256})
                    +3*Bits.compareNotNullSigned(new byte[]{-1},new byte[]{1})+7*Bits.compareNotNullUnsigned(new byte[]{-1},new byte[]{1});}
                public static long uuid(){byte[] bytes=Bits.uuidToBytes(new java.util.UUID(123,456));return Bits.readLong(bytes,0)+Bits.readLong(bytes,8);}
                public static int failure(){byte[] bytes={1,2,3,4,5,6,7,8};try{Bits.writeLong(bytes,1,0);return 0;}
                    catch(IndexOutOfBoundsException e){return bytes[0]+bytes[7];}}
            }
            """);
        try(JarFile input=new JarFile(h2.toFile());InputStream bytes=input.getInputStream(input.getJarEntry("META-INF/versions/9/org/h2/util/Bits.class"))) {
            Path output=directory.resolve("org/h2/util/Bits.class");Files.createDirectories(output.getParent());Files.copy(bytes,output);
        }
        StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL(),h2.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            for(String method:List.of("integral","floating"))for(int little=0;little<2;little++)for(int offset:new int[]{0,1,16})
                for(long bits:new long[]{0,-1,Long.MIN_VALUE,0x7ff8000000000123L,0x123456789abcdefL})
                    checks.append("assert.equal(e['Metadata.").append(method).append("'](").append(little).append(',').append(offset).append(',').append(bits).append("n),")
                        .append(jvm.getMethod(method,int.class,int.class,long.class).invoke(null,little,offset,bits)).append("n);\n");
            for(String method:List.of("order","comparison","failure","uuid"))checks.append("assert.equal(e['Metadata.").append(method).append("'](),")
                .append(jvm.getMethod(method).invoke(null)).append(method.equals("uuid")?"n);\n":");\n");
        }
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar())),checks.toString());
    }

    private String js(String value) {
        if(value==null)return "null";
        StringBuilder out=new StringBuilder("\"");
        for(char c:value.toCharArray())out.append(String.format("\\u%04x",(int)c));
        return out.append('"').toString();
    }
    private void execute(String wat,String assertions) throws Exception {
        WasmTestSupport.execute(directory,wat,assertions);
    }
}
