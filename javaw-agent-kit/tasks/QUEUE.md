# Task queue

`bin/kit next` takes the first `todo` (or `in-progress`) row whose dependencies are all `done`. Statuses:
`todo`, `in-progress`, `done`, `blocked`. Only `bin/kit` edits the status column.

| ID | Task | Depends | Status |
|----|------|---------|--------|
| T01 | StringJoiner | - | done |
| T02 | StringTokenizer | - | done |
| T03 | ArrayList index failure messages | - | done |
| T04 | Queue and Deque interfaces; LinkedList implements Deque | - | done |
| T05 | ArrayDeque | T04 | done |
| T06 | AbstractQueue and PriorityQueue | T04 | done |
| T07 | EmptyStackException and Vector | - | done |
| T08 | Stack | T07 | done |
| T09 | Collections.synchronized wrappers | - | done |
| T10 | BitSet: single-bit and search operations | - | done |
| T11 | BitSet: bulk operations, equality and conversions | T10 | done |
| T12 | HexFormat | - | done |
| T13 | MessageDigest with SHA-256 | - | done |
| T14 | MessageDigest SHA-1, MD5 and SHA-512 | T13 | done |
| T15 | SecretKeySpec and Mac (HMAC) | T12, T13, T14 | done |
| T16 | Checksum, CRC32 and Adler32 | - | done |
| T17 | Base64 encoders and decoders | - | done |
| T18 | InputStream, ByteArrayInputStream and EOFException | - | done |
| T19 | ByteArrayOutputStream | - | done |
| T20 | Reader and StringReader | - | done |
| T21 | Dictionary and Hashtable | - | done |
| T22 | Properties on top of Hashtable | T21 | done |
| T23 | Properties.load from Reader and InputStream | T22, T20, T18 | done |
| T24 | FilterOutputStream, DataOutput and DataOutputStream | - | done |
| T25 | FilterInputStream, DataInput and DataInputStream | T18, T24 | done |
| T26 | StringWriter and CharArrayWriter | - | done |
| T27 | PrintWriter over a Writer | T26 | done |
| T28 | AtomicInteger and AtomicLong | - | done |
| T29 | AtomicBoolean and AtomicReference | - | done |
| T30 | AtomicReferenceArray update and accumulate retries | - | done |
| T31 | String and StringBuilder range failure messages | - | done |
| T32 | BufferedReader over a Reader | - | done |
| T33 | BufferedInputStream and BufferedOutputStream | - | done |
| T34 | OutputStreamWriter and PrintWriter(OutputStream) | - | done |
| T35 | InputStreamReader with streaming UTF-8, ISO-8859-1 and US-ASCII decoding | - | done |
| T36 | PrintStream over an OutputStream | - | done |
| T37 | EnumSet | - | done |
| T38 | EnumMap | - | done |
| T39 | UncheckedIOException and BufferedReader.lines() | - | done |
| T40 | BufferedWriter over a Writer | - | done |
| T41 | CopyOnWriteArrayList | - | blocked |
| T42 | ConcurrentLinkedQueue (single-threaded) | - | todo |
| T43 | LongAdder and LongAccumulator | - | todo |
| T44 | LineNumberReader | T32 | todo |
| T45 | Seeded java.util.Random | - | todo |
