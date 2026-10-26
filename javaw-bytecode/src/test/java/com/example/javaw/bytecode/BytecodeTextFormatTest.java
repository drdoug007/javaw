package com.example.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeTextFormatTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClasspath;
    private String fixtureClassName="Metadata";

    @Test void localeIdentities() throws Exception {
        fixture("""
            import java.util.Locale;
            public class Metadata {
                public static int run(int group) {
                    String[] languages={"EN","he","iw","yi","ji","id","in","","und","tr"};
                    String[] countries={"us","","gb"}; int h=1;
                    for(String language:languages)for(String country:countries)for(String variant:new String[]{"","V"}) {
                        Locale l=new Locale(language,country,variant);
                        h=31*h+l.getLanguage().hashCode();h=31*h+l.getCountry().hashCode();
                        h=31*h+l.getVariant().hashCode();h=31*h+l.toString().hashCode();h=31*h+l.hashCode();
                        h=31*h+(l.equals(l.clone())?1:0);h=31*h+(l!=l.clone()?1:0);
                    }
                    h=31*h+Locale.getDefault().hashCode();h=31*h+(Locale.PRC==Locale.CHINA?1:0);
                    try {new Locale(null);return 0;}catch(NullPointerException expected){return h;}
                }
            }
            """);compare(1);
    }

    @Test void unicodeUppercase() throws Exception {
        fixture("""
            import java.util.Locale;
            public class Metadata {
                public static int run(int group) {
                    int h=1,start=group*1024;
                    for(int cp=start;cp<start+1024 && cp<=0x10ffff;cp++) {
                        String text=new String(Character.toChars(cp));
                        String upper=text.toUpperCase(Locale.ROOT);
                        h=31*h+upper.hashCode();h=31*h+(upper==text?1:0);
                    }
                    return h;
                }
            }
            """);compare(1088);
    }

    @Test void turkicUppercase() throws Exception {
        fixture("""
            import java.util.Locale;
            public class Metadata {public static int run(int group){int h=1;
                for(String lang:new String[]{"tr","az","TR","AZ","en"}) {
                    Locale l=new Locale(lang,"tr");
                    for(int cp=group*256;cp<(group+1)*256;cp+=17){String s="i"+new String(Character.toChars(cp))+"ıİi";h=31*h+s.toUpperCase(l).hashCode();}
                }
                try {"".toUpperCase((Locale)null);return 0;}catch(NullPointerException expected){return h;}
            }}
            """);compare(256);
    }

    @Test void lithuanianUppercase() throws Exception {
        fixture("""
            import java.util.Locale;
            public class Metadata {public static int run(int group){int h=1;Locale l=new Locale("lt");
                int[] bases={105,106,303,616,1110,1112,7522,7725,7883,8305,97,304,0x10428,0xd800};
                for(int base:bases)for(int cc=group*256;cc<(group+1)*256;cc+=13){
                    String s=new String(Character.toChars(base))+new String(Character.toChars(cc))+"̇̇";
                    h=31*h+s.toUpperCase(l).hashCode();
                }
                for(int base:bases)h=31*h+(new String(Character.toChars(base))+"̇").toUpperCase(l).hashCode();
                return h;
            }}
            """);compare(256);
    }

    @Test void defaultUppercase() throws Exception {
        fixture("""
            import java.util.Locale;
            import java.util.function.Function;
            public class Metadata {public static int run(int group){int h=1;Function<String,String> f=String::toUpperCase;
                for(String s:new String[]{"","abc","ABC","Straße","iıİ","ﬃ","ᾀ","𐐨",new String(new char[]{(char)0xd800,120,(char)0xdc00}),"ȧi̇"}) {
                    h=31*h+s.toUpperCase().hashCode();h=31*h+f.apply(s).hashCode();h=31*h+(s==s.toUpperCase()?1:0);
                    h=31*h+(s.toUpperCase().equals(s.toUpperCase(Locale.ROOT))?1:0);
                }return h;
            }}
            """);compare(1);
    }

    @Test void textFormatting() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {
              static int failure(String s){try{String.format(s,"x");return 0;}
               catch(DuplicateFormatFlagsException e){return 1+e.getFlags().hashCode();}
               catch(MissingFormatWidthException e){return 2+e.getFormatSpecifier().hashCode();}
               catch(UnknownFormatConversionException e){return 3+e.getConversion().hashCode();}
               catch(IllegalFormatPrecisionException e){return 4+e.getPrecision();}
               catch(IllegalFormatFlagsException e){return 5+e.getFlags().hashCode();}
               catch(MissingFormatArgumentException e){return 6+e.getFormatSpecifier().hashCode();}
               catch(IllegalFormatWidthException e){return 7+e.getWidth();}
               catch(FormatFlagsConversionMismatchException e){return 8+e.getFlags().hashCode()+e.getConversion();}}
              public static int run(int group){int h=1;
               for(String f:new String[]{"literal","%s","%S","%10.3s","%-10.3S","%2$s %s %<S %1$s","%s%% %n%5%", "%1$s %1$S"})
                for(Object v:new Object[]{null,"Straße","iıİ","𐐨",123})h=31*h+String.format(f,v,"other").hashCode();
               h=31*h+String.format("%100$s %s",(Object[])null).hashCode();
               for(String f:new String[]{"%--s","%-s","%q","%","%.s","%.2%","%0%","%<s","%2$s","%#s","%+s","%5n","%-n","%00s","%2147483648s","%.2147483648s"})h=31*h+failure(f);
               try{String.format((String)null,"x");return 0;}catch(NullPointerException expected){return h;}
              }}
            """);compare(1);
    }

    @Test void generalFormatting() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {
             static int fail(String f,Object value){try{String.format(f,value);return 0;}
              catch(IllegalFormatConversionException e){return e.getConversion()+e.getArgumentClass().getName().hashCode();}
              catch(IllegalFormatCodePointException e){return e.getCodePoint();}
              catch(IllegalFormatPrecisionException e){return e.getPrecision()+19;}
              catch(FormatFlagsConversionMismatchException e){return e.getFlags().hashCode()+e.getConversion();}}
             public static int run(int group){int h=1;
              for(String f:new String[]{"%b/%B","%10.3h/%-10.2H","%.0B %.4H","%1$S %1$B %1$H"})
               for(Object value:new Object[]{null,true,false,0,1,-1,"iıİß"})h=31*h+String.format(f,value,value).hashCode();
              for(Object value:new Object[]{null,(byte)65,(short)105,0x10428,(char)0xd800,0x10ffff})
               for(String f:new String[]{"%c","%C","%5c/%-5C"})h=31*h+String.format(f,value,value).hashCode();
              for(Object value:new Object[]{-1,0x110000,1L,1.0,"x",true})h=31*h+fail("%c",value);
              for(String f:new String[]{"%#b","%+h","%.2c","%0c"})h=31*h+fail(f,65);
              return h;
             }}
            """);compare(1);
    }

    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        List<String> options=new ArrayList<>(List.of("-g","-parameters","--release",release,"-d",directory.toString()));
        if(fixtureClasspath!=null)options.addAll(List.of("-classpath",fixtureClasspath));
        Path oracle=directory.resolve("CaseOracle.java");Files.writeString(oracle,"public class CaseOracle {public static void main(String[] args)throws Exception{if(Runtime.version().feature()!=17)throw new Error();var method=Class.forName(args[0]).getMethod(\"run\",int.class);System.out.print(\"[\");for(int i=0;i<Integer.parseInt(args[1]);i++){if(i>0)System.out.print(\",\");System.out.print(method.invoke(null,i));}System.out.print(\"]\");}}");
        options.add(file.toString());options.add(oracle.toString());
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(String[]::new)));
        return jar(false);
    }

    private byte[] jar(boolean reverse) throws Exception {
        List<Path> files;
        try(var paths=Files.walk(directory)) {
            files=new ArrayList<>(paths.filter(path->path.toString().endsWith(".class") && !path.getFileName().toString().equals("CaseOracle.class")).sorted().toList());
        }
        if(reverse)Collections.reverse(files);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(bytes)) {
            for(Path file:files){jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(Files.readAllBytes(file));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    private void compare(int groups) throws Exception {
        Path java=Path.of(System.getProperty("javaw.jdk17.home",System.getProperty("user.home")+"/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"),"bin/java");
        assertTrue(Files.isExecutable(java),"A JDK 17 oracle is required (javaw.jdk17.home)");
        run(java.toString(),"-Duser.language=","-Duser.country=","-Duser.variant=","-cp",directory.toString(),"CaseOracle",fixtureClassName,Integer.toString(groups));
        String expected=Files.readString(directory.resolve("command.log")).trim();
        for(boolean reverse:new boolean[]{false,true})
            execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),expected);
    }

    private void run(String... arguments) throws Exception {
        Path log=directory.resolve("command.log");Process process=new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(60,TimeUnit.SECONDS)){process.destroyForcibly();fail("Command timed out");}
        assertEquals(0,process.exitValue(),Files.readString(log));
    }
    private void execute(String wat,String expected) throws Exception {
        Path input=directory.resolve("metadata.wat"),output=directory.resolve("metadata.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);run("wat2wasm","--enable-exceptions",input.toString(),"-o",output.toString());
        Files.writeString(script,"""
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            const expected=EXPECTED;
            for(let group=0;group<expected.length;group++)for(let instance=0;instance<2;instance++) {
                const memory=new WebAssembly.Memory({initial:2,maximum:20});let heap=1024,allocations=0;
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    allocations++;
                    const address=heap;heap+=size;
                    while(heap>memory.buffer.byteLength)memory.grow(1);
                    new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                }}}).exports;
                assert.equal(allocations,0);heap=Math.max(heap,e.__exception_heap_base?.value??0);
            """.replace("EXPECTED",expected)+"assert.equal(e['"+fixtureClassName+".run'](group),expected[group],'batch '+group);\n}");
        run("node",script.toString(),output.toString());
    }
}
