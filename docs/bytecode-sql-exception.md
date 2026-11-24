# Bytecode SQLException family

Revision `sql-exception-v320` links `java.sql.SQLException`, `SQLWarning`, `SQLNonTransientException`,
`SQLTransientException` and their nine subclasses in the bundled exception runtime.

Contract:

- Eight constructor shapes on every class: `()`, `(String)`, `(String,String)`, `(String,String,int)`,
  `(Throwable)`, `(String,Throwable)`, `(String,String,Throwable)` and
  `(String,String,int,Throwable)`. `SQLException` stores the `SQLState`/`vendorCode` arguments in fields
  (defaulting to `null`/`0` when not given) and forwards `reason`/`cause` to the matching `Exception`
  constructor, so `(Throwable)` messages are `cause.toString()` and `null` causes give a `null` message.
  `SQLWarning` and the eleven subclasses forward all arguments to their parent.
- `getSQLState()` returns the `SQLState` field, `getErrorCode()` the `vendorCode` field, and
  `getNextException()` the `next` field; `toString()` and `getMessage()` are inherited from `Throwable`
  (`java.sql.SQLSyntaxErrorException: m`).
- `setNextException(ex)` walks the `next` **fields** to the last exception whose `next` is `null` and sets
  it there, so `a.setNextException(b); a.setNextException(c);` yields `a -> b -> c`; it never replaces an
  existing `next`, and `null` is a no-op.
- `SQLException implements Iterable<Throwable>`: the anonymous iterator snapshots `this` plus
  `this.getNextException()` and `this.getCause()` once at creation (virtual calls, so later chain growth or
  a later cause is not seen, but a `next` added to a later element is). `next()` returns `this`, then
  `this`'s cause chain, then each following exception followed by its cause chain; `next()` past the end
  throws `NoSuchElementException` and `remove()` throws `UnsupportedOperationException` (both with `null`
  messages); `hasNext()` is true while any of the three pointers is non-null.
- `SQLWarning.getNextWarning()` casts `getNextException()` to `SQLWarning`, converting a
  `ClassCastException` into `new Error("SQLWarning chain holds value that is not a SQLWarning")` (no
  cause); `setNextWarning(w)` delegates to `setNextException(w)`.
- Hierarchy: `SQLNonTransientException` and `SQLTransientException` extend `SQLException`;
  `SQLDataException`, `SQLFeatureNotSupportedException`, `SQLIntegrityConstraintViolationException`,
  `SQLInvalidAuthorizationSpecException`, `SQLNonTransientConnectionException` and `SQLSyntaxErrorException`
  extend `SQLNonTransientException`; `SQLTimeoutException`, `SQLTransactionRollbackException` and
  `SQLTransientConnectionException` extend `SQLTransientException`.

Known differences: no DriverManager log-writer logging, no serialVersionUID; `BatchUpdateException`,
`SQLClientInfoException` and `SQLRecoverableException` are not linked; the runtime `Throwable` is not
`Serializable`.

Evidence: `BytecodeSqlExceptionTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
two fresh instances). `BatchUpdateException.<init>(String,String,int,int[])`,
`SQLClientInfoException.<init>(String,Map)`, `SQLRecoverableException.<init>(String)` and
`DriverManager.getLogWriter()` are rejected before WAT generation.
