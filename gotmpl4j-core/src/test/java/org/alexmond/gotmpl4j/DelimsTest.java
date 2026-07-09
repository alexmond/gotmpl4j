package org.alexmond.gotmpl4j;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Parity with Go's {@code text/template} {@code TestDelims} (exec_test.go): custom action
 * delimiters via {@link GoTemplate#delims(String, String)}. Each pair renders an action,
 * a comment, and a string literal containing the left delimiter — so the expected output
 * is {@code "Hello, world" + trueLeft}, exercising delimiter handling in all three
 * positions at once. Method/delimiter cases are outside the conformance corpus, so this
 * hand-written parity test is the guard.
 */
class DelimsTest {

	private static final String HELLO = "Hello, world";

	// Go's delimPairs (exec_test.go): default, same-as-default, distinct, same,
	// multibyte.
	private static final String[][] PAIRS = { { "", "" }, { "{{", "}}" }, { "<<", ">>" }, { "|", "|" },
			{ "(日)", "(本)" }, };

	private String render(String left, String right) throws IOException, TemplateException {
		String trueLeft = left.isEmpty() ? "{{" : left;
		String trueRight = right.isEmpty() ? "}}" : right;
		// {{.Str}}{{/*comment*/}}{{"{{"}} (with the configured delimiters)
		String text = trueLeft + ".Str" + trueRight + trueLeft + "/*comment*/" + trueRight + trueLeft + "\"" + trueLeft
				+ "\"" + trueRight;
		GoTemplate template = new GoTemplate().delims(left, right);
		template.parse("delims", text);
		StringWriter writer = new StringWriter();
		template.execute("delims", Map.of("Str", HELLO), writer);
		return writer.toString();
	}

	@Test
	void matchesGoDelimsAcrossPairs() throws IOException, TemplateException {
		for (String[] pair : PAIRS) {
			String trueLeft = pair[0].isEmpty() ? "{{" : pair[0];
			assertEquals(HELLO + trueLeft, render(pair[0], pair[1]), "delims [" + pair[0] + "] [" + pair[1] + "]");
		}
	}

	@Test
	void builderDelims() throws IOException, TemplateException {
		GoTemplate template = GoTemplate.builder().delims("[[", "]]").build();
		template.parse("t", "[[ .name ]]!");
		StringWriter writer = new StringWriter();
		template.execute("t", Map.of("name", "world"), writer);
		assertEquals("world!", writer.toString());
	}

	@Test
	void defaultDelimsUnchangedWhenUnset() throws IOException, TemplateException {
		GoTemplate template = new GoTemplate();
		template.parse("t", "{{ .name }}");
		StringWriter writer = new StringWriter();
		template.execute("t", Map.of("name", "x"), writer);
		assertEquals("x", writer.toString());
	}

	@Test
	void emptyDelimsResetToDefault() throws IOException, TemplateException {
		// Go: Delims("","") restores the defaults, overriding an earlier custom pair.
		GoTemplate template = new GoTemplate().delims("[[", "]]").delims("", "");
		template.parse("t", "{{ .name }}");
		StringWriter writer = new StringWriter();
		template.execute("t", Map.of("name", "y"), writer);
		assertEquals("y", writer.toString());
	}

	@Test
	void trimMarkersWorkWithCustomDelims() throws IOException, TemplateException {
		// Trim markers apply relative to the configured delimiters (Go parity).
		GoTemplate template = new GoTemplate().delims("<<", ">>");
		template.parse("t", "a  <<- .name ->>  b");
		StringWriter writer = new StringWriter();
		template.execute("t", Map.of("name", "X"), writer);
		assertEquals("aXb", writer.toString());
	}

}
