package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T06: java.util.AbstractQueue and java.util.PriorityQueue (binary heap). */
@Tag("agent-pending")
class BytecodePriorityQueueTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.*;
        public class Fixture {
            /** Exception simple name; the message too for the exceptions whose JDK message is fixed. */
            static String fail(Runnable action){
                try{action.run();return "none";}
                catch(IllegalStateException|IllegalArgumentException|NoSuchElementException e){return e.getClass().getSimpleName()+":"+e.getMessage();}
                catch(RuntimeException e){return e.getClass().getSimpleName();}
            }
            static <E> String drain(Queue<E> queue){
                StringBuilder out=new StringBuilder();E value;
                while((value=queue.poll())!=null)out.append(value).append(' ');
                return out.toString().trim();
            }
            static String sorted(Object[] values){Object[] copy=values.clone();Arrays.sort(copy);return Arrays.toString(copy);}
            static PriorityQueue<Integer> numbers(int... values){PriorityQueue<Integer> queue=new PriorityQueue<>();for(int value:values)queue.offer(value);return queue;}

            public static String naturalOrder(){
                PriorityQueue<Integer> queue=numbers(5,3,8,1,9,2,7,3);
                return queue.size()+","+queue.peek()+","+queue.comparator()+","+drain(queue)+","+queue.isEmpty()+","+queue.size();
            }
            public static String strings(){
                PriorityQueue<String> queue=new PriorityQueue<>();
                for(String value:"pear fig apple kiwi banana date cherry".split(" "))queue.add(value);
                return queue.peek()+","+drain(queue);
            }
            public static String comparatorOrder(){
                PriorityQueue<String> reverse=new PriorityQueue<>(Comparator.reverseOrder());
                reverse.addAll(List.of("b","d","a","c"));
                Comparator<String> byLength=(a,b)->a.length()!=b.length()?a.length()-b.length():a.compareTo(b);
                PriorityQueue<String> length=new PriorityQueue<>(3,byLength);
                for(String value:"ccc a bb aaa b dddd".split(" "))length.offer(value);
                return drain(reverse)+"|"+(length.comparator()==byLength)+"|"+drain(length);
            }
            public static String nullComparatorMeansNatural(){
                PriorityQueue<Integer> one=new PriorityQueue<>((Comparator<Integer>)null),two=new PriorityQueue<>(4,null);
                one.offer(3);one.offer(1);two.offer(9);two.offer(4);
                return one.comparator()+","+two.comparator()+","+drain(one)+","+drain(two);
            }
            public static String capacity(){
                String text=fail(()->new PriorityQueue<Integer>(0))+","+fail(()->new PriorityQueue<Integer>(-1))+","+fail(()->new PriorityQueue<String>(0,Comparator.reverseOrder()));
                PriorityQueue<Integer> small=new PriorityQueue<>(1);
                for(int i=100;i>0;i--)small.offer(i);
                return text+","+small.size()+","+small.peek()+","+small.poll()+","+small.poll()+","+small.size();
            }
            public static String nulls(){
                PriorityQueue<String> natural=new PriorityQueue<>();
                PriorityQueue<String> ordered=new PriorityQueue<>(Comparator.reverseOrder());
                natural.offer("a");
                return fail(()->natural.offer(null))+","+fail(()->natural.add(null))+","+fail(()->ordered.offer(null))
                    +","+natural.contains(null)+","+natural.remove(null)+","+natural.size()
                    +","+fail(()->new PriorityQueue<String>((Collection<String>)null))+","+fail(()->new PriorityQueue<String>(Arrays.asList("a",null)));
            }
            public static String notComparable(){
                PriorityQueue<Object> queue=new PriorityQueue<>();
                String first=fail(()->queue.offer(new Object()));
                int afterFirst=queue.size();
                queue.offer("a");
                String mixed=fail(()->queue.offer(Integer.valueOf(1)));
                return first+","+afterFirst+","+mixed+","+queue.size()+","+queue.peek();
            }
            public static String emptyQueue(){
                PriorityQueue<String> queue=new PriorityQueue<>();
                return queue.poll()+","+queue.peek()+","+fail(queue::remove)+","+fail(queue::element)+","+queue.isEmpty()+","+queue.size()+","+queue;
            }
            public static String abstractQueueMembers(){
                PriorityQueue<Integer> queue=new PriorityQueue<>();
                boolean added=queue.add(4);
                boolean all=queue.addAll(List.of(2,6)),none=queue.addAll(List.of());
                String text=added+","+all+","+none+","+queue.element()+","+queue.remove()+","+queue.size();
                text+=","+fail(()->queue.addAll(queue))+","+fail(()->queue.addAll(null))+","+queue.size();
                queue.clear();
                return text+","+queue.isEmpty()+","+queue.poll();
            }
            public static String removeObject(){
                PriorityQueue<Integer> queue=numbers(5,3,8,1,9,2,7,3,6);
                boolean middle=queue.remove(Integer.valueOf(8)),duplicate=queue.remove(Integer.valueOf(3)),head=queue.remove(Integer.valueOf(1));
                boolean missing=queue.remove(Integer.valueOf(42));
                return middle+","+duplicate+","+head+","+missing+","+queue.size()+","+queue.peek()+","+drain(queue);
            }
            /** Removing 11 moves the last element (4) into a slot below 10, so removeAt must sift it up. */
            public static String removeNeedsSiftUp(){
                PriorityQueue<Integer> queue=numbers(1,10,2,11,12,3,4);
                String before=queue.toString();boolean removed=queue.remove(Integer.valueOf(11));
                return before+","+removed+","+queue+","+drain(queue);
            }
            public static String containsAndClear(){
                PriorityQueue<String> queue=new PriorityQueue<>(List.of("m","c","x"));
                String text=queue.contains("c")+","+queue.contains("q")+","+queue.size();
                queue.clear();
                text+=","+queue.size()+","+queue.contains("c")+","+queue.peek();
                queue.offer("z");queue.offer("y");
                return text+","+drain(queue);
            }
            public static String iteration(){
                PriorityQueue<Integer> queue=numbers(5,3,8,1,9,2);
                List<Integer> seen=new ArrayList<>();Iterator<Integer> it=queue.iterator();
                while(it.hasNext())seen.add(it.next());
                Collections.sort(seen);
                return seen+","+it.hasNext()+","+fail(it::next)+","+fail(queue.iterator()::remove)+","+queue.size()+","+new PriorityQueue<String>().iterator().hasNext();
            }
            public static String iteratorRemove(){
                PriorityQueue<Integer> queue=numbers(5,3,8,1,9,2,7,4,6,10,0);
                Iterator<Integer> it=queue.iterator();int visited=0;List<Integer> seen=new ArrayList<>();
                while(it.hasNext()){int value=it.next();visited++;seen.add(value);if(value%2==0)it.remove();}
                Collections.sort(seen);
                String twice;
                Iterator<Integer> again=queue.iterator();again.next();again.remove();twice=fail(again::remove);
                return visited+","+seen+","+twice+","+queue.size()+","+drain(queue);
            }
            public static String arrays(){
                PriorityQueue<Integer> queue=numbers(4,1,3,2);
                Object[] plain=queue.toArray();Integer[] typed=queue.toArray(new Integer[0]);
                Integer[] large=new Integer[6];Arrays.fill(large,99);Integer[] same=queue.toArray(large);
                return sorted(plain)+","+sorted(typed)+","+(same==large)+","+same[4]+","+same[5]+","+queue.size()+","+new PriorityQueue<String>().toArray().length;
            }
            public static String fromCollection(){
                PriorityQueue<Integer> list=new PriorityQueue<>(List.of(5,1,4,2,3));
                Collection<String> sortedSet=new TreeSet<>(Comparator.reverseOrder());sortedSet.addAll(List.of("a","c","b"));
                PriorityQueue<String> fromSorted=new PriorityQueue<>(sortedSet);
                PriorityQueue<String> source=new PriorityQueue<>(Comparator.reverseOrder());source.addAll(List.of("x","z","y"));
                Collection<String> asCollection=source;
                PriorityQueue<String> fromQueue=new PriorityQueue<>(asCollection);
                return list.comparator()+","+drain(list)+","+(fromSorted.comparator()!=null)+","+drain(fromSorted)
                    +","+(fromQueue.comparator()!=null)+","+drain(fromQueue)+","+source.size();
            }
            public static long manyElements(){
                PriorityQueue<Integer> queue=new PriorityQueue<>();long seed=12345;
                for(int i=0;i<500;i++){seed=(seed*1103515245L+12345L)&0x7fffffffL;queue.offer((int)(seed%1000));}
                long hash=0;int previous=-1;boolean ordered=true;
                while(!queue.isEmpty()){int value=queue.poll();if(value<previous)ordered=false;previous=value;hash=hash*31+value;}
                return ordered?hash:-1;
            }
            public static String heapOrder(){
                PriorityQueue<Integer> queue=numbers(5,3,8,1,9,2,7);
                String text=queue.toString();
                queue.poll();text+="|"+queue;
                queue.offer(0);queue.offer(6);text+="|"+queue;
                queue.remove(Integer.valueOf(5));text+="|"+queue;
                queue.remove(Integer.valueOf(0));text+="|"+queue;
                return text;
            }

            /** Equal-length strings tie under this comparator but print differently, so tie handling shows. */
            public static String equalPriorities(){
                PriorityQueue<String> queue=new PriorityQueue<>((a,b)->a.length()-b.length());
                for(String value:"ccc a bb b aa c dd".split(" "))queue.offer(value);
                String text=queue.toString();
                queue.poll();text+="|"+queue;
                queue.remove("bb");
                return text+"|"+queue+"|"+drain(queue);
            }
            /** A user collection whose toArray() returns its own typed array: the queue must copy it, not share it. */
            static final class Backed extends AbstractCollection<Integer> {
                final Integer[] data={5,1,4,2,3};
                public Iterator<Integer> iterator(){return Arrays.asList(data).iterator();}
                public int size(){return data.length;}
                public Object[] toArray(){return data;}
            }
            public static String copiesCollectionArray(){
                Backed source=new Backed();
                PriorityQueue<Integer> queue=new PriorityQueue<>(source);
                String text=Arrays.toString(source.data)+","+queue;
                queue.offer(0);
                return text+","+queue+","+Arrays.toString(source.data)+","+drain(queue);
            }

            static final class Bounded extends AbstractQueue<String> {
                private final ArrayList<String> items=new ArrayList<>();
                public Iterator<String> iterator(){return items.iterator();}
                public int size(){return items.size();}
                public boolean offer(String value){if(value==null)throw new NullPointerException();if(items.size()==2)return false;items.add(value);return true;}
                public String poll(){return items.isEmpty()?null:items.remove(0);}
                public String peek(){return items.isEmpty()?null:items.get(0);}
            }
            public static String userAbstractQueue(){
                Bounded queue=new Bounded();
                String text=queue.add("a")+","+queue.add("b")+","+fail(()->queue.add("c"))+","+queue.element()+","+queue.remove()+","+queue;
                text+=","+fail(()->queue.addAll(queue))+","+fail(()->queue.addAll(null))+","+fail(()->queue.addAll(List.of("x","y")))+","+queue;
                queue.clear();
                text+=","+queue.isEmpty()+","+fail(queue::remove)+","+fail(queue::element)+","+queue.addAll(List.of("p"))+","+queue.addAll(List.of());
                Queue<String> asQueue=queue;
                return text+","+asQueue.peek()+","+asQueue.size()+","+drain(asQueue);
            }
            public static String types(){
                Object queue=new PriorityQueue<String>();
                return (queue instanceof Queue)+","+(queue instanceof AbstractQueue)+","+(queue instanceof AbstractCollection)+","+(queue instanceof Deque)+","+(new Bounded() instanceof Queue);
            }
        }
        """;

    @Test void orderingAndConstructors() throws Exception {
        matchesJvm(FIXTURE,"naturalOrder","strings","comparatorOrder","nullComparatorMeansNatural","capacity","manyElements");
    }

    @Test void failures() throws Exception {
        matchesJvm(FIXTURE,"nulls","notComparable","emptyQueue");
    }

    @Test void collectionMembers() throws Exception {
        matchesJvm(FIXTURE,"abstractQueueMembers","removeObject","removeNeedsSiftUp","containsAndClear","arrays","fromCollection","copiesCollectionArray");
    }

    @Test void iterators() throws Exception {
        matchesJvm(FIXTURE,"iteration","iteratorRemove");
    }

    @Test void heapLayoutMatchesTheJdkAlgorithm() throws Exception {
        matchesJvm(FIXTURE,"heapOrder","equalPriorities");
    }

    @Test void abstractQueueForUserClasses() throws Exception {
        matchesJvm(FIXTURE,"userAbstractQueue","types");
    }

    @Test void rejectsUnlinkedCopyConstructors() {
        rejectsUnusedCall("java/util/PriorityQueue","<init>","(Ljava/util/SortedSet;)V",INVOKESPECIAL,false);
        rejectsUnusedCall("java/util/PriorityQueue","<init>","(Ljava/util/PriorityQueue;)V",INVOKESPECIAL,false);
    }
}
