package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T11: java.util.BitSet bulk operations, equality, hashing, cloning and array conversions. */
class BytecodeBitSetBulkTest extends KitDifferential {
    private static final String HELPERS = """
                static BitSet of(int... indexes){BitSet bits=new BitSet();for(int index:indexes)bits.set(index);return bits;}
                static String show(BitSet bits){return bits+" len="+bits.length()+" size="+bits.size();}
            """;

    @Test void matchesLogicalOperations() throws Exception {
        matchesJvm("""
            import java.util.*;
            public class Fixture {
            """+HELPERS+"""
                public static String orBasics(){
                    BitSet a=of(1,70),b=of(3,200);a.or(b);
                    return show(a)+"|"+b;
                }
                public static String orSmallerArgument(){
                    BitSet a=of(1,200),b=of(2);a.or(b);
                    return show(a)+"|"+b;
                }
                public static String orGrowth(){
                    BitSet a=new BitSet();a.or(of(130));
                    BitSet b=new BitSet(0);b.or(new BitSet());
                    BitSet c=new BitSet(0);c.or(of(5));
                    BitSet d=of(70);d.or(of(130));
                    BitSet e=of(70);e.xor(of(140));
                    return show(a)+"|"+show(b)+"|"+show(c)+"|"+show(d)+"|"+show(e);
                }
                public static String andBasics(){
                    BitSet a=of(1,3,70,200),b=of(3,70,71);a.and(b);
                    return show(a)+"|"+b;
                }
                public static String andShrinks(){
                    BitSet a=of(5,300);a.and(of(5));
                    BitSet b=of(5,300);b.and(new BitSet());
                    BitSet c=of(9);c.and(of(9,400));
                    return show(a)+"|"+show(b)+"|"+b.isEmpty()+"|"+show(c);
                }
                public static String xorBasics(){
                    BitSet a=of(1,2,130);a.xor(of(2,3,130));
                    BitSet b=of(1);b.xor(of(1,500));
                    BitSet c=of(4,64);c.xor(of(4,64));
                    return show(a)+"|"+show(b)+"|"+show(c)+"|"+c.isEmpty();
                }
                public static String andNotBasics(){
                    BitSet a=of(1,2,3,100);a.andNot(of(2,100,900));
                    BitSet b=of(7);b.andNot(new BitSet());
                    return show(a)+"|"+show(b);
                }
                public static String withItself(){
                    BitSet a=of(1,65);a.and(a);String first=a.toString();
                    a.or(a);String second=a.toString();
                    BitSet b=of(2,66);b.xor(b);
                    BitSet c=of(3,67);c.andNot(c);
                    return first+"|"+second+"|"+b+b.isEmpty()+"|"+c+c.isEmpty();
                }
                public static String intersects(){
                    BitSet a=of(1,100);
                    return ""+a.intersects(of(100))+a.intersects(of(2,99))+a.intersects(new BitSet())+new BitSet().intersects(a)
                        +a.intersects(of(1))+of(500).intersects(of(500,1))+of(500).intersects(of(400))+a.intersects(a);
                }
            }
            """,
            "orBasics","orSmallerArgument","orGrowth","andBasics","andShrinks","xorBasics","andNotBasics","withItself","intersects");
    }

    @Test void matchesEqualityHashingAndClone() throws Exception {
        matchesJvm("""
            import java.util.*;
            public class Fixture {
            """+HELPERS+"""
                public static String equality(){
                    BitSet big=new BitSet(1000);big.set(3);big.set(64);
                    BitSet small=of(3,64);
                    BitSet trimmed=of(3,64,900);trimmed.clear(900);
                    Object text="{3, 64}";
                    return ""+big.equals(small)+small.equals(big)+small.equals(trimmed)+small.equals(of(3))+small.equals(of(3,65))
                        +small.equals(small)+small.equals(null)+small.equals(text)+new BitSet().equals(new BitSet(500));
                }
                public static String hashCodes(){
                    BitSet trimmed=of(7,300);trimmed.clear(300);
                    return new BitSet().hashCode()+","+of(0).hashCode()+","+of(1,3,5).hashCode()+","+of(63).hashCode()+","+of(64).hashCode()
                        +","+of(0,64,128).hashCode()+","+of(63,127,1000).hashCode()+","+trimmed.hashCode()+","+of(7).hashCode();
                }
                public static String hashMatchesEquals(){
                    BitSet big=new BitSet(1000);big.set(70);BitSet small=of(70);
                    return big.equals(small)+"|"+(big.hashCode()==small.hashCode());
                }
                public static String cloneIsDeep(){
                    BitSet a=of(1,70);BitSet copy=(BitSet)a.clone();
                    String same=""+copy.equals(a)+(copy!=a)+copy;
                    copy.set(2);copy.clear(70);copy.set(500);a.set(3);
                    return same+"|"+a+"|"+copy;
                }
                public static String cloneOfEmpty(){
                    BitSet copy=(BitSet)new BitSet().clone();copy.set(9);
                    return copy+"|"+copy.length()+"|"+copy.equals(of(9));
                }
            }
            """,
            "equality","hashCodes","hashMatchesEquals","cloneIsDeep","cloneOfEmpty");
    }

    @Test void matchesArrayConversions() throws Exception {
        matchesJvm("""
            import java.util.*;
            public class Fixture {
            """+HELPERS+"""
                public static String toByteArrays(){
                    return Arrays.toString(new BitSet().toByteArray())+Arrays.toString(of(0).toByteArray())+Arrays.toString(of(7).toByteArray())
                        +Arrays.toString(of(8).toByteArray())+Arrays.toString(of(0,63).toByteArray())+Arrays.toString(of(64).toByteArray())
                        +Arrays.toString(of(1,2,3,20).toByteArray());
                }
                public static String toByteArrayAfterClear(){
                    BitSet bits=of(5,100);bits.clear(100);
                    return Arrays.toString(bits.toByteArray())+"|"+bits.toByteArray().length;
                }
                public static String toLongArrays(){
                    BitSet trimmed=of(1,300);trimmed.clear(300);
                    return Arrays.toString(new BitSet().toLongArray())+Arrays.toString(of(0,2).toLongArray())+Arrays.toString(of(63).toLongArray())
                        +Arrays.toString(of(64,130).toLongArray())+Arrays.toString(trimmed.toLongArray())+Arrays.toString(new BitSet(1000).toLongArray());
                }
                public static String arraysAreCopies(){
                    BitSet bits=of(1);long[] longs=bits.toLongArray();longs[0]=-1L;byte[] bytes=bits.toByteArray();bytes[0]=-1;
                    return bits.toString();
                }
                public static String valueOfLongs(){
                    return show(BitSet.valueOf(new long[]{5,0,0}))+"|"+show(BitSet.valueOf(new long[0]))+"|"+show(BitSet.valueOf(new long[]{0,1}))
                        +"|"+show(BitSet.valueOf(new long[]{Long.MIN_VALUE,-1L}))+"|"+show(BitSet.valueOf(new long[]{0,0}));
                }
                public static String valueOfLongsCopies(){
                    long[] source={6};BitSet bits=BitSet.valueOf(source);source[0]=0;bits.set(100);
                    return bits+"|"+Arrays.toString(source);
                }
                public static String valueOfBytes(){
                    return show(BitSet.valueOf(new byte[]{1,(byte)0x80,0,0}))+"|"+show(BitSet.valueOf(new byte[0]))
                        +"|"+show(BitSet.valueOf(new byte[]{0,0,0,0,0,0,0,0,1}))+"|"+show(BitSet.valueOf(new byte[]{-1}))+"|"+show(BitSet.valueOf(new byte[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,(byte)0x81}))
                        +"|"+show(BitSet.valueOf(new byte[]{0,0,0}))+"|"+show(BitSet.valueOf(new byte[]{0,0,0,0,0,0,0,(byte)0x80}));
                }
                public static String roundTrips(){
                    BitSet bits=of(0,9,63,64,77,190);
                    return BitSet.valueOf(bits.toLongArray()).equals(bits)+"|"+BitSet.valueOf(bits.toByteArray()).equals(bits)
                        +"|"+Arrays.toString(BitSet.valueOf(new byte[]{3,0,(byte)0xf0,0}).toByteArray());
                }
            }
            """,
            "toByteArrays","toByteArrayAfterClear","toLongArrays","arraysAreCopies","valueOfLongs","valueOfLongsCopies","valueOfBytes","roundTrips");
    }

    @Test void matchesNullArguments() throws Exception {
        matchesJvm("""
            import java.util.*;
            public class Fixture {
                static String failure(Runnable action){
                    try{action.run();return "none";}catch(RuntimeException e){return e.getClass().getSimpleName();}
                }
                public static String nullSets(){
                    BitSet bits=new BitSet();bits.set(4);
                    return failure(()->bits.or(null))+"|"+failure(()->bits.and(null))+"|"+failure(()->bits.xor(null))
                        +"|"+failure(()->bits.andNot(null))+"|"+failure(()->bits.intersects(null))+"|"+bits;
                }
                public static String nullArrays(){
                    return failure(()->BitSet.valueOf((long[])null))+"|"+failure(()->BitSet.valueOf((byte[])null));
                }
            }
            """,
            "nullSets","nullArrays");
    }

    @Test void rejectsUnlinkedMembers() {
        rejectsUnusedCall("java/util/BitSet","valueOf","(Ljava/nio/ByteBuffer;)Ljava/util/BitSet;",INVOKESTATIC,false);
        rejectsUnusedCall("java/util/BitSet","valueOf","(Ljava/nio/LongBuffer;)Ljava/util/BitSet;",INVOKESTATIC,false);
    }
}
