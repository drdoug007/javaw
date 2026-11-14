package java.util.concurrent.atomic;

/** Atomic flag for the single-threaded bytecode contract; every access mode is a plain field access. */
public class AtomicBoolean implements java.io.Serializable {
    private boolean value;
    public AtomicBoolean(boolean initialValue){value=initialValue;}
    public AtomicBoolean(){value=false;}
    public final boolean get(){return value;}
    public final void set(boolean newValue){value=newValue;}
    public final void lazySet(boolean newValue){value=newValue;}
    public final boolean getAndSet(boolean newValue){boolean old=value;value=newValue;return old;}
    public final boolean compareAndSet(boolean expectedValue,boolean newValue){if(value!=expectedValue)return false;value=newValue;return true;}
    @Deprecated(since="9") public boolean weakCompareAndSet(boolean expectedValue,boolean newValue){return compareAndSet(expectedValue,newValue);}
    public boolean weakCompareAndSetPlain(boolean expectedValue,boolean newValue){return compareAndSet(expectedValue,newValue);}
    public final boolean weakCompareAndSetVolatile(boolean expectedValue,boolean newValue){return compareAndSet(expectedValue,newValue);}
    public final boolean weakCompareAndSetAcquire(boolean expectedValue,boolean newValue){return compareAndSet(expectedValue,newValue);}
    public final boolean weakCompareAndSetRelease(boolean expectedValue,boolean newValue){return compareAndSet(expectedValue,newValue);}
    public final boolean compareAndExchange(boolean expectedValue,boolean newValue){boolean old=value;if(old==expectedValue)value=newValue;return old;}
    public final boolean compareAndExchangeAcquire(boolean expectedValue,boolean newValue){return compareAndExchange(expectedValue,newValue);}
    public final boolean compareAndExchangeRelease(boolean expectedValue,boolean newValue){return compareAndExchange(expectedValue,newValue);}
    public final boolean getPlain(){return value;}
    public final void setPlain(boolean newValue){value=newValue;}
    public final boolean getOpaque(){return value;}
    public final void setOpaque(boolean newValue){value=newValue;}
    public final boolean getAcquire(){return value;}
    public final void setRelease(boolean newValue){value=newValue;}
    public String toString(){return Boolean.toString(get());}
}
