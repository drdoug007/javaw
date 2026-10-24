package com.example.javaw;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpringInjectionTest {
    private static final String IMPORTS = """
        import org.springframework.boot.SpringApplication;
        import org.springframework.boot.autoconfigure.SpringBootApplication;
        import org.springframework.stereotype.*;
        import org.springframework.web.bind.annotation.RestController;
        import org.springframework.context.annotation.Bean;
        import org.springframework.boot.ApplicationRunner;
        """;

    @Test void constructsTransitiveSingletonsAndRunsStartupOnceWithoutRepositories() throws Exception {
        String source = IMPORTS + """
            class Counts { static int stores; static int services; static int controllers; static int apps; static int runs; }
            interface ParentPort { int value(); }
            interface Port extends ParentPort {}
            @Component class Store {
                Store() { Counts.stores++; }
                int value() { return 41; }
            }
            @Service class Catalog implements Port {
                private final Store store;
                @org.springframework.beans.factory.annotation.Autowired
                Catalog(Store store) { this.store=store; Counts.services++; }
                public int value() { return store.value()+1; }
            }
            @RestController class Controller {
                final ParentPort service;
                static Controller current;
                Controller(ParentPort service) { this.service=service; current=this; Counts.controllers++; }
            }
            @SpringBootApplication class App {
                final Port service;
                static App current;
                App(Port service) { this.service=service; current=this; Counts.apps++; }
                @Bean ApplicationRunner initialize() { return args -> { Counts.runs++; }; }
                public static void start() { SpringApplication.run(App.class,null); }
                public static int check() {
                    if(Counts.stores!=1 || Counts.services!=1 || Counts.controllers!=1 || Counts.apps!=1 || Counts.runs!=1) return -1;
                    if(current.service!=Controller.current.service) return -2;
                    return Controller.current.service.value();
                }
            }
            """;
        byte[] wasm = JavacToWasm.compile("Injection.java", source, false).getWasm();
        OverloadResolutionTest.execute(wasm, "e.start(); assert.equal(e.check(),42); e.start(); assert.equal(e.check(),42);");
        // A new Wasm instance must construct its own graph.
        OverloadResolutionTest.execute(wasm, "e.start(); assert.equal(e.check(),42);");
    }

    @Test void rejectsMissingAmbiguousCyclicAndUnsupportedInjection() {
        reject("interface Port {} @Service class Service { Service(Port p){} }", "Missing bean");
        reject("interface Port {} @Service class A implements Port {} @Service class B implements Port {} @Component class C { C(Port p){} }", "Ambiguous bean");
        reject("@Service class A { A(B b){} } @Component class B { B(A a){} }", "A -> B -> A");
        reject("@Component class A { A(){} A(int x){} }", "One constructor");
        reject("@Component abstract class A {}", "concrete non-generic");
        reject("@Component class A { @org.springframework.beans.factory.annotation.Autowired A field; }", "Field/setter @Autowired");
        reject("@Component @org.springframework.context.annotation.Primary class A {}", "@Primary");
        reject("@Component class A { private A(){} }", "Private bean constructor");
        reject("@Component class A { @Bean A factory(){ return new A(); } }", "Factory @Bean");
        reject("@Component class A { @jakarta.annotation.PostConstruct void init(){} }", "@PostConstruct");
    }

    @Test void supportsImplicitConstructorsAndFailsClosedAfterConstructorFailure() throws Exception {
        byte[] wasm = JavacToWasm.compile("Failure.java", IMPORTS + """
            @Component class Plain {}
            @Service class Broken {
                static int attempts;
                Broken(Plain plain) { attempts++; throw new IllegalArgumentException("failed"); }
            }
            @SpringBootApplication class App {
                public static int attempt() {
                    try { SpringApplication.run(App.class,null); return 0; }
                    catch(IllegalArgumentException first) { return 1; }
                    catch(IllegalStateException subsequent) { return 2; }
                }
                public static int count() { return Broken.attempts; }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.attempt(),1); assert.equal(e.attempt(),2); assert.equal(e.count(),1);");
    }

    @Test void reentrantStartupFailsWithoutReconstructingBeans() throws Exception {
        byte[] wasm = JavacToWasm.compile("Recursive.java", IMPORTS + """
            @Component class Recursive {
                static int attempts;
                Recursive() { attempts++; App.start(); }
            }
            @SpringBootApplication class App {
                public static void start() { SpringApplication.run(App.class,null); }
                public static int attempt() {
                    try { start(); return 0; }
                    catch(IllegalStateException expected) { return Recursive.attempts; }
                }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.attempt(),1); assert.equal(e.attempt(),1);");
    }

    private void reject(String beans, String message) {
        var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("InvalidBeans.java", IMPORTS + beans + """
            @SpringBootApplication class App { public static void start() { SpringApplication.run(App.class,null); } }
            """, false));
        assertTrue(error.getMessage().contains(message), error.getMessage());
        assertTrue(error.getMessage().contains("InvalidBeans.java"), error.getMessage());
    }
}
