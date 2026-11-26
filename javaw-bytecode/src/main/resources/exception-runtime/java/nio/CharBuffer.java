package java.nio;

/**
 * Character buffers: the package-private {@link HeapCharBuffer} serves writable
 * and read-only heap buffers, and the read-only {@link StringCharBuffer} is a
 * live view of a {@code CharSequence}. Relative accesses use
 * {@code nextGetIndex}/{@code nextPutIndex}; absolute accesses check against
 * the <em>limit</em>, not the capacity.
 */
public abstract class CharBuffer extends Buffer implements Comparable<CharBuffer>, Appendable, CharSequence {
    final char[] hb;
    final int offset;
    final boolean isReadOnly;

    CharBuffer(int mark, int pos, int lim, int cap, char[] hb, int offset, boolean readOnly) {
        super(mark, pos, lim, cap);
        this.hb = hb;
        this.offset = offset;
        this.isReadOnly = readOnly;
    }

    public static CharBuffer allocate(int capacity) {
        if (capacity < 0) throw createCapacityException(capacity);
        return new HeapCharBuffer(new char[capacity], -1, 0, capacity, capacity, 0, false);
    }

    public static CharBuffer wrap(char[] array, int offset, int length) {
        int size = array.length;
        if (offset < 0 || offset > size || length < 0 || length > size - offset)
            throw new IndexOutOfBoundsException();
        return new HeapCharBuffer(array, -1, offset, offset + length, size, 0, false);
    }

    public static CharBuffer wrap(char[] array) {
        return wrap(array, 0, array.length);
    }

    public static CharBuffer wrap(CharSequence csq, int start, int end) {
        int size = csq.length();
        if (start < 0 || start > end || end > size)
            throw new IndexOutOfBoundsException();
        return new StringCharBuffer(csq, -1, start, end, size, 0);
    }

    public static CharBuffer wrap(CharSequence csq) {
        return wrap(csq, 0, csq.length());
    }

    public abstract CharBuffer slice();
    public abstract CharBuffer duplicate();
    public abstract CharBuffer asReadOnlyBuffer();
    public abstract char get();
    public abstract CharBuffer put(char c);
    public abstract char get(int index);
    public abstract CharBuffer put(int index, char c);
    public abstract CharBuffer compact();
    public abstract CharBuffer subSequence(int start, int end);
    public abstract ByteOrder order();
    abstract String toString(int start, int end);

    public CharBuffer get(char[] dst, int offset, int length) {
        checkRange(dst.length, offset, length);
        if (length > limit() - position()) throw new BufferUnderflowException();
        for (int i = 0; i < length; i++) dst[offset + i] = get(position() + i);
        position(position() + length);
        return this;
    }

    public CharBuffer get(char[] dst) {
        return get(dst, 0, dst.length);
    }

    public CharBuffer put(char[] src, int offset, int length) {
        if (isReadOnly) throw new ReadOnlyBufferException();
        checkRange(src.length, offset, length);
        if (length > limit() - position()) throw new BufferOverflowException();
        for (int i = 0; i < length; i++) put(position() + i, src[offset + i]);
        position(position() + length);
        return this;
    }

    public final CharBuffer put(char[] src) {
        return put(src, 0, src.length);
    }

    // A read-only heap buffer throws before the range check; a string buffer throws after it.
    public CharBuffer put(String src, int start, int end) {
        if (isReadOnly && hb != null) throw new ReadOnlyBufferException();
        checkRange(src.length(), start, end - start);
        if (isReadOnly) throw new ReadOnlyBufferException();
        if (end - start > limit() - position()) throw new BufferOverflowException();
        for (int i = 0; i < end - start; i++) put(position() + i, src.charAt(start + i));
        position(position() + end - start);
        return this;
    }

    public final CharBuffer put(String src) {
        return put(src, 0, src.length());
    }

    public CharBuffer put(CharBuffer src) {
        // The JDK read-only heap class throws before any other check, also for put(itself).
        if (isReadOnly && hb != null) throw new ReadOnlyBufferException();
        if (src == this) throw new IllegalArgumentException("The source buffer is this buffer");
        if (isReadOnly) throw new ReadOnlyBufferException();
        int n = Math.max(src.limit() - src.position(), 0);
        if (n > remaining()) throw new BufferOverflowException();
        // The source may share the backing array (duplicates) or be a string buffer:
        // copy through a temporary so overlapping ranges in either direction work.
        char[] tmp = new char[n];
        for (int i = 0; i < n; i++) tmp[i] = src.get(src.position() + i);
        for (int i = 0; i < n; i++) put(position() + i, tmp[i]);
        position(position() + n);
        src.position(src.position() + n);
        return this;
    }

    // Covariant overrides; javac emits the Buffer-returning bridges for Java 8 callers.
    public final CharBuffer position(int newPosition) {super.position(newPosition); return this;}
    public final CharBuffer limit(int newLimit) {super.limit(newLimit); return this;}
    public final CharBuffer mark() {super.mark(); return this;}
    public final CharBuffer reset() {super.reset(); return this;}
    public final CharBuffer clear() {super.clear(); return this;}
    public final CharBuffer flip() {super.flip(); return this;}
    public final CharBuffer rewind() {super.rewind(); return this;}

    public final boolean hasArray() {return hb != null && !isReadOnly;}

    public final char[] array() {
        if (hb == null) throw new UnsupportedOperationException();
        if (isReadOnly) throw new ReadOnlyBufferException();
        return hb;
    }

    public final int arrayOffset() {
        if (hb == null) throw new UnsupportedOperationException();
        if (isReadOnly) throw new ReadOnlyBufferException();
        return offset;
    }

    public boolean isDirect() {return false;}

    // CharSequence: the length and relative accesses are relative to the position.
    public final int length() {return remaining();}
    public final boolean isEmpty() {return remaining() == 0;}
    public final char charAt(int index) {return get(position() + checkIndex(index, 1));}

    /** The remaining chars, position to limit; the position does not move. */
    public String toString() {return toString(position(), limit());}

    public CharBuffer append(CharSequence csq) {
        if (isReadOnly && hb != null) throw new ReadOnlyBufferException();
        if (csq == null) return put("null");
        if (csq instanceof CharBuffer) return put((CharBuffer) csq);
        return put(csq.toString());
    }

    public CharBuffer append(CharSequence csq, int start, int end) {
        if (isReadOnly && hb != null) throw new ReadOnlyBufferException();
        if (csq == null) csq = "null";
        if (start < 0 || start > end || end > csq.length())
            throw new IndexOutOfBoundsException("Range [" + start + ", " + end
                + ") out of bounds for length " + csq.length());
        return put(csq.subSequence(start, end).toString());
    }

    public CharBuffer append(char c) {
        return put(c);
    }

    public int hashCode() {
        int h = 1;
        for (int i = limit() - 1; i >= position(); i--)
            h = 31 * h + get(i);
        return h;
    }

    public boolean equals(Object ob) {
        if (ob == this) return true;
        if (!(ob instanceof CharBuffer)) return false;
        CharBuffer that = (CharBuffer) ob;
        int rem = remaining();
        if (that.remaining() != rem) return false;
        for (int i = 0; i < rem; i++)
            if (get(position() + i) != that.get(that.position() + i)) return false;
        return true;
    }

    public int compareTo(CharBuffer that) {
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

    /** {@code length} is the array length; the JDK message names that length last. */
    static void checkRange(int length, int from, int size) {
        if ((size | from | length) < 0 || size > length - from)
            throw new IndexOutOfBoundsException("Range [" + from + ", " + from + " + " + size
                + ") out of bounds for length " + length);
    }
}
