package org.alexmond.gotmpl4j.sprig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Census of the Sprig conformance fixtures — cases ported from Masterminds/sprig's own
 * {@code runt}/{@code runtv} test tables and rendered through the real Sprig funcmap for
 * ground truth (asserted by {@link SprigConformanceTest}).
 *
 * <p>
 * Derives the published "N Sprig conformance cases" headline from the committed fixtures
 * so the docs figure is re-derivable and asserts a floor so a silent drop in coverage
 * fails the build. Each non-empty fixture line is one asserted case.
 */
class ConformanceCensusTest {

	private static final String[] RUNT = { "strings", "numeric", "defaults", "list", "dict", "crypto", "semver", "url",
			"date" };

	private static final String[] RUNTV = { "strings", "numeric", "defaults", "list", "dict", "crypto", "date" };

	/** Baselines (floors); sum is the published Sprig total. */
	static final int RUNT_TOTAL = 283; // runt(tpl, expect) — no-data cases

	static final int RUNTV_TOTAL = 82; // runtv(tpl, expect, vars) — with-data cases

	static final int SPRIG_TOTAL = RUNT_TOTAL + RUNTV_TOTAL;

	@Test
	void sprigConformanceCensusMeetsFloor() {
		StringBuilder report = new StringBuilder("\nSprig conformance census (Masterminds/sprig runt + runtv):\n");
		int runt = 0;
		for (String c : RUNT) {
			runt += count("/conformance/sprig_" + c + "_cases.tsv");
		}
		int runtv = 0;
		for (String c : RUNTV) {
			runtv += count("/conformance/sprig_" + c + "_runtv_cases.tsv");
		}
		int divergences = count("/conformance/sprig_known_divergences.txt", true);
		report.append(String.format("  runt (no-data)   %4d  (>= %d)%n", runt, RUNT_TOTAL));
		report.append(String.format("  runtv (vars)     %4d  (>= %d)%n", runtv, RUNTV_TOTAL));
		report.append(String.format("  TOTAL            %4d%n", runt + runtv));
		report.append(String.format("  pinned divergences %2d%n", divergences));
		System.out.print(report);
		assertTrue(runt >= RUNT_TOTAL, "Sprig runt cases dropped: " + runt + " < " + RUNT_TOTAL);
		assertTrue(runtv >= RUNTV_TOTAL, "Sprig runtv cases dropped: " + runtv + " < " + RUNTV_TOTAL);
	}

	private static int count(String resource) {
		return count(resource, false);
	}

	/**
	 * Count non-empty lines; when {@code skipComments}, ignore {@code #}-prefixed lines.
	 */
	private static int count(String resource, boolean skipComments) {
		int n = 0;
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
				ConformanceCensusTest.class.getResourceAsStream(resource), StandardCharsets.UTF_8))) {
			assertNotNull(r, "missing fixture " + resource);
			String line;
			while ((line = r.readLine()) != null) {
				String t = line.strip();
				if (t.isEmpty() || (skipComments && t.startsWith("#"))) {
					continue;
				}
				n++;
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("cannot read " + resource, ex);
		}
		return n;
	}

}
