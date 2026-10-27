package java.util.stream;
import java.util.*;
import java.util.function.*;
/** Adapter sharing reference pipeline single-use, traversal and close state. */
public final class IntPipeline implements IntStream {
    private final Stream<Integer> values;
    private IntPipeline(Stream<Integer> values){this.values=values;}
    public static IntStream from(Stream<Integer> values){return new IntPipeline(Objects.requireNonNull(values));}
    public static IntStream source(Supplier<? extends Iterator<Integer>> factory){return from(ReferenceStream.fromIterator(factory));}
    static IntStream array(final int[] data){
        Objects.requireNonNull(data);return source(()->new Iterator<Integer>(){
            int index;public boolean hasNext(){return index<data.length;}
            public Integer next(){if(!hasNext())throw new NoSuchElementException();return Integer.valueOf(data[index++]);}
        });
    }
    static IntStream range(final int start,final int end,final boolean closed){
        return source(()->new Iterator<Integer>(){
            long position=start,limit=(long)end+(closed?1:0);
            public boolean hasNext(){return position<limit;}
            public Integer next(){if(!hasNext())throw new NoSuchElementException();return Integer.valueOf((int)position++);}
        });
    }
    public boolean isParallel(){return false;}
    public IntStream sequential(){values.sequential();return this;}
    public IntStream unordered(){values.unordered();return this;}
    public IntStream onClose(Runnable handler){values.onClose(handler);return this;}
    public void close(){values.close();}
    public PrimitiveIterator.OfInt iterator(){
        final Iterator<Integer> cursor=values.iterator();return new PrimitiveIterator.OfInt(){
            public boolean hasNext(){return cursor.hasNext();}public int nextInt(){return cursor.next().intValue();}
        };
    }
    public int[] toArray(){Object[] boxed=values.toArray();int[] result=new int[boxed.length];for(int i=0;i<result.length;i++)result[i]=((Integer)boxed[i]).intValue();return result;}
    public void forEach(IntConsumer action){Objects.requireNonNull(action);values.forEach(v->action.accept(v.intValue()));}
    public void forEachOrdered(IntConsumer action){forEach(action);}
}
