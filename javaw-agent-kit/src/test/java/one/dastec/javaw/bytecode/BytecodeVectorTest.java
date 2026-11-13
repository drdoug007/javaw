package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T07: java.util.EmptyStackException and a monitor-free java.util.Vector. */
class BytecodeVectorTest extends KitDifferential {
    private static final String HELPERS = """
                static String failure(Runnable action){
                    try{action.run();return "no exception";}
                    catch(RuntimeException e){return e.getClass().getSimpleName()+": "+e.getMessage();}
                }
                static String type(Runnable action){
                    try{action.run();return "no exception";}
                    catch(RuntimeException e){return e.getClass().getSimpleName();}
                }
            """;
    private static final String ABC = """
                static Vector<String> abc(){Vector<String> v=new Vector<>();v.add("a");v.add("b");v.add("c");return v;}
            """;

    private static String fixture(String body) {
        return "import java.util.*;\npublic class Fixture {\n" + HELPERS + ABC + body + "}\n";
    }

    @Test void emptyStackException() throws Exception {
        matchesJvm("import java.util.*;\npublic class Fixture {\n" + HELPERS + """
                public static String created(){EmptyStackException e=new EmptyStackException();return e.getMessage()+"|"+(e instanceof RuntimeException)+"|"+e.getClass().getName();}
                public static String thrown(){return failure(()->{throw new EmptyStackException();});}
                public static boolean caughtAsRuntime(){try{throw new EmptyStackException();}catch(RuntimeException e){return e instanceof EmptyStackException;}}
            }
            """, "created", "thrown", "caughtAsRuntime");
    }

    @Test void basicsAndCapacity() throws Exception {
        matchesJvm(fixture("""
                public static String basics(){
                    Vector<String> v=new Vector<>();boolean empty=v.isEmpty();
                    boolean added=v.add("a");v.addElement("b");v.add(null);
                    return empty+"|"+added+"|"+v.size()+"|"+v.get(0)+"|"+v.elementAt(1)+"|"+v.get(2)+"|"+v+"|"+v.isEmpty();
                }
                public static String emptyToString(){return new Vector<String>().toString()+new Vector<String>(0).size();}
                public static String defaultCapacity(){
                    Vector<Integer> v=new Vector<>();StringBuilder out=new StringBuilder().append(v.capacity());
                    for(int i=0;i<11;i++)v.add(i);
                    out.append(',').append(v.capacity());
                    for(int i=0;i<10;i++)v.add(i);
                    return out.append(',').append(v.capacity()).append(',').append(v.size()).toString();
                }
                public static String sizedCapacity(){
                    Vector<Integer> v=new Vector<>(3);StringBuilder out=new StringBuilder().append(v.capacity()).append(v.size());
                    for(int i=0;i<4;i++)v.add(i);
                    return out.append(',').append(v.capacity()).append(',').append(v).toString();
                }
                public static String zeroCapacity(){
                    Vector<Integer> v=new Vector<>(0);StringBuilder out=new StringBuilder().append(v.capacity());
                    for(int i=0;i<5;i++){v.addElement(i);out.append(',').append(v.capacity());}
                    return out.toString();
                }
                public static String negativeCapacity(){return failure(()->new Vector<String>(-1));}
                public static String fromCollection(){
                    Vector<String> v=new Vector<>(List.of("x","y"));v.add("z");
                    return v+"|"+v.size()+"|"+v.capacity();
                }
                public static String fromNull(){return type(()->new Vector<String>((Collection<String>)null));}
                public static String interfaces(){
                    Object v=new Vector<String>();
                    return (v instanceof List)+"|"+(v instanceof RandomAccess)+"|"+(v instanceof Cloneable)+"|"+(v instanceof AbstractList)+"|"+(v instanceof java.io.Serializable);
                }
            """), "basics", "emptyToString", "defaultCapacity", "sizedCapacity", "zeroCapacity", "negativeCapacity",
                "fromCollection", "fromNull", "interfaces");
    }

    @Test void positionalAccess() throws Exception {
        matchesJvm(fixture("""
                public static String getAndSet(){
                    Vector<String> v=abc();String old=v.set(1,"B");
                    return old+"|"+v+"|"+v.get(1)+"|"+v.elementAt(2);
                }
                public static String getFailures(){
                    Vector<String> v=abc();
                    return failure(()->v.get(3))+"|"+failure(()->v.get(7))+"|"+type(()->v.get(-1));
                }
                public static String setFailures(){
                    Vector<String> v=abc();
                    return failure(()->v.set(3,"x"))+"|"+type(()->v.set(-1,"x"))+"|"+v;
                }
                public static String elementAtFailures(){
                    Vector<String> v=abc();
                    return failure(()->v.elementAt(3))+"|"+failure(()->v.elementAt(9))+"|"+type(()->v.elementAt(-1))+"|"+failure(()->new Vector<String>().elementAt(0));
                }
                public static String firstAndLast(){
                    Vector<String> v=abc();String one=v.firstElement()+v.lastElement();
                    v.removeElementAt(2);v.removeElementAt(1);
                    return one+"|"+v.firstElement()+v.lastElement();
                }
                public static String firstAndLastEmpty(){
                    Vector<String> v=new Vector<>();
                    return failure(()->v.firstElement())+"|"+failure(()->v.lastElement());
                }
            """), "getAndSet", "getFailures", "setFailures", "elementAtFailures", "firstAndLast", "firstAndLastEmpty");
    }

    @Test void insertAndRemove() throws Exception {
        matchesJvm(fixture("""
                public static String insertElementAt(){
                    Vector<String> v=abc();v.insertElementAt("0",0);v.insertElementAt("m",2);v.insertElementAt("end",v.size());
                    return v+"|"+v.size();
                }
                public static String insertFailures(){
                    Vector<String> v=abc();
                    return failure(()->v.insertElementAt("x",4))+"|"+type(()->v.insertElementAt("x",-1))+"|"+v;
                }
                public static String addAtIndex(){
                    Vector<String> v=abc();v.add(0,"first");v.add(4,"last");
                    return v+"|"+failure(()->v.add(9,"x"));
                }
                public static String removeElementAt(){
                    Vector<String> v=abc();v.add("d");v.removeElementAt(1);String middle=v.toString();v.removeElementAt(0);v.removeElementAt(v.size()-1);
                    return middle+"|"+v+"|"+v.size();
                }
                public static String removeElementAtFailures(){
                    Vector<String> v=abc();
                    return failure(()->v.removeElementAt(3))+"|"+failure(()->v.removeElementAt(5))+"|"+failure(()->v.removeElementAt(-1))+"|"+v;
                }
                public static String removeByIndex(){
                    Vector<String> v=abc();String old=v.remove(1);
                    return old+"|"+v+"|"+failure(()->v.remove(2))+"|"+type(()->v.remove(-1));
                }
                public static String removeByValue(){
                    Vector<String> v=abc();v.add("b");v.add(null);
                    boolean a=v.remove("b"),b=v.removeElement("c"),c=v.remove("zz"),d=v.remove(null);
                    return a+","+b+","+c+","+d+"|"+v;
                }
                public static String removeAllElements(){
                    Vector<String> v=abc();int capacity=v.capacity();v.removeAllElements();
                    String first=v.size()+","+v.isEmpty()+","+(v.capacity()==capacity)+","+v;
                    v.add("again");Vector<String> w=abc();w.clear();
                    return first+"|"+v+"|"+w+w.isEmpty();
                }
            """), "insertElementAt", "insertFailures", "addAtIndex", "removeElementAt", "removeElementAtFailures",
                "removeByIndex", "removeByValue", "removeAllElements");
    }

    @Test void searching() throws Exception {
        matchesJvm(fixture("""
                static Vector<String> repeated(){Vector<String> v=abc();v.add(null);v.add("a");v.add("b");v.add(null);return v;}
                public static String indexes(){
                    Vector<String> v=repeated();
                    return v.indexOf("a")+","+v.indexOf("b")+","+v.indexOf(null)+","+v.indexOf("zz")+"|"+v.lastIndexOf("a")+","+v.lastIndexOf(null)+","+v.lastIndexOf("c")+","+v.lastIndexOf("zz");
                }
                public static String indexesFrom(){
                    Vector<String> v=repeated();
                    return v.indexOf("a",1)+","+v.indexOf(null,4)+","+v.indexOf("c",3)+","+v.indexOf("a",99)+"|"+v.lastIndexOf("a",3)+","+v.lastIndexOf(null,5)+","+v.lastIndexOf("b",0)+","+v.lastIndexOf("a",-1);
                }
                public static String lastIndexOfFailure(){
                    Vector<String> v=abc();
                    return failure(()->v.lastIndexOf("a",3))+"|"+failure(()->v.lastIndexOf("a",8));
                }
                public static String contains(){
                    Vector<String> v=abc();v.add(null);
                    return v.contains("b")+","+v.contains(null)+","+v.contains("zz")+","+new Vector<String>().contains(null)+","+v.containsAll(List.of("a","c"));
                }
                public static int emptyIndexes(){Vector<String> v=new Vector<>();return v.indexOf("a")+v.lastIndexOf(null)*10;}
            """), "indexes", "indexesFrom", "lastIndexOfFailure", "contains", "emptyIndexes");
    }

    @Test void copiesAndArrays() throws Exception {
        matchesJvm(fixture("""
                public static String copyInto(){
                    Vector<String> v=abc();String[] target={"x","x","x","x","x"};v.copyInto(target);
                    return String.join(",",target);
                }
                public static String copyIntoTooSmall(){Vector<String> v=abc();return type(()->v.copyInto(new Object[2]));}
                public static String toArrayPlain(){
                    Vector<String> v=abc();Object[] values=v.toArray();values[0]="changed";
                    return values.length+"|"+values[0]+values[1]+values[2]+"|"+v.get(0)+"|"+(values.getClass()==Object[].class);
                }
                public static String toArrayTyped(){
                    Vector<String> v=abc();String[] small=v.toArray(new String[0]);
                    String[] big={"1","2","3","4","5"};String[] same=v.toArray(big);
                    return small.length+String.join("",small)+"|"+(same==big)+"|"+big[0]+big[1]+big[2]+big[3]+big[4]+"|"+small.getClass().getSimpleName();
                }
                public static String cloneIsIndependent(){
                    Vector<String> v=abc();
                    @SuppressWarnings("unchecked") Vector<String> copy=(Vector<String>)v.clone();
                    v.set(0,"A");v.add("d");copy.removeElementAt(1);
                    return v+"|"+copy+"|"+copy.capacity()+"|"+(copy.getClass()==Vector.class)+"|"+(copy!=v);
                }
                static class Sub extends Vector<String>{}
                public static String subclassClone(){
                    Sub s=new Sub();s.add("a");s.add("b");Object c=s.clone();s.add("c");
                    return (c.getClass()==Sub.class)+"|"+(c instanceof Sub)+"|"+c+"|"+((Vector<?>)c).capacity()+"|"+s;
                }
                public static String equalsAndHash(){
                    Vector<String> v=abc();List<String> list=new ArrayList<>(List.of("a","b","c"));
                    return v.equals(list)+","+list.equals(v)+","+(v.hashCode()==list.hashCode())+","+v.equals(abc())+","+v.equals(List.of("a","b"))+","+v.hashCode();
                }
            """), "copyInto", "copyIntoTooSmall", "toArrayPlain", "toArrayTyped", "cloneIsIndependent", "subclassClone",
                "equalsAndHash");
    }

    @Test void enumerationsAndIterators() throws Exception {
        matchesJvm(fixture("""
                public static String elements(){
                    Vector<String> v=abc();StringBuilder out=new StringBuilder();
                    for(Enumeration<String> e=v.elements();e.hasMoreElements();)out.append(e.nextElement());
                    return out.toString();
                }
                public static String elementsExhausted(){
                    Vector<String> v=abc();Enumeration<String> e=v.elements();while(e.hasMoreElements())e.nextElement();
                    return failure(()->e.nextElement())+"|"+failure(()->new Vector<String>().elements().nextElement());
                }
                public static String elementsSeeLaterAdds(){
                    Vector<String> v=abc();Enumeration<String> e=v.elements();e.nextElement();v.add("d");
                    StringBuilder out=new StringBuilder();while(e.hasMoreElements())out.append(e.nextElement());
                    Iterator<String> it=v.elements().asIterator();while(it.hasNext())out.append(it.next().toUpperCase());
                    return out.toString();
                }
                public static String iterate(){
                    Vector<String> v=abc();StringBuilder out=new StringBuilder();
                    for(String s:v)out.append(s);
                    v.forEach(s->out.append(s.toUpperCase()));
                    return out.toString();
                }
                public static String iteratorRemove(){
                    Vector<String> v=abc();Iterator<String> it=v.iterator();it.next();it.next();it.remove();
                    return v+"|"+it.next()+"|"+it.hasNext()+"|"+type(()->{it.remove();it.remove();})+v+"|"+type(()->it.next());
                }
                public static String iteratorFailFast(){
                    Vector<String> v=abc();Iterator<String> it=v.iterator();it.next();v.addElement("d");
                    String first=type(()->it.next());
                    Iterator<String> again=v.iterator();again.next();v.removeElementAt(0);
                    return first+"|"+type(()->again.next());
                }
                public static String listIterator(){
                    Vector<String> v=abc();ListIterator<String> it=v.listIterator();it.next();it.set("A");it.add("x");
                    it.next();ListIterator<String> back=v.listIterator(v.size());StringBuilder out=new StringBuilder();
                    while(back.hasPrevious())out.append(back.previous());
                    return v+"|"+out+"|"+type(()->v.listIterator(9));
                }
                public static String addAllVariants(){
                    Vector<String> v=abc();boolean a=v.addAll(List.of("d","e")),b=v.addAll(new ArrayList<String>());v.addAll(1,List.of("x","y"));
                    return a+","+b+"|"+v+"|"+v.size()+"|"+type(()->v.addAll(99,List.of("z")));
                }
                public static String subListWritesThrough(){
                    Vector<String> v=abc();v.add("d");List<String> sub=v.subList(1,3);String seen=sub.toString();sub.clear();
                    return seen+"|"+v+"|"+v.size();
                }
                public static String bulkRemoval(){
                    Vector<String> v=abc();v.add("d");v.removeAll(List.of("a","c"));
                    Vector<String> w=abc();w.retainAll(List.of("b"));
                    Vector<String> x=abc();x.removeIf(s->s.compareTo("b")>=0);
                    return v+"|"+w+"|"+x;
                }
            """), "elements", "elementsExhausted", "elementsSeeLaterAdds", "iterate", "iteratorRemove", "iteratorFailFast",
                "listIterator", "addAllVariants", "subListWritesThrough", "bulkRemoval");
    }

    /** Review of T07: JDK modCount points, remove(Object)/contains/isEmpty, and addAll(int,..) checking the index first. */
    @Test void reviewedModCountAndChecks() throws Exception {
        matchesJvm(fixture("""
                static String afterNext(Vector<String> v,Runnable change){
                    Iterator<String> it=v.iterator();
                    try{change.run();}catch(RuntimeException ignored){}
                    return type(()->it.next());
                }
                public static String removeElementMiss(){Vector<String> v=abc();return afterNext(v,()->v.removeElement("zz"))+"|"+v;}
                public static String removeObjectMiss(){Vector<String> v=abc();return afterNext(v,()->v.remove((Object)"zz"))+"|"+v;}
                public static String removeIndexFailure(){Vector<String> v=abc();return afterNext(v,()->v.remove(5))+"|"+v;}
                public static String removeObjectHit(){Vector<String> v=abc();v.add("b");boolean r=v.remove((Object)"b");return r+"|"+v+"|"+v.contains("b")+v.contains("q")+"|"+v.isEmpty()+new Vector<String>().isEmpty();}
                public static String containsNull(){Vector<String> v=abc();boolean before=v.contains(null);v.add(null);return before+"|"+v.contains(null)+"|"+v.remove((Object)null)+"|"+v;}
                public static String addAllBadIndexNull(){return failure(()->new Vector<String>().addAll(5,null));}
            """), "removeElementMiss", "removeObjectMiss", "removeIndexFailure", "removeObjectHit", "containsNull",
                "addAllBadIndexNull");
    }

    @Test void leavesOutResizingMembers() {
        rejectsUnusedCall("java/util/Vector", "setSize", "(I)V", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/Vector", "<init>", "(II)V", INVOKESPECIAL, false);
    }
}
