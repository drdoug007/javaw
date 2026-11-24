package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T59: java.util.logging.Level. See tasks/T59-logging-level.md. */
class BytecodeLevelTest extends KitDifferential {
    // Level keeps one JVM-wide registry of every constructed level, so each fixture method uses its own names and
    // int values (no two methods share one) and never depends on levels created by another method.
    private static final String FIXTURE = """
        import java.util.logging.Level;
        class AppLevel extends Level {
            AppLevel(String name,int value){super(name,value);}
            AppLevel(String name,int value,String bundle){super(name,value,bundle);}
        }
        class LoggingLevel extends Level {
            final StringBuilder log=new StringBuilder();
            LoggingLevel(String name,int value){super(name,value);}
            public String getName(){log.append("name;");return "renamed";}
            public String getLocalizedName(){log.append("localized;");return super.getLocalizedName();}
            public String getResourceBundleName(){log.append("bundle;");return "other";}
            public int hashCode(){log.append("hash;");return 7;}
        }
        public class Fixture {
            static String name(Throwable t){return t.getClass().getName();}
            static String show(Level l){return l.getName()+"="+l.intValue();}

            public static String constants(){
                StringBuilder out=new StringBuilder();
                Level[] all={Level.OFF,Level.SEVERE,Level.WARNING,Level.INFO,Level.CONFIG,Level.FINE,Level.FINER,Level.FINEST,Level.ALL};
                for(Level l:all)out.append(show(l)).append(',').append(l).append(',').append(l.hashCode()).append(';');
                out.append(Level.OFF.intValue()==Integer.MAX_VALUE).append(',').append(Level.ALL.intValue()==Integer.MIN_VALUE).append(';');
                out.append(Level.INFO.getResourceBundleName()).append(',').append(Level.ALL.getResourceBundleName()).append(';');
                out.append(Level.INFO.getClass().getName()).append(',').append((Object)Level.INFO instanceof java.io.Serializable).append(';');
                out.append(Level.SEVERE.intValue()>Level.WARNING.intValue()).append(',').append(Level.FINE.intValue()<Level.CONFIG.intValue());
                return out.toString();
            }
            public static String equality(){
                StringBuilder out=new StringBuilder();
                Level a=new AppLevel("APP_EQ",800);
                Level b=new AppLevel("OTHER_EQ",801);
                out.append(a.equals(Level.INFO)).append(',').append(Level.INFO.equals(a)).append(',');
                out.append(a.equals(b)).append(',').append(a.equals(a)).append(',');
                out.append(a.equals(null)).append(',').append(a.equals("APP_EQ")).append(',').append(a.equals(Integer.valueOf(800))).append(',');
                out.append(a.hashCode()).append(',').append(b.hashCode()).append(',').append(Level.ALL.hashCode()).append(';');
                out.append(a).append(',').append(a.getName()).append(',').append(a.intValue()).append(',').append(a.getResourceBundleName()).append(',').append(a.getLocalizedName()).append(';');
                Level c=new AppLevel("BUNDLED_EQ",802,"no.such.Bundle");
                out.append(c.getResourceBundleName()).append(',').append(c.getLocalizedName()).append(',').append(c).append(';');
                out.append(Level.WARNING.equals(Level.SEVERE)).append(',').append(Level.OFF.equals(Level.OFF));
                return out.toString();
            }
            public static String constructorNulls(){
                StringBuilder out=new StringBuilder();
                try{new AppLevel(null,803);out.append("no;");}catch(NullPointerException e){out.append(name(e)).append(';');}
                try{new AppLevel(null,804,"b");out.append("no;");}catch(NullPointerException e){out.append(name(e)).append(';');}
                Level empty=new AppLevel("",805);
                out.append('[').append(empty).append("],").append(empty.intValue());
                return out.toString();
            }
            public static String parseNames(){
                StringBuilder out=new StringBuilder();
                String[] names={"OFF","SEVERE","WARNING","INFO","CONFIG","FINE","FINER","FINEST","ALL"};
                Level[] all={Level.OFF,Level.SEVERE,Level.WARNING,Level.INFO,Level.CONFIG,Level.FINE,Level.FINER,Level.FINEST,Level.ALL};
                for(int i=0;i<names.length;i++)out.append(Level.parse(names[i])==all[i]);
                out.append(';');
                String[] bad={"info","Info"," INFO","INFO ","NOT_A_LEVEL_PN","","0x10","1e3","12a","-"};
                for(String s:bad){
                    try{Level l=Level.parse(s);out.append("ok:").append(l).append(',');}
                    catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage()).append(',');}
                }
                try{Level.parse(null);out.append("no");}catch(NullPointerException e){out.append(name(e));}
                return out.toString();
            }
            public static String parseNumbers(){
                StringBuilder out=new StringBuilder();
                out.append(Level.parse("800")==Level.INFO).append(',').append(Level.parse("+1000")==Level.SEVERE).append(',');
                out.append(Level.parse("2147483647")==Level.OFF).append(',').append(Level.parse("-2147483648")==Level.ALL).append(',');
                out.append(Level.parse("0300")==Level.FINEST).append(';');
                Level made=Level.parse("806");
                out.append(show(made)).append(',').append(made.getClass().getName()).append(',').append(made.getResourceBundleName()).append(',').append(made.getLocalizedName()).append(',');
                out.append(Level.parse("806")==made).append(',').append(Level.parse("+806")==made).append(',').append(Level.parse("0806")==made).append(';');
                Level neg=Level.parse("-807");
                out.append(show(neg)).append(',').append(Level.parse("-807")==neg).append(',');
                Level padded=Level.parse("0808");
                out.append(show(padded)).append(',').append(Level.parse("808")==padded).append(',').append(Level.parse("0808")==padded).append(';');
                try{Level.parse("2147483648");out.append("no");}catch(IllegalArgumentException e){out.append(e.getMessage());}
                return out.toString();
            }
            public static String parseCustom(){
                StringBuilder out=new StringBuilder();
                Level first=new AppLevel("CUSTOM_PC",809);
                Level second=new AppLevel("CUSTOM_PC",810);
                Level byValue=new AppLevel("VALUE_PC",811);
                out.append(Level.parse("CUSTOM_PC")==first).append(',').append(Level.parse("809")==first).append(',');
                out.append(Level.parse("810")==second).append(',').append(Level.parse("811")==byValue).append(',');
                out.append(Level.parse("VALUE_PC")==byValue).append(';');
                Level twin=new AppLevel("WARNING_TWIN_PC",900);
                out.append(Level.parse("900")==Level.WARNING).append(',').append(Level.parse("WARNING_TWIN_PC")==twin).append(',');
                Level dup=new AppLevel("SEVERE",812);
                out.append(Level.parse("SEVERE")==Level.SEVERE).append(',').append(Level.parse("812")==dup).append(';');
                Level numeric=new AppLevel("813",814);
                out.append(Level.parse("813")==numeric).append(',').append(show(Level.parse("814"))).append(';');
                LoggingLevel logged=new LoggingLevel("LOGGED_PC",815);
                out.append(Level.parse("LOGGED_PC")==logged).append(',').append(Level.parse("815")==logged).append(',');
                try{Level.parse("renamed");out.append("no,");}catch(IllegalArgumentException e){out.append(e.getMessage()).append(',');}
                out.append(logged.log);
                return out.toString();
            }
            public static String overrides(){
                StringBuilder out=new StringBuilder();
                LoggingLevel l=new LoggingLevel("OVER_OV",816);
                out.append(l.log).append('|');
                out.append(l.toString()).append(',').append(l.intValue()).append(',').append(l.log).append('|');l.log.setLength(0);
                out.append(l.getLocalizedName()).append(',').append(l.log).append('|');l.log.setLength(0);
                out.append(l.equals(new AppLevel("OVER2_OV",816))).append(',').append(new AppLevel("OVER3_OV",816).equals(l)).append(',').append(l.log).append('|');l.log.setLength(0);
                out.append(l.getName()).append(',').append(l.getResourceBundleName()).append(',').append(l.hashCode()).append(',').append(l.log).append('|');l.log.setLength(0);
                out.append(Level.parse("816")==l).append(',').append(Level.parse("OVER_OV")==l).append(',').append(l.log);
                return out.toString();
            }
            public static String asKeys(){
                java.util.HashMap<Level,String> map=new java.util.HashMap<>();
                map.put(Level.INFO,"info");map.put(Level.FINE,"fine");
                map.put(new AppLevel("FINE_ALIAS_AK",500),"alias");
                StringBuilder out=new StringBuilder();
                out.append(map.size()).append(',').append(map.get(Level.FINE)).append(',').append(map.get(Level.parse("800"))).append(',');
                out.append(map.get(new AppLevel("NONE_AK",817))).append(';');
                Level[] order={Level.FINEST,Level.SEVERE,Level.ALL,Level.INFO,Level.OFF,Level.CONFIG};
                java.util.Arrays.sort(order,(x,y)->Integer.compare(x.intValue(),y.intValue()));
                for(Level l:order)out.append(l).append(' ');
                return out.toString();
            }
        }
        """;

    @Test void constantsAndEquality() throws Exception {
        matchesJvm(FIXTURE, "constants", "equality", "constructorNulls", "asKeys");
    }

    @Test void parsing() throws Exception {
        matchesJvm(FIXTURE, "parseNames", "parseNumbers", "parseCustom");
    }

    @Test void subclassOverrides() throws Exception {
        matchesJvm(FIXTURE, "overrides");
    }

    @Test void leavesOutLoggingFramework() {
        rejectsUnusedCall("java/util/logging/Logger", "getLogger", "(Ljava/lang/String;)Ljava/util/logging/Logger;", INVOKESTATIC, false);
        rejectsUnusedCall("java/util/logging/Level", "findLevel", "(Ljava/lang/String;)Ljava/util/logging/Level;", INVOKESTATIC, false);
        rejectsUnusedCall("java/util/logging/Level", "getLevelName", "()Ljava/lang/String;", INVOKEVIRTUAL, false);
    }
}
