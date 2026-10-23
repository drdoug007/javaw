package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SpringWebSupportTest {
    private static final String MODEL = """
        import jakarta.persistence.Entity;
        import jakarta.persistence.Id;
        import jakarta.persistence.GeneratedValue;
        @Entity class Book {
            @Id @GeneratedValue public Integer key;
            public String title;
            public void setTitle(String title) { this.title = title; }
            public Book(Integer key, String title) { this.key = key; this.title = title; }
        }
        """;
    private static final String REPOSITORY = """
        import org.springframework.data.repository.CrudRepository;
        interface Books extends CrudRepository<Book, Integer> { java.util.List<Book> findAll(); }
        """;
    private static final String APP = """
        import org.springframework.boot.SpringApplication;
        import org.springframework.web.bind.annotation.*;
        import org.springframework.web.server.ResponseStatusException;
        import org.springframework.http.HttpStatus;
        class LibraryApp { public static void main(String[] args) { SpringApplication.run(LibraryApp.class, args); } }
        @RestController @RequestMapping("/books") class LibraryController {
            private final Books books;
            LibraryController(Books books) { this.books = books; }
            @GetMapping public java.util.List<Book> all() { return books.findAll(); }
            @GetMapping("/{key}") public Book get(@PathVariable int key) {
                return books.findById(key).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Missing book " + key));
            }
            @PostMapping public Book create(String title) { var book = new Book(null, title); return books.save(book); }
            @PutMapping("/{key}") public Book update(@PathVariable int key, String title) {
                var book = books.findById(key).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Missing book " + key));
                book.setTitle(title);
                return books.save(book);
            }
            @DeleteMapping("/{key}") public void delete(@PathVariable int key) { books.deleteById(key); }
        }
        """;

    @Test void lowersMultipleFilesWithoutChangingTheInputAst() {
        List<CompilationUnit> units = List.of(StaticJavaParser.parse(MODEL), StaticJavaParser.parse(REPOSITORY), StaticJavaParser.parse(APP));
        List<String> before = units.stream().map(Object::toString).toList();
        String wat = JavacToWasm.generateWat(units);
        assertEquals(before, units.stream().map(Object::toString).toList());
        assertTrue(wat.contains("javaw_spring_web"));
        assertTrue(wat.contains("$__class_LibraryApp"));
        assertTrue(wat.contains("$Books_findById"));
        assertTrue(wat.contains("$Books_save"));
        assertTrue(wat.contains("$LibraryController_get"));
        assertFalse(wat.contains("call $setTitle\n"));
        assertTrue(wat.contains("$Book_setTitle"));
        assertFalse(wat.contains("call $run\n"));
        assertFalse(wat.contains("call $orElseThrow\n"));
        assertNotNull(JavacToWasm.assembleWasm(wat, false), "Lowered module must pass wat2wasm validation");
    }

    @Test void lowersApplicationRunnerAndDecimalConstants() {
        String app = APP.replace("class LibraryApp { public static void main", """
                @org.springframework.boot.autoconfigure.SpringBootApplication
                class LibraryApp {
                    private final Books books;
                    LibraryApp(Books books) { this.books = books; }
                    @org.springframework.context.annotation.Bean
                    ApplicationRunner initialize() {
                        return args -> { var book = new Book(null, "Seed"); books.save(book); };
                    }
                    public static String seedPrice() { return BigDecimal.valueOf(19.95); }
                    public static void main
                """);
        String wat = JavacToWasm.generateWat(List.of(StaticJavaParser.parse(MODEL), StaticJavaParser.parse(REPOSITORY), StaticJavaParser.parse(app)));
        assertTrue(wat.contains("__javaw_runner_LibraryApp_initialize"));
        assertFalse(wat.contains("call $valueOf\n"));
        assertNotNull(JavacToWasm.assembleWasm(wat, false));
    }

    @Test void keepsEveryTypeReachableInBatchCompilation() {
        String wat = JavacToWasm.generateWat(List.of(
                StaticJavaParser.parse("class First { public static int first() { return 1; } }"),
                StaticJavaParser.parse("class Second { public static int second() { return 2; } }")));
        assertTrue(wat.contains("(global $__class_First "));
        assertTrue(wat.contains("(global $__class_Second "));
        assertTrue(wat.contains("(func $first "));
        assertTrue(wat.contains("(func $second "));
        assertNotNull(JavacToWasm.assembleWasm(wat, false));
    }

    @Test void rejectsUnimplementedRepositoryQueries() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.generateWat(List.of(
                StaticJavaParser.parse(MODEL), StaticJavaParser.parse(REPOSITORY.replace("findAll()", "findByTitle(String title)")), StaticJavaParser.parse(APP))));
        assertTrue(error.getMessage().contains("Repository method Books.findByTitle"));
    }
}
