# TNN Title

```kit
id: TNN
slug: short-kebab-slug
title: link java.util.Example
tests: BytecodeExampleTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/Example.java
allow: docs/bytecode-short-kebab-slug.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-<slug>-vN`). `tests` are classes in
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/`. Each `allow` line is a path (or glob) the agent may
change; registration in `BytecodeExceptionRuntime.java`, the revision files and the agent's report are always
allowed.

## Goal

One paragraph: which JDK class/members, and why the pinned libraries need them (cite the audit rows).

## Members to implement

Exact list with signatures. Everything the test calls is listed here.

## Members to leave out

Members that must stay unlinked (the test checks some with `rejectsUnusedCall`), and why.

## JDK behaviour to match

The details a model gets wrong: formats, edge cases, exception types and messages, null handling.

## Steps

1. Numbered, concrete steps, including the exact registration edit.
2. ...

## Hints

- Example classes to copy from, helpers that exist, and pitfalls found when the task was calibrated.

## Docs

Create `docs/bytecode-<slug>.md` with this skeleton (fill in the brackets):

    # Bytecode <Title>

    Revision `<slug>-vN` links <classes> in the bundled exception runtime.

    Contract:

    - <behaviour>

    Evidence: `<Test>` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
    instances). <Rejected members> are rejected before WAT generation.
