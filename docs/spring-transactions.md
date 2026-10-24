# In-memory Spring transaction subset

The source compiler supports marker `@Transactional` with default REQUIRED behavior on initialized managed singleton services, components, controllers, and application beans. This is source lowering over the existing Wasm repository, not Spring proxies, a JVM transaction manager, JDBC, SQL, Hibernate, or H2.

```java
@Service
class Inventory {
    private final Products products;
    Inventory(Products products) { this.products = products; }

    @Transactional
    public void rename(int id, String name) {
        Product product = products.findById(id).orElseThrow(
            () -> new IllegalArgumentException("Missing product"));
        product.setName(name);
        products.save(product);
    }
}
```

Use `org.springframework.transaction.annotation.Transactional`. Marker syntax and empty parentheses are accepted. Class-level annotations apply to public concrete instance methods declared on that class, excluding Bean factories. Explicit method annotations require public, non-generic, non-varargs instance methods. Transactional managed classes with a superclass, annotations on interfaces, and custom annotation attributes are diagnosed. Static/private/native/abstract methods and Bean factories cannot define explicit boundaries.

## Boundaries and default rollback rules

Each method delegates its original body through a generated boundary, preserving normal returns and exception identity. Unlike proxy-based Spring, direct/self calls to an annotated source method also enter the boundary. Calls on manually constructed objects are rejected: transactions require the context's singleton instance. Bean-constructor calls are rejected before singleton publication; startup callbacks may invoke transactional services after the graph is initialized. Transactions cannot run after failed bootstrap.

The outermost boundary snapshots all managed repository singletons. Nested annotated calls join the same transaction. On success, the outer boundary releases snapshots. RuntimeException and Error escaping a boundary mark it rollback-only; ordinary checked exceptions commit by default. A checked exception does not clear an existing rollback-only flag. If an inner unchecked failure is caught by the outer method, the outer normal return rolls back and throws UnexpectedRollbackException. An escaping exception is rethrown after commit/rollback, preserving the original failure.

UnexpectedRollbackException is a minimal source-backed RuntimeException with a message constructor; the full Spring TransactionException hierarchy is not supplied. No programmatic transaction-status API or custom rollback rules are added.

## What is restored

Snapshots retain each preexisting row, its next link, its original entity reference, and the entity's direct mutable String/int/Integer/BigDecimal fields. Rollback restores fields on the original objects, then original row links and repository heads. This preserves aliases, row order, replaced row values, and shared entities across repositories. Inserted rows disappear, deleted rows return, and existing entity field changes are undone. Rollback itself does not allocate new rows.

Final/static fields are not copied back. Entities with inherited state or unsupported mutable field types are rejected when transaction support is enabled. Referenced Strings, wrappers, and BigDecimals use their existing immutable value contracts; this is not an arbitrary object-graph snapshot.

Generated-ID counters stay monotonic: failed inserts consume IDs. A newly inserted entity object retained elsewhere can still hold its assigned ID after its row is rolled back. Service fields, local/new unsaved objects, host I/O, DOM actions, files, logging, and other external side effects are not rolled back. Values escaping a failed transaction are not automatically invalidated.

Snapshot pointers are cleared after commit/rollback. The ordinary host-safepoint collector can then reclaim them. Allocation still does not trigger collection during active Wasm calls. Retained repositories/entities remain static roots through the managed context. A stable workload is checked after Integer-cache warmup in the transaction regression.

## Isolation and limits

This contract assumes one synchronous execution in one Wasm instance. Independent instances have independent transactions. Unannotated calls within an active boundary affect the same repository state and are included in rollback; unannotated calls outside it take effect immediately. There are no locks, concurrent workers, async transaction contexts, reader isolation snapshots, savepoints, REQUIRES_NEW, custom propagation/isolation, read-only enforcement, timeouts, rollbackFor/noRollbackFor, transaction events, or distributed transactions. Attributes requesting such features fail compilation instead of being ignored.

A snapshot costs O(total stored rows and supported fields) per outer boundary, even for read-only methods. Manually constructed repositories are outside the context snapshots. Fatal Wasm traps, host exceptions outside the Java Wasm exception tag, process exits, and allocator exhaustion are outside Java exception rollback and can leave the instance unusable; discard it rather than retrying. General transaction policy and full framework compatibility remain separate roadmap work.

## Verification

```sh
mvn -pl javaw-core -Dtest=SpringTransactionTest,SpringRepositoryQueryTest,SpringWebSupportTest,SpringInjectionTest test
```

SpringTransactionTest executes successful returns, inserts, deletes, row replacement, cross-repository aliases, primitive/String/decimal restoration, Error/runtime rollback, checked-exception commit, nested rollback-only behavior, managed-instance/startup guards, monotonic IDs, and 200 allocation/rollback/collection cycles with stable live and high-water heap bytes after warmup. The other suites guard derived queries, controller metadata, injection, and startup behavior.
