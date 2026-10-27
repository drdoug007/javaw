# Pinned Jackson buffers

Fixtures link unmodified class files from Jackson Core 2.15.4, SHA-256 `8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94`. The JVM oracle loads the original artifact. Javac fixtures target Java 17, and Surefire uses GraalVM CE Java 21.0.2 as the oracle. Each exported case is compared against the JVM in both JAR entry orders and in two fresh dirty-memory instances.

`BufferRecycler` is linked whole. This is possible since the single-threaded `AtomicReferenceArray` (v253); recycling is therefore not thread-safe beyond that contract.

`BytecodeJacksonBufferTest` covers:

- `recyclerAllocation`: buffer-index constants, default byte/char buffer sizes, minimum-size requests, release then re-allocation returning the same array identity, null and undersized releases, buffers from other recyclers, and out-of-range index failures (compared by exception class).
- `recyclerHooks`: a fixture subclass using the protected `(int,int)` constructor and overriding `byteBufferLength`, `charBufferLength`, `balloc` and `calloc`. It checks hook call order, size selection and reuse.

The compiled module declares a 20-page (1.25 MB) memory maximum, and the test host uses a non-freeing bump allocator. Allocation sizes are therefore kept small; sustained allocation and reclamation are not claimed. Thread-local recycler pools (`BufferRecyclers`) and `IOContext` integration remain open.
