package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T60: java.time.temporal.TemporalUnit, ChronoUnit and the unit-based Duration/TimeUnit members. See tasks/T60-chrono-unit.md. */
class BytecodeChronoUnitTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.time.DateTimeException;
        import java.time.Duration;
        import java.time.temporal.ChronoUnit;
        import java.time.temporal.TemporalUnit;
        import java.time.temporal.UnsupportedTemporalTypeException;
        import java.util.concurrent.TimeUnit;
        public class Fixture {
            static String name(Throwable t){return t.getClass().getName();}
            static String fail(Throwable t){return name(t)+":"+t.getMessage();}
            static String show(Duration d){return d+"="+d.getSeconds()+"/"+d.getNano();}
            interface Op{Duration run();}
            static String tryIt(Op op){
                try{return show(op.run());}catch(ArithmeticException e){return name(e);}catch(RuntimeException e){return fail(e);}
            }
            static final long[] AMOUNTS={0L,1L,-1L,7L,-25L,1001L,-1_000_000_001L,1L<<40,Long.MAX_VALUE,Long.MIN_VALUE,
                Long.MAX_VALUE/86400,Long.MAX_VALUE/86400+1,Long.MIN_VALUE/3600-1};

            public static String constants(){
                StringBuilder out=new StringBuilder();
                ChronoUnit[] all=ChronoUnit.values();
                out.append(all.length).append(';');
                for(ChronoUnit u:all)out.append(u.ordinal()).append(':').append(u.name()).append('=').append(u).append(',');
                out.append(ChronoUnit.values()!=all).append(',');
                out.append(ChronoUnit.SECONDS.compareTo(ChronoUnit.DAYS)).append(',');
                out.append(ChronoUnit.FOREVER.getDeclaringClass().getName()).append(',');
                TemporalUnit t=ChronoUnit.MILLIS;
                out.append(t).append(',').append(t instanceof ChronoUnit).append(',').append(t==ChronoUnit.MILLIS).append(',');
                out.append((Object)ChronoUnit.WEEKS instanceof Comparable);
                return out.toString();
            }
            public static String properties(){
                StringBuilder out=new StringBuilder();
                for(ChronoUnit u:ChronoUnit.values()){
                    TemporalUnit t=u;
                    out.append(u).append(':').append(show(u.getDuration())).append(',').append(u.isDurationEstimated())
                       .append(u.isDateBased()).append(u.isTimeBased()).append(',').append(show(t.getDuration()))
                       .append(t.isDurationEstimated()).append(t.isDateBased()).append(t.isTimeBased()).append(t.toString()).append('|');
                }
                out.append(ChronoUnit.HOURS.getDuration()==ChronoUnit.HOURS.getDuration());
                return out.toString();
            }
            static String kind(ChronoUnit u){
                switch(u){
                    case NANOS: case MICROS: case MILLIS: return "sub";
                    case SECONDS: case MINUTES: case HOURS: case HALF_DAYS: return "time";
                    case FOREVER: return "inf";
                    default: return "date";
                }
            }
            public static String switching(){
                StringBuilder out=new StringBuilder();
                for(ChronoUnit u:ChronoUnit.values())out.append(kind(u)).append(',');
                return out.toString();
            }
            public static String durationOf(){
                StringBuilder out=new StringBuilder();
                for(ChronoUnit u:ChronoUnit.values()){
                    out.append(u).append(':');
                    for(long a:AMOUNTS)out.append(tryIt(()->Duration.of(a,u))).append(',');
                    out.append('|');
                }
                return out.toString();
            }
            public static String plusAndMinus(){
                StringBuilder out=new StringBuilder();
                Duration base=Duration.ofSeconds(-3,250_000_000);
                for(ChronoUnit u:ChronoUnit.values()){
                    out.append(u).append(':');
                    for(long a:AMOUNTS){
                        out.append(tryIt(()->base.plus(a,u))).append('/').append(tryIt(()->base.minus(a,u))).append(',');
                    }
                    out.append('|');
                }
                out.append(base.plus(0,ChronoUnit.HOURS)==base).append(base.minus(0,ChronoUnit.NANOS)==base);
                out.append(Duration.of(0,ChronoUnit.SECONDS)==Duration.ZERO).append(Duration.of(0,ChronoUnit.DAYS)==Duration.ZERO);
                out.append(Duration.ZERO.plus(0,ChronoUnit.DAYS)==Duration.ZERO);
                return out.toString();
            }
            public static String unitErrors(){
                StringBuilder out=new StringBuilder();
                try{Duration.of(1,null);out.append("no");}catch(NullPointerException e){out.append(fail(e));}
                out.append(',');
                try{Duration.ZERO.plus(0,null);out.append("no");}catch(NullPointerException e){out.append(fail(e));}
                out.append(',');
                try{Duration.ZERO.minus(5,null);out.append("no");}catch(NullPointerException e){out.append(fail(e));}
                out.append(',');
                try{Duration.of(0,ChronoUnit.MONTHS);out.append("no");}catch(UnsupportedTemporalTypeException e){
                    out.append(fail(e)).append(e instanceof DateTimeException).append(e.getCause());}
                out.append(',');
                try{Duration.ZERO.minus(Long.MIN_VALUE,ChronoUnit.FOREVER);out.append("no");}catch(DateTimeException e){out.append(fail(e));}
                out.append(',');
                UnsupportedTemporalTypeException x=new UnsupportedTemporalTypeException("m",new ArithmeticException("c"));
                out.append(fail(x)).append(',').append(x.getCause().getMessage()).append(',');
                out.append(new UnsupportedTemporalTypeException(null).getMessage());
                return out.toString();
            }
            public static String timeUnitBridge(){
                StringBuilder out=new StringBuilder();
                for(TimeUnit t:TimeUnit.values()){
                    ChronoUnit c=t.toChronoUnit();
                    out.append(t).append("->").append(c).append("->").append(TimeUnit.of(c)).append(',');
                }
                for(ChronoUnit c:ChronoUnit.values()){
                    try{out.append(TimeUnit.of(c));}catch(IllegalArgumentException e){out.append(fail(e));}
                    out.append(',');
                }
                try{TimeUnit.of(null);out.append("no");}catch(NullPointerException e){out.append(fail(e));}
                return out.toString();
            }
            public static String asLibrariesUseIt(){
                StringBuilder out=new StringBuilder();
                TimeUnit unit=TimeUnit.MILLISECONDS;
                out.append(show(Duration.of(1500,unit.toChronoUnit()))).append(',');
                out.append(show(Duration.of(30,ChronoUnit.SECONDS).plus(90,ChronoUnit.MINUTES))).append(',');
                out.append(TimeUnit.of(ChronoUnit.MICROS).toNanos(3)).append(',');
                ChronoUnit[] spring={ChronoUnit.NANOS,ChronoUnit.MICROS,ChronoUnit.MILLIS,ChronoUnit.SECONDS,ChronoUnit.MINUTES,ChronoUnit.HOURS,ChronoUnit.DAYS};
                for(ChronoUnit u:spring)out.append(u.name().toLowerCase()).append('=').append(Duration.of(2,u).toMillis()).append(';');
                return out.toString();
            }
        }
        """;

    @Test void constantsAndProperties() throws Exception {
        matchesJvm(FIXTURE, "constants", "properties", "switching");
    }

    @Test void durationWithUnits() throws Exception {
        matchesJvm(FIXTURE, "durationOf", "plusAndMinus", "unitErrors");
    }

    @Test void timeUnitConversions() throws Exception {
        matchesJvm(FIXTURE, "timeUnitBridge", "asLibrariesUseIt");
    }

    @Test void leavesOutTemporalMembers() {
        rejectsUnusedCall("java/time/temporal/TemporalUnit", "addTo", "(Ljava/time/temporal/Temporal;J)Ljava/time/temporal/Temporal;", INVOKEINTERFACE, true);
        rejectsUnusedCall("java/time/temporal/ChronoUnit", "addTo", "(Ljava/time/temporal/Temporal;J)Ljava/time/temporal/Temporal;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/temporal/ChronoUnit", "between", "(Ljava/time/temporal/Temporal;Ljava/time/temporal/Temporal;)J", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/temporal/ChronoUnit", "isSupportedBy", "(Ljava/time/temporal/Temporal;)Z", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/Duration", "get", "(Ljava/time/temporal/TemporalUnit;)J", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/Duration", "truncatedTo", "(Ljava/time/temporal/TemporalUnit;)Ljava/time/Duration;", INVOKEVIRTUAL, false);
    }
}
