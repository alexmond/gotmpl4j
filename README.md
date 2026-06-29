# gotmpl4j

[![Maven Central](https://img.shields.io/maven-central/v/org.alexmond/gotmpl4j-core.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.alexmond/gotmpl4j-core)
[![Javadoc](https://img.shields.io/badge/Javadoc-API-blue)](https://javadoc.io/doc/org.alexmond/gotmpl4j-core)
[![Build](https://img.shields.io/github/actions/workflow/status/alexmond/gotmpl4j/maven.yml?branch=main)](https://github.com/alexmond/gotmpl4j/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/)
[![coverage](https://unitrack.alexmond.org/badge/37/coverage.svg)](https://unitrack.alexmond.org/projects/37)
[![tests](https://unitrack.alexmond.org/badge/37/pass.svg)](https://unitrack.alexmond.org/projects/37)
[![conformance](https://img.shields.io/badge/conformance-1305%20cases-brightgreen)](https://www.alexmond.org/gotmpl4j/current/conformance.html)

A pure-Java implementation of Go's [`text/template`](https://pkg.go.dev/text/template)
engine, with the [Sprig](https://masterminds.github.io/sprig/) function library and an
optional Spring Boot starter. No Go toolchain, no CGo, no native bindings — just the JVM.

It renders the same templates Helm, Hugo, and countless Go CLIs use, and is validated for
byte-for-byte parity across **1,305 conformance cases** ported from Go's own `text/template` /
`html/template` test suites and Sprig's upstream tests — each rendered through the real Go
engine and Sprig funcmap for ground truth.

📖 **Documentation:** <https://www.alexmond.org/gotmpl4j/current/index.html>

## Modules

| Artifact | Description |
|---|---|
| `org.alexmond:gotmpl4j-core` | The engine: lexer → parser → AST → executor. Go builtins only; functions are pluggable via `ServiceLoader`. |
| `org.alexmond:gotmpl4j-sprig` | The Sprig function library (strings, lists, dicts, math, crypto, date, semver, encoding, …), auto-discovered when on the classpath. |
| `org.alexmond:gotmpl4j-spring-boot-starter` | Spring Boot auto-configuration: a ready-to-inject template engine, configuration properties, a compile cache, function beans, and an optional MVC `ViewResolver`. |

## Quick start

```xml
<dependency>
    <groupId>org.alexmond</groupId>
    <artifactId>gotmpl4j-sprig</artifactId>
    <version>1.1.3</version>
</dependency>
```

```java
import org.alexmond.gotmpl4j.GoTemplate;

var tpl = new GoTemplate();                     // auto-loads Sprig from the classpath
tpl.parse("greet", "Hello {{ .name | upper }}!");

var out = new java.io.StringWriter();
tpl.execute("greet", java.util.Map.of("name", "world"), out);
//  -> "Hello WORLD!"
```

The engine alone (no Sprig) is just `gotmpl4j-core`; add `gotmpl4j-sprig` for the Sprig
functions, or the starter for Spring Boot integration.

## Conformance

Correctness is held to the originals, not to hand-written expectations. **1,305 cases** are
ported from the upstream suites and asserted byte-for-byte:

| Source | Cases |
|---|--:|
| Go `text/template` exec / text / value-reflection tables | 282 |
| Go `text/template/parse` lexer token tables | 40 |
| Go `html/template` escaper primitives / escape-text / errors / CSS | 280 |
| Go `html/template` end-to-end escaping (`TestEscape`) | 131 |
| Go `html/template` typed safe content (`TestTypedContent`) | 207 |
| **Go engine subtotal** | **940** |
| Sprig `runt` (no-data) + `runtv` (with-vars) tables | 365 |
| **Total** | **1,305** |

- **Engine** — ported from Go's `text/template`, `text/template/parse`, and `html/template`
  test files (incl. the end-to-end auto-escaping and typed-content matrices), rendered through
  the real Go engine to capture ground-truth output.
- **Sprig** — ported from Masterminds/sprig's own `runt`/`runtv` test tables, rendered
  through the real Sprig funcmap.

The extractor (`.claude/scripts/conformance/runtv_extract.go`) uses the Go compiler as an
oracle: any case whose data isn't portable to the JVM (Go-typed fixtures, methods, complex
numbers) is dropped automatically, so 967 is an honest **portable** denominator — not a claim
of "100% of Go's tests." Surviving cases become base64 TSV fixtures asserted by the JUnit
conformance suites; `ConformanceCensusTest` re-derives the counts on every build and fails if
coverage drops. Only a few intentional, pinned divergences (Helm `missingkey=zero`, Java regex
vs RE2, float formatting) deviate — see the
[Conformance docs](https://www.alexmond.org/gotmpl4j/current/conformance.html).

## Build

```bash
./mvnw clean install      # build + test + format/PMD/checkstyle gates
./mvnw test               # tests only
```

Requires JDK 17.

## License

Apache License 2.0 — see [LICENSE](LICENSE).
