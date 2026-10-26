package dastec.one.javaw.concurrent;

/**
 * Strongly-typed Java facade for WebAssembly 32-bit atomic integer operations.
 */
public class AtomicInteger {
    private int value;

    public AtomicInteger() {
        this.value = 0;
    }

    public AtomicInteger(int initialValue) {
        this.value = initialValue;
    }

    public int get() {
        return value;
    }

    public void set(int newValue) {
        this.value = newValue;
    }

    public int getAndSet(int newValue) {
        int prev = this.value;
        this.value = newValue;
        return prev;
    }

    public boolean compareAndSet(int expect, int update) {
        if (this.value == expect) {
            this.value = update;
            return true;
        }
        return false;
    }

    public int getAndIncrement() {
        return getAndAdd(1);
    }

    public int getAndDecrement() {
        return getAndAdd(-1);
    }

    public int getAndAdd(int delta) {
        int prev = this.value;
        this.value += delta;
        return prev;
    }

    public int incrementAndGet() {
        return addAndGet(1);
    }

    public int decrementAndGet() {
        return addAndGet(-1);
    }

    public int addAndGet(int delta) {
        this.value += delta;
        return this.value;
    }

    public int getAndAnd(int mask) {
        int prev = this.value;
        this.value &= mask;
        return prev;
    }

    public int getAndOr(int mask) {
        int prev = this.value;
        this.value |= mask;
        return prev;
    }

    public int getAndXor(int mask) {
        int prev = this.value;
        this.value ^= mask;
        return prev;
    }
}
