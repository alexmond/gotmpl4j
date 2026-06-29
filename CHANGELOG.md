# Changelog

All notable changes to gotmpl4j are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres to
[Semantic Versioning](https://semver.org/) (numeric `MAJOR.MINOR.PATCH`).

## [Unreleased]

### Added
- New module **`gotmpl4j-spring`** — template functions that read the running Spring
  application ([#90]): `msg` (i18n via `MessageSource`), `env` (Spring `Environment`),
  `bean` (`ApplicationContext` lookup, opt-in via `gotmpl4j.spring.expose-beans`), and Spring
  Security helpers (`hasRole`/`hasAnyRole`/`hasAuthority`/`isAuthenticated`/`username`/
  `principal`) when `spring-security-core` is on the classpath, and servlet request functions
  (`param`/`header`/`cookie`/`session`/`requestUri`/`csrf`) in a web application
  (`spring-web` optional). Auto-configured; ships with the Spring Boot starter.
  ([#91], [#92], [#94])

## [1.1.5] — 2026-06-29

### Fixed
- `sprig` `substr`: a negative or out-of-range `end` now slices to the end of the string
  (Masterminds semantics — `substr 1 -1 "pod"` → `"od"`, was `""`); a negative `start` slices
  from the beginning. Indices are clamped defensively. ([#85])

### Changed
- Parent POM `<description>` aligned with the 1,305-case parity positioning (the published
  Maven Central description now carries the number). ([#76])

### Build
- `gotmpl4j-benchmarks` is no longer published to Maven Central — `maven.deploy.skip` is a no-op
  for `central-publishing-maven-plugin`, so the throwaway harness was being bundled on every
  release; fixed with `<skipPublishing>true</skipPublishing>`. ([#82])

### Docs
- All benchmark numbers refreshed from a high-fidelity 100-iteration run (±1–2 % bands).
  gotmpl4j now out-throughputs native Go+Sprig on **all six** feature workloads — `printf`
  flipped from a ~2× loss to a ~1.2× win after the #83 regex pre-compile. Benchmark methodology
  (always multiple forks + ~100 iterations) documented. ([#89])

## [1.1.4] — 2026-06-29

### Performance
- `printf`: pre-compile the verb-rewrite regex `Pattern`s to `static final` (they were
  recompiled on every call). −67 % allocation, +57 % throughput. ([#83])

## [1.1.3] — 2026-06-29

### Performance
- Drop the per-function-call argument `subList` wrapper and back `CommandNode` with `ArrayList`
  (was `LinkedList`): −16 % allocation on Sprig-pipeline-heavy templates. ([#80])

### Docs
- Benchmark the gotmpl4j feature suite against its reference implementation, native Go
  `text/template` + Masterminds/sprig. ([#81])

## [1.1.2] — 2026-06-29

### Performance
- `GoFmt.floatString`: format directly from the `Double.toString` digits with no intermediate
  substrings (−26 % render allocation). ([#77])
- Unsynchronized `StringBuilder` render writer + per-thread float-format scratch reuse
  (−18 % render allocation). ([#78])

### Docs
- Enriched the cross-engine "stocks" table (conditional + computed ratio) and added the
  gotmpl4j-only feature benchmark suite. ([#79])

## [1.1.1] — 2026-06-28

### Added
- Quantified Go/Sprig conformance at **1,305 cases** and surfaced it in the docs/README; ported
  Go `html/template` `TestTypedContent` (typed safe-content matrix). ([#75])
- Expanded the benchmark to five JVM engines (FreeMarker, Thymeleaf, Mustache, Pebble) + a
  Performance docs page. ([#73])

### Performance
- Back `ListNode` with `ArrayList` and index-iterate the render loops (removes a per-render
  iterator-allocation hot spot). ([#67])

### Build
- CI: report tests/coverage + JMH perf to UniTrack; coverage/test badges. ([#70], [#68])

## [1.1.0] — 2026-06-26

### Performance
- Cache per-class property accessors, shared across renders (amortise JavaBeans introspection). ([#62])
- Format doubles without `BigDecimal` in the render hot path. ([#63])
- Make `CharUtils.isAnyOf` allocation-free in the lexer hot loop. ([#64])

### Added
- JMH benchmark module (`gotmpl4j-benchmarks`) + a Go `text/template` reference harness. ([#61])

## [1.0.0] — 2026-06-25

First stable release. The public API is frozen under semantic versioning.

- The full Go `text/template` engine (lexer → parser → AST → executor) + Go's built-in functions.
- The Sprig function library (`gotmpl4j-sprig`, auto-discovered via `ServiceLoader`).
- Contextual `html/template` auto-escaping (opt-in).
- Spring Boot starter: engine bean, configuration properties, compile cache, MVC/WebFlux
  `ViewResolver`.
- Oracle-based conformance against Go's own `text/template`/`html/template` test tables and
  Sprig's `runt`/`runtv` suites.

## 0.1.0 – 0.3.2 (2026-06-23 – 2026-06-24)

Pre-1.0 development line: gotmpl4j extracted from
[jhelm](https://github.com/alexmond/jhelm) into a standalone project, with the engine, Sprig,
the Spring Boot starter, the conformance tooling, and Maven Central publishing established.

[Unreleased]: https://github.com/alexmond/gotmpl4j/compare/1.1.5...HEAD
[1.1.5]: https://github.com/alexmond/gotmpl4j/compare/1.1.4...1.1.5
[1.1.4]: https://github.com/alexmond/gotmpl4j/compare/1.1.3...1.1.4
[1.1.3]: https://github.com/alexmond/gotmpl4j/compare/1.1.2...1.1.3
[1.1.2]: https://github.com/alexmond/gotmpl4j/compare/1.1.1...1.1.2
[1.1.1]: https://github.com/alexmond/gotmpl4j/compare/1.1.0...1.1.1
[1.1.0]: https://github.com/alexmond/gotmpl4j/compare/1.0.0...1.1.0
[1.0.0]: https://github.com/alexmond/gotmpl4j/releases/tag/1.0.0
[#94]: https://github.com/alexmond/gotmpl4j/pull/94
[#92]: https://github.com/alexmond/gotmpl4j/pull/92
[#91]: https://github.com/alexmond/gotmpl4j/pull/91
[#90]: https://github.com/alexmond/gotmpl4j/issues/90
[#89]: https://github.com/alexmond/gotmpl4j/pull/89
[#85]: https://github.com/alexmond/gotmpl4j/issues/85
[#83]: https://github.com/alexmond/gotmpl4j/pull/83
[#82]: https://github.com/alexmond/gotmpl4j/issues/82
[#81]: https://github.com/alexmond/gotmpl4j/pull/81
[#80]: https://github.com/alexmond/gotmpl4j/pull/80
[#79]: https://github.com/alexmond/gotmpl4j/pull/79
[#78]: https://github.com/alexmond/gotmpl4j/pull/78
[#77]: https://github.com/alexmond/gotmpl4j/pull/77
[#76]: https://github.com/alexmond/gotmpl4j/issues/76
[#75]: https://github.com/alexmond/gotmpl4j/pull/75
[#73]: https://github.com/alexmond/gotmpl4j/pull/73
[#70]: https://github.com/alexmond/gotmpl4j/pull/70
[#68]: https://github.com/alexmond/gotmpl4j/pull/68
[#67]: https://github.com/alexmond/gotmpl4j/pull/67
[#64]: https://github.com/alexmond/gotmpl4j/pull/64
[#63]: https://github.com/alexmond/gotmpl4j/pull/63
[#62]: https://github.com/alexmond/gotmpl4j/pull/62
[#61]: https://github.com/alexmond/gotmpl4j/pull/61
