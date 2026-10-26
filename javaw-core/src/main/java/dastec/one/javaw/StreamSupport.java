package dastec.one.javaw;
import com.github.javaparser.ast.CompilationUnit;

/** Sequential pull pipelines: no intermediate traversal before a terminal operation. */
final class StreamSupport {
    static void addTypes(CompilationUnit cu) {
        LibrarySupport.add(cu,"java.util", """
            interface Comparator<T> { public int compare(T left,T right); }
            class __JavawNaturalComparator<T> implements Comparator<T> {
                public __JavawNaturalComparator() {}
                public int compare(T left,T right) { return __JavawObjects.compare(left,right); }
            }
            class __JavawArrayIterator<T> implements Iterator<T> {
                private T[] values; private int index;
                public __JavawArrayIterator(T[] values) { this.values=values; }
                public boolean hasNext() { return index<values.length; }
                public T next() {
                    if(!hasNext()) throw new NoSuchElementException();
                    T result=values[index]; index++; return result;
                }
            }
            class __JavawGenerateIterator<T> implements Iterator<T> {
                private Supplier<T> supplier;
                public __JavawGenerateIterator(Supplier<T> supplier) { this.supplier=supplier; }
                public boolean hasNext() { return true; }
                public T next() { return supplier.get(); }
            }
            class __JavawIterateIterator<T> implements Iterator<T> {
                private T value; private UnaryOperator<T> nextValue; private boolean first;
                public __JavawIterateIterator(T seed,UnaryOperator<T> nextValue) { value=seed; this.nextValue=nextValue; first=true; }
                public boolean hasNext() { return true; }
                public T next() { if(first) first=false; else value=nextValue.apply(value); return value; }
            }
            class __JavawReadOnlyList<T> extends ArrayList<T> {
                public void add(int index,T value) { throw new UnsupportedOperationException(); }
                public boolean addAll(Collection<? extends T> values) { throw new UnsupportedOperationException(); }
                public boolean addAll(int index,Collection<? extends T> values) { throw new UnsupportedOperationException(); }
                public boolean removeAll(Collection<?> values) { throw new UnsupportedOperationException(); }
                public boolean retainAll(Collection<?> values) { throw new UnsupportedOperationException(); }
                public boolean removeIf(Predicate<T> predicate) { throw new UnsupportedOperationException(); }
                public void replaceAll(UnaryOperator<T> operator) { throw new UnsupportedOperationException(); }
                public void sort(Comparator<T> comparator) { throw new UnsupportedOperationException(); }
                public __JavawReadOnlyList(Object[] values) { super(); for(int i=0;i<values.length;i++) super.add((T)values[i]); }
                public boolean add(T value) { throw new UnsupportedOperationException(); }
                public T set(int index,T value) { throw new UnsupportedOperationException(); }
                public T remove(int index) { throw new UnsupportedOperationException(); }
                public boolean remove(Object value) { throw new UnsupportedOperationException(); }
                public void clear() { throw new UnsupportedOperationException(); }
            }
            """);
        LibrarySupport.add(cu,"java.util.stream", """
            class __JavawStreamIterator<T> implements Iterator<T> {
                private Stream<T> stream; private boolean prepared; private boolean available;
                public __JavawStreamIterator(Stream<T> stream) { this.stream=stream; }
                public boolean hasNext() {
                    if(!prepared) { available=stream.__advance(); prepared=true; } return available;
                }
                public T next() {
                    if(!hasNext()) throw new NoSuchElementException(); prepared=false; return stream.__current();
                }
            }
            class Stream<T> {
                private Iterator<T> source;
                private Iterable<T> sourceCollection;
                private Stream parent;
                private int stage;
                private Function mapper;
                private Predicate predicate;
                private Consumer observer;
                private Stream inner;
                private HashSet<Object> seen;
                private long remaining;
                private Object current;
                private Comparator<T> ordering;
                private ArrayList<T> sortedValues;
                private int sortedIndex;
                private boolean prefixDone;
                private boolean used;
                private boolean closed;
                private Stream(Iterator<T> source) { this.source=source; }
                private Stream(Stream parent,int stage) { this.parent=parent; this.stage=stage; }
                public static <U> Stream<U> of(U... values) {
                    if(values==null) throw new NullPointerException();
                    return new Stream<U>(new __JavawArrayIterator<U>(values));
                }
                public static <U> Stream<U> empty() { return Stream.<U>of(); }
                public static <U> Stream<U> from(Iterator<U> iterator) { return new Stream<U>(iterator); }
                public static <U> Stream<U> fromCollection(Iterable<U> collection) {
                    Stream<U> result=new Stream<U>((Iterator<U>)null); result.sourceCollection=collection; return result;
                }
                public static <U> Stream<U> generate(Supplier<U> supplier) {
                    if(supplier==null) throw new NullPointerException();
                    return new Stream<U>(new __JavawGenerateIterator<U>(supplier));
                }
                public static <U> Stream<U> iterate(U seed,UnaryOperator<U> nextValue) {
                    if(nextValue==null) throw new NullPointerException();
                    return new Stream<U>(new __JavawIterateIterator<U>(seed,nextValue));
                }
                private void available() { if(used || closed) throw new IllegalStateException(); }
                private void start() { available(); used=true; }
                private Stream<T> link(int stage) { start(); return new Stream<T>(this,stage); }
                public <R> Stream<R> map(Function<T,R> mapper) {
                    if(mapper==null) throw new NullPointerException();
                    start(); Stream<R> result=new Stream<R>(this,1); result.mapper=mapper; return result;
                }
                public Stream<T> filter(Predicate<T> predicate) {
                    if(predicate==null) throw new NullPointerException();
                    Stream<T> result=link(2); result.predicate=predicate; return result;
                }
                public Stream<T> peek(Consumer<T> observer) {
                    if(observer==null) throw new NullPointerException();
                    Stream<T> result=link(3); result.observer=observer; return result;
                }
                public Stream<T> limit(long count) {
                    if(count<0) throw new IllegalArgumentException();
                    Stream<T> result=link(4); result.remaining=count; return result;
                }
                public Stream<T> skip(long count) {
                    if(count<0) throw new IllegalArgumentException();
                    Stream<T> result=link(5); result.remaining=count; return result;
                }
                public Stream<T> distinct() {
                    Stream<T> result=link(6); result.seen=new HashSet<Object>(); return result;
                }
                public <R> Stream<R> flatMap(Function<T,Stream<R>> mapper) {
                    if(mapper==null) throw new NullPointerException();
                    start(); Stream<R> result=new Stream<R>(this,7); result.mapper=mapper; return result;
                }
                public Stream<T> sorted() { return sorted(new __JavawNaturalComparator<T>()); }
                public Stream<T> sorted(Comparator<T> comparator) {
                    if(comparator==null) throw new NullPointerException();
                    Stream<T> result=link(8); result.ordering=comparator; return result;
                }
                public Stream<T> takeWhile(Predicate<T> predicate) {
                    if(predicate==null) throw new NullPointerException();
                    Stream<T> result=link(9); result.predicate=predicate; return result;
                }
                public Stream<T> dropWhile(Predicate<T> predicate) {
                    if(predicate==null) throw new NullPointerException();
                    Stream<T> result=link(10); result.predicate=predicate; return result;
                }
                private boolean advance() {
                    if(closed) throw new IllegalStateException();
                    if(stage==0) {
                        if(source==null) source=sourceCollection.iterator();
                        if(!source.hasNext()) return false;
                        current=source.next(); return true;
                    }
                    if(stage==8) {
                        if(sortedValues==null) {
                            sortedValues=new ArrayList<T>();
                            while(parent.advance()) sortedValues.add((T)parent.current);
                            // Stable insertion sort retains encounter order for equal keys.
                            for(int i=1;i<sortedValues.size();i++) {
                                T value=sortedValues.get(i); int j=i;
                                while(j>0 && ordering.compare(sortedValues.get(j-1),value)>0) {
                                    sortedValues.set(j,sortedValues.get(j-1)); j--;
                                }
                                sortedValues.set(j,value);
                            }
                        }
                        if(sortedIndex>=sortedValues.size()) return false;
                        current=sortedValues.get(sortedIndex); sortedIndex++; return true;
                    }
                    if(stage==9 && prefixDone) return false;
                    if(stage==4 && remaining==0) return false;
                    if(stage==7) {
                        while(true) {
                            if(inner!=null && inner.advance()) { current=inner.current; return true; }
                            if(inner!=null) { inner.close(); inner=null; }
                            if(!parent.advance()) return false;
                            inner=(Stream)mapper.apply(parent.current);
                            if(inner!=null) inner.start();
                        }
                    }
                    while(parent.advance()) {
                        Object value=parent.current;
                        if(stage==9 && !predicate.test(value)) { prefixDone=true; return false; }
                        if(stage==10 && !prefixDone) { if(predicate.test(value)) continue; prefixDone=true; }
                        if(stage==2 && !predicate.test(value)) continue;
                        if(stage==5 && remaining>0) { remaining--; continue; }
                        if(stage==6 && !seen.add(value)) continue;
                        if(stage==1) value=mapper.apply(value);
                        if(stage==3) observer.accept(value);
                        if(stage==4) remaining--;
                        current=value; return true;
                    }
                    return false;
                }
                public long count() { start(); long count=0; while(advance()) count++; return count; }
                public Optional<T> findFirst() {
                    start(); if(advance()) return Optional.of((T)current); return Optional.<T>empty();
                }
                public Optional<T> findAny() { return findFirst(); }
                public boolean anyMatch(Predicate<T> predicate) {
                    if(predicate==null) throw new NullPointerException(); start();
                    while(advance()) if(predicate.test((T)current)) return true; return false;
                }
                public boolean allMatch(Predicate<T> predicate) {
                    if(predicate==null) throw new NullPointerException(); start();
                    while(advance()) if(!predicate.test((T)current)) return false; return true;
                }
                public boolean noneMatch(Predicate<T> predicate) {
                    if(predicate==null) throw new NullPointerException(); start();
                    while(advance()) if(predicate.test((T)current)) return false; return true;
                }
                public void forEach(Consumer<T> action) {
                    if(action==null) throw new NullPointerException(); start();
                    while(advance()) action.accept((T)current);
                }
                public void forEachOrdered(Consumer<T> action) { forEach(action); }
                public T reduce(T identity,BinaryOperator<T> accumulator) {
                    if(accumulator==null) throw new NullPointerException(); start();
                    T result=identity; while(advance()) result=accumulator.apply(result,(T)current); return result;
                }
                public Optional<T> reduce(BinaryOperator<T> accumulator) {
                    if(accumulator==null) throw new NullPointerException(); start();
                    if(!advance()) return Optional.<T>empty();
                    T result=(T)current; while(advance()) result=accumulator.apply(result,(T)current); return Optional.of(result);
                }
                public Optional<T> min(Comparator<T> comparator) {
                    if(comparator==null) throw new NullPointerException(); start();
                    if(!advance()) return Optional.<T>empty();
                    T result=(T)current; while(advance()) if(comparator.compare((T)current,result)<0) result=(T)current;
                    return Optional.of(result);
                }
                public Optional<T> max(Comparator<T> comparator) {
                    if(comparator==null) throw new NullPointerException(); start();
                    if(!advance()) return Optional.<T>empty();
                    T result=(T)current; while(advance()) if(comparator.compare((T)current,result)>0) result=(T)current;
                    return Optional.of(result);
                }
                public Object[] toArray() {
                    start(); ArrayList<T> result=new ArrayList<T>();
                    while(advance()) result.add((T)current); return result.toArray();
                }
                public <A,R> R collect(Collector<T,A,R> collector) {
                    if(collector==null) throw new NullPointerException(); start();
                    A container=collector.supplier().get();
                    BiConsumer<A,T> accumulator=collector.accumulator();
                    while(advance()) accumulator.accept(container,(T)current);
                    return collector.finisher().apply(container);
                }
                public List<T> toList() { return new __JavawReadOnlyList<T>(toArray()); }
                public Iterator<T> iterator() { start(); return new __JavawStreamIterator<T>(this); }
                public boolean __advance() { return advance(); }
                public T __current() { return (T)current; }
                public IntStream mapToInt(ToIntFunction<T> mapper) { if(mapper==null) throw new NullPointerException(); return new IntStream(map(new __JavawToInt<T>(mapper))); }
                public LongStream mapToLong(ToLongFunction<T> mapper) { if(mapper==null) throw new NullPointerException(); return new LongStream(map(new __JavawToLong<T>(mapper))); }
                public DoubleStream mapToDouble(ToDoubleFunction<T> mapper) { if(mapper==null) throw new NullPointerException(); return new DoubleStream(map(new __JavawToDouble<T>(mapper))); }
                public boolean isParallel() { return false; }
                public Stream<T> sequential() { available(); return this; }
                public Stream<T> parallel() { throw new UnsupportedOperationException(); }
                public void close() { closed=true; if(inner!=null) inner.close(); if(parent!=null) parent.close(); }
            }
            """);
    }
}
