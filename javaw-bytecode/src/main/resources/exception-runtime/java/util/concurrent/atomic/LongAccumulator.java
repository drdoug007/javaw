package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.LongBinaryOperator;

/** LongAccumulator for the single-threaded bytecode contract: one plain long field plus function and identity. */
public class LongAccumulator extends Number implements Serializable {
    private final LongBinaryOperator function;
    private final long identity;
    private long base;
    public LongAccumulator(LongBinaryOperator accumulatorFunction,long identity){
        this.function=accumulatorFunction;this.identity=identity;base=identity;
    }
    public void accumulate(long x){long r=function.applyAsLong(base,x);if(r!=base)base=r;}
    public long get(){return base;}
    public void reset(){base=identity;}
    public long getThenReset(){long old=base;base=identity;return old;}
    public String toString(){return Long.toString(get());}
    public long longValue(){return get();}
    public int intValue(){return (int)get();}
    public float floatValue(){return (float)get();}
    public double doubleValue(){return (double)get();}
}
