package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T55: java.util.concurrent.Semaphore (single-threaded). Expected values come from the JVM; see tasks/T55-semaphore.md. */
class BytecodeSemaphoreTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.concurrent.Semaphore;
        class Spy extends Semaphore {
            final StringBuilder log=new StringBuilder();
            Spy(int p){super(p);}
            Spy(int p,boolean fair){super(p,fair);}
            public void acquire() throws InterruptedException{log.append("acquire;");super.acquire();}
            public void acquire(int n) throws InterruptedException{log.append("acquire:"+n+";");super.acquire(n);}
            public void acquireUninterruptibly(){log.append("acqU;");super.acquireUninterruptibly();}
            public void acquireUninterruptibly(int n){log.append("acqU:"+n+";");super.acquireUninterruptibly(n);}
            public boolean tryAcquire(){log.append("try;");return super.tryAcquire();}
            public boolean tryAcquire(int n){log.append("try:"+n+";");return super.tryAcquire(n);}
            public void release(){log.append("release;");super.release();}
            public void release(int n){log.append("release:"+n+";");super.release(n);}
            public int availablePermits(){log.append("avail;");return super.availablePermits();}
            public int drainPermits(){log.append("drain;");return super.drainPermits();}
            public boolean isFair(){log.append("fair;");return super.isFair();}
            public int hashCode(){log.append("hash;");return 0x2a;}
            void reduce(int n){reducePermits(n);}
        }
        class Reducer extends Semaphore {
            Reducer(int p){super(p);}
            protected void reducePermits(int n){super.reducePermits(n);}
        }
        public class Fixture {
            static String name(Throwable t){return t.getClass().getName();}
            static String fail(Throwable t){return name(t)+":"+t.getMessage();}
            static String suffix(Semaphore s){
                String t=s.toString();
                boolean prefix=t.startsWith(s.getClass().getName()+"@"+Integer.toHexString(s.hashCode())+"[");
                return prefix+t.substring(t.indexOf('['));
            }

            public static String basics(){
                StringBuilder out=new StringBuilder();
                Semaphore a=new Semaphore(3),b=new Semaphore(0,true),c=new Semaphore(-2,false),d=new Semaphore(5,true);
                out.append(a.availablePermits()).append(',').append(b.availablePermits()).append(',');
                out.append(c.availablePermits()).append(',').append(d.availablePermits()).append(';');
                out.append(a.isFair()).append(',').append(b.isFair()).append(',').append(c.isFair()).append(',').append(d.isFair()).append(';');
                out.append((Object)a instanceof java.io.Serializable).append(',').append(a.equals(a)).append(',').append(a.equals(new Semaphore(3))).append(';');
                Semaphore max=new Semaphore(Integer.MAX_VALUE),min=new Semaphore(Integer.MIN_VALUE,true);
                out.append(max.availablePermits()).append(',').append(min.availablePermits());
                return out.toString();
            }
            public static String acquiring(){
                StringBuilder out=new StringBuilder();
                Semaphore s=new Semaphore(10);
                try{
                    s.acquire();out.append(s.availablePermits()).append(',');
                    s.acquire(3);out.append(s.availablePermits()).append(',');
                    s.acquire(0);out.append(s.availablePermits()).append(',');
                }catch(InterruptedException e){out.append(name(e));}
                s.acquireUninterruptibly();out.append(s.availablePermits()).append(',');
                s.acquireUninterruptibly(4);out.append(s.availablePermits()).append(',');
                s.acquireUninterruptibly(0);out.append(s.availablePermits()).append(',');
                try{s.acquire(1);}catch(InterruptedException e){out.append(name(e));}
                out.append(s.availablePermits()).append(';');
                Semaphore f=new Semaphore(2,true);
                try{f.acquire(2);}catch(InterruptedException e){out.append(name(e));}
                out.append(f.availablePermits()).append(',').append(f.tryAcquire()).append(',').append(f.tryAcquire(0)).append(';');
                f.release(5);f.acquireUninterruptibly(5);out.append(f.availablePermits());
                return out.toString();
            }
            public static String tryAcquiring(){
                StringBuilder out=new StringBuilder();
                Semaphore s=new Semaphore(3);
                out.append(s.tryAcquire()).append(s.availablePermits()).append(',');
                out.append(s.tryAcquire(3)).append(s.availablePermits()).append(',');
                out.append(s.tryAcquire(2)).append(s.availablePermits()).append(',');
                out.append(s.tryAcquire()).append(s.availablePermits()).append(',');
                out.append(s.tryAcquire()).append(s.availablePermits()).append(',');
                out.append(s.tryAcquire(0)).append(s.availablePermits()).append(';');
                Semaphore neg=new Semaphore(-1,true);
                out.append(neg.tryAcquire(0)).append(',').append(neg.tryAcquire()).append(',').append(neg.availablePermits()).append(';');
                Semaphore big=new Semaphore(Integer.MAX_VALUE);
                out.append(big.tryAcquire(Integer.MAX_VALUE)).append(',').append(big.availablePermits()).append(',').append(big.tryAcquire());
                return out.toString();
            }
            public static String negativeArguments(){
                StringBuilder out=new StringBuilder();
                Semaphore s=new Semaphore(4);
                try{s.acquire(-1);out.append("no,");}catch(IllegalArgumentException e){out.append(fail(e)).append(',');}catch(InterruptedException e){out.append(name(e));}
                try{s.acquireUninterruptibly(-1);out.append("no,");}catch(IllegalArgumentException e){out.append(fail(e)).append(',');}
                try{s.tryAcquire(-1);out.append("no,");}catch(IllegalArgumentException e){out.append(fail(e)).append(',');}
                try{s.release(-1);out.append("no,");}catch(IllegalArgumentException e){out.append(fail(e)).append(',');}
                try{new Reducer(1).reducePermits(-1);out.append("no,");}catch(IllegalArgumentException e){out.append(fail(e)).append(',');}
                try{s.tryAcquire(Integer.MIN_VALUE);out.append("no,");}catch(RuntimeException e){out.append(name(e)).append(',');}
                out.append(s.availablePermits());
                return out.toString();
            }
            public static String releasing(){
                StringBuilder out=new StringBuilder();
                Semaphore s=new Semaphore(0);
                s.release();out.append(s.availablePermits()).append(',');
                s.release(4);out.append(s.availablePermits()).append(',');
                s.release(0);out.append(s.availablePermits()).append(';');
                Semaphore neg=new Semaphore(-3);
                neg.release();out.append(neg.availablePermits()).append(',');neg.release(5);out.append(neg.availablePermits()).append(';');
                Semaphore max=new Semaphore(Integer.MAX_VALUE);
                try{max.release();out.append("no,");}catch(Error e){out.append(fail(e)).append(',');}
                max.release(0);out.append(max.availablePermits()).append(',');
                Semaphore near=new Semaphore(Integer.MAX_VALUE-2);
                near.release(2);out.append(near.availablePermits()).append(',');
                near.acquireUninterruptibly(10);
                try{near.release(11);out.append("no,");}catch(Error e){out.append(fail(e)).append(',').append((Object)e instanceof RuntimeException).append(',');}
                out.append(near.availablePermits()).append(',');
                Semaphore low=new Semaphore(Integer.MIN_VALUE);
                low.release(Integer.MAX_VALUE);out.append(low.availablePermits());
                try{low.release(Integer.MAX_VALUE);out.append(",").append(low.availablePermits());}catch(Error e){out.append(',').append(fail(e));}
                return out.toString();
            }
            public static String drainingAndReducing(){
                StringBuilder out=new StringBuilder();
                Semaphore s=new Semaphore(7);
                out.append(s.drainPermits()).append(',').append(s.availablePermits()).append(',').append(s.drainPermits()).append(';');
                Semaphore neg=new Semaphore(-4);
                out.append(neg.drainPermits()).append(',').append(neg.availablePermits()).append(';');
                Reducer r=new Reducer(5);
                r.reducePermits(2);out.append(r.availablePermits()).append(',');
                r.reducePermits(0);out.append(r.availablePermits()).append(',');
                r.reducePermits(6);out.append(r.availablePermits()).append(',').append(r.tryAcquire(0)).append(',');
                r.release(4);out.append(r.availablePermits()).append(',').append(r.tryAcquire()).append(';');
                Reducer low=new Reducer(Integer.MIN_VALUE+1);
                low.reducePermits(1);out.append(low.availablePermits()).append(',');
                try{low.reducePermits(1);out.append("no,");}catch(Error e){out.append(fail(e)).append(',');}
                out.append(low.availablePermits()).append(',').append(low.drainPermits()).append(',').append(low.availablePermits());
                return out.toString();
            }
            public static String toStrings(){
                StringBuilder out=new StringBuilder();
                Semaphore s=new Semaphore(3);
                out.append(suffix(s)).append(';');
                s.acquireUninterruptibly(3);out.append(suffix(s)).append(';');
                out.append(suffix(new Semaphore(-12,true))).append(';');
                out.append(suffix(new Semaphore(Integer.MIN_VALUE))).append(';');
                Spy spy=new Spy(9);
                String t=spy.toString();
                out.append(t).append('|').append(spy.log);
                return out.toString();
            }
            public static String selfCalls(){
                StringBuilder out=new StringBuilder();
                Spy s=new Spy(10,true);
                try{s.acquire();s.log.append('|');s.acquire(2);s.log.append('|');}catch(InterruptedException e){out.append(name(e));}
                s.acquireUninterruptibly();s.log.append('|');s.acquireUninterruptibly(2);s.log.append('|');
                s.tryAcquire();s.log.append('|');s.tryAcquire(2);s.log.append('|');
                s.release();s.log.append('|');s.release(3);s.log.append('|');
                s.reduce(1);s.log.append('|');
                out.append(s.drainPermits()).append(',');s.log.append('|');
                out.append(s.isFair()).append(',');
                out.append(s.log);
                return out.toString();
            }
            public static String guard(){
                Semaphore s=new Semaphore(2);StringBuilder out=new StringBuilder();
                int total=0;
                for(int i=1;i<=5;i++){
                    if(!s.tryAcquire()){out.append('x');s.release(2);continue;}
                    try{
                        total+=i;
                        if(i==2)throw new IllegalStateException("two");
                        out.append(s.availablePermits());
                    }catch(IllegalStateException e){out.append('!');}
                    finally{if(i%2==1)s.release();}
                }
                out.append(',').append(total).append(',').append(s.availablePermits());
                return out.toString();
            }
        }
        """;

    @Test void constructAndAcquire() throws Exception {
        matchesJvm(FIXTURE, "basics", "acquiring", "tryAcquiring", "guard");
    }

    @Test void argumentsReleaseAndDrain() throws Exception {
        matchesJvm(FIXTURE, "negativeArguments", "releasing", "drainingAndReducing");
    }

    @Test void toStringAndSelfCalls() throws Exception {
        matchesJvm(FIXTURE, "toStrings", "selfCalls");
    }

    @Test void leavesOutTimedAcquireAndQueues() {
        rejectsUnusedCall("java/util/concurrent/Semaphore", "tryAcquire", "(JLjava/util/concurrent/TimeUnit;)Z", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/Semaphore", "tryAcquire", "(IJLjava/util/concurrent/TimeUnit;)Z", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/Semaphore", "hasQueuedThreads", "()Z", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/Semaphore", "getQueueLength", "()I", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/Semaphore", "getQueuedThreads", "()Ljava/util/Collection;", INVOKEVIRTUAL, false);
    }
}
