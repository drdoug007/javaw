package one.dastec.javaw.simd;

/**
 * 128-bit SIMD vector representing four 32-bit signed integers (i32x4).
 * Compiles directly to native WebAssembly v128 vector instructions.
 */
public class Int32x4 {

    private final int x;
    private final int y;
    private final int z;
    private final int w;

    public Int32x4(int x, int y, int z, int w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public static Int32x4 of(int x, int y, int z, int w) {
        return new Int32x4(x, y, z, w);
    }

    public static Int32x4 splat(int val) {
        return new Int32x4(val, val, val, val);
    }

    public static Int32x4 zero() {
        return new Int32x4(0, 0, 0, 0);
    }

    public Int32x4 add(Int32x4 other) {
        return new Int32x4(this.x + other.x, this.y + other.y, this.z + other.z, this.w + other.w);
    }

    public Int32x4 sub(Int32x4 other) {
        return new Int32x4(this.x - other.x, this.y - other.y, this.z - other.z, this.w - other.w);
    }

    public Int32x4 mul(Int32x4 other) {
        return new Int32x4(this.x * other.x, this.y * other.y, this.z * other.z, this.w * other.w);
    }

    public Int32x4 min(Int32x4 other) {
        return new Int32x4(Math.min(this.x, other.x), Math.min(this.y, other.y), Math.min(this.z, other.z), Math.min(this.w, other.w));
    }

    public Int32x4 max(Int32x4 other) {
        return new Int32x4(Math.max(this.x, other.x), Math.max(this.y, other.y), Math.max(this.z, other.z), Math.max(this.w, other.w));
    }

    public Int32x4 abs() {
        return new Int32x4(Math.abs(this.x), Math.abs(this.y), Math.abs(this.z), Math.abs(this.w));
    }

    public Int32x4 neg() {
        return new Int32x4(-this.x, -this.y, -this.z, -this.w);
    }

    public Int32x4 and(Int32x4 other) {
        return new Int32x4(this.x & other.x, this.y & other.y, this.z & other.z, this.w & other.w);
    }

    public Int32x4 or(Int32x4 other) {
        return new Int32x4(this.x | other.x, this.y | other.y, this.z | other.z, this.w | other.w);
    }

    public Int32x4 xor(Int32x4 other) {
        return new Int32x4(this.x ^ other.x, this.y ^ other.y, this.z ^ other.z, this.w ^ other.w);
    }

    public Int32x4 not() {
        return new Int32x4(~this.x, ~this.y, ~this.z, ~this.w);
    }

    public Int32x4 shiftLeft(int count) {
        return new Int32x4(this.x << count, this.y << count, this.z << count, this.w << count);
    }

    public Int32x4 shl(int count) {
        return shiftLeft(count);
    }

    public Int32x4 shiftRight(int count) {
        return new Int32x4(this.x >> count, this.y >> count, this.z >> count, this.w >> count);
    }

    public Int32x4 shr(int count) {
        return shiftRight(count);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public int getW() { return w; }

    public int get(int lane) {
        return switch (lane) {
            case 0 -> x;
            case 1 -> y;
            case 2 -> z;
            case 3 -> w;
            default -> 0;
        };
    }

    public int extractLane(int lane) {
        return get(lane);
    }

    public Int32x4 withLane(int lane, int val) {
        return switch (lane) {
            case 0 -> new Int32x4(val, y, z, w);
            case 1 -> new Int32x4(x, val, z, w);
            case 2 -> new Int32x4(x, y, val, w);
            case 3 -> new Int32x4(x, y, z, val);
            default -> this;
        };
    }

    public Int32x4 replaceLane(int lane, int val) {
        return withLane(lane, val);
    }

    public Int32x4 withX(int val) { return withLane(0, val); }
    public Int32x4 withY(int val) { return withLane(1, val); }
    public Int32x4 withZ(int val) { return withLane(2, val); }
    public Int32x4 withW(int val) { return withLane(3, val); }

    public static Int32x4 load(int[] array, int offset) {
        return new Int32x4(array[offset], array[offset + 1], array[offset + 2], array[offset + 3]);
    }

    public static Int32x4 load(int memoryAddress) {
        return zero();
    }

    public void store(int[] array, int offset) {
        array[offset] = x;
        array[offset + 1] = y;
        array[offset + 2] = z;
        array[offset + 3] = w;
    }

    public void store(int memoryAddress) {
    }

    public int sum() {
        return x + y + z + w;
    }
}
