package java.util.stream;
import java.util.*;
import java.util.function.*;
public final class Collectors {
    private Collectors(){}
    static final class Flags extends AbstractSet<Collector.Characteristics> {
        final HashSet<Collector.Characteristics> flags=new HashSet<Collector.Characteristics>();
        Flags(Collector.Characteristics[] values,boolean identity){Objects.requireNonNull(values);for(Collector.Characteristics value:values)flags.add(Objects.requireNonNull(value));if(identity)flags.add(Collector.Characteristics.IDENTITY_FINISH);}
        public int size(){return flags.size();}
        public boolean contains(Object value){return flags.contains(value);}
        public Iterator<Collector.Characteristics> iterator(){final Iterator<Collector.Characteristics> cursor=flags.iterator();return new Iterator<Collector.Characteristics>(){public boolean hasNext(){return cursor.hasNext();}public Collector.Characteristics next(){return cursor.next();}};}
        public boolean add(Collector.Characteristics value){throw new UnsupportedOperationException();}
        public boolean remove(Object value){throw new UnsupportedOperationException();}
        public boolean addAll(Collection<? extends Collector.Characteristics> values){throw new UnsupportedOperationException();}
        public boolean removeAll(Collection<?> values){throw new UnsupportedOperationException();}
        public boolean retainAll(Collection<?> values){throw new UnsupportedOperationException();}
        public boolean removeIf(Predicate<? super Collector.Characteristics> predicate){throw new UnsupportedOperationException();}
        public void clear(){throw new UnsupportedOperationException();}
    }
    static final class Implementation<T,A,R> implements Collector<T,A,R> {
        final Supplier<A> supplier;final BiConsumer<A,T> accumulator;final BinaryOperator<A> combiner;final Function<A,R> finisher;final Set<Collector.Characteristics> flags;
        Implementation(Supplier<A> supplier,BiConsumer<A,T> accumulator,BinaryOperator<A> combiner,Function<A,R> finisher,Collector.Characteristics[] flags,boolean identity){this.supplier=Objects.requireNonNull(supplier);this.accumulator=Objects.requireNonNull(accumulator);this.combiner=Objects.requireNonNull(combiner);this.finisher=Objects.requireNonNull(finisher);this.flags=new Flags(flags,identity);}
        public Supplier<A> supplier(){return supplier;}public BiConsumer<A,T> accumulator(){return accumulator;}public BinaryOperator<A> combiner(){return combiner;}public Function<A,R> finisher(){return finisher;}public Set<Collector.Characteristics> characteristics(){return flags;}
    }
    public static <T,C extends Collection<T>> Collector<T,?,C> toCollection(Supplier<C> factory){return Collector.of(factory,Collection<T>::add,(left,right)->{left.addAll(right);return left;});}
    public static <T> Collector<T,?,List<T>> toList(){return toCollection(ArrayList<T>::new);}
    public static <T> Collector<T,?,Set<T>> toSet(){return Collector.of(HashSet<T>::new,Set<T>::add,(left,right)->{left.addAll(right);return left;},Collector.Characteristics.UNORDERED);}
    public static <T> Collector<T,?,Set<T>> toUnmodifiableSet(){return collectingAndThen(Collectors.<T>toSet(),Set::copyOf);}
    public static <T> Collector<T,?,List<T>> toUnmodifiableList(){return collectingAndThen(Collectors.<T>toList(),values->{for(T value:values)Objects.requireNonNull(value);return new ReferenceStream.FrozenList<T>(values.toArray());});}
    public static <T,A,R,RR> Collector<T,A,RR> collectingAndThen(Collector<T,A,R> downstream,Function<R,RR> finisher){
        Objects.requireNonNull(finisher);ArrayList<Collector.Characteristics> flags=new ArrayList<Collector.Characteristics>();for(Collector.Characteristics flag:downstream.characteristics())if(flag!=Collector.Characteristics.IDENTITY_FINISH)flags.add(flag);
        return new Implementation<T,A,RR>(downstream.supplier(),downstream.accumulator(),downstream.combiner(),downstream.finisher().andThen(finisher),flags.toArray(new Collector.Characteristics[0]),false);
    }
    public static <T,U,A,R> Collector<T,?,R> mapping(Function<? super T,? extends U> mapper,Collector<? super U,A,R> downstream){
        Objects.requireNonNull(mapper);return new Implementation<T,A,R>(downstream.supplier(),(result,value)->downstream.accumulator().accept(result,mapper.apply(value)),downstream.combiner(),downstream.finisher(),downstream.characteristics().toArray(new Collector.Characteristics[0]),false);
    }
    private static <K,V> void unique(Map<K,V> map,K key,V value){
        Objects.requireNonNull(value);if(map.putIfAbsent(key,value)!=null)throw new IllegalStateException("Duplicate key");
    }
    public static <T,K,U> Collector<T,?,Map<K,U>> toMap(Function<? super T,? extends K> keys,Function<? super T,? extends U> values){
        return Collector.of(HashMap<K,U>::new,(map,item)->unique(map,keys.apply(item),values.apply(item)),(left,right)->{for(Map.Entry<K,U> entry:right.entrySet())unique(left,entry.getKey(),entry.getValue());return left;});
    }
    public static <T,K,U> Collector<T,?,Map<K,U>> toMap(Function<? super T,? extends K> keys,Function<? super T,? extends U> values,BinaryOperator<U> merger){return toMap(keys,values,merger,HashMap<K,U>::new);}
    public static <T,K,U,M extends Map<K,U>> Collector<T,?,M> toMap(Function<? super T,? extends K> keys,Function<? super T,? extends U> values,BinaryOperator<U> merger,Supplier<M> factory){
        return Collector.of(()->factory.get(),(map,item)->map.merge(keys.apply(item),values.apply(item),merger),(left,right)->{for(Map.Entry<K,U> entry:right.entrySet())left.merge(entry.getKey(),entry.getValue(),merger);return left;});
    }
    public static <T,K,U> Collector<T,?,Map<K,U>> toUnmodifiableMap(Function<? super T,? extends K> keys,Function<? super T,? extends U> values){
        Objects.requireNonNull(keys);Objects.requireNonNull(values);return collectingAndThen(toMap(keys,values),Map::copyOf);
    }
    public static <T,K,U> Collector<T,?,Map<K,U>> toUnmodifiableMap(Function<? super T,? extends K> keys,Function<? super T,? extends U> values,BinaryOperator<U> merger){
        Objects.requireNonNull(keys);Objects.requireNonNull(values);Objects.requireNonNull(merger);return collectingAndThen(toMap(keys,values,merger),Map::copyOf);
    }
    private static final class Join {
        final StringBuilder text=new StringBuilder();final String delimiter;boolean present;
        Join(String delimiter){this.delimiter=delimiter;}
        void add(CharSequence value){if(present)text.append(delimiter);text.append(value);present=true;}
        Join merge(Join other){if(other.present){if(present)text.append(delimiter);text.append(other.text);present=true;}return this;}
    }
    public static Collector<CharSequence,?,String> joining(){return joining("");}
    public static Collector<CharSequence,?,String> joining(CharSequence delimiter){return joining(delimiter,"","");}
    public static Collector<CharSequence,?,String> joining(CharSequence delimiter,CharSequence prefix,CharSequence suffix){
        final String d=Objects.requireNonNull(delimiter).toString(),p=Objects.requireNonNull(prefix).toString(),s=Objects.requireNonNull(suffix).toString();
        return Collector.of(()->new Join(d),Join::add,Join::merge,value->new StringBuilder(p).append(value.text).append(s).toString());
    }
}
