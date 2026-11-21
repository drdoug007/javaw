package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T49: java.util.concurrent.locks.Lock and ReentrantLock (single-threaded). See tasks/T49-reentrant-lock.md. */
@Tag("agent-pending")
class BytecodeReentrantLockTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.concurrent.locks.Lock;
        import java.util.concurrent.locks.ReentrantLock;
        class LoggingLock extends ReentrantLock {
            final StringBuilder log=new StringBuilder();
            LoggingLock(){super();}
            LoggingLock(boolean fair){super(fair);}
            public void lock(){log.append("lock;");super.lock();}
            public boolean tryLock(){log.append("tryLock;");return super.tryLock();}
            public void unlock(){log.append("unlock;");super.unlock();}
            public int getHoldCount(){log.append("holds;");return super.getHoldCount();}
            public boolean isLocked(){log.append("locked;");return super.isLocked();}
            public boolean isHeldByCurrentThread(){log.append("held;");return super.isHeldByCurrentThread();}
            public int hashCode(){log.append("hash;");return 0x2a;}
        }
        // A concrete Lock implementer outside ReentrantLock would have to declare tryLock(long,TimeUnit) and
        // newCondition(), whose descriptor types are not linked, so the fixture's own Lock overrides everything.
        class CountingLock extends ReentrantLock {
            int depth;final StringBuilder log=new StringBuilder();
            public void lock(){depth++;log.append("L");}
            public void lockInterruptibly(){depth++;log.append("I");}
            public boolean tryLock(){log.append("T");if(depth>0)return false;depth++;return true;}
            public void unlock(){if(depth==0)throw new IllegalMonitorStateException("free");depth--;log.append("U");}
        }
        public class Fixture {
            static String name(Throwable t){return t.getClass().getName();}
            static String state(ReentrantLock l){
                return l.isLocked()+","+l.isHeldByCurrentThread()+","+l.getHoldCount();
            }
            static String norm(String s){
                int at=s.indexOf("[Locked by thread ");
                return at<0?s:s.substring(0,at)+"[Locked by thread *]"+s.substring(s.indexOf(']',at)+1);
            }
            static String suffix(ReentrantLock l){
                String s=l.toString();
                boolean prefix=s.startsWith(l.getClass().getName()+"@"+Integer.toHexString(l.hashCode())+"[");
                String tail=s.substring(s.indexOf('['));
                if(tail.startsWith("[Locked by thread "))tail="[Locked by thread *]";
                return prefix+tail;
            }

            public static String basics(){
                ReentrantLock l=new ReentrantLock();StringBuilder out=new StringBuilder();
                out.append(state(l)).append(';');
                l.lock();out.append(state(l)).append(';');
                l.unlock();out.append(state(l)).append(';');
                out.append(l.isFair()).append(',').append(new ReentrantLock(true).isFair()).append(',').append(new ReentrantLock(false).isFair()).append(';');
                out.append((Object)l instanceof Lock).append(',').append((Object)l instanceof java.io.Serializable).append(',');
                out.append(l.equals(l)).append(',').append(l.equals(new ReentrantLock()));
                return out.toString();
            }
            public static String reentrancy(){
                ReentrantLock l=new ReentrantLock();StringBuilder out=new StringBuilder();
                for(int i=0;i<5;i++)l.lock();
                out.append(state(l)).append(';');
                l.unlock();l.unlock();out.append(state(l)).append(';');
                out.append(l.tryLock()).append(',').append(l.getHoldCount()).append(';');
                while(l.isLocked())l.unlock();
                out.append(state(l)).append(';');
                l.lock();l.unlock();l.lock();out.append(state(l));
                l.unlock();
                return out.toString();
            }
            public static String tryLocking(){
                StringBuilder out=new StringBuilder();
                ReentrantLock fair=new ReentrantLock(true);
                out.append(fair.tryLock()).append(',').append(state(fair)).append(';');
                out.append(fair.tryLock()).append(',').append(state(fair)).append(';');
                fair.unlock();fair.unlock();out.append(state(fair)).append(';');
                ReentrantLock plain=new ReentrantLock();
                plain.lock();out.append(plain.tryLock()).append(',').append(plain.getHoldCount()).append(';');
                plain.unlock();plain.unlock();out.append(plain.tryLock()).append(',').append(state(plain));
                plain.unlock();
                return out.toString();
            }
            public static String interruptibly(){
                ReentrantLock l=new ReentrantLock();StringBuilder out=new StringBuilder();
                try{
                    l.lockInterruptibly();out.append(state(l)).append(';');
                    l.lockInterruptibly();out.append(state(l)).append(';');
                }catch(InterruptedException e){out.append(name(e)).append(';');}
                l.unlock();l.unlock();out.append(state(l)).append(';');
                Lock asLock=new ReentrantLock(true);
                try{asLock.lockInterruptibly();out.append("ok;");}catch(InterruptedException e){out.append(name(e)).append(';');}
                asLock.unlock();
                out.append(((ReentrantLock)asLock).isLocked());
                return out.toString();
            }
            public static String unlockFailures(){
                StringBuilder out=new StringBuilder();
                ReentrantLock l=new ReentrantLock();
                try{l.unlock();out.append("no;");}catch(IllegalMonitorStateException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                out.append(state(l)).append(';');
                l.lock();l.unlock();
                try{l.unlock();out.append("no;");}catch(RuntimeException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                out.append(state(l)).append(';');
                l.lock();l.lock();l.unlock();
                out.append(state(l)).append(';');
                l.unlock();
                try{new ReentrantLock(true).unlock();out.append("no");}catch(IllegalMonitorStateException e){out.append(e instanceof RuntimeException).append(',').append(e.getCause());}
                return out.toString();
            }
            public static String exceptionTypes(){
                StringBuilder out=new StringBuilder();
                IllegalMonitorStateException a=new IllegalMonitorStateException();
                IllegalMonitorStateException b=new IllegalMonitorStateException("owner");
                out.append(a.getMessage()).append(',').append(b.getMessage()).append(',').append(b).append(',');
                out.append((Object)a instanceof RuntimeException).append(';');
                InterruptedException c=new InterruptedException();
                InterruptedException d=new InterruptedException("stop");
                out.append(c.getMessage()).append(',').append(d.getMessage()).append(',').append(d).append(',');
                out.append((Object)c instanceof Exception).append(',').append((Object)c instanceof RuntimeException);
                try{throw new InterruptedException("x");}catch(Exception e){out.append(',').append(name(e));}
                return out.toString();
            }
            public static String toStrings(){
                StringBuilder out=new StringBuilder();
                ReentrantLock l=new ReentrantLock();
                out.append(suffix(l)).append(';');
                l.lock();out.append(suffix(l)).append(';');
                l.lock();out.append(suffix(l)).append(';');
                l.unlock();l.unlock();out.append(suffix(l)).append(';');
                ReentrantLock f=new ReentrantLock(true);f.tryLock();
                out.append(suffix(f)).append(';');
                f.unlock();out.append(suffix(f));
                return out.toString();
            }
            public static String callPattern(){
                LoggingLock l=new LoggingLock();StringBuilder out=new StringBuilder();
                l.lock();out.append(l.log).append('|');l.log.setLength(0);
                l.tryLock();out.append(l.log).append('|');l.log.setLength(0);
                String s=l.toString();out.append(norm(s)).append(l.log).append('|');l.log.setLength(0);
                l.unlock();l.unlock();out.append(l.log).append('|');l.log.setLength(0);
                try{l.lockInterruptibly();}catch(InterruptedException e){out.append("int");}
                out.append(l.log).append('|');l.log.setLength(0);
                out.append(l.isFair()).append(l.log).append('|');l.log.setLength(0);
                out.append(norm(l.toString())).append(l.log).append('|');l.log.setLength(0);
                l.unlock();
                out.append(l.toString()).append(l.log);
                return out.toString();
            }
            public static String throughInterface(){
                StringBuilder out=new StringBuilder();
                Lock[] locks={new ReentrantLock(),new ReentrantLock(true),new LoggingLock(false)};
                for(Lock k:locks){
                    k.lock();out.append(k.tryLock()).append(',');
                    k.unlock();k.unlock();
                    out.append(k.tryLock()).append(',');k.unlock();
                    try{k.unlock();out.append("no;");}catch(IllegalMonitorStateException e){out.append("ims;");}
                }
                out.append(((LoggingLock)locks[2]).log).append(';');
                CountingLock c=new CountingLock();Lock k=c;
                k.lock();out.append(k.tryLock()).append(',');
                try{k.lockInterruptibly();}catch(InterruptedException e){out.append("int");}
                k.unlock();k.unlock();
                try{k.unlock();}catch(IllegalMonitorStateException e){out.append(e.getMessage()).append(',');}
                out.append(c.log).append(',').append(c.depth);
                return out.toString();
            }
            public static String lockGuard(){
                ReentrantLock l=new ReentrantLock();StringBuilder out=new StringBuilder();
                int total=0;
                for(int i=1;i<=4;i++){
                    l.lock();
                    try{
                        total+=i;
                        if(i==3)throw new IllegalStateException("three");
                        out.append(l.getHoldCount());
                    }catch(IllegalStateException e){out.append('!');}
                    finally{l.unlock();}
                }
                out.append(',').append(total).append(',').append(state(l));
                return out.toString();
            }

        }
        """;

    @Test void lockState() throws Exception {
        matchesJvm(FIXTURE, "basics", "reentrancy", "tryLocking", "lockGuard");
    }

    @Test void interruptiblyAndFailures() throws Exception {
        matchesJvm(FIXTURE, "interruptibly", "unlockFailures", "exceptionTypes");
    }

    @Test void toStringAndOverrides() throws Exception {
        matchesJvm(FIXTURE, "toStrings", "callPattern", "throughInterface");
    }

    @Test void leavesOutConditionsAndQueues() {
        rejectsUnusedCall("java/util/concurrent/locks/ReentrantLock", "newCondition", "()Ljava/util/concurrent/locks/Condition;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/locks/Lock", "newCondition", "()Ljava/util/concurrent/locks/Condition;", INVOKEINTERFACE, true);
        rejectsUnusedCall("java/util/concurrent/locks/ReentrantLock", "tryLock", "(JLjava/util/concurrent/TimeUnit;)Z", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/locks/ReentrantLock", "getQueueLength", "()I", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/locks/ReentrantLock", "hasQueuedThreads", "()Z", INVOKEVIRTUAL, false);
    }
}
