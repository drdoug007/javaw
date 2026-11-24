package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T50: java.util.concurrent.locks.ReadWriteLock and ReentrantReadWriteLock (single-threaded). See tasks/T50-reentrant-read-write-lock.md. */
class BytecodeReentrantReadWriteLockTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.concurrent.locks.Lock;
        import java.util.concurrent.locks.ReadWriteLock;
        import java.util.concurrent.locks.ReentrantReadWriteLock;
        class LoggingRwl extends ReentrantReadWriteLock {
            final StringBuilder log=new StringBuilder();
            LoggingRwl(boolean fair){super(fair);}
            public ReentrantReadWriteLock.ReadLock readLock(){log.append("readLock;");return super.readLock();}
            public ReentrantReadWriteLock.WriteLock writeLock(){log.append("writeLock;");return super.writeLock();}
            public int getReadLockCount(){log.append("getReadLockCount;");return super.getReadLockCount();}
            public int getReadHoldCount(){log.append("getReadHoldCount;");return super.getReadHoldCount();}
            public int getWriteHoldCount(){log.append("getWriteHoldCount;");return super.getWriteHoldCount();}
            public boolean isWriteLocked(){log.append("isWriteLocked;");return super.isWriteLocked();}
            public boolean isWriteLockedByCurrentThread(){log.append("isWriteLockedByCurrentThread;");return super.isWriteLockedByCurrentThread();}
        }
        class LoggingReadLock extends ReentrantReadWriteLock.ReadLock {
            final StringBuilder log;
            LoggingReadLock(ReentrantReadWriteLock rw,StringBuilder log){super(rw);this.log=log;}
            public void lock(){log.append("r.lock;");super.lock();}
            public boolean tryLock(){log.append("r.tryLock;");return super.tryLock();}
            public void unlock(){log.append("r.unlock;");super.unlock();}
        }
        class LoggingWriteLock extends ReentrantReadWriteLock.WriteLock {
            final StringBuilder log;
            LoggingWriteLock(ReentrantReadWriteLock rw,StringBuilder log){super(rw);this.log=log;}
            public void lock(){log.append("w.lock;");super.lock();}
            public boolean tryLock(){log.append("w.tryLock;");return super.tryLock();}
            public void unlock(){log.append("w.unlock;");super.unlock();}
            public int getHoldCount(){log.append("w.getHoldCount;");return super.getHoldCount();}
            public boolean isHeldByCurrentThread(){log.append("w.isHeld;");return super.isHeldByCurrentThread();}
        }
        public class Fixture {
            static String name(Throwable t){return t.getClass().getName();}
            static String fail(Throwable t){return name(t)+":"+t.getMessage();}
            static String state(ReentrantReadWriteLock rw){
                return "r"+rw.getReadLockCount()+"/"+rw.getReadHoldCount()+"w"+rw.getWriteHoldCount()
                    +(rw.isWriteLocked()?"L":"-")+(rw.isWriteLockedByCurrentThread()?"M":"-");
            }
            static String tail(Object o){String s=o.toString();return s.substring(s.indexOf('['));}
            static String head(Object o){String s=o.toString();return s.substring(0,s.indexOf('@'));}

            public static String initialState(){
                StringBuilder out=new StringBuilder();
                ReentrantReadWriteLock rw=new ReentrantReadWriteLock();
                out.append(state(rw)).append(',').append(rw.isFair()).append(',');
                out.append(rw.readLock()==rw.readLock()).append(',').append(rw.writeLock()==rw.writeLock()).append(',');
                out.append((Object)rw.readLock()==(Object)rw.writeLock()).append(',');
                out.append(rw.writeLock().getHoldCount()).append(',').append(rw.writeLock().isHeldByCurrentThread()).append(',');
                ReentrantReadWriteLock fair=new ReentrantReadWriteLock(true);
                out.append(fair.isFair()).append(',').append(new ReentrantReadWriteLock(false).isFair()).append(',').append(state(fair));
                return out.toString();
            }
            public static String typeChecks(){
                StringBuilder out=new StringBuilder();
                ReentrantReadWriteLock rw=new ReentrantReadWriteLock();
                Object o=rw,r=rw.readLock(),w=rw.writeLock();
                out.append(o instanceof ReadWriteLock).append(',').append(o instanceof java.io.Serializable).append(',');
                out.append(r instanceof Lock).append(',').append(r instanceof java.io.Serializable).append(',');
                out.append(w instanceof Lock).append(',').append(w instanceof java.io.Serializable).append(',');
                out.append(r instanceof ReadWriteLock).append(',').append(o instanceof Lock).append(',');
                out.append(rw.equals(rw)).append(',').append(rw.equals(new ReentrantReadWriteLock())).append(',').append(r.equals(w));
                return out.toString();
            }
            public static String readReentrancy(){
                StringBuilder out=new StringBuilder();
                ReentrantReadWriteLock rw=new ReentrantReadWriteLock();
                ReentrantReadWriteLock.ReadLock r=rw.readLock();
                r.lock();out.append(state(rw)).append(',');
                r.lock();out.append(r.tryLock()).append(',').append(state(rw)).append(',');
                out.append(rw.writeLock().tryLock()).append(',').append(state(rw)).append(',');
                r.unlock();out.append(state(rw)).append(',');
                r.unlock();r.unlock();out.append(state(rw)).append(',');
                out.append(rw.writeLock().tryLock()).append(',').append(state(rw));
                return out.toString();
            }
            public static String writeReentrancy(){
                StringBuilder out=new StringBuilder();
                ReentrantReadWriteLock rw=new ReentrantReadWriteLock();
                ReentrantReadWriteLock.WriteLock w=rw.writeLock();
                w.lock();out.append(state(rw)).append(',').append(w.getHoldCount()).append(w.isHeldByCurrentThread()).append(',');
                w.lock();out.append(w.tryLock()).append(',').append(state(rw)).append(',').append(w.getHoldCount()).append(',');
                w.unlock();out.append(state(rw)).append(',');
                w.unlock();out.append(state(rw)).append(',').append(w.isHeldByCurrentThread()).append(',');
                w.unlock();out.append(state(rw)).append(',').append(w.getHoldCount()).append(w.isHeldByCurrentThread());
                return out.toString();
            }
            public static String downgrade(){
                StringBuilder out=new StringBuilder();
                for(boolean fair:new boolean[]{false,true}){
                    ReentrantReadWriteLock rw=new ReentrantReadWriteLock(fair);
                    ReentrantReadWriteLock.ReadLock r=rw.readLock();ReentrantReadWriteLock.WriteLock w=rw.writeLock();
                    w.lock();r.lock();out.append(state(rw)).append(',');
                    out.append(r.tryLock()).append(',').append(state(rw)).append(',');
                    out.append(w.tryLock()).append(',').append(state(rw)).append(',');
                    w.unlock();w.unlock();out.append(state(rw)).append(',');
                    out.append(w.tryLock()).append(',').append(w.getHoldCount()).append(',');
                    r.unlock();out.append(state(rw)).append(',');
                    r.unlock();out.append(state(rw)).append(',').append(w.tryLock()).append(',').append(state(rw)).append(';');
                }
                return out.toString();
            }
            public static String upgradeFails(){
                StringBuilder out=new StringBuilder();
                ReentrantReadWriteLock rw=new ReentrantReadWriteLock();
                ReentrantReadWriteLock.ReadLock r=rw.readLock();ReentrantReadWriteLock.WriteLock w=rw.writeLock();
                r.lock();
                out.append(w.tryLock()).append(',');
                out.append(w.tryLock()).append(',');
                out.append(state(rw)).append(',').append(w.getHoldCount()).append(w.isHeldByCurrentThread()).append(',');
                r.unlock();
                out.append(w.tryLock()).append(',');
                out.append(state(rw)).append(',').append(r.tryLock()).append(',').append(state(rw));
                return out.toString();
            }
            public static String unlockFailures(){
                StringBuilder out=new StringBuilder();
                ReentrantReadWriteLock rw=new ReentrantReadWriteLock();
                ReentrantReadWriteLock.ReadLock r=rw.readLock();ReentrantReadWriteLock.WriteLock w=rw.writeLock();
                try{r.unlock();out.append("no;");}catch(IllegalMonitorStateException e){out.append(fail(e)).append(';');}
                try{w.unlock();out.append("no;");}catch(IllegalMonitorStateException e){out.append(fail(e)).append(';');}
                out.append(state(rw)).append(';');
                w.lock();
                try{r.unlock();out.append("no;");}catch(IllegalMonitorStateException e){out.append(fail(e)).append(';');}
                out.append(state(rw)).append(';');
                w.unlock();r.lock();
                try{w.unlock();out.append("no;");}catch(IllegalMonitorStateException e){out.append(fail(e)).append(';');}
                out.append(state(rw)).append(';');
                r.unlock();
                try{r.unlock();out.append("no;");}catch(RuntimeException e){out.append(fail(e)).append(';');}
                out.append(state(rw)).append(';').append(r.tryLock()).append(w.tryLock()).append(';');
                IllegalMonitorStateException plain=new IllegalMonitorStateException();
                IllegalMonitorStateException msg=new IllegalMonitorStateException("m");
                out.append(plain.getMessage()).append(',').append(msg.getMessage()).append(',').append((Object)plain instanceof RuntimeException);
                return out.toString();
            }
            public static String readLimit(){
                StringBuilder out=new StringBuilder();
                ReentrantReadWriteLock rw=new ReentrantReadWriteLock();
                ReentrantReadWriteLock.ReadLock r=rw.readLock();
                for(int i=0;i<65534;i++)r.lock();
                out.append(r.tryLock()).append(',').append(state(rw)).append(',');
                try{r.lock();out.append("no;");}catch(Error e){out.append(fail(e)).append(';');}
                try{out.append(r.tryLock()).append(';');}catch(Error e){out.append(fail(e)).append(';');}
                out.append(state(rw)).append(',').append(tail(rw)).append(',').append(tail(r)).append(',');
                r.unlock();out.append(r.tryLock()).append(',').append(rw.writeLock().tryLock()).append(',');
                for(int i=0;i<65535;i++)r.unlock();
                out.append(state(rw));
                return out.toString();
            }
            public static String writeLimit(){
                StringBuilder out=new StringBuilder();
                ReentrantReadWriteLock rw=new ReentrantReadWriteLock();
                ReentrantReadWriteLock.WriteLock w=rw.writeLock();
                for(int i=0;i<65534;i++)w.lock();
                out.append(w.tryLock()).append(',').append(state(rw)).append(',');
                try{w.lock();out.append("no;");}catch(Error e){out.append(fail(e)).append(';');}
                try{out.append(w.tryLock()).append(';');}catch(Error e){out.append(fail(e)).append(';');}
                out.append(state(rw)).append(',').append(w.getHoldCount()).append(',').append(tail(rw)).append(',');
                out.append(rw.readLock().tryLock()).append(',').append(state(rw)).append(',');
                rw.readLock().unlock();
                for(int i=0;i<65535;i++)w.unlock();
                out.append(state(rw)).append(',').append(tail(w));
                return out.toString();
            }
            public static String interruptible(){
                StringBuilder out=new StringBuilder();
                ReentrantReadWriteLock rw=new ReentrantReadWriteLock();
                ReentrantReadWriteLock.ReadLock r=rw.readLock();ReentrantReadWriteLock.WriteLock w=rw.writeLock();
                try{
                    r.lockInterruptibly();out.append(state(rw)).append(',');
                    r.lockInterruptibly();out.append(r.tryLock()).append(',').append(state(rw)).append(',');
                    r.unlock();r.unlock();r.unlock();
                    w.lockInterruptibly();out.append(state(rw)).append(',');
                    w.lockInterruptibly();out.append(r.tryLock()).append(',').append(state(rw)).append(',');
                    r.lockInterruptibly();out.append(state(rw)).append(';');
                }catch(InterruptedException e){out.append(name(e)).append(';');}
                out.append(state(rw));
                return out.toString();
            }
            public static String toStrings(){
                StringBuilder out=new StringBuilder();
                ReentrantReadWriteLock rw=new ReentrantReadWriteLock();
                ReentrantReadWriteLock.ReadLock r=rw.readLock();ReentrantReadWriteLock.WriteLock w=rw.writeLock();
                out.append(head(rw)).append('|').append(head(r)).append('|').append(head(w)).append('|');
                out.append(tail(rw)).append(tail(r)).append(tail(w)).append('|');
                r.lock();r.lock();out.append(tail(rw)).append(tail(r)).append(tail(w)).append('|');
                r.unlock();r.unlock();w.lock();w.lock();r.lock();r.lock();r.lock();
                out.append(tail(rw)).append(tail(r)).append(tail(w).startsWith("[Locked by thread ")).append(tail(w).endsWith("]")).append('|');
                w.unlock();w.unlock();out.append(tail(rw)).append(tail(w)).append('|');
                ReentrantReadWriteLock fair=new ReentrantReadWriteLock(true);
                out.append(tail(fair)).append(head(fair)).append('|');
                String s=rw.toString();
                out.append(s.substring(s.indexOf('@')+1,s.indexOf('[')).equals(Integer.toHexString(rw.hashCode())));
                return out.toString();
            }
            public static String viaInterfaces(){
                StringBuilder out=new StringBuilder();
                ReadWriteLock rwl=new ReentrantReadWriteLock();
                Lock r=rwl.readLock(),w=rwl.writeLock();
                out.append(r==rwl.readLock()).append(w==rwl.writeLock()).append(',');
                w.lock();
                try{r.lock();try{out.append(state((ReentrantReadWriteLock)rwl)).append(',');}finally{w.unlock();}
                    out.append(state((ReentrantReadWriteLock)rwl)).append(',').append(w.tryLock()).append(',');}
                finally{r.unlock();}
                out.append(w.tryLock()).append(',').append(r.tryLock()).append(',').append(state((ReentrantReadWriteLock)rwl)).append(',');
                r.unlock();w.unlock();
                try{r.unlock();out.append("no;");}catch(IllegalMonitorStateException e){out.append(name(e)).append(';');}
                out.append(state((ReentrantReadWriteLock)rwl));
                return out.toString();
            }
            public static String independentLocks(){
                StringBuilder out=new StringBuilder();
                ReentrantReadWriteLock a=new ReentrantReadWriteLock(),b=new ReentrantReadWriteLock();
                a.writeLock().lock();b.readLock().lock();b.readLock().lock();
                out.append(state(a)).append(',').append(state(b)).append(',');
                out.append(b.writeLock().tryLock()).append(a.readLock().tryLock()).append(a.writeLock().tryLock()).append(',');
                out.append(state(a)).append(',').append(state(b)).append(',');
                try{b.writeLock().unlock();out.append("no;");}catch(IllegalMonitorStateException e){out.append(name(e)).append(';');}
                out.append(tail(a)).append(tail(b));
                return out.toString();
            }
            public static String noSelfUse(){
                StringBuilder out=new StringBuilder();
                LoggingRwl rw=new LoggingRwl(false);
                out.append(rw.log).append('|');
                rw.toString();out.append(rw.log).append('|');
                ReentrantReadWriteLock.ReadLock r=rw.readLock();ReentrantReadWriteLock.WriteLock w=rw.writeLock();
                out.append(rw.log).append('|');rw.log.setLength(0);
                r.lock();w.tryLock();r.tryLock();r.unlock();r.unlock();w.lock();w.lock();r.lock();
                r.toString();w.toString();w.getHoldCount();w.isHeldByCurrentThread();rw.isFair();rw.toString();
                out.append(rw.log).append('|');
                out.append(rw.getReadLockCount()).append(rw.getWriteHoldCount()).append('|').append(rw.log).append('|').append(head(rw)).append(tail(rw));
                return out.toString();
            }
            public static String protectedConstructors(){
                StringBuilder out=new StringBuilder();StringBuilder log=new StringBuilder();
                ReentrantReadWriteLock rw=new ReentrantReadWriteLock();
                LoggingReadLock lr=new LoggingReadLock(rw,log);LoggingWriteLock lw=new LoggingWriteLock(rw,log);
                out.append(log).append('|').append(lr==(Object)rw.readLock()).append(',');
                lr.lock();out.append(state(rw)).append(',');
                rw.readLock().unlock();out.append(lr.tryLock()).append(',').append(state(rw)).append(',');
                out.append(lw.tryLock()).append(',');lr.unlock();
                lw.lock();lw.lock();out.append(state(rw)).append(',').append(rw.writeLock().getHoldCount()).append(rw.writeLock().isHeldByCurrentThread()).append(',');
                out.append(lw.getHoldCount()).append(',').append(lw.isHeldByCurrentThread()).append(',');
                rw.writeLock().unlock();lw.unlock();out.append(state(rw)).append('|').append(log).append('|');log.setLength(0);
                out.append(head(lr)).append(tail(lr)).append(head(lw)).append(tail(lw)).append(log).append('|');
                try{lw.unlock();out.append("no;");}catch(IllegalMonitorStateException e){out.append(fail(e)).append(';');}
                try{lr.unlock();out.append("no;");}catch(IllegalMonitorStateException e){out.append(fail(e)).append(';');}
                out.append(log).append('|');
                try{new ReentrantReadWriteLock.ReadLock(null){};out.append("no;");}catch(NullPointerException e){out.append(name(e)).append(';');}
                try{new ReentrantReadWriteLock.WriteLock(null){};out.append("no;");}catch(NullPointerException e){out.append(name(e)).append(';');}
                return out.toString();
            }
        }
        """;

    @Test void basics() throws Exception {
        matchesJvm(FIXTURE, "initialState", "typeChecks", "viaInterfaces", "independentLocks");
    }

    @Test void reentrancy() throws Exception {
        matchesJvm(FIXTURE, "readReentrancy", "writeReentrancy", "downgrade", "upgradeFails");
    }

    @Test void failures() throws Exception {
        matchesJvm(FIXTURE, "unlockFailures", "interruptible");
    }

    @Test void holdLimits() throws Exception {
        matchesJvm(FIXTURE, "readLimit", "writeLimit");
    }

    @Test void formatsAndSubclasses() throws Exception {
        matchesJvm(FIXTURE, "toStrings", "noSelfUse", "protectedConstructors");
    }

    @Test void leavesOutQueueQueries() {
        rejectsUnusedCall("java/util/concurrent/locks/ReentrantReadWriteLock", "hasQueuedThreads", "()Z", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/locks/ReentrantReadWriteLock", "getQueueLength", "()I", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/locks/ReentrantReadWriteLock", "hasWaiters", "(Ljava/util/concurrent/locks/Condition;)Z", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock", "newCondition", "()Ljava/util/concurrent/locks/Condition;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/locks/ReentrantReadWriteLock$WriteLock", "newCondition", "()Ljava/util/concurrent/locks/Condition;", INVOKEVIRTUAL, false);
    }
}
