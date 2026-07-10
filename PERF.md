# Performance & concurrency harness

How to measure a change to the engine — **correctness under concurrency first, then
throughput/allocation, then a before/after verdict** — so a perf ticket lands with evidence
rather than a "should be faster". This is the standing protocol for the epic #60 backlog;
every optimization runs through it.

The engine's thread-safety rests on one invariant: **shared state is immutable-after-parse
(`rootNodes`, `functions`) or a `ConcurrentHashMap` (reflection caches); per-render mutation
lives on a thread-confined `Executor` (a fresh one per `execute()`).** Every fix must keep
that invariant — the harness below is how you prove it did.

## 1. Correctness gate (always, before measuring)

The reusable stress harness renders **one shared, compiled `GoTemplate` from many threads
with distinct per-thread data** and asserts each thread gets its own output — the failure
mode of any shared-state optimization (crossed variables, corrupted scope, unsafely
published cache).

```bash
./mvnw -pl gotmpl4j-core test -Dtest='SharedTemplateConcurrencyTest,ConcurrentHtmlEscapeTest'
```

- Harness: `gotmpl4j-core/src/test/.../concurrency/ConcurrencyStress.java`.
- **Extend it** for the shape your fix touches — add an `IntFunction<Case>` (id → data +
  expected) and one `@Test`:
  ```java
  ConcurrencyStress.on(sharedTemplate, "name")
      .threads(16).iterations(500)
      .run((id) -> new Case(dataFor(id), expectedFor(id)));
  ```
  Give each id **distinct** data whose expected output differs, so a leaked render is a
  detectable mismatch, not a coincidental match.
- These tests **gate `./mvnw install`**. A shared-state fix (#116/#117/#119) must add its
  scenario here first, and — for memory-model (visibility) races a stress test can miss — a
  **jcstress** case for that class (jcstress tier is added per-fix when those land).

## 2. Capture a baseline (before the change)

JMH throughput + allocation, on `main`, kept for the A/B diff. Multithreaded contention is
measured by `ConcurrencyBenchmark` (shared engine, `@Threads` 1/2/4/8); single-engine
feature/render cost by `FeatureBenchmark` / `TableBenchmark` / `ParseBenchmark`.

```bash
./mvnw -q -pl gotmpl4j-benchmarks -am install -DskipTests
JAR=gotmpl4j-benchmarks/target/benchmarks.jar

# scaling + allocation, recording kept for the A/B verdict in step 4
java -jar "$JAR" "ConcurrencyBenchmark" -prof gc \
     -prof "org.alexmond.jvmlens.jmh.JvmlensProfiler:appPackage=org.alexmond.gotmpl4j;keep=/tmp/before.jfr"
```

(If `jvmlens-jmh.jar` isn't on the classpath, drop the last `-prof` and keep `-prof gc`; use
the `jvmlens-perf` skill to wire it.) **Read `ops/us` scaling across `scope1..scope8`** — a
shared-cache fix must keep near-linear scaling, not flatten it.

## 3. Apply the change

Implement the fix. Re-run **step 1** (correctness) — non-negotiable — then measure.

## 4. Measure the diff (after the change)

```bash
java -jar "$JAR" "ConcurrencyBenchmark" -prof gc \
     -prof "org.alexmond.jvmlens.jmh.JvmlensProfiler:appPackage=org.alexmond.gotmpl4j;baseline=/tmp/before.jfr"
```

With `-prof gc` + `baseline=`, JMH prints a **measured A/B verdict** gated on bytes/op with a
significance call (SIGNIFICANT only when the change exceeds the combined error band). Use
`-f 2`+ so cross-fork variance is real. For *where/why* (source-attributed hot paths +
allocation sites), run the `jvmlens-perf` loop:

```bash
java -jar "$JVMLENS" analyze /tmp/after.jfr -b /tmp/before.jfr -a org.alexmond.gotmpl4j
```

## 5. Rules (so numbers are publishable)

- **JMH only** — never hand-timed loops. Always `Blackhole`/return the output.
- **Multiple forks + ~100 iterations** for any number you publish or compare; a single short
  run is directional only.
- **Report throughput *and* allocation** (`-prof gc`, bytes/op) — interpreters bleed in
  allocation, and bytes/op is often more predictive than raw time.
- **Re-baseline on current `main`** before acting on an old profile — the engine moves (e.g.
  the scope-model rewrite invalidated the pre-1.2 numbers).

## Layout

| Piece | Location | Role |
|---|---|---|
| `ConcurrencyStress` | `gotmpl4j-core/src/test/.../concurrency/` | reusable correctness harness (gates build) |
| `SharedTemplateConcurrencyTest` | same | first scenarios: range / include / method-dispatch |
| `ConcurrencyBenchmark` | `gotmpl4j-benchmarks/src/main/.../` | multithreaded throughput + allocation (shared engine) |
| `FeatureBenchmark`, `TableBenchmark`, `ParseBenchmark` | same | single-engine render / parse cost |
| this file | repo root | the before/after protocol |
