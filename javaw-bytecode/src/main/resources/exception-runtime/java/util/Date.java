package java.util;

import java.time.Instant;

/** Millisecond value with Instant conversions; no wall clock, calendar or time-zone state. */
public class Date implements java.io.Serializable, Cloneable, Comparable<Date> {
    private transient long fastTime;

    public Date(long date) {
        fastTime = date;
    }

    public long getTime() {
        return fastTime;
    }

    public void setTime(long time) {
        fastTime = time;
    }

    public boolean before(Date when) {
        return getMillisOf(this) < getMillisOf(when);
    }

    public boolean after(Date when) {
        return getMillisOf(this) > getMillisOf(when);
    }

    /** JDK form: no null check; getClass() throws on null, subclasses are read through getTime(). */
    static final long getMillisOf(Date date) {
        if (date.getClass() != Date.class) return date.getTime();
        return date.fastTime;
    }

    public int compareTo(Date anotherDate) {
        long thisTime = getMillisOf(this);
        long anotherTime = getMillisOf(anotherDate);
        return thisTime < anotherTime ? -1 : (thisTime == anotherTime ? 0 : 1);
    }

    public boolean equals(Object obj) {
        return obj instanceof Date && getTime() == ((Date) obj).getTime();
    }

    public int hashCode() {
        long ht = this.getTime();
        return (int) ht ^ (int) (ht >> 32);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public static Date from(Instant instant) {
        try {
            return new Date(instant.toEpochMilli());
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public Instant toInstant() {
        return Instant.ofEpochMilli(getTime());
    }
}
