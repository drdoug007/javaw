package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** T37: java.util.EnumSet. Expected values come from the JVM; see tasks/T37-enum-set.md. */
@Tag("agent-pending")
class BytecodeEnumSetTest extends KitDifferential {
    private static final String TEMPLATE = """
        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.Collection;
        import java.util.EnumSet;
        import java.util.HashSet;
        import java.util.Iterator;
        import java.util.List;
        import java.util.Set;
        import java.util.TreeSet;

        enum Color{RED,GREEN,BLUE,YELLOW,BLACK,WHITE,PINK}
        enum Fruit{APPLE,PEAR}
        /** Constants with bodies: each constant's getClass() is an anonymous subclass (Op$1, Op$2, Op$3). */
        enum Op{
            PLUS{int apply(int a,int b){return a+b;}},
            MINUS{int apply(int a,int b){return a-b;}},
            TIMES{int apply(int a,int b){return a*b;}};
            abstract int apply(int a,int b);
        }
        /** toString differs from name(): EnumSet.toString and the range message use toString. */
        enum Level{LOW,MID,HIGH;public String toString(){return "lvl-"+ordinal();}}
        enum Empty{}
        /** Exactly 64 constants W0..W63: the largest universe that fits one long. */
        enum Wide{WIDE_CONSTANTS}
        /** 130 constants H0..H129: three 64-bit words (0-63, 64-127, 128-129). */
        enum Huge{HUGE_CONSTANTS}

        public class Fixture {
            interface Op0{Object run() throws Exception;}
            /** The value, or "SimpleName:message" of the exception. */
            static String t(Op0 op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            /** The value, or only the exception's simple class name. */
            static String ty(Op0 op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName();}
            }
            /** Size, first and last element of a large set, and the elements of the given ordinals that it contains. */
            static String sum(Set<Huge> s,int... probe){
                StringBuilder b=new StringBuilder().append(s.size());
                Iterator<Huge> i=s.iterator();Huge first=null,last=null;while(i.hasNext()){Huge h=i.next();if(first==null)first=h;last=h;}
                b.append(' ').append(first).append("..").append(last).append(" {");
                for(int p:probe)if(s.contains(Huge.values()[p]))b.append(Huge.values()[p]).append(' ');
                return b.append('}').toString();
            }
            static String wsum(Set<Wide> s){
                Iterator<Wide> i=s.iterator();Wide first=null,last=null;while(i.hasNext()){Wide h=i.next();if(first==null)first=h;last=h;}
                return s.size()+" "+first+".."+last;
            }

            // ---- factories ----
            public static String factoryBasics(){
                return EnumSet.noneOf(Color.class)+" "+EnumSet.noneOf(Color.class).isEmpty()+" "+EnumSet.allOf(Color.class)+" "+EnumSet.allOf(Color.class).size()
                        +" "+EnumSet.allOf(Fruit.class)+" "+EnumSet.allOf(Level.class);
            }
            public static String ofOverloads(){
                return EnumSet.of(Color.BLUE)+" "+EnumSet.of(Color.BLUE,Color.RED)+" "+EnumSet.of(Color.PINK,Color.BLUE,Color.RED)
                        +" "+EnumSet.of(Color.PINK,Color.BLUE,Color.RED,Color.BLUE)+" "+EnumSet.of(Color.WHITE,Color.BLACK,Color.YELLOW,Color.BLUE,Color.GREEN)
                        +" "+EnumSet.of(Color.PINK,Color.WHITE,Color.BLACK,Color.YELLOW,Color.BLUE,Color.GREEN)
                        +" "+EnumSet.of(Color.GREEN,new Color[0])+" "+EnumSet.of(Color.GREEN,Color.GREEN,Color.GREEN,Color.GREEN,Color.GREEN,Color.GREEN,Color.GREEN);
            }
            public static String rangeAndComplement(){
                return EnumSet.range(Color.GREEN,Color.BLACK)+" "+EnumSet.range(Color.BLUE,Color.BLUE)+" "+EnumSet.range(Color.RED,Color.PINK).size()
                        +" "+EnumSet.complementOf(EnumSet.of(Color.RED,Color.BLACK))+" "+EnumSet.complementOf(EnumSet.allOf(Color.class))
                        +" "+EnumSet.complementOf(EnumSet.noneOf(Fruit.class))+" "+EnumSet.range(Level.MID,Level.HIGH)
                        +" "+t(()->EnumSet.range(Color.BLUE,Color.RED))+" "+t(()->EnumSet.range(Level.HIGH,Level.LOW));
            }
            public static String copyOfVariants(){
                EnumSet<Color> source=EnumSet.of(Color.RED,Color.BLUE);
                EnumSet<Color> a=EnumSet.copyOf(source);a.add(Color.PINK);
                EnumSet<Color> b=EnumSet.copyOf((Collection<Color>)source);b.remove(Color.RED);
                EnumSet<Color> c=EnumSet.copyOf(Arrays.asList(Color.WHITE,Color.GREEN,Color.WHITE,Color.RED));
                EnumSet<Color> d=EnumSet.copyOf(new TreeSet<>(List.of(Color.YELLOW)));
                return source+" "+a+" "+b+" "+c+" "+d+" "+EnumSet.copyOf(EnumSet.noneOf(Color.class))+" "+EnumSet.copyOf((Collection<Color>)EnumSet.noneOf(Color.class))
                        +" "+t(()->EnumSet.copyOf(new ArrayList<Color>()))+" "+t(()->EnumSet.copyOf(List.<Color>of()));
            }
            @SuppressWarnings({"unchecked","rawtypes"})
            public static String factoryFailures(){
                return t(()->EnumSet.noneOf((Class)String.class))+" "+t(()->EnumSet.allOf((Class)Integer.class))+" "+t(()->EnumSet.noneOf((Class)Op.PLUS.getClass()))
                        +" "+ty(()->EnumSet.noneOf(null))+" "+ty(()->EnumSet.of((Color)null))+" "+ty(()->EnumSet.of(Color.RED,(Color)null))
                        +" "+ty(()->EnumSet.of(Color.RED,Color.BLUE,null))+" "+ty(()->EnumSet.of(Color.RED,(Color[])null))+" "+ty(()->EnumSet.range(null,Color.RED))
                        +" "+ty(()->EnumSet.copyOf((Collection<Color>)null))+" "+ty(()->EnumSet.copyOf((EnumSet<Color>)null))+" "+ty(()->EnumSet.complementOf(null))
                        +" "+ty(()->EnumSet.copyOf(Arrays.asList(null,Color.RED)))+" "+t(()->EnumSet.copyOf((List)Arrays.asList(Color.RED,Fruit.APPLE)))
                        +" "+t(()->EnumSet.of((Enum)Color.RED,(Enum)Fruit.PEAR))+" "+ty(()->EnumSet.range((Enum)Color.RED,(Enum)Fruit.PEAR));
            }

            // ---- element types ----
            public static String constantBodies(){
                EnumSet<Op> s=EnumSet.of(Op.TIMES,Op.PLUS);
                return s+" "+s.contains(Op.PLUS)+" "+s.contains(Op.MINUS)+" "+EnumSet.allOf(Op.class)+" "+EnumSet.complementOf(s)+" "+EnumSet.range(Op.MINUS,Op.TIMES)
                        +" "+s.remove(Op.PLUS)+" "+s+" "+s.add(Op.MINUS)+" "+s+" "+EnumSet.copyOf(List.of(Op.MINUS,Op.PLUS))+" "+(Op.PLUS.getClass()!=Op.class)
                        +" "+EnumSet.of(Op.PLUS).iterator().next().apply(2,3);
            }
            public static String emptyEnum(){
                EnumSet<Empty> n=EnumSet.noneOf(Empty.class),a=EnumSet.allOf(Empty.class),c=EnumSet.complementOf(n);
                Iterator<Empty> i=a.iterator();
                return n+" "+a+" "+c+" "+a.size()+" "+a.isEmpty()+" "+i.hasNext()+" "+t(()->i.next())+" "+n.equals(a)+" "+n.equals(EnumSet.noneOf(Color.class))
                        +" "+a.contains(null)+" "+a.contains(Color.RED)+" "+n.clone();
            }

            // ---- single-element operations ----
            @SuppressWarnings({"unchecked","rawtypes"})
            public static String addRemoveContains(){
                EnumSet<Color> s=EnumSet.noneOf(Color.class);
                String r=s.add(Color.BLUE)+" "+s.add(Color.BLUE)+" "+s.add(Color.RED)+" "+s+" "+s.size()+" "+s.contains(Color.RED)+" "+s.contains(Color.GREEN)
                        +" "+s.contains(null)+" "+s.contains(Fruit.APPLE)+" "+s.contains("RED")+" "+s.contains(0)+" "+s.contains(Op.PLUS)
                        +" "+s.remove(null)+" "+s.remove(Fruit.APPLE)+" "+s.remove("BLUE")+" "+s.remove(Color.GREEN)+" "+s.remove(Color.BLUE)+" "+s;
                Set raw=s;
                r+=" "+t(()->raw.add(Fruit.APPLE))+" "+t(()->raw.add(Op.PLUS))+" "+ty(()->raw.add(null))+" "+ty(()->raw.add("RED"))+" "+s;
                Set ops=EnumSet.noneOf(Op.class);
                r+=" "+t(()->ops.add(Level.LOW))+" "+ops.add(Op.MINUS)+" "+ops;
                return r;
            }
            public static String sizeAndClear(){
                EnumSet<Color> s=EnumSet.allOf(Color.class);
                String r=s.size()+" "+s.isEmpty();
                s.clear();
                r+=" "+s.size()+" "+s.isEmpty()+" "+s;
                s.add(Color.PINK);s.add(Color.RED);
                return r+" "+s.size()+" "+s+" "+Arrays.toString(s.toArray())+" "+Arrays.toString(s.toArray(new Color[0]))+" "+Arrays.toString(s.toArray(new Color[4]));
            }

            // ---- iteration ----
            public static String iteratorOrder(){
                EnumSet<Color> s=EnumSet.noneOf(Color.class);
                s.add(Color.PINK);s.add(Color.GREEN);s.add(Color.YELLOW);s.add(Color.RED);
                StringBuilder b=new StringBuilder();for(Color c:s)b.append(c.ordinal());
                return b+" "+s;
            }
            public static String iteratorRemove(){
                EnumSet<Color> s=EnumSet.of(Color.RED,Color.GREEN,Color.BLUE);
                Iterator<Color> i=s.iterator();
                String r=t(()->{i.remove();return "x";});
                r+=" "+i.next()+" "+t(()->{i.remove();return "ok";})+" "+t(()->{i.remove();return "x";})+" "+s;
                r+=" "+i.next()+" "+i.next()+" "+i.hasNext()+" "+t(()->i.next())+" "+t(()->{i.remove();return "ok";})+" "+s;
                EnumSet<Color> all=EnumSet.allOf(Color.class);
                r+=" "+all.removeIf(c->c.ordinal()%2==0)+" "+all+" "+all.removeIf(c->false);
                return r;
            }
            public static String iteratorSnapshot(){
                EnumSet<Color> s=EnumSet.of(Color.RED,Color.GREEN,Color.BLUE,Color.BLACK);
                Iterator<Color> i=s.iterator();
                StringBuilder b=new StringBuilder();
                b.append(i.next()).append(' ');
                s.remove(Color.GREEN);s.add(Color.YELLOW);s.add(Color.PINK);
                b.append(i.next()).append(' ');
                i.remove();
                b.append(s).append(' ');
                s.clear();
                b.append(i.hasNext()).append(' ').append(i.next()).append(' ');
                i.remove();
                b.append(i.next()).append(' ').append(i.hasNext()).append(' ').append(s);
                return b.toString();
            }

            // ---- bulk operations ----
            @SuppressWarnings({"unchecked","rawtypes"})
            public static String addAllCases(){
                EnumSet<Color> s=EnumSet.of(Color.RED);
                String r=s.addAll(EnumSet.of(Color.BLUE,Color.PINK))+" "+s.addAll(EnumSet.of(Color.BLUE))+" "+s.addAll(EnumSet.noneOf(Color.class))
                        +" "+s.addAll(List.of(Color.GREEN,Color.RED))+" "+s.addAll(new ArrayList<Color>())+" "+s;
                Set raw=s;
                r+=" "+raw.addAll(EnumSet.noneOf(Fruit.class))+" "+t(()->raw.addAll(EnumSet.of(Fruit.PEAR)))+" "+t(()->raw.addAll(EnumSet.of(Op.MINUS)))
                        +" "+t(()->raw.addAll(List.of(Op.MINUS)))+" "+t(()->raw.addAll(List.of(Color.WHITE,Fruit.APPLE)))+" "+s+" "+ty(()->s.addAll(null));
                return r;
            }
            @SuppressWarnings({"unchecked","rawtypes"})
            public static String removeAllCases(){
                EnumSet<Color> s=EnumSet.allOf(Color.class);
                String r=s.removeAll(EnumSet.of(Color.RED,Color.GREEN))+" "+s.removeAll(EnumSet.of(Color.RED))+" "+s;
                r+=" "+s.removeAll(EnumSet.allOf(Fruit.class))+" "+s.removeAll(List.of(Fruit.APPLE,"BLUE"))+" "+s.removeAll(Arrays.asList(Color.BLUE,null,Color.PINK))
                        +" "+s.removeAll(new HashSet<>(List.of(Color.WHITE,Color.RED,Color.GREEN,Color.PINK,Fruit.PEAR,Op.PLUS)))+" "+s+" "+ty(()->s.removeAll(null));
                return r;
            }
            @SuppressWarnings({"unchecked","rawtypes"})
            public static String retainAllCases(){
                EnumSet<Color> s=EnumSet.allOf(Color.class);
                String r=s.retainAll(EnumSet.range(Color.GREEN,Color.WHITE))+" "+s.retainAll(EnumSet.allOf(Color.class))+" "+s;
                r+=" "+s.retainAll(List.of(Color.BLUE,Color.BLACK,Color.PINK,Fruit.APPLE))+" "+s;
                Set raw=s;
                r+=" "+raw.retainAll(EnumSet.of(Fruit.APPLE))+" "+s+" "+raw.retainAll(EnumSet.noneOf(Fruit.class))+" "+ty(()->s.retainAll(null));
                EnumSet<Color> t=EnumSet.of(Color.RED,Color.GREEN);
                r+=" "+t.retainAll(new ArrayList<>())+" "+t;
                return r;
            }
            @SuppressWarnings({"unchecked","rawtypes"})
            public static String containsAllCases(){
                EnumSet<Color> s=EnumSet.of(Color.RED,Color.BLUE,Color.PINK);
                return s.containsAll(EnumSet.of(Color.RED,Color.PINK))+" "+s.containsAll(EnumSet.of(Color.RED,Color.GREEN))+" "+s.containsAll(EnumSet.noneOf(Color.class))
                        +" "+s.containsAll(EnumSet.noneOf(Fruit.class))+" "+s.containsAll(EnumSet.of(Fruit.APPLE))+" "+s.containsAll(List.of(Color.BLUE))
                        +" "+s.containsAll(Arrays.asList(Color.BLUE,null))+" "+s.containsAll(List.of("RED"))+" "+s.containsAll(List.of())
                        +" "+EnumSet.noneOf(Color.class).containsAll(EnumSet.noneOf(Op.class))+" "+ty(()->s.containsAll(null));
            }

            // ---- equality, hashing, text, copies ----
            public static String equalsAndHash(){
                EnumSet<Color> s=EnumSet.of(Color.RED,Color.BLUE);
                Set<Color> h=new HashSet<>(List.of(Color.BLUE,Color.RED));
                return s.equals(EnumSet.of(Color.BLUE,Color.RED))+" "+s.equals(EnumSet.of(Color.RED))+" "+s.equals(h)+" "+h.equals(s)
                        +" "+s.equals(new TreeSet<>(h))+" "+s.equals(List.of(Color.RED,Color.BLUE))+" "+s.equals(null)+" "+s.equals(EnumSet.of(Fruit.APPLE,Fruit.PEAR))
                        +" "+EnumSet.noneOf(Color.class).equals(EnumSet.noneOf(Fruit.class))+" "+EnumSet.noneOf(Color.class).equals(new HashSet<>())+" "+s.equals(s)
                        +" "+(s.hashCode()==h.hashCode())+" "+(EnumSet.noneOf(Color.class).hashCode())+" "+(EnumSet.of(Op.PLUS).hashCode()==Op.PLUS.hashCode())
                        +" "+s.equals(new HashSet<>(List.of(Color.RED,Color.GREEN)))+" "+s.equals(Set.of(Color.RED,Color.BLUE,Color.PINK));
            }
            public static String textAndCopies(){
                EnumSet<Level> l=EnumSet.of(Level.HIGH,Level.LOW);
                EnumSet<Color> s=EnumSet.of(Color.GREEN,Color.YELLOW);
                EnumSet<Color> c=s.clone();c.add(Color.RED);s.remove(Color.GREEN);
                EnumSet<Color> d=EnumSet.copyOf(c);d.clear();
                EnumSet<Color> e=EnumSet.complementOf(c);e.add(Color.RED);
                return l+" "+EnumSet.noneOf(Level.class)+" "+s+" "+c+" "+d+" "+e+" "+(c.clone()!=c)+" "+c.clone().equals(c)+" "+String.join(",",List.of(String.valueOf(c)));
            }

            // ---- large universes ----
            public static String wideUniverse(){
                EnumSet<Wide> a=EnumSet.allOf(Wide.class);
                EnumSet<Wide> c=EnumSet.complementOf(EnumSet.of(Wide.W63,Wide.W0));
                EnumSet<Wide> r=EnumSet.range(Wide.W1,Wide.W63);
                EnumSet<Wide> f=EnumSet.range(Wide.W0,Wide.W63);
                EnumSet<Wide> n=EnumSet.noneOf(Wide.class);n.add(Wide.W63);n.add(Wide.W62);
                Iterator<Wide> i=n.iterator();
                return wsum(a)+" "+wsum(c)+" "+wsum(r)+" "+wsum(f)+" "+n+" "+i.next()+" "+i.next()+" "+i.hasNext()+" "+n.contains(Wide.W63)+" "+n.remove(Wide.W63)+" "+n
                        +" "+EnumSet.complementOf(EnumSet.noneOf(Wide.class)).size()+" "+EnumSet.complementOf(a).isEmpty()+" "+a.containsAll(r)+" "+r.containsAll(a)
                        +" "+f.equals(a)+" "+EnumSet.range(Wide.W31,Wide.W33);
            }
            public static String hugeUniverse(){
                EnumSet<Huge> a=EnumSet.allOf(Huge.class);
                EnumSet<Huge> r=EnumSet.range(Huge.H60,Huge.H70);
                EnumSet<Huge> wide=EnumSet.range(Huge.H1,Huge.H129);
                EnumSet<Huge> one=EnumSet.range(Huge.H64,Huge.H127);
                EnumSet<Huge> c=EnumSet.complementOf(r);
                EnumSet<Huge> o=EnumSet.of(Huge.H129,Huge.H0,Huge.H64,Huge.H63,Huge.H128);
                return sum(a,0,63,64,127,128,129)+" | "+r+" | "+sum(wide,0,1,129)+" | "+sum(one,63,64,127,128)+" | "+sum(c,59,60,70,71)+" | "+o+" | "+o.size()
                        +" "+EnumSet.complementOf(a).isEmpty()+" "+EnumSet.complementOf(EnumSet.noneOf(Huge.class)).size()+" "+EnumSet.range(Huge.H128,Huge.H129)
                        +" "+t(()->EnumSet.range(Huge.H100,Huge.H99));
            }
            public static String hugeOperations(){
                EnumSet<Huge> s=EnumSet.of(Huge.H5,Huge.H70,Huge.H129);
                String r=s.add(Huge.H70)+" "+s.add(Huge.H128)+" "+s.remove(Huge.H5)+" "+s.remove(Huge.H5)+" "+s.contains(Huge.H129)+" "+s.contains(Huge.H64)+" "+s.contains(Color.RED)+" "+s;
                EnumSet<Huge> t=EnumSet.range(Huge.H60,Huge.H70);
                r+=" "+s.addAll(t)+" "+s.addAll(t)+" "+s.size()+" "+s.containsAll(t)+" "+t.containsAll(s)+" "+s.removeAll(EnumSet.range(Huge.H64,Huge.H128))+" "+s;
                r+=" "+s.retainAll(EnumSet.of(Huge.H60,Huge.H129,Huge.H1))+" "+s+" "+s.equals(EnumSet.of(Huge.H129,Huge.H60))+" "+s.equals(new HashSet<>(List.of(Huge.H60,Huge.H129)));
                EnumSet<Huge> k=s.clone();k.add(Huge.H100);s.remove(Huge.H129);
                EnumSet<Huge> e=EnumSet.copyOf(k);e.clear();
                r+=" "+s+" "+k+" "+e+" "+e.isEmpty()+" "+k.size()+" "+s.removeAll(EnumSet.noneOf(Huge.class))+" "+s.retainAll(EnumSet.noneOf(Color.class))+" "+s.isEmpty();
                return r;
            }
            @SuppressWarnings({"unchecked","rawtypes"})
            public static String hugeIteration(){
                EnumSet<Huge> s=EnumSet.of(Huge.H0,Huge.H2,Huge.H65,Huge.H129);
                Iterator<Huge> i=s.iterator();
                StringBuilder b=new StringBuilder();
                b.append(i.next()).append(' ');
                s.add(Huge.H1);s.remove(Huge.H2);s.add(Huge.H100);s.remove(Huge.H65);s.add(Huge.H128);
                while(i.hasNext()){Huge h=i.next();b.append(h).append(' ');if(h==Huge.H100)i.remove();}
                b.append(s).append(' ').append(s.size()).append(' ').append(t(()->i.next())).append(' ');
                Iterator<Huge> j=s.iterator();j.next();j.remove();
                b.append(t(()->{j.remove();return "x";})).append(' ').append(s).append(' ').append(s.size());
                Set raw=EnumSet.noneOf(Huge.class);
                b.append(' ').append(t(()->raw.add(Op.PLUS))).append(' ').append(t(()->raw.addAll(EnumSet.of(Op.PLUS)))).append(' ').append(t(()->raw.addAll(EnumSet.of(Color.RED))));
                Set colors=EnumSet.noneOf(Color.class);
                b.append(' ').append(t(()->colors.addAll(EnumSet.of(Huge.H3)))).append(' ').append(colors.addAll(EnumSet.noneOf(Huge.class)));
                return b.toString();
            }
        }
        """;

    private static String constants(String prefix, int count) {
        return IntStream.range(0, count).mapToObj(i -> prefix + i).collect(Collectors.joining(","));
    }

    private static final String FIXTURE = TEMPLATE.replace("WIDE_CONSTANTS", constants("W", 64)).replace("HUGE_CONSTANTS", constants("H", 130));

    @Test void factories() throws Exception {
        matchesJvm(FIXTURE, "factoryBasics", "ofOverloads", "rangeAndComplement", "copyOfVariants", "factoryFailures");
    }

    @Test void elementTypes() throws Exception {
        matchesJvm(FIXTURE, "constantBodies", "emptyEnum", "addRemoveContains", "sizeAndClear");
    }

    @Test void iteration() throws Exception {
        matchesJvm(FIXTURE, "iteratorOrder", "iteratorRemove", "iteratorSnapshot");
    }

    @Test void bulkOperations() throws Exception {
        matchesJvm(FIXTURE, "addAllCases", "removeAllCases", "retainAllCases", "containsAllCases");
    }

    @Test void equalityTextAndCopies() throws Exception {
        matchesJvm(FIXTURE, "equalsAndHash", "textAndCopies");
    }

    @Test void largeUniverses() throws Exception {
        matchesJvm(FIXTURE, "wideUniverse", "hugeUniverse", "hugeOperations", "hugeIteration");
    }

    @Test void leavesOutSerialization() {
        rejectsUnusedCall("java/util/EnumSet", "writeReplace", "()Ljava/lang/Object;", INVOKEVIRTUAL, false);
    }
}
