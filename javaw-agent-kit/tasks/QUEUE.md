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
| T18 | InputStream, ByteArrayInputStream and EOFException | - | blocked |
| T19 | ByteArrayOutputStream | - | done |
| T20 | Reader and StringReader | - | done |
| T21 | Dictionary and Hashtable | - | done |
