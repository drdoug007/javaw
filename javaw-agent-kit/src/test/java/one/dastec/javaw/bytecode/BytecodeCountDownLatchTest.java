package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T54: java.util.concurrent.CountDownLatch (single-threaded). Expected values come from the JVM; see tasks/T54-count-down-latch.md. */
@Tag("agent-pending")
class BytecodeCountDownLatchTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.concurrent.CountDownLatch;
        class Spy extends CountDownLatch {
            final StringBuilder log=new StringBuilder();
            Spy(int n){super(n);}
            public void countDown(){log.append("countDown;");super.countDown();}
            public long getCount(){log.append("getCount;");return super.getCount();}
            public void await() throws InterruptedException{log.append("await;");super.await();}
            public int hashCode(){log.append("hash;");return 0x2a;}
        }
        class Fixed extends CountDownLatch {
            Fixed(int n){super(n);}
            public int hashCode(){return 0xbeef;}
        }
        public class Fixture {
            static String t(Runnable r){
                try{r.run();return "ok";}
                catch(RuntimeException e){return e.getClass().getName()+":"+e.getMessage();}
            }
            static String tail(CountDownLatch l){
                String s=l.toString();
                boolean prefix=s.startsWith(l.getClass().getName()+"@"+Integer.toHexString(l.hashCode())+"[");
                return prefix+s.substring(s.indexOf('['));
            }

            public static String constructors(){
                StringBuilder out=new StringBuilder();
                out.append(new CountDownLatch(0).getCount()).append(',');
                out.append(new CountDownLatch(3).getCount()).append(',');
                out.append(new CountDownLatch(Integer.MAX_VALUE).getCount()).append(';');
                out.append(t(()->new CountDownLatch(-1))).append(';');
                out.append(t(()->new CountDownLatch(Integer.MIN_VALUE))).append(';');
                try{new CountDownLatch(-5);}catch(IllegalArgumentException e){out.append(e.getCause()).append(',').append((Object)e instanceof RuntimeException);}
                return out.toString();
            }
            public static String countingDown(){
                CountDownLatch l=new CountDownLatch(3);StringBuilder out=new StringBuilder();
                for(int i=0;i<5;i++){l.countDown();out.append(l.getCount()).append(',');}
                CountDownLatch z=new CountDownLatch(0);z.countDown();z.countDown();
                out.append(z.getCount()).append(';');
                CountDownLatch big=new CountDownLatch(Integer.MAX_VALUE);big.countDown();
                long c=big.getCount();
                out.append(c).append(',').append(c+1L>Integer.MAX_VALUE);
                return out.toString();
            }
            public static String awaiting(){
                StringBuilder out=new StringBuilder();
                try{
                    CountDownLatch z=new CountDownLatch(0);
                    z.await();z.await();out.append("zero:").append(z.getCount()).append(';');
                    CountDownLatch l=new CountDownLatch(2);
                    l.countDown();l.countDown();l.await();out.append("done:").append(l.getCount()).append(';');
                    l.countDown();l.await();out.append("again:").append(l.getCount());
                }catch(InterruptedException e){out.append("interrupted");}
                return out.toString();
            }
            public static String toStrings(){
                StringBuilder out=new StringBuilder();
                CountDownLatch l=new CountDownLatch(2);
                out.append(tail(l)).append(';');
                l.countDown();out.append(tail(l)).append(';');
                l.countDown();l.countDown();out.append(tail(l)).append(';');
                out.append(new Fixed(7)).append(';');
                out.append(tail(new CountDownLatch(Integer.MAX_VALUE)));
                return out.toString();
            }
            public static String subclassCalls(){
                Spy s=new Spy(2);StringBuilder out=new StringBuilder();
                out.append(s.log).append('|');
                s.countDown();out.append(s.log).append('|');s.log.setLength(0);
                String text=s.toString();out.append(text).append(' ').append(s.log).append('|');s.log.setLength(0);
                s.countDown();s.log.setLength(0);
                try{s.await();}catch(InterruptedException e){out.append("int");}
                out.append(s.log).append('|');s.log.setLength(0);
                out.append(s.getCount()).append(' ').append(s.log).append('|');s.log.setLength(0);
                CountDownLatch asBase=s;asBase.countDown();
                out.append(asBase.getCount()).append(' ').append(s.log).append('|');
                out.append((Object)s instanceof java.io.Serializable).append(',').append(s.equals(s)).append(',').append(s.equals(new Spy(0)));
                return out.toString();
            }
        }
        """;

    @Test void constructors() throws Exception {
        matchesJvm(FIXTURE, "constructors");
    }

    @Test void countDownAndAwait() throws Exception {
        matchesJvm(FIXTURE, "countingDown", "awaiting");
    }

    @Test void toStringAndSubclasses() throws Exception {
        matchesJvm(FIXTURE, "toStrings", "subclassCalls");
    }

    @Test void leavesOutTimedAwait() {
        rejectsUnusedCall("java/util/concurrent/CountDownLatch", "await", "(JLjava/util/concurrent/TimeUnit;)Z", INVOKEVIRTUAL, false);
    }
}
