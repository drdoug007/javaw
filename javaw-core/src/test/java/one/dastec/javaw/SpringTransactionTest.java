package one.dastec.javaw;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpringTransactionTest {
    private static final String IMPORTS = """
        import jakarta.persistence.*;
        import org.springframework.boot.SpringApplication;
        import org.springframework.boot.autoconfigure.SpringBootApplication;
        import org.springframework.stereotype.Service;
        import org.springframework.data.repository.CrudRepository;
        import org.springframework.transaction.annotation.Transactional;
        import org.springframework.transaction.UnexpectedRollbackException;
        import java.math.BigDecimal;
        """;
    private static final String MODEL = """
        @Entity class Item {
            @Id @GeneratedValue public Integer id;
            public String name;
            public int count;
            public BigDecimal price;
            Item(String name,int count) { this.name=name; this.count=count; this.price=new BigDecimal("9007199254740993.10"); }
        }
        interface Items extends CrudRepository<Item,Integer> { long countByName(String name); }
        interface OtherItems extends CrudRepository<Item,Integer> {}
        """;

    @Test void commitsAndRestoresRowsFieldsAliasesAndRepositoryMembership() throws Exception {
        String source = IMPORTS+MODEL+"""
            @Service class Operations {
                final Items items;
                final OtherItems other;
                Item original;
                Operations(Items items,OtherItems other) { this.items=items; this.other=other; }
                public void seed() { original=items.save(new Item("original",2)); other.save(original); }
                @Transactional public Item commit() { original.count=7; return items.save(new Item("committed",4)); }
                @Transactional public void rollback() {
                    original.name="mutated"; original.count=99; original.price=new BigDecimal("0.01");
                    Item replacement=new Item("replacement",3); replacement.id=1; items.save(replacement);
                    items.deleteById(1); other.deleteById(1);
                    items.save(new Item("failed",9)); other.save(new Item("failed",9));
                    throw new IllegalArgumentException("rollback");
                }
                @Transactional public void replace() { items.save(new Item("replacement",3)); throw new AssertionError("error"); }
                public int check() {
                    if(items.countByName("original")!=1L || items.countByName("failed")!=0L) return -1;
                    Item[] all=items.findAll();
                    if(all.length!=2 || all[0]!=original || all[0].count!=7 || !all[1].name.equals("committed")) return -2;
                    if(other.findAll().length!=1 || other.findAll()[0]!=original) return -3;
                    if(!original.price.toString().equals("9007199254740993.10")) return -4;
                    return 42;
                }
            }
            @SpringBootApplication class App {
                static Operations ops;
                App(Operations ops) { App.ops=ops; }
                public static int test() {
                    SpringApplication.run(App.class,null); ops.seed();
                    Item committed=ops.commit(); if(committed.id!=2) return -10;
                    try { ops.rollback(); return -11; } catch(IllegalArgumentException expected) {}
                    if(ops.check()!=42) return ops.check();
                    try { ops.replace(); return -12; } catch(AssertionError expected) {}
                    if(ops.check()!=42) return -13;
                    Item next=ops.commit(); if(next.id!=5) return -14;
                    return 42;
                }
            }
            """;
        OverloadResolutionTest.execute(JavacToWasm.compile("Transactions.java",source,false).getWasm(),"assert.equal(e.test(),42);");
    }

    @Test void joinsNestedTransactionsAndUsesDefaultExceptionRules() throws Exception {
        String source = IMPORTS+MODEL+"""
            class CheckedFailure extends Exception { CheckedFailure() { super(); } }
            @Service @Transactional class Inner {
                final Items items;
                Inner(Items items) { this.items=items; }
                public void fail() { items.save(new Item("inner",1)); throw new IllegalStateException("inner"); }
                public void checked() throws CheckedFailure { items.save(new Item("checked",1)); throw new CheckedFailure(); }
            }
            @Service class Outer {
                final Items items;
                final Inner inner;
                Outer(Items items,Inner inner) { this.items=items; this.inner=inner; }
                @Transactional public void swallowed() {
                    items.save(new Item("outer",1));
                    try { inner.fail(); } catch(IllegalStateException ignored) {}
                }
                @Transactional public int nestedChecked() {
                    try { inner.checked(); } catch(CheckedFailure ignored) {}
                    return 42;
                }
                public int count() { return items.findAll().length; }
            }
            @SpringBootApplication class App {
                static Outer outer;
                App(Outer outer) { App.outer=outer; }
                public static int test() {
                    SpringApplication.run(App.class,null);
                    try { outer.swallowed(); return -1; } catch(UnexpectedRollbackException expected) {}
                    if(outer.count()!=0) return -2;
                    if(outer.nestedChecked()!=42 || outer.count()!=1) return -3;
                    try { outer.inner.checked(); return -4; } catch(CheckedFailure expected) {}
                    if(outer.count()!=2) return -5;
                    return 42;
                }
            }
            """;
        OverloadResolutionTest.execute(JavacToWasm.compile("NestedTransactions.java",source,false).getWasm(),"assert.equal(e.test(),42);");
    }

    @Test void snapshotsAreReleasedAndUnmanagedInstancesCannotJoin() throws Exception {
        String source=IMPORTS+MODEL+"""
            @Service class LedgerOperations {
                final Items items;
                LedgerOperations(Items items) { this.items=items; }
                @Transactional public void fail() {
                    Item original=items.findAll()[0]; original.name="changed";
                    items.save(new Item("temporary",9)); throw new IllegalArgumentException("fail");
                }
            }
            @SpringBootApplication class App {
                static Items items;
                static LedgerOperations writer;
                App(Items items,LedgerOperations writer) { App.items=items; App.writer=writer; }
                public static void init() { SpringApplication.run(App.class,null); items.save(new Item("original",1)); }
                public static int cycle() {
                    try { writer.fail(); } catch(IllegalArgumentException expected) {}
                    if(items.findAll().length!=1 || items.countByName("original")!=1L) return -1;
                    return 42;
                }
                public static int unmanaged() {
                    LedgerOperations raw=new LedgerOperations(items);
                    try { raw.fail(); return -1; } catch(IllegalStateException expected) { return 42; }
                }
            }
            """;
        OverloadResolutionTest.execute(JavacToWasm.compile("TransactionHeap.java",source,false).getWasm(), """
            e.init(); assert.equal(e.unmanaged(),42); e.__gc_collect();
            for(let i=0;i<160;i++) { assert.equal(e.cycle(),42); e.__gc_collect(); }
            const live=e.__gc_live_bytes(), heap=e.__gc_heap_bytes();
            for(let i=0;i<200;i++) { assert.equal(e.cycle(),42); e.__gc_collect(); }
            assert.equal(e.__gc_live_bytes(),live); assert.equal(e.__gc_heap_bytes(),heap);
            """);
    }

    @Test void constructorCallsCannotStartTransactionsBeforeSingletonPublication() throws Exception {
        String source=IMPORTS+MODEL+"""
            @Service class EarlyOperation {
                static int denied;
                final Items items;
                EarlyOperation(Items items) {
                    this.items=items;
                    try { work(); } catch(IllegalStateException expected) { denied++; }
                }
                @Transactional public void work() { items.save(new Item("ready",1)); }
            }
            @SpringBootApplication class App {
                static EarlyOperation operation;
                App(EarlyOperation operation) { App.operation=operation; }
                public static int test() {
                    SpringApplication.run(App.class,null);
                    if(EarlyOperation.denied!=1 || operation.items.findAll().length!=0) return -1;
                    operation.work();
                    if(operation.items.findAll().length!=1) return -2;
                    return 42;
                }
            }
            """;
        OverloadResolutionTest.execute(JavacToWasm.compile("EarlyTransaction.java",source,false).getWasm(),"assert.equal(e.test(),42);");
    }

    @Test void rejectsUnsupportedBoundariesAndOptions() {
        for(String declaration : new String[]{
                "@Service class Bad { @Transactional(readOnly=true) public void work(){} }",
                "@Service class Bad { @Transactional private void work(){} }",
                "@Service class Bad { @Transactional public static void work(){} }",
                "class Bad { @Transactional public void work(){} }",
                "@Transactional interface Bad {}"}) {
            var error=assertThrows(IllegalArgumentException.class,()->JavacToWasm.compile("InvalidTransaction.java",IMPORTS+MODEL+declaration
                    +"@SpringBootApplication class App { public static void start(){SpringApplication.run(App.class,null);} }",false));
            assertTrue(error.getMessage().contains("Unsupported Spring transaction"),error.getMessage());
            assertTrue(error.getMessage().contains("InvalidTransaction.java:"),error.getMessage());
        }
    }
}
