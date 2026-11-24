package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T53: timed tryLock/offer/poll with TimeUnit (single-threaded). Expected values come from the JVM; see tasks/T53-timed-lock-queue.md. */
@Tag("agent-pending")
class BytecodeTimedLockQueueTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.concurrent.BlockingQueue;
        import java.util.concurrent.LinkedBlockingQueue;
        import java.util.concurrent.TimeUnit;
        import java.util.concurrent.locks.Lock;
        import java.util.concurrent.locks.ReentrantLock;
        import java.util.concurrent.locks.ReentrantReadWriteLock;

        /** Logs every untimed call a timed tryLock might make on itself. */
        class LoggingLock extends ReentrantLock {
            final StringBuilder log=new StringBuilder();
            public void lock(){log.append("lock;");super.lock();}
            public boolean tryLock(){log.append("tryLock;");return super.tryLock();}
            public int getHoldCount(){log.append("holds;");return super.getHoldCount();}
            public boolean isLocked(){log.append("locked;");return super.isLocked();}
            public boolean isHeldByCurrentThread(){log.append("held;");return super.isHeldByCurrentThread();}
        }
        /** Overrides the timed tryLock itself; reached through the Lock interface. */
        class TimedOverride extends ReentrantLock {
            final StringBuilder log=new StringBuilder();
            public boolean tryLock(long t,TimeUnit u) throws InterruptedException{log.append("timed:").append(t).append(u).append(';');return false;}
        }
        class LoggingReadLock extends ReentrantReadWriteLock.ReadLock {
            final StringBuilder log;
            LoggingReadLock(ReentrantReadWriteLock rw,StringBuilder log){super(rw);this.log=log;}
            public void lock(){log.append("r.lock;");super.lock();}
            public boolean tryLock(){log.append("r.tryLock;");return super.tryLock();}
        }
        class LoggingWriteLock extends ReentrantReadWriteLock.WriteLock {
            final StringBuilder log;
            LoggingWriteLock(ReentrantReadWriteLock rw,StringBuilder log){super(rw);this.log=log;}
            public void lock(){log.append("w.lock;");super.lock();}
            public boolean tryLock(){log.append("w.tryLock;");return super.tryLock();}
            public int getHoldCount(){log.append("w.holds;");return super.getHoldCount();}
            public boolean isHeldByCurrentThread(){log.append("w.held;");return super.isHeldByCurrentThread();}
        }
        /** Logs the overridable calls a timed offer/poll might make on itself. */
        class Spy<E> extends LinkedBlockingQueue<E> {
            final StringBuilder log=new StringBuilder();
            Spy(int capacity){super(capacity);}
            public boolean offer(E e){log.append("offer:").append(e).append(' ');return super.offer(e);}
            public E poll(){E e=super.poll();log.append("poll:").append(e).append(' ');return e;}
            public E peek(){E e=super.peek();log.append("peek:").append(e).append(' ');return e;}
            public int size(){int n=super.size();log.append("size:").append(n).append(' ');return n;}
            public int remainingCapacity(){log.append("remaining ");return super.remainingCapacity();}
            public boolean isEmpty(){log.append("isEmpty ");return super.isEmpty();}
            public void put(E e) throws InterruptedException{log.append("put:").append(e).append(' ');super.put(e);}
            public E take() throws InterruptedException{E e=super.take();log.append("take:").append(e).append(' ');return e;}
        }
        /** Overrides the timed queue operations; reached through the BlockingQueue interface. */
        class TimedQueue extends LinkedBlockingQueue<String> {
            public boolean offer(String e,long t,TimeUnit u){return super.offer("T"+e);}
            public String poll(long t,TimeUnit u){String s=super.poll();return s==null?"none":s+"!";}
        }

        public class Fixture {
            interface Op{Object run() throws Exception;}
            /** Exception class only: the JVM's helpful NPE message is not compared. */
            static String t(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName();}
            }
            static String state(ReentrantLock l){return l.isLocked()+"/"+l.getHoldCount();}
            static String state(ReentrantReadWriteLock rw){return "r"+rw.getReadHoldCount()+"w"+rw.getWriteHoldCount();}

            public static String lockTimed(){
                StringBuilder out=new StringBuilder();
                ReentrantLock l=new ReentrantLock();
                out.append(t(()->l.tryLock(0,TimeUnit.SECONDS))).append(',').append(state(l)).append(';');
                out.append(t(()->l.tryLock(-5,TimeUnit.MILLISECONDS))).append(',').append(state(l)).append(';');
                out.append(t(()->l.tryLock(1,TimeUnit.NANOSECONDS))).append(',').append(state(l)).append(';');
                out.append(t(()->l.tryLock(Long.MAX_VALUE,TimeUnit.DAYS))).append(',').append(state(l)).append(';');
                out.append(t(()->l.tryLock(Long.MIN_VALUE,TimeUnit.MICROSECONDS))).append(',').append(state(l)).append(';');
                while(l.isLocked())l.unlock();
                out.append(t(()->l.tryLock(3,null))).append(',').append(state(l)).append(';');
                l.lock();
                out.append(t(()->l.tryLock(0,null))).append(',').append(state(l)).append(';');
                l.unlock();
                ReentrantLock fair=new ReentrantLock(true);
                out.append(t(()->fair.tryLock(10,TimeUnit.MILLISECONDS))).append(',');
                out.append(t(()->fair.tryLock(0,TimeUnit.HOURS))).append(',').append(state(fair)).append(',').append(fair.isFair()).append(';');
                fair.unlock();fair.unlock();
                out.append(state(fair));
                return out.toString();
            }
            public static String lockCallPattern(){
                StringBuilder out=new StringBuilder();
                LoggingLock l=new LoggingLock();
                out.append(t(()->l.tryLock(0,TimeUnit.SECONDS))).append(l.log).append('|');l.log.setLength(0);
                out.append(t(()->l.tryLock(2,TimeUnit.MINUTES))).append(l.log).append('|');l.log.setLength(0);
                out.append(t(()->l.tryLock(2,null))).append(l.log).append('|');l.log.setLength(0);
                out.append(state(l)).append(l.log).append('|');l.log.setLength(0);
                l.unlock();l.unlock();
                Lock[] locks={new ReentrantLock(),new ReentrantLock(true),new LoggingLock(),new TimedOverride()};
                for(Lock k:locks){
                    out.append(t(()->k.tryLock(0,TimeUnit.SECONDS))).append(',');
                    out.append(t(()->k.tryLock(1,TimeUnit.NANOSECONDS))).append(',');
                    out.append(t(()->k.tryLock(1,null))).append(';');
                }
                out.append(((LoggingLock)locks[2]).log).append(((TimedOverride)locks[3]).log).append('|');
                for(int i=0;i<3;i++){ReentrantLock r=(ReentrantLock)locks[i];out.append(r.getHoldCount());while(r.isLocked())r.unlock();}
                out.append(((ReentrantLock)locks[3]).isLocked());
                return out.toString();
            }
            public static String readWriteTimed(){
                StringBuilder out=new StringBuilder();
                for(boolean fair:new boolean[]{false,true}){
                    ReentrantReadWriteLock rw=new ReentrantReadWriteLock(fair);
                    ReentrantReadWriteLock.ReadLock r=rw.readLock();ReentrantReadWriteLock.WriteLock w=rw.writeLock();
                    out.append(t(()->w.tryLock(0,TimeUnit.SECONDS))).append(',').append(state(rw)).append(',');
                    out.append(t(()->w.tryLock(1,TimeUnit.NANOSECONDS))).append(',').append(state(rw)).append(',');
                    // downgrade: the writer may also take the read lock
                    out.append(t(()->r.tryLock(0,TimeUnit.SECONDS))).append(',').append(state(rw)).append(',');
                    // writer that also reads re-enters the write lock
                    out.append(t(()->w.tryLock(-1,TimeUnit.SECONDS))).append(',').append(state(rw)).append(',');
                    w.unlock();w.unlock();w.unlock();
                    // upgrade: a reader cannot take the write lock; timed tryLock returns false without blocking
                    out.append(t(()->w.tryLock(0,TimeUnit.SECONDS))).append(',');
                    out.append(t(()->w.tryLock(-3,TimeUnit.DAYS))).append(',');
                    out.append(t(()->w.tryLock(1,TimeUnit.NANOSECONDS))).append(',').append(state(rw)).append(',');
                    out.append(t(()->r.tryLock(5,TimeUnit.MILLISECONDS))).append(',').append(state(rw)).append(',');
                    r.unlock();r.unlock();
                    out.append(t(()->r.tryLock(1,null))).append(',').append(t(()->w.tryLock(1,null))).append(',').append(state(rw)).append(',');
                    r.lock();
                    out.append(t(()->w.tryLock(0,null))).append(',').append(state(rw)).append(',');
                    r.unlock();
                    out.append(t(()->w.tryLock(Long.MAX_VALUE,TimeUnit.NANOSECONDS))).append(',').append(state(rw)).append(',').append(rw.isWriteLocked()).append(';');
                    w.unlock();
                }
                return out.toString();
            }
            public static String readWriteCallPattern(){
                StringBuilder out=new StringBuilder();StringBuilder log=new StringBuilder();
                ReentrantReadWriteLock rw=new ReentrantReadWriteLock();
                LoggingReadLock r=new LoggingReadLock(rw,log);LoggingWriteLock w=new LoggingWriteLock(rw,log);
                out.append(t(()->r.tryLock(0,TimeUnit.SECONDS))).append(log).append('|');log.setLength(0);
                out.append(t(()->w.tryLock(0,TimeUnit.SECONDS))).append(log).append('|');log.setLength(0);
                rw.readLock().unlock();
                out.append(t(()->w.tryLock(0,TimeUnit.SECONDS))).append(log).append('|');log.setLength(0);
                out.append(t(()->w.tryLock(0,TimeUnit.SECONDS))).append(log).append('|');log.setLength(0);
                out.append(state(rw)).append('|');
                Lock[] locks={rw.readLock(),rw.writeLock(),r,w};
                for(Lock k:locks)out.append(t(()->k.tryLock(0,TimeUnit.MILLISECONDS))).append(',');
                out.append(log).append(state(rw));
                return out.toString();
            }
            public static String queueTimed(){
                StringBuilder out=new StringBuilder();
                LinkedBlockingQueue<String> q=new LinkedBlockingQueue<>(2);
                out.append(t(()->q.offer("a",0,TimeUnit.SECONDS))).append(',');
                out.append(t(()->q.offer("b",-1,TimeUnit.MILLISECONDS))).append(',');
                out.append(t(()->q.offer("c",0,TimeUnit.SECONDS))).append(',');
                out.append(t(()->q.offer("d",1,TimeUnit.NANOSECONDS))).append(',');
                out.append(t(()->q.offer("e",Long.MIN_VALUE,TimeUnit.DAYS))).append(',');
                out.append(q).append(q.size()).append(q.remainingCapacity()).append(';');
                out.append(t(()->q.offer(null,0,TimeUnit.SECONDS))).append(',');
                out.append(t(()->q.offer("f",0,null))).append(',');
                out.append(t(()->q.offer(null,0,null))).append(',').append(q).append(';');
                out.append(t(()->q.poll(0,TimeUnit.SECONDS))).append(',');
                out.append(t(()->q.poll(1,null))).append(',').append(q).append(',');
                out.append(t(()->q.offer("g",0,null))).append(',');
                out.append(t(()->q.offer(null,0,TimeUnit.SECONDS))).append(',').append(q).append(',');
                out.append(t(()->q.offer("h",Long.MAX_VALUE,TimeUnit.DAYS))).append(',').append(q).append(';');
                out.append(t(()->q.poll(-1,TimeUnit.MINUTES))).append(',');
                out.append(t(()->q.poll(Long.MAX_VALUE,TimeUnit.SECONDS))).append(',');
                out.append(t(()->q.poll(0,TimeUnit.SECONDS))).append(',');
                out.append(t(()->q.poll(1,TimeUnit.NANOSECONDS))).append(',');
                out.append(t(()->q.poll(Long.MIN_VALUE,TimeUnit.MICROSECONDS))).append(',');
                out.append(t(()->q.poll(0,null))).append(',').append(q).append(q.size()).append(';');
                LinkedBlockingQueue<Integer> u=new LinkedBlockingQueue<>();
                for(int i=0;i<4;i++)out.append(t(()->u.offer(u.size(),1,TimeUnit.HOURS)));
                out.append(u.remainingCapacity()).append(',');
                out.append(t(()->u.poll(1,TimeUnit.HOURS))).append(t(()->u.peek())).append(',');
                java.util.Iterator<Integer> it=u.iterator();
                out.append(t(()->u.poll(0,TimeUnit.SECONDS))).append(t(()->it.next())).append(t(()->it.next())).append(u);
                return out.toString();
            }
            public static String queueCallPattern(){
                StringBuilder out=new StringBuilder();
                Spy<String> s=new Spy<>(2);
                out.append(t(()->s.offer("a",0,TimeUnit.SECONDS))).append(s.log).append('|');s.log.setLength(0);
                out.append(t(()->s.offer("b",0,TimeUnit.SECONDS))).append(s.log).append('|');s.log.setLength(0);
                out.append(t(()->s.offer("c",0,TimeUnit.SECONDS))).append(s.log).append('|');s.log.setLength(0);
                out.append(t(()->s.offer("d",0,null))).append(s.log).append('|');s.log.setLength(0);
                out.append(t(()->s.poll(0,TimeUnit.SECONDS))).append(s.log).append('|');s.log.setLength(0);
                out.append(t(()->s.poll(0,null))).append(s.log).append('|');s.log.setLength(0);
                out.append(t(()->s.poll(0,TimeUnit.SECONDS))).append(s.log).append('|');s.log.setLength(0);
                out.append(t(()->s.poll(0,TimeUnit.SECONDS))).append(s.log).append('|');s.log.setLength(0);
                BlockingQueue<String>[] qs=new BlockingQueue[]{new LinkedBlockingQueue<String>(1),new Spy<String>(1),new TimedQueue()};
                for(BlockingQueue<String> b:qs){
                    out.append(t(()->b.offer("x",0,TimeUnit.SECONDS))).append(',');
                    out.append(t(()->b.offer("y",0,TimeUnit.SECONDS))).append(',');
                    out.append(t(()->b.poll(0,TimeUnit.SECONDS))).append(',');
                    out.append(t(()->b.poll(0,TimeUnit.SECONDS))).append(',');
                    out.append(t(()->b.offer(null,0,TimeUnit.SECONDS))).append(',');
                    out.append(t(()->b.poll(0,null))).append(';');
                }
                out.append(((Spy<String>)qs[1]).log).append('|').append(qs[2]);
                return out.toString();
            }
        }
        """;

    @Test void timedReentrantLock() throws Exception {
        matchesJvm(FIXTURE, "lockTimed", "lockCallPattern");
    }

    @Test void timedReadWriteLock() throws Exception {
        matchesJvm(FIXTURE, "readWriteTimed", "readWriteCallPattern");
    }

    @Test void timedQueueOperations() throws Exception {
        matchesJvm(FIXTURE, "queueTimed", "queueCallPattern");
    }

    @Test void leavesOutConditionsAndSerialization() {
        rejectsUnusedCall("java/util/concurrent/locks/ReentrantLock", "newCondition", "()Ljava/util/concurrent/locks/Condition;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/locks/Lock", "newCondition", "()Ljava/util/concurrent/locks/Condition;", INVOKEINTERFACE, true);
        rejectsUnusedCall("java/util/concurrent/locks/ReentrantLock", "getQueueLength", "()I", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/locks/ReentrantReadWriteLock", "hasQueuedThreads", "()Z", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/LinkedBlockingQueue", "writeObject", "(Ljava/io/ObjectOutputStream;)V", INVOKESPECIAL, false);
    }
}
