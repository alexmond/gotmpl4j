// Command content_extract ports Go html/template's content_test.go TestTypedContent
// table into a base64 TSV fixture for HtmlTypedContentConformanceTest (core).
//
// TestTypedContent renders a matrix: each template (a single {{.}} in some HTML/CSS/JS/URL
// context) against nine *typed* content values (a plain string plus template.CSS / HTML /
// HTMLAttr / JS / JSStr / URL / Srcset). The expected output is either the typed content
// passed through (when the context matches its safe type) or Go's "ZgotmplZ" filter-failsafe
// (when it doesn't). gotmpl4j models the same typed kinds via SafeContent and the same
// ZgotmplZ filtering, so this is a real end-to-end conformance net for the typed-content
// contract.
//
// We render through the *real* html/template engine here (not the table's literal `want`),
// so the output column is ground truth; the data index column lets the Java side rebuild the
// matching SafeContent value. Run:
//
//	go run content_extract.go -out ../../../gotmpl4j-core/src/test/resources/html
package main

import (
	"bytes"
	"encoding/base64"
	"flag"
	"fmt"
	"html/template"
	"os"
	"path/filepath"
	"strings"
)

// data mirrors content_test.go's `data []any` — index-aligned with the Java side's SafeContent
// values. The typed kinds use html/template's exported content types.
var data = []any{
	`<b> "foo%" O'Reilly &bar;`,
	template.CSS(`a[href =~ "//example.com"]#foo`),
	template.HTML(`Hello, <b>World</b> &amp;tc!`),
	template.HTMLAttr(` dir="ltr"`),
	template.JS(`c && alert("Hello, World!");`),
	template.JSStr(`Hello, World & O'Reilly\u0021`),
	template.URL(`greeting=H%69,&addressee=(World)`),
	template.Srcset(`greeting=H%69,&addressee=(World) 2x, https://golang.org/favicon.ico 500.5w`),
	template.URL(`,foo/,`),
}

func main() {
	out := flag.String("out", ".", "directory to write html_typed_content_cases.tsv")
	flag.Parse()
	if err := os.MkdirAll(*out, 0o755); err != nil {
		panic(err)
	}
	tests := []struct {
		// A template containing a single {{.}}.
		input string
		want  []string
	}{
		{
			`<style>{{.}} { color: blue }</style>`,
			[]string{
				`ZgotmplZ`,
				// Allowed but not escaped.
				`a[href =~ "//example.com"]#foo`,
				`ZgotmplZ`,
				`ZgotmplZ`,
				`ZgotmplZ`,
				`ZgotmplZ`,
				`ZgotmplZ`,
				`ZgotmplZ`,
				`ZgotmplZ`,
			},
		},
		{
			`<div style="{{.}}">`,
			[]string{
				`ZgotmplZ`,
				// Allowed and HTML escaped.
				`a[href =~ &#34;//example.com&#34;]#foo`,
				`ZgotmplZ`,
				`ZgotmplZ`,
				`ZgotmplZ`,
				`ZgotmplZ`,
				`ZgotmplZ`,
				`ZgotmplZ`,
				`ZgotmplZ`,
			},
		},
		{
			`{{.}}`,
			[]string{
				`&lt;b&gt; &#34;foo%&#34; O&#39;Reilly &amp;bar;`,
				`a[href =~ &#34;//example.com&#34;]#foo`,
				// Not escaped.
				`Hello, <b>World</b> &amp;tc!`,
				` dir=&#34;ltr&#34;`,
				`c &amp;&amp; alert(&#34;Hello, World!&#34;);`,
				`Hello, World &amp; O&#39;Reilly\u0021`,
				`greeting=H%69,&amp;addressee=(World)`,
				`greeting=H%69,&amp;addressee=(World) 2x, https://golang.org/favicon.ico 500.5w`,
				`,foo/,`,
			},
		},
		{
			`<a{{.}}>`,
			[]string{
				`ZgotmplZ`,
				`ZgotmplZ`,
				`ZgotmplZ`,
				// Allowed and HTML escaped.
				` dir="ltr"`,
				`ZgotmplZ`,
				`ZgotmplZ`,
				`ZgotmplZ`,
				`ZgotmplZ`,
				`ZgotmplZ`,
			},
		},
		{
			`<a title={{.}}>`,
			[]string{
				`&lt;b&gt;&#32;&#34;foo%&#34;&#32;O&#39;Reilly&#32;&amp;bar;`,
				`a[href&#32;&#61;~&#32;&#34;//example.com&#34;]#foo`,
				// Tags stripped, spaces escaped, entity not re-escaped.
				`Hello,&#32;World&#32;&amp;tc!`,
				`&#32;dir&#61;&#34;ltr&#34;`,
				`c&#32;&amp;&amp;&#32;alert(&#34;Hello,&#32;World!&#34;);`,
				`Hello,&#32;World&#32;&amp;&#32;O&#39;Reilly\u0021`,
				`greeting&#61;H%69,&amp;addressee&#61;(World)`,
				`greeting&#61;H%69,&amp;addressee&#61;(World)&#32;2x,&#32;https://golang.org/favicon.ico&#32;500.5w`,
				`,foo/,`,
			},
		},
		{
			`<a title='{{.}}'>`,
			[]string{
				`&lt;b&gt; &#34;foo%&#34; O&#39;Reilly &amp;bar;`,
				`a[href =~ &#34;//example.com&#34;]#foo`,
				// Tags stripped, entity not re-escaped.
				`Hello, World &amp;tc!`,
				` dir=&#34;ltr&#34;`,
				`c &amp;&amp; alert(&#34;Hello, World!&#34;);`,
				`Hello, World &amp; O&#39;Reilly\u0021`,
				`greeting=H%69,&amp;addressee=(World)`,
				`greeting=H%69,&amp;addressee=(World) 2x, https://golang.org/favicon.ico 500.5w`,
				`,foo/,`,
			},
		},
		{
			`<textarea>{{.}}</textarea>`,
			[]string{
				`&lt;b&gt; &#34;foo%&#34; O&#39;Reilly &amp;bar;`,
				`a[href =~ &#34;//example.com&#34;]#foo`,
				// Angle brackets escaped to prevent injection of close tags, entity not re-escaped.
				`Hello, &lt;b&gt;World&lt;/b&gt; &amp;tc!`,
				` dir=&#34;ltr&#34;`,
				`c &amp;&amp; alert(&#34;Hello, World!&#34;);`,
				`Hello, World &amp; O&#39;Reilly\u0021`,
				`greeting=H%69,&amp;addressee=(World)`,
				`greeting=H%69,&amp;addressee=(World) 2x, https://golang.org/favicon.ico 500.5w`,
				`,foo/,`,
			},
		},
		{
			`<script>alert({{.}})</script>`,
			[]string{
				`"\u003cb\u003e \"foo%\" O'Reilly \u0026bar;"`,
				`"a[href =~ \"//example.com\"]#foo"`,
				`"Hello, \u003cb\u003eWorld\u003c/b\u003e \u0026amp;tc!"`,
				`" dir=\"ltr\""`,
				// Not escaped.
				`c && alert("Hello, World!");`,
				// Escape sequence not over-escaped.
				`"Hello, World & O'Reilly\u0021"`,
				`"greeting=H%69,\u0026addressee=(World)"`,
				`"greeting=H%69,\u0026addressee=(World) 2x, https://golang.org/favicon.ico 500.5w"`,
				`",foo/,"`,
			},
		},
		{
			`<button onclick="alert({{.}})">`,
			[]string{
				`&#34;\u003cb\u003e \&#34;foo%\&#34; O&#39;Reilly \u0026bar;&#34;`,
				`&#34;a[href =~ \&#34;//example.com\&#34;]#foo&#34;`,
				`&#34;Hello, \u003cb\u003eWorld\u003c/b\u003e \u0026amp;tc!&#34;`,
				`&#34; dir=\&#34;ltr\&#34;&#34;`,
				// Not JS escaped but HTML escaped.
				`c &amp;&amp; alert(&#34;Hello, World!&#34;);`,
				// Escape sequence not over-escaped.
				`&#34;Hello, World &amp; O&#39;Reilly\u0021&#34;`,
				`&#34;greeting=H%69,\u0026addressee=(World)&#34;`,
				`&#34;greeting=H%69,\u0026addressee=(World) 2x, https://golang.org/favicon.ico 500.5w&#34;`,
				`&#34;,foo/,&#34;`,
			},
		},
		{
			`<script>alert("{{.}}")</script>`,
			[]string{
				`\u003cb\u003e \u0022foo%\u0022 O\u0027Reilly \u0026bar;`,
				`a[href =~ \u0022\/\/example.com\u0022]#foo`,
				`Hello, \u003cb\u003eWorld\u003c\/b\u003e \u0026amp;tc!`,
				` dir=\u0022ltr\u0022`,
				`c \u0026\u0026 alert(\u0022Hello, World!\u0022);`,
				// Escape sequence not over-escaped.
				`Hello, World \u0026 O\u0027Reilly\u0021`,
				`greeting=H%69,\u0026addressee=(World)`,
				`greeting=H%69,\u0026addressee=(World) 2x, https:\/\/golang.org\/favicon.ico 500.5w`,
				`,foo\/,`,
			},
		},
		{
			`<script type="text/javascript">alert("{{.}}")</script>`,
			[]string{
				`\u003cb\u003e \u0022foo%\u0022 O\u0027Reilly \u0026bar;`,
				`a[href =~ \u0022\/\/example.com\u0022]#foo`,
				`Hello, \u003cb\u003eWorld\u003c\/b\u003e \u0026amp;tc!`,
				` dir=\u0022ltr\u0022`,
				`c \u0026\u0026 alert(\u0022Hello, World!\u0022);`,
				// Escape sequence not over-escaped.
				`Hello, World \u0026 O\u0027Reilly\u0021`,
				`greeting=H%69,\u0026addressee=(World)`,
				`greeting=H%69,\u0026addressee=(World) 2x, https:\/\/golang.org\/favicon.ico 500.5w`,
				`,foo\/,`,
			},
		},
		{
			`<script type="text/javascript">alert({{.}})</script>`,
			[]string{
				`"\u003cb\u003e \"foo%\" O'Reilly \u0026bar;"`,
				`"a[href =~ \"//example.com\"]#foo"`,
				`"Hello, \u003cb\u003eWorld\u003c/b\u003e \u0026amp;tc!"`,
				`" dir=\"ltr\""`,
				// Not escaped.
				`c && alert("Hello, World!");`,
				// Escape sequence not over-escaped.
				`"Hello, World & O'Reilly\u0021"`,
				`"greeting=H%69,\u0026addressee=(World)"`,
				`"greeting=H%69,\u0026addressee=(World) 2x, https://golang.org/favicon.ico 500.5w"`,
				`",foo/,"`,
			},
		},
		{
			// Not treated as JS. The output is same as for <div>{{.}}</div>
			`<script type="text/template">{{.}}</script>`,
			[]string{
				`&lt;b&gt; &#34;foo%&#34; O&#39;Reilly &amp;bar;`,
				`a[href =~ &#34;//example.com&#34;]#foo`,
				// Not escaped.
				`Hello, <b>World</b> &amp;tc!`,
				` dir=&#34;ltr&#34;`,
				`c &amp;&amp; alert(&#34;Hello, World!&#34;);`,
				`Hello, World &amp; O&#39;Reilly\u0021`,
				`greeting=H%69,&amp;addressee=(World)`,
				`greeting=H%69,&amp;addressee=(World) 2x, https://golang.org/favicon.ico 500.5w`,
				`,foo/,`,
			},
		},
		{
			`<button onclick='alert("{{.}}")'>`,
			[]string{
				`\u003cb\u003e \u0022foo%\u0022 O\u0027Reilly \u0026bar;`,
				`a[href =~ \u0022\/\/example.com\u0022]#foo`,
				`Hello, \u003cb\u003eWorld\u003c\/b\u003e \u0026amp;tc!`,
				` dir=\u0022ltr\u0022`,
				`c \u0026\u0026 alert(\u0022Hello, World!\u0022);`,
				// Escape sequence not over-escaped.
				`Hello, World \u0026 O\u0027Reilly\u0021`,
				`greeting=H%69,\u0026addressee=(World)`,
				`greeting=H%69,\u0026addressee=(World) 2x, https:\/\/golang.org\/favicon.ico 500.5w`,
				`,foo\/,`,
			},
		},
		{
			`<a href="?q={{.}}">`,
			[]string{
				`%3cb%3e%20%22foo%25%22%20O%27Reilly%20%26bar%3b`,
				`a%5bhref%20%3d~%20%22%2f%2fexample.com%22%5d%23foo`,
				`Hello%2c%20%3cb%3eWorld%3c%2fb%3e%20%26amp%3btc%21`,
				`%20dir%3d%22ltr%22`,
				`c%20%26%26%20alert%28%22Hello%2c%20World%21%22%29%3b`,
				`Hello%2c%20World%20%26%20O%27Reilly%5cu0021`,
				// Quotes and parens are escaped but %69 is not over-escaped. HTML escaping is done.
				`greeting=H%69,&amp;addressee=%28World%29`,
				`greeting%3dH%2569%2c%26addressee%3d%28World%29%202x%2c%20https%3a%2f%2fgolang.org%2ffavicon.ico%20500.5w`,
				`,foo/,`,
			},
		},
		{
			`<style>body { background: url('?img={{.}}') }</style>`,
			[]string{
				`%3cb%3e%20%22foo%25%22%20O%27Reilly%20%26bar%3b`,
				`a%5bhref%20%3d~%20%22%2f%2fexample.com%22%5d%23foo`,
				`Hello%2c%20%3cb%3eWorld%3c%2fb%3e%20%26amp%3btc%21`,
				`%20dir%3d%22ltr%22`,
				`c%20%26%26%20alert%28%22Hello%2c%20World%21%22%29%3b`,
				`Hello%2c%20World%20%26%20O%27Reilly%5cu0021`,
				// Quotes and parens are escaped but %69 is not over-escaped. HTML escaping is not done.
				`greeting=H%69,&addressee=%28World%29`,
				`greeting%3dH%2569%2c%26addressee%3d%28World%29%202x%2c%20https%3a%2f%2fgolang.org%2ffavicon.ico%20500.5w`,
				`,foo/,`,
			},
		},
		{
			`<img srcset="{{.}}">`,
			[]string{
				`#ZgotmplZ`,
				`#ZgotmplZ`,
				// Commas are not escaped.
				`Hello,#ZgotmplZ`,
				// Leading spaces are not percent escapes.
				` dir=%22ltr%22`,
				// Spaces after commas are not percent escaped.
				`#ZgotmplZ, World!%22%29;`,
				`Hello,#ZgotmplZ`,
				`greeting=H%69%2c&amp;addressee=%28World%29`,
				// Metadata is not escaped.
				`greeting=H%69,&amp;addressee=(World) 2x, https://golang.org/favicon.ico 500.5w`,
				`%2cfoo/%2c`,
			},
		},
		{
			`<img srcset={{.}}>`,
			[]string{
				`#ZgotmplZ`,
				`#ZgotmplZ`,
				`Hello,#ZgotmplZ`,
				// Spaces are HTML escaped not %-escaped
				`&#32;dir&#61;%22ltr%22`,
				`#ZgotmplZ,&#32;World!%22%29;`,
				`Hello,#ZgotmplZ`,
				`greeting&#61;H%69%2c&amp;addressee&#61;%28World%29`,
				`greeting&#61;H%69,&amp;addressee&#61;(World)&#32;2x,&#32;https://golang.org/favicon.ico&#32;500.5w`,
				// Commas are escaped.
				`%2cfoo/%2c`,
			},
		},
		{
			`<img srcset="{{.}} 2x, https://golang.org/ 500.5w">`,
			[]string{
				`#ZgotmplZ`,
				`#ZgotmplZ`,
				`Hello,#ZgotmplZ`,
				` dir=%22ltr%22`,
				`#ZgotmplZ, World!%22%29;`,
				`Hello,#ZgotmplZ`,
				`greeting=H%69%2c&amp;addressee=%28World%29`,
				`greeting=H%69,&amp;addressee=(World) 2x, https://golang.org/favicon.ico 500.5w`,
				`%2cfoo/%2c`,
			},
		},
		{
			`<img srcset="http://godoc.org/ {{.}}, https://golang.org/ 500.5w">`,
			[]string{
				`#ZgotmplZ`,
				`#ZgotmplZ`,
				`Hello,#ZgotmplZ`,
				` dir=%22ltr%22`,
				`#ZgotmplZ, World!%22%29;`,
				`Hello,#ZgotmplZ`,
				`greeting=H%69%2c&amp;addressee=%28World%29`,
				`greeting=H%69,&amp;addressee=(World) 2x, https://golang.org/favicon.ico 500.5w`,
				`%2cfoo/%2c`,
			},
		},
		{
			`<img srcset="http://godoc.org/?q={{.}} 2x, https://golang.org/ 500.5w">`,
			[]string{
				`#ZgotmplZ`,
				`#ZgotmplZ`,
				`Hello,#ZgotmplZ`,
				` dir=%22ltr%22`,
				`#ZgotmplZ, World!%22%29;`,
				`Hello,#ZgotmplZ`,
				`greeting=H%69%2c&amp;addressee=%28World%29`,
				`greeting=H%69,&amp;addressee=(World) 2x, https://golang.org/favicon.ico 500.5w`,
				`%2cfoo/%2c`,
			},
		},
		{
			`<img srcset="http://godoc.org/ 2x, {{.}} 500.5w">`,
			[]string{
				`#ZgotmplZ`,
				`#ZgotmplZ`,
				`Hello,#ZgotmplZ`,
				` dir=%22ltr%22`,
				`#ZgotmplZ, World!%22%29;`,
				`Hello,#ZgotmplZ`,
				`greeting=H%69%2c&amp;addressee=%28World%29`,
				`greeting=H%69,&amp;addressee=(World) 2x, https://golang.org/favicon.ico 500.5w`,
				`%2cfoo/%2c`,
			},
		},
		{
			`<img srcset="http://godoc.org/ 2x, https://golang.org/ {{.}}">`,
			[]string{
				`#ZgotmplZ`,
				`#ZgotmplZ`,
				`Hello,#ZgotmplZ`,
				` dir=%22ltr%22`,
				`#ZgotmplZ, World!%22%29;`,
				`Hello,#ZgotmplZ`,
				`greeting=H%69%2c&amp;addressee=%28World%29`,
				`greeting=H%69,&amp;addressee=(World) 2x, https://golang.org/favicon.ico 500.5w`,
				`%2cfoo/%2c`,
			},
		},
	}


	enc := base64.StdEncoding
	var rows []string
	for _, test := range tests {
		tmpl, err := template.New("x").Parse(test.input)
		if err != nil {
			fmt.Fprintf(os.Stderr, "skip (parse): %q: %v\n", test.input, err)
			continue
		}
		for i, d := range data {
			var buf bytes.Buffer
			if err := tmpl.Execute(&buf, d); err != nil {
				fmt.Fprintf(os.Stderr, "skip (exec) %q[%d]: %v\n", test.input, i, err)
				continue
			}
			got := buf.String()
			// Self-check: the table's want[i] is the fragment that replaces the single
			// {{.}}, so the full render must equal input with {{.}} -> want[i].
			if i < len(test.want) {
				expect := strings.Replace(test.input, "{{.}}", test.want[i], 1)
				if got != expect {
					fmt.Fprintf(os.Stderr, "MISMATCH %q[%d]: got=%q expect=%q\n", test.input, i, got, expect)
				}
			}
			rows = append(rows, fmt.Sprintf("%s\t%s\t%d",
				enc.EncodeToString([]byte(test.input)), enc.EncodeToString([]byte(got)), i))
		}
	}
	dst := filepath.Join(*out, "html_typed_content_cases.tsv")
	body := ""
	for _, r := range rows {
		body += r + "\n"
	}
	if err := os.WriteFile(dst, []byte(body), 0o644); err != nil {
		panic(err)
	}
	fmt.Printf("html_typed_content_cases.tsv  %d cases -> %s\n", len(rows), dst)
}
