package org.alexmond.gotmpl4j.benchmarks;

import java.io.StringReader;
import java.util.concurrent.TimeUnit;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import freemarker.template.Configuration;
import freemarker.template.Template;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.alexmond.gotmpl4j.GoTemplate;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Parse/compile cost of the table template (cold, per template), measured across the same
 * engines as the render suite. Each engine re-parses on every invocation: Mustache via a
 * fresh {@code DefaultMustacheFactory} (its compiled-template cache lives on the factory)
 * and Pebble via a {@code cacheActive(false)} engine, so the number is a real parse, not
 * a cache lookup. Thymeleaf is omitted: its public API ({@code TemplateEngine.process})
 * does not cleanly separate parse from render, so a comparable parse-only measurement
 * isn't available.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Thread)
public class ParseBenchmark {

	private static final String TABLE = "table";

	private String gotmplSource;

	private String gotmplLargeSource;

	private String freemarkerSource;

	private Configuration freemarkerConfig;

	private String mustacheSource;

	private String pebbleSource;

	private PebbleEngine pebbleEngine;

	@Setup
	public void setup() {
		this.gotmplSource = Templates.load("table.gotmpl");
		this.gotmplLargeSource = largeChartTemplate();
		this.freemarkerSource = Templates.load("table.ftl");
		this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
		this.freemarkerConfig.setLogTemplateExceptions(false);
		this.mustacheSource = Templates.load("table.mustache");
		this.pebbleSource = Templates.load("table.pebble");
		// cacheActive(false) makes getLiteralTemplate re-parse every call (else it caches
		// by
		// the literal source and we'd be timing a map lookup, not a parse).
		this.pebbleEngine = new PebbleEngine.Builder().autoEscaping(false).cacheActive(false).build();
	}

	// A Helm-chart-shaped template: multi-KB of YAML text with hundreds of {{ … }}
	// actions and
	// no comments — the shape where the lexer's per-{{ work (delimiter/comment scanning)
	// dominates, as profiled in the jhelm chart-render workload (#95).
	private static String largeChartTemplate() {
		StringBuilder sb = new StringBuilder(16384);
		for (int i = 0; i < 60; i++) {
			sb.append("apiVersion: apps/v1\nkind: Deployment\nmetadata:\n");
			sb.append("  name: {{ .Values.name").append(i).append(" }}\n");
			sb.append("  labels:\n    app: {{ .Values.app }}\n    release: {{ .Release.Name }}\n");
			sb.append("spec:\n  replicas: {{ .Values.replicas").append(i).append(" }}\n");
			sb.append("  template:\n    spec:\n      containers:\n      - name: {{ .Values.container }}\n");
			sb.append("        image: {{ .Values.image }}:{{ .Values.tag }}\n");
			sb.append("        {{- if .Values.resources }}\n        resources:\n          limits:\n");
			sb.append("            cpu: {{ .Values.cpu }}\n            memory: {{ .Values.mem }}\n");
			sb.append("        {{- end }}\n        env:\n        {{- range $k, $v := .Values.env }}\n");
			sb.append("        - name: {{ $k }}\n          value: {{ $v | quote }}\n        {{- end }}\n---\n");
		}
		return sb.toString();
	}

	@Benchmark
	public GoTemplate gotmpl4jParse() {
		GoTemplate t = new GoTemplate();
		t.parse(TABLE, this.gotmplSource);
		return t;
	}

	@Benchmark
	public GoTemplate gotmpl4jParseLargeChart() {
		GoTemplate t = new GoTemplate();
		t.parse("chart", this.gotmplLargeSource);
		return t;
	}

	@Benchmark
	public Template freemarkerParse() throws Exception {
		return new Template(TABLE, new StringReader(this.freemarkerSource), this.freemarkerConfig);
	}

	@Benchmark
	public Mustache mustacheParse() {
		return new DefaultMustacheFactory().compile(new StringReader(this.mustacheSource), TABLE);
	}

	@Benchmark
	public PebbleTemplate pebbleParse() {
		return this.pebbleEngine.getLiteralTemplate(this.pebbleSource);
	}

}
