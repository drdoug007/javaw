package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T71: Enum.valueOf(Class, String) and every enum's valueOf(String). See tasks/T71-enum-value-of.md. */
class BytecodeEnumValueOfTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.concurrent.TimeUnit;
        import java.time.temporal.ChronoUnit;
        enum Color { RED, GREEN { public String toString(){return "green!";} }, BLUE }
        enum Empty { }
        public class Fixture {
            enum Mode { READ_ONLY, READ_WRITE }
            static String name(Throwable t){return t.getClass().getName();}
            static String full(Throwable t){return t.getClass().getName()+":"+t.getMessage();}

            public static String userEnums(){
                StringBuilder out=new StringBuilder();
                out.append(Color.valueOf("RED")==Color.RED).append(',');
                out.append(Color.valueOf("GREEN")==Color.GREEN).append(',');
                out.append(Color.valueOf("GREEN")).append(',');
                out.append(Color.valueOf("BLUE").ordinal()).append(',');
                out.append(Enum.valueOf(Color.class,"BLUE")==Color.BLUE).append(',');
                out.append(Mode.valueOf("READ_WRITE").ordinal()).append(',');
                out.append(Enum.valueOf(Mode.class,"READ_ONLY").name()).append(',');
                for(Color c:Color.values())out.append(Color.valueOf(c.name())==c);
                return out.toString();
            }
            public static String unknownNames(){
                StringBuilder out=new StringBuilder();
                try{Color.valueOf("PINK");out.append("no");}catch(IllegalArgumentException e){out.append(full(e));}
                out.append('|');
                try{Color.valueOf("red");out.append("no");}catch(IllegalArgumentException e){out.append(full(e));}
                out.append('|');
                try{Color.valueOf("");out.append("no");}catch(IllegalArgumentException e){out.append(full(e));}
                out.append('|');
                try{Color.valueOf("green!");out.append("no");}catch(IllegalArgumentException e){out.append(full(e));}
                out.append('|');
                try{Mode.valueOf("APPEND");out.append("no");}catch(IllegalArgumentException e){out.append(full(e));}
                out.append('|');
                try{Empty.valueOf("X");out.append("no");}catch(IllegalArgumentException e){out.append(full(e));}
                return out.toString();
            }
            public static String nullArguments(){
                StringBuilder out=new StringBuilder();
                try{Color.valueOf(null);out.append("no");}catch(NullPointerException e){out.append(full(e));}
                out.append('|');
                try{Enum.valueOf(Mode.class,null);out.append("no");}catch(NullPointerException e){out.append(full(e));}
                out.append('|');
                try{Enum.valueOf((Class<Color>)null,"RED");out.append("no");}catch(NullPointerException e){out.append(name(e));}
                return out.toString();
            }
            @SuppressWarnings({"unchecked","rawtypes"})
            public static String notAnEnum(){
                StringBuilder out=new StringBuilder();
                try{Enum.valueOf((Class)String.class,"RED");out.append("no");}catch(IllegalArgumentException e){out.append(full(e));}
                return out.toString();
            }
            public static String runtimeEnums(){
                StringBuilder out=new StringBuilder();
                for(TimeUnit u:TimeUnit.values())out.append(TimeUnit.valueOf(u.name())==u);
                out.append(',').append(TimeUnit.valueOf("SECONDS").toMillis(2)).append(',');
                for(ChronoUnit u:ChronoUnit.values())out.append(ChronoUnit.valueOf(u.name())==u);
                out.append(',').append(ChronoUnit.valueOf("HALF_DAYS")).append(',');
                out.append(Enum.valueOf(TimeUnit.class,"DAYS")==TimeUnit.DAYS).append(',');
                out.append(Enum.valueOf(ChronoUnit.class,"MILLIS")==ChronoUnit.MILLIS);
                return out.toString();
            }
            public static String runtimeFailures(){
                StringBuilder out=new StringBuilder();
                try{TimeUnit.valueOf("SECOND");out.append("no");}catch(IllegalArgumentException e){out.append(full(e));}
                out.append('|');
                try{TimeUnit.valueOf(null);out.append("no");}catch(NullPointerException e){out.append(full(e));}
                out.append('|');
                try{ChronoUnit.valueOf("Days");out.append("no");}catch(IllegalArgumentException e){out.append(full(e));}
                out.append('|');
                try{ChronoUnit.valueOf(null);out.append("no");}catch(NullPointerException e){out.append(full(e));}
                return out.toString();
            }
            static TimeUnit parseUnit(String text){
                try{return TimeUnit.valueOf(text.trim().toUpperCase());}catch(IllegalArgumentException e){return null;}
            }
            public static String asLibrariesUseIt(){
                StringBuilder out=new StringBuilder();
                String[] configured={"seconds"," MILLISECONDS ","minutes","fortnights","Hours"};
                for(String text:configured){TimeUnit unit=parseUnit(text);out.append(unit==null?"?":unit.name()).append(',');}
                Mode mode=Mode.valueOf("READ_ONLY");
                out.append(mode==Mode.READ_ONLY ? "ro" : "rw");
                return out.toString();
            }
        }
        """;

    @Test void userEnumValueOf() throws Exception {
        matchesJvm(FIXTURE, "userEnums", "asLibrariesUseIt");
    }

    @Test void failureMessages() throws Exception {
        matchesJvm(FIXTURE, "unknownNames", "nullArguments", "notAnEnum");
    }

    @Test void runtimeEnumValueOf() throws Exception {
        matchesJvm(FIXTURE, "runtimeEnums", "runtimeFailures");
    }
}
