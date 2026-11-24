package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T63: java.util.Date millisecond core. See tasks/T63-date.md. */
@Tag("agent-pending")
class BytecodeDateTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.time.Instant;
        import java.util.Date;
        class Spy extends Date {
            final String id;final StringBuilder log;
            Spy(String id,long t,StringBuilder log){super(t);this.id=id;this.log=log;}
            public long getTime(){log.append("get").append(id).append(';');return super.getTime();}
            public void setTime(long t){log.append("set").append(id).append(';');super.setTime(t);}
        }
        class Shifted extends Date {
            Shifted(long t){super(t);}
            public long getTime(){return super.getTime()+1000;}
        }
        public class Fixture {
            static String name(Throwable t){return t.getClass().getName();}
            static String fail(Runnable r){
                try{r.run();return "no";}catch(RuntimeException e){return name(e);}
            }

            public static String millis(){
                StringBuilder out=new StringBuilder();
                long[] values={0L,1L,-1L,999L,-1000L,1700000000123L,-62135596800000L,Long.MAX_VALUE,Long.MIN_VALUE,0x80000000L,0xFFFFFFFF00000000L};
                for(long v:values){
                    Date d=new Date(v);
                    out.append(d.getTime()).append(',').append(d.hashCode()).append(';');
                }
                Date d=new Date(5L);
                d.setTime(-7L);out.append(d.getTime()).append(',').append(d.hashCode()).append(';');
                d.setTime(Long.MAX_VALUE);out.append(d.getTime()).append(';');
                Object o=d;
                out.append(o instanceof Comparable).append(',').append(o instanceof Cloneable).append(',').append(o instanceof java.io.Serializable);
                return out.toString();
            }
            public static String ordering(){
                StringBuilder out=new StringBuilder();
                Date[] ds={new Date(-5L),new Date(0L),new Date(0L),new Date(3L),new Date(Long.MIN_VALUE),new Date(Long.MAX_VALUE)};
                for(Date a:ds){
                    for(Date b:ds){
                        out.append(a.compareTo(b)).append(a.before(b)?'b':'-').append(a.after(b)?'a':'-').append(a.equals(b)?'e':'-').append(' ');
                    }
                    out.append(';');
                }
                Comparable<Date> c=ds[0];out.append(c.compareTo(ds[3])).append(';');
                @SuppressWarnings({"rawtypes","unchecked"}) Comparable raw=ds[3];
                out.append(raw.compareTo(ds[0])).append(';');
                out.append(fail(()->raw.compareTo("x"))).append(';');
                Date z=new Date(1L);
                out.append(z.equals(null)).append(',').append(z.equals("1")).append(',').append(z.equals(Long.valueOf(1L))).append(',').append(z.equals(z)).append(';');
                out.append(fail(()->z.compareTo(null))).append(',').append(fail(()->z.before(null))).append(',').append(fail(()->z.after(null)));
                java.util.List<Date> list=new java.util.ArrayList<>();
                list.add(new Date(9L));list.add(new Date(-2L));list.add(new Date(4L));list.add(new Date(-2L));
                java.util.Collections.sort(list);
                out.append(';');
                for(Date x:list)out.append(x.getTime()).append(' ');
                return out.toString();
            }
            public static String clones(){
                StringBuilder out=new StringBuilder();
                Date a=new Date(42L);
                Date b=(Date)a.clone();
                out.append(a==b).append(',').append(a.equals(b)).append(',').append(b.getTime()).append(',').append(b.getClass().getName()).append(';');
                b.setTime(43L);
                out.append(a.getTime()).append(',').append(b.getTime()).append(',').append(a.equals(b)).append(';');
                StringBuilder log=new StringBuilder();
                Spy s=new Spy("S",7L,log);
                Object c=s.clone();
                out.append(c.getClass().getName()).append(',').append(c!=s).append(',').append(log).append(';');
                log.setLength(0);
                out.append(((Date)c).getTime()).append(',').append(((Spy)c).id).append(',').append(log);
                return out.toString();
            }
            public static String instants(){
                StringBuilder out=new StringBuilder();
                long[] values={0L,1L,-1L,1500L,-1500L,1700000000123L,Long.MAX_VALUE,Long.MIN_VALUE};
                for(long v:values){
                    Instant i=new Date(v).toInstant();
                    out.append(i.getEpochSecond()).append('/').append(i.getNano()).append('/').append(i.toEpochMilli()).append(';');
                }
                out.append(Date.from(Instant.ofEpochMilli(-1L)).getTime()).append(',');
                out.append(Date.from(Instant.ofEpochSecond(12L,345678901L)).getTime()).append(',');
                out.append(Date.from(Instant.ofEpochSecond(-12L,345678901L)).getTime()).append(',');
                out.append(Date.from(Instant.ofEpochMilli(Long.MIN_VALUE)).getTime()).append(',');
                out.append(Date.from(Instant.ofEpochMilli(Long.MAX_VALUE)).getTime()).append(',');
                out.append(Date.from(new Date(77L).toInstant()).equals(new Date(77L))).append(',');
                out.append(new Date(-3L).toInstant().equals(Instant.ofEpochMilli(-3L))).append(';');
                out.append(fail(()->Date.from(null))).append(';');
                Instant[] big={Instant.ofEpochSecond(Long.MAX_VALUE/1000+1,0L),Instant.ofEpochSecond(Long.MIN_VALUE/1000-1,0L),Instant.ofEpochSecond(Long.MAX_VALUE/1000,808000000L)};
                for(Instant i:big){
                    try{out.append(Date.from(i).getTime()).append(';');}
                    catch(IllegalArgumentException e){
                        Throwable cause=e.getCause();
                        out.append(name(e)).append(',').append(cause==null?"null":name(cause)).append(',')
                           .append(e.getMessage()!=null&&e.getMessage().startsWith("java.lang.ArithmeticException")).append(';');
                    }
                }
                return out.toString();
            }
            public static String selfCalls(){
                StringBuilder out=new StringBuilder();
                StringBuilder log=new StringBuilder();
                Spy a=new Spy("A",5L,log),b=new Spy("B",9L,log);
                Date p=new Date(5L);
                out.append(a.compareTo(b)).append(log).append('|');log.setLength(0);
                out.append(p.compareTo(b)).append(log).append('|');log.setLength(0);
                out.append(b.compareTo(p)).append(log).append('|');log.setLength(0);
                out.append(a.before(b)).append(log).append('|');log.setLength(0);
                out.append(p.after(a)).append(log).append('|');log.setLength(0);
                out.append(a.equals(p)).append(log).append('|');log.setLength(0);
                out.append(p.equals(a)).append(log).append('|');log.setLength(0);
                out.append(a.equals("x")).append(log).append('|');log.setLength(0);
                out.append(a.hashCode()).append(log).append('|');log.setLength(0);
                out.append(a.toInstant().toEpochMilli()).append(log).append('|');log.setLength(0);
                a.setTime(11L);out.append(a.getTime()).append(log).append('|');log.setLength(0);
                Shifted s=new Shifted(0L);Date q=new Date(1000L);
                out.append(s.equals(q)).append(q.equals(s)).append(s.compareTo(q)).append(q.compareTo(s)).append(',');
                out.append(s.before(new Date(1001L))).append(s.after(new Date(999L))).append(',');
                out.append(s.hashCode()).append(',').append(s.toInstant().toEpochMilli()).append(',').append(((Date)s.clone()).getTime());
                return out.toString();
            }
        }
        """;

    @Test void millisecondsAndOrdering() throws Exception {
        matchesJvm(FIXTURE, "millis", "ordering", "clones");
    }

    @Test void instantConversions() throws Exception {
        matchesJvm(FIXTURE, "instants");
    }

    @Test void overridableSelfCalls() throws Exception {
        matchesJvm(FIXTURE, "selfCalls");
    }

    @Test void leavesOutClockZonesAndFields() {
        rejectsUnusedCall("java/util/Date", "<init>", "()V", INVOKESPECIAL, false);
        rejectsUnusedCall("java/util/Date", "getYear", "()I", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/Date", "parse", "(Ljava/lang/String;)J", INVOKESTATIC, false);
        rejectsUnusedCall("java/util/Date", "<init>", "(III)V", INVOKESPECIAL, false);
        rejectsUnusedCall("java/util/Date", "getTimezoneOffset", "()I", INVOKEVIRTUAL, false);
    }
}
