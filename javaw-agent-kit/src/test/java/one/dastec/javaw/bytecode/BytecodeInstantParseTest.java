package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T70: java.time.Instant.parse(CharSequence) for ISO_INSTANT text. Expected values come from the JVM; see tasks/T70-instant-parse.md. */
@Tag("agent-pending")
class BytecodeInstantParseTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.time.DateTimeException;
        import java.time.Instant;
        import java.time.format.DateTimeParseException;
        public class Fixture {
            static final String UNABLE="Unable to obtain Instant from TemporalAccessor";
            /** Parses one input; a DateTimeParseException prints its index, message, cause class and parsed string check. */
            static String p(CharSequence text){
                try{
                    Instant i=Instant.parse(text);
                    return i.getEpochSecond()+"/"+i.getNano()+"="+i;
                }catch(DateTimeParseException e){
                    String m=e.getMessage();
                    // The JDK appends a Parsed map whose order is not deterministic; compare up to the fixed prefix.
                    int u=m.indexOf(UNABLE);
                    if(u>=0)m=m.substring(0,u+UNABLE.length());
                    String cause=e.getCause()==null?"-":e.getCause().getClass().getSimpleName();
                    return "DTPE@"+e.getErrorIndex()+":"+m+":"+cause+":"+e.getParsedString().equals(String.valueOf(text))
                        +":"+(e instanceof DateTimeException);
                }catch(RuntimeException e){
                    return e.getClass().getSimpleName();
                }
            }
            static String all(String... texts){
                StringBuilder o=new StringBuilder();
                for(String t:texts)o.append(p(t)).append('\\n');
                return o.toString();
            }

            public static String valid(){
                return all("2011-12-03T10:15:30Z","2011-12-03t10:15:30z","1970-01-01T00:00:00Z","1969-12-31T23:59:59Z",
                    "2011-12-03T10:15:30.Z","2011-12-03T10:15:30.1Z","2011-12-03T10:15:30.12Z","2011-12-03T10:15:30.123Z",
                    "2011-12-03T10:15:30.1234Z","2011-12-03T10:15:30.123456789Z","2011-12-03T10:15:30.000000001Z",
                    "2011-12-03T10:15:30.000Z","2011-12-03T10:15:30.000000000Z","2011-12-03T10:15:30.5+01:00",
                    "2011-12-03T10:15:30+01:00","2011-12-03T10:15:30-05:30","2011-12-03T10:15:30+01:30:15",
                    "2011-12-03T10:15:30-18:00","2011-12-03T10:15:30+18:00","2011-12-03T10:15:30+00:00",
                    "2011-12-03T10:15:30-00:00","1970-01-01T00:00:00-00:00:00","1970-01-01T00:00:00-01:30:15",
                    "2011-12-03T10:15:30+01:00:00","2011-12-03T10:15:30.123+01:00:30","2011-12-03T23:59:60Z",
                    "2016-12-31T23:59:60Z","2011-12-03T23:59:60.5Z","2011-12-03T23:59:60+01:00","2011-12-03T24:00:00Z",
                    "1999-12-31T24:00:00Z","1970-01-01T24:00:00+01:00","2012-02-29T00:00:00Z","2000-02-29T12:00:00Z",
                    "0000-01-01T00:00:00Z","-0001-01-01T00:00:00Z","-0004-02-29T00:00:00Z","-2011-12-03T10:15:30Z",
                    "+10000-01-01T00:00:00Z","+10000-02-29T00:00:00Z","+10004-02-29T00:00:00Z","+12011-12-03T10:15:30Z",
                    "+01970-01-01T00:00:00Z","-10000-01-01T00:00:00Z","-10000-02-29T00:00:00Z","-10001-12-31T23:59:59Z",
                    "+12345-06-07T08:09:10.123Z","-12345-06-07T08:09:10.123Z","-999999999-01-01T00:00:00Z",
                    "+1000000000-12-31T23:59:59Z","+1000000000-12-31T23:59:59.999999999Z",
                    "+1000000000-12-31T23:59:59+00:00:01","-1000000000-01-01T00:00:00Z",
                    "-1000000000-01-01T00:00:00-00:00:01","9999-12-31T23:59:59.999Z")
                    +p(new StringBuilder("2011-12-03T10:15:30Z"))+","+(Instant.parse("1970-01-01T00:00:00Z")==Instant.EPOCH)
                    +","+Instant.parse("2011-12-03T10:15:30.5Z").equals(Instant.ofEpochSecond(1322907330,500_000_000));
            }
            public static String syntaxErrors(){
                return all("","+","-"," 2011-12-03T10:15:30Z","2011-12-03T10:15Z","2011-12-03T10:15","2011-12-03T10:15:30",
                    "1970-01-01T","1970-01-01","2011-12-03X10:15:30Z","2011-12-03 10:15:30Z","2011-12-3T10:15:30Z",
                    "2011-1a-03T10:15:30Z","2011/12/03T10:15:30Z","2011-12-03T1:15:30Z","2011-12-03T10:1:30Z",
                    "2011-12-03T10:15:3Z","2011-12-03T10-15:30Z","2011-12-03T10:15:30,5Z","2011-12-03T10:15:30 Z",
                    "2011-12-03T10:15:30.1234567891Z","1970-01-01T00:00:00.123456789123Z","1970-01-01T00:00:00..1Z",
                    "1970-01-01T00:00:00.","2011-12-03T10:15:30.12a","2011-12-03T10:15:30+01","2011-12-03T10:15:30+0130",
                    "2011-12-03T10:15:30+1:00","2011-12-03T10:15:30+01:3","1970-01-01T00:00:00+","1970-01-01T00:00:00+00:",
                    "1970-01-01T00:00:00+00:0a","1970-01-01T00:00:00+01:60","1970-01-01T00:00:00+60:00",
                    "2011-12-03T10:15:30ZX","1970-01-01T00:00:00Zz","2011-12-03T10:15:30+01:00Z",
                    "2011-12-03T10:15:30+01:30:1","1970-01-01T00:00:00+00:00:","1970-01-01T00:00:00+00:00:0a",
                    "1970-01-01T00:00:00+01:00:60","12011-12-03T10:15:30Z","01970-01-01T00:00:00Z","+2011-12-03T10:15:30Z",
                    "+0000-01-01T00:00:00Z","-0000-01-01T00:00:00Z","201-12-03T10:15:30Z","-201-12-03T10:15:30Z",
                    "+-12-03T10:15:30Z","1000000000-01-01T00:00:00Z","+10000000000-01-01T00:00:00Z",
                    "-10000000000-01-01T00:00:00Z","\\uFF12\\uFF10\\uFF11\\uFF11-12-03T10:15:30Z","2011-12-03T10:15:30.\\u0661Z");
            }
            public static String valueErrors(){
                return all("2011-02-30T10:15:30Z","2011-02-29T00:00:00Z","1900-02-29T00:00:00Z","-0001-02-29T00:00:00Z",
                    "-10001-02-29T00:00:00Z","2011-13-03T10:15:30Z","2011-00-03T10:15:30Z","2011-12-00T10:15:30Z",
                    "2011-04-31T10:15:30Z","2011-12-32T10:15:30Z","2011-12-03T25:15:30Z","2011-12-03T10:60:30Z",
                    "2011-12-03T10:15:61Z","2011-12-03T22:59:60Z","2011-12-03T23:58:60Z","2011-12-03T24:00:01Z",
                    "2011-12-03T24:01:00Z","1999-12-31T24:00:00.000000001Z","2011-12-03T10:15:30+18:01",
                    "2011-12-03T10:15:30+19:00","1970-01-01T00:00:00+18:00:01","1970-01-01T00:00:00-18:00:01",
                    "1970-01-01T00:00:00+24:00","1970-01-01T00:00:00+59:59","1970-01-01T00:00:00-24:00:00",
                    "2011-13-03T10:15:30+24:00","2011-13-03T10:15:30ZZ","+1000000001-01-01T00:00:00Z",
                    "+9999999999-01-01T00:00:00Z","+1000000000-12-31T23:59:59-00:00:01",
                    "-1000000000-01-01T00:00:00+00:00:01","-9999999999-12-31T23:59:59Z")
                    +p("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")+'\\n'
                    +p("2011-12-03T10:15:30.123456789+01:30:15xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")+'\\n'
                    +p("+1000000001-01-01T00:00:00.000000000Zxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx".substring(0,37))+'\\n'
                    +p("1970-01-01T00:00:00Z1970-01-01T00:00:00Z1970-01-01T00:00:00Z1970-01-01")+'\\n'
                    +p(null);
            }
            public static String roundTrip(){
                StringBuilder o=new StringBuilder();
                long h=0;int n=0,bad=0;
                String[] offsets={"Z","+00:00","+05:30","-08:00","+13:45:30","-00:00:59","z"};
                int[] offsetSecs={0,0,19800,-28800,49530,-59,0};
                for(long s=-400_000_000_000L;s<=400_000_000_000L;s+=987_654_321L){
                    Instant i=Instant.ofEpochSecond(s,(int)((s>>>7)%1_000_000_000L));
                    String text=i.toString();
                    Instant back=Instant.parse(text);
                    if(!back.equals(i))bad++;
                    int k=n%offsets.length;
                    String shifted=text.substring(0,text.length()-1)+offsets[k];
                    Instant b2=Instant.parse(shifted);
                    if(b2.getEpochSecond()!=s-offsetSecs[k]||b2.getNano()!=i.getNano())bad++;
                    h=h*31+back.getEpochSecond()*7+b2.getNano();
                    n++;
                    if(n%97==0)o.append(shifted).append(' ');
                }
                for(long s=-31557014167219200L;s<31556889864403199L;s+=777_777_777_777_777L){
                    Instant i=Instant.ofEpochSecond(s,(int)(s&0x3FFFFFFF));
                    if(!Instant.parse(i.toString()).equals(i))bad++;
                    n++;
                }
                return o.append(h).append(',').append(n).append(',').append(bad).toString();
            }
        }
        """;

    @Test void parsesValidText() throws Exception {
        matchesJvm(FIXTURE, "valid");
    }

    @Test void reportsSyntaxErrorIndex() throws Exception {
        matchesJvm(FIXTURE, "syntaxErrors");
    }

    @Test void reportsValueErrors() throws Exception {
        matchesJvm(FIXTURE, "valueErrors");
    }

    @Test void roundTripsToString() throws Exception {
        matchesJvm(FIXTURE, "roundTrip");
    }

    @Test void leavesOutTemporalMembers() {
        rejectsUnusedCall("java/time/Instant", "from", "(Ljava/time/temporal/TemporalAccessor;)Ljava/time/Instant;", INVOKESTATIC, false);
        rejectsUnusedCall("java/time/Instant", "now", "()Ljava/time/Instant;", INVOKESTATIC, false);
        rejectsUnusedCall("java/time/Instant", "atOffset", "(Ljava/time/ZoneOffset;)Ljava/time/OffsetDateTime;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/time/format/DateTimeFormatter", "parse", "(Ljava/lang/CharSequence;)Ljava/time/temporal/TemporalAccessor;", INVOKEVIRTUAL, false);
    }
}
