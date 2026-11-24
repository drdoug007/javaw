package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T56: java.time.Duration core subset, DateTimeException and DateTimeParseException. See tasks/T56-duration.md. */
class BytecodeDurationTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.time.DateTimeException;
        import java.time.Duration;
        import java.time.format.DateTimeParseException;
        import java.util.Arrays;
        public class Fixture {
            static String name(Throwable t){return t.getClass().getName();}
            static String show(Duration d){return d+"="+d.getSeconds()+"/"+d.getNano();}
            interface Op{Duration run();}
            interface LongOp{long run();}
            static String tryIt(Op op){
                try{return show(op.run());}catch(RuntimeException e){return name(e);}
            }
            static String tryLong(LongOp op){
                try{return String.valueOf(op.run());}catch(RuntimeException e){return name(e);}
            }

            public static String factories(){
                StringBuilder out=new StringBuilder();
                Duration[] all={Duration.ZERO,Duration.ofDays(2),Duration.ofDays(-1),Duration.ofHours(-3),Duration.ofMinutes(90),
                    Duration.ofSeconds(5),Duration.ofSeconds(5,-1),Duration.ofSeconds(-1,1_500_000_000L),Duration.ofSeconds(3,-4_000_000_001L),
                    Duration.ofSeconds(0,Long.MAX_VALUE),Duration.ofSeconds(0,Long.MIN_VALUE),Duration.ofMillis(-1),Duration.ofMillis(1234),
                    Duration.ofMillis(-1001),Duration.ofNanos(-1),Duration.ofNanos(1_000_000_001L),Duration.ofNanos(Long.MIN_VALUE),
                    Duration.ofMillis(Long.MAX_VALUE),Duration.ofSeconds(Long.MIN_VALUE),Duration.ofSeconds(Long.MAX_VALUE,999_999_999)};
                for(Duration d:all)out.append(show(d)).append(';');
                out.append(Duration.ofSeconds(0)==Duration.ZERO).append(',').append(Duration.ofMillis(0)==Duration.ZERO).append(',');
                out.append(Duration.ofSeconds(1,-1_000_000_000)==Duration.ZERO).append(',').append(Duration.ofNanos(0)==Duration.ZERO).append(',');
                out.append(Duration.ofDays(0)==Duration.ZERO).append(',').append(Duration.ofHours(0)==Duration.ZERO).append(',');
                out.append(Duration.ofMinutes(0)==Duration.ZERO).append(',').append(Duration.ofSeconds(7)==Duration.ofSeconds(7));
                return out.toString();
            }
            public static String factoryOverflow(){
                StringBuilder out=new StringBuilder();
                out.append(tryIt(()->Duration.ofDays(Long.MAX_VALUE))).append(';');
                out.append(tryIt(()->Duration.ofDays(106751991167300L))).append(';');
                out.append(tryIt(()->Duration.ofDays(106751991167301L))).append(';');
                out.append(tryIt(()->Duration.ofHours(Long.MIN_VALUE))).append(';');
                out.append(tryIt(()->Duration.ofMinutes(Long.MAX_VALUE/60+1))).append(';');
                out.append(tryIt(()->Duration.ofSeconds(Long.MAX_VALUE,1_000_000_000))).append(';');
                out.append(tryIt(()->Duration.ofSeconds(Long.MIN_VALUE,-1))).append(';');
                out.append(tryIt(()->Duration.ofSeconds(Long.MIN_VALUE,0)));
                return out.toString();
            }
            public static String isoStrings(){
                StringBuilder out=new StringBuilder();
                Duration[] all={Duration.ZERO,Duration.ofSeconds(8*3600+6*60+12,345_000_000),Duration.ofHours(25),Duration.ofDays(1),
                    Duration.ofMinutes(-61),Duration.ofMinutes(60),Duration.ofSeconds(59),Duration.ofSeconds(3601),Duration.ofMillis(-1),
                    Duration.ofSeconds(-1,1),Duration.ofSeconds(-61,500_000_000),Duration.ofSeconds(0,1),Duration.ofSeconds(-3600,1),
                    Duration.ofSeconds(-3601,999_000_000),Duration.ofMillis(1500),Duration.ofMillis(-1500),Duration.ofSeconds(60,10),
                    Duration.ofSeconds(Long.MAX_VALUE),Duration.ofSeconds(Long.MIN_VALUE),Duration.ofSeconds(Long.MIN_VALUE,1),
                    Duration.ofSeconds(Long.MAX_VALUE,999_999_999),Duration.ofNanos(Long.MIN_VALUE),Duration.ofNanos(-100)};
                for(Duration d:all)out.append(d).append(';');
                out.append(Duration.ofHours(2).toString().length());
                return out.toString();
            }
            public static String plusAndMinus(){
                StringBuilder out=new StringBuilder();
                Duration d=Duration.ofSeconds(10,500_000_000);
                out.append(d.plus(Duration.ofMillis(700))).append(';').append(d.minus(Duration.ofMillis(700))).append(';');
                out.append(d.plus(Duration.ofSeconds(-20))).append(';').append(d.minus(d)).append(';').append(d.minus(d)==Duration.ZERO).append(';');
                out.append(d.plusDays(1)).append(',').append(d.plusHours(-1)).append(',').append(d.plusMinutes(2)).append(',');
                out.append(d.plusSeconds(-11)).append(',').append(d.plusMillis(-500)).append(',').append(d.plusMillis(1501)).append(',');
                out.append(d.plusNanos(-500_000_001)).append(',').append(d.plusNanos(2_500_000_000L)).append(';');
                out.append(d.minusDays(1)).append(',').append(d.minusHours(1)).append(',').append(d.minusMinutes(-1)).append(',');
                out.append(d.minusSeconds(10)).append(',').append(d.minusMillis(10_501)).append(',').append(d.minusNanos(500_000_000)).append(';');
                out.append(d.plus(Duration.ZERO)==d).append(',').append(d.plusSeconds(0)==d).append(',').append(d.minusNanos(0)==d).append(',');
                out.append(d.plusMillis(0)==d).append(',').append(d.minus(Duration.ZERO)==d).append(';');
                out.append(show(Duration.ofSeconds(-1).minusSeconds(Long.MIN_VALUE))).append(',');
                out.append(show(Duration.ofSeconds(-1).minus(Duration.ofSeconds(Long.MIN_VALUE)))).append(',');
                out.append(show(Duration.ofSeconds(-1,999_999_999).minus(Duration.ofSeconds(Long.MIN_VALUE)))).append(',');
                out.append(show(Duration.ofNanos(-1).minusNanos(Long.MIN_VALUE))).append(',');
                out.append(show(Duration.ofMillis(-1).minusMillis(Long.MIN_VALUE))).append(';');
                out.append(tryIt(()->Duration.ofSeconds(Long.MAX_VALUE).plusNanos(1_000_000_000))).append(',');
                out.append(tryIt(()->Duration.ofSeconds(Long.MAX_VALUE).plus(Duration.ofSeconds(1)))).append(',');
                out.append(tryIt(()->Duration.ofSeconds(Long.MIN_VALUE).minusSeconds(1))).append(',');
                out.append(tryIt(()->Duration.ofSeconds(0).minus(Duration.ofSeconds(Long.MIN_VALUE)))).append(',');
                out.append(tryIt(()->Duration.ofSeconds(1).minusDays(Long.MIN_VALUE))).append(',');
                out.append(tryIt(()->Duration.ZERO.plusHours(Long.MAX_VALUE)));
                return out.toString();
            }
            public static String multiplyDivideNegate(){
                StringBuilder out=new StringBuilder();
                Duration d=Duration.ofSeconds(3,250_000_000);
                out.append(d.multipliedBy(3)).append(',').append(d.multipliedBy(-2)).append(',').append(d.multipliedBy(0)==Duration.ZERO).append(',');
                out.append(d.multipliedBy(1)==d).append(',').append(Duration.ofNanos(1).multipliedBy(1_000_000_000_000L)).append(';');
                out.append(d.dividedBy(2)).append(',').append(d.dividedBy(-4)).append(',').append(d.dividedBy(1)==d).append(',');
                out.append(Duration.ofSeconds(-7).dividedBy(2)).append(',').append(Duration.ofNanos(7).dividedBy(2)).append(',');
                out.append(Duration.ofNanos(-7).dividedBy(2)).append(',').append(Duration.ofSeconds(1).dividedBy(3)).append(',');
                out.append(Duration.ofSeconds(-1).dividedBy(3)).append(',').append(Duration.ofNanos(5).dividedBy(10)==Duration.ZERO).append(',');
                out.append(show(Duration.ofSeconds(Long.MIN_VALUE).dividedBy(-2))).append(';');
                try{d.dividedBy(0);out.append("no;");}catch(ArithmeticException e){out.append(e.getMessage()).append(';');}
                out.append(show(d.negated())).append(',').append(show(d.negated().negated())).append(',').append(Duration.ZERO.negated()==Duration.ZERO).append(',');
                out.append(show(Duration.ofNanos(-1).negated())).append(',').append(show(Duration.ofSeconds(Long.MAX_VALUE).negated())).append(';');
                out.append(d.abs()==d).append(',').append(show(d.negated().abs())).append(',').append(Duration.ZERO.abs()==Duration.ZERO).append(';');
                out.append(tryIt(()->Duration.ofSeconds(Long.MIN_VALUE).negated())).append(',');
                out.append(tryIt(()->Duration.ofSeconds(Long.MIN_VALUE).abs())).append(',');
                out.append(tryIt(()->Duration.ofSeconds(Long.MAX_VALUE).multipliedBy(2))).append(',');
                out.append(tryIt(()->Duration.ofSeconds(4).multipliedBy(Long.MAX_VALUE/3))).append(',');
                out.append(tryIt(()->Duration.ofSeconds(Long.MIN_VALUE).dividedBy(-1))).append(',');
                out.append(tryIt(()->Duration.ofSeconds(Long.MIN_VALUE).multipliedBy(-1)));
                return out.toString();
            }
            public static String conversions(){
                StringBuilder out=new StringBuilder();
                Duration[] all={Duration.ZERO,Duration.ofSeconds(90061,1_500_000),Duration.ofSeconds(-90061,1_500_000),Duration.ofSeconds(-90),
                    Duration.ofMillis(-1),Duration.ofNanos(-1),Duration.ofNanos(-1_000_001),Duration.ofDays(-3),Duration.ofSeconds(59,999_999_999)};
                for(Duration d:all){
                    out.append(d.toDays()).append('/').append(d.toHours()).append('/').append(d.toMinutes()).append('/').append(d.toSeconds());
                    out.append('/').append(d.toMillis()).append('/').append(d.toNanos()).append(';');
                }
                out.append(Duration.ofMillis(Long.MAX_VALUE).toMillis()).append(',').append(Duration.ofMillis(Long.MIN_VALUE).toMillis()).append(',');
                out.append(Duration.ofNanos(Long.MAX_VALUE).toNanos()).append(',').append(Duration.ofNanos(Long.MIN_VALUE).toNanos()).append(';');
                Duration max=Duration.ofSeconds(Long.MAX_VALUE,999_999_999),min=Duration.ofSeconds(Long.MIN_VALUE);
                out.append(max.toDays()).append(',').append(min.toHours()).append(',').append(min.toMinutes()).append(',').append(max.toSeconds()).append(';');
                out.append(tryLong(max::toMillis)).append(',').append(tryLong(min::toMillis)).append(',');
                out.append(tryLong(()->Duration.ofSeconds(Long.MAX_VALUE/1000+1).toMillis())).append(',');
                out.append(tryLong(()->Duration.ofNanos(Long.MAX_VALUE).plusNanos(1).toNanos())).append(',');
                out.append(tryLong(()->Duration.ofNanos(Long.MIN_VALUE).minusNanos(1).toNanos())).append(',');
                out.append(tryLong(()->Duration.ofSeconds(Long.MAX_VALUE/1_000_000_000L,775_807_000).toNanos()));
                return out.toString();
            }
            public static String comparing(){
                StringBuilder out=new StringBuilder();
                Duration a=Duration.ofSeconds(5,1),b=Duration.ofSeconds(5,2),c=Duration.ofSeconds(-5,999_999_999),e=Duration.ofMillis(5000).plusNanos(1);
                out.append(a.compareTo(b)).append(',').append(b.compareTo(a)).append(',').append(a.compareTo(e)).append(',');
                out.append(Integer.signum(c.compareTo(a))).append(',').append(Integer.signum(Duration.ofSeconds(Long.MAX_VALUE).compareTo(Duration.ofSeconds(Long.MIN_VALUE)))).append(';');
                out.append(a.equals(e)).append(',').append(a.equals(b)).append(',').append(a.equals(a)).append(',').append(a.equals(null)).append(',');
                out.append(a.equals("PT5.000000001S")).append(',').append(a.hashCode()).append(',').append(e.hashCode()).append(',');
                out.append(c.hashCode()).append(',').append(Duration.ZERO.hashCode()).append(',').append(Duration.ofSeconds(Long.MIN_VALUE,7).hashCode()).append(';');
                out.append(Duration.ZERO.isZero()).append(',').append(a.isZero()).append(',').append(Duration.ofNanos(1).isZero()).append(',');
                out.append(c.isNegative()).append(',').append(Duration.ZERO.isNegative()).append(',').append(Duration.ofNanos(-1).isNegative()).append(';');
                Duration[] sorted={b,Duration.ofDays(1),c,Duration.ZERO,a,Duration.ofNanos(-1),Duration.ofHours(-1)};
                Arrays.sort(sorted);
                for(Duration d:sorted)out.append(d).append(' ');
                Object o=a;
                out.append((o instanceof Comparable)).append(',').append((o instanceof java.io.Serializable));
                return out.toString();
            }
            static String parsed(CharSequence text){
                try{return show(Duration.parse(text));}
                catch(DateTimeParseException e){
                    return name(e)+":"+e.getMessage()+"|"+e.getParsedString()+"|"+e.getErrorIndex()+"|"+(e.getCause()==null?"-":name(e.getCause()));
                }
            }
            public static String parseValid(){
                StringBuilder out=new StringBuilder();
                String[] texts={"PT20.345S","PT15M","PT10H","P2D","P2DT3H4M","PT-6H3M","-PT6H3M","-PT-6H+3M","pt1h2m","Pt1,5s","PT-0.5S",
                    "PT0.123456789S","PT1.S","P-1DT+1H","+PT1S","PT-0S","P0D","PT9223372036854775807S","PT-9223372036854775808S",
                    "P1DT-86400S","-PT0.000000001S","PT0.1S","PT-1.000000001S","P1Dt","P2dT1M"};
                for(String t:texts)out.append(parsed(t)).append(';');
                out.append(parsed(new StringBuilder("PT1M30S")));
                return out.toString();
            }
            public static String parseInvalid(){
                StringBuilder out=new StringBuilder();
                String[] texts={"","P","PT","P1DT","-P","PT1.1234567890S","PT1H1H","PT S","1D","PT1M1H","PT.5S","P1H","PT1D","PT1HS",
                    "PT9223372036854775808S","P106751991167301D","PT2562047788015216H","PT153722867280912931M","P1DT9223372036854775807S",
                    "-PT-9223372036854775808S","PT1.-5S"," PT1S","PT1S ","PT1s0",
                    "P-D","P+D","PT-H","PT+M","PT-S","PT-.5S","P-DT1H","PT-H1M","PT1H-M","PT-H-M-S","-P-D","P1DT-S"};
                for(String t:texts)out.append(parsed(t)).append(';');
                try{Duration.parse(null);out.append("no");}catch(NullPointerException e){out.append(name(e)).append(':').append(e.getMessage());}
                return out.toString();
            }
            public static String exceptionTypes(){
                StringBuilder out=new StringBuilder();
                DateTimeException a=new DateTimeException("bad");
                DateTimeException b=new DateTimeException("worse",new ArithmeticException("x"));
                out.append(a.getMessage()).append(',').append(a).append(',').append(b.getCause().getMessage()).append(',');
                out.append((Object)a instanceof RuntimeException).append(';');
                DateTimeParseException c=new DateTimeParseException("msg",new StringBuilder("text"),3);
                DateTimeParseException d=new DateTimeParseException("m2","abc",1,new IllegalStateException("cause"));
                out.append(c.getMessage()).append(',').append(c.getParsedString()).append(',').append(c.getErrorIndex()).append(',').append(c.getCause()).append(',');
                out.append(d).append(',').append(d.getParsedString()).append(',').append(d.getErrorIndex()).append(',').append(name(d.getCause())).append(',');
                out.append((Object)c instanceof DateTimeException);
                try{throw new DateTimeParseException("t","u",0);}catch(DateTimeException e){out.append(',').append(name(e));}
                return out.toString();
            }
        }
        """;

    @Test void factoriesAndText() throws Exception {
        matchesJvm(FIXTURE, "factories", "factoryOverflow", "isoStrings");
    }

    @Test void arithmetic() throws Exception {
        matchesJvm(FIXTURE, "plusAndMinus", "multiplyDivideNegate");
    }

    @Test void conversionsAndComparison() throws Exception {
        matchesJvm(FIXTURE, "conversions", "comparing");
    }

    @Test void parsing() throws Exception {
        matchesJvm(FIXTURE, "parseValid", "parseInvalid", "exceptionTypes");
    }

    @Test void leavesOutTemporalMembers() {
        rejectsUnusedCall("java/time/Duration", "of", "(JLjava/time/temporal/TemporalUnit;)Ljava/time/Duration;", INVOKESTATIC, false);
        rejectsUnusedCall("java/time/Duration", "between", "(Ljava/time/temporal/Temporal;Ljava/time/temporal/Temporal;)Ljava/time/Duration;", INVOKESTATIC, false);
        rejectsUnusedCall("java/time/Duration", "get", "(Ljava/time/temporal/TemporalUnit;)J", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/Duration", "dividedBy", "(Ljava/time/Duration;)J", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/Duration", "withSeconds", "(J)Ljava/time/Duration;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/Duration", "toHoursPart", "()I", INVOKEVIRTUAL, false);
    }
}
