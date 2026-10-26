package dastec.one.javaw.simd;

/**
 * 128-bit SIMD vector representing four 32-bit floating-point numbers (f32x4).
 * Compiles directly to native WebAssembly v128 vector instructions.
 */
public class Float32x4 {

    private final float x;
    private final float y;
    private final float z;
    private final float w;

    public Float32x4(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public static Float32x4 of(float x, float y, float z, float w) {
        return new Float32x4(x, y, z, w);
    }

    public static Float32x4 of(double x, double y, double z, double w) {
        return new Float32x4((float) x, (float) y, (float) z, (float) w);
    }

    public static Float32x4 splat(float val) {
        return new Float32x4(val, val, val, val);
    }

    public static Float32x4 splat(double val) {
        return splat((float) val);
    }

    public static Float32x4 zero() {
        return new Float32x4(0f, 0f, 0f, 0f);
    }

    public Float32x4 add(Float32x4 other) {
        return new Float32x4(this.x + other.x, this.y + other.y, this.z + other.z, this.w + other.w);
    }

    public Float32x4 sub(Float32x4 other) {
        return new Float32x4(this.x - other.x, this.y - other.y, this.z - other.z, this.w - other.w);
    }

    public Float32x4 mul(Float32x4 other) {
        return new Float32x4(this.x * other.x, this.y * other.y, this.z * other.z, this.w * other.w);
    }

    public Float32x4 div(Float32x4 other) {
        return new Float32x4(this.x / other.x, this.y / other.y, this.z / other.z, this.w / other.w);
    }

    public Float32x4 min(Float32x4 other) {
        return new Float32x4(Math.min(this.x, other.x), Math.min(this.y, other.y), Math.min(this.z, other.z), Math.min(this.w, other.w));
    }

    public Float32x4 max(Float32x4 other) {
        return new Float32x4(Math.max(this.x, other.x), Math.max(this.y, other.y), Math.max(this.z, other.z), Math.max(this.w, other.w));
    }

    public Float32x4 sqrt() {
        return new Float32x4((float) Math.sqrt(this.x), (float) Math.sqrt(this.y), (float) Math.sqrt(this.z), (float) Math.sqrt(this.w));
    }

    public Float32x4 abs() {
        return new Float32x4(Math.abs(this.x), Math.abs(this.y), Math.abs(this.z), Math.abs(this.w));
    }

    public Float32x4 neg() {
        return new Float32x4(-this.x, -this.y, -this.z, -this.w);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getW() { return w; }

    public float get(int lane) {
        return switch (lane) {
            case 0 -> x;
            case 1 -> y;
            case 2 -> z;
            case 3 -> w;
            default -> 0f;
        };
    }

    public float extractLane(int lane) {
        return get(lane);
    }

    public Float32x4 withLane(int lane, float val) {
        return switch (lane) {
            case 0 -> new Float32x4(val, y, z, w);
            case 1 -> new Float32x4(x, val, z, w);
            case 2 -> new Float32x4(x, y, val, w);
            case 3 -> new Float32x4(x, y, z, val);
            default -> this;
        };
    }

    public Float32x4 replaceLane(int lane, float val) {
        return withLane(lane, val);
    }

    public Float32x4 withX(float val) { return withLane(0, val); }
    public Float32x4 withY(float val) { return withLane(1, val); }
    public Float32x4 withZ(float val) { return withLane(2, val); }
    public Float32x4 withW(float val) { return withLane(3, val); }

    public static Float32x4 load(float[] array, int offset) {
        return new Float32x4(array[offset], array[offset + 1], array[offset + 2], array[offset + 3]);
    }

    public static Float32x4 load(int memoryAddress) {
        return zero();
    }

    public void store(float[] array, int offset) {
        array[offset] = x;
        array[offset + 1] = y;
        array[offset + 2] = z;
        array[offset + 3] = w;
    }

    public void store(int memoryAddress) {
    }

    public float dot(Float32x4 other) {
        return (x * other.x) + (y * other.y) + (z * other.z) + (w * other.w);
    }

    public float sum() {
        return x + y + z + w;
    }
}
