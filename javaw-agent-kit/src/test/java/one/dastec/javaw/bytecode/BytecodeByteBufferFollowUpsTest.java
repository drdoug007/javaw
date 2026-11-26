package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T78: ByteBuffer float/double accessors, absolute bulk transfers and slice(int, int). Expected values come from the JVM; see tasks/T78-byte-buffer-follow-ups.md. */
@Tag("agent-pending")
class BytecodeByteBufferFollowUpsTest extends KitDifferential {
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
            static String s(Buffer b){return b.position()+"/"+b.limit()+"/"+b.capacity()+"/"+b.remaining();}
            static String rem(ByteBuffer b){StringBuilder o=new StringBuilder("[");for(int i=b.position();i<b.limit();i++)o.append(i>b.position()?" ":"").append(b.get(i));return o.append(']').toString();}
            static String all(ByteBuffer b){return rem(b.duplicate().clear());}
            static ByteBuffer bytes(int... v){byte[] a=new byte[v.length];for(int i=0;i<v.length;i++)a[i]=(byte)v[i];return ByteBuffer.wrap(a);}

            public static String floatsAndDoubles(){
                ByteBuffer b=ByteBuffer.allocate(24);
                b.putFloat(1.5f).putDouble(-2.25).putFloat(Float.MIN_VALUE).putDouble(Double.MAX_VALUE);
                String a=s(b)+","+all(b);
                b.flip();
                a+="|"+b.getFloat()+","+b.getDouble()+","+b.getFloat()+","+b.getDouble()+","+s(b);
                a+="|"+b.getFloat(0)+","+b.getDouble(4)+","+Integer.toHexString(b.getInt(0))+","+Long.toHexString(b.getLong(4));
                b.order(ByteOrder.LITTLE_ENDIAN);
                a+="|"+Integer.toHexString(Float.floatToRawIntBits(b.getFloat(0)))+","+Long.toHexString(Double.doubleToRawLongBits(b.getDouble(4)));
                ByteBuffer l=ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
                l.putFloat(-0.0f).putDouble(1e-300);
                a+="|"+all(l)+","+l.getFloat(0)+","+l.getDouble(4)+","+(1/l.getFloat(0));
                l.putFloat(8,Float.NEGATIVE_INFINITY).putDouble(0,Double.POSITIVE_INFINITY);
                a+="|"+all(l)+","+l.getFloat(8)+","+l.getDouble(0)+","+s(l);
                ByteBuffer q=ByteBuffer.allocate(10);
                q.position(7);
                a+="|"+t(()->q.getFloat())+","+t(()->q.putFloat(1f))+","+t(()->q.getDouble())+","+t(()->q.putDouble(1))+","+s(q);
                a+="|"+t(()->q.getFloat(7))+","+t(()->q.putFloat(-1,1f))+","+t(()->q.getDouble(3))+","+t(()->q.putDouble(3,1))+","+q.getFloat(6)+","+q.getDouble(2);
                q.limit(8);
                a+="|"+t(()->q.getFloat(5))+","+q.getFloat(4)+","+t(()->q.getDouble(1))+","+q.getDouble(0);
                ByteBuffer ro=q.asReadOnlyBuffer();
                return a+"|"+t(()->ro.putFloat(1f))+","+t(()->ro.putFloat(99,1f))+","+t(()->ro.putDouble(1))+","+t(()->ro.putDouble(-5,1))+","+ro.getFloat(0)+","+ro.getDouble(0);
            }
            public static String nanPayloads(){
                ByteBuffer b=ByteBuffer.allocate(32);
                int[] fi={0x7fc00000,0x7fc12345,0xffc00001,0x7fffffff,0xffffffff};
                String a="";
                for(int i=0;i<fi.length;i++){
                    b.putInt(0,fi[i]);
                    float f=b.getFloat(0);
                    b.putFloat(4,f);
                    a+=(i>0?",":"")+Integer.toHexString(b.getInt(4))+":"+Float.isNaN(f);
                }
                long[] di={0x7ff8000000000000L,0x7ff8000000012345L,0xfff8000000000001L,0x7fffffffffffffffL};
                a+="|";
                for(int i=0;i<di.length;i++){
                    b.putLong(8,di[i]);
                    double d=b.getDouble(8);
                    b.putDouble(16,d);
                    a+=(i>0?",":"")+Long.toHexString(b.getLong(16))+":"+Double.isNaN(d);
                }
                b.clear().putFloat(Float.intBitsToFloat(0x7fc0abcd)).putDouble(Double.longBitsToDouble(0x7ff80000abcdef01L));
                b.flip();
                String bytes=rem(b);
                b.order(ByteOrder.LITTLE_ENDIAN).putFloat(Float.intBitsToFloat(0xffc01234));
                return a+"|"+bytes+","+rem(b.duplicate().flip())+","+Integer.toHexString(Float.floatToRawIntBits(b.order(ByteOrder.BIG_ENDIAN).getFloat(0)))+","+Long.toHexString(Double.doubleToRawLongBits(b.getDouble(4)));
            }
            public static String absoluteBulkArrays(){
                ByteBuffer b=bytes(1,2,3,4,5,6,7,8);
                b.position(5).limit(7);
                byte[] d=new byte[3];
                String a=b.get(1,d)+","+Arrays.toString(d)+","+s(b);
                byte[] e=new byte[6];
                a+="|"+b.get(4,e,1,3).position()+","+Arrays.toString(e)+","+s(b);
                a+="|"+t(()->b.get(5,new byte[3]))+","+t(()->b.get(-1,new byte[1]))+","+t(()->b.get(2,e,4,3))+","+t(()->b.get(2,e,-1,1))+","+t(()->b.get(2,e,1,-1))+","+t(()->b.get(8,e,0,0))+","+t(()->b.get(9,e,9,1));
                a+="|"+b.get(7,new byte[0]).position()+","+b.get(0,e,6,0).position()+","+ty(()->b.get(0,(byte[])null))+","+ty(()->b.get(9,(byte[])null,0,1))+","+Arrays.toString(e)+","+s(b);
                ByteBuffer c=ByteBuffer.allocate(8);
                c.position(3).limit(6);
                a+="|"+c.put(0,new byte[]{9,8}).position()+","+c.put(3,new byte[]{1,2,3,4,5},1,3).position()+","+all(c)+","+s(c);
                a+="|"+t(()->c.put(5,new byte[]{1,2}))+","+t(()->c.put(-2,new byte[1]))+","+t(()->c.put(1,new byte[]{1,2,3},2,2))+","+t(()->c.put(1,new byte[3],-1,1))+","+t(()->c.put(7,new byte[3],0,1))+","+all(c);
                a+="|"+c.put(6,new byte[0]).position()+","+ty(()->c.put(0,(byte[])null))+","+ty(()->c.put(0,(byte[])null,0,1));
                ByteBuffer ro=c.asReadOnlyBuffer();
                a+="|"+t(()->ro.put(0,new byte[1]))+","+t(()->ro.put(99,new byte[1],5,5))+","+t(()->ro.put(0,new byte[0]))+","+t(()->ro.get(0,new byte[2]))+","+ro.get(3,new byte[2]).position();
                ByteBuffer sl=bytes(0,1,2,3,4,5,6,7).position(2).slice();
                byte[] f=new byte[2];
                sl.get(1,f).put(4,new byte[]{-1,-2});
                return a+"|"+Arrays.toString(f)+","+all(sl)+","+Arrays.toString(sl.array())+","+t(()->sl.get(5,new byte[2]));
            }
            public static String absoluteBulkBuffer(){
                ByteBuffer dst=ByteBuffer.allocate(8);
                dst.position(5).limit(6);
                ByteBuffer src=bytes(10,20,30,40,50,60);
                src.position(4).limit(5);
                String a=dst.put(1,src,0,4).position()+","+all(dst)+","+s(dst)+","+s(src);
                a+="|"+t(()->dst.put(4,src,0,3))+","+t(()->dst.put(-1,src,0,1))+","+t(()->dst.put(0,src,3,3))+","+t(()->dst.put(0,src,-1,1))+","+t(()->dst.put(0,src,1,-1))+","+t(()->dst.put(6,src,5,0))+","+all(dst);
                src.limit(6);
                a+="|"+dst.put(0,src,4,2).position()+","+all(dst)+","+s(src)+","+dst.put(6,src,6,0).position();
                a+="|"+ty(()->dst.put(0,(ByteBuffer)null,0,1))+","+ty(()->dst.put(9,(ByteBuffer)null,0,1));
                ByteBuffer ro=dst.asReadOnlyBuffer();
                a+="|"+t(()->ro.put(0,src,0,1))+","+t(()->ro.put(9,src,0,1))+","+t(()->ro.put(0,src,9,1))+","+t(()->ro.put(0,src,0,0));
                a+="|"+t(()->dst.put(0,dst,1,2))+","+all(dst);
                ByteBuffer over=bytes(1,2,3,4,5,6,7,8);
                over.put(2,over,0,5);
                a+="|"+all(over);
                over.put(0,over,3,5);
                a+="|"+all(over)+","+s(over);
                ByteBuffer back=bytes(1,2,3,4,5,6);
                ByteBuffer view=back.duplicate();
                view.put(1,back.slice(2,3),0,3);
                ByteBuffer rsrc=bytes(7,7,9).asReadOnlyBuffer();
                ByteBuffer lit=ByteBuffer.allocate(3).order(ByteOrder.LITTLE_ENDIAN);
                lit.put(0,rsrc,1,2);
                return a+"|"+all(back)+","+all(lit)+","+s(rsrc);
            }
            public static String sliceIndexLength(){
                ByteBuffer b=bytes(1,2,3,4,5,6,7,8);
                b.position(3).limit(6).mark();
                b.order(ByteOrder.LITTLE_ENDIAN);
                ByteBuffer sl=b.slice(1,4);
                String a=s(sl)+","+sl.arrayOffset()+","+(sl.array()==b.array())+","+rem(sl)+","+sl.order()+","+sl+","+t(()->sl.reset())+","+sl.isReadOnly()+","+s(b);
                sl.put(0,(byte)20);b.put(4,(byte)50);
                a+="|"+b.get(1)+","+sl.get(3)+","+t(()->sl.get(4))+","+t(()->sl.position(5));
                ByteBuffer sl2=sl.slice(2,2);
                a+="|"+sl2.arrayOffset()+","+rem(sl2)+","+s(sl2)+","+s(b.slice(6,0))+","+s(b.slice(0,6));
                a+="|"+t(()->b.slice(0,7))+","+t(()->b.slice(-1,1))+","+t(()->b.slice(2,-1))+","+t(()->b.slice(7,0))+","+t(()->b.slice(5,2))+","+s(b);
                ByteBuffer ro=b.asReadOnlyBuffer().slice(2,3);
                a+="|"+ro.isReadOnly()+","+ro+","+rem(ro)+","+t(()->ro.put(0,(byte)1))+","+t(()->ro.array());
                ByteBuffer w=ByteBuffer.wrap(new byte[]{9,8,7,6,5},1,3);
                ByteBuffer ws=w.slice(0,4);
                ByteBuffer dup=w.duplicate().slice(1,2);
                return a+"|"+ws.arrayOffset()+","+rem(ws)+","+s(w)+","+dup.arrayOffset()+","+rem(dup)+","+dup.getShort(0)+","+t(()->ws.slice(3,1).getFloat(0))+","+Integer.toHexString(Float.floatToRawIntBits(ws.getFloat(0)));
            }
        }
        """;

    @Test void floatAndDoubleAccessors() throws Exception {
        matchesJvm(FIXTURE, "floatsAndDoubles");
    }

    @Test void nanPayloadsRoundTripBitExactly() throws Exception {
        matchesJvm(FIXTURE, "nanPayloads");
    }

    @Test void absoluteBulkArrayTransfers() throws Exception {
        matchesJvm(FIXTURE, "absoluteBulkArrays");
    }

    @Test void absoluteBulkBufferTransfer() throws Exception {
        matchesJvm(FIXTURE, "absoluteBulkBuffer");
    }

    @Test void sliceWithIndexAndLength() throws Exception {
        matchesJvm(FIXTURE, "sliceIndexLength");
    }

    @Test void leavesOutViewBuffersAndMismatch() {
        rejectsUnusedCall("java/nio/ByteBuffer", "asFloatBuffer", "()Ljava/nio/FloatBuffer;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/nio/ByteBuffer", "asDoubleBuffer", "()Ljava/nio/DoubleBuffer;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/nio/ByteBuffer", "mismatch", "(Ljava/nio/ByteBuffer;)I", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/nio/ByteBuffer", "alignedSlice", "(I)Ljava/nio/ByteBuffer;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/nio/ByteBuffer", "allocateDirect", "(I)Ljava/nio/ByteBuffer;", INVOKESTATIC, false);
    }
}
