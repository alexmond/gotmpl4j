package org.alexmond.gotmpl4j.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
 * Workload 3 — gotmpl4j's distinctive surface: Sprig function pipelines, list/dict
 * functions, nested control flow, {@code printf} formatting, template composition, and a
 * large-output (writer-stress) render. These exercise Go-template + Sprig features the
 * cross-engine comparison can't express, so they are <em>single-engine</em> and track
 * gotmpl4j's own performance over time (and over optimizations) rather than ranking it
 * against other engines. Each template is pre-parsed in {@link #setup()}, so the
 * benchmark measures the hot render path.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Thread)
public class FeatureBenchmark {

	private GoTemplate gotmpl;

	private Map<String, Object> data;

	@Setup
	public void setup() {
		this.gotmpl = new GoTemplate(); // auto-loads Sprig from the classpath
		this.gotmpl.parse("pipeline", Templates.load("feature_pipeline.gotmpl"));
		this.gotmpl.parse("listdict", Templates.load("feature_listdict.gotmpl"));
		this.gotmpl.parse("controlflow", Templates.load("feature_controlflow.gotmpl"));
		this.gotmpl.parse("printf", Templates.load("feature_printf.gotmpl"));
		this.gotmpl.parse("composition", Templates.load("feature_composition.gotmpl"));
		this.gotmpl.parse("large", Templates.load("feature_large.gotmpl"));
		this.data = buildData();
	}

	private static Map<String, Object> buildData() {
		List<String> words = List.of("alpha", "bravo", "charlie", "delta", "echo", "foxtrot");
		List<String> nums = List.of("three", "one", "two", "five", "four");

		List<Map<String, Object>> items = new ArrayList<>();
		List<Map<String, Object>> rows = new ArrayList<>();
		List<Map<String, Object>> entries = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			items.add(Map.of("n", i, "label", "L" + i));
			rows.add(Map.of("sym", "SYM" + i, "qty", i * 7, "pct", (i - 6) * 1.375));
			entries.add(Map.of("name", "entry" + i, "val", "v" + i));
		}

		List<Integer> lines = new ArrayList<>();
		for (int i = 0; i < 200; i++) {
			lines.add(i);
		}

		return Map.of("words", words, "nums", nums, "items", items, "empty", List.of(), "rows", rows, "entries",
				entries, "lines", lines);
	}

	@Benchmark
	public String sprigPipeline() {
		return this.gotmpl.render("pipeline", this.data);
	}

	@Benchmark
	public String listDict() {
		return this.gotmpl.render("listdict", this.data);
	}

	@Benchmark
	public String controlFlow() {
		return this.gotmpl.render("controlflow", this.data);
	}

	@Benchmark
	public String printf() {
		return this.gotmpl.render("printf", this.data);
	}

	@Benchmark
	public String composition() {
		return this.gotmpl.render("composition", this.data);
	}

	@Benchmark
	public String largeOutput() {
		return this.gotmpl.render("large", this.data);
	}

}
