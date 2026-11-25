package java.sql;

import java.time.Instant;

/** Whole-second Date milliseconds plus a nanosecond field; no time-zone or calendar state. */
public class Timestamp extends java.util.Date {
    private static final int MILLIS_PER_SECOND = 1000;
    private int nanos;

    public Timestamp(long time) {
        super((time / MILLIS_PER_SECOND) * MILLIS_PER_SECOND);
        nanos = (int) ((time % MILLIS_PER_SECOND) * 1000000);
        if (nanos < 0) {
            nanos = 1000000000 + nanos;
            super.setTime(((time / MILLIS_PER_SECOND) - 1) * MILLIS_PER_SECOND);
        }
    }

    public void setTime(long time) {
        super.setTime((time / MILLIS_PER_SECOND) * MILLIS_PER_SECOND);
        nanos = (int) ((time % MILLIS_PER_SECOND) * 1000000);
        if (nanos < 0) {
            nanos = 1000000000 + nanos;
            super.setTime(((time / MILLIS_PER_SECOND) - 1) * MILLIS_PER_SECOND);
        }
    }

    public long getTime() {
        long time = super.getTime();
        return (time + (nanos / 1000000));
    }

    public int getNanos() {
        return nanos;
    }

    public void setNanos(int n) {
        if (n > 999999999 || n < 0) {
            throw new IllegalArgumentException("nanos > 999999999 or < 0");
        }
        nanos = n;
    }

    public boolean equals(Timestamp ts) {
        if (super.equals(ts)) {
            return nanos == ts.nanos;
        }
        return false;
    }

    public boolean equals(Object ts) {
        return (ts instanceof Timestamp) ? equals((Timestamp) ts) : false;
    }

    public boolean before(Timestamp ts) {
        return compareTo(ts) < 0;
    }

    public boolean after(Timestamp ts) {
        return compareTo(ts) > 0;
    }

    public int compareTo(Timestamp ts) {
        long thisTime = this.getTime();
        long anotherTime = ts.getTime();
        int i = (thisTime < anotherTime ? -1 : (thisTime == anotherTime ? 0 : 1));
        if (i == 0) {
            if (nanos > ts.nanos) return 1;
            else if (nanos < ts.nanos) return -1;
        }
        return i;
    }

    public int compareTo(java.util.Date o) {
        if (o instanceof Timestamp) return compareTo((Timestamp) o);
        Timestamp ts = new Timestamp(o.getTime());
        return this.compareTo(ts);
    }

    public int hashCode() {
        return super.hashCode();
    }

    public static Timestamp from(Instant instant) {
        try {
            Timestamp stamp = new Timestamp(instant.getEpochSecond() * MILLIS_PER_SECOND);
            stamp.nanos = instant.getNano();
            return stamp;
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public Instant toInstant() {
        return Instant.ofEpochSecond(super.getTime() / MILLIS_PER_SECOND, nanos);
    }
}
