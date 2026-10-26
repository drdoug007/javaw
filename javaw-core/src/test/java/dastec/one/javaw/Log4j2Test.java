package dastec.one.javaw;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Log4j2Test {

    @Test
    public void testLevelHierarchyAndParsing() {
        assertEquals("INFO", Level.INFO.name());
        assertEquals(400, Level.INFO.intLevel());
        assertTrue(Level.INFO.isMoreSpecificThan(Level.DEBUG));
        assertTrue(Level.DEBUG.isLessSpecificThan(Level.INFO));

        assertEquals(Level.FATAL, Level.getLevel("FATAL"));
        assertEquals(Level.ERROR, Level.getLevel("error"));
        assertEquals(Level.WARN, Level.getLevel("WARN"));
        assertEquals(Level.INFO, Level.getLevel("info"));
        assertEquals(Level.DEBUG, Level.getLevel("debug"));
        assertEquals(Level.TRACE, Level.getLevel("trace"));
    }

    @Test
    public void testThreadContextMDC() {
        ThreadContext.clearAll();
        ThreadContext.put("userId", "usr_123");
        ThreadContext.put("reqId", "req_abc");

        assertEquals("usr_123", ThreadContext.get("userId"));
        assertEquals("req_abc", ThreadContext.get("reqId"));
        assertTrue(ThreadContext.containsKey("userId"));

        String mdc = ThreadContext.formatContext();
        assertTrue(mdc.contains("userId=usr_123"));
        assertTrue(mdc.contains("reqId=req_abc"));

        ThreadContext.remove("userId");
        assertNull(ThreadContext.get("userId"));
        assertEquals("req_abc", ThreadContext.get("reqId"));
        ThreadContext.clearAll();
        assertTrue(ThreadContext.formatContext().isEmpty());
    }

    @Test
    public void testLoggerFormattingAndFiltering() {
        SimpleLogger.clearHistory();
        List<String> captured = new ArrayList<>();
        SimpleLogger.addListener((lvl, name, formatted, raw) -> captured.add(formatted));

        Logger logger = LogManager.getLogger("TestService");
        logger.setLevel(Level.INFO);

        logger.debug("This debug message should be filtered out: {}", 42);
        assertEquals(0, captured.size());

        logger.info("Application starting on port {}", 8080);
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).contains("[INFO] TestService"));
        assertTrue(captured.get(0).contains("Application starting on port 8080"));

        logger.warn("Memory high: {} MB used out of {} MB", 750, 1024);
        assertEquals(2, captured.size());
        assertTrue(captured.get(1).contains("[WARN]"));
        assertTrue(captured.get(1).contains("Memory high: 750 MB used out of 1024 MB"));

        logger.error("Connection failed to host {} after {} ms", "db.internal", 5000);
        assertEquals(3, captured.size());
        assertTrue(captured.get(2).contains("[ERROR]"));
        assertTrue(captured.get(2).contains("Connection failed to host db.internal after 5000 ms"));
    }

    @Test
    public void testWasmCompilationOfLog4j2Usage() {
        String source = """
            package dastec.one.test;
            import org.apache.logging.log4j.LogManager;
            import org.apache.logging.log4j.Logger;
            import org.apache.logging.log4j.Level;
            import org.apache.logging.log4j.ThreadContext;
            
            public class Log4jWasmApp {
                private static final Logger logger = LogManager.getLogger(Log4jWasmApp.class);
                
                public static int runLogs() {
                    ThreadContext.put("sessionId", "s_99");
                    logger.info("Service initialized successfully");
                    logger.warn("Cache warning on key {}", "user_data");
                    ThreadContext.clearAll();
                    return 1;
                }
            }
        """;

        JavacToWasm.CompilationResult result = JavacToWasm.compile(source);
        assertNotNull(result);
        String wat = result.getWat();
        assertNotNull(wat);
        assertTrue(wat.contains("(module"));
        assertTrue(wat.contains("$runLogs"));
    }
}
