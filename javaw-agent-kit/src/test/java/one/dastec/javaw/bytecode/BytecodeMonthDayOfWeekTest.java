package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T80: java.time.Month and java.time.DayOfWeek (no TemporalAccessor/TemporalAdjuster). See tasks/T80-month-day-of-week.md. */
@Tag("agent-pending")
class BytecodeMonthDayOfWeekTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.time.DateTimeException;
        import java.time.DayOfWeek;
        import java.time.Month;
        public class Fixture {
            interface Op{Object run();}
            static String t(Op op){
                try{return String.valueOf(op.run());}
                catch(DateTimeException e){return e.getClass().getName()+":"+e.getMessage();}
                catch(RuntimeException e){return e.getClass().getName();}
            }

            public static String constants(){
                StringBuilder out=new StringBuilder();
                for(Month m:Month.values())out.append(m.name()).append('=').append(m.ordinal()).append('/').append(m.getValue()).append('/').append(m).append(' ');
                out.append(Month.values().length).append(';');
                for(DayOfWeek d:DayOfWeek.values())out.append(d.name()).append('=').append(d.ordinal()).append('/').append(d.getValue()).append('/').append(d).append(' ');
                out.append(DayOfWeek.values().length).append(';');
                out.append(Month.values()!=Month.values()).append(',').append(Month.JANUARY.compareTo(Month.MARCH)).append(',');
                out.append(DayOfWeek.SUNDAY.compareTo(DayOfWeek.MONDAY)).append(',');
                out.append(Month.MAY.getDeclaringClass().getName()).append(',').append(DayOfWeek.FRIDAY.getDeclaringClass().getName()).append(',');
                Object o=Month.JULY,p=DayOfWeek.TUESDAY;
                out.append(o instanceof Comparable).append(',').append(o instanceof java.io.Serializable).append(',');
                out.append(p instanceof Comparable).append(',').append(p instanceof java.io.Serializable).append(',');
                out.append(Month.JULY.equals(Month.JULY)).append(',').append(Month.JULY.equals(DayOfWeek.MONDAY)).append(',').append(Month.JULY.equals(null));
                return out.toString();
            }
            public static String ofAndValueOf(){
                StringBuilder out=new StringBuilder();
                for(int i=1;i<=12;i++)out.append(Month.of(i)==Month.values()[i-1]);
                out.append(';');
                for(int i=1;i<=7;i++)out.append(DayOfWeek.of(i)==DayOfWeek.values()[i-1]);
                out.append(';');
                int[] bad={0,13,-1,Integer.MAX_VALUE,Integer.MIN_VALUE};
                for(int v:bad)out.append(t(()->Month.of(v))).append(';');
                int[] badDay={0,8,-7,Integer.MAX_VALUE,Integer.MIN_VALUE};
                for(int v:badDay)out.append(t(()->DayOfWeek.of(v))).append(';');
                out.append(Month.valueOf("DECEMBER")==Month.DECEMBER).append(',').append(DayOfWeek.valueOf("SUNDAY")==DayOfWeek.SUNDAY).append(',');
                out.append(Enum.valueOf(Month.class,"MARCH").getValue()).append(',');
                out.append(t(()->Month.valueOf("December"))).append(',').append(t(()->DayOfWeek.valueOf("MON")));
                return out.toString();
            }
            public static String plusMinus(){
                StringBuilder out=new StringBuilder();
                long[] amounts={0,1,-1,11,12,13,-12,-13,25,-25,100,6,7,8,-7,-8,Integer.MAX_VALUE,Integer.MIN_VALUE,Long.MAX_VALUE,Long.MIN_VALUE};
                for(long a:amounts){
                    out.append(a).append(':').append(Month.JANUARY.plus(a)).append('/').append(Month.DECEMBER.plus(a)).append('/');
                    out.append(Month.MAY.minus(a)).append('/').append(DayOfWeek.MONDAY.plus(a)).append('/');
                    out.append(DayOfWeek.SUNDAY.plus(a)).append('/').append(DayOfWeek.THURSDAY.minus(a)).append(' ');
                }
                out.append(Month.APRIL.plus(0)==Month.APRIL).append(',').append(DayOfWeek.WEDNESDAY.minus(14)==DayOfWeek.WEDNESDAY);
                return out.toString();
            }
            public static String monthLengths(){
                StringBuilder out=new StringBuilder();
                for(Month m:Month.values()){
                    out.append(m.getValue()).append(':').append(m.length(false)).append('/').append(m.length(true)).append('/');
                    out.append(m.minLength()).append('/').append(m.maxLength()).append('/');
                    out.append(m.firstDayOfYear(false)).append('/').append(m.firstDayOfYear(true)).append('/');
                    out.append(m.firstMonthOfQuarter()).append(' ');
                }
                int sum=0,leapSum=0;
                for(Month m:Month.values()){sum+=m.length(false);leapSum+=m.length(true);}
                out.append(sum).append(',').append(leapSum).append(',').append(Month.DECEMBER.firstDayOfYear(true)+Month.DECEMBER.length(true)-1);
                return out.toString();
            }
            static String season(Month m){
                switch(m){
                    case DECEMBER: case JANUARY: case FEBRUARY: return "winter";
                    case MARCH: case APRIL: case MAY: return "spring";
                    case JUNE: case JULY: case AUGUST: return "summer";
                    default: return "autumn";
                }
            }
            public static String collections(){
                StringBuilder out=new StringBuilder();
                for(Month m:Month.values())out.append(season(m).charAt(0));
                out.append(';');
                for(DayOfWeek d:DayOfWeek.values()){
                    switch(d){
                        case SATURDAY: case SUNDAY: out.append('W'); break;
                        default: out.append(d.getValue());
                    }
                }
                out.append(';');
                java.util.List<Month> thirty=new java.util.ArrayList<>();
                for(Month m:Month.values())if(m.length(false)==30)thirty.add(m);
                out.append(thirty).append(',').append(thirty.contains(Month.of(9))).append(',').append(thirty.indexOf(Month.valueOf("NOVEMBER")));
                return out.toString();
            }
        }
        """;

    @Test void constantsAndOf() throws Exception {
        matchesJvm(FIXTURE, "constants", "ofAndValueOf");
    }

    @Test void arithmetic() throws Exception {
        matchesJvm(FIXTURE, "plusMinus", "monthLengths");
    }

    @Test void enumSwitch() throws Exception {
        matchesJvm(FIXTURE, "collections");
    }

    @Test void leavesOutTemporalMembers() {
        rejectsUnusedCall("java/time/Month", "from", "(Ljava/time/temporal/TemporalAccessor;)Ljava/time/Month;", INVOKESTATIC, false);
        rejectsUnusedCall("java/time/DayOfWeek", "from", "(Ljava/time/temporal/TemporalAccessor;)Ljava/time/DayOfWeek;", INVOKESTATIC, false);
        rejectsUnusedCall("java/time/Month", "getDisplayName", "(Ljava/time/format/TextStyle;Ljava/util/Locale;)Ljava/lang/String;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/DayOfWeek", "getDisplayName", "(Ljava/time/format/TextStyle;Ljava/util/Locale;)Ljava/lang/String;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/Month", "range", "(Ljava/time/temporal/TemporalField;)Ljava/time/temporal/ValueRange;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/DayOfWeek", "getLong", "(Ljava/time/temporal/TemporalField;)J", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/Month", "adjustInto", "(Ljava/time/temporal/Temporal;)Ljava/time/temporal/Temporal;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/DayOfWeek", "query", "(Ljava/time/temporal/TemporalQuery;)Ljava/lang/Object;", INVOKEVIRTUAL, false);
    }
}
