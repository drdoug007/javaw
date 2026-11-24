package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T52: java.util.concurrent.TimeUnit (conversions only). See tasks/T52-time-unit.md. */
class BytecodeTimeUnitTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.concurrent.TimeUnit;
        public class Fixture {
            static final long MAX=Long.MAX_VALUE, MIN=Long.MIN_VALUE;
            static final long[] SAMPLES={0L,1L,-1L,999L,1000L,-1001L,59L,61L,3599L,86399L,86401L,-86401L,
                1L<<40,-(1L<<40),MAX/1000,MAX/1000+1,MIN/1000,MIN/1000-1,MAX/60,MAX/86400,MIN/86400-1,MAX,MIN,MAX-1,MIN+1};
            static String name(Throwable t){return t.getClass().getName();}

            public static String constants(){
                StringBuilder out=new StringBuilder();
                TimeUnit[] all=TimeUnit.values();
                out.append(all.length).append(';');
                for(TimeUnit u:all)out.append(u.ordinal()).append(':').append(u.name()).append('=').append(u).append(',');
                out.append(TimeUnit.values()!=all).append(',');
                out.append(TimeUnit.SECONDS.compareTo(TimeUnit.MINUTES)).append(',');
                out.append(TimeUnit.DAYS.compareTo(TimeUnit.NANOSECONDS)).append(',');
                out.append(TimeUnit.MILLISECONDS.equals(TimeUnit.MILLISECONDS)).append(',');
                out.append(TimeUnit.MILLISECONDS.equals(TimeUnit.MICROSECONDS)).append(',');
                out.append(TimeUnit.MINUTES.getDeclaringClass().getName()).append(',');
                out.append((Object)TimeUnit.SECONDS instanceof Enum).append(',');
                out.append((Object)TimeUnit.SECONDS instanceof Comparable);
                return out.toString();
            }
            static String kind(TimeUnit u){
                switch(u){
                    case NANOSECONDS: case MICROSECONDS: case MILLISECONDS: return "sub";
                    case SECONDS: return "s";
                    default: return "big";
                }
            }
            public static String switching(){
                StringBuilder out=new StringBuilder();
                for(TimeUnit u:TimeUnit.values())out.append(kind(u)).append(',');
                return out.toString();
            }
            public static String toMethods(){
                StringBuilder out=new StringBuilder();
                for(TimeUnit u:TimeUnit.values()){
                    out.append(u).append(':');
                    for(long d:SAMPLES){
                        out.append(u.toNanos(d)).append('/').append(u.toMicros(d)).append('/').append(u.toMillis(d)).append('/')
                           .append(u.toSeconds(d)).append('/').append(u.toMinutes(d)).append('/').append(u.toHours(d)).append('/')
                           .append(u.toDays(d)).append(' ');
                    }
                    out.append('|');
                }
                return out.toString();
            }
            public static String convertAll(){
                StringBuilder out=new StringBuilder();
                for(TimeUnit to:TimeUnit.values())for(TimeUnit from:TimeUnit.values()){
                    out.append(to.ordinal()).append(from.ordinal()).append(':');
                    for(long d:SAMPLES)out.append(to.convert(d,from)).append(',');
                    out.append('|');
                }
                return out.toString();
            }
            public static String saturation(){
                StringBuilder out=new StringBuilder();
                out.append(TimeUnit.DAYS.toNanos(106751)).append(',').append(TimeUnit.DAYS.toNanos(106752)).append(',');
                out.append(TimeUnit.DAYS.toNanos(-106752)).append(',').append(TimeUnit.HOURS.toMillis(MAX/3600000)).append(',');
                out.append(TimeUnit.HOURS.toMillis(MAX/3600000+1)).append(',').append(TimeUnit.SECONDS.toNanos(MIN/1000000000)).append(',');
                out.append(TimeUnit.SECONDS.toNanos(MIN/1000000000-1)).append(',').append(TimeUnit.MINUTES.toSeconds(MAX/60+1)).append(',');
                out.append(TimeUnit.NANOSECONDS.convert(MAX,TimeUnit.DAYS)).append(',').append(TimeUnit.NANOSECONDS.convert(MIN,TimeUnit.MICROSECONDS)).append(',');
                out.append(TimeUnit.DAYS.convert(MAX,TimeUnit.NANOSECONDS)).append(',').append(TimeUnit.DAYS.convert(MIN,TimeUnit.NANOSECONDS)).append(',');
                out.append(TimeUnit.MILLISECONDS.toMicros(MAX/1000)).append(',').append(TimeUnit.MILLISECONDS.toMicros(MAX/1000+1));
                return out.toString();
            }
            public static String truncation(){
                StringBuilder out=new StringBuilder();
                out.append(TimeUnit.NANOSECONDS.toMicros(-1999)).append(',').append(TimeUnit.MILLISECONDS.toSeconds(-1999)).append(',');
                out.append(TimeUnit.SECONDS.toMinutes(-119)).append(',').append(TimeUnit.MINUTES.toHours(-61)).append(',');
                out.append(TimeUnit.HOURS.toDays(47)).append(',').append(TimeUnit.HOURS.toDays(-47)).append(',');
                out.append(TimeUnit.MICROSECONDS.convert(1500,TimeUnit.NANOSECONDS)).append(',');
                out.append(TimeUnit.MINUTES.convert(-90,TimeUnit.SECONDS)).append(',');
                out.append(TimeUnit.DAYS.convert(-90000,TimeUnit.SECONDS));
                return out.toString();
            }
            public static String nullUnit(){
                StringBuilder out=new StringBuilder();
                try{TimeUnit.SECONDS.convert(1L,(TimeUnit)null);out.append("no");}catch(NullPointerException e){out.append(name(e));}
                return out.toString();
            }
            public static String asLibrariesUseIt(){
                StringBuilder out=new StringBuilder();
                long timeout=TimeUnit.SECONDS.toMillis(30);
                long start=TimeUnit.MILLISECONDS.toNanos(1500);
                long elapsed=TimeUnit.NANOSECONDS.convert(250,TimeUnit.MILLISECONDS);
                out.append(timeout).append(',').append(start).append(',').append(elapsed).append(',');
                out.append(TimeUnit.NANOSECONDS.toSeconds(start+elapsed)).append(',');
                out.append(TimeUnit.MINUTES.toMillis(30)).append(',').append(TimeUnit.DAYS.toSeconds(1)).append(',');
                out.append(TimeUnit.MILLISECONDS.toMicros(7)).append(',').append(TimeUnit.MILLISECONDS.toDays(864000000L));
                return out.toString();
            }
        }
        """;

    @Test void constantsAndEnumBehaviour() throws Exception {
        matchesJvm(FIXTURE, "constants", "switching");
    }

    @Test void toConversions() throws Exception {
        matchesJvm(FIXTURE, "toMethods", "truncation", "asLibrariesUseIt");
    }

    @Test void convertAndSaturation() throws Exception {
        matchesJvm(FIXTURE, "convertAll", "saturation", "nullUnit");
    }

    @Test void leavesOutSleepingAndJavaTime() {
        rejectsUnusedCall("java/util/concurrent/TimeUnit", "sleep", "(J)V", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/TimeUnit", "timedWait", "(Ljava/lang/Object;J)V", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/TimeUnit", "timedJoin", "(Ljava/lang/Thread;J)V", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/TimeUnit", "convert", "(Ljava/time/Duration;)J", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/TimeUnit", "toChronoUnit", "()Ljava/time/temporal/ChronoUnit;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/TimeUnit", "of", "(Ljava/time/temporal/ChronoUnit;)Ljava/util/concurrent/TimeUnit;", INVOKESTATIC, false);
    }
}
