package com.example.javaw;
import com.github.javaparser.ast.CompilationUnit;

/** Standard mutable-reduction contract and reusable source-backed collector factories. */
final class CollectorSupport {
    static void addTypes(CompilationUnit cu) {
        LibrarySupport.add(cu,"java.util.stream", """
            interface Collector<T,A,R> {
                enum Characteristics { CONCURRENT, UNORDERED, IDENTITY_FINISH }
                public Supplier<A> supplier();
                public BiConsumer<A,T> accumulator();
                public BinaryOperator<A> combiner();
                public Function<A,R> finisher();
                public Set<Characteristics> characteristics();
                public static <T,A> Collector<T,A,A> of(Supplier<A> supplier,BiConsumer<A,T> accumulator,BinaryOperator<A> combiner,Characteristics... characteristics) {
                    return new __JavawCollector<T,A,A>(supplier,accumulator,combiner,new __JavawCastIdentity<A,A>(),characteristics,true);
                }
                public static <T,A,R> Collector<T,A,R> of(Supplier<A> supplier,BiConsumer<A,T> accumulator,BinaryOperator<A> combiner,Function<A,R> finisher,Characteristics... characteristics) {
                    return new __JavawCollector<T,A,R>(supplier,accumulator,combiner,finisher,characteristics,false);
                }
            }
            class __JavawCastIdentity<A,R> implements Function<A,R> {
                public __JavawCastIdentity() {}
                public R apply(A value) { return (R)value; }
            }
            class __JavawCharacteristics implements Set<Characteristics> {
                private HashSet<Characteristics> values;
                public __JavawCharacteristics(Characteristics[] values,boolean identity) {
                    if(values==null) throw new NullPointerException(); this.values=new HashSet<Characteristics>();
                    for(int i=0;i<values.length;i++) { if(values[i]==null) throw new NullPointerException(); this.values.add(values[i]); }
                    if(identity) this.values.add(Characteristics.IDENTITY_FINISH);
                }
                public static Characteristics[] copy(Set<Characteristics> source,boolean stripIdentity) {
                    int count=source.size(); if(stripIdentity && source.contains(Characteristics.IDENTITY_FINISH)) count--;
                    Characteristics[] result=new Characteristics[count]; int i=0; Iterator<Characteristics> it=source.iterator();
                    while(it.hasNext()) { Characteristics value=it.next(); if(!stripIdentity || value!=Characteristics.IDENTITY_FINISH) { result[i]=value; i++; } } return result;
                }
                public int size() { return values.size(); }
                public boolean isEmpty() { return values.isEmpty(); }
                public boolean contains(Object value) { return values.contains(value); }
                public boolean add(Characteristics value) { throw new UnsupportedOperationException(); }
                public boolean remove(Object value) { throw new UnsupportedOperationException(); }
                public void clear() { throw new UnsupportedOperationException(); }
                public Iterator<Characteristics> iterator() { return new __JavawReadOnlyIterator<Characteristics>(values.iterator()); }
                public Stream<Characteristics> stream() { return Stream.fromCollection(this); }
            }
            class __JavawCollector<T,A,R> implements Collector<T,A,R> {
                private Supplier<A> source; private BiConsumer<A,T> action;
                private BinaryOperator<A> merge; private Function<A,R> finish; private Set<Characteristics> flags;
                public __JavawCollector(Supplier<A> source,BiConsumer<A,T> action,BinaryOperator<A> merge,Function<A,R> finish,Characteristics[] flags,boolean identity) {
                    if(source==null || action==null || merge==null || finish==null) throw new NullPointerException();
                    this.source=source; this.action=action; this.merge=merge; this.finish=finish; this.flags=new __JavawCharacteristics(flags,identity);
                }
                public Supplier<A> supplier() { return source; }
                public BiConsumer<A,T> accumulator() { return action; }
                public BinaryOperator<A> combiner() { return merge; }
                public Function<A,R> finisher() { return finish; }
                public Set<Characteristics> characteristics() { return flags; }
            }
            class __JavawListSource<T> implements Supplier<List<T>> { public __JavawListSource() {} public List<T> get() { return new ArrayList<T>(); } }
            class __JavawListAcc<T> implements BiConsumer<List<T>,T> { public __JavawListAcc() {} public void accept(List<T> list,T value) { list.add(value); } }
            class __JavawListMerge<T> implements BinaryOperator<List<T>> {
                public __JavawListMerge() {}
                public List<T> apply(List<T> first,List<T> second) { Iterator<T> iterator=second.iterator(); while(iterator.hasNext()) first.add(iterator.next()); return first; }
            }
            class __JavawSetSource<T> implements Supplier<Set<T>> { public __JavawSetSource() {} public Set<T> get() { return new HashSet<T>(); } }
            class __JavawSetAcc<T> implements BiConsumer<Set<T>,T> { public __JavawSetAcc() {} public void accept(Set<T> set,T value) { set.add(value); } }
            class __JavawSetMerge<T> implements BinaryOperator<Set<T>> {
                public __JavawSetMerge() {}
                public Set<T> apply(Set<T> first,Set<T> second) { Iterator<T> iterator=second.iterator(); while(iterator.hasNext()) first.add(iterator.next()); return first; }
            }
            class __JavawJoinBuffer {
                public String text; public int count; public String delimiter;
                public __JavawJoinBuffer(String delimiter) { this.delimiter=delimiter; text=""; }
                public void add(String value) { if(count>0) text=text+delimiter; text=text+(value==null ? "null" : value); count++; }
            }
            class __JavawJoinSource implements Supplier<__JavawJoinBuffer> {
                private String delimiter;
                public __JavawJoinSource(String delimiter) { this.delimiter=delimiter; }
                public __JavawJoinBuffer get() { return new __JavawJoinBuffer(delimiter); }
            }
            class __JavawJoinAcc implements BiConsumer<__JavawJoinBuffer,String> {
                public __JavawJoinAcc() {} public void accept(__JavawJoinBuffer buffer,String value) { buffer.add(value); }
            }
            class __JavawJoinMerge implements BinaryOperator<__JavawJoinBuffer> {
                public __JavawJoinMerge() {}
                public __JavawJoinBuffer apply(__JavawJoinBuffer first,__JavawJoinBuffer second) {
                    if(second.count>0) { if(first.count>0) first.text=first.text+first.delimiter; first.text=first.text+second.text; first.count+=second.count; } return first;
                }
            }
            class __JavawJoinFinish implements Function<__JavawJoinBuffer,String> {
                private String prefix; private String suffix;
                public __JavawJoinFinish(String prefix,String suffix) { this.prefix=prefix; this.suffix=suffix; }
                public String apply(__JavawJoinBuffer buffer) { return prefix+buffer.text+suffix; }
            }
            class __JavawMappingAcc<T,U,A> implements BiConsumer<A,T> {
                private Function<T,U> mapper; private BiConsumer<A,U> downstream;
                public __JavawMappingAcc(Function<T,U> mapper,BiConsumer<A,U> downstream) { this.mapper=mapper; this.downstream=downstream; }
                public void accept(A container,T value) { downstream.accept(container,mapper.apply(value)); }
            }
            class __JavawFilteringAcc<T,A> implements BiConsumer<A,T> {
                private Predicate<T> predicate; private BiConsumer<A,T> downstream;
                public __JavawFilteringAcc(Predicate<T> predicate,BiConsumer<A,T> downstream) { this.predicate=predicate; this.downstream=downstream; }
                public void accept(A container,T value) { if(predicate.test(value)) downstream.accept(container,value); }
            }
            class __JavawCountAcc<T> implements BiConsumer<long[],T> { public __JavawCountAcc() {} public void accept(long[] count,T value) { count[0]++; } }
            class __JavawReduceSource<T> implements Supplier<Object[]> {
                private T identity;
                public __JavawReduceSource(T identity) { this.identity=identity; }
                public Object[] get() { Object[] result=new Object[2]; result[0]=identity; result[1]=false; return result; }
            }
            class __JavawReduceAcc<T> implements BiConsumer<Object[],T> {
                private BinaryOperator<T> operator; private boolean optional;
                public __JavawReduceAcc(BinaryOperator<T> operator,boolean optional) { this.operator=operator; this.optional=optional; }
                public void accept(Object[] result,T value) {
                    if(optional && !((Boolean)result[1])) result[0]=value; else result[0]=operator.apply((T)result[0],value);
                    result[1]=true;
                }
            }
            class __JavawReduceMerge<T> implements BinaryOperator<Object[]> {
                private BinaryOperator<T> operator; private boolean optional;
                public __JavawReduceMerge(BinaryOperator<T> operator,boolean optional) { this.operator=operator; this.optional=optional; }
                public Object[] apply(Object[] first,Object[] second) {
                    if(!optional || (Boolean)second[1]) {
                        if(optional && !((Boolean)first[1])) first[0]=second[0]; else first[0]=operator.apply((T)first[0],(T)second[0]); first[1]=true;
                    } return first;
                }
            }
            class __JavawReduceFinish<T> implements Function<Object[],T> { public __JavawReduceFinish() {} public T apply(Object[] value) { return (T)value[0]; } }
            class __JavawOptionalReduceFinish<T> implements Function<Object[],Optional<T>> {
                public __JavawOptionalReduceFinish() {}
                public Optional<T> apply(Object[] value) { return (Boolean)value[1] ? Optional.of((T)value[0]) : Optional.<T>empty(); }
            }
            class Collectors {
                public static <T> Collector<T,long[],Long> counting() {
                    return new __JavawCollector<T,long[],Long>(new __JavawLongSumSource(),new __JavawCountAcc<T>(),new __JavawLongSumMerge(),new __JavawLongSumFinish(),new Characteristics[0],false);
                }
                public static <T> Collector<T,Object[],T> reducing(T identity,BinaryOperator<T> operator) {
                    if(operator==null) throw new NullPointerException();
                    return new __JavawCollector<T,Object[],T>(new __JavawReduceSource<T>(identity),new __JavawReduceAcc<T>(operator,false),new __JavawReduceMerge<T>(operator,false),new __JavawReduceFinish<T>(),new Characteristics[0],false);
                }
                public static <T> Collector<T,Object[],Optional<T>> reducing(BinaryOperator<T> operator) {
                    if(operator==null) throw new NullPointerException();
                    return new __JavawCollector<T,Object[],Optional<T>>(new __JavawReduceSource<T>(null),new __JavawReduceAcc<T>(operator,true),new __JavawReduceMerge<T>(operator,true),new __JavawOptionalReduceFinish<T>(),new Characteristics[0],false);
                }
                public static <T> Collector<T,List<T>,List<T>> toList() {
                    return new __JavawCollector<T,List<T>,List<T>>(new __JavawListSource<T>(),new __JavawListAcc<T>(),new __JavawListMerge<T>(),new __JavawCastIdentity<List<T>,List<T>>(),new Characteristics[0],true);
                }
                public static <T> Collector<T,Set<T>,Set<T>> toSet() {
                    Characteristics[] flags=new Characteristics[1]; flags[0]=Characteristics.UNORDERED;
                    return new __JavawCollector<T,Set<T>,Set<T>>(new __JavawSetSource<T>(),new __JavawSetAcc<T>(),new __JavawSetMerge<T>(),new __JavawCastIdentity<Set<T>,Set<T>>(),flags,true);
                }
                public static Collector<String,__JavawJoinBuffer,String> joining() { return Collectors.joining(""); }
                public static Collector<String,__JavawJoinBuffer,String> joining(String delimiter) { return Collectors.joining(delimiter,"",""); }
                public static Collector<String,__JavawJoinBuffer,String> joining(String delimiter,String prefix,String suffix) {
                    if(delimiter==null || prefix==null || suffix==null) throw new NullPointerException();
                    return new __JavawCollector<String,__JavawJoinBuffer,String>(new __JavawJoinSource(delimiter),new __JavawJoinAcc(),new __JavawJoinMerge(),new __JavawJoinFinish(prefix,suffix),new Characteristics[0],false);
                }
                public static <T,U,A,R> Collector<T,A,R> mapping(Function<T,U> mapper,Collector<U,A,R> downstream) {
                    if(mapper==null || downstream==null) throw new NullPointerException();
                    return new __JavawCollector<T,A,R>(downstream.supplier(),new __JavawMappingAcc<T,U,A>(mapper,downstream.accumulator()),downstream.combiner(),downstream.finisher(),__JavawCharacteristics.copy(downstream.characteristics(),false),false);
                }
                public static <T,A,R> Collector<T,A,R> filtering(Predicate<T> predicate,Collector<T,A,R> downstream) {
                    if(predicate==null || downstream==null) throw new NullPointerException();
                    return new __JavawCollector<T,A,R>(downstream.supplier(),new __JavawFilteringAcc<T,A>(predicate,downstream.accumulator()),downstream.combiner(),downstream.finisher(),__JavawCharacteristics.copy(downstream.characteristics(),false),false);
                }
                public static <T,A,R,RR> Collector<T,A,RR> collectingAndThen(Collector<T,A,R> downstream,Function<R,RR> finisher) {
                    if(downstream==null || finisher==null) throw new NullPointerException();
                    return new __JavawCollector<T,A,RR>(downstream.supplier(),downstream.accumulator(),downstream.combiner(),downstream.finisher().andThen(finisher),__JavawCharacteristics.copy(downstream.characteristics(),true),false);
                }
            }
            """);
        addNumericFactories(cu);
    }
    private static void addNumericFactories(CompilationUnit cu) {
        for(String kind:new String[]{"Int","Long","Double"}) {
            String p=kind.equals("Int") ? "int" : kind.toLowerCase(), b=kind.equals("Int") ? "Integer" : kind;
            LibrarySupport.add(cu,"java.util.stream", """
                class __Javaw$KSumSource implements Supplier<$P[]> { public __Javaw$KSumSource() {} public $P[] get() { return new $P[1]; } }
                class __Javaw$KSumAcc<T> implements BiConsumer<$P[],T> {
                    private To$KFunction<T> mapper;
                    public __Javaw$KSumAcc(To$KFunction<T> mapper) { this.mapper=mapper; }
                    public void accept($P[] sum,T value) { sum[0]+=mapper.applyAs$K(value); }
                }
                class __Javaw$KSumMerge implements BinaryOperator<$P[]> {
                    public __Javaw$KSumMerge() {} public $P[] apply($P[] first,$P[] second) { first[0]+=second[0]; return first; }
                }
                class __Javaw$KSumFinish implements Function<$P[],$B> { public __Javaw$KSumFinish() {} public $B apply($P[] sum) { return sum[0]; } }
                """.replace("$K",kind).replace("$P",p).replace("$B",b));
            var owner=cu.getTypes().stream().filter(t->t.getNameAsString().equals("Collectors")).findFirst().orElseThrow();
            owner.addMember(com.github.javaparser.StaticJavaParser.parseBodyDeclaration("public static <T> Collector<T,"+p+"[],"+b+"> summing"+kind+"(To"+kind+"Function<T> mapper) { if(mapper==null) throw new NullPointerException(); return new __JavawCollector<T,"+p+"[],"+b+">(new __Javaw"+kind+"SumSource(),new __Javaw"+kind+"SumAcc<T>(mapper),new __Javaw"+kind+"SumMerge(),new __Javaw"+kind+"SumFinish(),new Characteristics[0],false); }"));
        }
    }
}
