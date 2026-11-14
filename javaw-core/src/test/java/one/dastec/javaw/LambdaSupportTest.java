package one.dastec.javaw;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LambdaSupportTest {
    @Test void executesClosuresMethodReferencesAndOptionalCallbacks() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            class Plain { public Plain() {} public int value() { return 6; } }
            class App {
                public int base;
                public static int calls;
                public App(int base) { this.base=base; }
                private int read() { return base; }
                public static App make() { return new App(7); }
                public static String fallback() { calls++; return "empty"; }
                public static Integer twice(Integer x) { return x*2; }
                public Function<Integer,Integer> adder(int amount) { return x -> x + amount + this.base; }
                public static int test() {
                    App app = new App(10);
                    Function<Integer,Integer> f = app.adder(3);
                    if (f.apply(2)!=15 || f.apply(4)!=17) return -1;
                    Function<Integer,Integer> twice = App::twice;
                    if (twice.apply(5)!=10) return -2;
                    Function<String,Integer> length = String::length;
                    if (length.apply("abcd")!=4) return -8;
                    Supplier<Integer> bound = app::read;
                    app.base=11;
                    if (bound.get()!=11) return -9;
                    Supplier<Plain> constructor = Plain::new;
                    if(constructor.get().value()!=6) return -11;
                    Supplier<Integer> expressionBound = new App(12)::read;
                    if(expressionBound.get()!=12) return -12;
                    Supplier<App> factory = App::make;
                    if (factory.get().base!=7) return -10;
                    Supplier<String> fallback = App::fallback;
                    Optional<String> yes = Optional.of("hello");
                    if (yes.orElseGet(fallback).length()!=5 || calls!=0) return -3;
                    Optional<String> no = Optional.<String>empty();
                    if (no.orElseGet(fallback).length()!=5 || calls!=1) return -4;
                    Optional<Integer> mapped = yes.map(s -> s.length());
                    if (mapped.get()!=5) return -5;
                    Predicate<String> keep = s -> s.length()>2;
                    if (!yes.filter(keep).isPresent()) return -6;
                    Supplier<IllegalStateException> error = () -> new IllegalStateException();
                    if (yes.orElseThrow(error).length()!=5) return -7;
                    int n=4;
                    Supplier<Integer> captured = () -> { return n+1; };
                    return captured.get();
                }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),5);");
    }
    @Test void selectsFunctionalOverloadsByCallbackArity() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            interface Action { public int run(); }
            interface Handler { public int handle(int value); }
            class App {
                public int base;
                public App(int base) { this.base=base; }
                public static int on(Action action) { return 1000 + action.run(); }
                public static int on(Handler handler) { return 2000 + handler.handle(5); }
                public static int zero() { return 7; }
                public static int one(int value) { return value * 3; }
                private int bound() { return base; }
                public int viaThis() { return on(this::bound); }
                public static int test() {
                    if (on(() -> 4) != 1004) return -1;
                    if (on(v -> v + 1) != 2006) return -2;
                    if (on(App::zero) != 1007) return -3;
                    if (on(App::one) != 2015) return -4;
                    if (new App(9).viaThis() != 1009) return -5;
                    return 1;
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm, "assert.equal(e.test(), 1);");
    }
    @Test void rejectsMutatedCaptures() {
        var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("Bad.java", "class Bad { public static int test() { int n=1; Supplier<Integer> f=()->n; n++; return f.get(); } }",false));
        assertTrue(error.getMessage().contains("effectively final"),error.getMessage());
    }
}
