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

class BytecodeFloatingValuesTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClassName="Metadata";
    private String fixtureClasspath;

    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        List<String> options=new ArrayList<>(List.of("-g","-parameters","--release",release,"-d",directory.toString()));
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

    @Test void decimalHexSpecialAndWrapperValues() throws Exception {
        fixture("""
            public class Metadata {public static int run(){int hash=1;
                for(String s:new String[]{"0","-0","+0.0"," .125 ","1.","1e+5F","-1.234567890123456789d","NaN","-NaN","+Infinity","-Infinity","0x1p0","-0X.8P+2","0x1.fp-4f","1e999999999999999999","-1e-999999999999999","-0x0p999999999999999","0x1p999999999999999","0x1p-999999999999999"}){
                    double d=Double.parseDouble(s);float f=Float.parseFloat(s);hash=31*hash+Double.hashCode(d);hash=31*hash+Float.hashCode(f);
                    if(Double.doubleToLongBits(d)!=Double.doubleToLongBits(new Double(s).doubleValue()) || !Double.valueOf(s).equals(d))throw new Error();
                    if(Float.floatToIntBits(f)!=Float.floatToIntBits(new Float(s).floatValue()) || !Float.valueOf(s).equals(f))throw new Error();
                    hash+=Double.isFinite(d)?7:11;hash+=Float.isFinite(f)?13:17;
                    hash+=Double.isInfinite(d)?19:23;hash+=Float.isInfinite(f)?29:31;
                    if(Double.isInfinite(d)!=new Double(d).isInfinite() || Float.isInfinite(f)!=new Float(f).isInfinite())throw new Error();
                }
                for(String s:new String[]{null,"true","TRUE","TrUe","false"," true","true ","1","","trüe"}){
                    hash=31*hash+(Boolean.parseBoolean(s)?1:0);if(new Boolean(s).booleanValue()!=Boolean.valueOf(s).booleanValue() || Boolean.valueOf(s)!=(Boolean.parseBoolean(s)?Boolean.TRUE:Boolean.FALSE))throw new Error();
                }
                return hash;
            }}
            """);compare(List.of("run"));
    }
    @Test void exactRoundingAtIeeeBoundariesAndDoubleRoundingTraps() throws Exception {
        fixture("""
            public class Metadata {public static int run(){int hash=1;
                for(String s:new String[]{
                    "1.000000059604644775390625","1.0000000596046447753906250000000001","1.0000000596046447753906249999999999",
                    "1.00000000000000011102230246251565404236316680908203125","1.000000000000000111022302462515654042363166809082031251",
                    "1.7976931348623157e308","1.7976931348623159e308","2.2250738585072014e-308","4.9406564584124654e-324","2.4703282292062327e-324",
                    "3.4028234663852886e38","3.4028235677973366e38","1.1754943508222875e-38","1.401298464324817e-45","7.006492321624085e-46",
                    "0x1.000001p0","0x1.0000010000000000000001p0","0x1.000000fffffffp0","0x1.fffffffffffffp1023","0x1.fffffffffffff8p1023",
                    "0x1p-1074","0x1p-1075","0x1.0000000000001p-1075","0x1.fffffep127","0x1.ffffffp127","0x1p-149","0x1p-150","0x1.00000001p-150"
                })for(String prefix:new String[]{"","-"}){String input=prefix+s;hash=31*hash+Double.hashCode(Double.parseDouble(input));hash=31*hash+Float.hashCode(Float.parseFloat(input));}
                return hash;
            }}
            """);compare(List.of("run"));
    }
    @Test void invalidGrammarAndNullFailures() throws Exception {
        fixture("""
            public class Metadata {public static int run(){int hash=1;
                for(String s:new String[]{""," ","+",".","1..0","1_0","１.０","١.٢","1e","1e+","1e2e3","NaNf","Infinityd","nan","infinity","0x1","0x.p1","0xp0","0x1p","0x1p+","0x1.2.3p1","0x1p１","0x1p0x1","1fX","1\\u00a0","\\u20031"}){
                    try{Double.parseDouble(s);throw new Error(s);}catch(NumberFormatException e){hash++;}
                    try{Float.parseFloat(s);throw new Error(s);}catch(NumberFormatException e){hash++;}
                }
                try{Double.parseDouble(null);throw new Error();}catch(NullPointerException e){hash++;}
                try{Float.valueOf((String)null);throw new Error();}catch(NullPointerException e){hash++;}
                for(int i=0;i<33;i++){String s=""+(char)i+"-1.25"+(char)i;hash=31*hash+Double.hashCode(Double.parseDouble(s));}
                return hash;
            }}
            """);compare(List.of("run"));
    }
    @Test void executesOriginalPinnedH2RoundMagicBody() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("com/h2database/h2/2.2.224/h2-2.2.224.jar");assertDigest(artifact,"b9d8f19358ada82a4f6eb5b174c6cfe320a375b5a9cb5a4fe456d623e6e55497");
        try(JarFile jar=new JarFile(artifact.toFile())){
            ClassNode original=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry("org/h2/expression/function/MathFunction.class")).readAllBytes()).accept(original,0);
            MethodNode method=original.methods.stream().filter(m->m.name.equals("roundMagic")).findFirst().orElseThrow();
            // Keep the complete original instructions in an isolated static-method carrier.
            ClassWriter writer=new ClassWriter(0);writer.visit(original.version,ACC_PUBLIC,"SelectedH2RoundMagic",null,"java/lang/Object",null);
            method.access=ACC_PUBLIC|ACC_STATIC;method.accept(writer);writer.visitEnd();writeClass("SelectedH2RoundMagic",writer.toByteArray());
        }
        fixtureClasspath=directory.toString();fixture("""
            public class Metadata {public static int run(){int hash=1;
                for(double value:new double[]{0,-0.0,1e-14,-1e-14,1e13,-1e13,1.25,0.30000000000000004,1.0000000000000002,1.9999999999999998,123.45678901234567,-123.45678901234567})hash=31*hash+Double.hashCode(SelectedH2RoundMagic.roundMagic(value));
                return hash;
            }}
            """);compare(List.of("run"));
    }

    private void writeClass(String name,byte[] bytes) throws Exception {
        Path file=directory.resolve(name+".class");Files.createDirectories(file.getParent());Files.write(file,bytes);
    }
    private void assertDigest(Path artifact,String expected) throws Exception {
        assertEquals(expected,HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
    }

    private void compare(List<String> names) throws Exception {
        StringBuilder assertions=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
            Class<?> type=loader.loadClass(fixtureClassName);for(String name:names){Object expected=type.getMethod(name).invoke(null);assertions.append("assert.equal(").append(expected instanceof String?"text(e['"+fixtureClassName+".":"e['"+fixtureClassName+".").append(name).append(expected instanceof String?"']()),":"'](),").append(expected instanceof String?js((String)expected):expected).append(");\n");}
        }
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar())),assertions.toString());
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
