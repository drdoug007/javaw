package java.util.stream;
import java.util.*;
import java.util.function.*;
/** Adapter sharing reference pipeline single-use, traversal and close state. */
public final class LongPipeline implements LongStream {
    private final Stream<Long> values;
    private LongPipeline(Stream<Long> values){this.values=values;}
    public static LongStream from(Stream<Long> values){return new LongPipeline(Objects.requireNonNull(values));}
    public static LongStream source(Supplier<? extends Iterator<Long>> factory){return from(ReferenceStream.fromIterator(factory));}
    static LongStream array(final long[] data){
        Objects.requireNonNull(data);return source(()->new Iterator<Long>(){
            int index;public boolean hasNext(){return index<data.length;}
            public Long next(){if(!hasNext())throw new NoSuchElementException();return Long.valueOf(data[index++]);}
        });
    }
    static LongStream range(final long start,final long end,final boolean closed){
        // Walk start..end inclusive (end-1 when open) with a done flag: position++ may wrap at Long.MAX_VALUE.
        return source(()->new Iterator<Long>(){
            long position=start;boolean done;
            public boolean hasNext(){return !done && (closed ? position<=end : position<end);}
            public Long next(){if(!hasNext())throw new NoSuchElementException();long value=position++;if(closed ? value==end : value==end-1)done=true;return Long.valueOf(value);}
        });
    }
    public boolean isParallel(){return false;}
    public LongStream sequential(){values.sequential();return this;}
    public LongStream unordered(){values.unordered();return this;}
    public LongStream onClose(Runnable handler){values.onClose(handler);return this;}
    public void close(){values.close();}
    public PrimitiveIterator.OfLong iterator(){
        final Iterator<Long> cursor=values.iterator();return new PrimitiveIterator.OfLong(){
            public boolean hasNext(){return cursor.hasNext();}public long nextLong(){return cursor.next().longValue();}
        };
    }
    public long[] toArray(){Object[] boxed=values.toArray();long[] result=new long[boxed.length];for(int i=0;i<result.length;i++)result[i]=((Long)boxed[i]).longValue();return result;}
    public void forEach(LongConsumer action){Objects.requireNonNull(action);values.forEach(v->action.accept(v.longValue()));}
    public void forEachOrdered(LongConsumer action){forEach(action);}

    private static OptionalLong optional(Optional<Long> value){return value.isPresent()?OptionalLong.of(value.get().longValue()):OptionalLong.empty();}
    public long reduce(long identity,LongBinaryOperator accumulator){Objects.requireNonNull(accumulator);return values.reduce(Long.valueOf(identity),(a,v)->Long.valueOf(accumulator.applyAsLong(a.longValue(),v.longValue()))).longValue();}
    public OptionalLong reduce(LongBinaryOperator accumulator){Objects.requireNonNull(accumulator);return optional(values.reduce((a,v)->Long.valueOf(accumulator.applyAsLong(a.longValue(),v.longValue()))));}
    public long sum(){return reduce(0L,(a,v)->a+v);}
    public long count(){return values.count();}
    public OptionalLong min(){return reduce((a,v)->Math.min(a,v));}
    public OptionalLong max(){return reduce((a,v)->Math.max(a,v));}
    public OptionalDouble average(){long[] totals={0L,0L};forEach(v->{totals[0]++;totals[1]+=v;});return totals[0]==0?OptionalDouble.empty():OptionalDouble.of((double)totals[1]/totals[0]);}


    public LongStream filter(LongPredicate predicate){Objects.requireNonNull(predicate);return from(values.filter(v->predicate.test(v.longValue())));}
    public LongStream map(LongUnaryOperator mapper){Objects.requireNonNull(mapper);return from(values.map(v->Long.valueOf(mapper.applyAsLong(v.longValue()))));}
    public LongStream peek(LongConsumer action){Objects.requireNonNull(action);return from(values.peek(v->action.accept(v.longValue())));}
    public <U> Stream<U> mapToObj(LongFunction<? extends U> mapper){Objects.requireNonNull(mapper);return values.map(v->mapper.apply(v.longValue()));}
    public Stream<Long> boxed(){return values.map(v->v);}


    public LongStream distinct(){return from(values.distinct());}public LongStream sorted(){return from(values.sorted());}
    public LongStream limit(long count){return from(values.limit(count));}public LongStream skip(long count){return from(values.skip(count));}
    public boolean anyMatch(LongPredicate predicate){Objects.requireNonNull(predicate);return values.anyMatch(v->predicate.test(v.longValue()));}
    public boolean allMatch(LongPredicate predicate){Objects.requireNonNull(predicate);return values.allMatch(v->predicate.test(v.longValue()));}
    public boolean noneMatch(LongPredicate predicate){Objects.requireNonNull(predicate);return values.noneMatch(v->predicate.test(v.longValue()));}
    public OptionalLong findFirst(){return optional(values.findFirst());}public OptionalLong findAny(){return optional(values.findAny());}

}
