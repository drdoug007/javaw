package com.example.javaw.bytecode;

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
            assertTrue(error.getMessage().contains("unsupported dynamic call"),error.getMessage());
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
                "public static double f() { return 4.0; }",
                "public static int f() { return Math.abs(-4); }",
                "public static Class<?> f() { return Fixture.class; }",
                "public static int[] f() { return new int[2]; }",
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
            assertTrue(error.getMessage().contains("Broken.create()LBroken;"),error.getMessage());
            assertTrue(error.getMessage().contains("complete superclass chain"),error.getMessage());
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
