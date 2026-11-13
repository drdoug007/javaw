package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T02: java.util.StringTokenizer. Every fixture method checks one behaviour so a failure names the broken member. */
class BytecodeStringTokenizerTest extends KitDifferential {
    private static final String HELPERS = """
            static String all(StringTokenizer t){
                StringBuilder out=new StringBuilder();
                while(t.hasMoreTokens())out.append('<').append(t.nextToken()).append('>');
                return out.toString();
            }
        """;

    private static final String SPLITTING = """
        import java.util.*;
        public class Fixture {
        """+HELPERS+"""
            public static String defaultDelimiters(){return all(new StringTokenizer("  alpha beta\\tgamma\\ndelta\\r\\fepsilon  "));}
            public static String defaultDelimitersAreOnlyFive(){return all(new StringTokenizer("a\\u000bb\\u00a0c_d"));}
            public static String customDelimiters(){return all(new StringTokenizer("a,b;;c,,;d",",;"));}
            public static String leadingAndTrailingDelimiters(){return all(new StringTokenizer(",,a,b,,",","));}
            public static String noDelimiterPresent(){return all(new StringTokenizer("whole-string",","));}
            public static String emptyDelimiterSet(){return all(new StringTokenizer("a b,c",""));}
            public static String emptyInput(){StringTokenizer t=new StringTokenizer("");return t.hasMoreTokens()+"|"+t.countTokens();}
            public static String onlyDelimiters(){StringTokenizer t=new StringTokenizer(" \\t\\n ");return t.hasMoreTokens()+"|"+t.countTokens();}
            public static String twoArgumentConstructorDoesNotReturnDelims(){return all(new StringTokenizer("x+y-z","+-"));}
            public static String threeArgumentFalse(){return all(new StringTokenizer("x+y-z","+-",false));}
            public static String singleCharacterTokens(){return all(new StringTokenizer("a b c d e f"));}
            public static String delimiterOrderDoesNotMatter(){return all(new StringTokenizer("1.2:3",":."))+all(new StringTokenizer("1.2:3",".:"));}
        }
        """;

    private static final String RETURNING_DELIMITERS = """
        import java.util.*;
        public class Fixture {
        """+HELPERS+"""
            public static String returnsEachDelimiterAsToken(){return all(new StringTokenizer("a,b;;c",",;",true));}
            public static String returnsLeadingAndTrailing(){return all(new StringTokenizer(",a,",",",true));}
            public static String returnsOnlyDelimiters(){return all(new StringTokenizer(",,,",",",true));}
            public static int countIncludesDelimiters(){return new StringTokenizer("a,b;;c",",;",true).countTokens();}
            public static String returnsRepeatedSpaceDelimiters(){return all(new StringTokenizer("x  y"," ",true));}
        }
        """;

    private static final String COUNTING = """
        import java.util.*;
        public class Fixture {
            public static int countAll(){return new StringTokenizer("one two  three\\tfour").countTokens();}
            public static String countDoesNotAdvance(){StringTokenizer t=new StringTokenizer("a b c");int first=t.countTokens();int second=t.countTokens();return first+","+second+","+t.nextToken();}
            public static String countDecreasesAsTokensAreRead(){
                StringTokenizer t=new StringTokenizer("a b c d");StringBuilder out=new StringBuilder();
                while(t.hasMoreTokens()){out.append(t.countTokens());t.nextToken();}
                return out.append(t.countTokens()).toString();
            }
            public static String hasMoreTokensDoesNotAdvance(){StringTokenizer t=new StringTokenizer("  a  b");t.hasMoreTokens();t.hasMoreTokens();return t.nextToken()+t.countTokens();}
            public static int countEmptyDelimiterSet(){return new StringTokenizer("abc","").countTokens();}
        }
        """;

    private static final String ENUMERATION = """
        import java.util.*;
        public class Fixture {
            public static String enumerationMethods(){
                StringTokenizer t=new StringTokenizer("p q r");StringBuilder out=new StringBuilder();
                while(t.hasMoreElements())out.append('[').append((String)t.nextElement()).append(']');
                return out.toString();
            }
            public static String throughEnumerationInterface(){
                Enumeration<Object> e=new StringTokenizer("1;2;3",";");StringBuilder out=new StringBuilder();
                while(e.hasMoreElements())out.append(e.nextElement()).append('/');
                return out.toString();
            }
            public static String asIteratorDefault(){
                Iterator<Object> it=new StringTokenizer("x y").asIterator();StringBuilder out=new StringBuilder();
                while(it.hasNext())out.append(it.next());
                return out.toString();
            }
            public static boolean isEnumeration(){Object t=new StringTokenizer("");return t instanceof Enumeration;}
            public static String mixedCalls(){StringTokenizer t=new StringTokenizer("a b c");return t.nextToken()+t.nextElement()+t.hasMoreElements()+t.nextToken()+t.hasMoreTokens();}
        }
        """;

    private static final String CHANGING_DELIMITERS = """
        import java.util.*;
        public class Fixture {
        """+HELPERS+"""
            public static String changeIsPermanent(){StringTokenizer t=new StringTokenizer("a b,c d,e");return t.nextToken()+"|"+t.nextToken(",")+"|"+all(t);}
            public static String changeStartsAtCurrentPosition(){StringTokenizer t=new StringTokenizer("a,b;c",",");String first=t.nextToken();return first+"|"+t.nextToken(";")+"|"+all(t);}
            public static String changeAfterHasMoreTokens(){
                StringTokenizer t=new StringTokenizer("a,b;c",",");String first=t.nextToken();boolean more=t.hasMoreTokens();
                return first+"|"+more+"|"+t.nextToken(";")+"|"+all(t);
            }
            public static String changeOnFreshTokenizer(){StringTokenizer t=new StringTokenizer("x-y z-w");return t.nextToken("-")+"|"+all(t);}
            public static String changeWithReturnDelims(){StringTokenizer t=new StringTokenizer("a,b;c",",",true);return t.nextToken()+"|"+t.nextToken(";")+"|"+all(t);}
            public static int countUsesChangedDelimiters(){StringTokenizer t=new StringTokenizer("a b,c d,e f");t.nextToken(",");return t.countTokens();}
            public static String changeToEmptyDelimiters(){StringTokenizer t=new StringTokenizer("a b c");return t.nextToken()+"|"+t.nextToken("")+"|"+t.hasMoreTokens();}
        }
        """;

    private static final String FAILURES = """
        import java.util.*;
        public class Fixture {
            public static String exhaustedNextToken(){
                StringTokenizer t=new StringTokenizer("one");t.nextToken();
                try{t.nextToken();return "no exception";}catch(NoSuchElementException e){return "NSEE:"+e.getMessage();}
            }
            public static String emptyNextToken(){
                try{new StringTokenizer("   ").nextToken();return "no exception";}catch(NoSuchElementException e){return "NSEE:"+e.getMessage();}
            }
            public static String exhaustedNextElement(){
                try{new StringTokenizer("").nextElement();return "no exception";}catch(NoSuchElementException e){return "NSEE";}
            }
            public static String exhaustedChangedDelimiter(){
                StringTokenizer t=new StringTokenizer("a,b");t.nextToken(",");t.nextToken();
                try{t.nextToken(";");return "no exception";}catch(NoSuchElementException e){return "NSEE";}
            }
            public static String nullString(){
                try{new StringTokenizer(null);return "no exception";}catch(NullPointerException e){return "NPE";}
            }
            public static String nullDelimitersFailLater(){
                StringTokenizer t=new StringTokenizer("a b",null);String out="created";
                try{t.hasMoreTokens();out+="|no exception";}catch(NullPointerException e){out+="|NPE";}
                try{t.nextToken();out+="|no exception";}catch(NullPointerException e){out+="|NPE";}
                return out;
            }
            public static String nullDelimitersEmptyInputCount(){return "count="+new StringTokenizer("",null).countTokens();}
            public static String nullDelimiterChange(){
                StringTokenizer t=new StringTokenizer("a b c");String out=t.nextToken();
                try{t.nextToken(null);out+="|no exception";}catch(NullPointerException e){out+="|NPE";}
                try{t.hasMoreTokens();out+="|no exception";}catch(NullPointerException e){out+="|NPE";}
                return out;
            }
        }
        """;

    @Test void splitsOnDelimiterCharacters() throws Exception {
        matchesJvm(SPLITTING,"defaultDelimiters","defaultDelimitersAreOnlyFive","customDelimiters","leadingAndTrailingDelimiters",
                "noDelimiterPresent","emptyDelimiterSet","emptyInput","onlyDelimiters","twoArgumentConstructorDoesNotReturnDelims",
                "threeArgumentFalse","singleCharacterTokens","delimiterOrderDoesNotMatter");
    }

    @Test void returnsDelimitersAsTokensWhenAsked() throws Exception {
        matchesJvm(RETURNING_DELIMITERS,"returnsEachDelimiterAsToken","returnsLeadingAndTrailing","returnsOnlyDelimiters",
                "countIncludesDelimiters","returnsRepeatedSpaceDelimiters");
    }

    @Test void countsRemainingTokensWithoutAdvancing() throws Exception {
        matchesJvm(COUNTING,"countAll","countDoesNotAdvance","countDecreasesAsTokensAreRead","hasMoreTokensDoesNotAdvance",
                "countEmptyDelimiterSet");
    }

    @Test void actsAsAnEnumeration() throws Exception {
        matchesJvm(ENUMERATION,"enumerationMethods","throughEnumerationInterface","asIteratorDefault","isEnumeration","mixedCalls");
    }

    @Test void switchesDelimitersWithNextTokenString() throws Exception {
        matchesJvm(CHANGING_DELIMITERS,"changeIsPermanent","changeStartsAtCurrentPosition","changeAfterHasMoreTokens",
                "changeOnFreshTokenizer","changeWithReturnDelims","countUsesChangedDelimiters","changeToEmptyDelimiters");
    }

    @Test void failsLikeTheJdk() throws Exception {
        matchesJvm(FAILURES,"exhaustedNextToken","emptyNextToken","exhaustedNextElement","exhaustedChangedDelimiter","nullString",
                "nullDelimitersFailLater","nullDelimitersEmptyInputCount","nullDelimiterChange");
    }
}
