package org.alexmond.gotmpl4j.concurrency;

import java.util.List;
import java.util.Map;

import org.alexmond.gotmpl4j.GoTemplate;
import org.alexmond.gotmpl4j.concurrency.ConcurrencyStress.Case;
import org.junit.jupiter.api.Test;

/**
 * Concurrency-correctness guard for rendering one shared, compiled {@link GoTemplate}
 * from many threads — the exact production shape (the Spring starter caches one compiled
 * engine and renders it per request). Each scenario feeds every thread <em>distinct</em>
 * data and asserts each thread gets <em>its own</em> output, so a shared-state
 * optimization that crosses variables or corrupts scope state fails here.
 *
 * <p>
 * These are deliberately the shapes the perf backlog touches, so they act as a
 * pre-existing guard before those fixes land:
 * <ul>
 * <li>variable-binding {@code range} — scope frames (#115) and undo journal.
 * <li>{@code include}/{@code template} — the save/restore of the caller's variables
 * (#114).
 * <li>POJO getter + no-arg method — the reflective method-dispatch path (#117).
 * </ul>
 */
class SharedTemplateConcurrencyTest {

	@Test
	void variableBindingRangeIsRaceFree() {
		GoTemplate t = new GoTemplate();
		t.parse("main", "{{ $id := .id }}[{{ range .items }}{{ $id }}.{{ . }} {{ end }}]");
		ConcurrencyStress.on(t, "main").run((id) -> {
			Map<String, Object> data = Map.of("id", id, "items", List.of(10, 20, 30));
			return new Case(data, "[" + id + ".10 " + id + ".20 " + id + ".30 ]");
		});
	}

	@Test
	void includeCompositionPreservesCallerScope() {
		GoTemplate t = new GoTemplate();
		// The template call must not corrupt the caller's $id — that is exactly the
		// variables/scope save+restore #114 rewrites.
		t.parse("main", "{{ $id := .id }}{{ template \"row\" . }}={{ $id }}");
		t.parse("row", "{{ .id }}#");
		ConcurrencyStress.on(t, "main").run((id) -> new Case(Map.of("id", id), id + "#=" + id));
	}

	@Test
	void pojoMethodDispatchIsRaceFree() {
		GoTemplate t = new GoTemplate();
		// .Symbol -> getSymbol() (accessor cache); .Doubled -> Doubled() no-arg method
		// (the getMethods() dispatch path #117 will cache).
		t.parse("main", "{{ .Symbol }}/{{ .Doubled }}");
		ConcurrencyStress.on(t, "main").run((id) -> {
			Model model = new Model("S" + id, id);
			return new Case(model, "S" + id + "/" + (id * 2));
		});
	}

	/** POJO with a JavaBean getter and a Go-style no-arg method. */
	public static final class Model {

		private final String symbol;

		private final int n;

		Model(String symbol, int n) {
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
