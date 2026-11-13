# Task queue

`bin/kit next` takes the first `todo` (or `in-progress`) row whose dependencies are all `done`. Statuses:
`todo`, `in-progress`, `done`, `blocked`. Only `bin/kit` edits the status column.

| ID | Task | Depends | Status |
|----|------|---------|--------|
| T01 | StringJoiner | - | done |
| T02 | StringTokenizer | - | done |
| T03 | ArrayList index failure messages | - | done |
| T04 | Queue and Deque interfaces; LinkedList implements Deque | - | done |
| T05 | ArrayDeque | T04 | todo |
| T06 | AbstractQueue and PriorityQueue | T04 | todo |
| T07 | EmptyStackException and Vector | - | todo |
| T08 | Stack | T07 | todo |
| T09 | Collections.synchronized wrappers | - | todo |
| T10 | BitSet: single-bit and search operations | - | todo |
| T11 | BitSet: bulk operations, equality and conversions | T10 | todo |
| T12 | HexFormat | - | todo |
| T13 | MessageDigest with SHA-256 | - | todo |
| T14 | MessageDigest SHA-1, MD5 and SHA-512 | T13 | todo |
| T15 | SecretKeySpec and Mac (HMAC) | T12, T13, T14 | todo |
