package dastec.one.javaw.concurrent;

/**
 * Low-level WebAssembly atomic memory operations and synchronization primitives.
 */
public class Atomics {

    public static int load(int[] array, int index) {
        return array[index];
    }

    public static void store(int[] array, int index, int value) {
        array[index] = value;
    }

    public static int add(int[] array, int index, int value) {
        int prev = array[index];
        array[index] += value;
        return prev;
    }

    public static int sub(int[] array, int index, int value) {
        int prev = array[index];
        array[index] -= value;
        return prev;
    }

    public static int and(int[] array, int index, int value) {
        int prev = array[index];
        array[index] &= value;
        return prev;
    }

    public static int or(int[] array, int index, int value) {
        int prev = array[index];
        array[index] |= value;
        return prev;
    }

    public static int xor(int[] array, int index, int value) {
        int prev = array[index];
        array[index] ^= value;
        return prev;
    }

    public static int exchange(int[] array, int index, int value) {
        int prev = array[index];
        array[index] = value;
        return prev;
    }

    public static int compareExchange(int[] array, int index, int expected, int replacement) {
        int prev = array[index];
        if (prev == expected) {
            array[index] = replacement;
        }
        return prev;
    }

    public static int wait(int[] array, int index, int expectedValue, long timeoutNs) {
        return 0; // 0 = "ok", 1 = "not-equal", 2 = "timed-out"
    }

    public static int notify(int[] array, int index, int count) {
        return 0; // returns number of waiters awakened
    }
}
