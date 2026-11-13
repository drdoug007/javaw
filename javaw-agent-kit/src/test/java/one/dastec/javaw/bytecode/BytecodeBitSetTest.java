package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T10: java.util.BitSet single-bit, range, search and size operations. */
@Tag("agent-pending")
class BytecodeBitSetTest extends KitDifferential {
    @Test void matchesSingleBitOperations() throws Exception {
        matchesJvm("""
            import java.util.*;
            public class Fixture {
                public static String basics(){
                    BitSet bits=new BitSet();bits.set(1);bits.set(3);bits.set(5);bits.set(3);
                    return bits+"|"+bits.get(1)+bits.get(2)+bits.get(3)+bits.get(5)+bits.get(6)+"|"+bits.length()+"|"+bits.cardinality()+"|"+bits.isEmpty();
                }
                public static String emptyValue(){
                    BitSet bits=new BitSet();
                    return bits+"|"+bits.length()+"|"+bits.cardinality()+"|"+bits.isEmpty()+"|"+bits.get(0)+bits.get(1000)+"|"+bits.size();
                }
                public static String wordBoundaries(){
                    BitSet bits=new BitSet();int[] indexes={0,63,64,127,128,191,192};
                    for(int index:indexes)bits.set(index);
                    StringBuilder out=new StringBuilder(bits.toString());
                    for(int index:indexes)out.append('|').append(bits.get(index)).append(bits.get(index+1));
                    return out+"|"+bits.length()+"|"+bits.cardinality();
                }
                public static String setWithBoolean(){
                    BitSet bits=new BitSet();bits.set(4,true);bits.set(70,true);bits.set(9,false);bits.set(4,false);bits.set(80,false);
                    return bits+"|"+bits.length()+"|"+bits.cardinality()+"|"+bits.size();
                }
                public static String clearBits(){
                    BitSet bits=new BitSet();bits.set(2);bits.set(65);bits.set(130);
                    bits.clear(65);bits.clear(500);bits.clear(3);
                    String first=bits+"|"+bits.length();
                    bits.clear(130);String second=bits+"|"+bits.length();
                    bits.clear(2);
                    return first+"|"+second+"|"+bits+"|"+bits.length()+"|"+bits.isEmpty()+"|"+bits.cardinality();
                }
                public static String clearAll(){
                    BitSet bits=new BitSet();bits.set(10);bits.set(300);int before=bits.size();bits.clear();
                    return bits+"|"+bits.length()+"|"+bits.isEmpty()+"|"+before+"|"+bits.size()+"|"+bits.nextSetBit(0);
                }
                public static String flipBits(){
                    BitSet bits=new BitSet();bits.flip(3);bits.flip(100);bits.flip(3);bits.flip(64);
                    String text=bits+"|"+bits.length();
                    bits.flip(100);bits.flip(64);
                    return text+"|"+bits+"|"+bits.length()+"|"+bits.isEmpty();
                }
            }
            """,
            "basics","emptyValue","wordBoundaries","setWithBoolean","clearBits","clearAll","flipBits");
    }

    @Test void matchesRangeOperations() throws Exception {
        matchesJvm("""
            import java.util.*;
            public class Fixture {
                public static String setRange(){
                    BitSet bits=new BitSet();bits.set(2,6);
                    return bits+"|"+bits.length()+"|"+bits.cardinality();
                }
                public static String setRangeAcrossWords(){
                    BitSet bits=new BitSet();bits.set(60,130);
                    return bits.cardinality()+"|"+bits.length()+"|"+bits.get(59)+bits.get(60)+bits.get(63)+bits.get(64)+bits.get(127)+bits.get(128)+bits.get(129)+bits.get(130)+"|"+bits.nextClearBit(60);
                }
                public static String setRangeWholeWord(){
                    BitSet bits=new BitSet();bits.set(64,128);
                    return bits.cardinality()+"|"+bits.length()+"|"+bits.get(63)+bits.get(64)+bits.get(127)+bits.get(128)+"|"+bits.size();
                }
                public static String emptyRanges(){
                    BitSet bits=new BitSet();bits.set(7,7);bits.clear(3,3);bits.set(500,500);
                    return bits+"|"+bits.length()+"|"+bits.size();
                }
                public static String clearRange(){
                    BitSet bits=new BitSet();bits.set(0,200);bits.clear(10,190);
                    return bits.cardinality()+"|"+bits.length()+"|"+bits.get(9)+bits.get(10)+bits.get(189)+bits.get(190)+"|"+bits.nextSetBit(10);
                }
                public static String clearRangePastEnd(){
                    BitSet bits=new BitSet();bits.set(3);bits.set(70);bits.clear(5,1000);
                    String text=bits+"|"+bits.length();
                    bits.clear(400,900);bits.clear(0,4);
                    return text+"|"+bits+"|"+bits.length()+"|"+bits.isEmpty();
                }
                public static String clearRangeInsideWord(){
                    BitSet bits=new BitSet();bits.set(0,10);bits.clear(2,5);
                    return bits.toString();
                }
            }
            """,
            "setRange","setRangeAcrossWords","setRangeWholeWord","emptyRanges","clearRange","clearRangePastEnd","clearRangeInsideWord");
    }

    @Test void matchesSearches() throws Exception {
        matchesJvm("""
            import java.util.*;
            public class Fixture {
                static BitSet sample(){BitSet bits=new BitSet();for(int index:new int[]{0,5,63,64,65,127,200})bits.set(index);return bits;}
                public static String forwardSetBits(){
                    BitSet bits=sample();StringBuilder out=new StringBuilder();
                    for(int i=bits.nextSetBit(0);i>=0;i=bits.nextSetBit(i+1))out.append(i).append(',');
                    return out+"|"+bits.nextSetBit(66)+"|"+bits.nextSetBit(201)+"|"+bits.nextSetBit(5000);
                }
                public static String forwardClearBits(){
                    BitSet bits=sample();
                    return bits.nextClearBit(0)+","+bits.nextClearBit(5)+","+bits.nextClearBit(63)+","+bits.nextClearBit(127)+","+bits.nextClearBit(200)+","+bits.nextClearBit(201)+","+bits.nextClearBit(9999);
                }
                public static String fullWordClearBit(){
                    BitSet bits=new BitSet();bits.set(0,128);
                    return bits.nextClearBit(0)+","+bits.nextClearBit(70)+","+bits.previousClearBit(127)+","+bits.previousSetBit(500);
                }
                public static String backwardSetBits(){
                    BitSet bits=sample();StringBuilder out=new StringBuilder();
                    for(int i=bits.previousSetBit(5000);i>=0;i=bits.previousSetBit(i-1))out.append(i).append(',');
                    return out+"|"+bits.previousSetBit(62)+"|"+bits.previousSetBit(0)+"|"+bits.previousSetBit(-1);
                }
                public static String backwardClearBits(){
                    BitSet bits=sample();
                    return bits.previousClearBit(0)+","+bits.previousClearBit(5)+","+bits.previousClearBit(65)+","+bits.previousClearBit(66)+","+bits.previousClearBit(200)+","+bits.previousClearBit(9999)+","+bits.previousClearBit(-1);
                }
                public static String emptySearches(){
                    BitSet bits=new BitSet();
                    return bits.nextSetBit(0)+","+bits.nextSetBit(100)+","+bits.nextClearBit(0)+","+bits.nextClearBit(100)+","+bits.previousSetBit(100)+","+bits.previousClearBit(100)+","+bits.previousSetBit(-1)+","+bits.previousClearBit(-1);
                }
            }
            """,
            "forwardSetBits","forwardClearBits","fullWordClearBit","backwardSetBits","backwardClearBits","emptySearches");
    }

    @Test void matchesSizes() throws Exception {
        matchesJvm("""
            import java.util.*;
            public class Fixture {
                public static String initialSizes(){
                    return new BitSet().size()+","+new BitSet(0).size()+","+new BitSet(1).size()+","+new BitSet(64).size()+","+new BitSet(65).size()+","+new BitSet(128).size()+","+new BitSet(130).size();
                }
                public static String growth(){
                    BitSet a=new BitSet();a.set(100);
                    BitSet b=new BitSet();b.set(1000);
                    BitSet c=new BitSet(0);c.set(5);
                    BitSet d=new BitSet(130);d.set(200);
                    BitSet e=new BitSet();e.set(10,300);
                    BitSet f=new BitSet();f.flip(130);
                    return a.size()+","+b.size()+","+c.size()+","+d.size()+","+e.size()+","+f.size();
                }
                public static String noGrowth(){
                    BitSet bits=new BitSet();bits.clear(1000);bits.clear(500,2000);bits.set(700,false);bits.get(5000);
                    return bits.size()+","+bits.length()+","+bits.isEmpty();
                }
                public static String presizedIsEmpty(){
                    BitSet bits=new BitSet(500);
                    return bits+"|"+bits.length()+"|"+bits.isEmpty()+"|"+bits.cardinality()+"|"+bits.nextClearBit(0);
                }
            }
            """,
            "initialSizes","growth","noGrowth","presizedIsEmpty");
    }

    @Test void matchesFailures() throws Exception {
        matchesJvm("""
            import java.util.*;
            public class Fixture {
                static String failure(Runnable action){
                    try{action.run();return "none";}catch(RuntimeException e){return e.getClass().getSimpleName()+":"+e.getMessage();}
                }
                public static String negativeSize(){return failure(()->new BitSet(-1));}
                public static String negativeBitIndex(){
                    BitSet bits=new BitSet();
                    return failure(()->bits.get(-1))+"|"+failure(()->bits.set(-1))+"|"+failure(()->bits.set(-2,true))+"|"+failure(()->bits.clear(-1))+"|"+failure(()->bits.flip(-3));
                }
                public static String badRanges(){
                    BitSet bits=new BitSet();
                    return failure(()->bits.set(-1,4))+"|"+failure(()->bits.set(2,-1))+"|"+failure(()->bits.set(5,2))+"|"+failure(()->bits.clear(-1,4))+"|"+failure(()->bits.clear(3,-2))+"|"+failure(()->bits.clear(9,1))+"|"+failure(()->bits.set(-1,-5));
                }
                public static String badSearches(){
                    BitSet bits=new BitSet();bits.set(3);
                    return failure(()->bits.nextSetBit(-1))+"|"+failure(()->bits.nextClearBit(-1))+"|"+failure(()->bits.previousSetBit(-2))+"|"+failure(()->bits.previousClearBit(-2));
                }
                public static String failedCallsChangeNothing(){
                    BitSet bits=new BitSet();bits.set(1);bits.set(66);
                    failure(()->bits.set(5,2));failure(()->bits.clear(-1,70));failure(()->bits.set(-1));
                    return bits+"|"+bits.size();
                }
            }
            """,
            "negativeSize","negativeBitIndex","badRanges","badSearches","failedCallsChangeNothing");
    }

    @Test void rejectsUnlinkedMembers() {
        rejectsUnusedCall("java/util/BitSet","get","(II)Ljava/util/BitSet;",INVOKEVIRTUAL,false);
        rejectsUnusedCall("java/util/BitSet","stream","()Ljava/util/stream/IntStream;",INVOKEVIRTUAL,false);
    }
}
