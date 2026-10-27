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

class BytecodeLinkedListTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
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

    @Test void matchesLinkedListOperationsIteratorsAndCopies() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {
                static class Derived extends LinkedList<String>{int marker=7;}
                public static String run(){
                    LinkedList<String> list=new LinkedList<>(Arrays.asList("a",null,"b","a"));list.add(1,"x");list.set(2,"y");list.addAll(2,list);list.addAll(Collections.emptyList());list.remove("a");list.remove(0);
                    list.addFirst("F");list.addLast("L");list.push("P");list.offer("O");list.offerFirst("OF");list.offerLast("OL");
                    String text=list+"|"+list.getFirst()+list.getLast()+list.peek()+list.peekFirst()+list.peekLast()+list.element()+"|"+list.size();
                    text+="|"+list.pop()+list.poll()+list.pollFirst()+list.pollLast()+list.removeFirst()+list.removeLast()+list.remove()+"|"+list;
                    ListIterator<String> it=list.listIterator(2);it.previous();it.set("S");it.add("I");it.next();it.remove();it.add("J");while(it.hasPrevious())text+=it.previous()+it.nextIndex();
                    text+="|"+list.indexOf("a")+list.lastIndexOf("a")+list.indexOf(null)+list.lastIndexOf(null)+list.contains("y")+list.contains("missing")+"|"+list.hashCode()+"|"+list.equals(new ArrayList<>(list))+new ArrayList<>(list).equals(list);
                    list.add("a");list.add(null);text+="|"+list.removeFirstOccurrence("a")+list.removeLastOccurrence(null)+list.removeLastOccurrence("a")+list.removeFirstOccurrence("missing")+"|"+list;
                    Iterator<String> down=list.descendingIterator();StringBuilder reversed=new StringBuilder();while(down.hasNext()){String value=down.next();reversed.append(value);if("y".equals(value))down.remove();}
                    text+="|"+reversed+"|"+list;
                    List<String> view=list.subList(1,3);view.set(0,"V");view.add("W");text+="|"+view+list+Arrays.toString(list.toArray())+Arrays.toString(list.toArray(new String[0]));
                    LinkedList<String> copy=(LinkedList<String>)list.clone();copy.clear();copy.add("c");text+="|"+copy+list.size()+copy.isEmpty();
                    Derived derived=new Derived();derived.add("d");Derived cloned=(Derived)derived.clone();cloned.set(0,"e");text+="|"+cloned.marker+derived+cloned;
                    list.removeAll(Arrays.asList("V","I"));list.retainAll(Arrays.asList("W","J","b","x"));LinkedList<Integer> big=new LinkedList<>();for(int i=0;i<100;i++)big.add(i);
                    return text+"|"+list+"|"+big.get(3)+big.get(97)+big.get(50)+big.remove(49)+big.set(51,-1)+big.get(51)+big.size();
                }
                public static int failures(){
                    int score=0;LinkedList<String> list=new LinkedList<>();Iterator<String> iterator=list.iterator();
                    try{iterator.next();}catch(NoSuchElementException expected){score++;}try{iterator.remove();}catch(IllegalStateException expected){score+=10;}
                    list.add("a");try{iterator.next();}catch(ConcurrentModificationException expected){score+=100;}
                    list.clear();for(Runnable failure:new Runnable[]{list::getFirst,list::getLast,list::removeFirst,list::removeLast,list::element,list::pop,list::remove})try{failure.run();throw new Error();}catch(NoSuchElementException expected){score+=3;}
                    if(list.peek()==null && list.poll()==null && list.peekLast()==null && list.pollLast()==null)score+=10000;
                    list.add("a");list.add("b");
                    for(int index:new int[]{-1,2,Integer.MIN_VALUE})try{list.get(index);throw new Error();}catch(IndexOutOfBoundsException expected){score=31*score+expected.getMessage().hashCode();}
                    for(int index:new int[]{-1,3})try{list.add(index,"z");throw new Error();}catch(IndexOutOfBoundsException expected){score=31*score+expected.getMessage().hashCode();}
                    for(int index:new int[]{-1,3})try{list.listIterator(index);throw new Error();}catch(IndexOutOfBoundsException expected){score=31*score+expected.getMessage().hashCode();}
                    try{list.addAll(5,List.of("q"));throw new Error();}catch(IndexOutOfBoundsException expected){score=31*score+expected.getMessage().hashCode();}
                    try{list.set(2,"z");throw new Error();}catch(IndexOutOfBoundsException expected){score=31*score+expected.getMessage().hashCode();}
                    try{list.remove(2);throw new Error();}catch(IndexOutOfBoundsException expected){score=31*score+expected.getMessage().hashCode();}
                    try{list.addAll(null);throw new Error();}catch(NullPointerException expected){score+=7;}
                    try{new LinkedList<String>(null);throw new Error();}catch(NullPointerException expected){score+=11;}
                    ListIterator<String> cursor=list.listIterator();try{cursor.set("x");throw new Error();}catch(IllegalStateException expected){score+=13;}
                    cursor.next();cursor.add("m");try{cursor.remove();throw new Error();}catch(IllegalStateException expected){score+=17;}
                    Iterator<String> stale=list.iterator();list.addFirst("s");try{stale.next();throw new Error();}catch(ConcurrentModificationException expected){score+=19;}
                    Iterator<String> descending=list.descendingIterator();list.removeLast();try{descending.next();throw new Error();}catch(ConcurrentModificationException expected){score+=23;}
                    List<String> view=list.subList(0,1);list.add("t");try{view.size();throw new Error();}catch(ConcurrentModificationException expected){score+=29;}
                    return score;
                }
            }
            """);compare(List.of("run","failures"));
    }

    @Test void rejectsUnusedUnlinkedDequeMembers() {
        rejectsUnused("java/util/Deque","addFirst","(Ljava/lang/Object;)V",INVOKEINTERFACE,true);
        rejectsUnused("java/util/Queue","offer","(Ljava/lang/Object;)Z",INVOKEINTERFACE,true);
        rejectsUnused("java/util/LinkedList","reversed","()Ljava/util/LinkedList;",INVOKEVIRTUAL,false);
    }

    private void rejectsUnused(String owner,String name,String descriptor,int opcode,boolean isInterface) {
        ClassWriter writer=new ClassWriter(0);writer.visit(V17,ACC_PUBLIC,"UnusedCall",null,"java/lang/Object",null);writer.visitSource("UnusedCall.java",null);
        MethodVisitor method=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"unused","()V",null,null);method.visitCode();Label line=new Label();method.visitLabel(line);method.visitLineNumber(41,line);
        method.visitInsn(ACONST_NULL);method.visitInsn(ACONST_NULL);method.visitMethodInsn(opcode,owner,name,descriptor,isInterface);method.visitInsn(RETURN);method.visitMaxs(2,0);method.visitEnd();writer.visitEnd();
        var failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()));
        assertTrue(failure.getMessage().contains("UnusedCall.java:41"),failure.getMessage());
    }

    private void compare(List<String> names) throws Exception {
        StringBuilder assertions=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
            Class<?> type=loader.loadClass("Metadata");for(String name:names){Object expected=type.getMethod(name).invoke(null);assertions.append("assert.equal(").append(expected instanceof String?"text(e['Metadata.":"e['Metadata.").append(name).append(expected instanceof String?"']()),":"'](),").append(expected instanceof String?js((String)expected):expected).append(");\n");}
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
                const memory=new WebAssembly.Memory({initial:2,maximum:20});let heap=1024,allocations=0,failAllocation=false;
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    allocations++;if(failAllocation){failAllocation=false;return 0;}
                    const address=heap;heap+=size;
                    while(heap>memory.buffer.byteLength)memory.grow(1);
                    new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                }}}).exports;
                assert.equal(allocations,0);heap=Math.max(heap,e.__exception_heap_base?.value??0);
                function text(pointer){if(!pointer)return null;const view=new DataView(memory.buffer);
                    const chars=view.getInt32(pointer+4,true),length=view.getInt32(chars+4,true);
                    let result='';for(let i=0;i<length;i++)result+=String.fromCharCode(view.getUint16(chars+8+2*i,true));return result;}
            """+assertions+"\n}");
        run("node",script.toString(),output.toString());
    }
}
