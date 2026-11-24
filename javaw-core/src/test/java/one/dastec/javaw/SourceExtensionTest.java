package one.dastec.javaw;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Without the javaw-components extension on the classpath, its API is reported instead of miscompiled. */
class SourceExtensionTest {
    @Test
    void componentsApiWithoutTheExtensionIsDiagnosed() {
        var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("App.java", """
            import one.dastec.javaw.components.WebComponent;
            class App { }
            """, false));
        assertTrue(error.getMessage().contains("App.java:1:1"), error.getMessage());
        assertTrue(error.getMessage().contains("Uses one.dastec.javaw.components but the javaw-components compiler extension is not on the compiler classpath"), error.getMessage());
    }

    @Test
    void qualifiedComponentsApiWithoutTheExtensionIsDiagnosed() {
        var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("App.java", """
            class App extends one.dastec.javaw.components.WebComponent { }
            """, false));
        assertTrue(error.getMessage().contains("App.java:1:"), error.getMessage());
        assertTrue(error.getMessage().contains("compiler extension is not on the compiler classpath"), error.getMessage());
    }
}
