package org.alexmond.gotmpl4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Census of the core conformance fixtures — the cases ported from Go's own
 * {@code text/template} and {@code html/template} test tables and rendered through the
 * real Go engine for ground truth (see the suites in this package and
 * {@code html}/{@code parse}).
 *
 * <p>
 * It derives the published "N conformance cases" headline from the committed fixtures so
 * the number in the docs is re-derivable, never hand-counted, and it asserts a floor so a
 * silent <em>drop</em> in coverage fails the build. Each non-empty fixture line is one
 * asserted case, matching how every {@code *ConformanceTest} loads its table.
 */
class ConformanceCensusTest {

	/**
	 * Per-suite fixture → the number of ported cases it must carry. Sum is the published
	 * core total. Adding cases is fine (the assertion is a floor); a regression that
	 * drops cases fails. Bump a baseline here (and the docs) when a suite intentionally
	 * grows.
	 */
	private static final Map<String, Integer> CORE_BASELINE = baseline();

	static final int CORE_TOTAL = CORE_BASELINE.values().stream().mapToInt(Integer::intValue).sum();

	private static Map<String, Integer> baseline() {
		Map<String, Integer> m = new LinkedHashMap<>();
		m.put("gotmpl_exec", 28); // text/template exec_test execTests
		m.put("text", 31); // text/template Execute text cases
		m.put("tval", 223); // text/template field/method value reflection
		m.put("lex", 40); // text/template/parse lexer token tables
		m.put("escaper", 64); // html/template escaper
		m.put("escape_text", 153); // html/template escape-context text
		m.put("escape_errors", 45); // html/template escaper error cases
		m.put("css_decode", 18); // html/template CSS value decoding
		return m;
	}

	@Test
	void coreConformanceCensusMeetsFloor() {
		StringBuilder report = new StringBuilder("\nCore conformance census (Go text/template + html/template):\n");
		int total = 0;
		for (Map.Entry<String, Integer> e : CORE_BASELINE.entrySet()) {
			int actual = count("/conformance/" + e.getKey() + "_cases.tsv");
			total += actual;
			report.append(String.format("  %-16s %4d  (>= %d)%n", e.getKey(), actual, e.getValue()));
			assertTrue(actual >= e.getValue(),
					() -> e.getKey() + " conformance cases dropped: " + actual + " < baseline " + e.getValue());
		}
		report.append(String.format("  %-16s %4d%n", "TOTAL", total));
		System.out.print(report);
		assertTrue(total >= CORE_TOTAL, "core conformance total dropped below " + CORE_TOTAL);
	}

	private static int count(String resource) {
		int n = 0;
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
				ConformanceCensusTest.class.getResourceAsStream(resource), StandardCharsets.UTF_8))) {
			assertNotNull(r, "missing fixture " + resource);
			String line;
			while ((line = r.readLine()) != null) {
				if (!line.isEmpty()) {
					n++;
				}
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("cannot read " + resource, ex);
		}
		return n;
	}

}
