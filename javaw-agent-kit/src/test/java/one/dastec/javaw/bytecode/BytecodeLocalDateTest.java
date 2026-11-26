package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T81: java.time.LocalDate core (marker ChronoLocalDate, no Temporal, parse or format). Expected values come from the JVM; see tasks/T81-local-date.md. */
class BytecodeLocalDateTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.time.DateTimeException;
        import java.time.DayOfWeek;
        import java.time.LocalDate;
        import java.time.Month;
        import java.time.temporal.ChronoUnit;
        public class Fixture {
            interface Op{Object run();}
            static String show(LocalDate d){return d+"="+d.getYear()+"/"+d.getMonthValue()+"/"+d.getDayOfMonth();}
            /** DateTimeException messages are compared; ArithmeticException by class only. */
            static String t(Op op){
                try{Object o=op.run();return o instanceof LocalDate?show((LocalDate)o):String.valueOf(o);}
                catch(DateTimeException e){return e.getClass().getName()+":"+e.getMessage();}
                catch(RuntimeException e){return e.getClass().getName();}
            }

            public static String factories(){
                StringBuilder out=new StringBuilder();
                out.append(show(LocalDate.MIN)).append(';').append(show(LocalDate.MAX)).append(';').append(show(LocalDate.EPOCH)).append(';');
                out.append(show(LocalDate.of(2024,2,29))).append(';').append(show(LocalDate.of(2023,Month.DECEMBER,31))).append(';');
                out.append(show(LocalDate.of(-1,1,1))).append(';').append(show(LocalDate.of(0,12,31))).append(';');
                out.append(show(LocalDate.of(999,6,7))).append(';').append(show(LocalDate.of(-1000,3,4))).append(';');
                out.append(show(LocalDate.of(-10000,1,1))).append(';').append(show(LocalDate.of(10000,1,1))).append(';');
                out.append(show(LocalDate.of(9999,12,31))).append(';').append(show(LocalDate.of(-9999,1,1))).append(';');
                out.append(LocalDate.of(2023,Month.APRIL,30).equals(LocalDate.of(2023,4,30))).append(';');
                int[][] bad={{2023,2,30},{2023,2,29},{1900,2,29},{2023,13,1},{2023,0,1},{2023,4,31},{2023,4,0},{2023,1,32},
                    {1000000000,1,1},{-1000000000,1,1},{2023,11,31},{2023,6,31},{2023,9,31},{-1,2,29},{2023,2,-1}};
                for(int[] b:bad)out.append(t(()->LocalDate.of(b[0],b[1],b[2]))).append(';');
                out.append(t(()->LocalDate.of(2024,(Month)null,1)));
                return out.toString();
            }
            public static String yearDayAndEpochDay(){
                StringBuilder out=new StringBuilder();
                int[][] yd={{2023,1},{2023,31},{2023,32},{2023,59},{2023,60},{2024,60},{2024,366},{2023,365},{2000,335},{-4,366},{-1,200}};
                for(int[] v:yd)out.append(t(()->LocalDate.ofYearDay(v[0],v[1]))).append(';');
                int[][] badYd={{2023,366},{1900,366},{2023,0},{2024,367},{1000000000,1}};
                for(int[] v:badYd)out.append(t(()->LocalDate.ofYearDay(v[0],v[1]))).append(';');
                long[] days={0,1,-1,59,60,365,-365,-719528,-719529,-719469,10957,19782,-25567,146097,-146097,2932896,
                    365241780471L,-365243219162L,-678941,-12345678901L,98765432101L};
                for(long d:days){
                    LocalDate x=LocalDate.ofEpochDay(d);
                    out.append(show(x)).append('@').append(x.toEpochDay()).append(';');
                }
                out.append(t(()->LocalDate.ofEpochDay(365241780472L))).append(';').append(t(()->LocalDate.ofEpochDay(-365243219163L))).append(';');
                out.append(t(()->LocalDate.ofEpochDay(Long.MAX_VALUE))).append(';').append(t(()->LocalDate.ofEpochDay(Long.MIN_VALUE))).append(';');
                out.append(LocalDate.MIN.toEpochDay()).append(',').append(LocalDate.MAX.toEpochDay()).append(',').append(LocalDate.EPOCH.toEpochDay());
                boolean round=true;
                for(long d=-800000;d<=800000;d+=997){if(LocalDate.ofEpochDay(d).toEpochDay()!=d)round=false;}
                out.append(',').append(round);
                return out.toString();
            }
            public static String fields(){
                StringBuilder out=new StringBuilder();
                LocalDate[] all={LocalDate.of(2023,1,1),LocalDate.of(2024,2,29),LocalDate.of(2024,12,31),LocalDate.of(2023,12,31),
                    LocalDate.of(1900,3,1),LocalDate.of(2000,3,1),LocalDate.of(-1,7,4),LocalDate.of(-4,12,31),LocalDate.of(1970,1,1),
                    LocalDate.MIN,LocalDate.MAX,LocalDate.of(2023,4,15),LocalDate.of(2023,9,30),LocalDate.of(1582,10,15)};
                for(LocalDate d:all){
                    out.append(d).append(':').append(d.getMonth()).append('/').append(d.getDayOfYear()).append('/').append(d.getDayOfWeek())
                        .append('/').append(d.isLeapYear()).append('/').append(d.lengthOfMonth()).append('/').append(d.lengthOfYear()).append(';');
                }
                out.append(LocalDate.of(2024,3,10).getMonth()==Month.MARCH).append(',').append(LocalDate.of(2024,3,10).getDayOfWeek()==DayOfWeek.SUNDAY);
                return out.toString();
            }
            public static String plusMinus(){
                StringBuilder out=new StringBuilder();
                LocalDate d=LocalDate.of(2024,1,31);
                out.append(d.plusDays(0)==d).append(',').append(d.plusWeeks(0)==d).append(',').append(d.plusMonths(0)==d).append(',')
                    .append(d.plusYears(0)==d).append(',').append(d.minusDays(0)==d).append(',').append(d.minusMonths(0)==d).append(';');
                out.append(show(d.plusDays(1))).append(',').append(show(d.plusDays(29))).append(',').append(show(d.plusDays(30))).append(',')
                    .append(show(d.plusDays(366))).append(',').append(show(d.minusDays(31))).append(',').append(show(d.plusDays(-800000))).append(';');
                out.append(show(d.plusWeeks(1))).append(',').append(show(d.minusWeeks(5))).append(',').append(show(d.plusWeeks(-52))).append(';');
                out.append(show(d.plusMonths(1))).append(',').append(show(d.plusMonths(2))).append(',').append(show(d.plusMonths(13))).append(',')
                    .append(show(d.minusMonths(2))).append(',').append(show(d.minusMonths(11))).append(',').append(show(d.plusMonths(-25))).append(',')
                    .append(show(LocalDate.of(2023,3,31).minusMonths(1))).append(',').append(show(LocalDate.of(-1,1,15).plusMonths(-1))).append(';');
                LocalDate leap=LocalDate.of(2024,2,29);
                out.append(show(leap.plusYears(1))).append(',').append(show(leap.plusYears(4))).append(',').append(show(leap.minusYears(100))).append(',')
                    .append(show(leap.minusYears(2025))).append(',').append(show(leap.plusYears(7976))).append(';');
                out.append(show(d.plus(3,ChronoUnit.DAYS))).append(',').append(show(d.plus(2,ChronoUnit.WEEKS))).append(',')
                    .append(show(d.plus(1,ChronoUnit.MONTHS))).append(',').append(show(leap.plus(1,ChronoUnit.YEARS))).append(',')
                    .append(show(d.plus(2,ChronoUnit.DECADES))).append(',').append(show(d.minus(3,ChronoUnit.CENTURIES))).append(',')
                    .append(show(d.minus(1,ChronoUnit.MILLENNIA))).append(',').append(show(d.minus(10,ChronoUnit.DAYS))).append(',')
                    .append(show(d.minus(1,ChronoUnit.MONTHS))).append(';');
                out.append(t(()->d.plus(1,ChronoUnit.HOURS))).append(',').append(t(()->d.minus(1,ChronoUnit.NANOS))).append(',')
                    .append(t(()->d.plus(1,ChronoUnit.FOREVER))).append(',').append(t(()->d.plus(1,ChronoUnit.HALF_DAYS))).append(';');
                return out.toString();
            }
            public static String outOfRange(){
                StringBuilder out=new StringBuilder();
                LocalDate d=LocalDate.of(2024,1,31);
                out.append(t(()->LocalDate.MAX.plusDays(1))).append(';').append(t(()->LocalDate.MIN.minusDays(1))).append(';');
                out.append(t(()->LocalDate.MAX.plusMonths(1))).append(';').append(t(()->LocalDate.MIN.minusMonths(1))).append(';');
                out.append(t(()->LocalDate.MAX.plusYears(1))).append(';').append(t(()->LocalDate.MIN.minusYears(1))).append(';');
                out.append(t(()->LocalDate.MAX.plusWeeks(1))).append(';');
                out.append(t(()->d.plusDays(Long.MAX_VALUE))).append(';').append(t(()->d.plusDays(Long.MIN_VALUE))).append(';');
                out.append(t(()->d.minusDays(Long.MIN_VALUE))).append(';').append(t(()->d.plusWeeks(Long.MAX_VALUE))).append(';');
                out.append(t(()->d.plusWeeks(Long.MAX_VALUE/7))).append(';').append(t(()->d.plusMonths(Long.MAX_VALUE))).append(';');
                out.append(t(()->d.minusMonths(Long.MIN_VALUE))).append(';').append(t(()->d.plusYears(Long.MAX_VALUE))).append(';');
                out.append(t(()->d.minusYears(Long.MIN_VALUE))).append(';').append(t(()->d.plusYears(Integer.MAX_VALUE))).append(';');
                out.append(t(()->d.plus(Long.MAX_VALUE,ChronoUnit.CENTURIES))).append(';').append(t(()->d.plus(Long.MAX_VALUE,ChronoUnit.MILLENNIA))).append(';');
                out.append(t(()->d.plus(2000000,ChronoUnit.MILLENNIA))).append(';');
                out.append(show(LocalDate.MAX.minusDays(LocalDate.MAX.toEpochDay()-LocalDate.MIN.toEpochDay()))).append(';');
                out.append(show(LocalDate.MIN.plusMonths(1999999998L*12+11))).append(';').append(show(LocalDate.of(999999999,12,1).plusDays(30)));
                return out.toString();
            }
            public static String withers(){
                StringBuilder out=new StringBuilder();
                LocalDate leap=LocalDate.of(2024,2,29),d=LocalDate.of(2023,3,31);
                out.append(leap.withYear(2024)==leap).append(',').append(d.withMonth(3)==d).append(',').append(d.withDayOfMonth(31)==d).append(',')
                    .append(d.withDayOfYear(90)==d).append(';');
                out.append(show(leap.withYear(2023))).append(',').append(show(leap.withYear(2028))).append(',').append(show(leap.withYear(-1))).append(',')
                    .append(show(d.withMonth(2))).append(',').append(show(d.withMonth(4))).append(',').append(show(d.withMonth(12))).append(',')
                    .append(show(d.withDayOfMonth(1))).append(',').append(show(d.withDayOfYear(1))).append(',').append(show(d.withDayOfYear(365))).append(',')
                    .append(show(leap.withDayOfYear(366))).append(';');
                out.append(t(()->d.withYear(1000000000))).append(';').append(t(()->d.withMonth(13))).append(';').append(t(()->d.withMonth(0))).append(';')
                    .append(t(()->d.withDayOfMonth(32))).append(';').append(t(()->LocalDate.of(2023,4,1).withDayOfMonth(31))).append(';')
                    .append(t(()->LocalDate.of(2023,2,1).withDayOfMonth(29))).append(';').append(t(()->d.withDayOfYear(366))).append(';')
                    .append(t(()->d.withDayOfYear(0)));
                return out.toString();
            }
            public static String comparison(){
                StringBuilder out=new StringBuilder();
                LocalDate[] all={LocalDate.of(2020,1,1),LocalDate.of(2023,5,1),LocalDate.of(2020,1,9),LocalDate.of(2020,12,1),
                    LocalDate.of(-5,1,1),LocalDate.MIN,LocalDate.MAX,LocalDate.of(2020,1,1)};
                for(LocalDate a:all){
                    for(LocalDate b:all){
                        out.append(a.compareTo(b)).append(a.isBefore(b)?"<":"").append(a.isAfter(b)?">":"").append(a.isEqual(b)?"=":"")
                            .append(a.equals(b)?"e":"").append(' ');
                    }
                    out.append(';');
                }
                LocalDate a=LocalDate.of(2020,1,1);
                out.append(a.equals(null)).append(',').append(a.equals("2020-01-01")).append(',').append(a.equals(a)).append(';');
                LocalDate[] hashes={a,LocalDate.EPOCH,LocalDate.MIN,LocalDate.MAX,LocalDate.of(-1,12,31),LocalDate.of(0,1,1),LocalDate.of(2047,2,3)};
                for(LocalDate h:hashes)out.append(h.hashCode()).append(' ');
                Object o=a;
                out.append(o instanceof java.io.Serializable).append(',').append(o instanceof Comparable);
                LocalDate[] sorted={LocalDate.of(2020,3,1),LocalDate.MIN,LocalDate.of(2020,1,31),LocalDate.of(-3,5,5),LocalDate.MAX};
                java.util.Arrays.sort(sorted);
                out.append(',').append(java.util.Arrays.toString(sorted));
                return out.toString();
            }
        }
        """;

    @Test void factoriesAndEpochDay() throws Exception {
        matchesJvm(FIXTURE, "factories", "yearDayAndEpochDay");
    }

    @Test void fieldAccess() throws Exception {
        matchesJvm(FIXTURE, "fields");
    }

    @Test void arithmetic() throws Exception {
        matchesJvm(FIXTURE, "plusMinus", "outOfRange");
    }

    @Test void withersAndComparison() throws Exception {
        matchesJvm(FIXTURE, "withers", "comparison");
    }

    @Test void leavesOutTemporalMembers() {
        rejectsUnusedCall("java/time/LocalDate", "now", "()Ljava/time/LocalDate;", INVOKESTATIC, false);
        rejectsUnusedCall("java/time/LocalDate", "parse", "(Ljava/lang/CharSequence;)Ljava/time/LocalDate;", INVOKESTATIC, false);
        rejectsUnusedCall("java/time/LocalDate", "from", "(Ljava/time/temporal/TemporalAccessor;)Ljava/time/LocalDate;", INVOKESTATIC, false);
        rejectsUnusedCall("java/time/LocalDate", "atStartOfDay", "()Ljava/time/LocalDateTime;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/chrono/ChronoLocalDate", "isBefore", "(Ljava/time/chrono/ChronoLocalDate;)Z", INVOKEINTERFACE, true);
        rejectsUnusedCall("java/time/chrono/ChronoLocalDate", "toEpochDay", "()J", INVOKEINTERFACE, true);
        rejectsUnusedCall("java/time/LocalDate", "until", "(Ljava/time/chrono/ChronoLocalDate;)Ljava/time/Period;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/LocalDate", "format", "(Ljava/time/format/DateTimeFormatter;)Ljava/lang/String;", INVOKEVIRTUAL, false);
    }
}
