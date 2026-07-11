package org.alexmond.gotmpl4j.benchmarks;

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
 * Include-heavy render — the jhelm shape (a {@code _helpers.tpl} partial invoked from
 * every manifest, often in a loop) and the workload #114 targets. Each {@code {{ template
 * "helper" . }}} currently snapshots and restores the whole variable map + scope deque
 * around the call; this benchmark makes that clone-per-call the dominant cost so the fix
 * shows up. Run with {@code -prof gc}; the allocation attributed to
 * {@code Executor.writeTemplate} is the point.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Thread)
public class IncludeBenchmark {

	private GoTemplate gotmpl;

	private Map<String, Object> data;

	@Setup
	public void setup() {
		this.gotmpl = new GoTemplate();
		// jhelm shape: several caller-scope variables live, and the include nested inside
		// range+with (a deep scope stack), so the per-call save/restore of the variable
		// map + scope deque has realistic content to copy (#114).
		this.gotmpl.parse("main",
				"{{ $v1 := .greeting }}{{ $v2 := .greeting }}{{ $v3 := .greeting }}{{ $v4 := .greeting }}"
						+ "{{ $v5 := .greeting }}{{ $v6 := .greeting }}{{ $v7 := .greeting }}{{ $v8 := .greeting }}"
						+ "{{ range .rows }}{{ $r := . }}{{ with .sym }}{{ $w := . }}"
						+ "{{ template \"row\" $ }}{{ end }}{{ end }}{{ $v1 }}");
		this.gotmpl.parse("row", "{{ .greeting }};");
		this.data = buildData();
	}

	private static Map<String, Object> buildData() {
		java.util.List<Map<String, Object>> rows = new java.util.ArrayList<>();
		for (int i = 0; i < 50; i++) {
			rows.add(Map.of("sym", "SYM" + i, "qty", i * 7));
		}
		return Map.of("greeting", "hi", "rows", List.copyOf(rows));
	}

	@Benchmark
	public String includeLoop() {
		return this.gotmpl.render("main", this.data);
	}

}
