# Derived queries in the Spring API subset

The compiler generates Wasm source methods for equality queries declared directly on linked `CrudRepository<Entity, Integer>` interfaces. They scan the same rows used by save/findAll/deleteById; Node and JavaScript do not execute predicates or store query results. This is an in-memory API subset, not Spring Data JPA, SQL, Hibernate, or H2 execution.

```java
interface Products extends CrudRepository<Product, Integer> {
    Product[] findByNameAndStock(String name, int stock);
    Optional<Product> findByIdAndName(Integer id, String name);
    long countByPrice(BigDecimal price);
    boolean existsByNameEquals(String name);
}
```

Import `java.util.Optional` and `java.math.BigDecimal`; entity fields use the existing Spring entity subset. Each argument must have exactly the property's declared type (`int` and `Integer` are distinct query signatures).

## Supported grammar

| Query prefix | Result |
| --- | --- |
| findBy | Entity[], List<Entity>, Entity, or Optional<Entity> |
| countBy | long |
| existsBy | boolean |

A predicate consists of direct entity property names, capitalized as in `findByName`. `Is` and `Equals` are optional equality aliases. `And` binds more tightly than `Or`, following Java boolean precedence. The parser prefers longer property names, including names containing And/Or, before treating those words as connectors. There is one argument per property, in textual order. Parameter names do not control binding.

Queries admit String, int, Integer, and BigDecimal properties. Integers compare by numeric value, Strings by content, and BigDecimals using compareTo, so numerically equal values with different scales match without converting through JavaScript or floating point. A null argument matches a null field, including for decimal fields. String equality retains the existing runtime's string semantics.

A multi-result query returns a fresh reference array in repository traversal/insertion order. List<Entity> declarations use the existing Spring entity-list-to-array lowering; they do not acquire general List operations. Changing a result-array slot does not alter repository membership, but the entities themselves are shared live references. Mutating an entity field affects subsequent queries, as does saving or deleting a row. There is no transaction snapshot or concurrent-query guarantee.

A single Entity result is null when absent. Optional results use the reusable source-backed Optional runtime. Both single-result forms reject more than one matching row with IncorrectResultSizeDataAccessException. Its subset implementation extends RuntimeException and exposes getExpectedSize/getActualSize; the full Spring DataAccessException hierarchy, constructors, and cause metadata are not implemented. countBy scans all rows; existsBy returns on the first match.

## Admission and limits

Invalid property names/operators, arity/types, return types, query annotations (including Query), default/static/generic methods, varargs, sorting, paging, projections, nested properties, non-equality operators, and modifying queries are rejected before WAT is returned. Diagnostics include source location, repository/method, and a suggestion to implement unsupported filtering in a source service. Redeclaring built-in findById/save/deleteById is rejected; the established built-in lookup lowering remains unchanged. An unannotated findAll declaration with its existing array/List result is still accepted.

Transaction semantics remain open. Transactional annotations receive an explicit diagnostic: writes and mutations are immediate and there is no rollback, isolation, JDBC, or transaction manager. General query languages and advanced Spring Data return types remain future work.

## Verification

```sh
mvn -pl javaw-core -Dtest=SpringRepositoryQueryTest,SpringWebSupportTest,SpringInjectionTest test
```

The executable query tests cover empty/multiple/single results, null Strings/wrappers/decimals, boxed values beyond wrapper caches, exact large decimal values and scale differences, And/Or precedence, connector-containing property names, reusable Optional results, duplicate errors, result ordering, mutation/deletion visibility, and admission diagnostics. The Spring Web tests validate query-backed List controller metadata and WAT assembly; existing injection/startup tests guard singleton integration.
