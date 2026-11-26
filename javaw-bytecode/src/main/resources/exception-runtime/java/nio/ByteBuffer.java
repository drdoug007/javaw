package java.nio;

/**
 * Heap byte buffers, implemented by the package-private {@link HeapByteBuffer}.
 * Relative accesses use {@code nextGetIndex}/{@code nextPutIndex}; absolute
 * accesses check against the <em>limit</em>, not the capacity.
 */
public abstract class ByteBuffer extends Buffer implements Comparable<ByteBuffer> {
    final byte[] hb;
    final int offset;
    final boolean isReadOnly;
    boolean bigEndian = true;

    ByteBuffer(int mark, int pos, int lim, int cap, byte[] hb, int offset, boolean readOnly) {
        super(mark, pos, lim, cap);
        this.hb = hb;
        this.offset = offset;
        this.isReadOnly = readOnly;
    }

    public static ByteBuffer allocate(int capacity) {
        if (capacity < 0) throw createCapacityException(capacity);
        return new HeapByteBuffer(new byte[capacity], -1, 0, capacity, capacity, 0, false);
    }

    public static ByteBuffer wrap(byte[] array, int offset, int length) {
        int size = array.length;
        if (offset < 0 || offset > size || length < 0 || length > size - offset)
            throw new IndexOutOfBoundsException();
        return new HeapByteBuffer(array, -1, offset, offset + length, size, 0, false);
    }

    public static ByteBuffer wrap(byte[] array) {
        return wrap(array, 0, array.length);
    }

    public abstract ByteBuffer slice();
    public abstract ByteBuffer slice(int index, int length);
    public abstract ByteBuffer duplicate();
    public abstract ByteBuffer asReadOnlyBuffer();
    public abstract byte get();
    public abstract ByteBuffer put(byte b);
    public abstract byte get(int index);
    public abstract ByteBuffer put(int index, byte b);
    public abstract ByteBuffer compact();

    public final boolean hasArray() {return hb != null && !isReadOnly;}
    public final byte[] array() {
        if (isReadOnly) throw new ReadOnlyBufferException();
        return hb;
    }

    public final int arrayOffset() {
        if (isReadOnly) throw new ReadOnlyBufferException();
        return offset;
    }

    public boolean isDirect() {return false;}

    // Covariant overrides; javac emits the Buffer-returning bridges for Java 8 callers.
    public ByteBuffer position(int newPosition) {super.position(newPosition); return this;}
    public ByteBuffer limit(int newLimit) {super.limit(newLimit); return this;}
    public ByteBuffer mark() {super.mark(); return this;}
    public ByteBuffer reset() {super.reset(); return this;}
    public ByteBuffer clear() {super.clear(); return this;}
    public ByteBuffer flip() {super.flip(); return this;}
    public ByteBuffer rewind() {super.rewind(); return this;}

    public final ByteOrder order() {
        return bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    }

    public final ByteBuffer order(ByteOrder bo) {
        bigEndian = (bo == ByteOrder.BIG_ENDIAN);
        return this;
    }

    public ByteBuffer get(byte[] dst, int offset, int length) {
        checkRange(dst.length, offset, length);
        if (length > limit() - position()) throw new BufferUnderflowException();
        System.arraycopy(hb, this.offset + position(), dst, offset, length);
        position(position() + length);
        return this;
    }

    public ByteBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    public ByteBuffer put(ByteBuffer src) {
        // JDK: the read-only class (HeapByteBufferR) throws before any other check, also for put(itself).
        if (isReadOnly) throw new ReadOnlyBufferException();
        if (src == this) throw new IllegalArgumentException("The source buffer is this buffer");
        int n = Math.max(src.limit() - src.position(), 0);
        if (n > remaining()) throw new BufferOverflowException();
        byte[] tmp = new byte[n];
        System.arraycopy(src.hb, src.offset + src.position(), tmp, 0, n);
        System.arraycopy(tmp, 0, hb, offset + position(), n);
        position(position() + n);
        src.position(src.position() + n);
        return this;
    }

    public ByteBuffer put(byte[] src, int offset, int length) {
        if (isReadOnly) throw new ReadOnlyBufferException();
        checkRange(src.length, offset, length);
        if (length > limit() - position()) throw new BufferOverflowException();
        System.arraycopy(src, offset, hb, this.offset + position(), length);
        position(position() + length);
        return this;
    }

    public final ByteBuffer put(byte[] src) {
        return put(src, 0, src.length);
    }

    // Absolute bulk transfers: they check against the limit (never the position) and do not move it.
    public ByteBuffer get(int index, byte[] dst, int offset, int length) {
        checkRange(limit(), index, length);
        checkRange(dst.length, offset, length);
        System.arraycopy(hb, this.offset + index, dst, offset, length);
        return this;
    }

    public ByteBuffer get(int index, byte[] dst) {
        return get(index, dst, 0, dst.length);
    }

    public ByteBuffer put(int index, byte[] src, int offset, int length) {
        if (isReadOnly) throw new ReadOnlyBufferException();
        checkRange(limit(), index, length);
        checkRange(src.length, offset, length);
        System.arraycopy(src, offset, hb, this.offset + index, length);
        return this;
    }

    public ByteBuffer put(int index, byte[] src) {
        return put(index, src, 0, src.length);
    }

    public ByteBuffer put(int index, ByteBuffer src, int offset, int length) {
        if (isReadOnly) throw new ReadOnlyBufferException();
        checkRange(limit(), index, length);
        checkRange(src.limit(), offset, length);
        System.arraycopy(src.hb, src.offset + offset, hb, this.offset + index, length);
        return this;
    }

    /** {@code size} is the array length; the JDK message names that length last. Package-private so HeapByteBuffer reuses it. */
    static void checkRange(int size, int from, int length) {
        if ((length | from | size) < 0 || length > size - from)
            throw new IndexOutOfBoundsException("Range [" + from + ", " + from + " + " + length
                + ") out of bounds for length " + size);
    }

    private long readValue(int i, int nb) {
        long v = 0;
        if (bigEndian) {
            for (int j = 0; j < nb; j++)
                v = (v << 8) | (hb[offset + i + j] & 0xff);
        } else {
            for (int j = nb - 1; j >= 0; j--)
                v = (v << 8) | (hb[offset + i + j] & 0xff);
        }
        return v;
    }

    private void writeValue(int i, long v, int nb) {
        if (bigEndian) {
            for (int j = 0; j < nb; j++)
                hb[offset + i + j] = (byte) (v >>> (8 * (nb - 1 - j)));
        } else {
            for (int j = 0; j < nb; j++)
                hb[offset + i + j] = (byte) (v >>> (8 * j));
        }
    }

    public short getShort() {
        return (short) readValue(nextGetIndex(2), 2);
    }

    public short getShort(int index) {
        return (short) readValue(checkIndex(index, 2), 2);
    }

    public ByteBuffer putShort(short s) {
        if (isReadOnly) throw new ReadOnlyBufferException();
        writeValue(nextPutIndex(2), s, 2);
        return this;
    }

    public ByteBuffer putShort(int index, short s) {
        if (isReadOnly) throw new ReadOnlyBufferException();
        writeValue(checkIndex(index, 2), s, 2);
        return this;
    }

    public char getChar() {
        return (char) readValue(nextGetIndex(2), 2);
    }

    public char getChar(int index) {
        return (char) readValue(checkIndex(index, 2), 2);
    }

    public ByteBuffer putChar(char c) {
        if (isReadOnly) throw new ReadOnlyBufferException();
        writeValue(nextPutIndex(2), c, 2);
        return this;
    }

    public ByteBuffer putChar(int index, char c) {
        if (isReadOnly) throw new ReadOnlyBufferException();
        writeValue(checkIndex(index, 2), c, 2);
        return this;
    }

    public int getInt() {
        return (int) readValue(nextGetIndex(4), 4);
    }

    public int getInt(int index) {
        return (int) readValue(checkIndex(index, 4), 4);
    }

    public ByteBuffer putInt(int i) {
        if (isReadOnly) throw new ReadOnlyBufferException();
        writeValue(nextPutIndex(4), i, 4);
        return this;
    }

    public ByteBuffer putInt(int index, int i) {
        if (isReadOnly) throw new ReadOnlyBufferException();
        writeValue(checkIndex(index, 4), i, 4);
        return this;
    }

    public long getLong() {
        return readValue(nextGetIndex(8), 8);
    }

    public long getLong(int index) {
        return readValue(checkIndex(index, 8), 8);
    }

    public ByteBuffer putLong(long l) {
        if (isReadOnly) throw new ReadOnlyBufferException();
        writeValue(nextPutIndex(8), l, 8);
        return this;
    }

    public ByteBuffer putLong(int index, long l) {
        if (isReadOnly) throw new ReadOnlyBufferException();
        writeValue(checkIndex(index, 8), l, 8);
        return this;
    }

    // Float and double: raw conversions keep NaN payloads bit-exactly.
    public float getFloat() {
        return Float.intBitsToFloat(getInt());
    }

    public float getFloat(int index) {
        return Float.intBitsToFloat(getInt(index));
    }

    public ByteBuffer putFloat(float value) {
        return putInt(Float.floatToRawIntBits(value));
    }

    public ByteBuffer putFloat(int index, float value) {
        return putInt(index, Float.floatToRawIntBits(value));
    }

    public double getDouble() {
        return Double.longBitsToDouble(getLong());
    }

    public double getDouble(int index) {
        return Double.longBitsToDouble(getLong(index));
    }

    public ByteBuffer putDouble(double value) {
        return putLong(Double.doubleToRawLongBits(value));
    }

    public ByteBuffer putDouble(int index, double value) {
        return putLong(index, Double.doubleToRawLongBits(value));
    }

    public String toString() {
        return (isReadOnly ? "java.nio.HeapByteBufferR" : "java.nio.HeapByteBuffer")
            + "[pos=" + position() + " lim=" + limit() + " cap=" + capacity() + "]";
    }

    public int hashCode() {
        int h = 1;
        for (int i = limit() - 1; i >= position(); i--)
            h = 31 * h + get(i);
        return h;
    }

    public boolean equals(Object ob) {
        if (ob == this) return true;
        if (!(ob instanceof ByteBuffer)) return false;
        ByteBuffer that = (ByteBuffer) ob;
        int rem = remaining();
        if (that.remaining() != rem) return false;
        for (int i = 0; i < rem; i++)
            if (get(position() + i) != that.get(that.position() + i)) return false;
        return true;
    }

    public int compareTo(ByteBuffer that) {
        if (that == null) throw new NullPointerException();
        int thisRem = remaining();
        int thatRem = that.remaining();
        int length = Math.min(thisRem, thatRem);
        if (length < 0) return -1;
        for (int i = 0; i < length; i++) {
            int x = get(position() + i);
            int y = that.get(that.position() + i);
            if (x != y) return x - y;
        }
        return thisRem - thatRem;
    }
}
