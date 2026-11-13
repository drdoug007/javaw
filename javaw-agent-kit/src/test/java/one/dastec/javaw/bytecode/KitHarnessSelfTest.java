package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Proves the kit harness works on APIs the runtime already supports. Always runs; never tagged pending. */
class KitHarnessSelfTest extends KitDifferential {
    @Test void matchesSupportedCollectionsAndText() throws Exception {
        matchesJvm("""
            import java.util.*;
            public class Fixture {
                public static String text(){
                    List<String> list=new ArrayList<>(List.of("b","a","c"));Collections.sort(list);
                    StringBuilder out=new StringBuilder();for(String s:list)out.append(s).append(',');
                    return out+"|"+list.size()+"|\\u00e9\\u4e2d";
                }
                public static int number(){int sum=0;for(int i=0;i<10;i++)sum+=i*i;return sum;}
                public static long wide(){return Long.MAX_VALUE-7;}
                public static boolean flag(){return "abc".contains("b");}
                public static char letter(){return "xyz".charAt(1);}
                public static String none(){return null;}
                public static int failure(){try{new ArrayList<String>().get(0);return 0;}catch(IndexOutOfBoundsException e){return 7;}}
            }
            """,
            "text","number","wide","flag","letter","none","failure");
    }

    @Test void reportsMissingRuntimeOwnersAsRuntimeWork() {
        AssertionError failure=assertThrows(AssertionError.class,()->matchesJvm("""
            public class Fixture {
                public static int weak(){return new java.util.WeakHashMap<String,String>().size();}
            }
            ""","weak"));
        assertTrue(failure.getMessage().startsWith("FIX THE RUNTIME"),failure.getMessage());
    }

    @Test void rejectsCallsToUnlinkedMembers() {
        rejectsUnusedCall("java/util/KitNoSuchOwner","size","()I",INVOKEVIRTUAL,false);
        rejectsUnusedCall("java/util/KitNoSuchOwner","of","(Ljava/lang/String;J)Ljava/lang/Object;",INVOKESTATIC,false);
    }

    @Test void detectsLinkedMembersThatShouldBeRejected() {
        assertThrows(AssertionError.class,()->rejectsUnusedCall("java/util/ArrayList","size","()I",INVOKEVIRTUAL,false));
    }
}
