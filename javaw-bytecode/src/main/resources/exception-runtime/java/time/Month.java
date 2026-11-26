package java.time;

public enum Month {
    JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER;

    private static final Month[] ENUMS = Month.values();

    public static Month of(int month) {
        if (month < 1 || month > 12) {
            throw new DateTimeException("Invalid value for MonthOfYear: " + month);
        }
        return ENUMS[month - 1];
    }

    public int getValue() {
        return ordinal() + 1;
    }

    public Month plus(long months) {
        int amount = (int) (months % 12);
        return ENUMS[(ordinal() + (amount + 12)) % 12];
    }

    public Month minus(long months) {
        return plus(-(months % 12));
    }

    public int length(boolean leapYear) {
        if (this == FEBRUARY) {
            return leapYear ? 29 : 28;
        }
        if (this == APRIL || this == JUNE || this == SEPTEMBER || this == NOVEMBER) {
            return 30;
        }
        return 31;
    }

    public int minLength() {
        if (this == FEBRUARY) {
            return 28;
        }
        if (this == APRIL || this == JUNE || this == SEPTEMBER || this == NOVEMBER) {
            return 30;
        }
        return 31;
    }

    public int maxLength() {
        if (this == FEBRUARY) {
            return 29;
        }
        if (this == APRIL || this == JUNE || this == SEPTEMBER || this == NOVEMBER) {
            return 30;
        }
        return 31;
    }

    public int firstDayOfYear(boolean leapYear) {
        int l = leapYear ? 1 : 0;
        if (this == JANUARY) {
            return 1;
        }
        if (this == FEBRUARY) {
            return 32;
        }
        if (this == MARCH) {
            return 60 + l;
        }
        if (this == APRIL) {
            return 91 + l;
        }
        if (this == MAY) {
            return 121 + l;
        }
        if (this == JUNE) {
            return 152 + l;
        }
        if (this == JULY) {
            return 182 + l;
        }
        if (this == AUGUST) {
            return 213 + l;
        }
        if (this == SEPTEMBER) {
            return 244 + l;
        }
        if (this == OCTOBER) {
            return 274 + l;
        }
        if (this == NOVEMBER) {
            return 305 + l;
        }
        return 335 + l;
    }

    public Month firstMonthOfQuarter() {
        return ENUMS[(ordinal() / 3) * 3];
    }
}
