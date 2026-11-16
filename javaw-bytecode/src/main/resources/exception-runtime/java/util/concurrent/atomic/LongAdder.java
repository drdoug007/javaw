package java.util.concurrent.atomic;

import java.io.Serializable;

/** LongAdder for the single-threaded bytecode contract: one plain long field, no striped cells. */
public class LongAdder extends Number implements Serializable {
    private long base;
    public LongAdder(){base=0L;}
    public void add(long x){base+=x;}
    public void increment(){add(1L);}
    public void decrement(){add(-1L);}
    public long sum(){return base;}
    public void reset(){base=0L;}
    public long sumThenReset(){long old=base;base=0L;return old;}
    public String toString(){return Long.toString(sum());}
    public long longValue(){return sum();}
    public int intValue(){return (int)sum();}
    public float floatValue(){return (float)sum();}
    public double doubleValue(){return (double)sum();}
}
