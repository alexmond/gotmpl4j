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
	"strconv"
	"text/template"

	"github.com/Masterminds/sprig/v3"
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

// --- Feature workloads: gotmpl4j's Sprig/pipeline surface, rendered through the original
// Go text/template + Masterminds/sprig (gotmpl4j-sprig's reference implementation), so the
// FeatureBenchmark numbers can be reported relative to native Go+Sprig on the same templates
// and data. Data mirrors FeatureBenchmark.buildData().

func featureData() map[string]any {
	items := make([]map[string]any, 12)
	rows := make([]map[string]any, 12)
	entries := make([]map[string]any, 12)
	for i := 0; i < 12; i++ {
		items[i] = map[string]any{"n": i, "label": "L" + strconv.Itoa(i)}
		rows[i] = map[string]any{"sym": "SYM" + strconv.Itoa(i), "qty": i * 7, "pct": float64(i-6) * 1.375}
		entries[i] = map[string]any{"name": "entry" + strconv.Itoa(i), "val": "v" + strconv.Itoa(i)}
	}
	lines := make([]int, 200)
	for i := range lines {
		lines[i] = i
	}
	return map[string]any{
		"words":   []string{"alpha", "bravo", "charlie", "delta", "echo", "foxtrot"},
		"nums":    []string{"three", "one", "two", "five", "four"},
		"items":   items,
		"empty":   []any{},
		"rows":    rows,
		"entries": entries,
		"lines":   lines,
	}
}

func benchFeature(b *testing.B, file string) {
	tpl := template.Must(template.New("f").Funcs(sprig.TxtFuncMap()).Parse(mustRead(file)))
	data := featureData()
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_ = tpl.Execute(io.Discard, data)
	}
}

func BenchmarkFeaturePipeline(b *testing.B)    { benchFeature(b, "feature_pipeline.gotmpl") }
func BenchmarkFeatureListDict(b *testing.B)    { benchFeature(b, "feature_listdict.gotmpl") }
func BenchmarkFeatureControlFlow(b *testing.B) { benchFeature(b, "feature_controlflow.gotmpl") }
func BenchmarkFeaturePrintf(b *testing.B)      { benchFeature(b, "feature_printf.gotmpl") }
func BenchmarkFeatureComposition(b *testing.B) { benchFeature(b, "feature_composition.gotmpl") }
func BenchmarkFeatureLarge(b *testing.B)       { benchFeature(b, "feature_large.gotmpl") }
