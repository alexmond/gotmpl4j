# Vendored Sprig test sources

These `sprig_*.go` files are vendored verbatim from
[Masterminds/sprig](https://github.com/Masterminds/sprig) (`master`). They are **input** to
`../runtv_extract.go`, which renders Sprig's own `runt`/`runtv` test tables through the real
Sprig funcmap to produce the ground-truth conformance fixtures under each module's
`src/test/resources/conformance/`.

They are not compiled or shipped — they exist only so the conformance fixtures can be
regenerated from a clean checkout without re-fetching upstream, and to pin the exact upstream
revision the committed fixtures were generated from.

- `sprig_<category>_test.go` — Sprig's per-category test files (the `runt`/`runtv` tables).
- `sprig_numeric.go` — Sprig's `numeric.go` implementation, kept as the reference for the
  `seq`/`until`/`untilStep` semantics.

The four categories Sprig has no separate test file for (encoding, paths, regexp,
strings_slice) are intentionally absent.

## Go `text/template` sources

The engine (`-mode gotmpl`) extractor reads Go's own `text/template` test tables
(`exec_test.go`, …) directly from the local Go installation (`$(go env GOROOT)/src/text/template/`).
Running the extractor already requires a Go toolchain, so those sources are always present and
are not vendored here.

## License

Sprig is MIT-licensed; its license is included as [`LICENSE`](LICENSE)
(Copyright © 2013–2020 Masterminds). The vendored files retain that license.
