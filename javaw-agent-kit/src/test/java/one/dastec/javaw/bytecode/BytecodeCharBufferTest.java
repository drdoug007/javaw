package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T79: the heap and CharSequence-backed java.nio.CharBuffer core. Expected values come from the JVM; see tasks/T79-char-buffer.md. */
@Tag("agent-pending")
class BytecodeCharBufferTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.nio.Buffer;
        import java.nio.ByteOrder;
        import java.nio.CharBuffer;
        import java.util.Arrays;

        public class Fixture {
            interface Op{Object run() throws Exception;}
            static String t(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            /** Exception type only, for exceptions whose JVM message is a helpful NullPointerException text. */
            static String ty(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName();}
            }
            /** pos/lim/cap/remaining of a buffer. */
            static String s(Buffer b){return b.position()+"/"+b.limit()+"/"+b.capacity()+"/"+b.remaining();}
            /** Every char from 0 to capacity, read with absolute get after widening the limit on a duplicate. */
            static String all(CharBuffer b){CharBuffer d=b.duplicate();d.clear();StringBuilder o=new StringBuilder();for(int i=0;i<d.capacity();i++){char c=d.get(i);o.append(c==0?'_':c);}return o.toString();}

            public static String allocateAndWrap(){
                CharBuffer b=CharBuffer.allocate(8);
                String a=s(b)+","+b.isReadOnly()+","+b.isDirect()+","+b.hasArray()+","+b.arrayOffset()+","+b.array().length+","+(b.order()==ByteOrder.nativeOrder())+","+b.toString().length()+","+(int)b.toString().charAt(7)+","+b.length()+","+b.isEmpty();
                a+="|"+t(()->CharBuffer.allocate(-2))+","+s(CharBuffer.allocate(0))+","+CharBuffer.allocate(0).isEmpty();
                char[] arr={'a','b','c','d','e','f'};
                CharBuffer w=CharBuffer.wrap(arr);
                w.put(1,'B');arr[2]='C';
                a+="|"+s(w)+","+(w.array()==arr)+","+w.arrayOffset()+","+arr[1]+","+w.get(2)+","+w;
                CharBuffer r=CharBuffer.wrap(arr,1,3);
                a+="|"+s(r)+","+r.arrayOffset()+","+r+","+r.get()+","+r.get(0)+","+r.length();
                a+="|"+t(()->CharBuffer.wrap(arr,4,3))+","+t(()->CharBuffer.wrap(arr,-1,2))+","+t(()->CharBuffer.wrap(arr,7,0))+","+t(()->CharBuffer.wrap(arr,2,-1))+","+s(CharBuffer.wrap(arr,6,0))+","+ty(()->CharBuffer.wrap((char[])null));
                CharBuffer cs=CharBuffer.wrap("hello world");
                a+="|"+s(cs)+","+cs+","+cs.isReadOnly()+","+cs.hasArray()+","+cs.isDirect()+","+(cs.order()==ByteOrder.nativeOrder())+","+t(()->cs.array())+","+t(()->cs.arrayOffset());
                CharBuffer part=CharBuffer.wrap(new StringBuilder("0123456789"),2,7);
                a+="|"+s(part)+","+part+","+part.get()+","+part.get(0)+","+part+","+part.length();
                a+="|"+t(()->CharBuffer.wrap("abc",2,1))+","+t(()->CharBuffer.wrap("abc",0,4))+","+t(()->CharBuffer.wrap("abc",-1,2))+","+s(CharBuffer.wrap("abc",3,3))+","+ty(()->CharBuffer.wrap((CharSequence)null));
                Object o=b;
                return a+"|"+(o instanceof Buffer)+","+(o instanceof Comparable)+","+(o instanceof CharSequence)+","+(o instanceof Appendable)+","+(o instanceof java.io.Serializable);
            }
            public static String positionAndElements(){
                CharBuffer b=CharBuffer.allocate(5);
                b.put('x').put('y').put('z');
                String a=s(b)+","+b.put('w').put('v').hasRemaining()+","+t(()->b.put('u'))+","+s(b)+","+t(()->b.get());
                b.flip();
                a+="|"+b.get()+","+b.get()+","+s(b)+","+b.get(0)+","+b.get(4)+","+b+","+b.length()+","+b.charAt(0)+","+b.charAt(2);
                a+="|"+t(()->b.get(5))+","+t(()->b.get(-1))+","+t(()->b.put(5,'q'))+","+t(()->b.put(-1,'q'))+","+t(()->b.charAt(-1))+","+t(()->b.charAt(3))+","+t(()->b.charAt(5));
                b.limit(4);
                a+="|"+t(()->b.get(4))+","+b.put(3,'W').get(3)+","+s(b)+","+b+","+b.get()+","+b.get()+","+t(()->b.get())+","+b.isEmpty()+",["+b+"]";
                b.clear().position(1).mark().position(3);
                a+="|"+s(b.reset())+","+t(()->b.position(6))+","+t(()->b.limit(-1))+","+s(b.position(2))+","+s(b.limit(1))+","+t(()->b.reset());
                b.limit(5).position(1);
                a+="|"+b.chars().sum()+","+b.chars().count()+","+b.chars().filter(c->c>'w').count()+","+b.position();
                Buffer old=b;
                old.limit(4);old.position(1);old.mark();old.position(3);old.reset();
                a+="|"+s(old)+","+(old.flip()==b)+","+s(b)+","+(old.rewind()==b)+","+(old.clear()==b)+","+s(b);
                CharBuffer cs=CharBuffer.wrap("abcdef");
                cs.position(2);
                return a+"|"+cs.get()+","+cs.charAt(1)+","+cs.length()+","+cs.get(0)+","+t(()->cs.get(6))+","+t(()->cs.charAt(3))+","+cs.chars().sum()+","+s(cs.flip())+","+cs;
            }
            public static String bulkTransfers(){
                CharBuffer b=CharBuffer.wrap("abcdefgh".toCharArray());
                char[] d=new char[3];
                b.get(d);
                String a=new String(d)+","+s(b);
                char[] e=new char[6];Arrays.fill(e,'.');
                b.get(e,2,3);
                a+="|"+new String(e)+","+s(b)+","+t(()->b.get(new char[3]))+","+s(b)+","+t(()->b.get(e,4,3))+","+t(()->b.get(e,-1,1))+","+t(()->b.get(e,1,-1))+","+s(b);
                a+="|"+b.get(new char[0]).position()+","+ty(()->b.get((char[])null));
                CharBuffer c=CharBuffer.allocate(8);
                c.put(new char[]{'p','q'}).put("ABCDE".toCharArray(),1,3);
                a+="|"+all(c)+","+s(c)+","+t(()->c.put(new char[4]))+","+t(()->c.put(new char[]{'1','2','3'},2,2))+","+s(c);
                c.put("xy");
                a+="|"+all(c)+","+s(c)+","+t(()->c.put("long"))+","+s(c)+","+c.put("0123456789",8,9).position()+","+all(c);
                a+="|"+t(()->c.put("abc",2,5))+","+t(()->c.put("abc",2,1))+","+t(()->c.put("abc",-1,1))+","+ty(()->c.put((String)null));
                CharBuffer src=CharBuffer.wrap("vwxyz".toCharArray());
                src.position(1).limit(3);
                CharBuffer dst=CharBuffer.allocate(5);
                dst.put('<').put(src);
                a+="|"+s(dst)+","+s(src)+","+all(dst)+","+t(()->dst.put(dst));
                CharBuffer big=CharBuffer.wrap("ZZZZZ");
                a+="|"+t(()->dst.put(big))+","+s(dst)+","+s(big)+","+ty(()->dst.put((CharBuffer)null));
                CharBuffer text=CharBuffer.wrap("hi!");
                dst.clear().put(text);
                a+="|"+all(dst)+","+s(dst)+","+s(text);
                CharBuffer from=CharBuffer.wrap("123456".toCharArray());
                from.position(2);
                CharBuffer to=from.duplicate();
                to.position(0);
                to.put(from);
                CharBuffer back=CharBuffer.wrap("123456".toCharArray());
                CharBuffer ahead=back.duplicate();
                back.limit(4);ahead.position(2);
                ahead.put(back);
                return a+"|"+all(to)+","+s(to)+","+s(from)+","+all(ahead)+","+s(ahead)+","+s(back);
            }
            public static String viewsAndCompact(){
                CharBuffer b=CharBuffer.wrap("abcdefgh".toCharArray());
                b.position(2).limit(6);
                CharBuffer sl=b.slice();
                String a=s(sl)+","+sl.arrayOffset()+","+(sl.array()==b.array())+","+sl+","+t(()->sl.reset());
                sl.put(0,'C');b.put(3,'D');
                a+="|"+b.get(2)+","+sl.get(1)+","+t(()->sl.get(4));
                CharBuffer sl2=sl.position(1).slice();
                a+="|"+sl2.arrayOffset()+","+sl2+","+s(sl2);
                b.position(3).mark().position(4);
                CharBuffer du=b.duplicate();
                a+="|"+s(du)+","+s(du.reset())+","+s(b)+","+du.arrayOffset()+","+du;
                du.put(4,'E');
                a+="|"+b.get(4)+","+s(du.clear())+","+s(b);
                CharBuffer sub=b.subSequence(1,2);
                a+="|"+s(sub)+","+sub+","+sub.arrayOffset()+","+t(()->sub.reset())+","+b.subSequence(0,0)+","+s(b.subSequence(2,2));
                a+="|"+t(()->b.subSequence(1,3))+","+t(()->b.subSequence(2,1))+","+t(()->b.subSequence(-1,1));
                CharBuffer ro=b.asReadOnlyBuffer();
                a+="|"+ro.isReadOnly()+","+ro.hasArray()+","+s(ro)+","+s(ro.reset())+","+ro.get()+","+ro+","+(ro.order()==ByteOrder.nativeOrder());
                a+="|"+t(()->ro.array())+","+t(()->ro.arrayOffset())+","+t(()->ro.put('1'))+","+t(()->ro.put(0,'1'))+","+t(()->ro.put(new char[0]))+","+t(()->ro.put("toolong",0,7))+","+t(()->ro.put(CharBuffer.allocate(0)))+","+t(()->ro.compact())+","+t(()->ro.append('x'))+","+t(()->ro.append("abc",5,9));
                b.put(5,'F');
                a+="|"+ro.get(5)+","+ro.duplicate().isReadOnly()+","+ro.slice().isReadOnly()+","+ro.slice()+","+ro.subSequence(0,1).isReadOnly()+","+ro.asReadOnlyBuffer().isReadOnly()+","+b.isReadOnly()+","+ty(()->ro.put(ro))+","+ty(()->b.put(b));
                CharBuffer c=CharBuffer.wrap("123456".toCharArray());
                c.position(2).limit(5).mark();
                c.compact();
                a+="|"+s(c)+","+all(c)+","+t(()->c.reset());
                CharBuffer e=CharBuffer.wrap("zz".toCharArray());
                e.position(2).compact();
                return a+"|"+s(e)+","+s(CharBuffer.allocate(3).compact());
            }
            public static String sequenceBuffers(){
                StringBuilder live=new StringBuilder("abcdefg");
                CharBuffer cs=CharBuffer.wrap(live,1,6);
                live.setCharAt(2,'C');
                String a=s(cs)+","+cs+","+cs.get(2)+","+cs.charAt(1);
                CharBuffer sl=cs.slice();
                a+="|"+s(sl)+","+sl+","+sl.get(0)+","+sl.isReadOnly()+","+t(()->sl.array());
                cs.position(2).mark().position(3);
                CharBuffer du=cs.duplicate();
                CharBuffer ro=cs.asReadOnlyBuffer();
                a+="|"+s(du)+","+s(du.reset())+","+s(ro)+","+s(ro.reset())+","+ro+","+ro.isReadOnly();
                CharBuffer sub=cs.subSequence(1,3);
                a+="|"+s(sub)+","+sub+","+t(()->sub.reset())+","+cs.subSequence(0,0).length()+","+t(()->cs.subSequence(0,4))+","+t(()->cs.subSequence(2,1))+","+t(()->cs.subSequence(-1,1));
                a+="|"+t(()->cs.put('x'))+","+t(()->cs.put(0,'x'))+","+t(()->cs.put(new char[]{'x'},0,1))+","+t(()->cs.put("x"))+","+t(()->cs.put("abc",2,1))+","+t(()->cs.put(CharBuffer.wrap("q")))+","+t(()->cs.put(cs))+","+t(()->cs.compact())+","+t(()->cs.append('x'))+","+t(()->cs.append("xyz"));
                char[] d=new char[4];
                cs.clear().position(1).get(d,1,3);
                a+="|"+new String(d,1,3)+","+s(cs)+","+t(()->cs.get(new char[9]));
                CharBuffer heap=CharBuffer.allocate(10);
                heap.put("[").put(CharBuffer.wrap("hello",1,4)).put(CharBuffer.wrap(new StringBuilder("ab"))).put(']');
                return a+"|"+heap.flip()+","+s(heap)+","+CharBuffer.wrap("").length()+","+CharBuffer.wrap("").isEmpty();
            }
            public static String appendAndCompare(){
                CharBuffer b=CharBuffer.allocate(19);
                Appendable ap=b;
                b.append('a').append("bc").append(null).append("0123",1,3).append(new StringBuilder("XY")).append(new StringBuilder("klmn"),1,2).append(null,1,3);
                String a=s(b)+","+all(b);
                try{ap.append('!');ap.append("?").append("^&*",1,2);}catch(java.io.IOException ex){a+="io";}
                a+="|"+s(b)+","+t(()->b.append("abc",2,1))+","+t(()->b.append("abc",1,4))+","+t(()->b.append("long"))+","+t(()->b.append("long",0,3))+","+s(b);
                b.append(CharBuffer.wrap("Q")).append(CharBuffer.wrap("RS"),1,2);
                a+="|"+s(b)+","+all(b)+","+t(()->b.append('z'));
                CharBuffer x=CharBuffer.wrap("abcd".toCharArray()),y=CharBuffer.wrap("_abcd_");
                y.position(1).limit(5);
                a+="|"+x.equals(y)+","+y.equals(x)+","+(x.hashCode()==y.hashCode())+","+x.hashCode()+","+x.compareTo(y)+","+x.equals("abcd")+","+x.equals(null)+","+x.equals(x);
                y.limit(4);
                a+="|"+x.equals(y)+","+x.compareTo(y)+","+y.compareTo(x)+","+y.hashCode();
                CharBuffer m=CharBuffer.wrap("abz"),n=CharBuffer.wrap("abA");
                a+="|"+m.compareTo(n)+","+n.compareTo(m)+","+CharBuffer.wrap("\\uffff").compareTo(CharBuffer.wrap("a"))+","+CharBuffer.wrap("").compareTo(CharBuffer.wrap(""))+","+CharBuffer.wrap("").hashCode()+","+CharBuffer.wrap("\\u0000").hashCode()+","+CharBuffer.wrap("\\uffff\\u8000").hashCode();
                CharBuffer ro=x.asReadOnlyBuffer();
                a+="|"+x.equals(ro)+","+ro.equals(x)+","+(ro.hashCode()==x.hashCode())+","+ro.compareTo(x);
                x.position(4);
                CharBuffer empty=CharBuffer.allocate(5);empty.position(5);
                return a+"|"+x.equals(empty)+","+x.hashCode()+","+x.compareTo(CharBuffer.wrap("a"))+","+ty(()->x.compareTo(null))+","+CharBuffer.wrap("hello").equals(CharBuffer.wrap("hello".toCharArray()));
            }
        }
        """;

    @Test void allocateAndWrap() throws Exception {
        matchesJvm(FIXTURE, "allocateAndWrap");
    }

    @Test void positionAndElements() throws Exception {
        matchesJvm(FIXTURE, "positionAndElements");
    }

    @Test void bulkTransfers() throws Exception {
        matchesJvm(FIXTURE, "bulkTransfers");
    }

    @Test void viewsReadOnlyAndCompact() throws Exception {
        matchesJvm(FIXTURE, "viewsAndCompact");
    }

    @Test void charSequenceBuffers() throws Exception {
        matchesJvm(FIXTURE, "sequenceBuffers");
    }

    @Test void appendEqualityAndOrdering() throws Exception {
        matchesJvm(FIXTURE, "appendAndCompare");
    }

    @Test void leavesOutReadableAbsoluteBulkAndViews() {
        rejectsUnusedCall("java/nio/CharBuffer", "read", "(Ljava/nio/CharBuffer;)I", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/nio/CharBuffer", "slice", "(II)Ljava/nio/CharBuffer;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/nio/CharBuffer", "get", "(I[C)Ljava/nio/CharBuffer;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/nio/CharBuffer", "mismatch", "(Ljava/nio/CharBuffer;)I", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/nio/ByteBuffer", "asCharBuffer", "()Ljava/nio/CharBuffer;", INVOKEVIRTUAL, false);
    }
}
