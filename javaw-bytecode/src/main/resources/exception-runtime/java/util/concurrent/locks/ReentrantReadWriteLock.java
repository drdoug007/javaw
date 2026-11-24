package java.util.concurrent.locks;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Read-write lock for the single-threaded bytecode contract: two plain hold counters, never blocks. */
public class ReentrantReadWriteLock implements ReadWriteLock,Serializable {
    private final boolean fair;
    private int readHolds;
    private int writeHolds;
    private final ReentrantReadWriteLock.ReadLock readerLock;
    private final ReentrantReadWriteLock.WriteLock writerLock;
    public ReentrantReadWriteLock(){this(false);}
    public ReentrantReadWriteLock(boolean fair){this.fair=fair;readerLock=new ReadLock(this);writerLock=new WriteLock(this);}
    public ReentrantReadWriteLock.ReadLock readLock(){return readerLock;}
    public ReentrantReadWriteLock.WriteLock writeLock(){return writerLock;}
    public final boolean isFair(){return fair;}
    public int getReadLockCount(){return readHolds;}
    public int getReadHoldCount(){return readHolds;}
    public boolean isWriteLocked(){return writeHolds!=0;}
    public boolean isWriteLockedByCurrentThread(){return writeHolds!=0;}
    public int getWriteHoldCount(){return writeHolds;}
    public String toString(){return super.toString()+"[Write locks = "+writeHolds+", Read locks = "+readHolds+"]";}

    public static class ReadLock implements Lock,Serializable {
        private final ReentrantReadWriteLock lock;
        protected ReadLock(ReentrantReadWriteLock lock){this.lock=Objects.requireNonNull(lock);}
        public void lock(){if(lock.readHolds==65535)throw new Error("Maximum lock count exceeded");lock.readHolds++;}
        public void lockInterruptibly() throws InterruptedException{if(lock.readHolds==65535)throw new Error("Maximum lock count exceeded");lock.readHolds++;}
        public boolean tryLock(){if(lock.readHolds==65535)throw new Error("Maximum lock count exceeded");lock.readHolds++;return true;}
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException{unit.toNanos(timeout);if(lock.readHolds==65535)throw new Error("Maximum lock count exceeded");lock.readHolds++;return true;}
        public void unlock(){if(lock.readHolds==0)throw new IllegalMonitorStateException("attempt to unlock read lock, not locked by current thread");lock.readHolds--;}
        public String toString(){return super.toString()+"[Read locks = "+lock.readHolds+"]";}
    }

    public static class WriteLock implements Lock,Serializable {
        private final ReentrantReadWriteLock lock;
        protected WriteLock(ReentrantReadWriteLock lock){this.lock=Objects.requireNonNull(lock);}
        public void lock(){if(lock.readHolds!=0&&lock.writeHolds==0)throw new IllegalMonitorStateException("write lock would block forever: read lock held");if(lock.writeHolds==65535)throw new Error("Maximum lock count exceeded");lock.writeHolds++;}
        public void lockInterruptibly() throws InterruptedException{if(lock.readHolds!=0&&lock.writeHolds==0)throw new IllegalMonitorStateException("write lock would block forever: read lock held");if(lock.writeHolds==65535)throw new Error("Maximum lock count exceeded");lock.writeHolds++;}
        public boolean tryLock(){if(lock.readHolds!=0&&lock.writeHolds==0)return false;if(lock.writeHolds==65535)throw new Error("Maximum lock count exceeded");lock.writeHolds++;return true;}
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException{unit.toNanos(timeout);if(lock.readHolds!=0&&lock.writeHolds==0)return false;if(lock.writeHolds==65535)throw new Error("Maximum lock count exceeded");lock.writeHolds++;return true;}
        public void unlock(){if(lock.writeHolds==0)throw new IllegalMonitorStateException();lock.writeHolds--;}
        public boolean isHeldByCurrentThread(){return lock.writeHolds!=0;}
        public int getHoldCount(){return lock.writeHolds;}
        public String toString(){return super.toString()+(lock.writeHolds==0?"[Unlocked]":"[Locked by thread main]");}
    }
}
