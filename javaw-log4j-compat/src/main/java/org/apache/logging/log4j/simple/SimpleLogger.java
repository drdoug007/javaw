package org.apache.logging.log4j.simple;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * High-performance, zero-dependency Log4j2 Logger implementation for WebAssembly.
 */
public class SimpleLogger implements Logger {

    public interface LogListener {
        void onLog(Level level, String loggerName, String formattedMessage, String rawMessage);
    }

    private static final List<LogListener> listeners = new ArrayList<>();
    private static final List<String> logHistory = new ArrayList<>();

    private final String name;
    private Level level;

    public SimpleLogger(String name, Level level) {
        this.name = name != null ? name : "root";
        this.level = level != null ? level : Level.INFO;
    }

    public static void addListener(LogListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public static void removeListener(LogListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public static List<String> getLogHistory() {
        return logHistory;
    }

    public static void clearHistory() {
        logHistory.clear();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public void setLevel(Level level) {
        if (level != null) {
            this.level = level;
        }
    }

    @Override
    public boolean isEnabled(Level targetLevel) {
        if (targetLevel == null || level == Level.OFF) return false;
        return targetLevel.intLevel() <= level.intLevel();
    }

    @Override
    public boolean isTraceEnabled() {
        return isEnabled(Level.TRACE);
    }

    @Override
    public boolean isDebugEnabled() {
        return isEnabled(Level.DEBUG);
    }

    @Override
    public boolean isInfoEnabled() {
        return isEnabled(Level.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return isEnabled(Level.WARN);
    }

    @Override
    public boolean isErrorEnabled() {
        return isEnabled(Level.ERROR);
    }

    @Override
    public boolean isFatalEnabled() {
        return isEnabled(Level.FATAL);
    }

    @Override
    public void trace(String message) {
        log(Level.TRACE, message);
    }

    @Override
    public void trace(String message, Object p0) {
        log(Level.TRACE, message, p0);
    }

    @Override
    public void trace(String message, Object p0, Object p1) {
        log(Level.TRACE, message, p0, p1);
    }

    @Override
    public void trace(String message, Object p0, Object p1, Object p2) {
        log(Level.TRACE, message, p0, p1, p2);
    }

    @Override
    public void debug(String message) {
        log(Level.DEBUG, message);
    }

    @Override
    public void debug(String message, Object p0) {
        log(Level.DEBUG, message, p0);
    }

    @Override
    public void debug(String message, Object p0, Object p1) {
        log(Level.DEBUG, message, p0, p1);
    }

    @Override
    public void debug(String message, Object p0, Object p1, Object p2) {
        log(Level.DEBUG, message, p0, p1, p2);
    }

    @Override
    public void info(String message) {
        log(Level.INFO, message);
    }

    @Override
    public void info(String message, Object p0) {
        log(Level.INFO, message, p0);
    }

    @Override
    public void info(String message, Object p0, Object p1) {
        log(Level.INFO, message, p0, p1);
    }

    @Override
    public void info(String message, Object p0, Object p1, Object p2) {
        log(Level.INFO, message, p0, p1, p2);
    }

    @Override
    public void warn(String message) {
        log(Level.WARN, message);
    }

    @Override
    public void warn(String message, Object p0) {
        log(Level.WARN, message, p0);
    }

    @Override
    public void warn(String message, Object p0, Object p1) {
        log(Level.WARN, message, p0, p1);
    }

    @Override
    public void warn(String message, Object p0, Object p1, Object p2) {
        log(Level.WARN, message, p0, p1, p2);
    }

    @Override
    public void error(String message) {
        log(Level.ERROR, message);
    }

    @Override
    public void error(String message, Object p0) {
        log(Level.ERROR, message, p0);
    }

    @Override
    public void error(String message, Object p0, Object p1) {
        log(Level.ERROR, message, p0, p1);
    }

    @Override
    public void error(String message, Object p0, Object p1, Object p2) {
        log(Level.ERROR, message, p0, p1, p2);
    }

    @Override
    public void fatal(String message) {
        log(Level.FATAL, message);
    }

    @Override
    public void fatal(String message, Object p0) {
        log(Level.FATAL, message, p0);
    }

    @Override
    public void fatal(String message, Object p0, Object p1) {
        log(Level.FATAL, message, p0, p1);
    }

    @Override
    public void fatal(String message, Object p0, Object p1, Object p2) {
        log(Level.FATAL, message, p0, p1, p2);
    }

    @Override
    public void log(Level targetLevel, String message) {
        if (!isEnabled(targetLevel)) return;
        emit(targetLevel, message);
    }

    @Override
    public void log(Level targetLevel, String message, Object p0) {
        if (!isEnabled(targetLevel)) return;
        emit(targetLevel, format(message, p0));
    }

    @Override
    public void log(Level targetLevel, String message, Object p0, Object p1) {
        if (!isEnabled(targetLevel)) return;
        emit(targetLevel, format(message, p0, p1));
    }

    @Override
    public void log(Level targetLevel, String message, Object p0, Object p1, Object p2) {
        if (!isEnabled(targetLevel)) return;
        emit(targetLevel, format(message, p0, p1, p2));
    }

    private void emit(Level targetLevel, String formattedMessage) {
        long time = System.currentTimeMillis();
        String mdc = ThreadContext.formatContext();
        String mdcPart = mdc.isEmpty() ? "" : " " + mdc;
        String line = "[" + targetLevel.name() + "] " + name + mdcPart + " - " + formattedMessage;

        if (logHistory.size() > 500) {
            logHistory.remove(0);
        }
        logHistory.add(line);

        if (targetLevel.intLevel() <= Level.ERROR.intLevel()) {
            System.err.println(line);
        } else {
            System.out.println(line);
        }

        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onLog(targetLevel, name, line, formattedMessage);
        }
    }

    public static String format(String pattern, Object... args) {
        if (pattern == null) return "null";
        if (args == null || args.length == 0) return pattern;

        StringBuilder sb = new StringBuilder();
        int argIdx = 0;
        int len = pattern.length();

        for (int i = 0; i < len; i++) {
            char c = pattern.charAt(i);
            if (c == '{' && i + 1 < len && pattern.charAt(i + 1) == '}') {
                if (argIdx < args.length) {
                    Object arg = args[argIdx++];
                    sb.append(arg != null ? arg.toString() : "null");
                } else {
                    sb.append("{}");
                }
                i++; // skip '}'
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
