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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Contention workload — many threads render <em>one shared, compiled</em>
 * {@link GoTemplate} (the production shape: the Spring starter caches a single engine and
 * renders it per request; a shared-registry consumer does the same). The template lives
 * in a {@link Scope#Benchmark benchmark-scoped} state so every thread hits the same
 * {@code rootNodes} + shared reflection caches; per-thread data is {@link Scope#Thread
 * thread-scoped}.
 *
 * <p>
 * Run at 1/2/4/8 threads to read <strong>throughput scaling</strong>, and always with
 * {@code -prof gc} for per-op allocation. This is the harness for the shared-state
 * optimizations: it proves a shared cache (#117) or shared registry (#119)
 * <em>scales</em> under contention rather than becoming a lock/false-sharing bottleneck,
 * and that a fix did not regress single-thread throughput. Compare a before/after with
 * the inline JMH {@code keep=}/{@code baseline=} A/B verdict (see {@code PERF.md}).
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class ConcurrencyBenchmark {

	/** One compiled engine shared by every measurement thread. */
	@State(Scope.Benchmark)
	public static class Shared {

		GoTemplate gotmpl;

		@Setup
		public void setup() {
			this.gotmpl = new GoTemplate();
			// scope + range: exercises per-block scope frames + variable bind/restore.
			this.gotmpl.parse("scope", "{{ $id := .id }}{{ range .items }}{{ $id }}:{{ . }} {{ end }}");
			// getter + no-arg method: exercises the reflective method-dispatch path
			// (#117).
			this.gotmpl.parse("pojo", "{{ .Symbol }}/{{ .Doubled }}");
		}

	}

	/** Per-thread data, so threads render distinct inputs against the shared engine. */
	@State(Scope.Thread)
	public static class PerThread {

		Map<String, Object> mapData;

		Model pojoData;

		@Setup
		public void setup() {
			this.mapData = Map.of("id", 7, "items", List.of(10, 20, 30, 40));
			this.pojoData = new Model("SYM", 21);
		}

	}

	@Benchmark
	@Threads(1)
	public String scope1(Shared s, PerThread d) {
		return s.gotmpl.render("scope", d.mapData);
	}

	@Benchmark
	@Threads(2)
	public String scope2(Shared s, PerThread d) {
		return s.gotmpl.render("scope", d.mapData);
	}

	@Benchmark
	@Threads(4)
	public String scope4(Shared s, PerThread d) {
		return s.gotmpl.render("scope", d.mapData);
	}

	@Benchmark
	@Threads(8)
	public String scope8(Shared s, PerThread d) {
		return s.gotmpl.render("scope", d.mapData);
	}

	@Benchmark
	@Threads(8)
	public String methodDispatch8(Shared s, PerThread d) {
		return s.gotmpl.render("pojo", d.pojoData);
	}

	/** POJO with a JavaBean getter and a Go-style no-arg method. */
	public static class Model {

		private final String symbol;

		private final int n;

		public Model(String symbol, int n) {
			this.symbol = symbol;
			this.n = n;
		}

		public String getSymbol() {
			return this.symbol;
		}

		public int Doubled() {
			return this.n * 2;
		}

	}

}
