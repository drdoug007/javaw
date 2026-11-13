package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * T03: ArrayList (and its subList view and iterators) failures must carry the exact JDK 17/21 exception messages.
 * Every fixture method returns "SimpleClassName:message" so a mismatch names the member and the message.
 */
@Tag("agent-pending")
class BytecodeArrayListIndexMessagesTest extends KitDifferential {
    private static final String HELPERS = """
            import java.util.*;
            public class Fixture {
                static String name(RuntimeException e){return e.getClass().getSimpleName()+":"+e.getMessage();}
                static List<String> two(){return new ArrayList<>(List.of("a","b"));}
                static List<String> view(){return new ArrayList<>(List.of("a","b","c","d")).subList(1,3);}
            """;

    private void check(String methods, String... names) throws Exception {
        matchesJvm(HELPERS + methods + "\n}\n", names);
    }

    @Test void getSetAndRemoveUseCheckIndexMessages() throws Exception {
        check("""
                public static String getEmpty(){try{new ArrayList<String>().get(0);return "none";}catch(RuntimeException e){return name(e);}}
                public static String getPastEnd(){try{two().get(2);return "none";}catch(RuntimeException e){return name(e);}}
                public static String getNegative(){try{two().get(-1);return "none";}catch(RuntimeException e){return name(e);}}
                public static String getMinValue(){try{two().get(Integer.MIN_VALUE);return "none";}catch(RuntimeException e){return name(e);}}
                public static String setPastEnd(){List<String> l=two();try{l.set(2,"x");return "none";}catch(RuntimeException e){return name(e)+"|"+l;}}
                public static String setNegative(){List<String> l=two();try{l.set(-1,"x");return "none";}catch(RuntimeException e){return name(e)+"|"+l;}}
                public static String removePastEnd(){List<String> l=two();try{l.remove(2);return "none";}catch(RuntimeException e){return name(e)+"|"+l;}}
                public static String removeEmpty(){try{new ArrayList<String>().remove(0);return "none";}catch(RuntimeException e){return name(e);}}
                public static String sizeAfterGrowth(){List<String> l=two();l.add("c");l.add("d");l.remove(0);try{l.get(3);return "none";}catch(RuntimeException e){return name(e);}}
                public static String inBounds(){List<String> l=two();return l.get(1)+l.set(0,"z")+l.remove(1)+l;}
                """,
                "getEmpty", "getPastEnd", "getNegative", "getMinValue", "setPastEnd", "setNegative", "removePastEnd",
                "removeEmpty", "sizeAfterGrowth", "inBounds");
    }

    @Test void addAtIndexAndAddAllAtIndexUseRangeCheckForAddMessages() throws Exception {
        check("""
                public static String addAtPastEnd(){List<String> l=two();try{l.add(5,"x");return "none";}catch(RuntimeException e){return name(e)+"|"+l;}}
                public static String addAtNegative(){List<String> l=two();try{l.add(-1,"x");return "none";}catch(RuntimeException e){return name(e)+"|"+l;}}
                public static String addAtEnd(){List<String> l=two();l.add(2,"x");return l.toString();}
                public static String addAllAtPastEnd(){List<String> l=two();try{l.addAll(5,List.of("z"));return "none";}catch(RuntimeException e){return name(e)+"|"+l;}}
                public static String addAllAtNegativeEmpty(){List<String> l=two();try{l.addAll(-1,new ArrayList<String>());return "none";}catch(RuntimeException e){return name(e)+"|"+l;}}
                public static String addAllAtNullCollection(){List<String> l=two();try{l.addAll(3,null);return "none";}catch(RuntimeException e){return name(e);}}
                public static String addAllAtEnd(){List<String> l=two();return l.addAll(2,List.of("y"))+""+l;}
                """,
                "addAtPastEnd", "addAtNegative", "addAtEnd", "addAllAtPastEnd", "addAllAtNegativeEmpty",
                "addAllAtNullCollection", "addAllAtEnd");
    }

    @Test void listIteratorAtIndexUsesRangeCheckForAddMessage() throws Exception {
        check("""
                public static String listIteratorPastEnd(){try{two().listIterator(7);return "none";}catch(RuntimeException e){return name(e);}}
                public static String listIteratorNegative(){try{two().listIterator(-1);return "none";}catch(RuntimeException e){return name(e);}}
                public static String listIteratorAtEnd(){ListIterator<String> it=two().listIterator(2);return it.hasNext()+","+it.previous()+","+it.nextIndex();}
                """,
                "listIteratorPastEnd", "listIteratorNegative", "listIteratorAtEnd");
    }

    @Test void subListRangeChecksUseJdkMessagesInJdkOrder() throws Exception {
        check("""
                public static String subListNegativeFrom(){try{two().subList(-1,1);return "none";}catch(RuntimeException e){return name(e);}}
                public static String subListToPastEnd(){try{two().subList(0,5);return "none";}catch(RuntimeException e){return name(e);}}
                public static String subListFromAfterTo(){try{two().subList(2,1);return "none";}catch(RuntimeException e){return name(e);}}
                public static String subListBothBad(){try{two().subList(3,5);return "none";}catch(RuntimeException e){return name(e);}}
                public static String subListFromPastSize(){try{two().subList(3,2);return "none";}catch(RuntimeException e){return name(e);}}
                public static String subListNegativeAndInverted(){try{two().subList(-2,-3);return "none";}catch(RuntimeException e){return name(e);}}
                public static String subListEmptyAtEnd(){return two().subList(2,2).toString();}
                """,
                "subListNegativeFrom", "subListToPastEnd", "subListFromAfterTo", "subListBothBad",
                "subListFromPastSize", "subListNegativeAndInverted", "subListEmptyAtEnd");
    }

    @Test void subListViewUsesItsOwnSizeInMessages() throws Exception {
        check("""
                public static String viewGet(){try{view().get(2);return "none";}catch(RuntimeException e){return name(e);}}
                public static String viewSet(){try{view().set(-1,"x");return "none";}catch(RuntimeException e){return name(e);}}
                public static String viewRemove(){try{view().remove(2);return "none";}catch(RuntimeException e){return name(e);}}
                public static String viewAdd(){try{view().add(3,"x");return "none";}catch(RuntimeException e){return name(e);}}
                public static String viewAddAll(){try{view().addAll(4,List.of("x"));return "none";}catch(RuntimeException e){return name(e);}}
                public static String viewListIterator(){try{view().listIterator(3);return "none";}catch(RuntimeException e){return name(e);}}
                public static String viewSubList(){try{view().subList(0,3);return "none";}catch(RuntimeException e){return name(e);}}
                public static String viewSubListInverted(){try{view().subList(2,1);return "none";}catch(RuntimeException e){return name(e);}}
                public static String nestedViewGet(){try{new ArrayList<>(List.of("a","b","c","d","e")).subList(1,4).subList(1,2).get(1);return "none";}catch(RuntimeException e){return name(e);}}
                public static String viewAfterParentAdd(){List<String> p=two();List<String> v=p.subList(0,1);p.add("c");try{v.get(0);return "none";}catch(RuntimeException e){return name(e);}}
                """,
                "viewGet", "viewSet", "viewRemove", "viewAdd", "viewAddAll", "viewListIterator", "viewSubList",
                "viewSubListInverted", "nestedViewGet", "viewAfterParentAdd");
    }

    @Test void iteratorFailuresKeepNullMessages() throws Exception {
        check("""
                public static String iteratorNextEmpty(){try{new ArrayList<String>().iterator().next();return "none";}catch(RuntimeException e){return name(e);}}
                public static String iteratorRemoveFirst(){try{two().iterator().remove();return "none";}catch(RuntimeException e){return name(e);}}
                public static String listIteratorPreviousAtStart(){try{two().listIterator().previous();return "none";}catch(RuntimeException e){return name(e);}}
                public static String listIteratorSetFirst(){try{two().listIterator().set("x");return "none";}catch(RuntimeException e){return name(e);}}
                public static String iteratorAfterAdd(){List<String> l=two();Iterator<String> it=l.iterator();l.add("c");try{it.next();return "none";}catch(RuntimeException e){return name(e);}}
                """,
                "iteratorNextEmpty", "iteratorRemoveFirst", "listIteratorPreviousAtStart", "listIteratorSetFirst",
                "iteratorAfterAdd");
    }

    @Test void negativeCapacityMessage() throws Exception {
        check("""
                public static String negativeCapacity(){try{new ArrayList<String>(-1);return "none";}catch(RuntimeException e){return name(e);}}
                public static String zeroCapacity(){List<String> l=new ArrayList<>(0);l.add("a");return l.toString();}
                """,
                "negativeCapacity", "zeroCapacity");
    }

    @Test void leavesOutRemoveRangeAndObjectsCheckIndex() {
        rejectsUnusedCall("java/util/ArrayList", "removeRange", "(II)V", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/Objects", "checkIndex", "(II)I", INVOKESTATIC, false);
    }
}
