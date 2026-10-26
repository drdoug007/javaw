package dastec.one.javaw;

import org.junit.jupiter.api.Test;

class OptionalSupportTest {
    @Test void executesOptionalObjectsAndLazyCallbacks() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            class Fallback implements Supplier<String> {
                public Fallback() {}
                public String get() { App.calls++; return "fallback"; }
            }
            class Length implements Function<String,Integer> {
                public Length() {}
                public Integer apply(String value) { return value.length(); }
            }
            class Wrap implements Function<String,Optional<String>> {
                public Wrap() {}
                public Optional<String> apply(String value) { return Optional.of(value); }
            }
            class Keep implements Predicate<String> {
                public Keep() {}
                public boolean test(String value) { return value.length() > 2; }
            }
            class App {
                public static int calls;
                public static int test() {
                    Optional<String> yes = Optional.of("hello");
                    Optional<String> no = Optional.<String>empty();
                    if (!yes.isPresent() || !no.isEmpty()) return -1;
                    if (yes.orElseGet(new Fallback()).length() != 5 || calls != 0) return -2;
                    if (no.orElseGet(new Fallback()).length() != 8 || calls != 1) return -3;
                    if (yes.map(new Length()).get() != 5) return -4;
                    if (yes.flatMap(new Wrap()).get().length() != 5) return -5;
                    if (!yes.filter(new Keep()).isPresent() || !no.filter(new Keep()).isEmpty()) return -6;
                    return no.orElse("ok").length() + yes.orElseThrow().length();
                }
                public static int nullOf() { Optional.of(null); return 0; }
                public static int emptyGet() { Optional.empty().get(); return 0; }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm, """
            assert.equal(e.test(), 7);
            assert.throws(() => e.nullOf()); assert.throws(() => e.emptyGet());
            """);
    }
}
