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

	private String freemarkerSource;

	private Configuration freemarkerConfig;

	@Setup
	public void setup() {
		this.gotmplSource = Templates.load("table.gotmpl");
		this.freemarkerSource = Templates.load("table.ftl");
		this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
		this.freemarkerConfig.setLogTemplateExceptions(false);
	}

	@Benchmark
	public GoTemplate gotmpl4jParse() {
		GoTemplate t = new GoTemplate();
		t.parse("table", this.gotmplSource);
		return t;
	}

	@Benchmark
	public Template freemarkerParse() throws Exception {
		return new Template("table", new StringReader(this.freemarkerSource), this.freemarkerConfig);
	}

}
