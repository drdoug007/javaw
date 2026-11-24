package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T61: java.time.Instant core subset (no Temporal/TemporalAccessor). Expected values come from the JVM; see tasks/T61-instant.md. */
class BytecodeInstantTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.Serializable;
        import java.time.DateTimeException;
        import java.time.Instant;
        public class Fixture {
            static final long MIN_SECOND=-31557014167219200L, MAX_SECOND=31556889864403199L;
            /** Year 0000-01-01T00:00:00Z and 10000 Gregorian years in seconds. */
            static final long Y0=-62167219200L, K=146097L*25*86400;
            interface Op{Object run();}
            /** DateTimeException messages are compared; ArithmeticException only by class (the runtime's Math has no messages). */
            static String t(Op op){
                try{return String.valueOf(op.run());}
                catch(DateTimeException e){return e.getClass().getSimpleName()+":"+e.getMessage();}
                catch(RuntimeException e){return e.getClass().getSimpleName();}
            }
            static String show(Instant i){return i.getEpochSecond()+"/"+i.getNano();}
            static String s(Op op){
                try{Object o=op.run();return o instanceof Instant?show((Instant)o):String.valueOf(o);}
                catch(DateTimeException e){return e.getClass().getSimpleName()+":"+e.getMessage();}
                catch(RuntimeException e){return e.getClass().getSimpleName();}
            }

            public static String factories(){
                StringBuilder o=new StringBuilder();
                Instant[] all={Instant.EPOCH,Instant.MIN,Instant.MAX,Instant.ofEpochSecond(5),Instant.ofEpochSecond(-5),
                    Instant.ofEpochSecond(5,-1),Instant.ofEpochSecond(-1,1_500_000_000L),Instant.ofEpochSecond(3,-4_000_000_001L),
                    Instant.ofEpochSecond(0,Long.MAX_VALUE),Instant.ofEpochSecond(0,Long.MIN_VALUE),Instant.ofEpochMilli(-1),
                    Instant.ofEpochMilli(1234),Instant.ofEpochMilli(-1001),Instant.ofEpochMilli(-1000),Instant.ofEpochMilli(Long.MIN_VALUE),
                    Instant.ofEpochMilli(Long.MAX_VALUE),Instant.ofEpochSecond(MIN_SECOND),Instant.ofEpochSecond(MAX_SECOND,999_999_999)};
                for(Instant i:all)o.append(show(i)).append(';');
                o.append(Instant.ofEpochSecond(0)==Instant.EPOCH).append(',').append(Instant.ofEpochMilli(0)==Instant.EPOCH).append(',');
                o.append(Instant.ofEpochSecond(1,-1_000_000_000)==Instant.EPOCH).append(',').append(Instant.ofEpochSecond(0,0)==Instant.EPOCH).append(',');
                o.append(Instant.ofEpochSecond(7)==Instant.ofEpochSecond(7)).append(',').append(Instant.ofEpochSecond(MIN_SECOND).equals(Instant.MIN)).append(',');
                o.append(Instant.ofEpochSecond(MAX_SECOND,999_999_999).equals(Instant.MAX)).append(',').append(Instant.MIN.getNano());
                Object w=Instant.EPOCH;
                o.append('|').append(w instanceof Comparable).append(',').append(w instanceof Serializable);
                o.append(',').append(new DateTimeException("x") instanceof RuntimeException);
                return o.toString();
            }
            public static String rangeErrors(){
                StringBuilder o=new StringBuilder();
                Op[] ops={()->Instant.ofEpochSecond(MAX_SECOND+1),()->Instant.ofEpochSecond(MIN_SECOND-1),
                    ()->Instant.ofEpochSecond(MAX_SECOND,1_000_000_000L),()->Instant.ofEpochSecond(MIN_SECOND,-1),
                    ()->Instant.ofEpochSecond(Long.MAX_VALUE),()->Instant.ofEpochSecond(Long.MIN_VALUE),
                    ()->Instant.ofEpochSecond(Long.MAX_VALUE,Long.MAX_VALUE),()->Instant.ofEpochSecond(Long.MIN_VALUE,-1_000_000_000L),
                    ()->Instant.MIN.minusNanos(1),()->Instant.MAX.plusNanos(1),()->Instant.MAX.plusSeconds(Long.MAX_VALUE),
                    ()->Instant.MIN.minusSeconds(Long.MAX_VALUE),()->Instant.EPOCH.plusSeconds(Long.MAX_VALUE),
                    ()->Instant.EPOCH.minusSeconds(Long.MIN_VALUE),()->Instant.EPOCH.minusMillis(Long.MIN_VALUE),
                    ()->Instant.EPOCH.minusNanos(Long.MIN_VALUE),()->Instant.MAX.plusMillis(1),()->Instant.MIN.plusMillis(-1)};
                for(Op op:ops)o.append(s(op)).append(';');
                return o.toString();
            }
            public static String epochMillis(){
                StringBuilder o=new StringBuilder();
                Op[] ops={()->Instant.EPOCH.toEpochMilli(),()->Instant.ofEpochSecond(1,999_999_999).toEpochMilli(),
                    ()->Instant.ofEpochSecond(-1,1).toEpochMilli(),()->Instant.ofEpochSecond(-1,999_999_999).toEpochMilli(),
                    ()->Instant.ofEpochSecond(-1,1_000_000).toEpochMilli(),()->Instant.ofEpochSecond(-2,500_000_000).toEpochMilli(),
                    ()->Instant.ofEpochSecond(-1).toEpochMilli(),()->Instant.ofEpochMilli(-1001).toEpochMilli(),
                    ()->Instant.ofEpochMilli(Long.MIN_VALUE).toEpochMilli(),()->Instant.ofEpochMilli(Long.MAX_VALUE).toEpochMilli(),
                    ()->Instant.ofEpochMilli(Long.MAX_VALUE).plusNanos(999_999).toEpochMilli(),
                    ()->Instant.ofEpochMilli(Long.MAX_VALUE).plusNanos(1_000_000).toEpochMilli(),
                    ()->Instant.ofEpochMilli(Long.MIN_VALUE).minusNanos(1).toEpochMilli(),
                    ()->Instant.ofEpochSecond(Long.MIN_VALUE/1000-1).toEpochMilli(),()->Instant.MAX.toEpochMilli(),
                    ()->Instant.MIN.toEpochMilli(),()->Instant.ofEpochSecond(1322907330).toEpochMilli()};
                for(Op op:ops)o.append(t(op)).append(';');
                long sum=0;
                for(long m=-5_000_000_000L;m<=5_000_000_000L;m+=7_777_777)sum=sum*31+Instant.ofEpochMilli(m).plusNanos(123_456).toEpochMilli();
                return o.append(sum).toString();
            }
            public static String arithmetic(){
                StringBuilder o=new StringBuilder();
                Instant i=Instant.ofEpochSecond(10,500_000_000);
                Op[] ops={()->i.plusSeconds(-11),()->i.plusMillis(-500),()->i.plusMillis(1501),()->i.plusMillis(-10_501),
                    ()->i.plusNanos(-500_000_001),()->i.plusNanos(2_500_000_000L),()->i.plusNanos(Long.MAX_VALUE),()->i.plusNanos(Long.MIN_VALUE),
                    ()->i.minusSeconds(10),()->i.minusSeconds(-5),()->i.minusMillis(10_501),()->i.minusMillis(-1),
                    ()->i.minusNanos(500_000_000),()->i.minusNanos(-499_999_999),()->i.minusNanos(Long.MIN_VALUE),
                    ()->i.minusMillis(Long.MIN_VALUE),()->i.plusMillis(Long.MIN_VALUE),()->i.plusMillis(Long.MAX_VALUE),
                    ()->Instant.ofEpochSecond(-1).minusSeconds(MIN_SECOND),()->Instant.MAX.minusNanos(999_999_999).plusSeconds(-MAX_SECOND),
                    ()->Instant.MIN.plusNanos(1).minusSeconds(MIN_SECOND)};
                for(Op op:ops)o.append(s(op)).append(';');
                o.append(i.plusSeconds(0)==i).append(',').append(i.plusMillis(0)==i).append(',').append(i.plusNanos(0)==i).append(',');
                o.append(i.minusSeconds(0)==i).append(',').append(i.minusMillis(0)==i).append(',').append(i.minusNanos(0)==i).append(',');
                o.append(i.minusNanos(500_000_000).minusSeconds(10)==Instant.EPOCH).append(',').append(i.plusMillis(999)==i);
                Instant c=Instant.EPOCH;
                for(int k=0;k<50;k++)c=c.plusMillis(k*37-900).plusNanos(k*1_234_567L).minusSeconds(k%7-3);
                return o.append(';').append(show(c)).toString();
            }
            public static String comparing(){
                StringBuilder o=new StringBuilder();
                Instant a=Instant.ofEpochSecond(5,100),b=Instant.ofEpochSecond(5,300),c=Instant.ofEpochSecond(6),d=Instant.ofEpochSecond(5,100);
                o.append(a.compareTo(b)).append(',').append(b.compareTo(a)).append(',').append(a.compareTo(d)).append(',').append(a.compareTo(c)).append(',');
                o.append(Instant.MAX.compareTo(Instant.MIN)).append(',').append(Instant.MIN.compareTo(Instant.MAX)).append(',');
                o.append(Instant.ofEpochSecond(0,999_999_999).compareTo(Instant.EPOCH)).append(';');
                o.append(a.isBefore(b)).append(',').append(b.isBefore(a)).append(',').append(a.isBefore(d)).append(',');
                o.append(a.isAfter(b)).append(',').append(b.isAfter(a)).append(',').append(a.isAfter(d)).append(',');
                o.append(Instant.MIN.isBefore(Instant.EPOCH)).append(',').append(Instant.MAX.isAfter(c)).append(';');
                o.append(a.equals(d)).append(',').append(a.equals(b)).append(',').append(a.equals(null)).append(',').append(a.equals("x")).append(',');
                o.append(a.equals(a)).append(',').append(Instant.ofEpochSecond(5).equals(Instant.ofEpochSecond(6))).append(';');
                Instant[] hs={Instant.EPOCH,a,b,c,Instant.MIN,Instant.MAX,Instant.ofEpochSecond(-1),Instant.ofEpochSecond(1L<<32,7),Instant.ofEpochMilli(-1)};
                for(Instant h:hs)o.append(h.hashCode()).append(',');
                o.append(a.hashCode()==d.hashCode());
                Comparable<Instant> cmp=c;
                return o.append(',').append(cmp.compareTo(a)).toString();
            }
            public static String isoStrings(){
                StringBuilder o=new StringBuilder();
                long[][] secsNanos={{0,0},{1322907330,0},{1322907300,0},{1322906400,0},{1322907330,100_000_000},{1322907330,120_000_000},
                    {1322907330,123_000_000},{1322907330,123_400_000},{1322907330,123_456_000},{1322907330,123_456_700},
                    {1322907330,123_456_789},{0,1},{0,1000},{-1,0},{-1,999_999_999},{-60,0},{-86400,0},{-86401,0},
                    {951782400,0},{951868800,0},{-2203891200L,0},{4107542400L,0},{253402300799L,999_000_000},
                    {Y0,0},{Y0-1,0},{Y0+1,0},{Y0-86400,0},{Y0-31622400,0},{Y0-31622401,0},{Y0-K+K/10,0},{Y0-K/1000*999,0},
                    {Y0+K,0},{Y0+K-1,0},{Y0+K+59,0},{Y0+12345L*31556952,5},{Y0-K,0},{Y0-K-1,0},{Y0-K+1,0},{Y0-K+60,0},
                    {Y0-K-86400,0},{Y0-2*K,0},{Y0-2*K+86400,0},{Y0-2*K-1,500_000_000},{Y0-3*K-1,0},
                    {MIN_SECOND,0},{MAX_SECOND,999_999_999},{MAX_SECOND,0},{MIN_SECOND+1,1}};
                for(long[] sn:secsNanos)o.append(Instant.ofEpochSecond(sn[0],sn[1])).append(';');
                return o.append(Instant.EPOCH.toString().length()).toString();
            }
            public static String calendarSweep(){
                StringBuilder o=new StringBuilder();
                int h=0;int n=0;
                for(long d=-800_000;d<=800_000;d+=331){
                    String x=Instant.ofEpochSecond(d*86400+(d%86400+86400)%86400,(int)((d*7919)%1000)*1_000_000).toString();
                    h=h*31+x.hashCode();n++;
                    if(n%400==0)o.append(x).append(' ');
                }
                long sum=0;
                for(long s=MIN_SECOND;s<MAX_SECOND-777_777_777_777_777L;s+=777_777_777_777_777L){
                    String x=Instant.ofEpochSecond(s,s%1_000_000_000L<0?-(s%1_000_000_000L):s%1_000_000_000L).toString();
                    sum=sum*31+x.hashCode();
                    o.append(x).append(' ');
                }
                return o.append(h).append(',').append(sum).append(',').append(n).toString();
            }
        }
        """;

    @Test void factoriesAndRange() throws Exception {
        matchesJvm(FIXTURE, "factories", "rangeErrors");
    }

    @Test void epochMillisAndArithmetic() throws Exception {
        matchesJvm(FIXTURE, "epochMillis", "arithmetic");
    }

    @Test void comparing() throws Exception {
        matchesJvm(FIXTURE, "comparing");
    }

    @Test void isoStrings() throws Exception {
        matchesJvm(FIXTURE, "isoStrings", "calendarSweep");
    }

    @Test void leavesOutTemporalAndClockMembers() {
        rejectsUnusedCall("java/time/Instant", "plus", "(Ljava/time/temporal/TemporalAmount;)Ljava/time/Instant;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/Instant", "minus", "(Ljava/time/temporal/TemporalAmount;)Ljava/time/Instant;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/Instant", "now", "()Ljava/time/Instant;", INVOKESTATIC, false);
        rejectsUnusedCall("java/time/Instant", "parse", "(Ljava/lang/CharSequence;)Ljava/time/Instant;", INVOKESTATIC, false);
        rejectsUnusedCall("java/time/Instant", "truncatedTo", "(Ljava/time/temporal/TemporalUnit;)Ljava/time/Instant;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/Instant", "from", "(Ljava/time/temporal/TemporalAccessor;)Ljava/time/Instant;", INVOKESTATIC, false);
    }
}
