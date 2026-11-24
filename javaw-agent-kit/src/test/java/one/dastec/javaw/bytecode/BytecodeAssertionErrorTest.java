package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T58: java.lang.AssertionError constructors. See tasks/T58-assertion-error.md. */
@Tag("agent-pending")
class BytecodeAssertionErrorTest extends KitDifferential {
    private static final String FIXTURE = """
        class Detail {
            final StringBuilder log;
            Detail(StringBuilder log){this.log=log;}
            public String toString(){log.append("toString;");return "detail!";}
        }
        class Probe extends AssertionError {
            Probe(Object detail){super(detail);}
            public Throwable initCause(Throwable cause){Fixture.LOG.append("initCause;");return super.initCause(cause);}
        }
        public class Fixture {
            static final StringBuilder LOG=new StringBuilder();
            static String show(Throwable t){
                Throwable c=t.getCause();
                return t.getMessage()+"|"+(c==null?"-":c.getClass().getName()+":"+c.getMessage())+"|"+t;
            }

            public static String noDetail(){
                AssertionError e=new AssertionError();
                StringBuilder out=new StringBuilder(show(e)).append(';');
                out.append((Object)e instanceof Error).append(',').append((Object)e instanceof Throwable).append(',');
                out.append((Object)e instanceof Exception).append(',').append(e.getClass().getName()).append(';');
                e.initCause(new IllegalStateException("late"));
                out.append(show(e));
                return out.toString();
            }
            public static String objectDetail(){
                StringBuilder out=new StringBuilder();
                out.append(show(new AssertionError((Object)"text"))).append(';');
                out.append(show(new AssertionError((Object)null))).append(';');
                out.append(show(new AssertionError(Integer.valueOf(42)))).append(';');
                StringBuilder log=new StringBuilder();
                AssertionError e=new AssertionError(new Detail(log));
                out.append(log).append(show(e)).append(';');
                out.append(show(new AssertionError(new StringBuilder("sb"))));
                return out.toString();
            }
            public static String throwableDetail(){
                StringBuilder out=new StringBuilder();
                IllegalArgumentException cause=new IllegalArgumentException("bad");
                AssertionError e=new AssertionError(cause);
                out.append(show(e)).append(';').append(e.getCause()==cause).append(';');
                try{e.initCause(new RuntimeException());out.append("reset");}
                catch(IllegalStateException x){out.append("ISE");}
                out.append(';');
                out.append(show(new AssertionError(new NullPointerException()))).append(';');
                out.append(show(new AssertionError((Object)new AssertionError("inner"))));
                return out.toString();
            }
            public static String callPattern(){
                StringBuilder out=new StringBuilder();
                AssertionError e=new AssertionError((Object)"x");
                e.initCause(new RuntimeException("later"));
                out.append(show(e)).append(';');
                LOG.setLength(0);
                new Probe("plain");
                out.append(LOG).append('|');
                LOG.setLength(0);
                Probe q=new Probe(new RuntimeException("r"));
                out.append(LOG).append('|').append(show(q));
                return out.toString();
            }
            public static String primitives(){
                StringBuilder out=new StringBuilder();
                out.append(show(new AssertionError(true))).append(';');
                out.append(show(new AssertionError(false))).append(';');
                out.append(show(new AssertionError('x'))).append(';');
                out.append(show(new AssertionError(-7))).append(';');
                out.append(show(new AssertionError(Integer.MIN_VALUE))).append(';');
                out.append(show(new AssertionError(1234567890123L))).append(';');
                out.append(show(new AssertionError(Long.MIN_VALUE))).append(';');
                out.append(show(new AssertionError(1.5f))).append(';');
                out.append(show(new AssertionError(-0.0f))).append(';');
                out.append(show(new AssertionError(0.25))).append(';');
                out.append(show(new AssertionError(Double.NaN))).append(';');
                out.append(show(new AssertionError(100.0)));
                return out.toString();
            }
            public static String messageAndCause(){
                StringBuilder out=new StringBuilder();
                RuntimeException cause=new RuntimeException("root");
                AssertionError e=new AssertionError("msg",cause);
                out.append(show(e)).append(';').append(e.getCause()==cause).append(';');
                out.append(show(new AssertionError("only",null))).append(';');
                out.append(show(new AssertionError(null,cause))).append(';');
                AssertionError none=new AssertionError("n",null);
                try{none.initCause(cause);out.append("reset");}
                catch(IllegalStateException x){out.append("ISE");}
                return out.toString();
            }
            public static String thrownAndCaught(){
                StringBuilder out=new StringBuilder();
                try{throw new AssertionError(5);}
                catch(Error e){out.append("Error:").append(show(e)).append(';');}
                try{
                    try{throw new AssertionError("in",new IllegalStateException("s"));}
                    catch(RuntimeException wrong){out.append("wrong;");}
                }catch(AssertionError e){out.append("AE:").append(e.getMessage()).append(';');}
                try{throw new AssertionError((Object)new UnsupportedOperationException("u"));}
                catch(Throwable t){out.append(t.getCause().getMessage());}
                return out.toString();
            }
        }
        """;

    @Test void constructors() throws Exception {
        matchesJvm(FIXTURE, "noDetail", "objectDetail", "throwableDetail", "callPattern");
    }

    @Test void primitiveAndMessageConstructors() throws Exception {
        matchesJvm(FIXTURE, "primitives", "messageAndCause", "thrownAndCaught");
    }

    @Test void leavesOutAssertionStatus() {
        rejectsUnusedCall("java/lang/Class", "desiredAssertionStatus", "()Z", INVOKEVIRTUAL, false);
    }
}
