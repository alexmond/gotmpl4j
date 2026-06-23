package org.alexmond.gotmpl4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests how null/nil values render in templates. A bare action over a nil/absent value
 * renders Go's {@code <no value>} marker (text/template parity, issue #13), while the
 * {@code print}/{@code printf}/{@code println} functions follow {@code fmt} semantics
 * ({@code <nil>}). Java's {@code String.valueOf(null)} ("null") must never appear.
 */
class NullHandlingTest {

	@Test
	void testNilActionRendersNoValueMarker() throws Exception {
		// Go text/template renders a nil action value as the literal "<no value>".
		Map<String, Object> data = new HashMap<>();
		data.put("name", null);
		assertEquals("prefix-<no value>suffix", render("prefix-{{ .name }}suffix", data));
	}

	@Test
	void testNullInPrintMatchesGoFmtSprint() throws Exception {
		// Go's fmt.Sprint(nil) returns "<nil>", not ""
		Map<String, Object> data = new HashMap<>();
		data.put("val", null);
		assertEquals("hello<nil>", render("{{ print \"hello\" .val }}", data));
	}

	@Test
	void testNullInPrintfMatchesGoFmtMarker() throws Exception {
		// Go's fmt prints `%!s(<nil>)` for a nil %s argument (not an empty string).
		Map<String, Object> data = new HashMap<>();
		data.put("name", null);
		assertEquals("release-%!s(<nil>)", render("{{ printf \"%s-%s\" \"release\" .name }}", data));
	}

	@Test
	void testNullInPrintlnMatchesGoFmtSprintln() throws Exception {
		// Go's fmt.Sprintln("hello", nil) returns "hello <nil>\n"
		Map<String, Object> data = new HashMap<>();
		data.put("val", null);
		assertEquals("hello <nil>\n", render("{{ println \"hello\" .val }}", data));
	}

	@Test
	void testMissingFieldRendersNoValueMarker() throws Exception {
		Map<String, Object> data = new HashMap<>();
		data.put("config", new HashMap<>());
		// A missing map key renders "<no value>" in Go text/template.
		assertEquals("value=<no value>", render("value={{ .config.name }}", data));
	}

	private String render(String template, Map<String, Object> data) throws TemplateException, IOException {
		GoTemplate tmpl = new GoTemplate();
		tmpl.parse("test", template);
		StringWriter writer = new StringWriter();
		tmpl.execute(data, writer);
		return writer.toString();
	}

}
