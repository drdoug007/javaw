package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T67: JDK messages for Throwable.initCause and Math exact arithmetic. See tasks/T67-jdk-exception-messages.md. */
@Tag("agent-pending")
class BytecodeJdkExceptionMessagesTest extends KitDifferential {
    private static final String FIXTURE = """
        class AppException extends Exception {
            AppException(String m){super(m);}
            AppException(String m,Throwable c){super(m,c);}
            public String toString(){return "App["+getMessage()+"]";}
        }
        interface Op { long run(); }
        public class Fixture {
            static String fail(Throwable x,Throwable owner){
                return x.getClass().getSimpleName()+":"+x.getMessage()+":"+(x.getCause()==owner?"owner":String.valueOf(x.getCause()));
            }
            static String calc(Op op){
                try{return "="+op.run();}
                catch(ArithmeticException e){return e.getClass().getSimpleName()+":"+e.getMessage()+":"+e.getCause();}
            }
            public static String initCause(){
                StringBuilder out=new StringBuilder();
                Throwable t=new Throwable("t");
                t.initCause(new Exception("c"));
                try{t.initCause(new Error("again"));}catch(IllegalStateException x){out.append(fail(x,t));}
                out.append('|');
                try{t.initCause(null);}catch(IllegalStateException x){out.append(fail(x,t));}
                out.append('|');
                Throwable n=new RuntimeException("n");
                n.initCause(null);
                try{n.initCause(new AppException("app"));}catch(IllegalStateException x){out.append(fail(x,n));}
                out.append('|');
                Exception ctor=new Exception("ctor",new Error("e"));
                try{ctor.initCause(new IllegalArgumentException());}catch(IllegalStateException x){out.append(fail(x,ctor));}
                out.append('|');
                AppException nullCtor=new AppException("nc",null);
                try{nullCtor.initCause(new Exception());}catch(IllegalStateException x){out.append(fail(x,nullCtor));}
                out.append('|');
                Throwable self=new Throwable("self");
                try{self.initCause(self);}catch(IllegalArgumentException x){out.append(fail(x,self));}
                out.append(',').append(self.getCause());
                self.initCause(new Exception("late"));
                out.append(',').append(self.getCause().getMessage()).append('|');
                Throwable selfAfter=new Throwable("sa");selfAfter.initCause(null);
                try{selfAfter.initCause(selfAfter);}catch(RuntimeException x){out.append(fail(x,selfAfter));}
                return out.toString();
            }
            public static String intExact(){
                int max=Integer.MAX_VALUE,min=Integer.MIN_VALUE;
                return String.join(",",
                    calc(()->Math.addExact(max,1)),calc(()->Math.addExact(3,4)),calc(()->Math.subtractExact(min,1)),
                    calc(()->Math.subtractExact(10,4)),calc(()->Math.multiplyExact(65536,65536)),calc(()->Math.multiplyExact(-6,7)),
                    calc(()->Math.incrementExact(max)),calc(()->Math.incrementExact(5)),calc(()->Math.decrementExact(min)),
                    calc(()->Math.decrementExact(5)),calc(()->Math.negateExact(min)),calc(()->Math.negateExact(9)),
                    calc(()->Math.toIntExact(1L<<31)),calc(()->Math.toIntExact(-123L)),calc(()->Math.absExact(min)),
                    calc(()->Math.absExact(-8)));
            }
            public static String longExact(){
                long max=Long.MAX_VALUE,min=Long.MIN_VALUE;
                return String.join(",",
                    calc(()->Math.addExact(max,1L)),calc(()->Math.addExact(3L,4L)),calc(()->Math.subtractExact(min,1L)),
                    calc(()->Math.subtractExact(10L,4L)),calc(()->Math.multiplyExact(1L<<32,1L<<32)),calc(()->Math.multiplyExact(-6L,7L)),
                    calc(()->Math.multiplyExact(min,-1)),calc(()->Math.multiplyExact(5L,-3)),
                    calc(()->Math.incrementExact(max)),calc(()->Math.incrementExact(5L)),calc(()->Math.decrementExact(min)),
                    calc(()->Math.decrementExact(5L)),calc(()->Math.negateExact(min)),calc(()->Math.negateExact(9L)),
                    calc(()->Math.absExact(min)),calc(()->Math.absExact(-8L)));
            }
            public static String strictExact(){
                return String.join(",",
                    calc(()->StrictMath.addExact(Integer.MAX_VALUE,1)),calc(()->StrictMath.addExact(Long.MAX_VALUE,1L)),
                    calc(()->StrictMath.multiplyExact(Integer.MIN_VALUE,-1)),calc(()->StrictMath.multiplyExact(Long.MIN_VALUE,-1L)),
                    calc(()->StrictMath.toIntExact(Long.MAX_VALUE)),calc(()->StrictMath.negateExact(Long.MIN_VALUE)),
                    calc(()->StrictMath.absExact(Integer.MIN_VALUE)),calc(()->StrictMath.absExact(Long.MIN_VALUE)),
                    calc(()->StrictMath.subtractExact(2,3)));
            }
            public static String division(){
                return String.join(",",
                    calc(()->Math.floorDiv(7,0)),calc(()->Math.floorDiv(7L,0L)),calc(()->Math.floorDiv(7L,0)),
                    calc(()->Math.floorMod(7,0)),calc(()->Math.floorMod(7L,0L)),calc(()->Math.floorMod(7L,0)),
                    calc(()->StrictMath.floorDiv(1,0)),calc(()->StrictMath.floorMod(1L,0L)),
                    calc(()->Math.floorDiv(-7,2)),calc(()->Math.floorMod(-7,2)),calc(()->Math.floorDiv(Integer.MIN_VALUE,-1)),
                    calc(()->Math.floorDiv(Long.MIN_VALUE,-1L)),calc(()->Math.floorMod(-7L,3)));
            }
        }
        """;

    @Test void initCauseMessages() throws Exception {
        matchesJvm(FIXTURE, "initCause");
    }

    @Test void exactArithmeticMessages() throws Exception {
        matchesJvm(FIXTURE, "intExact", "longExact", "strictExact");
    }

    @Test void divisionByZeroMessages() throws Exception {
        matchesJvm(FIXTURE, "division");
    }

    @Test void leavesOutNewerExactMethods() {
        rejectsUnusedCall("java/lang/Math", "divideExact", "(II)I", INVOKESTATIC, false);
        rejectsUnusedCall("java/lang/Math", "ceilDiv", "(JJ)J", INVOKESTATIC, false);
    }
}
