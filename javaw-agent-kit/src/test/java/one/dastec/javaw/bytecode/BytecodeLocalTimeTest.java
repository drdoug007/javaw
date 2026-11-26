package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T82: java.time.LocalTime core subset (no Temporal/TemporalAccessor). Expected values come from the JVM; see tasks/T82-local-time.md. */
@Tag("agent-pending")
class BytecodeLocalTimeTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.time.DateTimeException;
        import java.time.LocalTime;
        import java.time.format.DateTimeParseException;
        public class Fixture {
            interface Op{Object run();}
            static String show(LocalTime t){return t+"="+t.getHour()+"/"+t.getMinute()+"/"+t.getSecond()+"/"+t.getNano();}
            static String t(Op op){
                try{Object o=op.run();return o instanceof LocalTime?show((LocalTime)o):String.valueOf(o);}
                catch(DateTimeParseException e){
                    return "DTPE@"+e.getErrorIndex()+":"+e.getMessage()+"|"+e.getParsedString()+"|"
                        +(e.getCause()==null?"-":e.getCause().getClass().getName()+":"+e.getCause().getMessage());
                }
                catch(DateTimeException e){return "DTE:"+e.getMessage();}
                catch(RuntimeException e){return e.getClass().getName()+":"+e.getMessage();}
            }

            public static String constants(){
                StringBuilder out=new StringBuilder();
                LocalTime[] all={LocalTime.MIN,LocalTime.MAX,LocalTime.MIDNIGHT,LocalTime.NOON};
                for(LocalTime x:all)out.append(show(x)).append(';');
                out.append(LocalTime.MIN==LocalTime.MIDNIGHT).append(',').append(LocalTime.of(0,0)==LocalTime.MIDNIGHT).append(',');
                out.append(LocalTime.of(12,0)==LocalTime.NOON).append(',').append(LocalTime.of(12,0,0)==LocalTime.NOON).append(',');
                out.append(LocalTime.of(12,0,0,0)==LocalTime.NOON).append(',').append(LocalTime.of(7,0)==LocalTime.of(7,0,0,0)).append(',');
                out.append(LocalTime.ofSecondOfDay(3600)==LocalTime.of(1,0)).append(',').append(LocalTime.ofNanoOfDay(0)==LocalTime.MIDNIGHT).append(',');
                out.append(LocalTime.of(7,1)==LocalTime.of(7,1)).append(',').append(LocalTime.MAX.toNanoOfDay()).append(',').append(LocalTime.MAX.toSecondOfDay());
                return out.toString();
            }
            public static String factories(){
                StringBuilder out=new StringBuilder();
                LocalTime[] all={LocalTime.of(9,5),LocalTime.of(23,59),LocalTime.of(1,2,3),LocalTime.of(0,0,59),LocalTime.of(10,15,30,1),
                    LocalTime.of(23,59,59,999999999),LocalTime.ofSecondOfDay(0),LocalTime.ofSecondOfDay(86399),LocalTime.ofSecondOfDay(45296),
                    LocalTime.ofNanoOfDay(1),LocalTime.ofNanoOfDay(86399999999999L),LocalTime.ofNanoOfDay(45296789000000L),LocalTime.ofNanoOfDay(3600000000000L)};
                for(LocalTime x:all)out.append(show(x)).append(':').append(x.toSecondOfDay()).append(':').append(x.toNanoOfDay()).append(';');
                return out.toString();
            }
            public static String invalidValues(){
                StringBuilder out=new StringBuilder();
                Op[] ops={()->LocalTime.of(24,0),()->LocalTime.of(-1,0),()->LocalTime.of(0,60),()->LocalTime.of(0,-1),()->LocalTime.of(0,0,60),
                    ()->LocalTime.of(0,0,-5),()->LocalTime.of(0,0,0,1000000000),()->LocalTime.of(0,0,0,-1),()->LocalTime.of(24,60,60,-1),
                    ()->LocalTime.of(1,60,60,-1),()->LocalTime.of(1,2,60,-1),()->LocalTime.ofSecondOfDay(86400),()->LocalTime.ofSecondOfDay(-1),
                    ()->LocalTime.ofNanoOfDay(-1),()->LocalTime.ofNanoOfDay(86400000000000L),()->LocalTime.ofNanoOfDay(Long.MIN_VALUE),
                    ()->LocalTime.ofSecondOfDay(Long.MAX_VALUE),()->LocalTime.NOON.withHour(24),()->LocalTime.NOON.withMinute(60),
                    ()->LocalTime.NOON.withSecond(-1),()->LocalTime.NOON.withNano(-1),()->LocalTime.NOON.withNano(1000000000)};
                for(Op op:ops)out.append(t(op)).append(';');
                Object o=new DateTimeException("x");
                out.append(o instanceof RuntimeException);
                return out.toString();
            }
            public static String withers(){
                StringBuilder out=new StringBuilder();
                LocalTime x=LocalTime.of(10,15,30,123);
                out.append(x.withHour(10)==x).append(',').append(x.withMinute(15)==x).append(',').append(x.withSecond(30)==x).append(',').append(x.withNano(123)==x).append(';');
                out.append(show(x.withHour(0))).append(',').append(show(x.withMinute(59))).append(',').append(show(x.withSecond(0))).append(',').append(show(x.withNano(999999999))).append(';');
                out.append(LocalTime.of(1,2).withHour(12)==LocalTime.NOON).append(',').append(LocalTime.of(12,0,0,5).withNano(0)==LocalTime.NOON).append(',');
                out.append(LocalTime.of(12,7).withMinute(0)==LocalTime.NOON).append(',').append(LocalTime.of(12,0,9).withSecond(0)==LocalTime.NOON);
                return out.toString();
            }
            public static String plusMinus(){
                StringBuilder out=new StringBuilder();
                LocalTime x=LocalTime.of(10,15,30,500);
                out.append(show(x.plusHours(5))).append(',').append(show(x.plusHours(14))).append(',').append(show(x.plusHours(-11))).append(',');
                out.append(show(x.plusHours(Long.MAX_VALUE))).append(',').append(show(x.plusHours(Long.MIN_VALUE))).append(';');
                out.append(show(x.plusMinutes(45))).append(',').append(show(x.plusMinutes(-616))).append(',').append(show(x.plusMinutes(1000000))).append(',');
                out.append(show(x.plusMinutes(Long.MIN_VALUE))).append(';');
                out.append(show(x.plusSeconds(30))).append(',').append(show(x.plusSeconds(-36931))).append(',').append(show(x.plusSeconds(Long.MAX_VALUE))).append(';');
                out.append(show(x.plusNanos(999999500))).append(',').append(show(x.plusNanos(-501))).append(',').append(show(x.plusNanos(Long.MIN_VALUE))).append(',');
                out.append(show(x.plusNanos(Long.MAX_VALUE))).append(';');
                out.append(show(x.minusHours(11))).append(',').append(show(x.minusHours(Long.MIN_VALUE))).append(',').append(show(x.minusMinutes(616))).append(',');
                out.append(show(x.minusMinutes(Long.MIN_VALUE))).append(',').append(show(x.minusSeconds(36931))).append(',').append(show(x.minusSeconds(Long.MIN_VALUE))).append(',');
                out.append(show(x.minusNanos(501))).append(',').append(show(x.minusNanos(Long.MIN_VALUE))).append(',').append(show(x.minusNanos(Long.MAX_VALUE))).append(';');
                out.append(x.plusHours(0)==x).append(',').append(x.plusHours(48)==x).append(',').append(x.plusMinutes(1440)==x).append(',');
                out.append(x.plusSeconds(-86400)==x).append(',').append(x.plusNanos(86400000000000L)==x).append(',').append(x.minusHours(24)==x).append(',');
                out.append(x.minusMinutes(0)==x).append(',').append(x.minusSeconds(0)==x).append(',').append(x.minusNanos(0)==x).append(';');
                out.append(LocalTime.MAX.plusNanos(1)==LocalTime.MIDNIGHT).append(',').append(LocalTime.MIDNIGHT.minusNanos(1).equals(LocalTime.MAX)).append(',');
                out.append(LocalTime.of(11,0).plusHours(1)==LocalTime.NOON).append(',').append(LocalTime.of(11,59).plusMinutes(1)==LocalTime.NOON).append(',');
                out.append(LocalTime.of(11,59,59).plusSeconds(1)==LocalTime.NOON);
                return out.toString();
            }
            public static String comparison(){
                StringBuilder out=new StringBuilder();
                LocalTime[] all={LocalTime.MIDNIGHT,LocalTime.of(0,0,0,1),LocalTime.of(0,0,1),LocalTime.of(0,1),LocalTime.of(1,0),LocalTime.of(10,15,30,5),
                    LocalTime.of(10,15,30,6),LocalTime.NOON,LocalTime.MAX};
                for(LocalTime a:all){
                    for(LocalTime b:all){
                        int c=a.compareTo(b);
                        out.append(c<0?'<':c>0?'>':'=').append(a.isBefore(b)?'b':'-').append(a.isAfter(b)?'a':'-').append(a.equals(b)?'e':'-');
                    }
                    out.append(' ').append(a.hashCode()).append(';');
                }
                LocalTime x=LocalTime.of(10,15,30,5);
                out.append(x.equals(LocalTime.ofNanoOfDay(x.toNanoOfDay()))).append(',').append(x.equals(null)).append(',').append(x.equals("10:15:30.000000005")).append(',');
                out.append(x.hashCode()==LocalTime.of(10,15,30,5).hashCode()).append(',').append(LocalTime.of(5,30,0,1000).compareTo(LocalTime.of(5,30))).append(',');
                Object o=x;
                out.append(o instanceof Comparable).append(',').append(o instanceof java.io.Serializable);
                return out.toString();
            }
            public static String strings(){
                StringBuilder out=new StringBuilder();
                LocalTime[] all={LocalTime.MIDNIGHT,LocalTime.of(9,5),LocalTime.of(23,59,1),LocalTime.of(10,0,0,1),LocalTime.of(10,0,0,1000),
                    LocalTime.of(10,0,0,1000000),LocalTime.of(10,0,0,100000000),LocalTime.of(10,0,0,120000000),LocalTime.of(10,0,0,123400000),
                    LocalTime.of(10,0,0,123450000),LocalTime.of(10,0,0,123456000),LocalTime.of(10,0,0,123456700),LocalTime.of(10,0,0,10),
                    LocalTime.of(1,2,3,4005000),LocalTime.MAX,LocalTime.of(0,0,10),LocalTime.of(0,10,0,500000000)};
                for(LocalTime x:all)out.append(x).append(' ');
                return out.toString();
            }
            public static String parseValid(){
                StringBuilder out=new StringBuilder();
                String[] texts={"10:15","10:15:30","00:00","23:59:59.999999999","10:15:30.","10:15:30.1","10:15:30.12","10:15:30.123","10:15:30.1234",
                    "10:15:30.123456789","10:15:30.000000001","00:00:00.000000000","12:00:00","07:08:09.0100"};
                for(String s:texts)out.append(t(()->LocalTime.parse(s))).append(';');
                out.append(t(()->LocalTime.parse(new StringBuilder("01:02:03")))).append(';');
                out.append(LocalTime.parse("05:00")==LocalTime.of(5,0)).append(',').append(LocalTime.parse("12:00:00.0")==LocalTime.NOON).append(',');
                LocalTime[] round={LocalTime.MAX,LocalTime.of(1,2,3,4000),LocalTime.of(4,5),LocalTime.of(6,7,8,900000000),LocalTime.of(0,0,0,7)};
                for(LocalTime x:round)out.append(LocalTime.parse(x.toString()).equals(x));
                return out.toString();
            }
            public static String parseSyntax(){
                StringBuilder out=new StringBuilder();
                String[] texts={"","1","10","10:","1:15","10:1","10-15","10:1x","x0:15","1x:15"," 10:15","\\uFF110:15","10:15:","10:15:3","10:15:3x",
                    "10:15 ","10:15:30Z","10:15:30,5","10:15.5","10:15:30.x","10:15:30.1234567891","10:15:30.5 ",
                    "10:15:30.12345678901234567890123456789012345678901234567890123456789012345"};
                for(String s:texts)out.append(t(()->LocalTime.parse(s))).append(';');
                out.append(t(()->LocalTime.parse(null)));
                return out.toString();
            }
            public static String parseValues(){
                StringBuilder out=new StringBuilder();
                String[] texts={"24:00","24:00:00","25:00","99:00","23:60","24:60","99:99:99","23:59:60","24:00:60","23:61:60","00:00:99.5",
                    "23:60x","24:00Z"};
                for(String s:texts)out.append(t(()->LocalTime.parse(s))).append(';');
                return out.toString();
            }
        }
        """;

    @Test void factoriesAndValidation() throws Exception {
        matchesJvm(FIXTURE, "constants", "factories", "invalidValues");
    }

    @Test void arithmetic() throws Exception {
        matchesJvm(FIXTURE, "withers", "plusMinus");
    }

    @Test void comparisonAndStrings() throws Exception {
        matchesJvm(FIXTURE, "comparison", "strings");
    }

    @Test void parsing() throws Exception {
        matchesJvm(FIXTURE, "parseValid", "parseSyntax", "parseValues");
    }

    @Test void leavesOutTemporalMembers() {
        rejectsUnusedCall("java/time/LocalTime", "now", "()Ljava/time/LocalTime;", INVOKESTATIC, false);
        rejectsUnusedCall("java/time/LocalTime", "from", "(Ljava/time/temporal/TemporalAccessor;)Ljava/time/LocalTime;", INVOKESTATIC, false);
        rejectsUnusedCall("java/time/LocalTime", "atDate", "(Ljava/time/LocalDate;)Ljava/time/LocalDateTime;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/LocalTime", "format", "(Ljava/time/format/DateTimeFormatter;)Ljava/lang/String;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/LocalTime", "parse", "(Ljava/lang/CharSequence;Ljava/time/format/DateTimeFormatter;)Ljava/time/LocalTime;", INVOKESTATIC, false);
        rejectsUnusedCall("java/time/LocalTime", "with", "(Ljava/time/temporal/TemporalField;J)Ljava/time/LocalTime;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/LocalTime", "truncatedTo", "(Ljava/time/temporal/TemporalUnit;)Ljava/time/LocalTime;", INVOKEVIRTUAL, false);
    }
}
