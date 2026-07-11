//go:build ignore

// parity_extract.go — enumerate Go text/template's *non-render* parity surfaces
// straight from the stdlib source of the running toolchain, so gaps in the
// engine API / grammar (not just functions) can be diffed against gotmpl4j.
//
// The table-driven conformance oracle (runtv_extract.go) only covers render
// cases; it is structurally blind to (a) the public Template API — e.g. Delims,
// which shipped late — and (b) parser grammar. This tool surfaces both, plus an
// inventory of Go's own test functions as a coverage ledger.
//
// Modes:
//
//	-mode api     exported *Template methods + package-level funcs (text/template)
//	-mode nodes   exported parse.Node struct types (the grammar's AST surface)
//	-mode tests   every `func Test*` across text/template, .../parse, html/template
//
// Usage:
//
//	go run parity_extract.go -mode api
//	go run parity_extract.go -mode nodes
//	go run parity_extract.go -mode tests
//
// Output is one name per line (sorted), suitable for `comm`/diff against the
// gotmpl4j side (see parity_audit.sh).
package main

import (
	"flag"
	"fmt"
	"go/ast"
	"go/parser"
	"go/token"
	"os"
	"path/filepath"
	"runtime"
	"sort"
	"strings"
)

func goroot() string { return runtime.GOROOT() }

// parsePkg parses every non-test .go file in a stdlib package dir (or, if
// testFiles, only the _test.go files) and calls visit on each file's AST.
func parsePkg(rel string, testFiles bool, visit func(*ast.File)) error {
	dir := filepath.Join(goroot(), "src", rel)
	entries, err := os.ReadDir(dir)
	if err != nil {
		return err
	}
	fset := token.NewFileSet()
	for _, e := range entries {
		name := e.Name()
		if !strings.HasSuffix(name, ".go") {
			continue
		}
		isTest := strings.HasSuffix(name, "_test.go")
		if isTest != testFiles {
			continue
		}
		f, err := parser.ParseFile(fset, filepath.Join(dir, name), nil, 0)
		if err != nil {
			return err
		}
		visit(f)
	}
	return nil
}

func emit(set map[string]bool) {
	out := make([]string, 0, len(set))
	for k := range set {
		out = append(out, k)
	}
	sort.Strings(out)
	for _, k := range out {
		fmt.Println(k)
	}
}

// modeAPI: exported methods with receiver *Template, plus exported top-level funcs.
func modeAPI() {
	set := map[string]bool{}
	err := parsePkg("text/template", false, func(f *ast.File) {
		for _, d := range f.Decls {
			fn, ok := d.(*ast.FuncDecl)
			if !ok || !fn.Name.IsExported() {
				continue
			}
			if fn.Recv == nil {
				set["func "+fn.Name.Name] = true
				continue
			}
			// method — keep only receivers on Template
			if recvType(fn.Recv.List[0].Type) == "Template" {
				set["Template."+fn.Name.Name] = true
			}
		}
	})
	fail(err)
	emit(set)
}

func recvType(e ast.Expr) string {
	switch t := e.(type) {
	case *ast.StarExpr:
		return recvType(t.X)
	case *ast.Ident:
		return t.Name
	}
	return ""
}

// modeNodes: exported struct type names ending in "Node" in text/template/parse.
func modeNodes() {
	set := map[string]bool{}
	err := parsePkg("text/template/parse", false, func(f *ast.File) {
		for _, d := range f.Decls {
			gd, ok := d.(*ast.GenDecl)
			if !ok || gd.Tok != token.TYPE {
				continue
			}
			for _, spec := range gd.Specs {
				ts := spec.(*ast.TypeSpec)
				if !ts.Name.IsExported() || !strings.HasSuffix(ts.Name.Name, "Node") {
					continue
				}
				if _, isStruct := ts.Type.(*ast.StructType); isStruct {
					set[ts.Name.Name] = true
				}
			}
		}
	})
	fail(err)
	emit(set)
}

// modeTests: every exported `func Test*` (recv nil) across the three packages.
func modeTests() {
	set := map[string]bool{}
	for _, pkg := range []string{"text/template", "text/template/parse", "html/template"} {
		short := pkg[strings.LastIndex(pkg, "/")+1:]
		if pkg == "text/template/parse" {
			short = "parse"
		}
		err := parsePkg(pkg, true, func(f *ast.File) {
			for _, d := range f.Decls {
				fn, ok := d.(*ast.FuncDecl)
				if !ok || fn.Recv != nil {
					continue
				}
				if strings.HasPrefix(fn.Name.Name, "Test") {
					set[short+"\t"+fn.Name.Name] = true
				}
			}
		})
		fail(err)
	}
	emit(set)
}

func fail(err error) {
	if err != nil {
		fmt.Fprintln(os.Stderr, "error:", err)
		os.Exit(1)
	}
}

func main() {
	mode := flag.String("mode", "api", "api | nodes | tests")
	flag.Parse()
	switch *mode {
	case "api":
		modeAPI()
	case "nodes":
		modeNodes()
	case "tests":
		modeTests()
	default:
		fmt.Fprintf(os.Stderr, "unknown -mode %q (want api|nodes|tests)\n", *mode)
		os.Exit(2)
	}
}
