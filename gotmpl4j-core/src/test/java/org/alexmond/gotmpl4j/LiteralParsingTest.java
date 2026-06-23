package org.alexmond.gotmpl4j;

import java.io.StringWriter;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Character and integer literal parsing, verified against go1.23.4 (issue #14). A rune
 * literal yields its code point; an integer literal that overflows 64 bits is rejected
 * rather than silently widened to a lossy float.
 */
class LiteralParsingTest {

	private static String render(String tmpl) throws Exception {
		GoTemplate t = new GoTemplate();
		t.parse("t", tmpl);
		StringWriter w = new StringWriter();
		t.execute("t", Map.of(), w);
		return w.toString();
	}

	@Test
	void charEscapesYieldCodePoints() throws Exception {
		assertEquals("10", render("{{ '\\n' }}"));
		assertEquals("65", render("{{ '\\x41' }}"));
		assertEquals("65", render("{{ '\\101' }}"));
		assertEquals("128512", render("{{ '\\U0001F600' }}"));
	}

	@Test
	void plainRuneYieldsCodePoint() throws Exception {
		assertEquals("97", render("{{ 'a' }}"));
	}

	@Test
	void integerOverflowIsRejected() {
		// Go rejects a too-large integer literal; gotmpl4j must not demote it to a
		// double.
		assertThrows(TemplateParseException.class, () -> render("{{ 0xFFFFFFFFFFFFFFFF }}"));
		assertThrows(TemplateParseException.class, () -> render("{{ 123456789012345678901234567890 }}"));
	}

	@Test
	void floatLiteralsStillParse() throws Exception {
		assertEquals("3.14", render("{{ 3.14 }}"));
		assertEquals("100000", render("{{ 1e5 }}"));
	}

}
