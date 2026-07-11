package org.alexmond.gotmpl4j.benchmarks;

import java.util.concurrent.TimeUnit;

import org.alexmond.gotmpl4j.GoTemplate;
import org.alexmond.gotmpl4j.GoTemplateRegistry;
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
 * Construction cost — the win from #119. A consumer that needs a clean template namespace
 * per operation (e.g. jhelm rendering one chart per call) builds a {@link GoTemplate} per
 * render. {@code newGoTemplate} pays the full cost every time (ServiceLoader + rebuilding
 * every Sprig lambda + cold reflection caches); {@code viaSharedRegistry} builds the same
 * fresh namespace but reuses a pre-built {@link GoTemplateRegistry} (Sprig built once,
 * caches warm across instances). Run with {@code -prof gc} — the allocation delta is the
 * point (the CPU/alloc that #119 removes from the construct-per-render path).
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class RegistryBenchmark {

	private GoTemplateRegistry registry;

	@Setup
	public void setup() {
		this.registry = GoTemplateRegistry.create();
	}

	@Benchmark
	public String newGoTemplate() {
		GoTemplate t = new GoTemplate();
		t.parse("t", "{{ upper .in }}");
		return t.render(java.util.Map.of("in", "hello"));
	}

	@Benchmark
	public String viaSharedRegistry() {
		GoTemplate t = GoTemplate.builder().registry(this.registry).build();
		t.parse("t", "{{ upper .in }}");
		return t.render(java.util.Map.of("in", "hello"));
	}

}
