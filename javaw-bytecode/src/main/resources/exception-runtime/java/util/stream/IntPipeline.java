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

    private static OptionalInt optional(Optional<Integer> value){return value.isPresent()?OptionalInt.of(value.get().intValue()):OptionalInt.empty();}
    public int reduce(int identity,IntBinaryOperator accumulator){Objects.requireNonNull(accumulator);return values.reduce(Integer.valueOf(identity),(a,v)->Integer.valueOf(accumulator.applyAsInt(a.intValue(),v.intValue()))).intValue();}
    public OptionalInt reduce(IntBinaryOperator accumulator){Objects.requireNonNull(accumulator);return optional(values.reduce((a,v)->Integer.valueOf(accumulator.applyAsInt(a.intValue(),v.intValue()))));}
    public int sum(){return reduce(0,(a,v)->a+v);}
    public long count(){return values.count();}
    public OptionalInt min(){return reduce((a,v)->Math.min(a,v));}
    public OptionalInt max(){return reduce((a,v)->Math.max(a,v));}
    public OptionalDouble average(){long[] totals={0,0};forEach(v->{totals[0]++;totals[1]+=v;});return totals[0]==0?OptionalDouble.empty():OptionalDouble.of((double)totals[1]/totals[0]);}
    public <R> R collect(Supplier<R> supplier,ObjIntConsumer<R> accumulator,BiConsumer<R,R> combiner){Objects.requireNonNull(supplier);Objects.requireNonNull(accumulator);Objects.requireNonNull(combiner);return values.collect(supplier,(result,v)->accumulator.accept(result,v.intValue()),combiner);}
    public IntSummaryStatistics summaryStatistics(){return collect(IntSummaryStatistics::new,IntSummaryStatistics::accept,IntSummaryStatistics::combine);}


    public IntStream filter(IntPredicate predicate){Objects.requireNonNull(predicate);return from(values.filter(v->predicate.test(v.intValue())));}
    public IntStream map(IntUnaryOperator mapper){Objects.requireNonNull(mapper);return from(values.map(v->Integer.valueOf(mapper.applyAsInt(v.intValue()))));}
    public IntStream peek(IntConsumer action){Objects.requireNonNull(action);return from(values.peek(v->action.accept(v.intValue())));}
    public <U> Stream<U> mapToObj(IntFunction<? extends U> mapper){Objects.requireNonNull(mapper);return values.map(v->mapper.apply(v.intValue()));}
    public Stream<Integer> boxed(){return values.map(v->v);}

}
