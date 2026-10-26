package dastec.one.javaw;
import org.junit.jupiter.api.Test;
class TargetTypingTest {
    @Test void executesContextualFactoriesAndWildcardCallbacks() throws Exception {
        byte[] wasm=JavacToWasm.compile("App.java", """
            class App {
                public static Optional<String> absent() { return Optional.empty(); }
                public static int test() {
                    Optional<String> empty=Optional.empty();
                    Optional<String> nullable=Optional.ofNullable(null);
                    empty=Optional.empty();
                    Optional<String> conditional=true ? Optional.empty() : Optional.of("unused");
                    Stream<Integer> stream=Stream.empty();
                    Function<? super Integer,? extends String> text=n->"v"+n;
                    Predicate<? super Integer> positive=n->n>0;
                    Supplier<? extends String> source=()->"hello";
                    if(empty.isPresent() || nullable.isPresent() || absent().isPresent() || conditional.isPresent()) return -1;
                    if(stream.count()!=0 || !text.apply(3).equals("v3") || !positive.test(2) || !source.get().equals("hello")) return -2;
                    return 42;
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }
}
