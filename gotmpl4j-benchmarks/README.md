# gotmpl4j-benchmarks

JMH benchmarks comparing **gotmpl4j** against four mainstream JVM template engines —
**FreeMarker**, **Thymeleaf**, **Mustache** (mustache.java) and **Pebble** — plus
**Go `text/template`** ("the original") as a cross-runtime reference. The workload mirrors the
canonical [mbosecke/template-benchmark](https://github.com/mbosecke/template-benchmark) "stocks"
table. Tracking epic: [#60](https://github.com/alexmond/gotmpl4j/issues/60).

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
| `InterpolationBenchmark` | `Hello <name>` render | gotmpl4j, FreeMarker, Thymeleaf, Mustache, Pebble |
| `TableBenchmark` | render N stock rows — loop + `if`/`else` + 6 fields + 2 floats (`n` = 10/100/1000) | gotmpl4j, FreeMarker, Thymeleaf, Mustache, Pebble |
| `FeatureBenchmark` | Sprig pipeline, list/dict, control flow, `printf`, composition, large output | gotmpl4j only |
| `ParseBenchmark` | compile the table template | gotmpl4j, FreeMarker |

`InterpolationBenchmark` and `TableBenchmark` are the **cross-engine comparison** (limited to
what every engine can express; the table mirrors the canonical
[mbosecke/template-benchmark](https://github.com/mbosecke/template-benchmark) "stocks" page).
`FeatureBenchmark` has no peer JVM engine (Sprig functions, pipelines, composition), so it's
compared instead against its **reference implementation** — Go `text/template` +
Masterminds/sprig — which the Go bench renders from the same `feature_*.gotmpl` files and data.

The data model (`Stock`, `Person`) is shared across all JVM engines via bean getters.
gotmpl4j resolves `{{ .Symbol }}` → `getSymbol()` (Go-style property mapping), so
`table.gotmpl` is shared with the Go reference.

## Go reference

gotmpl4j renders the same syntax as Go, so the Go bench reuses the exact `.gotmpl` files:

```bash
cd benchmarks/go && go test -bench . -benchmem
```

Cross-runtime (JVM vs Go) — read as *indicative* ("within N× of native"), not a direct number.

## Results (indicative)

Render throughput, **ops/µs (higher is better)**, JMH 2 forks × 5 iterations on a dev
workstation (JDK 17) — re-run on your own hardware before quoting:

| Workload | gotmpl4j | FreeMarker | Thymeleaf | Mustache | Pebble | Go `text/template` (ref) |
|---|---|---|---|---|---|---|
| Interpolation   | 3.16  | 1.78  | 0.17  | **4.18** | 2.46  | 0.94 |
| Table, 10 rows  | **0.058** | 0.051 | 0.010 | **0.058** | 0.054 | 0.020 |
| Table, 100 rows | 0.0059 | 0.0055 | 0.0013 | **0.0060** | 0.0055 | 0.0023 |
| Table, 1000 rows | 0.00060 | 0.00053 | 0.00013 | 0.00060 | 0.00056 | 0.00021 |

**gotmpl4j leads the interpreter pack on the table** — it *ties logic-less Mustache* for the
top spot and edges out Pebble and FreeMarker on the loop-and-conditional stocks workload, a
strong result for a full Go-template + Sprig interpreter (the float-formatting + allocation
optimizations land right on this two-doubles-per-row workload). On interpolation it's second
only to Mustache (thinnest render path), well ahead of Pebble/FreeMarker and far ahead of
Thymeleaf. The headline: gotmpl4j is the **only** engine here that speaks Go `text/template` +
Sprig, and it pays no throughput penalty for that — it renders the *same* template Go does,
faster than native Go on the JVM. **Fairness note:** all five JVM engines materialise the
output (a `String`/`StringWriter`), so those columns are apples-to-apples; the Go bench writes
to `io.Discard` (no output buffer), so Go is a *runtime reference*, not a like-for-like — it does
less work and allocates far less, yet the JVM JIT still edges it on raw throughput here.

For the broader compiled-engine field (Rocker, JTE, JStachio, Velocity, Trimou…), see the
canonical [mbosecke/template-benchmark](https://github.com/mbosecke/template-benchmark) and
[agentgt/template-benchmark](https://github.com/agentgt/template-benchmark) suites. Compiled
template engines (templates → bytecode) sit a tier above all the interpreted engines measured
here, gotmpl4j included; gotmpl4j's niche is Go-template compatibility, not beating a code
generator.

### gotmpl4j feature suite (single-engine)

`FeatureBenchmark` renders gotmpl4j's Go-template + Sprig surface against its reference
implementation, **native Go `text/template` + Masterminds/sprig**, on the same templates/data
(ops/µs, higher is better):

| Workload | gotmpl4j | Go+Sprig (ref) | gotmpl4j is | Exercises |
|---|--:|--:|---|---|
| Sprig pipeline | **0.20** | 0.016 | **12× faster** | `upper \| trunc \| trimSuffix \| repeat \| quote` |
| Control flow | **0.14** | 0.015 | **9× faster** | nested `if`/`else if`/`else`, `with`, `range … else` |
| List / dict | **0.16** | 0.027 | **6× faster** | `dict`, `keys`, `sortAlpha`, `index`, `join`, `len` |
| Composition | **0.12** | 0.022 | **5× faster** | `define` + `template` per row |
| Large output | **0.025** | 0.013 | **2× faster** | 200-element loop (writer-stress) |
| `printf` | 0.011 | **0.0135** | 0.8× (Go ~1.2×) | `printf "%s=%d (%.2f%%)"` — verb-parse dominates |

gotmpl4j beats native Go+Sprig 2–12× across the Sprig/control-flow surface (JVM JIT vs the Go
interpreter). `printf` is the lone hold-out — Go ~1.2× faster, narrowed from ~2× by
pre-compiling printf's rewrite patterns (#83: −67% alloc, +57% throughput).

### Optimization history (before → after)

| Change | Workload | Throughput | Allocation (B/op) |
|---|---|---|---|
| Shared reflective accessor cache (#60) | Interpolation | 1.93 → 2.96 ops/µs | 744 → 480 (−35%) |
| | Table 1000 | — | 2,051,069 → 1,394,849 (−32%) |
| `GoFmt.floatString` without `BigDecimal` (#60) | Table 1000 | — | 1,394,849 → 1,038,586 (−26%) |
| `floatString` without intermediate substrings (#77) | Table 1000 | — | 1,038,586 → 771,716 (−26%) |
| Unsync writer + per-thread float scratch (#78) | Table 1000 | — | 771,716 → 644,082 (−17%) |
| Drop per-call arg `subList` wrapper, ArrayList `CommandNode` (#80) | Sprig pipeline | — | 3,488 → 2,912 (−16%) |
| Pre-compile `printf` regex patterns (#83) | printf | 0.007 → 0.011 ops/µs | 123,136 → 40,288 (−67%) |

Cumulative on the table render: per-render allocation dropped **~two-thirds** vs the original
baseline (n=1000: 2.05 MB → 0.64 MB, −69%). Every win was found with jvmlens (JFR → LLM-ready
hot-path/allocation summary) and verified with a multi-fork JMH `gc.alloc.rate.norm` A/B — the
exact measure matters, since sampled allocation can flag JIT-eliminated (escape-analysed)
sites that don't move real bytes/op.

## Notes

- Numbers are hardware/JVM-specific; always re-run on your own box. Treat single-fork results
  as smoke tests — use `-f 2+` and several iterations for anything you'd quote.
- This module exists to drive the optimizations tracked in #60 (e.g. caching the reflective
  field/method lookups); commit before/after numbers when landing those.
