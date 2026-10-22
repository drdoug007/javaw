package org.apache.logging.log4j;

/**
 * Standard Log4j2 Logger interface and facade for WebAssembly applications.
 */
public interface Logger {

    String getName();

    Level getLevel();

    void setLevel(Level level);

    boolean isEnabled(Level level);

    boolean isTraceEnabled();

    boolean isDebugEnabled();

    boolean isInfoEnabled();

    boolean isWarnEnabled();

    boolean isErrorEnabled();

    boolean isFatalEnabled();

    void trace(String message);

    void trace(String message, Object p0);

    void trace(String message, Object p0, Object p1);

    void trace(String message, Object p0, Object p1, Object p2);

    void debug(String message);

    void debug(String message, Object p0);

    void debug(String message, Object p0, Object p1);

    void debug(String message, Object p0, Object p1, Object p2);

    void info(String message);

    void info(String message, Object p0);

    void info(String message, Object p0, Object p1);

    void info(String message, Object p0, Object p1, Object p2);

    void warn(String message);

    void warn(String message, Object p0);

    void warn(String message, Object p0, Object p1);

    void warn(String message, Object p0, Object p1, Object p2);

    void error(String message);

    void error(String message, Object p0);

    void error(String message, Object p0, Object p1);

    void error(String message, Object p0, Object p1, Object p2);

    void fatal(String message);

    void fatal(String message, Object p0);

    void fatal(String message, Object p0, Object p1);

    void fatal(String message, Object p0, Object p1, Object p2);

    void log(Level level, String message);

    void log(Level level, String message, Object p0);

    void log(Level level, String message, Object p0, Object p1);

    void log(Level level, String message, Object p0, Object p1, Object p2);
}
