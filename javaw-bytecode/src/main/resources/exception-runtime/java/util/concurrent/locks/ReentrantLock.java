package java.util.concurrent.locks;

import java.io.Serializable;

/** Reentrant lock for the single-threaded bytecode contract: one plain hold count, never blocks. */
public class ReentrantLock implements Lock,Serializable {
    private final boolean fair;
    private int holds;
    public ReentrantLock(){fair=false;}
    public ReentrantLock(boolean fair){this.fair=fair;}
    public void lock(){if(holds==Integer.MAX_VALUE)throw new Error("Maximum lock count exceeded");holds++;}
    public void lockInterruptibly(){if(holds==Integer.MAX_VALUE)throw new Error("Maximum lock count exceeded");holds++;}
    public boolean tryLock(){if(holds==Integer.MAX_VALUE)throw new Error("Maximum lock count exceeded");holds++;return true;}
    public void unlock(){if(holds==0)throw new IllegalMonitorStateException();holds--;}
    public int getHoldCount(){return holds;}
    public boolean isHeldByCurrentThread(){return holds!=0;}
    public boolean isLocked(){return holds!=0;}
    public final boolean isFair(){return fair;}
    public String toString(){return super.toString()+(holds==0?"[Unlocked]":"[Locked by thread main]");}
}
