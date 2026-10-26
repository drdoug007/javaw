package dastec.one.javaw;
import com.github.javaparser.ast.CompilationUnit;

/** Primitive API adapters retain the shared lazy pipeline engine and primitive terminal arithmetic. */
final class PrimitiveStreamSupport {
    static void addTypes(CompilationUnit cu) {
        for (String kind : new String[]{"Int", "Long", "Double"}) {
            String primitive=kind.equals("Int") ? "int" : kind.toLowerCase();
            String box=kind.equals("Int") ? "Integer" : kind;
            String source="""
                interface $KUnaryOperator { public $P applyAs$K($P value); }
                interface $KBinaryOperator { public $P applyAs$K($P left,$P right); }
                interface $KPredicate { public boolean test($P value); }
                interface $KConsumer { public void accept($P value); }
                interface $KSupplier { public $P getAs$K(); }
                interface $KFunction<R> { public R apply($P value); }
                interface To$KFunction<T> { public $P applyAs$K(T value); }
                class Optional$K {
                    private $P value; private boolean present;
                    private Optional$K($P value,boolean present) { this.value=value; this.present=present; }
                    public static Optional$K empty() { return new Optional$K(($P)0,false); }
                    public static Optional$K of($P value) { return new Optional$K(value,true); }
                    public boolean isPresent() { return present; }
                    public boolean isEmpty() { return !present; }
                    public $P getAs$K() { if(!present) throw new NoSuchElementException(); return value; }
                    public $P orElse($P fallback) { return present ? value : fallback; }
                    public $P orElseGet($KSupplier fallback) { return present ? value : fallback.getAs$K(); }
                    public $P orElseThrow() { return getAs$K(); }
                    public void ifPresent($KConsumer consumer) { if(present) consumer.accept(value); }
                }
                class __Javaw$KArrayIterator implements Iterator<$B> {
                    private $P[] values; private int index;
                    public __Javaw$KArrayIterator($P[] values) { this.values=values; }
                    public boolean hasNext() { return index<values.length; }
                    public $B next() { if(!hasNext()) throw new NoSuchElementException(); $P value=values[index]; index++; return value; }
                }
                class __Javaw$KMap implements Function<$B,$B> {
                    private $KUnaryOperator operator;
                    public __Javaw$KMap($KUnaryOperator operator) { this.operator=operator; }
                    public $B apply($B value) { return operator.applyAs$K(value); }
                }
                class __Javaw$KFilter implements Predicate<$B> {
                    private $KPredicate predicate;
                    public __Javaw$KFilter($KPredicate predicate) { this.predicate=predicate; }
                    public boolean test($B value) { return predicate.test(value); }
                }
                class __Javaw$KConsumer implements Consumer<$B> {
                    private $KConsumer consumer;
                    public __Javaw$KConsumer($KConsumer consumer) { this.consumer=consumer; }
                    public void accept($B value) { consumer.accept(value); }
                }
                class __Javaw$KGenerate implements Supplier<$B> {
                    private $KSupplier supplier;
                    public __Javaw$KGenerate($KSupplier supplier) { this.supplier=supplier; }
                    public $B get() { return supplier.getAs$K(); }
                }
                class __Javaw$KIterate implements UnaryOperator<$B> {
                    private $KUnaryOperator operator;
                    public __Javaw$KIterate($KUnaryOperator operator) { this.operator=operator; }
                    public $B apply($B value) { return operator.applyAs$K(value); }
                }
                class __Javaw$KToObj<R> implements Function<$B,R> {
                    private $KFunction<R> mapper;
                    public __Javaw$KToObj($KFunction<R> mapper) { this.mapper=mapper; }
                    public R apply($B value) { return mapper.apply(value); }
                }
                class __JavawTo$K<T> implements Function<T,$B> {
                    private To$KFunction<T> mapper;
                    public __JavawTo$K(To$KFunction<T> mapper) { this.mapper=mapper; }
                    public $B apply(T value) { return mapper.applyAs$K(value); }
                }
                class __Javaw$KFlatMap implements Function<$B,Stream<$B>> {
                    private $KFunction<$KStream> mapper;
                    public __Javaw$KFlatMap($KFunction<$KStream> mapper) { this.mapper=mapper; }
                    public Stream<$B> apply($B value) { $KStream result=mapper.apply(value); return result==null ? null : result.data; }
                }
                class $KStream {
                    public Stream<$B> data;
                    public $KStream(Stream<$B> data) { this.data=data; }
                    public static $KStream of($P... values) {
                        if(values==null) throw new NullPointerException();
                        return new $KStream(Stream.from(new __Javaw$KArrayIterator(values)));
                    }
                    public static $KStream empty() { return $KStream.of(); }
                    public static $KStream generate($KSupplier supplier) {
                        if(supplier==null) throw new NullPointerException();
                        return new $KStream(Stream.generate(new __Javaw$KGenerate(supplier)));
                    }
                    public static $KStream iterate($P seed,$KUnaryOperator operator) {
                        if(operator==null) throw new NullPointerException();
                        $B boxedSeed=seed; return new $KStream(Stream.iterate(boxedSeed,new __Javaw$KIterate(operator)));
                    }
                    public $KStream map($KUnaryOperator mapper) {
                        if(mapper==null) throw new NullPointerException();
                        return new $KStream(data.map(new __Javaw$KMap(mapper)));
                    }
                    public $KStream filter($KPredicate predicate) {
                        if(predicate==null) throw new NullPointerException();
                        return new $KStream(data.filter(new __Javaw$KFilter(predicate)));
                    }
                    public $KStream peek($KConsumer consumer) {
                        if(consumer==null) throw new NullPointerException();
                        return new $KStream(data.peek(new __Javaw$KConsumer(consumer)));
                    }
                    public $KStream takeWhile($KPredicate predicate) {
                        if(predicate==null) throw new NullPointerException();
                        return new $KStream(data.takeWhile(new __Javaw$KFilter(predicate)));
                    }
                    public $KStream dropWhile($KPredicate predicate) {
                        if(predicate==null) throw new NullPointerException();
                        return new $KStream(data.dropWhile(new __Javaw$KFilter(predicate)));
                    }
                    public $KStream flatMap($KFunction<$KStream> mapper) {
                        if(mapper==null) throw new NullPointerException();
                        return new $KStream(data.flatMap(new __Javaw$KFlatMap(mapper)));
                    }
                    public <R> Stream<R> mapToObj($KFunction<R> mapper) {
                        if(mapper==null) throw new NullPointerException();
                        return data.map(new __Javaw$KToObj<R>(mapper));
                    }
                    public $KStream limit(long count) { return new $KStream(data.limit(count)); }
                    public $KStream skip(long count) { return new $KStream(data.skip(count)); }
                    public $KStream distinct() { return new $KStream(data.distinct()); }
                    public $KStream sorted() { return new $KStream(data.sorted()); }
                    public Stream<$B> boxed() { return data; }
                    public long count() { return data.count(); }
                    public Optional$K findFirst() { Optional<$B> value=data.findFirst(); return value.isPresent() ? Optional$K.of(value.get()) : Optional$K.empty(); }
                    public Optional$K findAny() { return findFirst(); }
                    public boolean anyMatch($KPredicate predicate) { if(predicate==null) throw new NullPointerException(); return data.anyMatch(new __Javaw$KFilter(predicate)); }
                    public boolean allMatch($KPredicate predicate) { if(predicate==null) throw new NullPointerException(); return data.allMatch(new __Javaw$KFilter(predicate)); }
                    public boolean noneMatch($KPredicate predicate) { if(predicate==null) throw new NullPointerException(); return data.noneMatch(new __Javaw$KFilter(predicate)); }
                    public void forEach($KConsumer consumer) { if(consumer==null) throw new NullPointerException(); data.forEach(new __Javaw$KConsumer(consumer)); }
                    public void forEachOrdered($KConsumer consumer) { forEach(consumer); }
                    public $P sum() { Iterator<$B> it=data.iterator(); $P sum=($P)0; while(it.hasNext()) sum+=it.next(); return sum; }
                    public OptionalDouble average() {
                        Iterator<$B> it=data.iterator(); double sum=0.0; long count=0;
                        while(it.hasNext()) { sum+=it.next(); count++; }
                        return count==0 ? OptionalDouble.empty() : OptionalDouble.of(sum/count);
                    }
                    public Optional$K min() { return extreme(false); }
                    public Optional$K max() { return extreme(true); }
                    private Optional$K extreme(boolean maximum) {
                        Iterator<$B> it=data.iterator(); if(!it.hasNext()) return Optional$K.empty();
                        $P result=it.next();
                        while(it.hasNext()) { $P value=it.next(); if(maximum ? value>result : value<result) result=value; }
                        return Optional$K.of(result);
                    }
                    public $P reduce($P identity,$KBinaryOperator accumulator) {
                        if(accumulator==null) throw new NullPointerException();
                        Iterator<$B> it=data.iterator(); $P result=identity;
                        while(it.hasNext()) result=accumulator.applyAs$K(result,it.next()); return result;
                    }
                    public Optional$K reduce($KBinaryOperator accumulator) {
                        if(accumulator==null) throw new NullPointerException();
                        Iterator<$B> it=data.iterator(); if(!it.hasNext()) return Optional$K.empty();
                        $P result=it.next(); while(it.hasNext()) result=accumulator.applyAs$K(result,it.next()); return Optional$K.of(result);
                    }
                    public $P[] toArray() {
                        List<$B> values=data.toList(); $P[] result=new $P[values.size()];
                        for(int i=0;i<values.size();i++) result[i]=values.get(i); return result;
                    }
                    public boolean isParallel() { return data.isParallel(); }
                    public $KStream sequential() { data.sequential(); return this; }
                    public $KStream parallel() { data.parallel(); return this; }
                    public void close() { data.close(); }
                }
                """.replace("$K",kind).replace("$P",primitive).replace("$B",box);
            if (kind.equals("Double")) source=source.replace("if(maximum ? value>result : value<result) result=value;",
                "if(value!=value || result==result && (maximum ? value>result : value<result) || value==0.0 && result==0.0 && (maximum ? Double.doubleToLongBits(value)>Double.doubleToLongBits(result) : Double.doubleToLongBits(value)<Double.doubleToLongBits(result))) result=value;");
            else source=source.replace("double sum=0.0; long count=0;", "long sum=0; long count=0;").replace("OptionalDouble.of(sum/count)", "OptionalDouble.of((double)sum/count)");
            LibrarySupport.add(cu,"java.util.stream",source);
            if (!kind.equals("Double")) {
                LibrarySupport.add(cu,"java.util.stream", """
                    class __Javaw$KRange implements Iterator<$B> {
                        private $P value; private $P end; private boolean inclusive; private boolean done;
                        public __Javaw$KRange($P start,$P end,boolean inclusive) { value=start; this.end=end; this.inclusive=inclusive; }
                        public boolean hasNext() { return !done && (value<end || inclusive && value==end); }
                        public $B next() {
                            if(!hasNext()) throw new NoSuchElementException();
                            $P result=value; if(value==end) done=true; else value++; return result;
                        }
                    }
                    """.replace("$K",kind).replace("$P",primitive).replace("$B",box));
                var owner=cu.getTypes().stream().filter(t->t.getNameAsString().equals(kind+"Stream")).findFirst().orElseThrow();
                owner.addMember(com.github.javaparser.StaticJavaParser.parseBodyDeclaration("public static "+kind+"Stream range("+primitive+" start,"+primitive+" end) { return new "+kind+"Stream(Stream.from(new __Javaw"+kind+"Range(start,end,false))); }"));
                owner.addMember(com.github.javaparser.StaticJavaParser.parseBodyDeclaration("public static "+kind+"Stream rangeClosed("+primitive+" start,"+primitive+" end) { return new "+kind+"Stream(Stream.from(new __Javaw"+kind+"Range(start,end,true))); }"));
            }
        }
        for (String from : new String[]{"Int","Long","Double"}) for (String to : new String[]{"Int","Long","Double"}) {
            if(from.equals(to)) continue;
            String fp=from.equals("Int") ? "int" : from.toLowerCase(), tp=to.equals("Int") ? "int" : to.toLowerCase();
            String fb=from.equals("Int") ? "Integer" : from, tb=to.equals("Int") ? "Integer" : to;
            String iface=from+"To"+to+"Function", adapter="__Javaw"+iface;
            LibrarySupport.add(cu,"java.util.function", "interface "+iface+" { public "+tp+" applyAs"+to+"("+fp+" value); } "+
                "class "+adapter+" implements Function<"+fb+","+tb+"> { private "+iface+" mapper; public "+adapter+"("+iface+" mapper) { this.mapper=mapper; } public "+tb+" apply("+fb+" value) { return mapper.applyAs"+to+"(value); } }");
            var owner=cu.getTypes().stream().filter(t->t.getNameAsString().equals(from+"Stream")).findFirst().orElseThrow();
            owner.addMember(com.github.javaparser.StaticJavaParser.parseBodyDeclaration("public "+to+"Stream mapTo"+to+"("+iface+" mapper) { if(mapper==null) throw new NullPointerException(); return new "+to+"Stream(data.map(new "+adapter+"(mapper))); }"));
            if ((from.equals("Int") && !to.equals("Int")) || (from.equals("Long") && to.equals("Double"))) {
                String widening="__Javaw"+from+"Widen"+to;
                LibrarySupport.add(cu,"java.util.function","class "+widening+" implements "+iface+" { public "+widening+"() {} public "+tp+" applyAs"+to+"("+fp+" value) { return value; } }");
                owner.addMember(com.github.javaparser.StaticJavaParser.parseBodyDeclaration("public "+to+"Stream as"+to+"Stream() { return mapTo"+to+"(new "+widening+"()); }"));
            }
        }
    }
}
