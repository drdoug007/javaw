package java.nio;

/**
 * A read-only view of a {@code CharSequence}. Every access reads the
 * sequence live, so later changes to the wrapped sequence are visible.
 */
final class StringCharBuffer extends CharBuffer {
    final CharSequence str;

    StringCharBuffer(CharSequence str, int mark, int pos, int lim, int cap, int offset) {
        super(mark, pos, lim, cap, null, offset, true);
        this.str = str;
    }

    public boolean isReadOnly() {return true;}

    public ByteOrder order() {return ByteOrder.nativeOrder();}

    public CharBuffer slice() {
        int rem = remaining();
        return new StringCharBuffer(str, -1, 0, rem, rem, offset + position());
    }

    public CharBuffer duplicate() {
        return new StringCharBuffer(str, markValue(), position(), limit(), capacity(), offset);
    }

    public CharBuffer asReadOnlyBuffer() {
        return duplicate();
    }

    public char get() {
        return str.charAt(offset + nextGetIndex(1));
    }

    public char get(int index) {
        return str.charAt(offset + checkIndex(index, 1));
    }

    public CharBuffer put(char c) {
        throw new ReadOnlyBufferException();
    }

    public CharBuffer put(int index, char c) {
        throw new ReadOnlyBufferException();
    }

    public CharBuffer compact() {
        throw new ReadOnlyBufferException();
    }

    /** Relative to the position: it keeps the capacity and offset, only position and limit move. */
    public CharBuffer subSequence(int start, int end) {
        int pos = position();
        int n = limit() - pos;
        if (start < 0 || start > end || end > n)
            throw new IndexOutOfBoundsException();
        return new StringCharBuffer(str, -1, pos + start, pos + end, capacity(), offset);
    }

    String toString(int start, int end) {
        return str.subSequence(offset + start, offset + end).toString();
    }
}
