package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T74: java.nio.Buffer and the heap java.nio.ByteBuffer core. Expected values come from the JVM; see tasks/T74-byte-buffer.md. */
@Tag("agent-pending")
class BytecodeByteBufferTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.nio.Buffer;
        import java.nio.ByteBuffer;
        import java.nio.ByteOrder;
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
            /** The remaining bytes, read with absolute get so the position does not move. */
            static String rem(ByteBuffer b){StringBuilder o=new StringBuilder("[");for(int i=b.position();i<b.limit();i++)o.append(i>b.position()?" ":"").append(b.get(i));return o.append(']').toString();}
            static ByteBuffer bytes(int... v){byte[] a=new byte[v.length];for(int i=0;i<v.length;i++)a[i]=(byte)v[i];return ByteBuffer.wrap(a);}

            public static String positionLimitAndMark(){
                ByteBuffer b=ByteBuffer.allocate(16);
                String a=b+","+s(b)+","+b.hasRemaining()+","+b.isReadOnly()+","+b.isDirect()+","+b.hasArray()+","+b.arrayOffset()+","+b.array().length;
                a+="|"+t(()->ByteBuffer.allocate(-3))+","+ByteBuffer.allocate(0)+","+ByteBuffer.allocate(0).hasRemaining();
                a+="|"+t(()->b.position(17))+","+t(()->b.position(-1))+","+t(()->b.limit(17))+","+t(()->b.limit(-2))+","+s(b);
                b.position(5).mark();b.position(9);
                a+="|"+s(b.reset())+","+s(b.position(7))+","+t(()->b.reset())+","+s(b);
                b.position(3).mark();b.limit(2);
                a+="|"+s(b)+","+t(()->b.reset());
                b.limit(16).position(4).mark().position(6);b.limit(5);
                a+="|"+s(b)+","+t(()->b.reset());
                b.clear().position(8).mark();
                a+="|"+s(b.flip())+","+t(()->b.reset());
                b.limit(12).position(6).mark();
                a+="|"+s(b.rewind())+","+t(()->b.reset())+","+s(b.clear());
                Buffer old=b;
                old.limit(10);old.position(4);old.mark();old.position(7);old.reset();
                a+="|"+s(old)+","+(old.flip()==b)+","+s(b)+","+(old.rewind()==b)+","+(old.clear()==b)+","+s(b);
                b.position(4).mark();b.position(2);
                a+="|"+t(()->b.reset())+","+b.position(16).hasRemaining()+","+b;
                Object o=b;
                return a+"|"+(o instanceof Buffer)+","+(o instanceof Comparable)+","+(ByteBuffer.allocate(1).order()==ByteOrder.BIG_ENDIAN);
            }
            public static String relativeAndAbsolute(){
                ByteBuffer b=ByteBuffer.allocate(4);
                b.put((byte)1).put((byte)-2).put((byte)127);
                String a=s(b)+","+b.put((byte)-128).hasRemaining()+","+t(()->b.put((byte)9))+","+s(b)+","+t(()->b.get());
                b.flip();
                a+="|"+b.get()+","+b.get()+","+s(b)+","+b.get(0)+","+b.get(3)+","+s(b);
                a+="|"+t(()->b.get(4))+","+t(()->b.get(-1))+","+t(()->b.put(4,(byte)0))+","+t(()->b.put(-1,(byte)0));
                b.limit(3);
                a+="|"+t(()->b.get(3))+","+t(()->b.put(3,(byte)5))+","+b.put(1,(byte)44).get(1)+","+s(b)+","+b.get()+","+t(()->b.get())+","+s(b);
                byte[] arr={10,20,30,40,50,60};
                ByteBuffer w=ByteBuffer.wrap(arr);
                w.put(2,(byte)33);arr[5]=66;
                a+="|"+s(w)+","+(w.array()==arr)+","+w.arrayOffset()+","+arr[2]+","+w.get(5)+","+w;
                ByteBuffer r=ByteBuffer.wrap(arr,1,3);
                a+="|"+s(r)+","+r.arrayOffset()+","+(r.array()==arr)+","+r.get()+","+r.get(0)+","+rem(r)+","+r;
                a+="|"+t(()->ByteBuffer.wrap(arr,4,3))+","+t(()->ByteBuffer.wrap(arr,-1,2))+","+t(()->ByteBuffer.wrap(arr,7,0))+","+t(()->ByteBuffer.wrap(arr,2,-1))+","+s(ByteBuffer.wrap(arr,6,0));
                return a+"|"+s(ByteBuffer.wrap(new byte[0]))+","+ty(()->ByteBuffer.wrap(null));
            }
            public static String bulkTransfers(){
                ByteBuffer b=bytes(1,2,3,4,5,6,7,8);
                byte[] d=new byte[3];
                b.get(d);
                String a=Arrays.toString(d)+","+s(b);
                byte[] e=new byte[6];
                b.get(e,2,3);
                a+="|"+Arrays.toString(e)+","+s(b)+","+t(()->b.get(new byte[3]))+","+s(b)+","+t(()->b.get(e,4,3))+","+t(()->b.get(e,-1,1))+","+t(()->b.get(e,1,-1))+","+s(b);
                a+="|"+b.get(new byte[0]).position()+","+Arrays.toString(e)+","+ty(()->b.get((byte[])null));
                ByteBuffer c=ByteBuffer.allocate(6);
                c.put(new byte[]{9,8}).put(new byte[]{1,2,3,4,5},1,3);
                a+="|"+rem(c.duplicate().flip())+","+s(c)+","+t(()->c.put(new byte[]{1,2}))+","+t(()->c.put(new byte[]{1,2,3},2,2))+","+s(c);
                ByteBuffer src=bytes(7,6,5,4,3);
                src.position(1).limit(3);
                ByteBuffer dst=ByteBuffer.allocate(5);
                dst.put((byte)0).put(src);
                a+="|"+s(dst)+","+s(src)+","+rem(dst.duplicate().flip())+","+t(()->dst.put(dst));
                ByteBuffer big=bytes(1,1,1,1,1);
                a+="|"+t(()->dst.put(big))+","+s(dst)+","+s(big)+","+ty(()->dst.put((ByteBuffer)null));
                ByteBuffer from=bytes(1,2,3,4,5,6);
                from.position(2);
                ByteBuffer to=from.duplicate();
                to.position(0);
                to.put(from);
                return a+"|"+rem(to.duplicate().clear())+","+s(to)+","+s(from);
            }
            public static String typedValues(){
                ByteBuffer b=ByteBuffer.allocate(16);
                b.putInt(0x01020304).putShort((short)-2).putChar('A').putLong(0x1122334455667788L);
                String a=s(b)+","+rem(b.duplicate().flip())+","+b.order();
                b.flip();
                a+="|"+Integer.toHexString(b.getInt())+","+b.getShort()+","+b.getChar()+","+Long.toHexString(b.getLong())+","+s(b);
                a+="|"+b.getInt(0)+","+b.getShort(4)+","+(int)b.getChar(6)+","+b.getLong(8)+","+b.getShort(1)+","+b.getInt(13-1);
                b.order(ByteOrder.LITTLE_ENDIAN);
                a+="|"+b.order()+","+Integer.toHexString(b.getInt(0))+","+b.getShort(4)+","+(int)b.getChar(6)+","+Long.toHexString(b.getLong(8));
                ByteBuffer l=ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
                l.putInt(-2).putShort((short)0x1234).putChar('\\uABCD').putLong(-3L);
                a+="|"+rem(l.duplicate().flip())+","+l.flip().getInt()+","+l.getShort()+","+(int)l.getChar()+","+l.getLong();
                l.order(ByteOrder.BIG_ENDIAN);
                a+="|"+l.getInt(0)+","+l.getShort(4)+","+(int)l.getChar(6)+","+l.getLong(8);
                ByteBuffer p=ByteBuffer.allocate(10);
                p.putInt(6,-7).putShort(0,(short)300).putChar(2,'z').putLong(2,Long.MIN_VALUE+5);
                a+="|"+rem(p)+","+s(p)+","+p.getInt(6)+","+p.getLong(2);
                ByteBuffer q=ByteBuffer.allocate(7);
                q.position(5);
                a+="|"+t(()->q.getInt())+","+t(()->q.putInt(1))+","+t(()->q.getLong())+","+s(q)+","+q.getShort()+","+t(()->q.getChar())+","+t(()->q.putShort((short)1));
                a+="|"+t(()->q.getInt(4))+","+t(()->q.putInt(4,1))+","+t(()->q.getLong(-1))+","+t(()->q.getShort(6))+","+t(()->q.putChar(6,'c'))+","+q.getInt(3)+","+t(()->q.putLong(0,1L));
                q.limit(4);
                return a+"|"+t(()->q.getInt(1))+","+q.getInt(0)+","+t(()->q.order(null).order());
            }
            public static String viewsAndCompact(){
                ByteBuffer b=bytes(1,2,3,4,5,6,7,8);
                b.position(2).limit(6);
                ByteBuffer sl=b.slice();
                String a=s(sl)+","+sl.arrayOffset()+","+(sl.array()==b.array())+","+rem(sl)+","+sl;
                sl.put(0,(byte)30);b.put(3,(byte)40);
                a+="|"+b.get(2)+","+sl.get(1)+","+t(()->sl.get(4))+","+t(()->sl.reset());
                ByteBuffer sl2=sl.position(1).slice();
                a+="|"+sl2.arrayOffset()+","+rem(sl2)+","+s(sl2);
                b.order(ByteOrder.LITTLE_ENDIAN).position(3).mark().position(4);
                ByteBuffer du=b.duplicate();
                a+="|"+s(du)+","+du.order()+","+b.slice().order()+","+s(du.reset())+","+s(b)+","+du.arrayOffset();
                du.put(4,(byte)-5);
                a+="|"+b.get(4)+","+s(du.clear())+","+s(b);
                ByteBuffer ro=b.asReadOnlyBuffer();
                a+="|"+ro.isReadOnly()+","+ro.hasArray()+","+s(ro)+","+ro.order()+","+s(ro.reset())+","+ro.get()+","+ro;
                a+="|"+t(()->ro.array())+","+t(()->ro.arrayOffset())+","+t(()->ro.put((byte)1))+","+t(()->ro.put(0,(byte)1))+","+t(()->ro.put(new byte[0]))+","+t(()->ro.put(ByteBuffer.allocate(0)))+","+t(()->ro.putInt(0,1))+","+t(()->ro.putLong(1L))+","+t(()->ro.compact());
                b.put(5,(byte)55);
                a+="|"+ro.get(5)+","+ro.duplicate().isReadOnly()+","+ro.slice().isReadOnly()+","+ro.slice()+","+ro.asReadOnlyBuffer().isReadOnly()+","+b.isReadOnly()+","+ro.getShort(4);
                ByteBuffer c=bytes(1,2,3,4,5,6);
                c.position(2).limit(5).mark();
                c.compact();
                a+="|"+s(c)+","+rem(c.duplicate().clear())+","+t(()->c.reset());
                ByteBuffer e=bytes(9,9);
                e.position(2).compact();
                return a+"|"+s(e)+","+s(ByteBuffer.allocate(3).compact());
            }
            public static String equalityAndOrdering(){
                ByteBuffer x=bytes(1,-2,3,4),y=bytes(0,1,-2,3,4,9);
                y.position(1).limit(5);
                String a=x.equals(y)+","+y.equals(x)+","+(x.hashCode()==y.hashCode())+","+x.hashCode()+","+x.compareTo(y)+","+x.equals("x")+","+x.equals(null)+","+x.equals(x);
                y.limit(4);
                a+="|"+x.equals(y)+","+x.compareTo(y)+","+y.compareTo(x)+","+y.hashCode();
                ByteBuffer m=bytes(1,-2,7),n=bytes(1,-2,-100);
                a+="|"+m.compareTo(n)+","+n.compareTo(m)+","+bytes(5).compareTo(bytes(-3))+","+bytes().compareTo(bytes())+","+bytes().hashCode()+","+bytes(0).hashCode()+","+bytes(-1,-1).hashCode();
                x.order(ByteOrder.LITTLE_ENDIAN);
                ByteBuffer ro=x.asReadOnlyBuffer();
                a+="|"+x.equals(ro)+","+ro.equals(x)+","+(ro.hashCode()==x.hashCode())+","+ro.compareTo(x);
                x.position(4);
                ByteBuffer empty=ByteBuffer.allocate(5);empty.position(5);
                return a+"|"+x.equals(empty)+","+x.hashCode()+","+x.compareTo(bytes(0))+","+ty(()->x.compareTo(null));
            }
        }
        """;

    @Test void positionLimitAndMark() throws Exception {
        matchesJvm(FIXTURE, "positionLimitAndMark");
    }

    @Test void relativeAndAbsoluteBytes() throws Exception {
        matchesJvm(FIXTURE, "relativeAndAbsolute");
    }

    @Test void bulkTransfers() throws Exception {
        matchesJvm(FIXTURE, "bulkTransfers");
    }

    @Test void typedValuesAndByteOrder() throws Exception {
        matchesJvm(FIXTURE, "typedValues");
    }

    @Test void viewsReadOnlyAndCompact() throws Exception {
        matchesJvm(FIXTURE, "viewsAndCompact");
    }

    @Test void equalityAndOrdering() throws Exception {
        matchesJvm(FIXTURE, "equalityAndOrdering");
    }

    @Test void leavesOutDirectBuffersViewsAndFloatingPoint() {
        rejectsUnusedCall("java/nio/ByteBuffer", "allocateDirect", "(I)Ljava/nio/ByteBuffer;", INVOKESTATIC, false);
        rejectsUnusedCall("java/nio/ByteBuffer", "asLongBuffer", "()Ljava/nio/LongBuffer;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/nio/ByteBuffer", "getFloat", "()F", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/nio/ByteBuffer", "slice", "(II)Ljava/nio/ByteBuffer;", INVOKEVIRTUAL, false);
    }
}
