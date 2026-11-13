package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T01: java.util.StringJoiner. Every fixture method checks one behaviour so a failure names the broken member. */
@Tag("agent-pending")
class BytecodeStringJoinerTest extends KitDifferential {
    private static final String BUILDING = """
        import java.util.*;
        public class Fixture {
            public static String delimiterOnly(){StringJoiner j=new StringJoiner(", ");j.add("a");j.add("b");j.add("c");return j.toString();}
            public static String singleElement(){StringJoiner j=new StringJoiner("-");j.add("only");return j.toString();}
            public static String emptyNoAffixes(){return "<"+new StringJoiner(",").toString()+">";}
            public static String emptyWithAffixes(){return new StringJoiner(",","[","]").toString();}
            public static String withAffixes(){StringJoiner j=new StringJoiner(",","[","]");j.add("x");String one=j.toString();j.add("y");return one+"|"+j;}
            public static String emptyStringsAreElements(){StringJoiner j=new StringJoiner(",","{","}");j.add("");j.add("");return j.toString();}
            public static String addNull(){StringJoiner j=new StringJoiner("/");j.add(null);j.add("b");j.add(null);return j.toString();}
            public static String addReturnsThis(){StringJoiner j=new StringJoiner("+");return (j.add("a")==j)+"|"+j.add("b").add("c");}
            public static String charSequenceArguments(){
                StringJoiner j=new StringJoiner(new StringBuilder("::"),new StringBuilder("<"),new StringBuilder(">"));
                j.add(new StringBuilder("sb"));j.add("str");return j.toString();
            }
            public static String defensiveCopies(){
                StringBuilder delimiter=new StringBuilder(","),prefix=new StringBuilder("("),element=new StringBuilder("e");
                StringJoiner j=new StringJoiner(delimiter,prefix,")");j.add(element);
                delimiter.append("!");prefix.append("!");element.append("!");j.add("f");
                return j.toString();
            }
            public static String toStringRepeatsAndContinues(){StringJoiner j=new StringJoiner(";");j.add("1");String a=j.toString();String b=j.toString();j.add("2");return a+b+"|"+j;}
            public static String manyElements(){StringJoiner j=new StringJoiner(",","[","]");for(int i=0;i<40;i++)j.add(Integer.toString(i));return j.toString();}
        }
        """;

    private static final String EMPTY_VALUE = """
        import java.util.*;
        public class Fixture {
            public static String emptyValueUsed(){StringJoiner j=new StringJoiner(",","[","]");j.setEmptyValue("EMPTY");return j.toString();}
            public static String emptyValueIgnoresAffixes(){StringJoiner j=new StringJoiner(",","[","]").setEmptyValue("");return "<"+j+">";}
            public static String emptyValueReplacedByElement(){StringJoiner j=new StringJoiner(",","[","]");j.setEmptyValue("EMPTY");j.add("");return j.toString();}
            public static String emptyValueReturnsThis(){StringJoiner j=new StringJoiner(",");return Boolean.toString(j.setEmptyValue("e")==j);}
            public static String emptyValueSetTwice(){StringJoiner j=new StringJoiner(",");j.setEmptyValue("first");j.setEmptyValue("second");return j.toString();}
            public static String emptyValueCopied(){StringBuilder value=new StringBuilder("none");StringJoiner j=new StringJoiner(",");j.setEmptyValue(value);value.append("!");return j.toString();}
            public static String emptyValueAfterAdd(){StringJoiner j=new StringJoiner(",","<",">");j.add("a");j.setEmptyValue("EMPTY");return j.toString();}
        }
        """;

    private static final String LENGTHS = """
        import java.util.*;
        public class Fixture {
            public static int lengthEmpty(){return new StringJoiner(",").length();}
            public static int lengthEmptyWithAffixes(){return new StringJoiner(",","[[","]]]").length();}
            public static int lengthEmptyValue(){StringJoiner j=new StringJoiner(",","[","]");j.setEmptyValue("nothing");return j.length();}
            public static int lengthOneElement(){StringJoiner j=new StringJoiner(", ","[","]");j.add("abc");return j.length();}
            public static int lengthSeveral(){StringJoiner j=new StringJoiner(", ","[","]");j.add("abc");j.add("");j.add(null);return j.length();}
            public static int lengthEmptyValueThenAdd(){StringJoiner j=new StringJoiner("--","(",")");j.setEmptyValue("nothing");j.add("x");j.add("y");return j.length();}
            public static boolean lengthMatchesToString(){
                StringJoiner j=new StringJoiner("::","<",">");
                for(int i=0;i<12;i++){if(j.length()!=j.toString().length())return false;j.add("v"+i);}
                return j.length()==j.toString().length();
            }
        }
        """;

    private static final String MERGING = """
        import java.util.*;
        public class Fixture {
            static StringJoiner left(){StringJoiner j=new StringJoiner(",","[","]");j.add("a");j.add("b");return j;}
            static StringJoiner right(){StringJoiner j=new StringJoiner("-","{","}");j.add("x");j.add("y");return j;}
            public static String mergeUsesOtherDelimiterNoAffixes(){StringJoiner a=left();a.merge(right());return a.toString();}
            public static String mergeReturnsThis(){StringJoiner a=left();return Boolean.toString(a.merge(right())==a);}
            public static String mergeLeavesOtherUsable(){StringJoiner a=left(),b=right();a.merge(b);String before=b.toString();b.add("z");return before+"|"+b+"|"+a;}
            public static String mergeIntoEmpty(){StringJoiner a=new StringJoiner(",","[","]");a.setEmptyValue("EMPTY");a.merge(right());return a.toString();}
            public static String mergeEmptyOther(){StringJoiner a=left();a.merge(new StringJoiner("-","{","}"));return a.toString();}
            public static String mergeEmptyOtherWithEmptyValue(){StringJoiner a=left();StringJoiner b=new StringJoiner("-");b.setEmptyValue("EMPTY");a.merge(b);return a.toString();}
            public static String mergeEmptyIntoEmpty(){StringJoiner a=new StringJoiner(",");a.setEmptyValue("still empty");a.merge(new StringJoiner(","));return a.toString();}
            public static String mergeSingleElement(){StringJoiner a=left();StringJoiner b=new StringJoiner("-");b.add("one");a.merge(b);return a.toString();}
            public static String mergeSelf(){StringJoiner a=left();a.merge(a);return a.toString()+"|"+a.length();}
            public static String mergeThenAdd(){StringJoiner a=left();a.merge(right());a.add("c");return a.toString()+"|"+a.length();}
            public static String mergeOtherWithEmptyElement(){StringJoiner a=left();StringJoiner b=new StringJoiner("-");b.add("");a.merge(b);return a.toString();}
        }
        """;

    private static final String FAILURES = """
        import java.util.*;
        public class Fixture {
            static String npe(Runnable action){
                try{action.run();return "no exception";}
                // A null message must not print as the text "null", or the two would look the same.
                catch(NullPointerException e){return e.getMessage()==null?"NPE without message":"NPE:"+e.getMessage();}
            }
            public static String nullDelimiter(){return npe(()->new StringJoiner(null));}
            public static String nullDelimiterWithAffixes(){return npe(()->new StringJoiner(null,"[","]"));}
            public static String nullPrefix(){return npe(()->new StringJoiner(",",null,"]"));}
            public static String nullSuffix(){return npe(()->new StringJoiner(",","[",null));}
            public static String allNullChecksPrefixFirst(){return npe(()->new StringJoiner(null,null,null));}
            public static String delimiterAndSuffixNull(){return npe(()->new StringJoiner(null,"[",null));}
            public static String nullEmptyValue(){return npe(()->new StringJoiner(",").setEmptyValue(null));}
            public static String nullMerge(){return npe(()->new StringJoiner(",").merge(null));}
            public static String failedSetEmptyValueKeepsOld(){
                StringJoiner j=new StringJoiner(",");j.setEmptyValue("old");
                try{j.setEmptyValue(null);}catch(NullPointerException e){}
                return j.toString();
            }
        }
        """;

    @Test void buildsJoinedText() throws Exception {
        matchesJvm(BUILDING,"delimiterOnly","singleElement","emptyNoAffixes","emptyWithAffixes","withAffixes",
                "emptyStringsAreElements","addNull","addReturnsThis","charSequenceArguments","defensiveCopies",
                "toStringRepeatsAndContinues","manyElements");
    }

    @Test void usesEmptyValueOnlyWhileNothingWasAdded() throws Exception {
        matchesJvm(EMPTY_VALUE,"emptyValueUsed","emptyValueIgnoresAffixes","emptyValueReplacedByElement",
                "emptyValueReturnsThis","emptyValueSetTwice","emptyValueCopied","emptyValueAfterAdd");
    }

    @Test void reportsLengthOfCurrentValue() throws Exception {
        matchesJvm(LENGTHS,"lengthEmpty","lengthEmptyWithAffixes","lengthEmptyValue","lengthOneElement","lengthSeveral",
                "lengthEmptyValueThenAdd","lengthMatchesToString");
    }

    @Test void mergesAnotherJoinerAsOneElement() throws Exception {
        matchesJvm(MERGING,"mergeUsesOtherDelimiterNoAffixes","mergeReturnsThis","mergeLeavesOtherUsable","mergeIntoEmpty",
                "mergeEmptyOther","mergeEmptyOtherWithEmptyValue","mergeEmptyIntoEmpty","mergeSingleElement","mergeSelf",
                "mergeThenAdd","mergeOtherWithEmptyElement");
    }

    @Test void rejectsNullArgumentsWithJdkMessages() throws Exception {
        matchesJvm(FAILURES,"nullDelimiter","nullDelimiterWithAffixes","nullPrefix","nullSuffix","allNullChecksPrefixFirst",
                "delimiterAndSuffixNull","nullEmptyValue","nullMerge","failedSetEmptyValueKeepsOld");
    }
}
