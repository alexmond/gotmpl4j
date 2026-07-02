package org.alexmond.gotmpl4j;

import java.io.StringWriter;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The {@code missingkey} option (issue #37) controls how a nil/absent value renders: Go's
 * {@code <no value>} by default, an empty string under {@code missingkey=zero} (Helm
 * semantics), or an error under {@code missingkey=error}.
 */
class MissingKeyTest {

	private static String render(GoTemplate t, String tmpl) throws Exception {
		t.parse("t", tmpl);
		StringWriter w = new StringWriter();
		t.execute("t", Map.of(), w);
		return w.toString();
	}

	@Test
	void defaultRendersNoValue() throws Exception {
		assertEquals("x[<no value>]", render(new GoTemplate(), "x[{{ .missing }}]"));
	}

	@Test
	void zeroRendersEmpty() throws Exception {
		assertEquals("x[]", render(new GoTemplate().option("missingkey=zero"), "x[{{ .missing }}]"));
	}

	@Test
	void invalidIsAliasForDefault() throws Exception {
		assertEquals("<no value>", render(new GoTemplate().option("missingkey=invalid"), "{{ .missing }}"));
	}

	@Test
	void errorFailsExecution() {
		GoTemplate t = new GoTemplate().option("missingkey=error");
		assertThrows(TemplateExecutionException.class, () -> render(t, "{{ .missing }}"));
	}

	@Test
	void unrecognizedOptionThrows() {
		GoTemplate t = new GoTemplate();
		assertThrows(IllegalArgumentException.class, () -> t.option("missingkey=bogus"));
		assertThrows(IllegalArgumentException.class, () -> t.option("colors=on"));
	}

	@Test
	void builderOptionSetsMissingKey() throws Exception {
		assertEquals("x[]", render(GoTemplate.builder().option("missingkey=zero").build(), "x[{{ .missing }}]"));
	}

	@Test
	void builderRejectsUnknownOptionAtBuild() {
		var b = GoTemplate.builder().option("missingkey=bogus");
		assertThrows(IllegalArgumentException.class, b::build);
	}

}
