package java.util.concurrent;

public enum TimeUnit {
    NANOSECONDS(TimeUnit.NANO_SCALE), MICROSECONDS(TimeUnit.MICRO_SCALE), MILLISECONDS(TimeUnit.MILLI_SCALE),
    SECONDS(TimeUnit.SECOND_SCALE), MINUTES(TimeUnit.MINUTE_SCALE), HOURS(TimeUnit.HOUR_SCALE), DAYS(TimeUnit.DAY_SCALE);

    private static final long NANO_SCALE = 1L;
    private static final long MICRO_SCALE = 1000L * NANO_SCALE;
    private static final long MILLI_SCALE = 1000L * MICRO_SCALE;
    private static final long SECOND_SCALE = 1000L * MILLI_SCALE;
    private static final long MINUTE_SCALE = 60L * SECOND_SCALE;
    private static final long HOUR_SCALE = 60L * MINUTE_SCALE;
    private static final long DAY_SCALE = 24L * HOUR_SCALE;

    private final long scale, maxNanos, maxMicros, maxMillis, maxSecs;
    private final long microRatio;
    private final int milliRatio, secRatio;

    private TimeUnit(long s) {
        this.scale = s;
        this.maxNanos = Long.MAX_VALUE / s;
        long ur = (s >= MICRO_SCALE) ? (s / MICRO_SCALE) : (MICRO_SCALE / s);
        this.microRatio = ur;
        this.maxMicros = Long.MAX_VALUE / ur;
        ur = (s >= MILLI_SCALE) ? (s / MILLI_SCALE) : (MILLI_SCALE / s);
        this.milliRatio = (int) ur;
        this.maxMillis = Long.MAX_VALUE / ur;
        ur = (s >= SECOND_SCALE) ? (s / SECOND_SCALE) : (SECOND_SCALE / s);
        this.secRatio = (int) ur;
        this.maxSecs = Long.MAX_VALUE / ur;
    }

    public long convert(long sourceDuration, TimeUnit sourceUnit) {
        switch (this) {
            case NANOSECONDS: return sourceUnit.toNanos(sourceDuration);
            case MICROSECONDS: return sourceUnit.toMicros(sourceDuration);
            case MILLISECONDS: return sourceUnit.toMillis(sourceDuration);
            case SECONDS: return sourceUnit.toSeconds(sourceDuration);
            default: return cvt(sourceDuration, scale, sourceUnit.scale);
        }
    }

    public long toNanos(long duration) {
        long s, m;
        if ((s = scale) == NANO_SCALE) return duration;
        else if (duration > (m = maxNanos)) return Long.MAX_VALUE;
        else if (duration < -m) return Long.MIN_VALUE;
        else return duration * s;
    }

    public long toMicros(long duration) {
        long s, m;
        if ((s = scale) <= MICRO_SCALE) return (s == MICRO_SCALE) ? duration : duration / microRatio;
        else if (duration > (m = maxMicros)) return Long.MAX_VALUE;
        else if (duration < -m) return Long.MIN_VALUE;
        else return duration * microRatio;
    }

    public long toMillis(long duration) {
        long s, m;
        if ((s = scale) <= MILLI_SCALE) return (s == MILLI_SCALE) ? duration : duration / milliRatio;
        else if (duration > (m = maxMillis)) return Long.MAX_VALUE;
        else if (duration < -m) return Long.MIN_VALUE;
        else return duration * milliRatio;
    }

    public long toSeconds(long duration) {
        long s, m;
        if ((s = scale) <= SECOND_SCALE) return (s == SECOND_SCALE) ? duration : duration / secRatio;
        else if (duration > (m = maxSecs)) return Long.MAX_VALUE;
        else if (duration < -m) return Long.MIN_VALUE;
        else return duration * secRatio;
    }

    public long toMinutes(long duration) { return cvt(duration, MINUTE_SCALE, scale); }
    public long toHours(long duration) { return cvt(duration, HOUR_SCALE, scale); }
    public long toDays(long duration) { return cvt(duration, DAY_SCALE, scale); }

    private static long cvt(long d, long dst, long src) {
        if (src == dst) return d;
        else if (src < dst) return d / (dst / src);
        else {
            long r = src / dst;
            long m = Long.MAX_VALUE / r;
            if (d > m) return Long.MAX_VALUE;
            else if (d < -m) return Long.MIN_VALUE;
            else return d * r;
        }
    }
}
