package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T05: java.util.ArrayDeque, a resizable ring buffer that rejects null elements. */
class BytecodeArrayDequeTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.*;
        public class Fixture {
            static String fail(Runnable action){
                try{action.run();return "none";}
                catch(RuntimeException e){return e.getClass().getSimpleName();}
            }
            static String forward(Iterable<String> values){StringBuilder out=new StringBuilder();for(String value:values)out.append(value);return out.toString();}
            static String backward(Deque<String> deque){StringBuilder out=new StringBuilder();for(Iterator<String> it=deque.descendingIterator();it.hasNext();)out.append(it.next());return out.toString();}
            /** Builds a deque whose ring buffer has wrapped around: head is not at index 0. */
            static ArrayDeque<String> wrapped(){
                ArrayDeque<String> deque=new ArrayDeque<>(4);
                deque.addLast("c");deque.addLast("d");deque.addFirst("b");deque.addFirst("a");deque.addLast("e");
                return deque;
            }

            public static String constructors(){
                ArrayDeque<String> plain=new ArrayDeque<>(),sized=new ArrayDeque<>(100),zero=new ArrayDeque<>(0),negative=new ArrayDeque<>(-5);
                zero.add("z");negative.add("n");negative.add("m");
                ArrayDeque<String> copy=new ArrayDeque<>(List.of("x","y","z"));
                return plain+","+plain.size()+","+sized.isEmpty()+","+zero+","+negative+","+copy+","+copy.size();
            }
            public static String constructorFailures(){
                return fail(()->new ArrayDeque<String>((Collection<String>)null))+","+fail(()->new ArrayDeque<String>(Arrays.asList("a",null)));
            }
            public static String addEnds(){
                ArrayDeque<String> deque=new ArrayDeque<>();
                deque.addFirst("b");deque.addLast("c");
                boolean first=deque.offerFirst("a"),last=deque.offerLast("d");
                return first+","+last+","+deque+","+deque.getFirst()+","+deque.getLast()+","+deque.peekFirst()+","+deque.peekLast()+","+deque.size();
            }
            public static String removeEnds(){
                ArrayDeque<String> deque=new ArrayDeque<>(List.of("a","b","c","d","e"));
                String text=deque.removeFirst()+","+deque.removeLast()+","+deque.pollFirst()+","+deque.pollLast();
                return text+","+deque+","+deque.size()+","+deque.removeFirst()+","+deque.isEmpty();
            }
            public static String emptyNulls(){
                ArrayDeque<String> deque=new ArrayDeque<>();
                return deque.peekFirst()+","+deque.peekLast()+","+deque.pollFirst()+","+deque.pollLast()+","+deque.peek()+","+deque.poll()+","+deque.isEmpty();
            }
            public static String emptyFailures(){
                ArrayDeque<String> deque=new ArrayDeque<>();
                return fail(deque::getFirst)+","+fail(deque::getLast)+","+fail(deque::removeFirst)+","+fail(deque::removeLast)
                    +","+fail(deque::pop)+","+fail(deque::element)+","+fail(deque::remove);
            }
            public static String nullElementsRejected(){
                ArrayDeque<String> deque=new ArrayDeque<>();
                String text=fail(()->deque.addFirst(null))+","+fail(()->deque.addLast(null))+","+fail(()->deque.offerFirst(null))
                    +","+fail(()->deque.offerLast(null))+","+fail(()->deque.push(null))+","+fail(()->deque.add(null))+","+fail(()->deque.offer(null));
                return text+","+deque.size();
            }
            public static String addAllWithNull(){
                ArrayDeque<String> deque=new ArrayDeque<>(List.of("x"));
                String failure=fail(()->deque.addAll(Arrays.asList("a",null,"b")));
                return failure+","+deque+","+fail(()->deque.addAll(null))+","+deque.addAll(List.of())+","+deque.addAll(List.of("c"))+","+deque;
            }
            public static String nullQueries(){
                ArrayDeque<String> deque=new ArrayDeque<>(List.of("a"));
                return deque.contains(null)+","+deque.remove(null)+","+deque.removeFirstOccurrence(null)+","+deque.removeLastOccurrence(null)+","+deque;
            }
            public static String stack(){
                Deque<String> stack=new ArrayDeque<>();
                stack.push("a");stack.push("b");stack.push("c");
                String text=stack+","+stack.peek()+","+stack.pop()+","+stack.pop();
                return text+","+stack+","+stack.size()+","+stack.pop()+","+fail(stack::pop);
            }
            public static String queueView(){
                Queue<String> queue=new ArrayDeque<>();
                boolean added=queue.add("a"),offered=queue.offer("b");queue.offer("c");
                String text=added+","+offered+","+queue.peek()+","+queue.element()+","+queue.poll()+","+queue.remove()+","+queue;
                return text+","+queue.size()+","+queue.poll()+","+queue.poll()+","+fail(queue::remove)+","+fail(queue::element);
            }
            public static String occurrences(){
                ArrayDeque<String> deque=wrapped();deque.addAll(List.of("b","a","q"));
                boolean first=deque.removeFirstOccurrence("b"),last=deque.removeLastOccurrence("a"),missing=deque.removeFirstOccurrence("zz")||deque.removeLastOccurrence("zz");
                boolean plain=deque.remove("q");
                return first+","+last+","+missing+","+plain+","+deque+","+deque.size()+","+backward(deque);
            }
            public static String wrapAround(){
                ArrayDeque<String> deque=wrapped();
                return deque+","+deque.size()+","+deque.getFirst()+","+deque.getLast()+","+forward(deque)+","+backward(deque);
            }
            public static String growWhileWrapped(){
                ArrayDeque<String> deque=new ArrayDeque<>(2);StringBuilder polled=new StringBuilder();
                for(int i=0;i<40;i++){
                    if(i%3==0)deque.addFirst("f"+i);else deque.addLast("l"+i);
                    if(i%5==4)polled.append(deque.pollFirst()).append(' ');
                    if(i%7==6)polled.append(deque.pollLast()).append(' ');
                }
                return deque.size()+"|"+polled+"|"+deque;
            }
            public static long manyElements(){
                ArrayDeque<Integer> deque=new ArrayDeque<>();
                for(int i=0;i<1000;i++){if((i&1)==0)deque.addLast(i);else deque.addFirst(i);}
                long sum=0;int steps=0;
                while(!deque.isEmpty()){int value=(steps++&1)==0?deque.pollFirst():deque.pollLast();sum=sum*31+value;}
                return sum+steps;
            }
            public static String iteration(){
                ArrayDeque<String> deque=wrapped();
                Iterator<String> it=deque.iterator();StringBuilder out=new StringBuilder();
                while(it.hasNext())out.append(it.next());
                return out+","+it.hasNext()+","+fail(it::next)+","+forward(new ArrayDeque<String>());
            }
            public static String iteratorRemove(){
                ArrayDeque<String> deque=wrapped();
                Iterator<String> it=deque.iterator();StringBuilder seen=new StringBuilder();
                String before=fail(it::remove);
                while(it.hasNext()){String value=it.next();seen.append(value);if(value.equals("b")||value.equals("d")||value.equals("e"))it.remove();}
                String twice=fail(it::remove);
                return before+","+seen+","+deque+","+twice+","+deque.size()+","+deque.getLast();
            }
            public static String iteratorRemoveAll(){
                ArrayDeque<String> deque=wrapped();
                Iterator<String> it=deque.iterator();
                while(it.hasNext()){it.next();it.remove();}
                deque.addLast("again");
                return deque+","+deque.size();
            }
            public static String descending(){
                ArrayDeque<String> deque=wrapped();
                Iterator<String> down=deque.descendingIterator();StringBuilder seen=new StringBuilder();
                String before=fail(down::remove);
                while(down.hasNext()){String value=down.next();seen.append(value);if(value.equals("d")||value.equals("a"))down.remove();}
                return before+","+seen+","+deque+","+fail(down::next)+","+fail(down::remove)+","+backward(deque);
            }
            static final class Named extends ArrayDeque<String> {}
            public static String cloneCopy(){
                ArrayDeque<String> deque=wrapped();
                ArrayDeque<String> copy=deque.clone();
                copy.addFirst("new");deque.pollLast();
                Named named=new Named();named.add("n");
                Object namedCopy=named.clone();
                return copy+","+deque+","+(copy!=deque)+","+copy.getClass().getSimpleName()+","+namedCopy.getClass().getSimpleName()+","+namedCopy;
            }
            public static String arrays(){
                ArrayDeque<String> deque=wrapped();
                Object[] plain=deque.toArray();String[] exact=deque.toArray(new String[0]);
                String[] large=new String[7];Arrays.fill(large,"old");String[] same=deque.toArray(large);
                return Arrays.toString(plain)+","+Arrays.toString(exact)+","+(same==large)+","+Arrays.toString(same)+","+new ArrayDeque<String>().toArray().length;
            }
            public static String collectionMembers(){
                ArrayDeque<String> deque=wrapped();
                String text=deque.contains("c")+","+deque.contains("zz")+","+deque.containsAll(List.of("a","e"))+","+deque.remove("c")+","+deque.remove("zz");
                boolean removed=deque.removeIf(value->value.equals("a"));
                boolean all=deque.removeAll(List.of("b"));
                boolean kept=deque.retainAll(List.of("d","e","x"));
                text+=","+removed+","+all+","+kept+","+deque+","+fail(()->deque.removeIf(null));
                deque.clear();
                text+=","+deque.isEmpty()+","+deque.size()+","+deque.peekFirst();
                deque.addLast("p");deque.addFirst("o");
                return text+","+deque;
            }
            public static String types(){
                Object deque=new ArrayDeque<String>();
                return (deque instanceof Deque)+","+(deque instanceof Queue)+","+(deque instanceof Collection)+","+(deque instanceof Cloneable)+","+(deque instanceof List);
            }
        }
        """;

    @Test void constructorsAndEnds() throws Exception {
        matchesJvm(FIXTURE,"constructors","constructorFailures","addEnds","removeEnds","emptyNulls","emptyFailures");
    }

    @Test void nullsAreRejected() throws Exception {
        matchesJvm(FIXTURE,"nullElementsRejected","addAllWithNull","nullQueries");
    }

    @Test void queueAndStackViews() throws Exception {
        matchesJvm(FIXTURE,"stack","queueView","occurrences");
    }

    @Test void ringBufferWrapsAndGrows() throws Exception {
        matchesJvm(FIXTURE,"wrapAround","growWhileWrapped","manyElements");
    }

    @Test void iterators() throws Exception {
        matchesJvm(FIXTURE,"iteration","iteratorRemove","iteratorRemoveAll","descending");
    }

    @Test void copiesAndCollectionMembers() throws Exception {
        matchesJvm(FIXTURE,"cloneCopy","arrays","collectionMembers","types");
    }

    @Test void rejectsUnlinkedSequencedMembers() {
        rejectsUnusedCall("java/util/ArrayDeque","reversed","()Ljava/util/Deque;",INVOKEVIRTUAL,false);
    }
}
