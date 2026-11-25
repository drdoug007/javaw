package java.nio;

/**
 * Abstract superclass of the heap byte buffer. The state invariant is
 * {@code -1 <= mark <= position <= limit <= capacity}.
 */
public abstract class Buffer {
    private int mark = -1;
    private int position;
    private int limit;
    private final int capacity;

    Buffer(int mark, int pos, int lim, int cap) {
        if (cap < 0) throw createCapacityException(cap);
        this.capacity = cap;
        limit(lim);
        position(pos);
        if (mark >= 0) this.mark = mark;
    }

    static IllegalArgumentException createCapacityException(int capacity) {
        return new IllegalArgumentException("capacity < 0: (" + capacity + " < 0)");
    }

    public final int capacity() {return capacity;}
    public final int position() {return position;}
    public final int limit() {return limit;}

    public Buffer position(int newPosition) {
        if (newPosition > limit)
            throw new IllegalArgumentException("newPosition > limit: (" + newPosition + " > " + limit + ")");
        if (newPosition < 0)
            throw new IllegalArgumentException("newPosition < 0: (" + newPosition + " < 0)");
        if (mark > newPosition) mark = -1;
        position = newPosition;
        return this;
    }

    public Buffer limit(int newLimit) {
        if (newLimit > capacity)
            throw new IllegalArgumentException("newLimit > capacity: (" + newLimit + " > " + capacity + ")");
        if (newLimit < 0)
            throw new IllegalArgumentException("newLimit < 0: (" + newLimit + " < 0)");
        limit = newLimit;
        if (position > newLimit) position = newLimit;
        if (mark > newLimit) mark = -1;
        return this;
    }

    public Buffer mark() {
        mark = position;
        return this;
    }

    public Buffer reset() {
        if (mark < 0) throw new InvalidMarkException();
        position = mark;
        return this;
    }

    public Buffer clear() {
        position = 0;
        limit = capacity;
        mark = -1;
        return this;
    }

    public Buffer flip() {
        limit = position;
        position = 0;
        mark = -1;
        return this;
    }

    public Buffer rewind() {
        position = 0;
        mark = -1;
        return this;
    }

    public final int remaining() {return limit - position;}
    public final boolean hasRemaining() {return position < limit;}

    public abstract boolean isReadOnly();
    public abstract boolean hasArray();
    public abstract Object array();
    public abstract int arrayOffset();
    public abstract boolean isDirect();
    public abstract Buffer slice();
    public abstract Buffer duplicate();

    final int markValue() {return mark;}
    final void discardMark() {mark = -1;}

    final int nextGetIndex(int nb) {
        if (limit - position < nb) throw new BufferUnderflowException();
        int result = position;
        position += nb;
        return result;
    }

    final int nextPutIndex(int nb) {
        if (limit - position < nb) throw new BufferOverflowException();
        int result = position;
        position += nb;
        return result;
    }

    final int checkIndex(int i, int nb) {
        if (i < 0 || nb > limit - i) throw new IndexOutOfBoundsException();
        return i;
    }
}
