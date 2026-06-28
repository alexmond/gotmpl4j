// Package benchmarks measures Go's native text/template on the SAME templates and data as
// the JVM gotmpl4j benchmark, so gotmpl4j can be reported relative to the original engine.
//
// gotmpl4j renders identical Go-template syntax, so the table template is read verbatim from
// the shared JVM resources directory. Run from this directory:
//
//	go test -bench . -benchmem
package benchmarks

import (
	"io"
	"os"
	"testing"
	"text/template"
)

const templatesDir = "../../gotmpl4j-benchmarks/src/main/resources/templates/"

// Stock mirrors the JVM Stock model. Fields are exported so {{ .Symbol }} resolves them,
// matching gotmpl4j's Go-style property mapping over getSymbol().
type Stock struct {
	Symbol string
	Name   string
	Price  float64
	Change float64
	Ratio  float64
	Minus  bool
}

func stocks(n int) []Stock {
	symbols := []string{"ADBE", "AMD", "AAPL", "AMZN", "BIDU", "CSCO", "GOOG", "INTC", "MSFT", "NFLX"}
	out := make([]Stock, n)
	for i := 0; i < n; i++ {
		s := symbols[i%len(symbols)]
		price := 100.0 + float64(i%500)
		change := float64((i%7)-3) * 1.25
		out[i] = Stock{Symbol: s, Name: s + " Inc.", Price: price, Change: change, Ratio: change / price * 100.0,
			Minus: change < 0}
	}
	return out
}

func mustRead(name string) string {
	b, err := os.ReadFile(templatesDir + name)
	if err != nil {
		panic(err)
	}
	return string(b)
}

func BenchmarkInterpolationRender(b *testing.B) {
	tpl := template.Must(template.New("hello").Parse(mustRead("hello.gotmpl")))
	data := struct{ Name string }{"World"}
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_ = tpl.Execute(io.Discard, data)
	}
}

func benchTable(b *testing.B, n int) {
	tpl := template.Must(template.New("table").Parse(mustRead("table.gotmpl")))
	data := map[string]any{"Items": stocks(n)}
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_ = tpl.Execute(io.Discard, data)
	}
}

func BenchmarkTableRender10(b *testing.B)   { benchTable(b, 10) }
func BenchmarkTableRender100(b *testing.B)  { benchTable(b, 100) }
func BenchmarkTableRender1000(b *testing.B) { benchTable(b, 1000) }

func BenchmarkTableParse(b *testing.B) {
	src := mustRead("table.gotmpl")
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_ = template.Must(template.New("table").Parse(src))
	}
}
