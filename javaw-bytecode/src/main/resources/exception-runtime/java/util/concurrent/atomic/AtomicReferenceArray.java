package java.util.concurrent.atomic;

/** Reference array with atomic-style accessors for the single-threaded bytecode contract; every access mode is a plain array access. */
public class AtomicReferenceArray<E> implements java.io.Serializable {
    private final Object[] array;
    public AtomicReferenceArray(int length){array=new Object[length];}
    public AtomicReferenceArray(E[] values){array=java.util.Arrays.copyOf(values,values.length,Object[].class);}
    public final int length(){return array.length;}
    public final E get(int index){return (E)array[index];}
    public final void set(int index,E value){array[index]=value;}
    public final void lazySet(int index,E value){array[index]=value;}
    public final E getAndSet(int index,E value){E old=(E)array[index];array[index]=value;return old;}
    public final boolean compareAndSet(int index,E expected,E value){if(array[index]!=expected)return false;array[index]=value;return true;}
    @Deprecated(since="9") public final boolean weakCompareAndSet(int index,E expected,E value){return compareAndSet(index,expected,value);}
    public final boolean weakCompareAndSetPlain(int index,E expected,E value){return compareAndSet(index,expected,value);}
    public final boolean weakCompareAndSetVolatile(int index,E expected,E value){return compareAndSet(index,expected,value);}
    public final boolean weakCompareAndSetAcquire(int index,E expected,E value){return compareAndSet(index,expected,value);}
    public final boolean weakCompareAndSetRelease(int index,E expected,E value){return compareAndSet(index,expected,value);}
    public final E compareAndExchange(int index,E expected,E value){E old=(E)array[index];if(old==expected)array[index]=value;return old;}
    public final E compareAndExchangeAcquire(int index,E expected,E value){return compareAndExchange(index,expected,value);}
    public final E compareAndExchangeRelease(int index,E expected,E value){return compareAndExchange(index,expected,value);}
    public final E getPlain(int index){return (E)array[index];}
    public final void setPlain(int index,E value){array[index]=value;}
    public final E getOpaque(int index){return (E)array[index];}
    public final void setOpaque(int index,E value){array[index]=value;}
    public final E getAcquire(int index){return (E)array[index];}
    public final void setRelease(int index,E value){array[index]=value;}
    public final E getAndUpdate(int index,java.util.function.UnaryOperator<E> update){
        E prev=get(index);
        for(;;){E next=update.apply(prev);if(compareAndSet(index,prev,next))return prev;prev=get(index);}
    }
    public final E updateAndGet(int index,java.util.function.UnaryOperator<E> update){
        E prev=get(index);
        for(;;){E next=update.apply(prev);if(compareAndSet(index,prev,next))return next;prev=get(index);}
    }
    public final E getAndAccumulate(int index,E value,java.util.function.BinaryOperator<E> accumulator){
        E prev=get(index);
        for(;;){E next=accumulator.apply(prev,value);if(compareAndSet(index,prev,next))return prev;prev=get(index);}
    }
    public final E accumulateAndGet(int index,E value,java.util.function.BinaryOperator<E> accumulator){
        E prev=get(index);
        for(;;){E next=accumulator.apply(prev,value);if(compareAndSet(index,prev,next))return next;prev=get(index);}
    }
    public String toString(){
        int last=array.length-1;if(last==-1)return "[]";
        StringBuilder result=new StringBuilder("[");
        for(int i=0;;i++){result.append(array[i]);if(i==last)return result.append(']').toString();result.append(',').append(' ');}
    }
}
