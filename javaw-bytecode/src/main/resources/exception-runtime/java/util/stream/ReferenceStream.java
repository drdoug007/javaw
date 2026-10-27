package java.util.stream;
import java.util.*;
import java.util.function.*;

/** Pull-based, single-use sequential pipelines. Intermediate operations never visit elements. */
public final class ReferenceStream<T> implements Stream<T> {
    private final Supplier<Cursor<T>> factory;
    private final State state;
    private boolean used;
    private Supplier<? extends Spliterator<T>> sourceSpliterator;
    ReferenceStream<T> withSpliteratorSource(Supplier<? extends Spliterator<T>> source){sourceSpliterator=source;return this;}
    public Spliterator<T> spliterator(){check();if(sourceSpliterator==null)throw new UnsupportedOperationException("spliterator of transformed/generated reference stream");used=true;return Objects.requireNonNull(sourceSpliterator.get());}

    private ReferenceStream(Supplier<Cursor<T>> factory,State state){this.factory=factory;this.state=state;}
    private static <E extends Throwable> void rethrow(Throwable failure)throws E{throw (E)failure;}
    private static Throwable suppress(Throwable first,Throwable next){if(first==null)return next;if(first!=next)first.addSuppressed(next);return first;}
    private static final class State {
        boolean closed;
        final ArrayList<Runnable> handlers=new ArrayList<Runnable>();
        void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable handler:handlers)try{handler.run();}catch(Throwable next){failure=suppress(failure,next);}
            handlers.clear();if(failure!=null)ReferenceStream.<RuntimeException>rethrow(failure);
        }
    }
    private void check(){if(used || state.closed)throw new IllegalStateException("Stream already operated upon or closed");}
    private Supplier<Cursor<T>> link(){check();used=true;return factory;}
    private Cursor<T> consume(){return link().get();}
    private <R> ReferenceStream<R> stage(Supplier<Cursor<R>> source){return new ReferenceStream<R>(source,state);}
    private abstract static class Cursor<E> implements Iterator<E> {
        E value;boolean ready,ended;
        abstract boolean advance();
        public boolean hasNext(){if(!ready && !ended){ready=advance();if(!ready)ended=true;}return ready;}
        public E next(){if(!hasNext())throw new NoSuchElementException();E result=value;value=null;ready=false;return result;}
        void close(){}
    }
    private abstract static class Chained<E,U> extends Cursor<E> {
        final Cursor<U> upstream;
        Chained(Cursor<U> upstream){this.upstream=upstream;}
        void close(){upstream.close();}
    }
    private static <E> Cursor<E> wrap(final Iterator<? extends E> iterator){return new Cursor<E>(){
        boolean advance(){if(!iterator.hasNext())return false;value=iterator.next();return true;}
    };}
    public static <E> Stream<E> fromIterator(final Supplier<? extends Iterator<E>> factory){
        Objects.requireNonNull(factory);return new ReferenceStream<E>(()->wrap(Objects.requireNonNull(factory.get())),new State());
    }
    public static <E> Stream<E> collection(final Collection<E> values){
        Objects.requireNonNull(values);return new ReferenceStream<E>(()->wrap(values.iterator()),new State()).withSpliteratorSource(()->values.spliterator());
    }
    static <E> Stream<E> empty(){return new ReferenceStream<E>(() -> wrap(Collections.<E>emptyIterator()),new State()).withSpliteratorSource(()->Spliterators.<E>emptySpliterator());}
    public static <E> Stream<E> array(final E[] values,final int start,final int end){
        Objects.requireNonNull(values);if(start>end)throw new ArrayIndexOutOfBoundsException();if(start<0 || end>values.length)throw new ArrayIndexOutOfBoundsException();
        return new ReferenceStream<E>(()->new Cursor<E>(){int index=start;boolean advance(){if(index==end)return false;value=values[index++];return true;}},new State()).withSpliteratorSource(()->Spliterators.spliterator(values,start,end,Spliterator.ORDERED|Spliterator.IMMUTABLE));
    }
    static <E> Stream<E> generate(final Supplier<? extends E> supplier){
        Objects.requireNonNull(supplier);return new ReferenceStream<E>(()->new Cursor<E>(){boolean advance(){value=supplier.get();return true;}},new State());
    }
    static <E> Stream<E> iterate(final E seed,final Predicate<? super E> predicate,final UnaryOperator<E> next){
        Objects.requireNonNull(next);return new ReferenceStream<E>(()->new Cursor<E>(){
            E previous;boolean first=true;
            boolean advance(){E candidate=first?seed:next.apply(previous);first=false;if(predicate!=null && !predicate.test(candidate))return false;previous=candidate;value=candidate;return true;}
        },new State());
    }
    private static <E> Supplier<Cursor<E>> input(Stream<? extends E> stream){
        if(stream instanceof ReferenceStream)return (Supplier)((ReferenceStream)stream).link();
        final Iterator<? extends E> cursor=stream.iterator();return ()->wrap(cursor);
    }
    static <E> Stream<E> concat(final Stream<? extends E> left,final Stream<? extends E> right){
        Objects.requireNonNull(left);Objects.requireNonNull(right);
        final Supplier<Cursor<E>> a=input(left),b=input(right);State state=new State();
        state.handlers.add(()->{Throwable failure=null;try{left.close();}catch(Throwable next){failure=next;}try{right.close();}catch(Throwable next){failure=suppress(failure,next);}if(failure!=null)ReferenceStream.<RuntimeException>rethrow(failure);});
        return new ReferenceStream<E>(()->new Cursor<E>(){
            Cursor<E> current=a.get();boolean second;
            boolean advance(){if(current.hasNext()){value=current.next();return true;}if(second)return false;current.close();second=true;current=b.get();if(!current.hasNext())return false;value=current.next();return true;}
            void close(){current.close();}
        },state);
    }
    public boolean isParallel(){return false;}
    public Stream<T> sequential(){return this;}
    public Stream<T> unordered(){check();return this;}
    public Stream<T> onClose(Runnable handler){Objects.requireNonNull(handler);check();state.handlers.add(handler);return this;}
    public void close(){state.close();}
    public Iterator<T> iterator(){final Cursor<T> cursor=consume();state.handlers.add(()->cursor.close());return cursor;}
    public Stream<T> filter(final Predicate<? super T> predicate){
        Objects.requireNonNull(predicate);final Supplier<Cursor<T>> previous=link();
        return stage(()->new Chained<T,T>(previous.get()){boolean advance(){while(upstream.hasNext()){T candidate=upstream.next();if(predicate.test(candidate)){value=candidate;return true;}}return false;}});
    }
    public <R> Stream<R> map(final Function<? super T,? extends R> mapper){
        Objects.requireNonNull(mapper);final Supplier<Cursor<T>> previous=link();
        return stage(()->new Chained<R,T>(previous.get()){boolean advance(){if(!upstream.hasNext())return false;value=mapper.apply(upstream.next());return true;}});
    }
    public Stream<T> peek(final Consumer<? super T> action){
        Objects.requireNonNull(action);final Supplier<Cursor<T>> previous=link();
        return stage(()->new Chained<T,T>(previous.get()){boolean advance(){if(!upstream.hasNext())return false;value=upstream.next();action.accept(value);return true;}});
    }
    public <R> Stream<R> flatMap(final Function<? super T,? extends Stream<? extends R>> mapper){
        Objects.requireNonNull(mapper);final Supplier<Cursor<T>> previous=link();
        return stage(()->new Chained<R,T>(previous.get()){
            Stream<? extends R> inner;Iterator<? extends R> cursor;
            void finish(){Stream<? extends R> old=inner;inner=null;cursor=null;if(old!=null)old.close();}
            boolean advance(){while(true){if(cursor!=null && cursor.hasNext()){value=cursor.next();return true;}finish();if(!upstream.hasNext())return false;inner=mapper.apply(upstream.next());if(inner!=null)cursor=inner.iterator();}}
            void close(){Throwable failure=null;try{finish();}catch(Throwable next){failure=next;}try{upstream.close();}catch(Throwable next){failure=suppress(failure,next);}if(failure!=null)ReferenceStream.<RuntimeException>rethrow(failure);}
        });
    }
    public Stream<T> distinct(){
        final Supplier<Cursor<T>> previous=link();
        return stage(()->new Chained<T,T>(previous.get()){final HashSet<T> seen=new HashSet<T>();boolean advance(){while(upstream.hasNext()){T candidate=upstream.next();if(seen.add(candidate)){value=candidate;return true;}}return false;}});
    }
    public Stream<T> sorted(){return sorting(null);}
    public Stream<T> sorted(Comparator<? super T> comparator){return sorting(Objects.requireNonNull(comparator));}
    private Stream<T> sorting(final Comparator<? super T> comparator){
        final Supplier<Cursor<T>> previous=link();
        return stage(()->new Chained<T,T>(previous.get()){
            Iterator<T> sorted;
            boolean advance(){if(sorted==null){ArrayList<T> values=new ArrayList<T>();while(upstream.hasNext())values.add(upstream.next());values.sort(comparator);sorted=values.iterator();}if(!sorted.hasNext())return false;value=sorted.next();return true;}
        });
    }
    public Stream<T> limit(final long count){
        if(count<0)throw new IllegalArgumentException();final Supplier<Cursor<T>> previous=link();
        return stage(()->new Chained<T,T>(previous.get()){long remaining=count;boolean advance(){if(remaining==0 || !upstream.hasNext())return false;remaining--;value=upstream.next();return true;}});
    }
    public Stream<T> skip(final long count){
        if(count<0)throw new IllegalArgumentException();final Supplier<Cursor<T>> previous=link();
        return stage(()->new Chained<T,T>(previous.get()){long remaining=count;boolean advance(){while(remaining>0 && upstream.hasNext()){upstream.next();remaining--;}if(!upstream.hasNext())return false;value=upstream.next();return true;}});
    }
    public Stream<T> takeWhile(final Predicate<? super T> predicate){
        Objects.requireNonNull(predicate);final Supplier<Cursor<T>> previous=link();
        return stage(()->new Chained<T,T>(previous.get()){boolean advance(){if(!upstream.hasNext())return false;T candidate=upstream.next();if(!predicate.test(candidate))return false;value=candidate;return true;}});
    }
    public Stream<T> dropWhile(final Predicate<? super T> predicate){
        Objects.requireNonNull(predicate);final Supplier<Cursor<T>> previous=link();
        return stage(()->new Chained<T,T>(previous.get()){boolean dropping=true;boolean advance(){while(upstream.hasNext()){T candidate=upstream.next();if(dropping && predicate.test(candidate))continue;dropping=false;value=candidate;return true;}return false;}});
    }
    // Cursor cleanup closes active flatMap children, not the pipeline's user onClose handlers.
    private static void cleanup(Cursor<?> cursor,Throwable failure){try{cursor.close();}catch(Throwable next){if(failure==null)ReferenceStream.<RuntimeException>rethrow(next);else suppress(failure,next);}}
    public void forEach(Consumer<? super T> action){Objects.requireNonNull(action);Cursor<T> cursor=consume();Throwable failure=null;try{while(cursor.hasNext())action.accept(cursor.next());}catch(Throwable next){failure=next;throw next;}finally{cleanup(cursor,failure);}}
    public void forEachOrdered(Consumer<? super T> action){forEach(action);}
    public long count(){final long[] count={0};forEach(value->count[0]++);return count[0];}
    public boolean anyMatch(Predicate<? super T> predicate){return matching(predicate,0);}
    public boolean allMatch(Predicate<? super T> predicate){return matching(predicate,1);}
    public boolean noneMatch(Predicate<? super T> predicate){return matching(predicate,2);}
    private boolean matching(Predicate<? super T> predicate,int mode){
        Objects.requireNonNull(predicate);Cursor<T> cursor=consume();Throwable failure=null;
        try{while(cursor.hasNext()){boolean match=predicate.test(cursor.next());if(mode==1?!match:match)return mode==0;}return mode!=0;}catch(Throwable next){failure=next;throw next;}finally{cleanup(cursor,failure);}
    }
    public Optional<T> findFirst(){Cursor<T> cursor=consume();Throwable failure=null;try{return cursor.hasNext()?Optional.of(cursor.next()):Optional.<T>empty();}catch(Throwable next){failure=next;throw next;}finally{cleanup(cursor,failure);}}
    public Optional<T> findAny(){return findFirst();}
    public Optional<T> min(final Comparator<? super T> comparator){Objects.requireNonNull(comparator);return reduce((a,b)->comparator.compare(a,b)<=0?a:b);}
    public Optional<T> max(final Comparator<? super T> comparator){Objects.requireNonNull(comparator);return reduce((a,b)->comparator.compare(a,b)>=0?a:b);}
    public T reduce(T identity,BinaryOperator<T> accumulator){Objects.requireNonNull(accumulator);final Object[] result={identity};forEach(value->result[0]=accumulator.apply((T)result[0],value));return (T)result[0];}
    public Optional<T> reduce(BinaryOperator<T> accumulator){
        Objects.requireNonNull(accumulator);Cursor<T> cursor=consume();Throwable failure=null;
        try{if(!cursor.hasNext())return Optional.empty();T result=cursor.next();while(cursor.hasNext())result=accumulator.apply(result,cursor.next());return Optional.of(result);}catch(Throwable next){failure=next;throw next;}finally{cleanup(cursor,failure);}
    }
    public <U> U reduce(U identity,BiFunction<U,? super T,U> accumulator,BinaryOperator<U> combiner){Objects.requireNonNull(accumulator);Objects.requireNonNull(combiner);final Object[] result={identity};forEach(value->result[0]=accumulator.apply((U)result[0],value));return (U)result[0];}
    public <R> R collect(Supplier<R> supplier,BiConsumer<R,? super T> accumulator,BiConsumer<R,R> combiner){
        Objects.requireNonNull(supplier);Objects.requireNonNull(accumulator);Objects.requireNonNull(combiner);check();
        // Claim the terminal operation before invoking user code, including a throwing supplier.
        Cursor<T> cursor=consume();Throwable failure=null;
        try{R result=supplier.get();while(cursor.hasNext())accumulator.accept(result,cursor.next());return result;}catch(Throwable next){failure=next;throw next;}finally{cleanup(cursor,failure);}
    }
    public <R,A> R collect(Collector<? super T,A,R> collector){Objects.requireNonNull(collector);A result=collect(collector.supplier(),collector.accumulator(),(a,b)->collector.combiner().apply(a,b));return collector.finisher().apply(result);}
    public Object[] toArray(){return collect(ArrayList<T>::new,ArrayList<T>::add,ArrayList<T>::addAll).toArray();}
    public <A> A[] toArray(IntFunction<A[]> generator){Objects.requireNonNull(generator);ArrayList<T> values=collect(ArrayList<T>::new,ArrayList<T>::add,ArrayList<T>::addAll);return values.toArray(generator.apply(values.size()));}
    public List<T> toList(){return new FrozenList<T>(toArray());}
    static final class FrozenList<E> extends AbstractList<E> implements RandomAccess,java.io.Serializable {
        private final Object[] values;
        FrozenList(Object[] values){this.values=values;}
        public int size(){return values.length;}
        public E get(int index){index(index);return (E)values[index];}
        public E set(int index,E value){throw new UnsupportedOperationException();}
        public boolean add(E value){throw new UnsupportedOperationException();}
        public void add(int index,E value){throw new UnsupportedOperationException();}
        public E remove(int index){throw new UnsupportedOperationException();}
        public boolean remove(Object value){throw new UnsupportedOperationException();}
        public boolean addAll(Collection<? extends E> values){throw new UnsupportedOperationException();}
        public boolean addAll(int index,Collection<? extends E> values){throw new UnsupportedOperationException();}
        public boolean removeAll(Collection<?> values){throw new UnsupportedOperationException();}
        public boolean retainAll(Collection<?> values){throw new UnsupportedOperationException();}
        public boolean removeIf(Predicate<? super E> predicate){throw new UnsupportedOperationException();}
        public void clear(){throw new UnsupportedOperationException();}
        public void replaceAll(UnaryOperator<E> operator){throw new UnsupportedOperationException();}
        public void sort(Comparator<? super E> comparator){throw new UnsupportedOperationException();}
        public List<E> subList(int start,int end){if(start<0 || end>size())throw new IndexOutOfBoundsException();if(start>end)throw new IllegalArgumentException();return new FrozenList<E>(Arrays.copyOfRange(values,start,end));}
    }

    public IntStream mapToInt(ToIntFunction<? super T> mapper){Objects.requireNonNull(mapper);return IntPipeline.from(map(value->Integer.valueOf(mapper.applyAsInt(value))));}

}
