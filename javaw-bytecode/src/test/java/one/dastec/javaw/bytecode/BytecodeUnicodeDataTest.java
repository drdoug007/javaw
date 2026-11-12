package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Java 17 / Unicode 13 tables and the traversal oracle come from the javaw-unicode-data dependency (its jar, or its
 * target/classes in a reactor build), not from javaw-bytecode itself, and the bundled runtime still compiles them.
 */
class BytecodeUnicodeDataTest {
    private static final List<String> TABLES = List.of("UnicodeCase", "UnicodeProperties", "StringUpperData", "StringLowerData");

    private static void assertFromUnicodeData(String resource) {
        URL url = BytecodeExceptionRuntime.class.getResource(resource);
        assertNotNull(url, resource + " is not on the classpath");
        assertTrue(url.toString().contains("javaw-unicode-data"), resource + " resolved to " + url);
        assertFalse(url.toString().contains("javaw-bytecode"), resource + " resolved to " + url);
    }

    @Test void loadsTheRuntimeTablesAndTheirNoticesFromTheDataModule() {
        for (String table : TABLES) assertFromUnicodeData("/exception-runtime/javaw/runtime/" + table + ".java");
        assertFromUnicodeData("/exception-runtime/legal/word-break/LICENSE");
    }

    @Test void loadsTheTraversalOracleFromTheDataModule() {
        assertFromUnicodeData("/javaw-unicode-data/character-traversal-jdk17.txt");
    }

    @Test void compilesTheTablesIntoTheBundledRuntime() throws Exception {
        var definitions = BytecodeExceptionRuntime.definitions();
        for (String table : TABLES) assertTrue(definitions.containsKey("javaw/runtime/" + table), table);
    }
}
