package org.alexmond.gotmpl4j;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the method/field dispatch cache (#117) semantics that are easy to regress when
 * the per-access {@code getMethods()} scan is replaced by a cached, name-grouped index: a
 * map-like object that also exposes helper methods must resolve its keys first, resolve
 * its own helper methods, and never invoke an inherited JDK method (e.g. {@code size}) as
 * a template helper. Repeated renders exercise the warm-cache path.
 */
class DispatchCacheTest {

	@Test
	void mapKeyMethodHelperAndJdkMethodResolutionThroughCache() {
		GoTemplate t = new GoTemplate();
		// .key -> map lookup ; .Helper -> the class's own no-arg method ;
		// .size -> must NOT invoke Map.size() (JDK method), so it renders as missing.
		t.parse("t", "{{ .key }}|{{ .Helper }}|{{ .size }}");

		FilesLike data = new FilesLike();
		data.put("key", "V");

		// Two renders: the second hits the warm cache.
		assertEquals("V|helper|<no value>", t.render(data));
		assertEquals("V|helper|<no value>", t.render(data));
	}

	/** A map that also exposes a helper method — the Helm {@code .Files} shape. */
	public static final class FilesLike extends HashMap<String, Object> {

		public String Helper() {
			return "helper";
		}

	}

}
