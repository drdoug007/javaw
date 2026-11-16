package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T45: java.util.Random with an explicit seed. Expected values come from the JVM; see tasks/T45-seeded-random.md. */
class BytecodeSeededRandomTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.Random;

        /** Records every overridable call that Random makes on itself, then delegates to Random. */
        class Spy extends Random{
            static StringBuilder log=new StringBuilder();
            Spy(long seed){super(seed);}
            public void setSeed(long seed){log.append("S").append(seed).append(' ');super.setSeed(seed);}
            protected int next(int bits){log.append("n").append(bits).append(' ');return super.next(bits);}
            public int nextInt(){log.append("I ");return super.nextInt();}
            public double nextDouble(){log.append("D ");return super.nextDouble();}
        }
        /** setSeed does not reach Random.setSeed, so the seed stays as Random(long) left it for a subclass. */
        class NoSeed extends Random{
            NoSeed(long seed){super(seed);}
            public void setSeed(long seed){}
        }
        /** next(bits) returns scripted values in order (ignoring bits) and logs the bits asked for. */
        class Script extends Random{
            static StringBuilder log=new StringBuilder();
            int[] values;int index;
            Script(int... values){super(7L);this.values=values;}
            protected int next(int bits){log.append(bits).append(' ');return values[index++];}
        }

        public class Fixture {
            interface Op0{Object run() throws Exception;}
            /** The value, or "SimpleName:message" of the exception. */
            static String t(Op0 op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            static String d(double v){return Long.toHexString(Double.doubleToLongBits(v));}
            static String f(float v){return Integer.toHexString(Float.floatToIntBits(v));}
            static String ints(Random r,int n){StringBuilder b=new StringBuilder();for(int i=0;i<n;i++)b.append(r.nextInt()).append(' ');return b.toString();}

            // ---- the LCG ----
            public static String seedSequences(){
                StringBuilder b=new StringBuilder();
                long[] seeds={0L,1L,42L,-1L,Long.MIN_VALUE,Long.MAX_VALUE,0x5DEECE66DL,1L<<48,123456789012345L};
                for(long s:seeds)b.append(ints(new Random(s),4)).append("| ");
                return b.toString();
            }
            public static String setSeedRestarts(){
                Random r=new Random(5L);String first=ints(r,3);
                r.setSeed(5L);String again=ints(r,3);
                r.setSeed(5L | (7L<<48));String high=ints(r,3);
                Random other=new Random(99L);other.nextInt();other.setSeed(5L);
                return first+"| "+again+"| "+high+"| "+ints(other,3);
            }
            public static String longsAndBooleans(){
                Random r=new Random(42L);StringBuilder b=new StringBuilder();
                for(int i=0;i<4;i++)b.append(r.nextLong()).append(' ');
                for(int i=0;i<16;i++)b.append(r.nextBoolean()?'1':'0');
                return b.append(' ').append(new Random(-7L).nextLong()).toString();
            }
            public static String floatsAndDoubles(){
                Random r=new Random(2024L);StringBuilder b=new StringBuilder();
                for(int i=0;i<4;i++)b.append(f(r.nextFloat())).append(' ');
                for(int i=0;i<4;i++)b.append(d(r.nextDouble())).append(' ');
                Random z=new Random(0L);
                return b.append(f(z.nextFloat())).append(' ').append(d(z.nextDouble())).toString();
            }

            // ---- bounded ints ----
            public static String boundedInts(){
                Random r=new Random(11L);StringBuilder b=new StringBuilder();
                int[] bounds={1,2,3,7,10,16,100,1<<30,(1<<30)+1,Integer.MAX_VALUE,1000000007};
                for(int bound:bounds){b.append(bound).append(':');for(int i=0;i<3;i++)b.append(r.nextInt(bound)).append(',');b.append(' ');}
                b.append(t(()->r.nextInt(0))).append(' ').append(t(()->r.nextInt(-1))).append(' ').append(t(()->r.nextInt(Integer.MIN_VALUE)));
                return b.append(' ').append(r.nextInt()).toString();
            }
            public static String rangeInts(){
                Random r=new Random(3L);StringBuilder b=new StringBuilder();
                int[][] ranges={{0,10},{-5,5},{5,6},{-8,8},{100,1124},{Integer.MIN_VALUE,Integer.MAX_VALUE},
                    {Integer.MIN_VALUE,0},{-1,Integer.MAX_VALUE},{-2,Integer.MAX_VALUE},{Integer.MAX_VALUE-1,Integer.MAX_VALUE}};
                for(int[] g:ranges){b.append(g[0]).append("..").append(g[1]).append(':');for(int i=0;i<3;i++)b.append(r.nextInt(g[0],g[1])).append(',');b.append(' ');}
                b.append(t(()->r.nextInt(1,1))).append(' ').append(t(()->r.nextInt(2,1))).append(' ').append(t(()->r.nextInt(Integer.MAX_VALUE,Integer.MIN_VALUE)));
                return b.append(' ').append(r.nextInt()).toString();
            }

            // ---- gaussian ----
            public static String gaussians(){
                Random r=new Random(8L);StringBuilder b=new StringBuilder();
                for(int i=0;i<5;i++)b.append(d(r.nextGaussian())).append(' ');
                Random a=new Random(8L);String g1=d(a.nextGaussian());a.setSeed(8L);
                b.append(g1.equals(d(a.nextGaussian()))).append(' ');
                Random c=new Random(8L);c.nextGaussian();int mid=c.nextInt();
                b.append(mid).append(' ').append(d(c.nextGaussian())).append(' ').append(c.nextInt());
                Random e=new Random(8L);e.nextGaussian();e.setSeed(9L);
                return b.append(' ').append(d(e.nextGaussian())).append(' ').append(d(new Random(9L).nextGaussian())).toString();
            }

            // ---- bytes ----
            public static String bytes(){
                Random r=new Random(77L);StringBuilder b=new StringBuilder();
                for(int len:new int[]{0,1,3,4,5,9}){byte[] a=new byte[len];r.nextBytes(a);for(byte x:a)b.append(x).append(',');b.append("| ");}
                try{r.nextBytes(null);b.append("ok");}catch(NullPointerException e){b.append("NPE");}
                return b.append(' ').append(r.nextInt()).toString();
            }

            // ---- subclasses ----
            public static String spyCalls(){
                Spy.log.setLength(0);StringBuilder b=new StringBuilder();
                Spy s=new Spy(42L);b.append("new[").append(Spy.log).append("] ");
                String[] names={"nextInt","nextInt(5)","nextInt(16)","nextInt(1,4)","nextLong","nextBoolean","nextFloat","nextDouble","nextGaussian","nextGaussian","nextBytes(5)","setSeed"};
                for(String name:names){
                    Spy.log.setLength(0);
                    if(name.equals("nextInt"))s.nextInt();else if(name.equals("nextInt(5)"))s.nextInt(5);else if(name.equals("nextInt(16)"))s.nextInt(16);
                    else if(name.equals("nextInt(1,4)"))s.nextInt(1,4);else if(name.equals("nextLong"))s.nextLong();else if(name.equals("nextBoolean"))s.nextBoolean();
                    else if(name.equals("nextFloat"))s.nextFloat();else if(name.equals("nextDouble"))s.nextDouble();else if(name.equals("nextGaussian"))s.nextGaussian();
                    else if(name.equals("nextBytes(5)"))s.nextBytes(new byte[5]);else s.setSeed(3L);
                    b.append(name).append('[').append(Spy.log).append("] ");
                }
                Spy.log.setLength(0);
                b.append(t(()->s.nextInt(0))).append('[').append(Spy.log).append("] ");
                b.append(t(()->s.nextInt(4,4))).append('[').append(Spy.log).append("] ");
                Random plain=new Random(42L);Spy same=new Spy(42L);
                return b.append(ints(plain,3).equals(ints(same,3))).append(' ').append(d(new Spy(1L).nextGaussian()).equals(d(new Random(1L).nextGaussian()))).toString();
            }
            public static String subclassSeed(){
                NoSeed n=new NoSeed(42L);String a=ints(n,3);
                String zero=ints(new Random(0x5DEECE66DL),3);
                n.setSeed(1L);
                return a+"| "+zero+"| "+ints(n,2)+"| "+ints(new NoSeed(-9L),2);
            }
            public static String scriptedRejection(){
                Script.log.setLength(0);StringBuilder b=new StringBuilder();
                b.append(new Script(Integer.MAX_VALUE,5).nextInt(3)).append(' ');
                b.append(new Script(Integer.MAX_VALUE,Integer.MAX_VALUE-1,Integer.MAX_VALUE-2).nextInt(3)).append(' ');
                b.append(new Script(Integer.MAX_VALUE).nextInt(8)).append(' ');
                b.append(new Script(-1,6).nextInt(1,4)).append(' ');
                b.append(new Script(-1,0).nextInt(0,3)).append(' ');
                b.append(new Script(Integer.MIN_VALUE,Integer.MAX_VALUE,5).nextInt(Integer.MIN_VALUE+1,Integer.MAX_VALUE)).append(' ');
                b.append(new Script(-1).nextInt(Integer.MIN_VALUE,0)).append(' ');
                b.append(new Script(0x12345678).nextInt()).append(' ').append(new Script(1,0).nextLong()).append(' ').append(new Script(-1,-1).nextLong()).append(' ');
                b.append(new Script(2,1,0).nextBoolean()).append(new Script(0).nextBoolean()).append(' ');
                b.append(f(new Script(1<<23).nextFloat())).append(' ').append(d(new Script(1<<25,0).nextDouble())).append(' ');
                b.append(d(new Script((1<<26)-1,(1<<27)-1).nextDouble())).append(" log[").append(Script.log).append("] ");
                Script.log.setLength(0);
                // nextGaussian: (0.5,0.5) gives s == 0, (0,0) gives s == 2, then (0.75,0.5) is accepted.
                Script g=new Script(1<<25,0,1<<25,0,0,0,0,0,3<<24,0,1<<25,0);
                b.append(d(g.nextGaussian())).append(' ').append(d(g.nextGaussian())).append(' ').append(g.index).append(" log[").append(Script.log).append(']');
                return b.toString();
            }
        }
        """;

    @Test void lcgSequences() throws Exception {
        matchesJvm(FIXTURE, "seedSequences", "setSeedRestarts", "longsAndBooleans", "floatsAndDoubles");
    }

    @Test void boundedValues() throws Exception {
        matchesJvm(FIXTURE, "boundedInts", "rangeInts");
    }

    @Test void gaussianAndBytes() throws Exception {
        matchesJvm(FIXTURE, "gaussians", "bytes");
    }

    @Test void subclassCalls() throws Exception {
        matchesJvm(FIXTURE, "spyCalls", "subclassSeed", "scriptedRejection");
    }

    @Test void leavesOutEntropyAndStreams() {
        rejectsUnusedCall("java/util/Random", "<init>", "()V", INVOKESPECIAL, false);
        rejectsUnusedCall("java/util/Random", "ints", "()Ljava/util/stream/IntStream;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/Random", "nextLong", "(J)J", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/Random", "nextDouble", "(DD)D", INVOKEVIRTUAL, false);
    }
}
