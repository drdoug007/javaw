package java.util.logging;

public class Level implements java.io.Serializable {
    private final String name;
    private final int value;
    private final String resourceBundleName;
    private static final java.util.ArrayList<Level> known = new java.util.ArrayList<>();

    public static final Level OFF = new Level("OFF", Integer.MAX_VALUE, "sun.util.logging.resources.logging");
    public static final Level SEVERE = new Level("SEVERE", 1000, "sun.util.logging.resources.logging");
    public static final Level WARNING = new Level("WARNING", 900, "sun.util.logging.resources.logging");
    public static final Level INFO = new Level("INFO", 800, "sun.util.logging.resources.logging");
    public static final Level CONFIG = new Level("CONFIG", 700, "sun.util.logging.resources.logging");
    public static final Level FINE = new Level("FINE", 500, "sun.util.logging.resources.logging");
    public static final Level FINER = new Level("FINER", 400, "sun.util.logging.resources.logging");
    public static final Level FINEST = new Level("FINEST", 300, "sun.util.logging.resources.logging");
    public static final Level ALL = new Level("ALL", Integer.MIN_VALUE, "sun.util.logging.resources.logging");

    protected Level(String name, int value) {
        this(name, value, null);
    }

    protected Level(String name, int value, String resourceBundleName) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.value = value;
        this.resourceBundleName = resourceBundleName;
        known.add(this);
    }

    public String getName() {
        return name;
    }

    public String getLocalizedName() {
        return name;
    }

    public String getResourceBundleName() {
        return resourceBundleName;
    }

    public final String toString() {
        return name;
    }

    public final int intValue() {
        return value;
    }

    public static Level parse(String name) {
        name.length();
        for (int i = 0; i < known.size(); i++) {
            Level l = known.get(i);
            if (l.name.equals(name)) {
                return l;
            }
        }
        int x;
        try {
            x = Integer.parseInt(name);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad level \"" + name + "\"");
        }
        for (int i = 0; i < known.size(); i++) {
            Level l = known.get(i);
            if (l.value == x) {
                return l;
            }
        }
        return new Level(name, x);
    }

    public boolean equals(Object ox) {
        return ox instanceof Level && ((Level) ox).value == value;
    }

    public int hashCode() {
        return value;
    }
}
