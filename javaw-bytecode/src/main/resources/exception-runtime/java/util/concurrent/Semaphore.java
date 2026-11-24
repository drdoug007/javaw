package java.util.concurrent;

import java.io.Serializable;

/** Counting semaphore for the single-threaded bytecode contract: one plain signed permit count, nothing blocks. */
public class Semaphore implements Serializable {
    private int permits;
    private final boolean fair;
    public Semaphore(int permits){this.permits=permits;fair=false;}
    public Semaphore(int permits,boolean fair){this.permits=permits;this.fair=fair;}
    public void acquire() throws InterruptedException{take(1,"acquire");}
    public void acquire(int n) throws InterruptedException{checkNonNegative(n);take(n,"acquire");}
    public void acquireUninterruptibly(){take(1,"acquireUninterruptibly");}
    public void acquireUninterruptibly(int n){checkNonNegative(n);take(n,"acquireUninterruptibly");}
    public boolean tryAcquire(){return tryTake(1);}
    public boolean tryAcquire(int n){checkNonNegative(n);return tryTake(n);}
    public void release(){give(1);}
    public void release(int n){checkNonNegative(n);give(n);}
    public int availablePermits(){return permits;}
    public int drainPermits(){int drained=permits;permits=0;return drained;}
    protected void reducePermits(int reduction){checkNonNegative(reduction);int next=permits-reduction;if(next>permits)throw new Error("Permit count underflow");permits=next;}
    public boolean isFair(){return fair;}
    public String toString(){return super.toString()+"[Permits = "+permits+"]";}
    private void checkNonNegative(int n){if(n<0)throw new IllegalArgumentException();}
    private boolean tryTake(int n){int next=permits-n;if(next<0)return false;permits=next;return true;}
    private void take(int n,String method){if(!tryTake(n))throw new IllegalStateException("Semaphore."+method+" would block forever: not enough permits and no other thread");}
    private void give(int n){int next=permits+n;if(next<permits)throw new Error("Maximum permit count exceeded");permits=next;}
}
