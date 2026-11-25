package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T69: java.time.Period core subset (no TemporalAmount/ChronoPeriod). Expected values come from the JVM; see tasks/T69-period.md. */
@Tag("agent-pending")
class BytecodePeriodTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.time.Period;
        import java.time.format.DateTimeParseException;
        public class Fixture {
            interface Op{Object run();}
            static String show(Period p){return p+"="+p.getYears()+"/"+p.getMonths()+"/"+p.getDays();}
            /** ArithmeticException is compared by class only (the runtime's Math has no messages). */
            static String t(Op op){
                try{Object o=op.run();return o instanceof Period?show((Period)o):String.valueOf(o);}
                catch(DateTimeParseException e){
                    return "DateTimeParseException:"+e.getMessage()+"|"+e.getParsedString()+"|"+e.getErrorIndex()+"|"
                        +(e.getCause()==null?"-":e.getCause().getClass().getName());
                }
                catch(RuntimeException e){return e.getClass().getName();}
            }

            public static String factories(){
                StringBuilder out=new StringBuilder();
                Period[] all={Period.ZERO,Period.of(1,2,3),Period.of(-1,0,5),Period.of(0,-14,0),Period.ofYears(7),Period.ofMonths(-3),
                    Period.ofWeeks(3),Period.ofWeeks(-2),Period.ofDays(45),Period.of(Integer.MAX_VALUE,Integer.MIN_VALUE,Integer.MAX_VALUE),
                    Period.ofWeeks(Integer.MAX_VALUE/7)};
                for(Period p:all)out.append(show(p)).append(';');
                out.append(Period.of(0,0,0)==Period.ZERO).append(',').append(Period.ofYears(0)==Period.ZERO).append(',');
                out.append(Period.ofMonths(0)==Period.ZERO).append(',').append(Period.ofWeeks(0)==Period.ZERO).append(',');
                out.append(Period.ofDays(0)==Period.ZERO).append(',').append(Period.ofDays(2)==Period.ofDays(2)).append(';');
                out.append(t(()->Period.ofWeeks(Integer.MAX_VALUE/7+1))).append(',').append(t(()->Period.ofWeeks(Integer.MIN_VALUE)));
                return out.toString();
            }
            public static String accessors(){
                StringBuilder out=new StringBuilder();
                Period[] all={Period.ZERO,Period.ofDays(1),Period.ofDays(-1),Period.of(1,-1,0),Period.of(5,13,40),
                    Period.of(Integer.MAX_VALUE,Integer.MAX_VALUE,0),Period.of(Integer.MIN_VALUE,Integer.MIN_VALUE,9),Period.of(-1,11,0)};
                for(Period p:all){
                    out.append(p).append(':').append(p.isZero()).append('/').append(p.isNegative()).append('/').append(p.toTotalMonths()).append(';');
                }
                return out.toString();
            }
            public static String withAndPlus(){
                StringBuilder out=new StringBuilder();
                Period p=Period.of(1,2,3);
                out.append(p.withYears(1)==p).append(',').append(p.withMonths(2)==p).append(',').append(p.withDays(3)==p).append(',');
                out.append(show(p.withYears(-4))).append(',').append(show(p.withMonths(0))).append(',').append(show(p.withDays(30))).append(',');
                out.append(Period.ofYears(5).withYears(0)==Period.ZERO).append(',').append(Period.ofDays(5).withDays(0)==Period.ZERO).append(';');
                out.append(p.plusYears(0)==p).append(',').append(p.plusMonths(0)==p).append(',').append(p.plusDays(0)==p).append(',');
                out.append(p.minusYears(0)==p).append(',').append(p.minusMonths(0)==p).append(',').append(p.minusDays(0)==p).append(';');
                out.append(show(p.plusYears(-1))).append(',').append(show(p.plusMonths(10))).append(',').append(show(p.plusDays(-3))).append(',');
                out.append(show(p.minusYears(3))).append(',').append(show(p.minusMonths(-12))).append(',').append(show(p.minusDays(4))).append(',');
                out.append(p.minusYears(1).minusMonths(2).minusDays(3)==Period.ZERO).append(';');
                out.append(show(Period.ofMonths(-1).plusMonths(Integer.MAX_VALUE+1L))).append(',');
                out.append(show(Period.ofDays(1).minusDays(Integer.MAX_VALUE+2L))).append(',');
                out.append(show(Period.ofYears(Integer.MIN_VALUE).plusYears(4294967295L))).append(';');
                out.append(t(()->Period.ofYears(Integer.MAX_VALUE).plusYears(1))).append(',');
                out.append(t(()->Period.ZERO.plusMonths(Long.MAX_VALUE))).append(',');
                out.append(t(()->Period.ofDays(-1).plusDays(Long.MIN_VALUE))).append(',');
                out.append(t(()->Period.ofDays(1).plusDays(Long.MAX_VALUE))).append(',');
                out.append(t(()->Period.ofYears(Integer.MIN_VALUE).minusYears(1))).append(',');
                out.append(t(()->Period.ofYears(-1).minusYears(Long.MIN_VALUE))).append(',');
                out.append(t(()->Period.ZERO.minusMonths(Long.MIN_VALUE))).append(',');
                out.append(t(()->Period.ZERO.minusDays(Integer.MIN_VALUE)));
                return out.toString();
            }
            public static String multiplyNegateNormalize(){
                StringBuilder out=new StringBuilder();
                Period p=Period.of(1,-2,3);
                out.append(show(p.multipliedBy(3))).append(',').append(show(p.multipliedBy(-1))).append(',').append(p.multipliedBy(1)==p).append(',');
                out.append(p.multipliedBy(0)==Period.ZERO).append(',').append(Period.ZERO.multipliedBy(7)==Period.ZERO).append(',');
                out.append(show(p.negated())).append(',').append(p.negated().negated().equals(p)).append(',').append(Period.ZERO.negated()==Period.ZERO).append(';');
                out.append(t(()->Period.ofYears(Integer.MIN_VALUE).negated())).append(',');
                out.append(t(()->Period.ofDays(Integer.MIN_VALUE).negated())).append(',');
                out.append(t(()->Period.ofMonths(1<<16).multipliedBy(1<<16))).append(',');
                out.append(t(()->Period.of(1,1,Integer.MAX_VALUE).multipliedBy(2))).append(';');
                Period[] all={Period.of(1,15,3),Period.of(1,-25,0),Period.of(-1,13,0),Period.of(2,0,40),Period.of(0,-1,0),Period.ofMonths(24),
                    Period.of(-3,-12,-1),Period.of(0,12,0),Period.of(1,-12,7),Period.ofMonths(Integer.MIN_VALUE),Period.of(Integer.MIN_VALUE,11,0),
                    Period.of(1,-12,0)};
                for(Period q:all){
                    Period n=q.normalized();
                    out.append(show(n)).append(n==q?"=":"").append(n==Period.ZERO?"Z":"").append(' ');
                }
                out.append(t(()->Period.of(Integer.MAX_VALUE,12,0).normalized())).append(',');
                out.append(t(()->Period.of(Integer.MIN_VALUE,-12,0).normalized()));
                return out.toString();
            }
            public static String equality(){
                StringBuilder out=new StringBuilder();
                Period a=Period.of(1,2,3),b=Period.ofYears(1).plusMonths(2).plusDays(3),c=Period.of(1,2,4);
                out.append(a.equals(b)).append(',').append(a.equals(c)).append(',').append(a.equals(a)).append(',').append(a.equals(null)).append(',');
                out.append(a.equals("P1Y2M3D")).append(',').append(Period.ofDays(14).equals(Period.ofWeeks(2))).append(',');
                out.append(Period.ofMonths(12).equals(Period.ofYears(1))).append(';');
                Period[] all={Period.ZERO,a,c,Period.ofDays(-1),Period.ofMonths(-1),Period.ofYears(-1),Period.of(Integer.MIN_VALUE,Integer.MAX_VALUE,-7),
                    Period.of(0,1<<24,1<<16)};
                for(Period p:all)out.append(p.hashCode()).append(' ');
                Object o=a;
                out.append(o instanceof java.io.Serializable).append(',').append(o instanceof Comparable);
                return out.toString();
            }
            public static String parseValid(){
                StringBuilder out=new StringBuilder();
                String[] texts={"P1Y2M3D","P2W","P1Y2W3D","p1y","P1m","P-1Y+2M","-P1Y2M","-P-1Y","+P3D","P0D","P1W-7D","P2147483647Y",
                    "P-2147483648D","-P-2147483647M","P+0Y-0M","P12M","P1y2m3w4d","P0001D","-P2W1D","P306783378W","-P1W-2147483641D"};
                for(String s:texts)out.append(t(()->Period.parse(s))).append(';');
                out.append(t(()->Period.parse(new StringBuilder("P1Y1D")))).append(';');
                out.append(Period.parse("P0D")==Period.ZERO).append(',').append(Period.parse("-P0Y")==Period.ZERO).append(',');
                out.append(Period.parse("P1W-7D")==Period.ZERO);
                return out.toString();
            }
            public static String parseInvalid(){
                StringBuilder out=new StringBuilder();
                String[] texts={"","P","-P","+P","PT1D","P1D2Y","P1Y1Y","P1M1Y","P1D1W","P1H","P-Y","P+M","P-W","P1Y-D","P1.5D"," P1D",
                    "P1D ","1D","PP1D","P1","+-P1D","P1Y2","P--1D","P1YD","p1x","P1D-","P1Y2M3D4W","P-"};
                for(String s:texts)out.append(t(()->Period.parse(s))).append(';');
                String[] overflow={"P2147483648Y","P-2147483649M","P99999999999D","-P-2147483648Y","-P-2147483648D","P306783379W",
                    "P1W2147483641D","P-1W-2147483642D","-P1W-2147483648D"};
                for(String s:overflow)out.append(t(()->Period.parse(s))).append(';');
                out.append(t(()->Period.parse(null)));
                try{Period.parse(null);}catch(NullPointerException e){out.append(':').append(e.getMessage());}
                return out.toString();
            }
        }
        """;

    @Test void factoriesAndAccessors() throws Exception {
        matchesJvm(FIXTURE, "factories", "accessors");
    }

    @Test void arithmetic() throws Exception {
        matchesJvm(FIXTURE, "withAndPlus", "multiplyNegateNormalize");
    }

    @Test void equalityAndHash() throws Exception {
        matchesJvm(FIXTURE, "equality");
    }

    @Test void parsing() throws Exception {
        matchesJvm(FIXTURE, "parseValid", "parseInvalid");
    }

    @Test void leavesOutTemporalMembers() {
        rejectsUnusedCall("java/time/Period", "between", "(Ljava/time/LocalDate;Ljava/time/LocalDate;)Ljava/time/Period;", INVOKESTATIC, false);
        rejectsUnusedCall("java/time/Period", "from", "(Ljava/time/temporal/TemporalAmount;)Ljava/time/Period;", INVOKESTATIC, false);
        rejectsUnusedCall("java/time/Period", "get", "(Ljava/time/temporal/TemporalUnit;)J", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/Period", "getUnits", "()Ljava/util/List;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/Period", "plus", "(Ljava/time/temporal/TemporalAmount;)Ljava/time/Period;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/Period", "addTo", "(Ljava/time/temporal/Temporal;)Ljava/time/temporal/Temporal;", INVOKEVIRTUAL, false);
    }
}
