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
 * Scope-frame allocation — the workload #115 targets. A {@code range} over N items with a
 * nested {@code if}, and <em>no</em> variable declarations, pushes a scope frame per
 * range-iteration and per if with nothing to record. This isolates the per-block frame
 * allocation the pre-{@code ScopeUndo}-rewrite issue reported at 1.8 GB (much of which
 * was the map clone, now gone). Run with {@code -prof gc}.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Thread)
public class ScopeFrameBenchmark {

	private GoTemplate gotmpl;

	private Map<String, Object> data;

	@Setup
	public void setup() {
		this.gotmpl = new GoTemplate();
		// range + nested if, no ':=' declarations -> two frames per item, both empty.
		this.gotmpl.parse("main", "{{ range .items }}{{ if .on }}{{ .v }}{{ end }} {{ end }}");
		List<Map<String, Object>> items = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			items.add(Map.of("on", true, "v", "x" + i));
		}
		this.data = Map.of("items", List.copyOf(items));
	}

	@Benchmark
	public String scopeFrames() {
		return this.gotmpl.render("main", this.data);
	}

}
