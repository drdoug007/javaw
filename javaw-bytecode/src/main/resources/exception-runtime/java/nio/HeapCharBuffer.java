package java.nio;

/**
 * The single heap character implementation, serving both writable and
 * read-only buffers (selected by the {@code isReadOnly} flag). All views
 * share the backing array.
 */
final class HeapCharBuffer extends CharBuffer {
    HeapCharBuffer(char[] hb, int mark, int pos, int lim, int cap, int offset, boolean readOnly) {
        super(mark, pos, lim, cap, hb, offset, readOnly);
    }

    public boolean isReadOnly() {return isReadOnly;}

    public ByteOrder order() {return ByteOrder.nativeOrder();}

    public CharBuffer slice() {
        int rem = remaining();
        return new HeapCharBuffer(hb, -1, 0, rem, rem, offset + position(), isReadOnly);
    }

    public CharBuffer duplicate() {
        return new HeapCharBuffer(hb, markValue(), position(), limit(), capacity(), offset, isReadOnly);
    }

    public CharBuffer asReadOnlyBuffer() {
        return new HeapCharBuffer(hb, markValue(), position(), limit(), capacity(), offset, true);
    }

    public char get() {
        return hb[offset + nextGetIndex(1)];
    }

    public char get(int index) {
        return hb[offset + checkIndex(index, 1)];
    }

    public CharBuffer put(char c) {
        if (isReadOnly) throw new ReadOnlyBufferException();
        hb[offset + nextPutIndex(1)] = c;
        return this;
    }

    public CharBuffer put(int index, char c) {
        if (isReadOnly) throw new ReadOnlyBufferException();
        hb[offset + checkIndex(index, 1)] = c;
        return this;
    }

    public CharBuffer compact() {
        if (isReadOnly) throw new ReadOnlyBufferException();
        int rem = remaining();
        if (rem > 0) System.arraycopy(hb, offset + position(), hb, offset, rem);
        position(rem);
        limit(capacity());
        discardMark();
        return this;
    }

    /** Relative to the position: it keeps the capacity and offset, only position and limit move. */
    public CharBuffer subSequence(int start, int end) {
        int pos = position();
        int n = limit() - pos;
        if (start < 0 || start > end || end > n)
            throw new IndexOutOfBoundsException("Range [" + start + ", " + end + ") out of bounds for length " + n);
        return new HeapCharBuffer(hb, -1, pos + start, pos + end, capacity(), offset, isReadOnly);
    }

    String toString(int start, int end) {
        return new String(hb, offset + start, end - start);
    }
}
