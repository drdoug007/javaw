package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T68: java.sql.Timestamp millisecond/nanosecond core. See tasks/T68-timestamp.md. */
class BytecodeTimestampTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.sql.Timestamp;
        import java.time.Instant;
        import java.util.Date;
        class Spy extends Timestamp {
            final String id;final StringBuilder log;
            Spy(String id,long t,StringBuilder log){super(t);this.id=id;this.log=log;}
            public long getTime(){log.append("get").append(id).append(';');return super.getTime();}
        }
        public class Fixture {
            static String name(Throwable t){return t.getClass().getName();}
            static String fail(Runnable r){
                try{r.run();return "no";}catch(RuntimeException e){return name(e)+":"+e.getMessage();}
            }
            static String failName(Runnable r){
                try{r.run();return "no";}catch(RuntimeException e){return name(e);}
            }
            static String show(Timestamp t){return t.getTime()+"/"+t.getNanos();}

            public static String construct(){
                StringBuilder out=new StringBuilder();
                long[] values={0L,1L,-1L,999L,-999L,1000L,-1000L,-1001L,1500L,-1500L,1700000000123L,-62135596800001L,
                    Long.MAX_VALUE,Long.MIN_VALUE,Long.MIN_VALUE+1,0x80000000L};
                for(long v:values){
                    Timestamp t=new Timestamp(v);
                    out.append(show(t)).append(',').append(t.hashCode()).append(';');
                }
                Timestamp t=new Timestamp(5L);
                t.setTime(-7L);out.append(show(t)).append(';');
                t.setTime(123456L);out.append(show(t)).append(';');
                t.setTime(Long.MIN_VALUE);out.append(show(t)).append(';');
                Object o=t;
                out.append(o instanceof Date).append(',').append(o instanceof Comparable).append(',').append(o instanceof Cloneable).append(',').append(o instanceof java.io.Serializable);
                return out.toString();
            }
            public static String nanos(){
                StringBuilder out=new StringBuilder();
                Timestamp t=new Timestamp(2500L);
                int[] ns={0,1,999999,1000000,123456789,999999999};
                for(int n:ns){t.setNanos(n);out.append(show(t)).append(',').append(t.hashCode()).append(';');}
                out.append(fail(()->t.setNanos(-1))).append(';');
                out.append(fail(()->t.setNanos(1000000000))).append(';');
                out.append(fail(()->t.setNanos(Integer.MIN_VALUE))).append(';');
                out.append(show(t)).append(';');
                Timestamp n=new Timestamp(-1L);
                n.setNanos(0);out.append(show(n)).append(';');
                Timestamp c=(Timestamp)new Timestamp(-1234L).clone();
                out.append(c.getClass().getName()).append(',').append(show(c));
                return out.toString();
            }
            public static String equality(){
                StringBuilder out=new StringBuilder();
                Timestamp a=new Timestamp(1000L),b=new Timestamp(1000L),c=new Timestamp(1000L);
                c.setNanos(1);
                Date d=new Date(1000L);
                out.append(a.equals(b)).append(a.equals(c)).append(a.equals((Object)b)).append(a.equals((Object)c)).append(';');
                out.append(a.equals(d)).append(d.equals(a)).append(d.equals(c)).append(a.equals("x")).append(a.equals((Object)null)).append(a.equals((Timestamp)null)).append(';');
                out.append(a.hashCode()==c.hashCode()).append(',').append(a.hashCode()==d.hashCode()).append(';');
                Timestamp e=new Timestamp(1000L);e.setNanos(999999);
                Timestamp f=new Timestamp(1000L);f.setNanos(999998);
                out.append(e.equals(f)).append(e.getTime()==f.getTime()).append(';');
                Timestamp g=new Timestamp(1000L);g.setNanos(1000000);
                Timestamp h=new Timestamp(1001L);
                out.append(g.equals(h)).append(g.getTime()==h.getTime()).append(g.compareTo(h)).append(h.compareTo(g));
                return out.toString();
            }
            public static String ordering(){
                StringBuilder out=new StringBuilder();
                Timestamp[] ts={new Timestamp(-5L),new Timestamp(0L),new Timestamp(0L),new Timestamp(0L),new Timestamp(3L),new Timestamp(Long.MIN_VALUE),new Timestamp(Long.MAX_VALUE)};
                ts[3].setNanos(1);
                for(Timestamp a:ts){
                    for(Timestamp b:ts){
                        out.append(a.compareTo(b)).append(a.before(b)?'b':'-').append(a.after(b)?'a':'-').append(a.equals(b)?'e':'-').append(' ');
                    }
                    out.append(';');
                }
                Date d0=new Date(0L),d3=new Date(3L);
                out.append(ts[1].compareTo(d0)).append(ts[3].compareTo(d0)).append(ts[4].compareTo(d0)).append(ts[1].compareTo(d3)).append(',');
                out.append(ts[3].compareTo((Date)ts[1])).append(ts[1].compareTo((Date)ts[3])).append(',');
                out.append(d0.compareTo(ts[3])).append(d3.compareTo(ts[3])).append(d0.before(ts[4])).append(d3.after(ts[0])).append(',');
                out.append(ts[3].before((Date)ts[1])).append(ts[3].after((Date)ts[1])).append(';');
                @SuppressWarnings({"rawtypes","unchecked"}) Comparable raw=ts[3];
                out.append(raw.compareTo(ts[1])).append(raw.compareTo(d0)).append(';');
                out.append(failName(()->raw.compareTo("x"))).append(';');
                out.append(failName(()->ts[1].compareTo((Timestamp)null))).append(',');
                out.append(failName(()->ts[1].compareTo((Date)null))).append(',');
                out.append(failName(()->ts[1].before((Timestamp)null))).append(';');
                java.util.List<Date> list=new java.util.ArrayList<>();
                Timestamp n=new Timestamp(4L);n.setNanos(4500000);
                list.add(new Timestamp(9L));list.add(n);list.add(new Timestamp(-2L));list.add(new Timestamp(4L));
                java.util.Collections.sort(list);
                for(Date x:list)out.append(x.getTime()).append('/').append(((Timestamp)x).getNanos()).append(' ');
                return out.toString();
            }
            public static String instants(){
                StringBuilder out=new StringBuilder();
                long[] values={0L,1L,-1L,1500L,-1500L,1700000000123L,Long.MAX_VALUE,Long.MIN_VALUE};
                for(long v:values){
                    Instant i=new Timestamp(v).toInstant();
                    out.append(i.getEpochSecond()).append('/').append(i.getNano()).append(';');
                }
                Timestamp t=new Timestamp(-2000L);t.setNanos(123456789);
                Instant i=t.toInstant();
                out.append(i.getEpochSecond()).append('/').append(i.getNano()).append(';');
                Instant[] ins={Instant.ofEpochSecond(12L,345678901L),Instant.ofEpochSecond(-12L,345678901L),Instant.ofEpochSecond(0L,1L),
                    Instant.ofEpochMilli(-1L),Instant.ofEpochSecond(Long.MAX_VALUE/1000+1,7L),Instant.ofEpochSecond(-31557014167219200L,0L)};
                for(Instant x:ins){
                    Timestamp s=Timestamp.from(x);
                    out.append(show(s)).append(',').append(s.toInstant().equals(x)).append(';');
                }
                out.append(failName(()->Timestamp.from(null))).append(';');
                Date viaDate=Date.from(Instant.ofEpochSecond(3L,5L));
                out.append(viaDate.getClass().getName()).append(',').append(viaDate.getTime());
                return out.toString();
            }
            public static String selfCalls(){
                StringBuilder out=new StringBuilder();
                StringBuilder log=new StringBuilder();
                Spy a=new Spy("A",5L,log),b=new Spy("B",9L,log);
                Timestamp p=new Timestamp(5L);
                Date q=new Date(5L);
                out.append(a.compareTo(b)).append(log).append('|');log.setLength(0);
                out.append(p.compareTo(b)).append(log).append('|');log.setLength(0);
                out.append(a.compareTo(q)).append(log).append('|');log.setLength(0);
                out.append(q.compareTo(a)).append(log).append('|');log.setLength(0);
                out.append(a.before(b)).append(log).append('|');log.setLength(0);
                out.append(a.after(p)).append(log).append('|');log.setLength(0);
                out.append(a.equals(p)).append(log).append('|');log.setLength(0);
                out.append(a.equals((Object)p)).append(log).append('|');log.setLength(0);
                out.append(a.equals(q)).append(log).append('|');log.setLength(0);
                out.append(q.equals(a)).append(log).append('|');log.setLength(0);
                out.append(a.hashCode()).append(log).append('|');log.setLength(0);
                out.append(a.toInstant().getNano()).append(log).append('|');log.setLength(0);
                a.setTime(11L);out.append(log).append('|');log.setLength(0);
                a.setNanos(7);out.append(a.getNanos()).append(log).append('|');log.setLength(0);
                out.append(a.getTime()).append(log);
                return out.toString();
            }
        }
        """;

    @Test void millisAndNanos() throws Exception {
        matchesJvm(FIXTURE, "construct", "nanos");
    }

    @Test void equalityAndOrdering() throws Exception {
        matchesJvm(FIXTURE, "equality", "ordering");
    }

    @Test void instantConversions() throws Exception {
        matchesJvm(FIXTURE, "instants");
    }

    @Test void overridableSelfCalls() throws Exception {
        matchesJvm(FIXTURE, "selfCalls");
    }

    @Test void leavesOutZonesAndLocalDateTime() {
        rejectsUnusedCall("java/sql/Timestamp", "valueOf", "(Ljava/lang/String;)Ljava/sql/Timestamp;", INVOKESTATIC, false);
        rejectsUnusedCall("java/sql/Timestamp", "valueOf", "(Ljava/time/LocalDateTime;)Ljava/sql/Timestamp;", INVOKESTATIC, false);
        rejectsUnusedCall("java/sql/Timestamp", "toLocalDateTime", "()Ljava/time/LocalDateTime;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/sql/Timestamp", "<init>", "(IIIIIII)V", INVOKESPECIAL, false);
        rejectsUnusedCall("java/sql/Timestamp", "getYear", "()I", INVOKEVIRTUAL, false);
    }
}
