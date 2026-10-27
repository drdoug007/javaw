package java.util;import java.util.function.*;
public interface Set<E> extends Collection<E>{
 default Spliterator<E> spliterator(){return Spliterators.spliterator(this,Spliterator.DISTINCT);}

 static <E>Set<E> of(){return ImmutableCollections.<E>set((E[])new Object[]{},true);}
 static <E>Set<E> of(E e0){return ImmutableCollections.<E>set((E[])new Object[]{e0},true);}
 static <E>Set<E> of(E e0,E e1){return ImmutableCollections.<E>set((E[])new Object[]{e0,e1},true);}
 static <E>Set<E> of(E e0,E e1,E e2){return ImmutableCollections.<E>set((E[])new Object[]{e0,e1,e2},true);}
 static <E>Set<E> of(E e0,E e1,E e2,E e3){return ImmutableCollections.<E>set((E[])new Object[]{e0,e1,e2,e3},true);}
 static <E>Set<E> of(E e0,E e1,E e2,E e3,E e4){return ImmutableCollections.<E>set((E[])new Object[]{e0,e1,e2,e3,e4},true);}
 static <E>Set<E> of(E e0,E e1,E e2,E e3,E e4,E e5){return ImmutableCollections.<E>set((E[])new Object[]{e0,e1,e2,e3,e4,e5},true);}
 static <E>Set<E> of(E e0,E e1,E e2,E e3,E e4,E e5,E e6){return ImmutableCollections.<E>set((E[])new Object[]{e0,e1,e2,e3,e4,e5,e6},true);}
 static <E>Set<E> of(E e0,E e1,E e2,E e3,E e4,E e5,E e6,E e7){return ImmutableCollections.<E>set((E[])new Object[]{e0,e1,e2,e3,e4,e5,e6,e7},true);}
 static <E>Set<E> of(E e0,E e1,E e2,E e3,E e4,E e5,E e6,E e7,E e8){return ImmutableCollections.<E>set((E[])new Object[]{e0,e1,e2,e3,e4,e5,e6,e7,e8},true);}
 static <E>Set<E> of(E e0,E e1,E e2,E e3,E e4,E e5,E e6,E e7,E e8,E e9){return ImmutableCollections.<E>set((E[])new Object[]{e0,e1,e2,e3,e4,e5,e6,e7,e8,e9},true);}
 @SafeVarargs static <E>Set<E> of(E... values){return ImmutableCollections.set(values,true);}
 static <E>Set<E> copyOf(Collection<? extends E> values){return ImmutableCollections.set((E[])values.toArray(),false);}
}
