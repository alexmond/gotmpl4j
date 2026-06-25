# gotmpl4j-benchmarks

JMH benchmarks comparing **gotmpl4j** against the two most-used Spring Boot view engines —
**Thymeleaf** and **FreeMarker** — plus **Go `text/template`** ("the original") as a
cross-runtime reference. Tracking epic: [#60](https://github.com/alexmond/gotmpl4j/issues/60).

Not published (no Maven Central deploy); excluded from the lint/coverage gates.

## Build & run

```bash
./mvnw -q -pl gotmpl4j-benchmarks -am package   # produces target/benchmarks.jar (shaded)
java -jar gotmpl4j-benchmarks/target/benchmarks.jar            # run everything
java -jar gotmpl4j-benchmarks/target/benchmarks.jar TableBenchmark -prof gc   # one class + alloc profiling
java -jar gotmpl4j-benchmarks/target/benchmarks.jar -l         # list benchmarks
```

Common flags: `-f <forks> -wi <warmup-iters> -i <measure-iters> -p n=100` (table size).

## What's measured

The render benchmarks pre-`parse` the template in `@Setup` (gotmpl4j and FreeMarker via their
parse APIs; Thymeleaf via its parsed-template cache), so they measure the **hot render path**.
`ParseBenchmark` measures compile cost separately for gotmpl4j and FreeMarker — Thymeleaf has
no separable parse API, so it's omitted there.

| Benchmark | Workload | Engines |
|---|---|---|
| `InterpolationBenchmark` | `Hello <name>` render | gotmpl4j, FreeMarker, Thymeleaf |
| `TableBenchmark` | render N stock rows (`n` = 10/100/1000) | gotmpl4j, FreeMarker, Thymeleaf |
| `ParseBenchmark` | compile the table template | gotmpl4j, FreeMarker |

The data model (`Stock`, `Person`) is shared across all JVM engines via bean getters.
gotmpl4j resolves `{{ .Symbol }}` → `getSymbol()` (Go-style property mapping), so
`table.gotmpl` is **byte-identical** to the Go template.

## Go reference

gotmpl4j renders the same syntax as Go, so the Go bench reuses the exact `.gotmpl` files:

```bash
cd benchmarks/go && go test -bench . -benchmem
```

Cross-runtime (JVM vs Go) — read as *indicative* ("within N× of native"), not a direct number.

## Notes

- Numbers are hardware/JVM-specific; always re-run on your own box. Treat single-fork results
  as smoke tests — use `-f 2+` and several iterations for anything you'd quote.
- This module exists to drive the optimizations tracked in #60 (e.g. caching the reflective
  field/method lookups); commit before/after numbers when landing those.
