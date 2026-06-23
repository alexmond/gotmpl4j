# CLAUDE.md — gotmpl4j

## What this is

A pure-Java implementation of Go's `text/template` engine, the Sprig function library, and a
Spring Boot starter. Extracted from [jhelm](https://github.com/alexmond/jhelm) (which keeps
the Helm-specific function extension and consumes these as published artifacts).

**Tech stack:** Java 17, Spring Boot (starter only), Lombok, Maven. Published to Maven Central.

## Modules

```
gotmpl4j-parent (pom)
├── gotmpl4j-core                  — lexer → parser → AST → executor; Go builtins only
├── gotmpl4j-sprig                 — Sprig functions (ServiceLoader, priority 100)
└── gotmpl4j-spring-boot-starter   — Spring Boot auto-config, ViewResolver, compile cache
```

`gotmpl4j-core` must NOT depend on Sprig or any host application. Functions are pluggable via
`ServiceLoader` (see `.claude/rules/gotemplate-module.md`).

## Build & test

```bash
./mvnw clean install     # build + test + spring-javaformat + PMD + checkstyle (all gate the build)
./mvnw test              # tests only
./mvnw spring-javaformat:apply   # auto-format (tabs)
```

JDK 17. Build/format/lint gates are identical to the sibling alexmond libraries.

## Coding standards

- Tabs (enforced by spring-javaformat). Run `spring-javaformat:apply` before committing.
- Java 17 only — **no Java 21 features** (no pattern-matching `switch`, no `SequencedCollection`).
  Use `if/else instanceof` patterns instead.
- Imports, never inline FQNs (PMD `UnnecessaryFullyQualifiedName`).
- `@code true`/`@code false` in Javadoc; `Locale.ROOT` on case conversions; `.append('c')` for
  single chars. File ≤800 lines, method ≤80 (checkstyle).

## Conformance tooling

Correctness is held to the originals via `.claude/scripts/conformance/`:

- `runtv_extract.go` — extracts Go's `text/template` `execTest` tables (`-mode gotmpl`) and
  Sprig's `runt`/`runtv` tables (default mode), rendering each through the real Go engine /
  Sprig funcmap to capture ground-truth output. The Go compiler is the portability oracle:
  non-portable cases (Go-typed fixtures, methods) drop automatically.
- Fixtures land as base64 TSV under each module's `src/test/resources/conformance/`, asserted
  by `GoTemplateConformanceTest` (core) and `SprigConformanceTest` (sprig). Known, intended
  divergences are pinned in `*_known_divergences.txt`.
- Requires a Go toolchain to regenerate fixtures; the JUnit suites need only the committed TSVs.

## Releasing

`.github/workflows/maven_release.yml` (manual dispatch) sets the version, builds, deploys to
Maven Central via the `release` profile (GPG sign + central-publishing-plugin), tags, and opens
a GitHub release. Secrets (`OSSRH_*`, `GPG_*`, `CODECOV_TOKEN`) are provisioned from infra via
`infra/scripts/gh-release-secrets.sh apply alexmond/gotmpl4j`.
