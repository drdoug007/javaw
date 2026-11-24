package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T64: timed CountDownLatch.await and Semaphore.tryAcquire (single-threaded). Expected values come from the JVM; see tasks/T64-timed-latch-semaphore.md. */
class BytecodeTimedLatchSemaphoreTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.concurrent.CountDownLatch;
        import java.util.concurrent.Semaphore;
        import java.util.concurrent.TimeUnit;
        class LatchSpy extends CountDownLatch {
            final StringBuilder log=new StringBuilder();
            LatchSpy(int c){super(c);}
            public void await() throws InterruptedException{log.append("await;");super.await();}
            public void countDown(){log.append("down;");super.countDown();}
            public long getCount(){log.append("count;");return super.getCount();}
        }
        class SemSpy extends Semaphore {
            final StringBuilder log=new StringBuilder();
            SemSpy(int p){super(p);}
            SemSpy(int p,boolean fair){super(p,fair);}
            public void acquire() throws InterruptedException{log.append("acquire;");super.acquire();}
            public void acquire(int n) throws InterruptedException{log.append("acquire:"+n+";");super.acquire(n);}
            public void acquireUninterruptibly(){log.append("acqU;");super.acquireUninterruptibly();}
            public void acquireUninterruptibly(int n){log.append("acqU:"+n+";");super.acquireUninterruptibly(n);}
            public boolean tryAcquire(){log.append("try;");return super.tryAcquire();}
            public boolean tryAcquire(int n){log.append("try:"+n+";");return super.tryAcquire(n);}
            public int availablePermits(){log.append("avail;");return super.availablePermits();}
            public boolean isFair(){log.append("fair;");return super.isFair();}
        }
        class TimedLatch extends CountDownLatch {
            TimedLatch(){super(5);}
            public boolean await(long t,TimeUnit u) throws InterruptedException{return t==7;}
        }
        class TimedSem extends Semaphore {
            TimedSem(){super(0);}
            public boolean tryAcquire(long t,TimeUnit u) throws InterruptedException{return t==7;}
            public boolean tryAcquire(int n,long t,TimeUnit u) throws InterruptedException{return n==t;}
        }
        public class Fixture {
            static String name(Throwable t){return t.getClass().getName();}

            public static String latchTimed(){
                StringBuilder out=new StringBuilder();
                try{
                    CountDownLatch z=new CountDownLatch(0);
                    out.append(z.await(0,TimeUnit.SECONDS)).append(',').append(z.await(-3,TimeUnit.MILLISECONDS)).append(',');
                    out.append(z.await(Long.MAX_VALUE,TimeUnit.DAYS)).append(',').append(z.await(Long.MIN_VALUE,TimeUnit.NANOSECONDS)).append(';');
                    CountDownLatch two=new CountDownLatch(2);
                    out.append(two.await(0,TimeUnit.SECONDS)).append(',').append(two.await(1,TimeUnit.NANOSECONDS)).append(',');
                    out.append(two.await(-1,TimeUnit.DAYS)).append(',').append(two.getCount()).append(',');
                    two.countDown();
                    out.append(two.await(Long.MIN_VALUE,TimeUnit.HOURS)).append(',').append(two.getCount()).append(',');
                    two.countDown();
                    out.append(two.await(Long.MAX_VALUE,TimeUnit.SECONDS)).append(',').append(two.getCount()).append(',');
                    two.countDown();
                    out.append(two.await(5,TimeUnit.MINUTES)).append(',').append(two.getCount()).append(';');
                    CountDownLatch big=new CountDownLatch(Integer.MAX_VALUE);
                    out.append(big.await(0,TimeUnit.MICROSECONDS)).append(',').append(big.getCount());
                }catch(InterruptedException e){out.append(name(e));}
                return out.toString();
            }
            public static String latchNullUnit(){
                StringBuilder out=new StringBuilder();
                CountDownLatch z=new CountDownLatch(0),one=new CountDownLatch(1);
                try{out.append(z.await(1,null));}catch(NullPointerException e){out.append(name(e));}catch(InterruptedException e){out.append("int");}
                out.append(',');
                try{out.append(one.await(0,null));}catch(NullPointerException e){out.append(name(e));}catch(InterruptedException e){out.append("int");}
                out.append(',').append(one.getCount()).append(',');
                one.countDown();
                try{out.append(one.await(Long.MAX_VALUE,null));}catch(NullPointerException e){out.append(name(e));}catch(InterruptedException e){out.append("int");}
                out.append(',').append(one.getCount());
                return out.toString();
            }
            public static String latchCallPattern(){
                StringBuilder out=new StringBuilder();
                LatchSpy s=new LatchSpy(1);
                try{
                    out.append(s.await(0,TimeUnit.SECONDS)).append(',');
                    out.append(s.log).append('|');s.log.setLength(0);
                    CountDownLatch base=s;
                    base.countDown();s.log.setLength(0);
                    out.append(base.await(1,TimeUnit.NANOSECONDS)).append(',').append(s.log).append('|');
                    CountDownLatch t=new TimedLatch();
                    out.append(t.await(7,TimeUnit.SECONDS)).append(',').append(t.await(0,null)).append(',').append(t.getCount());
                }catch(InterruptedException e){out.append(name(e));}
                return out.toString();
            }
            public static String semaphoreTimed(){
                StringBuilder out=new StringBuilder();
                try{
                    Semaphore s=new Semaphore(3);
                    out.append(s.tryAcquire(0,TimeUnit.SECONDS)).append(s.availablePermits()).append(',');
                    out.append(s.tryAcquire(Long.MAX_VALUE,TimeUnit.DAYS)).append(s.availablePermits()).append(',');
                    out.append(s.tryAcquire(Long.MIN_VALUE,TimeUnit.NANOSECONDS)).append(s.availablePermits()).append(',');
                    out.append(s.tryAcquire(1,TimeUnit.NANOSECONDS)).append(s.availablePermits()).append(',');
                    out.append(s.tryAcquire(-9,TimeUnit.MILLISECONDS)).append(s.availablePermits()).append(';');
                    Semaphore m=new Semaphore(5,true);
                    out.append(m.tryAcquire(2,0,TimeUnit.SECONDS)).append(m.availablePermits()).append(',');
                    out.append(m.tryAcquire(4,1,TimeUnit.NANOSECONDS)).append(m.availablePermits()).append(',');
                    out.append(m.tryAcquire(3,Long.MAX_VALUE,TimeUnit.HOURS)).append(m.availablePermits()).append(',');
                    out.append(m.tryAcquire(0,-1,TimeUnit.SECONDS)).append(m.availablePermits()).append(',');
                    out.append(m.tryAcquire(1,0,TimeUnit.MICROSECONDS)).append(m.availablePermits()).append(';');
                    Semaphore neg=new Semaphore(-2);
                    out.append(neg.tryAcquire(0,0,TimeUnit.SECONDS)).append(',').append(neg.tryAcquire(0,TimeUnit.SECONDS)).append(',');
                    neg.release(3);
                    out.append(neg.tryAcquire(1,TimeUnit.NANOSECONDS)).append(',').append(neg.availablePermits()).append(';');
                    Semaphore big=new Semaphore(Integer.MAX_VALUE);
                    out.append(big.tryAcquire(Integer.MAX_VALUE,Long.MAX_VALUE,TimeUnit.SECONDS)).append(',').append(big.availablePermits());
                    out.append(',').append(big.tryAcquire(Integer.MAX_VALUE,0,TimeUnit.SECONDS));
                }catch(InterruptedException e){out.append(name(e));}
                return out.toString();
            }
            public static String semaphoreArguments(){
                StringBuilder out=new StringBuilder();
                Semaphore s=new Semaphore(4);
                try{out.append(s.tryAcquire(1,null));}catch(NullPointerException e){out.append(name(e));}catch(InterruptedException e){out.append("int");}
                out.append(s.availablePermits()).append(',');
                try{out.append(s.tryAcquire(2,5,null));}catch(NullPointerException e){out.append(name(e));}catch(InterruptedException e){out.append("int");}
                out.append(s.availablePermits()).append(',');
                try{out.append(s.tryAcquire(9,0,null));}catch(NullPointerException e){out.append(name(e));}catch(InterruptedException e){out.append("int");}
                out.append(s.availablePermits()).append(',');
                try{out.append(s.tryAcquire(-1,0,TimeUnit.SECONDS));}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage());}catch(InterruptedException e){out.append("int");}
                out.append(',');
                try{out.append(s.tryAcquire(-1,0,null));}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage());}catch(NullPointerException e){out.append("npe");}catch(InterruptedException e){out.append("int");}
                out.append(',');
                try{out.append(s.tryAcquire(Integer.MIN_VALUE,Long.MAX_VALUE,TimeUnit.DAYS));}catch(RuntimeException e){out.append(name(e));}catch(InterruptedException e){out.append("int");}
                out.append(',').append(s.availablePermits()).append(';');
                Semaphore empty=new Semaphore(0);
                try{out.append(empty.tryAcquire(0,null));}catch(NullPointerException e){out.append(name(e));}catch(InterruptedException e){out.append("int");}
                out.append(',');
                try{out.append(empty.tryAcquire(0,0,null));}catch(NullPointerException e){out.append(name(e));}catch(InterruptedException e){out.append("int");}
                out.append(',').append(empty.availablePermits());
                return out.toString();
            }
            public static String semaphoreCallPattern(){
                StringBuilder out=new StringBuilder();
                SemSpy s=new SemSpy(4,true);
                try{
                    out.append(s.tryAcquire(0,TimeUnit.SECONDS)).append(',');
                    out.append(s.tryAcquire(2,1,TimeUnit.NANOSECONDS)).append(',');
                    out.append(s.tryAcquire(5,0,TimeUnit.SECONDS)).append(',');
                    out.append(s.tryAcquire(1,0,TimeUnit.SECONDS)).append(',');
                    out.append(s.tryAcquire(0,TimeUnit.SECONDS)).append(',');
                    out.append(s.log).append('|');
                    out.append(s.availablePermits()).append(',');
                    Semaphore t=new TimedSem();
                    out.append(t.tryAcquire(7,TimeUnit.SECONDS)).append(',').append(t.tryAcquire(3,null)).append(',');
                    out.append(t.tryAcquire(4,4,TimeUnit.DAYS)).append(',').append(t.tryAcquire(-1,2,null)).append(',').append(t.availablePermits());
                }catch(InterruptedException e){out.append(name(e));}
                return out.toString();
            }
            public static String timedGuard(){
                StringBuilder out=new StringBuilder();
                Semaphore permits=new Semaphore(2);
                CountDownLatch done=new CountDownLatch(3);
                try{
                    for(int i=0;i<5;i++){
                        if(permits.tryAcquire(10,TimeUnit.NANOSECONDS)){
                            out.append('+');
                            if(i%2==0){permits.release();done.countDown();}
                        }else out.append('-');
                        out.append(done.await(0,TimeUnit.SECONDS)?'D':'w');
                    }
                }catch(InterruptedException e){out.append(name(e));}
                out.append(',').append(permits.availablePermits()).append(',').append(done.getCount());
                return out.toString();
            }
        }
        """;

    @Test void latchTimedAwait() throws Exception {
        matchesJvm(FIXTURE, "latchTimed", "latchNullUnit", "latchCallPattern");
    }

    @Test void semaphoreTimedTryAcquire() throws Exception {
        matchesJvm(FIXTURE, "semaphoreTimed", "semaphoreArguments", "semaphoreCallPattern");
    }

    @Test void timedGuardLoop() throws Exception {
        matchesJvm(FIXTURE, "timedGuard");
    }

    @Test void leavesOutQueuesAndThreads() {
        rejectsUnusedCall("java/util/concurrent/Semaphore", "hasQueuedThreads", "()Z", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/Semaphore", "getQueueLength", "()I", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/Semaphore", "getQueuedThreads", "()Ljava/util/Collection;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/TimeUnit", "sleep", "(J)V", INVOKEVIRTUAL, false);
    }
}
