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
 * Method-dispatch-heavy render — the POJO/method shape that the map/field-based feature
 * benchmarks never exercise, which is exactly why method dispatch (#117) never topped a
 * profile. Rendering a no-arg method ({@code .Doubled}) and a method-with-args
 * ({@code .Scale 3}) over a list of POJOs drives the two uncached
 * {@code getClass().getMethods()} scans per access. This is also the jgomplate namespace
 * shape ({@code {{ strings.ToUpper }}} is method dispatch on a POJO). Run with
 * {@code -prof gc}.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Thread)
public class PojoDispatchBenchmark {

	private GoTemplate gotmpl;

	private Map<String, Object> data;

	@Setup
	public void setup() {
		this.gotmpl = new GoTemplate();
		this.gotmpl.parse("main", "{{ range .items }}{{ .Doubled }}:{{ .Scale 3 }} {{ end }}");
		List<Item> items = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			items.add(new Item(i));
		}
		this.data = Map.of("items", List.copyOf(items));
	}

	@Benchmark
	public String methodDispatchLoop() {
		return this.gotmpl.render("main", this.data);
	}

	/**
	 * POJO whose values come from Go-style no-arg and single-arg methods (not getters).
	 */
	public static final class Item {

		private final int n;

		public Item(int n) {
			this.n = n;
		}

		public int Doubled() {
			return this.n * 2;
		}

		public int Scale(int factor) {
			return this.n * factor;
		}

	}

}
