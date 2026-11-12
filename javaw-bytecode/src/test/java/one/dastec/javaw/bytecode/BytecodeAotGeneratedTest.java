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

class BytecodeAotGeneratedTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClasspath;

    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        List<String> options=new ArrayList<>(List.of("-g","--release",release,"-d",directory.toString()));
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

    @Test void executesUnchangedPinnedByteBuddyGeneratedSubclassAndFieldAccessors() throws Exception {
        fixture("""
            public class Metadata {
                public static int calls;
                public static abstract class Base {
                    private final int bias;
                    public Base(int bias){if(bias<0)throw new IllegalArgumentException("constructor");this.bias=bias;}
                    public int bias(){return bias;}
                    public int work(int value){return bias+value;}
                    public abstract long getExtra();public abstract void setExtra(long value);
                }
                public static class Interceptor {public static int intercept(int value){calls++;return value*3;}}
                public static Base create(int value){return null;}
                public static long run(){Base value=create(9);value.setExtra(9007199254740993L);return value.getExtra()+value.work(7)+value.bias();}
                public static int failure(){try{create(-1);return 0;}catch(IllegalArgumentException expected){return 1;}}
                public static int calls(){return calls;}
                public static int identity(){Base a=create(1),b=create(1);return a!=b && a.equals(a) && !a.equals(b)?1:0;}
            }
            """);
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path byteBuddy=repository.resolve("net/bytebuddy/byte-buddy/1.14.11/byte-buddy-1.14.11.jar");
        assertEquals("62ae28187ed2b062813da6a9d567bfee733c341582699b62dd980230729a0313",
            java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(byteBuddy))));
        Path tools=Files.createTempDirectory("javaw-aot-builder-");
        try {
            Path source=tools.resolve("Build.java");Files.writeString(source,"""
                import net.bytebuddy.ByteBuddy;
                import net.bytebuddy.implementation.*;
                import static net.bytebuddy.matcher.ElementMatchers.*;
                public class Build {
                    public static void main(String[] args) throws Exception {
                        new ByteBuddy().subclass(Metadata.Base.class).name("Generated")
                            .defineField("extra",long.class,java.lang.reflect.Modifier.PRIVATE)
                            .method(named("work")).intercept(MethodCall.invoke(Metadata.Interceptor.class.getMethod("intercept",int.class)).withArgument(0))
                            .method(named("getExtra")).intercept(FieldAccessor.ofField("extra"))
                            .method(named("setExtra")).intercept(FieldAccessor.ofField("extra"))
                            .make().saveIn(new java.io.File(args[0]));
                    }
                }
                """);
            String classpath=directory+File.pathSeparator+byteBuddy;
            assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-classpath",classpath,"-d",tools.toString(),source.toString()));
            run(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-classpath",tools+File.pathSeparator+classpath,"Build",directory.toString());
        } finally {try(var paths=Files.walk(tools)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
        byte[] generated=Files.readAllBytes(directory.resolve("Generated.class"));
        Path file=directory.resolve("Metadata.class");ClassNode type=new ClassNode();new ClassReader(Files.readAllBytes(file)).accept(type,0);
        MethodNode create=type.methods.stream().filter(m->m.name.equals("create")).findFirst().orElseThrow();create.instructions.clear();
        create.visitTypeInsn(NEW,"Generated");create.visitInsn(DUP);create.visitVarInsn(ILOAD,0);create.visitMethodInsn(INVOKESPECIAL,"Generated","<init>","(I)V",false);create.visitInsn(ARETURN);
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);type.accept(writer);Files.write(file,writer.toByteArray());
        StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            for(String method:List.of("run","failure","identity","calls")) {
                Object expected=jvm.getMethod(method).invoke(null);checks.append("assert.equal(e['Metadata.").append(method).append("'](),").append(expected).append(expected instanceof Long?"n);\n":");\n");
            }
        }
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),checks.toString());
        assertArrayEquals(generated,Files.readAllBytes(directory.resolve("Generated.class")),"generated artifact must remain unchanged");
    }

    @Test void comparesSelectedAotPlansWithPinnedSpringAopAndHibernateProxyAndEnhancement() throws Exception {
        fixture("""
            import java.lang.reflect.*;
            public class Metadata {
                public interface Service {int work(int value);}
                public static class Target implements Service {public int work(int value){if(value<0)throw new IllegalArgumentException();return value*3;}}
                public static class Advice implements InvocationHandler {
                    final Target target=new Target();int calls;
                    public Object invoke(Object proxy,Method method,Object[] args) throws Throwable {
                        calls++;try{return ((Integer)method.invoke(target,args))+5;}
                        catch(InvocationTargetException error){throw error.getCause();}
                    }
                }
                public static int spring(){
                    Advice advice=new Advice();Service proxy=(Service)Proxy.newProxyInstance(Metadata.class.getClassLoader(),new Class<?>[]{Service.class},advice);
                    int value=proxy.work(7);try{proxy.work(-1);}catch(IllegalArgumentException expected){value+=100;}
                    return value+advice.calls*1000;
                }
                public static class Entity {
                    private Long id;private int value;
                    public Entity(){}public Long getId(){return id;}public void setId(Long id){this.id=id;}
                    public int getValue(){return value;}public void setValue(int value){this.value=value;}
                }
                // Explicit AOT lazy-reference plan: identifier access is load-free; initialization is supplied by the application.
                public static class Reference extends Entity {
                    final Long id;Entity implementation;
                    Reference(Long id){this.id=id;}
                    public Long getId(){return id;}
                    public int getValue(){if(implementation==null)throw new IllegalStateException();return implementation.getValue();}
                    public void setValue(int value){if(implementation==null)throw new IllegalStateException();implementation.setValue(value);}
                }
                public static int hibernateProxy(){
                    Reference proxy=new Reference(17L);int value=proxy.getId().intValue();
                    if(proxy.implementation==null)value+=100;
                    try{proxy.getValue();}catch(IllegalStateException expected){value+=1000;}
                    Entity entity=new Entity();entity.setValue(9);proxy.implementation=entity;
                    value+=proxy.getValue();proxy.setValue(12);return value+entity.getValue()*10000;
                }
                // Explicit AOT dirty-state plan for the one persistent int attribute selected by this test.
                public static class DirtyEntity {
                    int value;boolean dirty;
                    void setValue(int next){if(value!=next)dirty=true;value=next;}
                    void clear(){dirty=false;}
                }
                public static int hibernateEnhancement(){
                    DirtyEntity entity=new DirtyEntity();int trace=entity.dirty?1:0;
                    entity.setValue(9);trace=trace*10+(entity.dirty?1:0);entity.clear();trace=trace*10+(entity.dirty?1:0);
                    entity.setValue(9);trace=trace*10+(entity.dirty?1:0);entity.setValue(12);return trace*10+(entity.dirty?1:0);
                }
            }
            """);
        Path tools=Files.createTempDirectory("javaw-framework-oracle-");
        try {
            String classpath=directory+File.pathSeparator+pinnedClasspath();
            Path entity=tools.resolve("OrmEntity.java");Files.writeString(entity,"""
                @jakarta.persistence.Entity public class OrmEntity {
                    @jakarta.persistence.Id private Long id;private int value;
                    public OrmEntity(){}public int getValue(){return value;}public void setValue(int value){this.value=value;}
                }
                """);
            Path source=tools.resolve("FrameworkOracle.java");Files.writeString(source,"""
                import java.nio.file.*;import java.net.*;import java.util.*;
                public class FrameworkOracle {
                    public static void main(String[] args) throws Exception {
                        org.springframework.aop.framework.ProxyFactory spring=new org.springframework.aop.framework.ProxyFactory(new Metadata.Target());
                        int[] calls={0};spring.addAdvice((org.aopalliance.intercept.MethodInterceptor) invocation->{calls[0]++;return ((Integer)invocation.proceed())+5;});
                        Metadata.Service service=(Metadata.Service)spring.getProxy();int sv=service.work(7);
                        try{service.work(-1);}catch(IllegalArgumentException expected){sv+=100;}sv+=calls[0]*1000;
                        var provider=new org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl();
                        var factory=new org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyFactory(provider.getByteBuddyProxyHelper());
                        factory.postInstantiate("Entity",Metadata.Entity.class,new HashSet<>(List.of(org.hibernate.proxy.HibernateProxy.class)),
                            Metadata.Entity.class.getMethod("getId"),Metadata.Entity.class.getMethod("setId",Long.class),null);
                        var hp=factory.getProxy(17L,null);var lazy=hp.getHibernateLazyInitializer();Metadata.Entity proxy=(Metadata.Entity)hp;
                        int hv=proxy.getId().intValue();if(lazy.isUninitialized())hv+=100;
                        try{proxy.getValue();}catch(org.hibernate.LazyInitializationException expected){hv+=1000;}
                        Metadata.Entity target=new Metadata.Entity();target.setValue(9);lazy.setImplementation(target);
                        hv+=proxy.getValue();proxy.setValue(12);hv+=target.getValue()*10000;
                        Path root=Path.of(args[0]);byte[] original=Files.readAllBytes(root.resolve("OrmEntity.class"));
                        var context=new org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext(){
                            public boolean hasLazyLoadableAttributes(org.hibernate.bytecode.enhance.spi.UnloadedClass type){return false;}
                        };
                        byte[] enhanced=provider.getEnhancer(context).enhance("OrmEntity",original);
                        if(enhanced==null || Arrays.equals(original,enhanced))throw new AssertionError("no real enhancement");
                        Class<?> type=new ClassLoader(FrameworkOracle.class.getClassLoader()){
                            Class<?> define(){return defineClass("OrmEntity",enhanced,0,enhanced.length);}
                        }.define();
                        Object object=type.getConstructor().newInstance();var dirty=(org.hibernate.engine.spi.SelfDirtinessTracker)object;
                        int ev=dirty.$$_hibernate_hasDirtyAttributes()?1:0;
                        type.getMethod("setValue",int.class).invoke(object,9);ev=ev*10+(dirty.$$_hibernate_hasDirtyAttributes()?1:0);
                        if(!Arrays.equals(new String[]{"value"},dirty.$$_hibernate_getDirtyAttributes()))throw new AssertionError("dirty attribute name");
                        dirty.$$_hibernate_clearDirtyAttributes();ev=ev*10+(dirty.$$_hibernate_hasDirtyAttributes()?1:0);
                        type.getMethod("setValue",int.class).invoke(object,9);ev=ev*10+(dirty.$$_hibernate_hasDirtyAttributes()?1:0);
                        type.getMethod("setValue",int.class).invoke(object,12);ev=ev*10+(dirty.$$_hibernate_hasDirtyAttributes()?1:0);
                        Files.writeString(Path.of(args[1]),sv+"\\n"+hv+"\\n"+ev+"\\n");
                    }
                }
                """);
            assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-classpath",classpath,"-d",tools.toString(),entity.toString(),source.toString()));
            Path results=tools.resolve("results.txt");
            run(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-classpath",tools+File.pathSeparator+classpath,"FrameworkOracle",tools.toString(),results.toString());
            List<String> expected=Files.readAllLines(results);assertEquals(3,expected.size());
            String checks="assert.equal(e['Metadata.spring'](),"+expected.get(0)+");\n"+
                "assert.equal(e['Metadata.hibernateProxy'](),"+expected.get(1)+");\n"+
                "assert.equal(e['Metadata.hibernateEnhancement'](),"+expected.get(2)+");\n";
            for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse)),List.of(List.of("Metadata$Service"))),checks);
        } finally {try(var paths=Files.walk(tools)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
    }

    private String pinnedClasspath() throws Exception {
        Path lock=Path.of("runtime-target/dependencies.lock.json");if(!Files.exists(lock))lock=Path.of("../runtime-target/dependencies.lock.json");
        String json=Files.readString(lock);List<String> jars=new ArrayList<>();
        var entries=java.util.regex.Pattern.compile("\\{[^{}]*groupId[^{}]*}").matcher(json);
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        while(entries.find()) {
            Map<String,String> fields=new HashMap<>();var values=java.util.regex.Pattern.compile("\"(groupId|artifactId|version|sha256)\"\\s*:\\s*\"([^\"]+)\"").matcher(entries.group());
            while(values.find())fields.put(values.group(1),values.group(2));
            String artifact=fields.get("artifactId"),version=fields.get("version");
            Path jar=repository.resolve(fields.get("groupId").replace('.','/')).resolve(artifact).resolve(version).resolve(artifact+"-"+version+".jar");
            assertEquals(fields.get("sha256"),java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))),jar.toString());jars.add(jar.toString());
        }
        assertEquals(63,jars.size());return String.join(File.pathSeparator,jars);
    }

    @Test void diagnosesGenerationFactoriesAndInheritedClassDefinitionAtOriginalSites() throws Exception {
        for(String owner:List.of("org/springframework/cglib/proxy/Enhancer","net/bytebuddy/dynamic/DynamicType$Builder","net/bytebuddy/dynamic/DynamicType$Unloaded",
                "org/springframework/cglib/core/ReflectUtils","org/hibernate/bytecode/internal/bytebuddy/ByteBuddyState",
                "org/hibernate/proxy/pojo/bytebuddy/ByteBuddyProxyHelper","java/lang/invoke/MethodHandles$Lookup","sun/misc/Unsafe")) {
            String name=owner.endsWith("Enhancer")?"create":owner.endsWith("Builder")?"make":owner.endsWith("ReflectUtils")?"defineClass":
                owner.endsWith("ByteBuddyProxyHelper")?"buildProxy":owner.endsWith("Lookup")?"defineHiddenClass":owner.endsWith("Unsafe")?"defineAnonymousClass":"load";
            ClassWriter writer=new ClassWriter(0);writer.visit(V17,ACC_PUBLIC,"Metadata",null,"java/lang/Object",null);writer.visitSource("Metadata.java",null);
            MethodVisitor code=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"run","()Ljava/lang/Object;",null,null);code.visitCode();Label line=new Label();code.visitLabel(line);code.visitLineNumber(9,line);
            code.visitInsn(ACONST_NULL);code.visitMethodInsn(INVOKEVIRTUAL,owner,name,"()Ljava/lang/Object;",false);code.visitInsn(ARETURN);code.visitMaxs(1,0);code.visitEnd();writer.visitEnd();
            String error=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray())).getMessage();
            assertTrue(error.contains("Metadata.java:9"),error);assertTrue(error.contains("generate and package classes on the build JVM"),error);
        }
        byte[] jar=fixture("""
            public class Metadata extends ClassLoader {
                public Class<?> run(byte[] bytes){return defineClass("Generated",bytes,0,bytes.length);}
            }
            """);
        String error=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar))).getMessage();
        assertTrue(error.contains("runtime class definition"),error);assertTrue(error.contains("Metadata.run"),error);
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
        WasmTestSupport.execute(directory,wat,assertions);
    }
}
