package org.alexmond.gotmpl4j.benchmarks;

import java.io.StringReader;
import java.util.concurrent.TimeUnit;

import freemarker.template.Configuration;
import freemarker.template.Template;
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
 * Parse/compile cost of the table template (cold, per template). Thymeleaf is omitted:
 * its public API ({@code TemplateEngine.process}) does not cleanly separate parse from
 * render, so a comparable parse-only measurement isn't available.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Thread)
public class ParseBenchmark {

	private String gotmplSource;

	private String gotmplLargeSource;

	private String freemarkerSource;

	private Configuration freemarkerConfig;

	@Setup
	public void setup() {
		this.gotmplSource = Templates.load("table.gotmpl");
		this.gotmplLargeSource = largeChartTemplate();
		this.freemarkerSource = Templates.load("table.ftl");
		this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
		this.freemarkerConfig.setLogTemplateExceptions(false);
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
		t.parse("table", this.gotmplSource);
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
		return new Template("table", new StringReader(this.freemarkerSource), this.freemarkerConfig);
	}

}
