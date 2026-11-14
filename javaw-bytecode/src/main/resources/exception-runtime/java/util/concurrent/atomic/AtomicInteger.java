package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/** Atomic integer for the single-threaded bytecode contract; every access mode is a plain field access. */
public class AtomicInteger extends Number implements Serializable {
    private int value;
    public AtomicInteger(int initialValue){value=initialValue;}
    public AtomicInteger(){value=0;}
    public final int get(){return value;}
    public final void set(int newValue){value=newValue;}
    public final void lazySet(int newValue){value=newValue;}
    public final int getAndSet(int newValue){int old=value;value=newValue;return old;}
    public final boolean compareAndSet(int expectedValue,int newValue){if(value!=expectedValue)return false;value=newValue;return true;}
    @Deprecated(since="9") public final boolean weakCompareAndSet(int expectedValue,int newValue){return compareAndSet(expectedValue,newValue);}
    public final boolean weakCompareAndSetPlain(int expectedValue,int newValue){return compareAndSet(expectedValue,newValue);}
    public final boolean weakCompareAndSetVolatile(int expectedValue,int newValue){return compareAndSet(expectedValue,newValue);}
    public final boolean weakCompareAndSetAcquire(int expectedValue,int newValue){return compareAndSet(expectedValue,newValue);}
    public final boolean weakCompareAndSetRelease(int expectedValue,int newValue){return compareAndSet(expectedValue,newValue);}
    public final int compareAndExchange(int expectedValue,int newValue){int old=value;if(old==expectedValue)value=newValue;return old;}
    public final int compareAndExchangeAcquire(int expectedValue,int newValue){return compareAndExchange(expectedValue,newValue);}
    public final int compareAndExchangeRelease(int expectedValue,int newValue){return compareAndExchange(expectedValue,newValue);}
    public final int getAndIncrement(){return getAndAdd(1);}
    public final int getAndDecrement(){return getAndAdd(-1);}
    public final int getAndAdd(int delta){int old=value;value=old+delta;return old;}
    public final int incrementAndGet(){return addAndGet(1);}
    public final int decrementAndGet(){return addAndGet(-1);}
    public final int addAndGet(int delta){value+=delta;return value;}
    public final int getAndUpdate(IntUnaryOperator updateFunction){
        int prev=value;
        for(;;){
            int next=updateFunction.applyAsInt(prev);
            if(value==prev){value=next;return prev;}
            prev=value;
        }
    }
    public final int updateAndGet(IntUnaryOperator updateFunction){
        int prev=value;
        for(;;){
            int next=updateFunction.applyAsInt(prev);
            if(value==prev){value=next;return next;}
            prev=value;
        }
    }
    public final int getAndAccumulate(int x,IntBinaryOperator accumulatorFunction){
        int prev=value;
        for(;;){
            int next=accumulatorFunction.applyAsInt(prev,x);
            if(value==prev){value=next;return prev;}
            prev=value;
        }
    }
    public final int accumulateAndGet(int x,IntBinaryOperator accumulatorFunction){
        int prev=value;
        for(;;){
            int next=accumulatorFunction.applyAsInt(prev,x);
            if(value==prev){value=next;return next;}
            prev=value;
        }
    }
    public final int getPlain(){return value;}
    public final void setPlain(int newValue){value=newValue;}
    public final int getOpaque(){return value;}
    public final void setOpaque(int newValue){value=newValue;}
    public final int getAcquire(){return value;}
    public final void setRelease(int newValue){value=newValue;}
    public String toString(){return Integer.toString(value);}
    public int intValue(){return value;}
    public long longValue(){return (long)value;}
    public float floatValue(){return (float)value;}
    public double doubleValue(){return (double)value;}
}
