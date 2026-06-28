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
| `TableBenchmark` | render N stock rows (`n` = 10/100/1000) | gotmpl4j, FreeMarker, Thymeleaf, Mustache, Pebble |
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

Render throughput, **ops/µs (higher is better)**, JMH 2 forks × 5 iterations on a dev
workstation (JDK 17) — re-run on your own hardware before quoting:

| Workload | gotmpl4j | FreeMarker | Thymeleaf | Mustache | Pebble | Go `text/template` (ref) |
|---|---|---|---|---|---|---|
| Interpolation   | 2.83  | 1.61  | 0.17  | **4.22** | 2.26  | 0.97 |
| Table, 10 rows  | 0.074 | 0.080 | 0.021 | **0.098** | 0.091 | 0.028 |
| Table, 100 rows | 0.0080 | 0.0088 | 0.0025 | **0.0106** | 0.0091 | 0.0032 |
| Table, 1000 rows | 0.00080 | 0.00083 | 0.00029 | **0.00109** | 0.00093 | 0.00032 |

gotmpl4j sits squarely in the mainstream pack: faster than FreeMarker and far ahead of
Thymeleaf on interpolation, and within ~10–25 % of the FreeMarker/Pebble cluster on the
loop-heavy table. **Mustache** leads both workloads — it's a logic-less engine with the
thinnest possible render path — and **Pebble** edges gotmpl4j/FreeMarker on the table. The
headline: gotmpl4j is the **only** engine here that speaks Go `text/template` + Sprig, and it
pays no throughput penalty for that — it renders the *identical* template Go does, faster than
native Go on the JVM. **Fairness note:** all five JVM engines materialise the output (a
`String`/`StringWriter`), so those columns are apples-to-apples; the Go bench writes to
`io.Discard` (no output buffer), so Go is a *runtime reference*, not a like-for-like — it does
less work and allocates far less, yet the JVM JIT still edges it on raw throughput here.

For the broader compiled-engine field (Rocker, JTE, JStachio, Velocity, Trimou…), see the
canonical [mbosecke/template-benchmark](https://github.com/mbosecke/template-benchmark) and
[agentgt/template-benchmark](https://github.com/agentgt/template-benchmark) suites. Compiled
template engines (templates → bytecode) sit a tier above all the interpreted engines measured
here, gotmpl4j included; gotmpl4j's niche is Go-template compatibility, not beating a code
generator.

### Optimization history (before → after)

| Change | Workload | Throughput | Allocation (B/op) |
|---|---|---|---|
| Shared reflective accessor cache (#60) | Interpolation | 1.93 → 2.96 ops/µs | 744 → 480 (−35%) |
| | Table 100 | 0.005 → 0.008 ops/µs | 203,513 → 137,753 (−32%) |
| | Table 1000 | — | 2,051,069 → 1,394,849 (−32%) |
| `GoFmt.floatString` without `BigDecimal` (#60) | Table 100 | — | 137,753 → 106,761 (−23%) |
| | Table 1000 | — | 1,394,849 → 1,038,586 (−26%) |

Cumulative on the table render: allocation roughly **halved** vs the original baseline
(n=1000: 2.05 MB → 1.04 MB per render, −49%). Both wins were found with jvmlens (JFR →
LLM-ready hot-path/allocation summary); it now points at `Executor.pushScope` (per-iteration
scope allocation) and the reflective getter `invoke` as the next levers.

Caching the per-class property accessor map (keyed by both the bean and Go-style names) on the
`GoTemplate` removed a linear `PropertyDescriptor` scan + a per-property string allocation on
every field access, and amortised JavaBeans introspection across renders instead of rebuilding
it per execution.

## Notes

- Numbers are hardware/JVM-specific; always re-run on your own box. Treat single-fork results
  as smoke tests — use `-f 2+` and several iterations for anything you'd quote.
- This module exists to drive the optimizations tracked in #60 (e.g. caching the reflective
  field/method lookups); commit before/after numbers when landing those.
