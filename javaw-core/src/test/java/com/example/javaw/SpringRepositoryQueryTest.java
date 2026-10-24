package com.example.javaw;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpringRepositoryQueryTest {
    private static final String IMPORTS = """
        import jakarta.persistence.*;
        import org.springframework.data.repository.CrudRepository;
        import org.springframework.boot.SpringApplication;
        import org.springframework.boot.autoconfigure.SpringBootApplication;
        import org.springframework.dao.IncorrectResultSizeDataAccessException;
        import java.util.Optional;
        import java.math.BigDecimal;
        """;
    private static final String ENTITY = """
        @Entity class Product {
            @Id @GeneratedValue public Integer id;
            public String name;
            public int stock;
            public Integer rating;
            public BigDecimal price;
            Product(String name,int stock,Integer rating,BigDecimal price) {
                this.name=name; this.stock=stock; this.rating=rating; this.price=price;
            }
        }
        """;

    @Test void derivedQueriesExecuteAgainstLiveWasmRows() throws Exception {
        String source = IMPORTS + ENTITY + """
            interface Products extends CrudRepository<Product,Integer> {
                Product[] findByName(String name);
                Product[] findByNameAndStock(String name,int stock);
                Product[] findByNameOrStockAndRating(String name,int stock,Integer rating);
                long countByStock(int stock);
                long countByPrice(BigDecimal price);
                boolean existsByNameEquals(String name);
                boolean existsByRatingIs(Integer rating);
                Optional<Product> findByIdAndName(Integer id,String name);
                Product findByStock(int stock);
                Optional<Product> findByNameEquals(String name);
            }
            @SpringBootApplication class App {
                static Products products;
                App(Products products) { App.products=products; }
                public static int test() {
                    SpringApplication.run(App.class,null);
                    products.save(new Product("Pen",2,500,new BigDecimal("9007199254740993.10")));
                    products.save(new Product("Pen",3,null,new BigDecimal("9007199254740993.100")));
                    products.save(new Product(null,3,500,null));
                    if(products.countByStock(3)!=2L || products.countByStock(99)!=0L) return -1;
                    if(products.countByPrice(new BigDecimal("9007199254740993.1"))!=2L) return -2;
                    if(products.countByPrice(null)!=1L) return -3;
                    if(products.findByName("Pen").length!=2 || products.findByName("Other").length!=0) return -4;
                    Product[] nulls=products.findByName(null);
                    if(nulls.length!=1 || nulls[0].stock!=3) return -5;
                    if(products.findByNameAndStock("Pen",3).length!=1) return -6;
                    if(products.findByNameOrStockAndRating("Pen",3,500).length!=3) return -7;
                    if(!products.existsByNameEquals("Pen") || products.existsByNameEquals("Other")) return -8;
                    if(!products.existsByRatingIs(500) || !products.existsByRatingIs(null)) return -9;
                    Optional<Product> found=products.findByIdAndName(1,"Pen");
                    if(!found.isPresent() || found.get().stock!=2) return -10;
                    if(products.findByIdAndName(99,"Pen").isPresent() || products.findByStock(99)!=null) return -11;
                    if(products.findByStock(2).id!=1) return -12;
                    try { products.findByStock(3); return -13; }
                    catch(IncorrectResultSizeDataAccessException expected) {
                        if(expected.getActualSize()!=2 || expected.getExpectedSize()!=1) return -14;
                    }
                    try { products.findByNameEquals("Pen"); return -18; }
                    catch(IncorrectResultSizeDataAccessException expected) {}
                    Product first=found.get(); first.name="Changed";
                    if(products.findByName("Pen").length!=1 || !products.existsByNameEquals("Changed")) return -15;
                    products.deleteById(1);
                    if(products.existsByNameEquals("Changed") || products.countByPrice(new BigDecimal("9007199254740993.1"))!=1L) return -16;
                    Product[] ordered=products.findByNameOrStockAndRating("Pen",3,500);
                    if(ordered.length!=2 || ordered[0].id!=2 || ordered[1].id!=3) return -17;
                    return 42;
                }
            }
            """;
        OverloadResolutionTest.execute(JavacToWasm.compile("Queries.java",source,false).getWasm(),"assert.equal(e.test(),42);");
    }

    @Test void propertyNamesContainingConnectorsAreNotSplit() throws Exception {
        String source = IMPORTS + """
            @Entity class Device {
                @Id @GeneratedValue int id;
                String brandAndModel;
                int stock;
                Device(String value,int stock) { this.brandAndModel=value; this.stock=stock; }
            }
            interface Entries extends CrudRepository<Device,Integer> {
                Device[] findByBrandAndModelEqualsAndStock(String name,int stock);
            }
            @SpringBootApplication class App {
                static Entries entries;
                App(Entries entries) { App.entries=entries; }
                public static int test() {
                    SpringApplication.run(App.class,null);
                    entries.save(new Device("Model",3));
                    if(entries.findByBrandAndModelEqualsAndStock("Model",3).length!=1) return -1;
                    return 42;
                }
            }
            """;
        OverloadResolutionTest.execute(JavacToWasm.compile("PropertyNames.java",source,false).getWasm(),"assert.equal(e.test(),42);");
    }

    @Test void rejectsUnsupportedQueryFormsBeforeAssembly() {
        for (String method : new String[]{
                "Product[] findByMissing(String name);", "Product[] findByNameContaining(String name);",
                "Product[] findByName();", "Product[] findByStock(String value);", "int countByStock(int value);",
                "boolean existsByName(String... names);", "Product[] findByNameOrderByStock(String name);",
                "@org.springframework.data.jpa.repository.Query(\"select p from Product p\") Product[] findByName(String name);",
                "void deleteByName(String name);", "Product[] findAll(int value);", "Optional<Product> findById(Integer id);"}) {
            var error=assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("InvalidQuery.java",IMPORTS+ENTITY
                    +"interface Products extends CrudRepository<Product,Integer>{"+method+"}"
                    +"@SpringBootApplication class App { public static void start(){SpringApplication.run(App.class,null);} }",false));
            assertTrue(error.getMessage().contains("Repository method Products."),error.getMessage());
            assertTrue(error.getMessage().contains("InvalidQuery.java:"),error.getMessage());
        }
    }

    @Test void unsupportedTransactionOptionsAreExplicitlyRejected() {
        var error=assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("Transaction.java",IMPORTS
                +"@SpringBootApplication @org.springframework.transaction.annotation.Transactional(readOnly=true) class App {}",false));
        assertTrue(error.getMessage().contains("@Transactional options are unsupported"));
    }
}
