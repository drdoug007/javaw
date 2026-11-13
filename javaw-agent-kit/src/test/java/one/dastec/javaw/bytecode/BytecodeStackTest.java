package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T08: java.util.Stack on top of the T07 Vector. */
class BytecodeStackTest extends KitDifferential {
    private static String fixture(String body) {
        return "import java.util.*;\npublic class Fixture {\n" + """
                static String failure(Runnable action){
                    try{action.run();return "no exception";}
                    catch(RuntimeException e){return e.getClass().getSimpleName()+": "+e.getMessage();}
                }
                static String type(Runnable action){
                    try{action.run();return "no exception";}
                    catch(RuntimeException e){return e.getClass().getSimpleName();}
                }
                static Stack<String> abc(){Stack<String> s=new Stack<>();s.push("a");s.push("b");s.push("c");return s;}
            """ + body + "}\n";
    }

    @Test void stackOperations() throws Exception {
        matchesJvm(fixture("""
                public static String pushReturnsItem(){
                    Stack<String> s=new Stack<>();String a=s.push("a");String n=s.push(null);
                    return a+"|"+n+"|"+s.size()+"|"+s;
                }
                public static String popOrder(){
                    Stack<String> s=abc();StringBuilder out=new StringBuilder();
                    while(!s.empty())out.append(s.pop());
                    return out+"|"+s.size()+"|"+s.empty()+"|"+s.isEmpty();
                }
                public static String peekDoesNotRemove(){
                    Stack<String> s=abc();String first=s.peek(),second=s.peek();
                    return first+second+"|"+s.size()+"|"+s;
                }
                public static String pushAfterPop(){
                    Stack<Integer> s=new Stack<>();for(int i=0;i<5;i++)s.push(i);s.pop();s.pop();s.push(9);
                    return s+"|"+s.peek()+"|"+s.size();
                }
                public static boolean emptyFlag(){Stack<String> s=new Stack<>();boolean before=s.empty();s.push("x");return before && !s.empty();}
                public static String emptyFailures(){
                    Stack<String> s=new Stack<>();
                    return failure(()->s.pop())+"|"+failure(()->s.peek());
                }
                public static String drainedFailures(){
                    Stack<String> s=abc();s.pop();s.pop();s.pop();
                    return failure(()->s.peek())+"|"+failure(()->s.pop())+"|"+s.size();
                }
                public static String failureIsCatchable(){
                    try{new Stack<String>().pop();return "none";}
                    catch(EmptyStackException e){return "caught "+(e instanceof RuntimeException);}
                }
                public static String manyPushes(){
                    Stack<Integer> s=new Stack<>();for(int i=0;i<25;i++)s.push(i);
                    int sum=0;while(!s.empty())sum=sum*3+s.pop()%7;
                    return sum+"|"+s.capacity();
                }
            """), "pushReturnsItem", "popOrder", "peekDoesNotRemove", "pushAfterPop", "emptyFlag", "emptyFailures",
                "drainedFailures", "failureIsCatchable", "manyPushes");
    }

    @Test void search() throws Exception {
        matchesJvm(fixture("""
                public static String distances(){
                    Stack<String> s=abc();
                    return s.search("c")+","+s.search("b")+","+s.search("a")+","+s.search("zz");
                }
                public static String duplicatesUseTopmost(){
                    Stack<String> s=abc();s.push("a");
                    return s.search("a")+","+s.search("b");
                }
                public static String nulls(){
                    Stack<String> s=new Stack<>();int absent=s.search(null);s.push(null);s.push("x");
                    return absent+","+s.search(null)+","+s.search("x");
                }
                public static int emptySearch(){return new Stack<String>().search("a");}
                public static String usesEquals(){
                    Stack<String> s=new Stack<>();s.push(new String("key"));s.push("other");
                    return s.search(new String("key"))+","+s.search("othe"+"r".toString());
                }
            """), "distances", "duplicatesUseTopmost", "nulls", "emptySearch", "usesEquals");
    }

    @Test void inheritedVectorMembers() throws Exception {
        matchesJvm(fixture("""
                public static String listView(){
                    Stack<String> s=abc();s.add("d");s.addAll(List.of("e","f"));
                    return s+"|"+s.get(0)+s.elementAt(3)+s.lastElement()+s.firstElement()+"|"+s.peek()+"|"+s.indexOf("c");
                }
                public static String iteratesBottomToTop(){
                    Stack<String> s=abc();StringBuilder out=new StringBuilder();
                    for(String x:s)out.append(x);
                    for(Enumeration<String> e=s.elements();e.hasMoreElements();)out.append(e.nextElement().toUpperCase());
                    Iterator<String> it=s.iterator();it.next();it.remove();
                    return out+"|"+s;
                }
                public static String clearAndReuse(){
                    Stack<String> s=abc();s.clear();boolean empty=s.empty()&&s.isEmpty();s.push("z");
                    return empty+"|"+s+"|"+s.peek();
                }
                public static String cloneIsStack(){
                    Stack<String> s=abc();
                    @SuppressWarnings("unchecked") Stack<String> copy=(Stack<String>)s.clone();
                    s.pop();copy.push("d");
                    return (copy.getClass()==Stack.class)+"|"+s+"|"+copy+"|"+copy.peek()+"|"+copy.search("a");
                }
                public static String types(){
                    Object s=new Stack<String>();
                    return (s instanceof Vector)+","+(s instanceof List)+","+(s instanceof RandomAccess)+","+(s instanceof Cloneable)+","+s.getClass().getName()+","+s.getClass().getSuperclass().getName();
                }
                public static String equalsVector(){
                    Stack<String> s=abc();Vector<String> v=new Vector<>(List.of("a","b","c"));
                    return s.equals(v)+","+v.equals(s)+","+(s.hashCode()==v.hashCode())+","+s.equals(List.of("c","b","a"));
                }
                public static String failFast(){
                    Stack<String> s=abc();Iterator<String> it=s.iterator();it.next();s.push("d");
                    String afterPush=type(()->it.next());
                    Iterator<String> again=s.iterator();again.next();s.pop();
                    return afterPush+"|"+type(()->again.next());
                }
                public static String positionalFailures(){
                    Stack<String> s=abc();
                    return failure(()->s.get(3))+"|"+failure(()->s.elementAt(5))+"|"+failure(()->new Stack<String>().lastElement());
                }
            """), "listView", "iteratesBottomToTop", "clearAndReuse", "cloneIsStack", "types", "equalsVector",
                "failFast", "positionalFailures");
    }

    @Test void leavesOutNonJdkMembers() {
        rejectsUnusedCall("java/util/Stack", "setSize", "(I)V", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/Stack", "<init>", "(I)V", INVOKESPECIAL, false);
    }
}
