//go:build ignore

// builtins_extract.go — enumerate Go's built-in template functions straight from
// the stdlib source of the toolchain running this program, so the list is
// authoritative for that Go version rather than hand-maintained.
//
// text/template's builtins() returns an *unexported* FuncMap, so it can't be
// reflected from a normal program. Instead we parse $GOROOT/src/text/template/
// funcs.go with go/ast and read the map keys from the `builtins()` return
// literal. We also surface html/template's internal escaper funcMap (the
// `_html_template_*` names) for completeness — those are injected by the
// auto-escaping pass and are NOT user-callable.
//
// Usage:
//
//	go run builtins_extract.go            # human-readable, grouped
//	go run builtins_extract.go -names     # one user-facing builtin per line
//	go run builtins_extract.go -names | sort > /tmp/go-builtins.txt
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
)

// mapKeysFromFunc parses goFile, finds the top-level function funcName, and
// returns the string keys of the first composite-literal map it returns
// (text/template's builtins) or, if funcName is empty, of the first top-level
// `var x = FuncMap{...}` (html/template's funcMap).
func mapKeys(goFile, funcName, varName string) ([]string, error) {
	fset := token.NewFileSet()
	f, err := parser.ParseFile(fset, goFile, nil, 0)
	if err != nil {
		return nil, err
	}
	var lit *ast.CompositeLit
	ast.Inspect(f, func(n ast.Node) bool {
		if lit != nil {
			return false
		}
		if funcName != "" {
			fn, ok := n.(*ast.FuncDecl)
			if !ok || fn.Name.Name != funcName {
				return true
			}
			ast.Inspect(fn.Body, func(m ast.Node) bool {
				if cl, ok := m.(*ast.CompositeLit); ok && lit == nil {
					lit = cl
					return false
				}
				return true
			})
			return false
		}
		// varName branch: `var funcMap = template.FuncMap{...}`
		vs, ok := n.(*ast.ValueSpec)
		if !ok {
			return true
		}
		for i, name := range vs.Names {
			if name.Name == varName && i < len(vs.Values) {
				if cl, ok := vs.Values[i].(*ast.CompositeLit); ok {
					lit = cl
				}
			}
		}
		return true
	})
	if lit == nil {
		return nil, fmt.Errorf("no map literal found for %q/%q in %s", funcName, varName, goFile)
	}
	var keys []string
	for _, el := range lit.Elts {
		kv, ok := el.(*ast.KeyValueExpr)
		if !ok {
			continue
		}
		if bl, ok := kv.Key.(*ast.BasicLit); ok && bl.Kind == token.STRING {
			keys = append(keys, bl.Value[1:len(bl.Value)-1]) // strip quotes
		}
	}
	sort.Strings(keys)
	return keys, nil
}

func main() {
	namesOnly := flag.Bool("names", false, "print one user-facing builtin name per line")
	flag.Parse()

	goroot := runtime.GOROOT()
	textFuncs := filepath.Join(goroot, "src", "text", "template", "funcs.go")
	htmlEscape := filepath.Join(goroot, "src", "html", "template", "escape.go")

	userFacing, err := mapKeys(textFuncs, "builtins", "")
	if err != nil {
		fmt.Fprintln(os.Stderr, "error:", err)
		os.Exit(1)
	}

	if *namesOnly {
		for _, k := range userFacing {
			fmt.Println(k)
		}
		return
	}

	fmt.Printf("Go %s — text/template built-in functions (user-callable): %d\n", runtime.Version(), len(userFacing))
	for _, k := range userFacing {
		fmt.Printf("  %s\n", k)
	}

	if internal, err := mapKeys(htmlEscape, "", "funcMap"); err == nil {
		fmt.Printf("\nhtml/template internal escapers (auto-injected, NOT user-callable): %d\n", len(internal))
		for _, k := range internal {
			fmt.Printf("  %s\n", k)
		}
	}
}
