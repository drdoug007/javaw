package java.util.concurrent;

/** Count down latch for the single-threaded bytecode contract: a plain count, never blocks. */
public class CountDownLatch {
    private int count;
    public CountDownLatch(int count){if(count<0)throw new IllegalArgumentException("count < 0");this.count=count;}
    public void await() throws InterruptedException{if(count>0)throw new IllegalStateException("CountDownLatch.await would block forever: count is "+count+" and no other thread");}
    public boolean await(long timeout,TimeUnit unit) throws InterruptedException{unit.toNanos(timeout);return count==0;}
    public void countDown(){if(count>0)count--;}
    public long getCount(){return count;}
    public String toString(){return super.toString()+"[Count = "+count+"]";}
}
