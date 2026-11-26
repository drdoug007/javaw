package java.nio;

/**
 * The single heap implementation, serving both writable and read-only
 * buffers (selected by the {@code isReadOnly} flag). All views share the
 * backing array and reset the byte order to big-endian, as the JDK does.
 */
final class HeapByteBuffer extends ByteBuffer {
    HeapByteBuffer(byte[] hb, int mark, int pos, int lim, int cap, int offset, boolean readOnly) {
        super(mark, pos, lim, cap, hb, offset, readOnly);
    }

    public boolean isReadOnly() {return isReadOnly;}

    public ByteBuffer slice() {
        int rem = remaining();
        return new HeapByteBuffer(hb, -1, 0, rem, rem, offset + position(), isReadOnly);
    }

    /** Absolute slice: offset is relative to index 0, never to the position, which is untouched. */
    public ByteBuffer slice(int index, int length) {
        checkRange(limit(), index, length);
        return new HeapByteBuffer(hb, -1, 0, length, length, offset + index, isReadOnly);
    }

    public ByteBuffer duplicate() {
        return new HeapByteBuffer(hb, markValue(), position(), limit(), capacity(), offset, isReadOnly);
    }

    public ByteBuffer asReadOnlyBuffer() {
        return new HeapByteBuffer(hb, markValue(), position(), limit(), capacity(), offset, true);
    }

    public byte get() {
        return hb[offset + nextGetIndex(1)];
    }

    public byte get(int index) {
        return hb[offset + checkIndex(index, 1)];
    }

    public ByteBuffer put(byte b) {
        if (isReadOnly) throw new ReadOnlyBufferException();
        hb[offset + nextPutIndex(1)] = b;
        return this;
    }

    public ByteBuffer put(int index, byte b) {
        if (isReadOnly) throw new ReadOnlyBufferException();
        hb[offset + checkIndex(index, 1)] = b;
        return this;
    }

    public ByteBuffer compact() {
        if (isReadOnly) throw new ReadOnlyBufferException();
        int rem = remaining();
        if (rem > 0) System.arraycopy(hb, offset + position(), hb, offset, rem);
        position(rem);
        limit(capacity());
        discardMark();
        return this;
    }
}
