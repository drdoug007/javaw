package org.apache.logging.log4j;

import org.apache.logging.log4j.simple.SimpleLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * Standard Log4j2 LogManager entry point for WebAssembly runtime.
 */
public class LogManager {

    private static final Map<String, Logger> loggers = new HashMap<>();
    private static Level rootLevel = Level.INFO;

    public static Logger getLogger() {
        return getLogger("root");
    }

    public static Logger getLogger(String name) {
        if (name == null || name.isEmpty()) {
            name = "root";
        }
        Logger logger = loggers.get(name);
        if (logger == null) {
            logger = new SimpleLogger(name, rootLevel);
            loggers.put(name, logger);
        }
        return logger;
    }

    public static Logger getLogger(Class<?> clazz) {
        if (clazz == null) {
            return getLogger("root");
        }
        return getLogger(clazz.getSimpleName());
    }

    public static Logger getFormatterLogger(String name) {
        return getLogger(name);
    }

    public static Logger getFormatterLogger(Class<?> clazz) {
        return getLogger(clazz);
    }

    public static Logger getRootLogger() {
        return getLogger("root");
    }

    public static Level getRootLevel() {
        return rootLevel;
    }

    public static void setRootLevel(Level level) {
        if (level != null) {
            rootLevel = level;
            for (Logger l : loggers.values()) {
                l.setLevel(level);
            }
        }
    }
}
