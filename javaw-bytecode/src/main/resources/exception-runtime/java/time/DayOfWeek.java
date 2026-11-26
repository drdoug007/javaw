package java.time;

public enum DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;

    private static final DayOfWeek[] ENUMS = DayOfWeek.values();

    public static DayOfWeek of(int dayOfWeek) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new DateTimeException("Invalid value for DayOfWeek: " + dayOfWeek);
        }
        return ENUMS[dayOfWeek - 1];
    }

    public int getValue() {
        return ordinal() + 1;
    }

    public DayOfWeek plus(long days) {
        int amount = (int) (days % 7);
        return ENUMS[(ordinal() + (amount + 7)) % 7];
    }

    public DayOfWeek minus(long days) {
        return plus(-(days % 7));
    }
}
