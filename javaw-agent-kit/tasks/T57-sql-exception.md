# T57 java.sql exception family

```kit
id: T57
slug: sql-exception
title: link java.sql.SQLException, SQLWarning and the SQLException subclasses
tests: BytecodeSqlExceptionTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/sql/*.java
allow: docs/bytecode-sql-exception.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-sql-exception-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeSqlExceptionTest.java`.

## Goal

Add `java.sql.SQLException`, `java.sql.SQLWarning` and the eleven `SQLException` subclasses that the pinned libraries
construct to the bundled runtime. `runtime-target/target/audit/member-references.tsv` lists the uses.
`SQLException` construction (opcode 183): `<init>(Ljava/lang/String;)V` 33 sites (Spring
`AbstractDataSource.unwrap`), `<init>()V` 5 (H2 `JdbcPreparedStatement.executeBatch`), `(String,String)` 4 (H2
`FullText.throwException`), `(String,Throwable)` 4 (Hibernate `ClobProxy.resetIfNeeded`), `(Throwable)` 3 (H2
`CreateCluster.performTransfer`), `(String,String,int,Throwable)` 3 (H2 `DbException.<clinit>`), `(String,String,int)`
2 (H2 `JdbcSQLException.<init>`), `(String,String,Throwable)` 1 (H2 `FullTextLucene.convertException`). Calls
(opcode 182): `getMessage()` 64 sites, `getErrorCode()` 37 and `getSQLState()` 32 (Spring
`UncategorizedSQLException.<init>`), `getNextException()` 21 (Spring `SQLErrorCodeSQLExceptionTranslator.doTranslate`),
`toString()` 12 (Hibernate `TypeInfo.extractTypeInfo`), `setNextException` 3 (Spring `JdbcTemplate.handleWarnings`),
`addSuppressed` 2 (Hibernate `DeferredResultSetAccess.executeQuery`). `SQLWarning`: `getNextWarning`, `getSQLState`,
`getErrorCode`, `getMessage` 3 sites each in Spring `JdbcTemplate.handleWarnings`. Subclasses: H2's
`JdbcSQL*Exception` classes call `<init>(String,String,int)` of `SQLDataException`, `SQLFeatureNotSupportedException`,
`SQLIntegrityConstraintViolationException`, `SQLInvalidAuthorizationSpecException`,
`SQLNonTransientConnectionException`, `SQLNonTransientException`, `SQLSyntaxErrorException`, `SQLTimeoutException`,
`SQLTransactionRollbackException` and `SQLTransientException`; `SQLFeatureNotSupportedException.<init>()V` 9 sites
(Hibernate `AbstractHANADialect$MaterializedBlob.position`) and `(String)` 3 (H2
`OsgiDataSourceFactory.rejectPoolingOptions`); HikariCP `PoolBase.newConnection` and `HikariPool.createTimeoutException`
build `SQLTransientConnectionException(String)` / `(String,String,Throwable)` and call `setNextException` on it;
HikariCP `SuspendResumeLock.acquire` builds `SQLTransientException(String)`. The `printStackTrace(PrintStream)` rows
belong to `Throwable` and are not part of this task.

Copy the JDK 21 *observable* behaviour: the eight constructor shapes, the `next` chain appended at its end, the
iterator that walks each exception and then its cause chain, and the `Error` that `getNextWarning` throws when the
chain holds a plain `SQLException`.

## Members to implement

All files go in `javaw-bytecode/src/main/resources/exception-runtime/java/sql/` and start with `package java.sql;`.

1. `SQLException.java`: `public class SQLException extends Exception implements Iterable<Throwable>` with
   - fields `private String SQLState;`, `private int vendorCode;`, `private SQLException next;`
   - the eight constructors, each calling the `Exception` constructor shown and then setting the fields
     (`SQLState` `null` and `vendorCode` `0` when not given):
     - `public SQLException(String reason, String SQLState, int vendorCode)` -> `super(reason)`
     - `public SQLException(String reason, String SQLState)` -> `super(reason)`
     - `public SQLException(String reason)` -> `super(reason)`
     - `public SQLException()` -> `super()`
     - `public SQLException(Throwable cause)` -> `super(cause)` (message is `cause.toString()`, or `null` for a
       `null` cause; the runtime `Throwable` already does this)
     - `public SQLException(String reason, Throwable cause)` -> `super(reason, cause)`
     - `public SQLException(String reason, String sqlState, Throwable cause)` -> `super(reason, cause)`
     - `public SQLException(String reason, String sqlState, int vendorCode, Throwable cause)` -> `super(reason, cause)`
   - `public String getSQLState()`, `public int getErrorCode()`, `public SQLException getNextException()`
   - `public void setNextException(SQLException ex)`
   - `public Iterator<Throwable> iterator()` (an anonymous `Iterator<Throwable>`, see below)
2. `SQLWarning.java`: `public class SQLWarning extends SQLException` with the same eight constructors (each just
   `super(...)` with the same arguments), `public SQLWarning getNextWarning()` and
   `public void setNextWarning(SQLWarning w)`.
3. `SQLNonTransientException.java` and `SQLTransientException.java`, both `extends SQLException`.
4. `extends SQLNonTransientException`: `SQLDataException.java`, `SQLFeatureNotSupportedException.java`,
   `SQLIntegrityConstraintViolationException.java`, `SQLInvalidAuthorizationSpecException.java`,
   `SQLNonTransientConnectionException.java`, `SQLSyntaxErrorException.java`.
5. `extends SQLTransientException`: `SQLTimeoutException.java`, `SQLTransactionRollbackException.java`,
   `SQLTransientConnectionException.java`.

Each class in 3-5 is `public class Name extends Parent` with exactly these eight constructors, each only
`super(...)` with the same arguments:
`()`, `(String reason)`, `(String reason, String SQLState)`, `(String reason, String SQLState, int vendorCode)`,
`(Throwable cause)`, `(String reason, Throwable cause)`, `(String reason, String SQLState, Throwable cause)`,
`(String reason, String SQLState, int vendorCode, Throwable cause)`.

That is 13 new files. Register every one of them.

## Members to leave out

- `BatchUpdateException` (needs `long[]` update counts and serialization helpers) and `SQLClientInfoException`
  (needs `Map` and the `ClientInfoStatus` enum): later tasks. The test checks that their constructors stay unlinked.
- `SQLRecoverableException`: no pinned library references it. The test checks it stays unlinked.
- `DriverManager` and the JDK constructors' `DriverManager.getLogWriter()` logging: there is no log writer here
  (the JDK default is `null`, so nothing is printed). The test checks `DriverManager.getLogWriter` stays unlinked. Do
  not create `DriverManager.java`.
- `serialVersionUID` fields, `AtomicReferenceFieldUpdater`, `volatile`. Do not edit `Throwable.java`,
  `Exception.java` or any other runtime file; do not add `printStackTrace` (the runtime `Throwable` has none yet).

## JDK behaviour to match

- `getSQLState()` returns the `SQLState` field, `getErrorCode()` the `vendorCode` field, `getNextException()` the
  `next` field. `toString()` and `getMessage()` are inherited from `Throwable` (`java.sql.SQLSyntaxErrorException: m`).
- `setNextException(ex)`: walk the **fields** (not `getNextException()`) from `this` to the last exception whose
  `next` is `null` and set its `next = ex`. So `a.setNextException(b); a.setNextException(c);` gives `a -> b -> c`;
  `setNextException(null)` changes nothing; adding to `b` later extends `a`'s chain too. Plain loop: no CAS, no
  locks.
- `iterator()`: the anonymous iterator has fields `SQLException firstException = SQLException.this;`,
  `SQLException nextException = firstException.getNextException();`, `Throwable cause = firstException.getCause();`
  (both **virtual** calls, evaluated once when the iterator is created: a chain or cause added afterwards to `this`
  is not seen; a `next` added to a later element is seen).
  - `hasNext()`: `firstException != null || nextException != null || cause != null`.
  - `next()`: if `firstException != null` return it and set it to `null`; else if `cause != null` return it and set
    `cause = cause.getCause()`; else if `nextException != null` return it, then `cause = nextException.getCause();
    nextException = nextException.getNextException();`; else throw `new NoSuchElementException()` (message `null`).
    So the order is: this, this's causes, next, next's causes, next-next, ...
  - `remove()` throws `new UnsupportedOperationException()` (message `null`).
  - `for (Throwable t : e)` works because the class `implements Iterable<Throwable>` (`forEach` is the inherited
    default).
- `getNextWarning()`: `try { return (SQLWarning) getNextException(); } catch (ClassCastException ex) { throw new
  Error("SQLWarning chain holds value that is not a SQLWarning"); }` (virtual `getNextException`; the `Error` has
  no cause). `setNextWarning(w)`: `setNextException(w)`.
- Catch order: an `SQLSyntaxErrorException` is caught by `catch (SQLNonTransientException e)`, an
  `SQLTimeoutException` by `catch (SQLTransientException e)`; a vendor subclass may override `toString()`.

## Steps

1. Read the test, then `java/lang/Exception.java`, `java/lang/Throwable.java` and, for the iterator style,
   `java/util/NoSuchElementException.java` in `javaw-bytecode/src/main/resources/exception-runtime/`.
2. Create `java/sql/SQLException.java` and `java/sql/SQLWarning.java`, then the eleven subclasses.
3. Register each of the 13 files (do not edit `BytecodeExceptionRuntime.java` by hand), for example
   `javaw-agent-kit/bin/kit register java/sql/SQLException.java`, then the same for `SQLWarning`,
   `SQLNonTransientException`, `SQLTransientException`, `SQLDataException`, `SQLFeatureNotSupportedException`,
   `SQLIntegrityConstraintViolationException`, `SQLInvalidAuthorizationSpecException`,
   `SQLNonTransientConnectionException`, `SQLSyntaxErrorException`, `SQLTimeoutException`,
   `SQLTransactionRollbackException` and `SQLTransientConnectionException`.
4. `javaw-agent-kit/bin/kit check T57` until PASS.
5. Write `docs/bytecode-sql-exception.md` (below), the report, then `javaw-agent-kit/bin/kit finish T57`.

## Hints

- `java.sql` is not in `java.base`, but the runtime build compiles it with `--patch-module java.base=...` without
  complaint (checked when the task was calibrated). Each file needs `package java.sql;`.
- Import `java.util.Iterator` and `java.util.NoSuchElementException` in `SQLException.java`. `Iterable`, `Error`,
  `ClassCastException` and `UnsupportedOperationException` are in `java.lang` (no import).
- The eleven subclasses are identical apart from the name and the parent: write one, copy it.
- The iterator must be an anonymous (or inner) class so `SQLException.this` works; nested classes need no
  registration.
- Do not override `getMessage`, `toString` or `getCause`; the inherited ones are correct.
- Calibration pitfalls: forgetting to register one of the 13 files shows as `link the symbolic class` for that name;
  the runtime `Throwable` does not implement `java.io.Serializable` yet, so the test does not check it (do not add
  it).
- The JVM side needs `KitDifferential` to load fixtures with the platform class loader as parent (the `java.sql`
  module is not visible to the bootstrap loader). If every test fails with
  `NoClassDefFoundError: java/sql/SQLException` before any Wasm runs, that is a TEST PROBLEM: do not edit the kit,
  `kit block`.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
sqlExceptionBasics, chainIteration, warnings, subclasses: if FIX THE RUNTIME says `link the symbolic class/interface java/sql/...` or `link a complete acyclic type hierarchy`, that class is missing or not registered: run `javaw-agent-kit/bin/kit register java/sql/Name.java` for each of the 13 files (never edit BytecodeExceptionRuntime.java by hand) and give it all eight constructors. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
constructors: SQLState is null and vendorCode 0 unless given; the (Throwable) constructor calls super(cause) so the message is cause.toString().
nextChain: setNextException walks the next fields to the end of the chain and appends there; it never replaces an existing next, and null is a no-op.
iteration, iteratorSnapshot: the iterator returns this, then this's cause chain, then each next exception followed by its cause chain; nextException and cause are read once in the iterator's field initializers; next() past the end throws `new NoSuchElementException()` and remove() throws `new UnsupportedOperationException()`.
overridableCalls: the iterator's initializers call getNextException() and getCause() once each (virtual), next() calls getCause()/getNextException() on the chain elements, setNextException uses the fields only, and getNextWarning calls getNextException().
warnings: getNextWarning casts getNextException() to SQLWarning and turns ClassCastException into `new Error("SQLWarning chain holds value that is not a SQLWarning")`; SQLWarning constructors pass all arguments to SQLException.
subclassHierarchy, subclassConstructors, libraryPatterns: check each subclass's parent (six extend SQLNonTransientException, three extend SQLTransientException) and that every one of the eight constructors passes all its arguments to super.
leavesOutBatchClientInfoAndDriverManager: this fails when BatchUpdateException, SQLClientInfoException, SQLRecoverableException or DriverManager became linked. Do not add them.
```

## Docs

Create `docs/bytecode-sql-exception.md` with this skeleton (fill in the brackets):

    # Bytecode SQLException family

    Revision `sql-exception-vN` links `java.sql.SQLException`, `SQLWarning`, `SQLNonTransientException`,
    `SQLTransientException` and their nine subclasses in the bundled exception runtime.

    Contract:

    - <the eight constructor shapes; SQLState/vendorCode accessors; the next chain appended at its end; the
      Iterable<Throwable> iterator over exceptions and causes, snapshotting this's next and cause; getNextWarning's
      Error for a non-warning; the subclass hierarchy>

    Known differences: no DriverManager log-writer logging, no serialVersionUID; `BatchUpdateException`,
    `SQLClientInfoException` and `SQLRecoverableException` are not linked; the runtime `Throwable` is not
    `Serializable`.

    Evidence: `BytecodeSqlExceptionTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
    two fresh instances). `BatchUpdateException.<init>(String,String,int,int[])`,
    `SQLClientInfoException.<init>(String,Map)`, `SQLRecoverableException.<init>(String)` and
    `DriverManager.getLogWriter()` are rejected before WAT generation.
