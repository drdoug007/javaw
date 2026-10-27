# Pinned Jackson buffers

Fixtures link unmodified class files from Jackson Core 2.15.4, SHA-256 `8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94`. The JVM oracle loads the original artifact. Javac fixtures target Java 17, and Surefire uses GraalVM CE Java 21.0.2 as the oracle. Each exported case is compared against the JVM in both JAR entry orders and in two fresh dirty-memory instances.

`BufferRecycler` and `ByteArrayBuilder` are linked whole. This is possible since the single-threaded `AtomicReferenceArray` (v253), `LinkedList` (v252) and `OutputStream` (v251) runtime classes landed. Recycling is not thread-safe beyond that contract.

`BytecodeJacksonBufferTest` covers:

- `recyclerAllocation`: buffer-index constants, default byte/char buffer sizes, minimum-size requests, release then re-allocation returning the same array identity, null and undersized releases, buffers from other recyclers, and out-of-range index failures (compared by exception class).
- `recyclerHooks`: a fixture subclass using the protected `(int,int)` constructor and overriding `byteBufferLength`, `charBufferLength`, `balloc` and `calloc`. It checks hook call order, size selection and reuse.
- `builderRecycling`: all constructors and `fromInitial`; recycler-backed first segments returned by `release` and reused by later builders; `resetAndGetFirstSegment`; repeated release; the `NO_BYTES` empty result; the inherited `OutputStream` view; and range, null and negative-size failures.
- `builderSegments`: mixed `append`/`appendTwoBytes`/`appendThreeBytes`/`appendFourBytes` across segment growth, a 60,000-byte bulk write, and manual segment control (`getCurrentSegment`, `setCurrentSegmentLength`, `finishCurrentSegment`, `completeAndCoalesce`). It also covers 12 rounds of geometric block growth, fresh `toByteArray` copies and `reset`.

The compiled module declares a 20-page (1.25 MB) memory maximum, and the test host uses a non-freeing bump allocator. Workloads are therefore sized below it, and the 128 KB maximum block size is not reached. Sustained allocation and reclamation are not claimed. Thread-local recycler pools (`BufferRecyclers`) and `IOContext` integration remain open.
