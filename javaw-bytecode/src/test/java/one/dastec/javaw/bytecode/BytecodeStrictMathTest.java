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
import java.util.concurrent.TimeUnit;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeStrictMathTest implements Opcodes {
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
        return jar(false);
    }

    private byte[] jar(boolean reverse) throws Exception {
        List<Path> files;
        try(var paths=Files.walk(directory)) {
            files=new ArrayList<>(paths.filter(path->path.toString().endsWith(".class")).sorted().toList());
        }
        if(reverse)Collections.reverse(files);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(bytes)) {
            for(Path file:files){jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(Files.readAllBytes(file));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    @Test void rootsAndAngles() throws Exception {
        fixture("""
            public class Metadata {
            interface Scale {double apply(double x,int n);}
            interface Fused {double apply(double a,double b,double c);}
            static int d(int h,double x){return 31*h+Long.hashCode(Double.doubleToLongBits(x));}
            static int f(int h,float x){return 31*h+Float.floatToIntBits(x);}
            public static int run(){
             double[] values={0.0,-0.0,1,-1,0.5,-0.5,1.5,-1.5,2.5,-2.5,Double.MIN_VALUE,-Double.MIN_VALUE,Double.MIN_NORMAL,Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN,Double.longBitsToDouble(0xfff8000000000001L)};
             long[] edges={0,Long.MIN_VALUE,Long.MAX_VALUE,-1,1,0x8000000000000001L,0x7fefffffffffffffL,0x0010000000000000L,0x000fffffffffffffL,0x7ff0000000000000L,0xfff0000000000000L,0x7ff8000000000000L,0xfff8000000000001L,0x80000000L,0x7fffffffL,0x7f800000L,0xff800000L,0x7fc00000L,0xffc00001L,0x00800000L,0x007fffffL};
             int h=1;long state=123456789;
             java.util.function.DoubleUnaryOperator op=StrictMath::sqrt;
             for(int k=0;k<250;k++){
             state=state*6364136223846793005L+1442695040888963407L;
             long bits=k<edges.length?edges[k]:state,other=edges[(k*7+1)%edges.length];
             double x=k<values.length?values[k]:Double.longBitsToDouble(bits);
             float f=Float.intBitsToFloat((int)bits);int i=(int)bits,j=(int)other;
             h=d(h,StrictMath.sqrt(x));h=d(h,StrictMath.toRadians(x));h=d(h,StrictMath.toDegrees(x));h=d(h,StrictMath.PI);h=d(h,StrictMath.E);
             h=d(h,op.applyAsDouble(x));
             }
             return h;
            }}
            """);compare(List.of("run"));
    }

    @Test void rounding() throws Exception {
        fixture("""
            public class Metadata {
            interface Scale {double apply(double x,int n);}
            interface Fused {double apply(double a,double b,double c);}
            static int d(int h,double x){return 31*h+Long.hashCode(Double.doubleToLongBits(x));}
            static int f(int h,float x){return 31*h+Float.floatToIntBits(x);}
            public static int run(){
             double[] values={0.0,-0.0,1,-1,0.5,-0.5,1.5,-1.5,2.5,-2.5,Double.MIN_VALUE,-Double.MIN_VALUE,Double.MIN_NORMAL,Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN,Double.longBitsToDouble(0xfff8000000000001L)};
             long[] edges={0,Long.MIN_VALUE,Long.MAX_VALUE,-1,1,0x8000000000000001L,0x7fefffffffffffffL,0x0010000000000000L,0x000fffffffffffffL,0x7ff0000000000000L,0xfff0000000000000L,0x7ff8000000000000L,0xfff8000000000001L,0x80000000L,0x7fffffffL,0x7f800000L,0xff800000L,0x7fc00000L,0xffc00001L,0x00800000L,0x007fffffL};
             int h=1;long state=123456789;
             java.util.function.DoubleUnaryOperator op=StrictMath::rint;
             for(int k=0;k<250;k++){
             state=state*6364136223846793005L+1442695040888963407L;
             long bits=k<edges.length?edges[k]:state,other=edges[(k*7+1)%edges.length];
             double x=k<values.length?values[k]:Double.longBitsToDouble(bits);
             float f=Float.intBitsToFloat((int)bits);int i=(int)bits,j=(int)other;
             h=d(h,StrictMath.floor(x));h=d(h,StrictMath.ceil(x));h=d(h,StrictMath.rint(x));h=31*h+Long.hashCode(StrictMath.round(x));h=31*h+StrictMath.round(f);
             h=d(h,op.applyAsDouble(x));
             }
             return h;
            }}
            """);compare(List.of("run"));
    }

    @Test void absoluteValues() throws Exception {
        fixture("""
            public class Metadata {
            interface Scale {double apply(double x,int n);}
            interface Fused {double apply(double a,double b,double c);}
            static int d(int h,double x){return 31*h+Long.hashCode(Double.doubleToLongBits(x));}
            static int f(int h,float x){return 31*h+Float.floatToIntBits(x);}
            public static int run(){
             double[] values={0.0,-0.0,1,-1,0.5,-0.5,1.5,-1.5,2.5,-2.5,Double.MIN_VALUE,-Double.MIN_VALUE,Double.MIN_NORMAL,Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN,Double.longBitsToDouble(0xfff8000000000001L)};
             long[] edges={0,Long.MIN_VALUE,Long.MAX_VALUE,-1,1,0x8000000000000001L,0x7fefffffffffffffL,0x0010000000000000L,0x000fffffffffffffL,0x7ff0000000000000L,0xfff0000000000000L,0x7ff8000000000000L,0xfff8000000000001L,0x80000000L,0x7fffffffL,0x7f800000L,0xff800000L,0x7fc00000L,0xffc00001L,0x00800000L,0x007fffffL};
             int h=1;long state=123456789;
             java.util.function.DoubleUnaryOperator op=StrictMath::abs;
             for(int k=0;k<250;k++){
             state=state*6364136223846793005L+1442695040888963407L;
             long bits=k<edges.length?edges[k]:state,other=edges[(k*7+1)%edges.length];
             double x=k<values.length?values[k]:Double.longBitsToDouble(bits);
             float f=Float.intBitsToFloat((int)bits);int i=(int)bits,j=(int)other;
             h=d(h,StrictMath.abs(x));h=f(h,StrictMath.abs(f));h=31*h+StrictMath.abs(i);h=31*h+Long.hashCode(StrictMath.abs(bits));try{h=31*h+StrictMath.absExact(i);}catch(ArithmeticException e){h+=17;}try{h=31*h+Long.hashCode(StrictMath.absExact(bits));}catch(ArithmeticException e){h+=19;}
             h=d(h,op.applyAsDouble(x));
             }
             return h;
            }}
            """);compare(List.of("run"));
    }

    @Test void extrema() throws Exception {
        fixture("""
            public class Metadata {
            interface Scale {double apply(double x,int n);}
            interface Fused {double apply(double a,double b,double c);}
            static int d(int h,double x){return 31*h+Long.hashCode(Double.doubleToLongBits(x));}
            static int f(int h,float x){return 31*h+Float.floatToIntBits(x);}
            public static int run(){
             double[] values={0.0,-0.0,1,-1,0.5,-0.5,1.5,-1.5,2.5,-2.5,Double.MIN_VALUE,-Double.MIN_VALUE,Double.MIN_NORMAL,Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN,Double.longBitsToDouble(0xfff8000000000001L)};
             long[] edges={0,Long.MIN_VALUE,Long.MAX_VALUE,-1,1,0x8000000000000001L,0x7fefffffffffffffL,0x0010000000000000L,0x000fffffffffffffL,0x7ff0000000000000L,0xfff0000000000000L,0x7ff8000000000000L,0xfff8000000000001L,0x80000000L,0x7fffffffL,0x7f800000L,0xff800000L,0x7fc00000L,0xffc00001L,0x00800000L,0x007fffffL};
             int h=1;long state=123456789;
             java.util.function.DoubleBinaryOperator op=StrictMath::min;
             for(int k=0;k<250;k++){
             state=state*6364136223846793005L+1442695040888963407L;
             long bits=k<edges.length?edges[k]:state,other=edges[(k*7+1)%edges.length];
             double x=k<values.length?values[k]:Double.longBitsToDouble(bits);
             float f=Float.intBitsToFloat((int)bits);int i=(int)bits,j=(int)other;
             for(double y:values){h=d(h,StrictMath.min(x,y));h=d(h,StrictMath.max(x,y));h=f(h,StrictMath.min(f,(float)y));h=f(h,StrictMath.max(f,(float)y));}h=31*h+StrictMath.min(i,-i)+StrictMath.max(i,-i);h=31*h+Long.hashCode(StrictMath.min(bits,-bits)^StrictMath.max(bits,-bits));
             h=d(h,op.applyAsDouble(x,-0.0));
             }
             return h;
            }}
            """);compare(List.of("run"));
    }

    @Test void signOperations() throws Exception {
        fixture("""
            public class Metadata {
            interface Scale {double apply(double x,int n);}
            interface Fused {double apply(double a,double b,double c);}
            static int d(int h,double x){return 31*h+Long.hashCode(Double.doubleToLongBits(x));}
            static int f(int h,float x){return 31*h+Float.floatToIntBits(x);}
            public static int run(){
             double[] values={0.0,-0.0,1,-1,0.5,-0.5,1.5,-1.5,2.5,-2.5,Double.MIN_VALUE,-Double.MIN_VALUE,Double.MIN_NORMAL,Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN,Double.longBitsToDouble(0xfff8000000000001L)};
             long[] edges={0,Long.MIN_VALUE,Long.MAX_VALUE,-1,1,0x8000000000000001L,0x7fefffffffffffffL,0x0010000000000000L,0x000fffffffffffffL,0x7ff0000000000000L,0xfff0000000000000L,0x7ff8000000000000L,0xfff8000000000001L,0x80000000L,0x7fffffffL,0x7f800000L,0xff800000L,0x7fc00000L,0xffc00001L,0x00800000L,0x007fffffL};
             int h=1;long state=123456789;
             java.util.function.DoubleBinaryOperator op=StrictMath::copySign;
             for(int k=0;k<250;k++){
             state=state*6364136223846793005L+1442695040888963407L;
             long bits=k<edges.length?edges[k]:state,other=edges[(k*7+1)%edges.length];
             double x=k<values.length?values[k]:Double.longBitsToDouble(bits);
             float f=Float.intBitsToFloat((int)bits);int i=(int)bits,j=(int)other;
             h=d(h,StrictMath.signum(x));h=f(h,StrictMath.signum(f));for(double y:values){h=d(h,StrictMath.copySign(x,y));h=f(h,StrictMath.copySign(f,(float)y));}
             h=d(h,op.applyAsDouble(x,Double.NaN));
             }
             return h;
            }}
            """);compare(List.of("run"));
    }

    @Test void exponentsAndUlps() throws Exception {
        fixture("""
            public class Metadata {
            interface Scale {double apply(double x,int n);}
            interface Fused {double apply(double a,double b,double c);}
            static int d(int h,double x){return 31*h+Long.hashCode(Double.doubleToLongBits(x));}
            static int f(int h,float x){return 31*h+Float.floatToIntBits(x);}
            public static int run(){
             double[] values={0.0,-0.0,1,-1,0.5,-0.5,1.5,-1.5,2.5,-2.5,Double.MIN_VALUE,-Double.MIN_VALUE,Double.MIN_NORMAL,Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN,Double.longBitsToDouble(0xfff8000000000001L)};
             long[] edges={0,Long.MIN_VALUE,Long.MAX_VALUE,-1,1,0x8000000000000001L,0x7fefffffffffffffL,0x0010000000000000L,0x000fffffffffffffL,0x7ff0000000000000L,0xfff0000000000000L,0x7ff8000000000000L,0xfff8000000000001L,0x80000000L,0x7fffffffL,0x7f800000L,0xff800000L,0x7fc00000L,0xffc00001L,0x00800000L,0x007fffffL};
             int h=1;long state=123456789;
             java.util.function.DoubleUnaryOperator op=StrictMath::ulp;
             for(int k=0;k<250;k++){
             state=state*6364136223846793005L+1442695040888963407L;
             long bits=k<edges.length?edges[k]:state,other=edges[(k*7+1)%edges.length];
             double x=k<values.length?values[k]:Double.longBitsToDouble(bits);
             float f=Float.intBitsToFloat((int)bits);int i=(int)bits,j=(int)other;
             h=31*h+StrictMath.getExponent(x);h=31*h+StrictMath.getExponent(f);h=d(h,StrictMath.ulp(x));h=f(h,StrictMath.ulp(f));
             h=d(h,op.applyAsDouble(x));
             }
             return h;
            }}
            """);compare(List.of("run"));
    }

    @Test void adjacentValues() throws Exception {
        fixture("""
            public class Metadata {
            interface Scale {double apply(double x,int n);}
            interface Fused {double apply(double a,double b,double c);}
            static int d(int h,double x){return 31*h+Long.hashCode(Double.doubleToLongBits(x));}
            static int f(int h,float x){return 31*h+Float.floatToIntBits(x);}
            public static int run(){
             double[] values={0.0,-0.0,1,-1,0.5,-0.5,1.5,-1.5,2.5,-2.5,Double.MIN_VALUE,-Double.MIN_VALUE,Double.MIN_NORMAL,Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN,Double.longBitsToDouble(0xfff8000000000001L)};
             long[] edges={0,Long.MIN_VALUE,Long.MAX_VALUE,-1,1,0x8000000000000001L,0x7fefffffffffffffL,0x0010000000000000L,0x000fffffffffffffL,0x7ff0000000000000L,0xfff0000000000000L,0x7ff8000000000000L,0xfff8000000000001L,0x80000000L,0x7fffffffL,0x7f800000L,0xff800000L,0x7fc00000L,0xffc00001L,0x00800000L,0x007fffffL};
             int h=1;long state=123456789;
             java.util.function.DoubleBinaryOperator op=StrictMath::nextAfter;
             for(int k=0;k<250;k++){
             state=state*6364136223846793005L+1442695040888963407L;
             long bits=k<edges.length?edges[k]:state,other=edges[(k*7+1)%edges.length];
             double x=k<values.length?values[k]:Double.longBitsToDouble(bits);
             float f=Float.intBitsToFloat((int)bits);int i=(int)bits,j=(int)other;
             h=d(h,StrictMath.nextUp(x));h=d(h,StrictMath.nextDown(x));h=f(h,StrictMath.nextUp(f));h=f(h,StrictMath.nextDown(f));for(double y:values){h=d(h,StrictMath.nextAfter(x,y));h=f(h,StrictMath.nextAfter(f,y));}
             h=d(h,op.applyAsDouble(x,0.0));
             }
             return h;
            }}
            """);compare(List.of("run"));
    }

    @Test void binaryScaling() throws Exception {
        fixture("""
            public class Metadata {
            interface Scale {double apply(double x,int n);}
            interface Fused {double apply(double a,double b,double c);}
            static int d(int h,double x){return 31*h+Long.hashCode(Double.doubleToLongBits(x));}
            static int f(int h,float x){return 31*h+Float.floatToIntBits(x);}
            public static int run(){
             double[] values={0.0,-0.0,1,-1,0.5,-0.5,1.5,-1.5,2.5,-2.5,Double.MIN_VALUE,-Double.MIN_VALUE,Double.MIN_NORMAL,Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN,Double.longBitsToDouble(0xfff8000000000001L)};
             long[] edges={0,Long.MIN_VALUE,Long.MAX_VALUE,-1,1,0x8000000000000001L,0x7fefffffffffffffL,0x0010000000000000L,0x000fffffffffffffL,0x7ff0000000000000L,0xfff0000000000000L,0x7ff8000000000000L,0xfff8000000000001L,0x80000000L,0x7fffffffL,0x7f800000L,0xff800000L,0x7fc00000L,0xffc00001L,0x00800000L,0x007fffffL};
             int h=1;long state=123456789;
             Scale op=StrictMath::scalb;
             for(int k=0;k<250;k++){
             state=state*6364136223846793005L+1442695040888963407L;
             long bits=k<edges.length?edges[k]:state,other=edges[(k*7+1)%edges.length];
             double x=k<values.length?values[k]:Double.longBitsToDouble(bits);
             float f=Float.intBitsToFloat((int)bits);int i=(int)bits,j=(int)other;
             for(int n:new int[]{Integer.MIN_VALUE,-2099,-1075,-1023,-512,-150,-149,-1,0,1,127,512,1023,2099,Integer.MAX_VALUE}){h=d(h,StrictMath.scalb(x,n));h=f(h,StrictMath.scalb(f,n));}
             h=d(h,op.apply(x,-1023));
             }
             return h;
            }}
            """);compare(List.of("run"));
    }

    @Test void integerArithmetic() throws Exception {
        fixture("""
            public class Metadata {
            interface Scale {double apply(double x,int n);}
            interface Fused {double apply(double a,double b,double c);}
            static int d(int h,double x){return 31*h+Long.hashCode(Double.doubleToLongBits(x));}
            static int f(int h,float x){return 31*h+Float.floatToIntBits(x);}
            public static int run(){
             double[] values={0.0,-0.0,1,-1,0.5,-0.5,1.5,-1.5,2.5,-2.5,Double.MIN_VALUE,-Double.MIN_VALUE,Double.MIN_NORMAL,Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN,Double.longBitsToDouble(0xfff8000000000001L)};
             long[] edges={0,Long.MIN_VALUE,Long.MAX_VALUE,-1,1,0x8000000000000001L,0x7fefffffffffffffL,0x0010000000000000L,0x000fffffffffffffL,0x7ff0000000000000L,0xfff0000000000000L,0x7ff8000000000000L,0xfff8000000000001L,0x80000000L,0x7fffffffL,0x7f800000L,0xff800000L,0x7fc00000L,0xffc00001L,0x00800000L,0x007fffffL};
             int h=1;long state=123456789;
             java.util.function.LongBinaryOperator op=StrictMath::multiplyHigh;
             for(int k=0;k<600;k++){
             state=state*6364136223846793005L+1442695040888963407L;
             long bits=k<edges.length*edges.length?edges[k%edges.length]:state,other=edges[(k/edges.length)%edges.length];
             double x=k<values.length?values[k]:Double.longBitsToDouble(bits);
             float f=Float.intBitsToFloat((int)bits);int i=(int)bits,j=(int)other;
             try{h=31*h+StrictMath.addExact(i,j);}catch(ArithmeticException e){h=31*h+23;}try{h=31*h+Long.hashCode(StrictMath.addExact(bits,other));}catch(ArithmeticException e){h=31*h+23;}try{h=31*h+StrictMath.subtractExact(i,j);}catch(ArithmeticException e){h=31*h+23;}try{h=31*h+Long.hashCode(StrictMath.subtractExact(bits,other));}catch(ArithmeticException e){h=31*h+23;}try{h=31*h+StrictMath.multiplyExact(i,j);}catch(ArithmeticException e){h=31*h+23;}try{h=31*h+Long.hashCode(StrictMath.multiplyExact(bits,other));}catch(ArithmeticException e){h=31*h+23;}try{h=31*h+StrictMath.floorDiv(i,j);}catch(ArithmeticException e){h=31*h+23;}try{h=31*h+Long.hashCode(StrictMath.floorDiv(bits,other));}catch(ArithmeticException e){h=31*h+23;}try{h=31*h+StrictMath.floorMod(i,j);}catch(ArithmeticException e){h=31*h+23;}try{h=31*h+Long.hashCode(StrictMath.floorMod(bits,other));}catch(ArithmeticException e){h=31*h+23;}try{h=31*h+StrictMath.incrementExact(i);}catch(ArithmeticException e){h=31*h+29;}try{h=31*h+Long.hashCode(StrictMath.incrementExact(bits));}catch(ArithmeticException e){h=31*h+29;}try{h=31*h+StrictMath.decrementExact(i);}catch(ArithmeticException e){h=31*h+29;}try{h=31*h+Long.hashCode(StrictMath.decrementExact(bits));}catch(ArithmeticException e){h=31*h+29;}try{h=31*h+StrictMath.negateExact(i);}catch(ArithmeticException e){h=31*h+29;}try{h=31*h+Long.hashCode(StrictMath.negateExact(bits));}catch(ArithmeticException e){h=31*h+29;}try{h=31*h+StrictMath.toIntExact(bits);}catch(ArithmeticException e){h+=31;}h=31*h+Long.hashCode(StrictMath.multiplyFull(i,j));h=31*h+Long.hashCode(StrictMath.multiplyHigh(bits,other));try{h=31*h+Long.hashCode(StrictMath.multiplyExact(bits,j));}catch(ArithmeticException e){h+=37;}try{h=31*h+Long.hashCode(StrictMath.floorDiv(bits,j));h=31*h+StrictMath.floorMod(bits,j);}catch(ArithmeticException e){h+=41;}
             h=d(h,(double)op.applyAsLong(bits,other));
             }
             return h;
            }}
            """);compare(List.of("run"));
    }

    @Test void fusedMultiplyAdd() throws Exception {
        fixture("""
            public class Metadata {
            interface Scale {double apply(double x,int n);}
            interface Fused {double apply(double a,double b,double c);}
            static int d(int h,double x){return 31*h+Long.hashCode(Double.doubleToLongBits(x));}
            static int f(int h,float x){return 31*h+Float.floatToIntBits(x);}
            public static int run(){
             double[] values={0.0,-0.0,1,-1,0.5,-0.5,1.5,-1.5,2.5,-2.5,Double.MIN_VALUE,-Double.MIN_VALUE,Double.MIN_NORMAL,Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN,Double.longBitsToDouble(0xfff8000000000001L)};
             long[] edges={0,Long.MIN_VALUE,Long.MAX_VALUE,-1,1,0x8000000000000001L,0x7fefffffffffffffL,0x0010000000000000L,0x000fffffffffffffL,0x7ff0000000000000L,0xfff0000000000000L,0x7ff8000000000000L,0xfff8000000000001L,0x80000000L,0x7fffffffL,0x7f800000L,0xff800000L,0x7fc00000L,0xffc00001L,0x00800000L,0x007fffffL};
             int h=1;long state=123456789;
             Fused op=StrictMath::fma;
             for(int k=0;k<250;k++){
             state=state*6364136223846793005L+1442695040888963407L;
             long bits=k<edges.length?edges[k]:state,other=edges[(k*7+1)%edges.length];
             double x=k<values.length?values[k]:Double.longBitsToDouble(bits);
             float f=Float.intBitsToFloat((int)bits);int i=(int)bits,j=(int)other;
             double y=values[k%values.length],z=values[(k*7+3)%values.length];h=d(h,StrictMath.fma(x,y,z));h=f(h,StrictMath.fma(f,(float)y,(float)z));h=d(h,StrictMath.fma(x,1.0000000000000002,-x));h=f(h,StrictMath.fma(f,1.0000001f,-f));
             h=d(h,op.apply(x,1.0000000000000002,-x));
             }
             return h;
            }}
            """);compare(List.of("run"));
    }

    @Test void malformedDescriptorsAndUnboundRandomAreRejected() {
        for(String name:List.of("sqrt","copySign","scalb","fma","random")) {
            ClassWriter writer=new ClassWriter(0);
            writer.visit(V17,ACC_PUBLIC,"BadStrictMath",null,"java/lang/Object",null);
            MethodVisitor method=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"run","()D",null,null);
            method.visitCode();
            method.visitMethodInsn(INVOKESTATIC,"java/lang/StrictMath",name,"()D",false);
            method.visitInsn(DRETURN);method.visitMaxs(2,0);method.visitEnd();writer.visitEnd();
            var failure=assertThrows(IllegalArgumentException.class,
                ()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()));
            assertTrue(failure.getMessage().contains(name),failure.getMessage());
        }
    }

    private void compare(List<String> names) throws Exception {
        StringBuilder assertions=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
            Class<?> type=loader.loadClass(fixtureClassName);for(String name:names){Object expected=type.getMethod(name).invoke(null);assertions.append("assert.equal(").append(expected instanceof String?"text(e['"+fixtureClassName+".":"e['"+fixtureClassName+".").append(name).append(expected instanceof String?"']()),":"'](),").append(expected instanceof String?js((String)expected):expected).append(");\n");}
        }
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),assertions.toString());
    }

    private String js(String value) {
        if(value==null)return "null";
        StringBuilder out=new StringBuilder("\"");
        for(char c:value.toCharArray())out.append(String.format("\\u%04x",(int)c));
        return out.append('"').toString();
    }
    private void run(String... arguments) throws Exception {
        Path log=directory.resolve("command.log");Process process=new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(60,TimeUnit.SECONDS)){process.destroyForcibly();fail("Command timed out");}
        assertEquals(0,process.exitValue(),Files.readString(log));
    }
    private void execute(String wat,String assertions) throws Exception {
        Path input=directory.resolve("metadata.wat"),output=directory.resolve("metadata.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);run("wat2wasm","--enable-exceptions",input.toString(),"-o",output.toString());
        Files.writeString(script,"""
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            for(let instance=0;instance<2;instance++) {
                const memory=new WebAssembly.Memory({initial:2});let heap=1024,allocations=0,failAllocation=false;
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    allocations++;if(failAllocation){failAllocation=false;return 0;}
                    const address=heap;heap+=size;
                    while(heap>memory.buffer.byteLength)memory.grow(1);
                    new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                }}}).exports;
                assert.equal(allocations,0);assert.equal(e.__gc_heap_bytes?.()??0,0);if(e.__gc_scribble)e.__gc_scribble.value=1;heap=Math.max(heap,e.__exception_heap_base?.value??0);
                function text(pointer){if(!pointer)return null;const view=new DataView(memory.buffer);
                    const chars=view.getInt32(pointer+4,true),length=view.getInt32(chars+4,true);
                    let result='';for(let i=0;i<length;i++)result+=String.fromCharCode(view.getUint16(chars+8+2*i,true));return result;}
            """+assertions+"\n}");
        run("node",script.toString(),output.toString());
    }
}
