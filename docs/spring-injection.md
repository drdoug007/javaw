# Linked Spring constructor injection

The source compiler resolves a singleton graph ahead of time. This extends the Spring API subset; no Spring container, JVM classpath scanning, or reflection-based bean creation runs in Wasm.

Managed types are the lowered CrudRepository implementations, RestController classes, SpringBootApplication classes, and linked concrete classes annotated Service or Component. All such linked components are eagerly created, including those not referenced by a controller. Annotation names may be imported or qualified. Component-scan package boundaries and custom stereotype meta-annotations are not implemented.

A bean uses its sole constructor; a missing declaration receives Java's implicit no-argument constructor. A marker @Autowired on that constructor is accepted but unnecessary. Parameters resolve to exactly one managed source class by class or transitive interface inheritance. Dependencies are initialized before consumers and shared across controllers, services, application objects, and startup runners. An ordinary unannotated class is not implicitly registered. Generic beans, multiple/private constructors, missing candidates, ambiguity, and dependency cycles receive compile-time diagnostics with source locations and remediation. Parameter names do not break ties.

For example, a controller can depend on a service interface whose implementation depends on a repository:

```java
interface Catalog { Book get(int id); }

@Service
class BookCatalog implements Catalog {
    private final Books books;
    BookCatalog(Books books) { this.books = books; }
    public Book get(int id) {
        return books.findById(id).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Missing book"));
    }
}

@RestController
class BooksController {
    private final Catalog catalog;
    BooksController(Catalog catalog) { this.catalog = catalog; }
    @GetMapping("/first") public Book first() { return catalog.get(1); }
}
```

Imports, the linked Book entity, and Books repository use the existing Spring example contract. Repository rows and decimal values retain their existing Wasm implementations. HTTP routing and return-type restrictions are unchanged.

## Startup and failures

SpringApplication.run lowers to generated bootstrap. It constructs the full graph, then executes supported ApplicationRunner beans. The existing runner contract remains: a no-argument factory on a SpringBootApplication returning a one-argument expression/block lambda. Other factory return types and factory dependencies are not added by this milestone.

Successful repeated starts do nothing. Each fresh Wasm instance owns an independent graph. Bootstrap has initializing, ready, and failed states: recursive starts throw IllegalStateException, and constructor/runner failures propagate and leave the context permanently failed. Subsequent starts throw IllegalStateException rather than recreating partial state or duplicating startup side effects. There is no rollback of constructor side effects, retry/recovery API, or automatic destruction of previously created objects. Dropping the Wasm instance discards its context.

The state guard is for synchronous startup, not concurrent workers. Shared-memory presence does not make the context safe across workers. Normal host garbage collection retains the graph through generated static roots.

## Explicit limits

Field/setter Autowired, Inject/Resource/Value, Qualifier/Primary, Scope/Lazy/DependsOn, ordering annotations, and PostConstruct/PreDestroy callbacks on managed beans are rejected rather than silently interpreted. Constructor Autowired options are also rejected; use one required constructor. General Bean factories, configuration enhancement, proxies, annotation processing, bean names, optional/multiple injection, collection injection, framework lifecycle events, broader query languages, and advanced transaction policies remain pending. The [default REQUIRED transaction subset](spring-transactions.md) operates on initialized managed singletons. A separate [derived equality-query subset](spring-repository-queries.md) is implemented. No additional JMM or Spring/Hibernate/H2 compatibility is implied.

## Evidence

```sh
mvn -pl javaw-core -Dtest=SpringInjectionTest,SpringWebSupportTest test
```

SpringInjectionTest executes transitive interface injection, shared instances, startup exactly once, instance isolation, implicit constructors, and failure/retry guards in Node. It verifies diagnostics for missing/ambiguous/cyclic dependencies, constructor selection limits, and unsupported annotations. SpringWebSupportTest executes a startup repository shared through a service and controller, retains route metadata, and validates existing multi-file compilation. These tests compile source through the standard backend and assemble Wasm; failed assembly/execution fails the test.
