package dastec.one.javaw;
import org.junit.jupiter.api.Test;
class FunctionalDefaultsTest {
    @Test void executesInheritedSamAndDefaults() throws Exception {
        byte[] wasm=JavacToWasm.compile("App.java", """
            interface Parent<T> { public T apply(T value); public default T twice(T value) { return apply(apply(value)); } }
            interface Child extends Parent<Integer> { public default int marker() { return 7; } }
            interface Base { public default int marker() { return 1; } }
            interface Derived extends Base { public default int marker() { return 2; } }
            class Defaults implements Derived {}
            class App {
                public static int calls;
                public static int test() {
                    Base base=new Defaults(); if(base.marker()!=2) return -10;
                    Child child=x->x+1;
                    if(child.apply(1)!=2 || child.twice(1)!=3 || child.marker()!=7) return -1;
                    Function<Integer,Integer> plus=x->x+1;
                    Function<Integer,Integer> twice=x->x*2;
                    if(plus.andThen(twice).apply(3)!=8 || plus.compose(twice).apply(3)!=7) return -2;
                    Function<String,String> identity=Function.<String>identity();
                    if(identity.apply("ok").length()!=2) return -3;
                    Predicate<Integer> positive=x->x>0;
                    Predicate<Integer> count=x->{ calls++; return true; };
                    if(positive.and(count).test(-1) || calls!=0) return -4;
                    if(!positive.or(count).test(1) || calls!=0) return -5;
                    if(!positive.negate().test(-1)) return -6;
                    Consumer<Integer> first=x->{ calls=calls*10+x; };
                    Consumer<Integer> second=x->{ calls=calls*10+x+1; };
                    first.andThen(second).accept(2); if(calls!=23) return -7;
                    UnaryOperator<Integer> operator=x->x+2; if(operator.apply(1)!=3) return -8;
                    BinaryOperator<Integer> add=(a,b)->a+b; if(add.apply(2,3)!=5) return -9;
                    return 42;
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }
}
