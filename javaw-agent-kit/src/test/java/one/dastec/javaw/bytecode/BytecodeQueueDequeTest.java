package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T04: java.util.Queue and java.util.Deque are linked, and LinkedList is used through them. */
class BytecodeQueueDequeTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.*;
        public class Fixture {
            static String fail(Runnable action){
                try{action.run();return "none";}
                catch(RuntimeException e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            static String drain(Queue<String> queue){
                StringBuilder out=new StringBuilder();String value;
                while((value=queue.poll())!=null)out.append(value);
                return out.toString();
            }

            public static String queueBasics(){
                Queue<String> queue=new LinkedList<>();
                boolean added=queue.add("a"),offered=queue.offer("b");queue.offer("c");
                String text=added+","+offered+","+queue.peek()+","+queue.element()+","+queue.size();
                text+=","+queue.poll()+","+queue.remove()+","+queue+","+queue.size();
                return text;
            }
            public static String queueEmpty(){
                Queue<String> queue=new LinkedList<>();
                return queue.peek()+","+queue.poll()+","+fail(queue::element)+","+fail(queue::remove)+","+queue.isEmpty();
            }
            public static String queueDrain(){
                Queue<String> queue=new LinkedList<>(List.of("x","y","z"));
                return drain(queue)+"|"+queue.size();
            }
            public static String dequeAddEnds(){
                Deque<String> deque=new LinkedList<>();
                deque.addFirst("b");deque.addLast("c");
                boolean first=deque.offerFirst("a"),last=deque.offerLast("d");
                return first+","+last+","+deque+","+deque.getFirst()+","+deque.getLast()+","+deque.peekFirst()+","+deque.peekLast();
            }
            public static String dequeRemoveEnds(){
                Deque<String> deque=new LinkedList<>(List.of("a","b","c","d","e"));
                String text=deque.removeFirst()+","+deque.removeLast()+","+deque.pollFirst()+","+deque.pollLast();
                return text+","+deque+","+deque.size();
            }
            public static String dequeEmptyNulls(){
                Deque<String> deque=new LinkedList<>();
                return deque.peekFirst()+","+deque.peekLast()+","+deque.pollFirst()+","+deque.pollLast()+","+deque.peek()+","+deque.poll();
            }
            public static String dequeEmptyFailures(){
                Deque<String> deque=new LinkedList<>();
                return fail(deque::getFirst)+","+fail(deque::getLast)+","+fail(deque::removeFirst)+","+fail(deque::removeLast)
                    +","+fail(deque::pop)+","+fail(deque::element)+","+fail(deque::remove);
            }
            public static String dequeQueueView(){
                Deque<String> deque=new LinkedList<>();
                deque.add("a");deque.offer("b");deque.addFirst("z");
                return deque.peek()+","+deque.element()+","+deque.poll()+","+deque.remove()+","+deque;
            }
            public static String stack(){
                Deque<String> stack=new LinkedList<>();
                stack.push("a");stack.push("b");stack.push("c");
                String text=stack+","+stack.peek()+","+stack.pop()+","+stack.pop();
                return text+","+stack+","+stack.size();
            }
            public static String occurrences(){
                Deque<String> deque=new LinkedList<>(Arrays.asList("a","b",null,"a","b",null));
                boolean first=deque.removeFirstOccurrence("b"),last=deque.removeLastOccurrence("a");
                boolean firstNull=deque.removeFirstOccurrence(null),lastNull=deque.removeLastOccurrence(null);
                boolean missing=deque.removeFirstOccurrence("q")||deque.removeLastOccurrence("q");
                return first+","+last+","+firstNull+","+lastNull+","+missing+","+deque;
            }
            public static String descending(){
                Deque<String> deque=new LinkedList<>(List.of("a","b","c","d"));
                Iterator<String> down=deque.descendingIterator();StringBuilder out=new StringBuilder();
                while(down.hasNext()){String value=down.next();out.append(value);if(value.equals("c"))down.remove();}
                String failure=fail(down::next);
                return out+","+deque+","+failure;
            }
            public static String descendingEmpty(){
                Deque<String> deque=new LinkedList<>();
                Iterator<String> down=deque.descendingIterator();
                return down.hasNext()+","+fail(down::next);
            }
            public static String collectionMembers(){
                Deque<String> deque=new LinkedList<>();
                boolean added=deque.add("a");boolean all=deque.addAll(List.of("b","c","b"));
                String text=added+","+all+","+deque.size()+","+deque.contains("c")+","+deque.contains("q");
                text+=","+deque.remove("b")+","+deque.remove("q")+","+deque;
                StringBuilder out=new StringBuilder();for(Iterator<String> it=deque.iterator();it.hasNext();)out.append(it.next());
                text+=","+out+","+deque.isEmpty();
                deque.clear();
                return text+","+deque.isEmpty()+","+deque.size();
            }
            public static String nullElements(){
                Deque<String> deque=new LinkedList<>();
                deque.offerFirst(null);deque.addLast("x");deque.push(null);
                return deque.size()+","+deque.peekFirst()+","+deque.pollFirst()+","+deque.getFirst()+","+deque.getLast()+","+deque;
            }
            public static String types(){
                Object linked=new LinkedList<String>(),array=new ArrayList<String>();
                return (linked instanceof Deque)+","+(linked instanceof Queue)+","+(linked instanceof List)+","+(linked instanceof Collection)
                    +","+(array instanceof Queue)+","+(array instanceof Deque);
            }
            public static String widening(){
                LinkedList<String> list=new LinkedList<>(List.of("m","n"));
                Deque<String> deque=list;Queue<String> queue=deque;Collection<String> collection=queue;List<String> asList=list;
                deque.addFirst("l");queue.offer("o");collection.add("p");
                return list+","+asList.get(0)+","+queue.peek()+","+collection.size()+","+drain(queue)+","+list.isEmpty();
            }

            static final class Ring extends AbstractCollection<String> implements Queue<String> {
                private final ArrayList<String> items=new ArrayList<>();
                public Iterator<String> iterator(){return items.iterator();}
                public int size(){return items.size();}
                public boolean offer(String value){if(items.size()==2)return false;items.add(value);return true;}
                public boolean add(String value){if(!offer(value))throw new IllegalStateException("full");return true;}
                public String poll(){return items.isEmpty()?null:items.remove(0);}
                public String peek(){return items.isEmpty()?null:items.get(0);}
                public String remove(){if(items.isEmpty())throw new NoSuchElementException("empty");return items.remove(0);}
                public String element(){if(items.isEmpty())throw new NoSuchElementException("empty");return items.get(0);}
            }
            public static String userQueue(){
                Queue<String> queue=new Ring();
                String text=queue.offer("a")+","+queue.offer("b")+","+queue.offer("c")+","+fail(()->queue.add("d"));
                text+=","+queue.peek()+","+queue.element()+","+queue.size()+","+queue;
                text+=","+queue.remove()+","+queue.poll()+","+queue.poll()+","+fail(queue::element);
                return text+","+drain(new Ring());
            }
        }
        """;

    @Test void queueMembers() throws Exception {
        matchesJvm(FIXTURE,"queueBasics","queueEmpty","queueDrain");
    }

    @Test void dequeEndMembers() throws Exception {
        matchesJvm(FIXTURE,"dequeAddEnds","dequeRemoveEnds","dequeEmptyNulls","dequeEmptyFailures","dequeQueueView","stack");
    }

    @Test void dequeSearchAndIteration() throws Exception {
        matchesJvm(FIXTURE,"occurrences","descending","descendingEmpty","collectionMembers","nullElements");
    }

    @Test void interfaceTypesAndUserImplementations() throws Exception {
        matchesJvm(FIXTURE,"types","widening","userQueue");
    }

    @Test void rejectsUnlinkedSequencedMembers() {
        rejectsUnusedCall("java/util/Deque","reversed","()Ljava/util/Deque;",INVOKEINTERFACE,true);
        rejectsUnusedCall("java/util/LinkedList","reversed","()Ljava/util/LinkedList;",INVOKEVIRTUAL,false);
    }
}
