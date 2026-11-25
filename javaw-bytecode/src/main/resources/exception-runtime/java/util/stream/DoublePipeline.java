package java.util.stream;
import java.util.*;
import java.util.function.*;
/** Adapter sharing reference pipeline single-use, traversal and close state. */
public final class DoublePipeline implements DoubleStream {
    private final Stream<Double> values;
    private DoublePipeline(Stream<Double> values){this.values=values;}
    public static DoubleStream from(Stream<Double> values){return new DoublePipeline(Objects.requireNonNull(values));}
    public static DoubleStream source(Supplier<? extends Iterator<Double>> factory){return from(ReferenceStream.fromIterator(factory));}
    static DoubleStream array(final double[] data){
        Objects.requireNonNull(data);return source(()->new Iterator<Double>(){
            int index;public boolean hasNext(){return index<data.length;}
            public Double next(){if(!hasNext())throw new NoSuchElementException();return Double.valueOf(data[index++]);}
        });
    }
    public boolean isParallel(){return false;}
    public DoubleStream sequential(){values.sequential();return this;}
    public DoubleStream unordered(){values.unordered();return this;}
    public DoubleStream onClose(Runnable handler){values.onClose(handler);return this;}
    public void close(){values.close();}
    public PrimitiveIterator.OfDouble iterator(){
        final Iterator<Double> cursor=values.iterator();return new PrimitiveIterator.OfDouble(){
            public boolean hasNext(){return cursor.hasNext();}public double nextDouble(){return cursor.next().doubleValue();}
        };
    }
    public double[] toArray(){Object[] boxed=values.toArray();double[] result=new double[boxed.length];for(int i=0;i<result.length;i++)result[i]=((Double)boxed[i]).doubleValue();return result;}
    public void forEach(DoubleConsumer action){Objects.requireNonNull(action);values.forEach(v->action.accept(v.doubleValue()));}
    public void forEachOrdered(DoubleConsumer action){forEach(action);}

    private static OptionalDouble optional(Optional<Double> value){return value.isPresent()?OptionalDouble.of(value.get().doubleValue()):OptionalDouble.empty();}
    public double reduce(double identity,DoubleBinaryOperator accumulator){Objects.requireNonNull(accumulator);return values.reduce(Double.valueOf(identity),(a,v)->Double.valueOf(accumulator.applyAsDouble(a.doubleValue(),v.doubleValue()))).doubleValue();}
    public OptionalDouble reduce(DoubleBinaryOperator accumulator){Objects.requireNonNull(accumulator);return optional(values.reduce((a,v)->Double.valueOf(accumulator.applyAsDouble(a.doubleValue(),v.doubleValue()))));}
    // JDK DoublePipeline.sum: Kahan (compensated) summation, plus the plain sum in the last slot.
    private static void sumWithCompensation(double[] s,double d){
        double tmp=d-s[1];double sum=s[0];double velvel=sum+tmp;
        s[1]=(velvel-sum)-tmp;s[0]=velvel;
    }
    private static double computeFinalSum(double[] s){
        double tmp=s[0]-s[1];double simple=s[s.length-1];
        return (Double.isNaN(tmp)&&Double.isInfinite(simple))?simple:tmp;
    }
    public double sum(){final double[] s=new double[3];forEach(v->{sumWithCompensation(s,v);s[2]+=v;});return computeFinalSum(s);}
    public long count(){return values.count();}
    public OptionalDouble min(){return reduce((a,v)->Math.min(a,v));}
    public OptionalDouble max(){return reduce((a,v)->Math.max(a,v));}
    public OptionalDouble average(){final double[] s=new double[4];forEach(v->{s[2]++;sumWithCompensation(s,v);s[3]+=v;});return s[2]>0?OptionalDouble.of(computeFinalSum(s)/s[2]):OptionalDouble.empty();}


    public DoubleStream filter(DoublePredicate predicate){Objects.requireNonNull(predicate);return from(values.filter(v->predicate.test(v.doubleValue())));}
    public DoubleStream map(DoubleUnaryOperator mapper){Objects.requireNonNull(mapper);return from(values.map(v->Double.valueOf(mapper.applyAsDouble(v.doubleValue()))));}
    public DoubleStream peek(DoubleConsumer action){Objects.requireNonNull(action);return from(values.peek(v->action.accept(v.doubleValue())));}
    public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper){Objects.requireNonNull(mapper);return values.map(v->mapper.apply(v.doubleValue()));}
    public Stream<Double> boxed(){return values.map(v->v);}


    public DoubleStream distinct(){return from(values.distinct());}public DoubleStream sorted(){return from(values.sorted());}
    public DoubleStream limit(long count){return from(values.limit(count));}public DoubleStream skip(long count){return from(values.skip(count));}
    public boolean anyMatch(DoublePredicate predicate){Objects.requireNonNull(predicate);return values.anyMatch(v->predicate.test(v.doubleValue()));}
    public boolean allMatch(DoublePredicate predicate){Objects.requireNonNull(predicate);return values.allMatch(v->predicate.test(v.doubleValue()));}
    public boolean noneMatch(DoublePredicate predicate){Objects.requireNonNull(predicate);return values.noneMatch(v->predicate.test(v.doubleValue()));}
    public OptionalDouble findFirst(){return optional(values.findFirst());}public OptionalDouble findAny(){return optional(values.findAny());}

}
