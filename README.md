# gotmpl4j

[![Maven Central](https://img.shields.io/maven-central/v/org.alexmond/gotmpl4j-core.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.alexmond/gotmpl4j-core)
[![Javadoc](https://javadoc.io/badge2/org.alexmond/gotmpl4j-core/javadoc.svg)](https://javadoc.io/doc/org.alexmond/gotmpl4j-core)
[![Build](https://img.shields.io/github/actions/workflow/status/alexmond/gotmpl4j/maven.yml?branch=main)](https://github.com/alexmond/gotmpl4j/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/)

A pure-Java implementation of Go's [`text/template`](https://pkg.go.dev/text/template)
engine, with the [Sprig](https://masterminds.github.io/sprig/) function library and an
optional Spring Boot starter. No Go toolchain, no CGo, no native bindings — just the JVM.

It renders the same templates Helm, Hugo, and countless Go CLIs use, and is validated for
byte-for-byte parity against Go's own `text/template` test suite and Sprig's upstream tests.

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
    <version>1.0.0</version>
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

Correctness is held to the originals, not to hand-written expectations:

- **Engine** — ported from Go's `text/template` `exec_test.go` tables, rendered through the
  real Go engine to capture ground-truth output (see `.claude/scripts/conformance/`).
- **Sprig** — ported from Masterminds/sprig's own `runt`/`runtv` test tables, rendered
  through the real Sprig funcmap.

The extractor (`.claude/scripts/conformance/runtv_extract.go`) uses the Go compiler as an
oracle: any case whose data isn't portable to the JVM is dropped automatically, and the
surviving cases become base64 TSV fixtures asserted by the JUnit conformance suites.

## Build

```bash
./mvnw clean install      # build + test + format/PMD/checkstyle gates
./mvnw test               # tests only
```

Requires JDK 17.

## License

Apache License 2.0 — see [LICENSE](LICENSE).
