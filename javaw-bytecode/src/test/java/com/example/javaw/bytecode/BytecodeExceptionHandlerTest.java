package com.example.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import javax.tools.ToolProvider;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeExceptionHandlerTest implements Opcodes {
    @TempDir Path directory;

    private ClassNode type(String name, String parent) {
        ClassNode node = new ClassNode(); node.version = V17; node.access = ACC_PUBLIC;
        node.name = name; node.superName = parent; node.sourceFile = "HandlerFixture.java";
        return node;
    }

    private Map<String, ClassNode> linked(ClassNode owner) {
        Map<String, ClassNode> types = new LinkedHashMap<>(); types.put(owner.name, owner);
        for (String[] entry : new String[][]{{"java/lang/Throwable","java/lang/Object"},
                {"java/lang/Exception","java/lang/Throwable"}, {"java/lang/RuntimeException","java/lang/Exception"},
                {"java/lang/IllegalArgumentException","java/lang/RuntimeException"}, {"java/lang/String","java/lang/Object"}})
            types.put(entry[0], type(entry[0], entry[1]));
        return types;
    }

    private ClassNode fixture(String body) throws Exception {
        Path source = directory.resolve("HandlerFixture.java");
        Files.writeString(source, "public class HandlerFixture {" + body + "}");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null,null,null,"-g","-d",directory.toString(),source.toString()));
        ClassNode owner = new ClassNode(); new ClassReader(Files.readAllBytes(directory.resolve("HandlerFixture.class"))).accept(owner,0);
        return owner;
    }

    private byte[] bytes(ClassNode owner) {
        ClassWriter writer = new ClassWriter(0); owner.accept(writer); return writer.toByteArray();
    }

    private void jvm(ClassNode owner) {
        byte[] bytes = bytes(owner);
        new ClassLoader(null) { Class<?> load() { return defineClass(null,bytes,0,bytes.length); } }.load().getDeclaredMethods();
    }

    private void analyze(ClassNode owner) throws Exception {
        for (MethodNode method : owner.methods) BytecodeFrames.analyze(owner,method,linked(owner));
    }

    private MethodNode run(ClassNode owner) { return owner.methods.stream().filter(m -> m.name.equals("run")).findFirst().orElseThrow(); }

    @Test void verifiesJavacOrderedMultiCatchFinallyRethrowAndWideLocals() throws Exception {
        ClassNode owner = fixture("""
            static int ticks;
            public static long run(int n, long value, double other) {
                long result = value;
                try {
                    result = value / n;
                    if (n == 7) throw new IllegalArgumentException();
                    return result + (long)other;
                } catch (IllegalArgumentException e) { return result + 10; }
                  catch (RuntimeException e) { throw e; }
                  finally { ticks++; }
            }
            public static int multi(int n) {
                try { if (n == 1) throw new IllegalArgumentException(); return 10 / n; }
                catch (IllegalArgumentException | ArithmeticException e) { return 7; }
            }
            """);
        Map<String,ClassNode> types=linked(owner);
        types.put("java/lang/ArithmeticException",type("java/lang/ArithmeticException","java/lang/RuntimeException"));
        assertDoesNotThrow(() -> jvm(owner));
        for(int flags : new int[]{0,ClassReader.EXPAND_FRAMES}) {
            ClassNode parsed=new ClassNode();new ClassReader(bytes(owner)).accept(parsed,flags);
            types.put(parsed.name,parsed);
            for(MethodNode method:parsed.methods) assertDoesNotThrow(()->BytecodeFrames.analyze(parsed,method,types),method.name);
        }
    }

    @Test void rejectsMissingAndIncompatibleHandlerStackMapsLikeJvm() throws Exception {
        for(String mutation : new String[]{"missing","empty","extra","type","local"}) {
            ClassNode owner=fixture("public static int run(int n){try{return 10/n;}catch(RuntimeException e){return n;}}");
            MethodNode method=run(owner);
            FrameNode frame=null;
            for(AbstractInsnNode instruction:method.instructions) if(instruction instanceof FrameNode found) frame=found;
            assertNotNull(frame);
            switch(mutation) {
                case "missing" -> method.instructions.remove(frame);
                case "empty" -> {frame.type=F_FULL;frame.local=new ArrayList<>(List.of(INTEGER));frame.stack=new ArrayList<>();}
                case "extra" -> {frame.type=F_FULL;frame.local=new ArrayList<>(List.of(INTEGER));frame.stack=new ArrayList<>(List.of("java/lang/RuntimeException",INTEGER));}
                case "type" -> frame.stack=new ArrayList<>(List.of("java/lang/String"));
                case "local" -> {frame.type=F_FULL;frame.local=new ArrayList<>(List.of(FLOAT));}
            }
            assertThrows(LinkageError.class,()->jvm(owner),mutation);
            assertThrows(Exception.class,()->analyze(owner),mutation);
        }
    }

    @Test void validatesEveryTableRangeLabelAndCatchTypeWithoutCallSites() throws Exception {
        for(String mutation:new String[]{"empty","sameOffset","reverse","startEnd","handlerEnd","foreignStart","foreignEnd","foreignHandler",
                "array","notThrowable","missing","interface","cycle","catchAllMissingRoot"}) {
            ClassNode owner=fixture("public static int run(int n){try{return 10/n;}catch(RuntimeException e){return n;}}");
            MethodNode method=run(owner);TryCatchBlockNode table=method.tryCatchBlocks.get(0);
            Map<String,ClassNode> types=linked(owner);
            LabelNode end=new LabelNode();method.instructions.add(end);
            switch(mutation) {
                case "empty" -> table.end=table.start;
                case "sameOffset" -> {LabelNode next=new LabelNode();method.instructions.insert(table.start,next);table.end=next;}
                case "reverse" -> {LabelNode temp=table.start;table.start=table.end;table.end=temp;}
                case "startEnd" -> table.start=end;
                case "handlerEnd" -> table.handler=end;
                case "foreignStart" -> table.start=new LabelNode();
                case "foreignEnd" -> table.end=new LabelNode();
                case "foreignHandler" -> table.handler=new LabelNode();
                case "array" -> table.type="[I";
                case "notThrowable" -> table.type="java/lang/String";
                case "missing" -> table.type="missing/Exception";
                case "interface" -> types.get(table.type).access|=ACC_INTERFACE|ACC_ABSTRACT;
                case "cycle" -> types.get(table.type).superName=table.type;
                case "catchAllMissingRoot" -> {table.type=null;types.remove("java/lang/Throwable");}
            }
            String message=assertThrows(IllegalArgumentException.class,()->BytecodeFrames.analyze(owner,method,types),mutation).getMessage();
            assertTrue(message.contains("exception handler table"),message);
            if(Set.of("empty","sameOffset","reverse","startEnd","handlerEnd","array","notThrowable").contains(mutation))
                assertThrows(LinkageError.class,()->jvm(owner),mutation);
        }
    }

    private ClassNode allocationHandler(boolean reuseAlias) {
        ClassNode owner=type("HandlerFixture","java/lang/Object");
        MethodNode init=new MethodNode(ACC_PUBLIC,"<init>","()V",null,null);init.maxLocals=1;init.maxStack=1;
        init.instructions.add(new VarInsnNode(ALOAD,0));init.instructions.add(new MethodInsnNode(INVOKESPECIAL,"java/lang/Object","<init>","()V",false));
        init.instructions.add(new InsnNode(RETURN));owner.methods.add(init);
        MethodNode method=new MethodNode(ACC_PUBLIC|ACC_STATIC,"run","()V",null,null);method.maxLocals=2;method.maxStack=2;owner.methods.add(method);
        var code=method.instructions;LabelNode start=new LabelNode(),end=new LabelNode(),handler=new LabelNode();
        code.add(new TypeInsnNode(NEW,owner.name));code.add(new InsnNode(DUP));code.add(new VarInsnNode(ASTORE,0));
        code.add(start);code.add(new MethodInsnNode(INVOKESPECIAL,owner.name,"<init>","()V",false));code.add(end);code.add(new InsnNode(RETURN));
        code.add(handler);code.add(new FrameNode(F_FULL,0,null,1,new Object[]{"java/lang/Throwable"}));code.add(new VarInsnNode(ASTORE,1));
        if(reuseAlias) {code.add(new VarInsnNode(ALOAD,0));code.add(new MethodInsnNode(INVOKESPECIAL,owner.name,"<init>","()V",false));}
        code.add(new InsnNode(RETURN));method.tryCatchBlocks.add(new TryCatchBlockNode(start,end,handler,null));return owner;
    }

    @Test void invalidatesFailedNewConstructorAliasesAndAcceptsDiscardingThem() {
        for(boolean reuse:new boolean[]{false,true}) {
            ClassNode owner=allocationHandler(reuse);
            if(reuse) {assertThrows(LinkageError.class,()->jvm(owner));assertThrows(Exception.class,()->analyze(owner));}
            else {assertDoesNotThrow(()->jvm(owner));assertDoesNotThrow(()->analyze(owner));}
        }
    }

    @Test void checksExceptionalLocalsBeforeNonThrowingStoresAndReplacesOperandStack() {
        ClassNode owner=type("HandlerFixture","java/lang/Object");owner.version=V1_5;
        MethodNode method=new MethodNode(ACC_PUBLIC|ACC_STATIC,"run","()V",null,null);method.maxLocals=1;method.maxStack=2;owner.methods.add(method);
        LabelNode start=new LabelNode(),end=new LabelNode(),handler=new LabelNode();var code=method.instructions;
        code.add(new InsnNode(FCONST_0));code.add(new VarInsnNode(FSTORE,0));code.add(new InsnNode(ICONST_1));
        code.add(start);code.add(new VarInsnNode(ISTORE,0));code.add(end);code.add(new InsnNode(RETURN));
        code.add(handler);code.add(new InsnNode(POP));code.add(new VarInsnNode(ILOAD,0));code.add(new InsnNode(POP));code.add(new InsnNode(RETURN));
        method.tryCatchBlocks.add(new TryCatchBlockNode(start,end,handler,null));
        assertThrows(LinkageError.class,()->jvm(owner));assertThrows(Exception.class,()->analyze(owner));
        // A handler starts with just the exception, regardless of the protected instruction's input stack.
        code.remove(code.get(code.size()-3));code.remove(code.get(code.size()-2));
        assertDoesNotThrow(()->jvm(owner));assertDoesNotThrow(()->analyze(owner));
    }

    @Test void rejectsHandlerMapsThatKeepFailedAllocationAliases() {
        for(boolean initialized:new boolean[]{false,true}) {
            ClassNode owner=allocationHandler(false);MethodNode method=run(owner);
            LabelNode allocation=new LabelNode();method.instructions.insertBefore(method.instructions.getFirst(),allocation);
            for(AbstractInsnNode instruction:method.instructions) if(instruction instanceof FrameNode frame)
                frame.local=new ArrayList<>(List.of(initialized ? owner.name : allocation));
            assertThrows(LinkageError.class,()->jvm(owner));assertThrows(Exception.class,()->analyze(owner));
        }
    }

    @Test void verifiesHandlersAcrossThisOrSuperInitializationWithoutRevivingFailedThis() {
        ClassNode owner=type("HandlerFixture","java/lang/Object");owner.version=V1_5;
        MethodNode init=new MethodNode(ACC_PUBLIC,"<init>","()V",null,null);init.maxLocals=1;init.maxStack=1;owner.methods.add(init);
        LabelNode start=new LabelNode(),end=new LabelNode(),handler=new LabelNode();var code=init.instructions;
        code.add(new VarInsnNode(ALOAD,0));code.add(start);
        code.add(new MethodInsnNode(INVOKESPECIAL,"java/lang/Object","<init>","()V",false));
        code.add(end);code.add(new InsnNode(RETURN));code.add(handler);code.add(new InsnNode(ATHROW));
        init.tryCatchBlocks.add(new TryCatchBlockNode(start,end,handler,null));
        assertDoesNotThrow(()->jvm(owner));
        assertDoesNotThrow(()->analyze(owner));
        // A failed super call cannot allow normal constructor return or reuse of this.
        code.set(code.getLast(),new InsnNode(RETURN));
        assertThrows(LinkageError.class,()->jvm(owner));assertThrows(Exception.class,()->analyze(owner));
        code.set(code.getLast(),new VarInsnNode(ALOAD,0));code.add(new InsnNode(ATHROW));
        assertThrows(LinkageError.class,()->jvm(owner));assertThrows(Exception.class,()->analyze(owner));
    }

    @Test void rejectsInvalidThrowOperandsLikeJvmAndRequiresLinkedThrowableMetadata() {
        for(String operand:new String[]{"object","integer","array","uninitialized"}) {
            ClassNode owner=type("HandlerFixture","java/lang/Object");
            MethodNode method=new MethodNode(ACC_PUBLIC|ACC_STATIC,"run","(Ljava/lang/Object;)V",null,null);
            method.maxLocals=1;method.maxStack=1;owner.methods.add(method);var code=method.instructions;
            switch(operand) {
                case "object" -> code.add(new VarInsnNode(ALOAD,0));
                case "integer" -> code.add(new InsnNode(ICONST_1));
                case "array" -> {code.add(new InsnNode(ICONST_0));code.add(new IntInsnNode(NEWARRAY,T_INT));}
                case "uninitialized" -> code.add(new TypeInsnNode(NEW,owner.name));
            }
            code.add(new InsnNode(ATHROW));
            assertThrows(LinkageError.class,()->jvm(owner),operand);
            assertThrows(IllegalArgumentException.class,()->new ClassBytecodeCompiler(owner).compileToWat(linked(owner)),operand);
        }
        ClassNode owner=type("HandlerFixture","java/lang/Object");
        MethodNode method=new MethodNode(ACC_PUBLIC|ACC_STATIC,"run","()V",null,null);method.maxStack=1;owner.methods.add(method);
        method.instructions.add(new InsnNode(ACONST_NULL));method.instructions.add(new InsnNode(ATHROW));
        assertDoesNotThrow(()->jvm(owner));
        for(boolean absent:new boolean[]{false,true}) {
            Map<String,ClassNode> metadata=new LinkedHashMap<>();metadata.put(owner.name,owner);
            if(!absent) {
                ClassNode root=type("java/lang/Throwable","java/lang/Object");root.access|=ACC_INTERFACE|ACC_ABSTRACT;
                metadata.put(root.name,root);
            }
            String message=assertThrows(IllegalArgumentException.class,()->new ClassBytecodeCompiler(owner).compileToWat(metadata)).getMessage();
            assertTrue(message.contains("ATHROW"),message);
        }
    }

    @Test void lowersSuccessfullyVerifiedHandlersWithTheSharedTag() {
        ClassNode owner=allocationHandler(false);
        String wat=new ClassBytecodeCompiler(owner).compileToWat(linked(owner));
        assertTrue(wat.contains("catch $__java_exception"),wat);
        assertTrue(wat.contains("(tag $__java_exception"),wat);
    }
}
