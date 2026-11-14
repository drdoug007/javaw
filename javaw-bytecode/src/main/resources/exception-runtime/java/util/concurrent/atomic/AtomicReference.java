package java.util.concurrent.atomic;

/** Atomic holder for the single-threaded bytecode contract; plain field, identity compare, no spurious failures. */
public class AtomicReference<V> implements java.io.Serializable {
    private V value;
    public AtomicReference(V initialValue){value=initialValue;}
    public AtomicReference(){value=null;}
    public final V get(){return value;}
    public final void set(V newValue){value=newValue;}
    public final void lazySet(V newValue){value=newValue;}
    public final V getAndSet(V newValue){V old=value;value=newValue;return old;}
    public final boolean compareAndSet(V expectedValue,V newValue){if(value!=expectedValue)return false;value=newValue;return true;}
    @Deprecated(since="9") public final boolean weakCompareAndSet(V expectedValue,V newValue){return compareAndSet(expectedValue,newValue);}
    public final boolean weakCompareAndSetPlain(V expectedValue,V newValue){return compareAndSet(expectedValue,newValue);}
    public final boolean weakCompareAndSetVolatile(V expectedValue,V newValue){return compareAndSet(expectedValue,newValue);}
    public final boolean weakCompareAndSetAcquire(V expectedValue,V newValue){return compareAndSet(expectedValue,newValue);}
    public final boolean weakCompareAndSetRelease(V expectedValue,V newValue){return compareAndSet(expectedValue,newValue);}
    public final V compareAndExchange(V expectedValue,V newValue){V old=value;if(old==expectedValue)value=newValue;return old;}
    public final V compareAndExchangeAcquire(V expectedValue,V newValue){return compareAndExchange(expectedValue,newValue);}
    public final V compareAndExchangeRelease(V expectedValue,V newValue){return compareAndExchange(expectedValue,newValue);}
    public final V getAndUpdate(java.util.function.UnaryOperator<V> updateFunction){
        V prev=get();
        for(;;){V next=updateFunction.apply(prev);if(compareAndSet(prev,next))return prev;prev=get();}
    }
    public final V updateAndGet(java.util.function.UnaryOperator<V> updateFunction){
        V prev=get();
        for(;;){V next=updateFunction.apply(prev);if(compareAndSet(prev,next))return next;prev=get();}
    }
    public final V getAndAccumulate(V x,java.util.function.BinaryOperator<V> accumulatorFunction){
        V prev=get();
        for(;;){V next=accumulatorFunction.apply(prev,x);if(compareAndSet(prev,next))return prev;prev=get();}
    }
    public final V accumulateAndGet(V x,java.util.function.BinaryOperator<V> accumulatorFunction){
        V prev=get();
        for(;;){V next=accumulatorFunction.apply(prev,x);if(compareAndSet(prev,next))return next;prev=get();}
    }
    public final V getPlain(){return value;}
    public final void setPlain(V newValue){value=newValue;}
    public final V getOpaque(){return value;}
    public final void setOpaque(V newValue){value=newValue;}
    public final V getAcquire(){return value;}
    public final void setRelease(V newValue){value=newValue;}
    public String toString(){return String.valueOf(get());}
}
