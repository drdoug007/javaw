package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/** Atomic long for the single-threaded bytecode contract; every access mode is a plain field access. */
public class AtomicLong extends Number implements Serializable {
    private long value;
    public AtomicLong(long initialValue){value=initialValue;}
    public AtomicLong(){value=0L;}
    public final long get(){return value;}
    public final void set(long newValue){value=newValue;}
    public final void lazySet(long newValue){value=newValue;}
    public final long getAndSet(long newValue){long old=value;value=newValue;return old;}
    public final boolean compareAndSet(long expectedValue,long newValue){if(value!=expectedValue)return false;value=newValue;return true;}
    @Deprecated(since="9") public final boolean weakCompareAndSet(long expectedValue,long newValue){return compareAndSet(expectedValue,newValue);}
    public final boolean weakCompareAndSetPlain(long expectedValue,long newValue){return compareAndSet(expectedValue,newValue);}
    public final boolean weakCompareAndSetVolatile(long expectedValue,long newValue){return compareAndSet(expectedValue,newValue);}
    public final boolean weakCompareAndSetAcquire(long expectedValue,long newValue){return compareAndSet(expectedValue,newValue);}
    public final boolean weakCompareAndSetRelease(long expectedValue,long newValue){return compareAndSet(expectedValue,newValue);}
    public final long compareAndExchange(long expectedValue,long newValue){long old=value;if(old==expectedValue)value=newValue;return old;}
    public final long compareAndExchangeAcquire(long expectedValue,long newValue){return compareAndExchange(expectedValue,newValue);}
    public final long compareAndExchangeRelease(long expectedValue,long newValue){return compareAndExchange(expectedValue,newValue);}
    public final long getAndIncrement(){return getAndAdd(1L);}
    public final long getAndDecrement(){return getAndAdd(-1L);}
    public final long getAndAdd(long delta){long old=value;value=old+delta;return old;}
    public final long incrementAndGet(){return addAndGet(1L);}
    public final long decrementAndGet(){return addAndGet(-1L);}
    public final long addAndGet(long delta){value+=delta;return value;}
    public final long getAndUpdate(LongUnaryOperator updateFunction){
        long prev=value;
        for(;;){
            long next=updateFunction.applyAsLong(prev);
            if(value==prev){value=next;return prev;}
            prev=value;
        }
    }
    public final long updateAndGet(LongUnaryOperator updateFunction){
        long prev=value;
        for(;;){
            long next=updateFunction.applyAsLong(prev);
            if(value==prev){value=next;return next;}
            prev=value;
        }
    }
    public final long getAndAccumulate(long x,LongBinaryOperator accumulatorFunction){
        long prev=value;
        for(;;){
            long next=accumulatorFunction.applyAsLong(prev,x);
            if(value==prev){value=next;return prev;}
            prev=value;
        }
    }
    public final long accumulateAndGet(long x,LongBinaryOperator accumulatorFunction){
        long prev=value;
        for(;;){
            long next=accumulatorFunction.applyAsLong(prev,x);
            if(value==prev){value=next;return next;}
            prev=value;
        }
    }
    public final long getPlain(){return value;}
    public final void setPlain(long newValue){value=newValue;}
    public final long getOpaque(){return value;}
    public final void setOpaque(long newValue){value=newValue;}
    public final long getAcquire(){return value;}
    public final void setRelease(long newValue){value=newValue;}
    public String toString(){return Long.toString(value);}
    public int intValue(){return (int)value;}
    public long longValue(){return value;}
    public float floatValue(){return (float)value;}
    public double doubleValue(){return (double)value;}
}
