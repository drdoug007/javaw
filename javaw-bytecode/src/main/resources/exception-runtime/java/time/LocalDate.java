package java.time;

import java.io.Serializable;
import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Objects;

/** Core java.time.LocalDate: ISO year/month/day with epoch-day conversion, arithmetic and withers. No Temporal, parse, format or clock access. */
public final class LocalDate implements ChronoLocalDate, Serializable {
    private final int year;
    private final short month;
    private final short day;

    public static final int MIN_YEAR = -999_999_999;
    public static final int MAX_YEAR = 999_999_999;

    private static final long DAYS_PER_CYCLE = 146097L;
    private static final long DAYS_0000_TO_1970 = 719528L;

    public static final LocalDate MIN = new LocalDate(MIN_YEAR, 1, 1);
    public static final LocalDate MAX = new LocalDate(MAX_YEAR, 12, 31);
    public static final LocalDate EPOCH = new LocalDate(1970, 1, 1);

    private LocalDate(int year, int month, int day) {
        this.year = year;
        this.month = (short) month;
        this.day = (short) day;
    }

    public static LocalDate of(int year, int month, int dayOfMonth) {
        checkYear(year);
        checkMonth(month);
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new DateTimeException("Invalid value for DayOfMonth (valid values 1 - 28/31): " + dayOfMonth);
        }
        if (dayOfMonth > 28 && dayOfMonth > monthLength(month, isLeap(year))) {
            if (dayOfMonth == 29) {
                throw new DateTimeException("Invalid date 'February 29' as '" + year + "' is not a leap year");
            }
            throw new DateTimeException("Invalid date '" + Month.of(month).name() + " " + dayOfMonth + "'");
        }
        return new LocalDate(year, month, dayOfMonth);
    }

    public static LocalDate of(int year, Month month, int dayOfMonth) {
        Objects.requireNonNull(month, "month");
        return of(year, month.getValue(), dayOfMonth);
    }

    public static LocalDate ofYearDay(int year, int dayOfYear) {
        checkYear(year);
        checkDayOfYear(dayOfYear);
        if (dayOfYear == 366 && !isLeap(year)) {
            throw new DateTimeException("Invalid date 'DayOfYear 366' as '" + year + "' is not a leap year");
        }
        boolean leap = isLeap(year);
        int m = (dayOfYear - 1) / 31 + 1;
        if (dayOfYear > firstDayOfYear(m, leap) + monthLength(m, leap) - 1) {
            m++;
        }
        return new LocalDate(year, m, dayOfYear - firstDayOfYear(m, leap) + 1);
    }

    public static LocalDate ofEpochDay(long epochDay) {
        if (epochDay < MIN.toEpochDay() || epochDay > MAX.toEpochDay()) {
            throw new DateTimeException("Invalid value for EpochDay (valid values -365243219162 - 365241780471): " + epochDay);
        }
        long zeroDay = epochDay + DAYS_0000_TO_1970 - 60;
        long adjust = 0;
        if (zeroDay < 0) {
            long cycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1;
            adjust = cycles * 400;
            zeroDay += -cycles * DAYS_PER_CYCLE;
        }
        long yearEst = (400 * zeroDay + 591) / DAYS_PER_CYCLE;
        long doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        if (doyEst < 0) {
            yearEst--;
            doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        }
        yearEst += adjust;
        int marchDoy0 = (int) doyEst;
        int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
        int month = (marchMonth0 + 2) % 12 + 1;
        int dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
        yearEst += marchMonth0 / 10;
        return new LocalDate((int) yearEst, month, dom);
    }

    public int getYear() {
        return year;
    }

    public int getMonthValue() {
        return month;
    }

    public int getDayOfMonth() {
        return day;
    }

    public Month getMonth() {
        return Month.of(month);
    }

    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.of((int) Math.floorMod(toEpochDay() + 3, 7L) + 1);
    }

    public int getDayOfYear() {
        return firstDayOfYear(month, isLeapYear()) + day - 1;
    }

    public boolean isLeapYear() {
        return isLeap(year);
    }

    public int lengthOfMonth() {
        return monthLength(month, isLeap(year));
    }

    public int lengthOfYear() {
        return isLeap(year) ? 366 : 365;
    }

    public long toEpochDay() {
        long y = year;
        long m = month;
        long total = 365 * y;
        if (y >= 0) {
            total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400;
        } else {
            total -= y / -4 - y / -100 + y / -400;
        }
        total += (367 * m - 362) / 12;
        total += day - 1;
        if (m > 2) {
            total--;
            if (!isLeapYear()) {
                total--;
            }
        }
        return total - DAYS_0000_TO_1970;
    }

    public LocalDate withYear(int year) {
        if (this.year == year) {
            return this;
        }
        int newDay = Math.min(day, monthLength(month, isLeap(year)));
        return new LocalDate(checkYear(year), month, newDay);
    }

    public LocalDate withMonth(int month) {
        if (this.month == month) {
            return this;
        }
        int newDay = Math.min(day, monthLength(month, isLeap(year)));
        return new LocalDate(year, checkMonth(month), newDay);
    }

    public LocalDate withDayOfMonth(int dayOfMonth) {
        if (this.day == dayOfMonth) {
            return this;
        }
        return of(year, month, dayOfMonth);
    }

    public LocalDate withDayOfYear(int dayOfYear) {
        if (getDayOfYear() == dayOfYear) {
            return this;
        }
        return ofYearDay(year, dayOfYear);
    }

    public LocalDate plusYears(long yearsToAdd) {
        if (yearsToAdd == 0) {
            return this;
        }
        int newYear = checkYear(year + yearsToAdd);
        return previousValid(newYear, month, day);
    }

    public LocalDate plusMonths(long monthsToAdd) {
        if (monthsToAdd == 0) {
            return this;
        }
        long calc = year * 12L + (month - 1) + monthsToAdd;
        int newYear = checkYear(Math.floorDiv(calc, 12L));
        int newMonth = (int) Math.floorMod(calc, 12L) + 1;
        return previousValid(newYear, newMonth, day);
    }

    public LocalDate plusWeeks(long weeksToAdd) {
        return plusDays(Math.multiplyExact(weeksToAdd, 7));
    }

    public LocalDate plusDays(long daysToAdd) {
        if (daysToAdd == 0) {
            return this;
        }
        long dom = day + daysToAdd;
        if (dom > 0) {
            if (dom <= 28) {
                return new LocalDate(year, month, (int) dom);
            }
            if (dom <= 59) {
                int len = lengthOfMonth();
                if (dom <= len) {
                    return new LocalDate(year, month, (int) dom);
                }
                if (month < 12) {
                    return new LocalDate(year, month + 1, (int) (dom - len));
                }
                int y = checkYear(year + 1L);
                return new LocalDate(y, 1, (int) (dom - len));
            }
        }
        return ofEpochDay(Math.addExact(toEpochDay(), daysToAdd));
    }

    public LocalDate minusYears(long yearsToSubtract) {
        if (yearsToSubtract == Long.MIN_VALUE) {
            return plusYears(Long.MAX_VALUE).plusYears(1);
        }
        return plusYears(-yearsToSubtract);
    }

    public LocalDate minusMonths(long monthsToSubtract) {
        if (monthsToSubtract == Long.MIN_VALUE) {
            return plusMonths(Long.MAX_VALUE).plusMonths(1);
        }
        return plusMonths(-monthsToSubtract);
    }

    public LocalDate minusWeeks(long weeksToSubtract) {
        if (weeksToSubtract == Long.MIN_VALUE) {
            return plusWeeks(Long.MAX_VALUE).plusWeeks(1);
        }
        return plusWeeks(-weeksToSubtract);
    }

    public LocalDate minusDays(long daysToSubtract) {
        if (daysToSubtract == Long.MIN_VALUE) {
            return plusDays(Long.MAX_VALUE).plusDays(1);
        }
        return plusDays(-daysToSubtract);
    }

    public LocalDate plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            ChronoUnit cu = (ChronoUnit) unit;
            if (cu == ChronoUnit.DAYS) {
                return plusDays(amountToAdd);
            }
            if (cu == ChronoUnit.WEEKS) {
                return plusWeeks(amountToAdd);
            }
            if (cu == ChronoUnit.MONTHS) {
                return plusMonths(amountToAdd);
            }
            if (cu == ChronoUnit.YEARS) {
                return plusYears(amountToAdd);
            }
            if (cu == ChronoUnit.DECADES) {
                return plusYears(Math.multiplyExact(amountToAdd, 10));
            }
            if (cu == ChronoUnit.CENTURIES) {
                return plusYears(Math.multiplyExact(amountToAdd, 100));
            }
            if (cu == ChronoUnit.MILLENNIA) {
                return plusYears(Math.multiplyExact(amountToAdd, 1000));
            }
        }
        throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    public LocalDate minus(long amountToSubtract, TemporalUnit unit) {
        if (amountToSubtract == Long.MIN_VALUE) {
            return plus(Long.MAX_VALUE, unit).plus(1, unit);
        }
        return plus(-amountToSubtract, unit);
    }

    public int compareTo(ChronoLocalDate other) {
        LocalDate o = (LocalDate) other;
        int y = year - o.year;
        if (y != 0) {
            return y;
        }
        int m = month - o.month;
        if (m != 0) {
            return m;
        }
        return day - o.day;
    }

    public boolean isAfter(ChronoLocalDate other) {
        return compareTo(other) > 0;
    }

    public boolean isBefore(ChronoLocalDate other) {
        return compareTo(other) < 0;
    }

    public boolean isEqual(ChronoLocalDate other) {
        return compareTo(other) == 0;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LocalDate)) {
            return false;
        }
        return compareTo((LocalDate) obj) == 0;
    }

    public int hashCode() {
        return (year & 0xFFFFF800) ^ ((year << 11) + (month << 6) + day);
    }

    public String toString() {
        int y = year;
        String yy;
        if (y > 9999) {
            yy = "+" + y;
        } else if (y >= -999 && y <= 999) {
            if (y >= 0) {
                yy = new StringBuilder().append(y + 10000).deleteCharAt(0).toString();
            } else {
                yy = new StringBuilder().append(y - 10000).deleteCharAt(1).toString();
            }
        } else {
            yy = String.valueOf(y);
        }
        return yy + "-" + twoDigits(month) + "-" + twoDigits(day);
    }

    private static LocalDate previousValid(int year, int month, int day) {
        return new LocalDate(year, month, Math.min(day, monthLength(month, isLeap(year))));
    }

    private static int checkYear(long year) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new DateTimeException("Invalid value for Year (valid values -999999999 - 999999999): " + year);
        }
        return (int) year;
    }

    private static int checkMonth(long monthValue) {
        if (monthValue < 1 || monthValue > 12) {
            throw new DateTimeException("Invalid value for MonthOfYear (valid values 1 - 12): " + monthValue);
        }
        return (int) monthValue;
    }

    private static int checkDayOfYear(int dayOfYear) {
        if (dayOfYear < 1 || dayOfYear > 366) {
            throw new DateTimeException("Invalid value for DayOfYear (valid values 1 - 365/366): " + dayOfYear);
        }
        return dayOfYear;
    }

    private static boolean isLeap(long year) {
        return (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
    }

    private static int monthLength(int month, boolean leapYear) {
        if (month == 2) {
            return leapYear ? 29 : 28;
        }
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            return 30;
        }
        return 31;
    }

    private static int firstDayOfYear(int month, boolean leapYear) {
        int leap = leapYear ? 1 : 0;
        if (month == 1) {
            return 1;
        }
        if (month == 2) {
            return 32;
        }
        if (month == 3) {
            return 60 + leap;
        }
        if (month == 4) {
            return 91 + leap;
        }
        if (month == 5) {
            return 121 + leap;
        }
        if (month == 6) {
            return 152 + leap;
        }
        if (month == 7) {
            return 182 + leap;
        }
        if (month == 8) {
            return 213 + leap;
        }
        if (month == 9) {
            return 244 + leap;
        }
        if (month == 10) {
            return 274 + leap;
        }
        if (month == 11) {
            return 305 + leap;
        }
        return 335 + leap;
    }

    private static String twoDigits(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }
}
