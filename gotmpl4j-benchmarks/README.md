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

## Results (indicative)

Render throughput, **ops/µs (higher is better)**, JMH 2 forks on a dev workstation — re-run on
your own hardware before quoting:

| Workload | gotmpl4j | FreeMarker | Thymeleaf | Go `text/template` (ref) |
|---|---|---|---|---|
| Interpolation | **2.96** | 1.75 | 0.16 | 0.89 |
| Table, 100 rows | 0.008 | **0.009** | 0.003 | 0.0032 |

gotmpl4j leads on interpolation and is on par with compiled FreeMarker on the loop-heavy
table, well ahead of Thymeleaf. **Fairness note:** the three JVM engines all materialise the
output (a `String`/`StringWriter`), so those columns are apples-to-apples; the Go bench writes
to `io.Discard` (no output buffer), so Go is a *runtime reference*, not a like-for-like — it
does less work and allocates far less, yet the JVM JIT still edges it on raw throughput here.

### Optimization history (before → after)

| Change | Workload | Throughput | Allocation (B/op) |
|---|---|---|---|
| Shared reflective accessor cache (#60) | Interpolation | 1.93 → 2.96 ops/µs | 744 → 480 (−35%) |
| | Table 100 | 0.005 → 0.008 ops/µs | 203,513 → 137,753 (−32%) |
| | Table 1000 | — | 2,051,069 → 1,394,849 (−32%) |

Caching the per-class property accessor map (keyed by both the bean and Go-style names) on the
`GoTemplate` removed a linear `PropertyDescriptor` scan + a per-property string allocation on
every field access, and amortised JavaBeans introspection across renders instead of rebuilding
it per execution.

## Notes

- Numbers are hardware/JVM-specific; always re-run on your own box. Treat single-fork results
  as smoke tests — use `-f 2+` and several iterations for anything you'd quote.
- This module exists to drive the optimizations tracked in #60 (e.g. caching the reflective
  field/method lookups); commit before/after numbers when landing those.
