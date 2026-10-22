package org.apache.logging.log4j;

/**
 * Standard Log4j2 Level constants and utility methods for WebAssembly runtime.
 */
public class Level {

    public static final Level OFF   = new Level("OFF", 0);
    public static final Level FATAL = new Level("FATAL", 100);
    public static final Level ERROR = new Level("ERROR", 200);
    public static final Level WARN  = new Level("WARN", 300);
    public static final Level INFO  = new Level("INFO", 400);
    public static final Level DEBUG = new Level("DEBUG", 500);
    public static final Level TRACE = new Level("TRACE", 600);
    public static final Level ALL   = new Level("ALL", Integer.MAX_VALUE);

    private final String name;
    private final int intLevel;

    public Level(String name, int intLevel) {
        this.name = name != null ? name : "UNKNOWN";
        this.intLevel = intLevel;
    }

    public String name() {
        return name;
    }

    public int intLevel() {
        return intLevel;
    }

    public boolean isLessSpecificThan(Level level) {
        return this.intLevel >= level.intLevel;
    }

    public boolean isMoreSpecificThan(Level level) {
        return this.intLevel <= level.intLevel;
    }

    public boolean isInRange(Level minLevel, Level maxLevel) {
        return this.intLevel >= minLevel.intLevel && this.intLevel <= maxLevel.intLevel;
    }

    public static Level valueOf(String name) {
        return getLevel(name);
    }

    public static Level getLevel(String name) {
        if (name == null) return INFO;
        String upper = name.toUpperCase();
        if (upper.equals("OFF")) return OFF;
        if (upper.equals("FATAL")) return FATAL;
        if (upper.equals("ERROR")) return ERROR;
        if (upper.equals("WARN")) return WARN;
        if (upper.equals("INFO")) return INFO;
        if (upper.equals("DEBUG")) return DEBUG;
        if (upper.equals("TRACE")) return TRACE;
        if (upper.equals("ALL")) return ALL;
        return INFO;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Level)) return false;
        Level l = (Level) other;
        return this.intLevel == l.intLevel;
    }

    @Override
    public int hashCode() {
        return intLevel;
    }

    @Override
    public String toString() {
        return name;
    }
}
