package java.time;

import java.time.format.DateTimeParseException;

/** Core java.time.LocalTime: time of day with nanosecond precision. No Temporal, parse(format) or clock members. */
public final class LocalTime implements Comparable<LocalTime>, java.io.Serializable {
    private static final long NANOS_PER_DAY = 86_400_000_000_000L;

    private static final LocalTime[] HOURS = new LocalTime[24];
    static {
        for (int i = 0; i < 24; i++) {
            HOURS[i] = new LocalTime(i, 0, 0, 0);
        }
    }

    public static final LocalTime MIN = HOURS[0];
    public static final LocalTime MAX = new LocalTime(23, 59, 59, 999_999_999);
    public static final LocalTime MIDNIGHT = HOURS[0];
    public static final LocalTime NOON = HOURS[12];

    private final byte hour;
    private final byte minute;
    private final byte second;
    private final int nano;

    private LocalTime(int hour, int minute, int second, int nano) {
        this.hour = (byte) hour;
        this.minute = (byte) minute;
        this.second = (byte) second;
        this.nano = nano;
    }

    /** Returns the cached whole hour when minute, second and nano are all zero. */
    private static LocalTime create(int hour, int minute, int second, int nano) {
        return (minute | second | nano) == 0 ? HOURS[hour] : new LocalTime(hour, minute, second, nano);
    }

    public static LocalTime of(int hour, int minute) {
        check("HourOfDay", 23, hour);
        if (minute == 0) {
            return HOURS[hour];
        }
        check("MinuteOfHour", 59, minute);
        return create(hour, minute, 0, 0);
    }

    public static LocalTime of(int hour, int minute, int second) {
        check("HourOfDay", 23, hour);
        if ((minute | second) == 0) {
            return HOURS[hour];
        }
        check("MinuteOfHour", 59, minute);
        check("SecondOfMinute", 59, second);
        return create(hour, minute, second, 0);
    }

    public static LocalTime of(int hour, int minute, int second, int nanoOfSecond) {
        check("HourOfDay", 23, hour);
        check("MinuteOfHour", 59, minute);
        check("SecondOfMinute", 59, second);
        check("NanoOfSecond", 999_999_999, nanoOfSecond);
        return create(hour, minute, second, nanoOfSecond);
    }

    public static LocalTime ofSecondOfDay(long secondOfDay) {
        check("SecondOfDay", 86_399, secondOfDay);
        int sod = (int) secondOfDay;
        return create(sod / 3600, (sod / 60) % 60, sod % 60, 0);
    }

    public static LocalTime ofNanoOfDay(long nanoOfDay) {
        check("NanoOfDay", 86_399_999_999_999L, nanoOfDay);
        long nod = nanoOfDay;
        int hour = (int) (nod / 3_600_000_000_000L);
        nod %= 3_600_000_000_000L;
        int minute = (int) (nod / 60_000_000_000L);
        nod %= 60_000_000_000L;
        int second = (int) (nod / 1_000_000_000L);
        int nano = (int) (nod % 1_000_000_000L);
        return create(hour, minute, second, nano);
    }

    public static LocalTime parse(CharSequence text) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        int hour = two(text, 0);
        if (hour < 0) {
            throw parseError(text, 0);
        }
        int len = text.length();
        if (len < 3 || text.charAt(2) != ':') {
            throw parseError(text, 2);
        }
        int minute = two(text, 3);
        if (minute < 0) {
            throw parseError(text, 3);
        }
        int pos = 5;
        int second = 0;
        boolean haveSeconds = false;
        if (pos < len && text.charAt(pos) == ':') {
            int seconds = two(text, pos + 1);
            if (seconds >= 0) {
                second = seconds;
                pos += 3;
                haveSeconds = true;
            }
        }
        int nano = 0;
        if (haveSeconds && pos < len && text.charAt(pos) == '.') {
            pos++;
            long value = 0;
            int digits = 0;
            while (pos < len && digits < 9 && isDigit(text.charAt(pos))) {
                value = value * 10 + (text.charAt(pos) - '0');
                pos++;
                digits++;
            }
            for (; digits < 9; digits++) {
                value *= 10;
            }
            nano = (int) value;
        }
        if (pos < len) {
            throw unparsedError(text, pos);
        }
        try {
            check("MinuteOfHour", 59, minute);
            check("HourOfDay", 23, hour);
            check("SecondOfMinute", 59, second);
        } catch (DateTimeException ex) {
            throw new DateTimeParseException("Text '" + abbreviate(text) + "' could not be parsed: " + ex.getMessage(), text, 0, ex);
        }
        return create(hour, minute, second, nano);
    }

    private static void check(String name, long max, long value) {
        if (value < 0 || value > max) {
            throw new DateTimeException("Invalid value for " + name + " (valid values 0 - " + max + "): " + value);
        }
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public int getSecond() {
        return second;
    }

    public int getNano() {
        return nano;
    }

    public int toSecondOfDay() {
        return hour * 3600 + minute * 60 + second;
    }

    public long toNanoOfDay() {
        return hour * 3600_000_000_000L + minute * 60_000_000_000L + second * 1_000_000_000L + nano;
    }

    public LocalTime withHour(int value) {
        if (value == hour) {
            return this;
        }
        check("HourOfDay", 23, value);
        return create(value, minute, second, nano);
    }

    public LocalTime withMinute(int value) {
        if (value == minute) {
            return this;
        }
        check("MinuteOfHour", 59, value);
        return create(hour, value, second, nano);
    }

    public LocalTime withSecond(int value) {
        if (value == second) {
            return this;
        }
        check("SecondOfMinute", 59, value);
        return create(hour, minute, value, nano);
    }

    public LocalTime withNano(int value) {
        if (value == nano) {
            return this;
        }
        check("NanoOfSecond", 999_999_999, value);
        return create(hour, minute, second, value);
    }

    public LocalTime plusHours(long hoursToAdd) {
        if (hoursToAdd == 0) {
            return this;
        }
        int newHour = ((int) (hoursToAdd % 24) + hour + 24) % 24;
        return create(newHour, minute, second, nano);
    }

    public LocalTime plusMinutes(long minutesToAdd) {
        if (minutesToAdd == 0) {
            return this;
        }
        int mofd = hour * 60 + minute;
        int newMofd = ((int) (minutesToAdd % 1440) + mofd + 1440) % 1440;
        if (mofd == newMofd) {
            return this;
        }
        return create(newMofd / 60, newMofd % 60, second, nano);
    }

    public LocalTime plusSeconds(long secondsToAdd) {
        if (secondsToAdd == 0) {
            return this;
        }
        int sofd = toSecondOfDay();
        int newSofd = ((int) (secondsToAdd % 86_400) + sofd + 86_400) % 86_400;
        if (sofd == newSofd) {
            return this;
        }
        return create(newSofd / 3600, (newSofd / 60) % 60, newSofd % 60, nano);
    }

    public LocalTime plusNanos(long nanosToAdd) {
        if (nanosToAdd == 0) {
            return this;
        }
        long nofd = toNanoOfDay();
        long newNofd = ((nanosToAdd % NANOS_PER_DAY) + nofd + NANOS_PER_DAY) % NANOS_PER_DAY;
        if (nofd == newNofd) {
            return this;
        }
        return ofNanoOfDay(newNofd);
    }

    public LocalTime minusHours(long hoursToSubtract) {
        return plusHours(-(hoursToSubtract % 24));
    }

    public LocalTime minusMinutes(long minutesToSubtract) {
        return plusMinutes(-(minutesToSubtract % 1440));
    }

    public LocalTime minusSeconds(long secondsToSubtract) {
        return plusSeconds(-(secondsToSubtract % 86_400));
    }

    public LocalTime minusNanos(long nanosToSubtract) {
        return plusNanos(-(nanosToSubtract % NANOS_PER_DAY));
    }

    @Override
    public int compareTo(LocalTime other) {
        int cmp = Integer.compare(hour, other.hour);
        if (cmp == 0) {
            cmp = Integer.compare(minute, other.minute);
        }
        if (cmp == 0) {
            cmp = Integer.compare(second, other.second);
        }
        if (cmp == 0) {
            cmp = Integer.compare(nano, other.nano);
        }
        return cmp;
    }

    public boolean isBefore(LocalTime other) {
        return compareTo(other) < 0;
    }

    public boolean isAfter(LocalTime other) {
        return compareTo(other) > 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof LocalTime) {
            LocalTime that = (LocalTime) obj;
            return hour == that.hour && minute == that.minute && second == that.second && nano == that.nano;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(toNanoOfDay());
    }

    @Override
    public String toString() {
        int hh = hour;
        int mm = minute;
        int ss = second;
        StringBuilder buf = new StringBuilder(16);
        buf.append(hh < 10 ? "0" : "").append(hh)
            .append(mm < 10 ? ":0" : ":").append(mm);
        if (ss > 0 || nano > 0) {
            buf.append(ss < 10 ? ":0" : ":").append(ss);
        }
        if (nano > 0) {
            if (nano % 1_000_000 == 0) {
                buf.append('.').append(Integer.toString(nano / 1_000_000 + 1000).substring(1));
            } else if (nano % 1_000 == 0) {
                buf.append('.').append(Integer.toString(nano / 1_000 + 1000000).substring(1));
            } else {
                buf.append('.').append(Integer.toString(nano + 1000000000).substring(1));
            }
        }
        return buf.toString();
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /** Two ASCII digits at pos, or -1 when either char is missing or not '0'..'9'. No range check. */
    private static int two(CharSequence text, int pos) {
        if (pos + 2 > text.length()) {
            return -1;
        }
        char a = text.charAt(pos);
        char b = text.charAt(pos + 1);
        if (!isDigit(a) || !isDigit(b)) {
            return -1;
        }
        return (a - '0') * 10 + (b - '0');
    }

    private static DateTimeParseException parseError(CharSequence text, int index) {
        return new DateTimeParseException("Text '" + abbreviate(text) + "' could not be parsed at index " + index, text, index);
    }

    private static DateTimeParseException unparsedError(CharSequence text, int index) {
        return new DateTimeParseException("Text '" + abbreviate(text) + "' could not be parsed, unparsed text found at index " + index, text, index);
    }

    private static String abbreviate(CharSequence text) {
        return text.length() > 64 ? text.subSequence(0, 64).toString() + "..." : text.toString();
    }
}
