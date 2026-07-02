package org.alexmond.gotmpl4j;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Go short-circuits {@code and}/{@code or}: operands past the deciding one are never
 * evaluated, so a later operand that would error is not reached (issue #11). Expectations
 * verified against go1.23.4 {@code text/template}.
 */
class AndOrShortCircuitTest {

	private static String render(String tmpl, Object data) throws Exception {
		GoTemplate t = new GoTemplate();
		t.parse("t", tmpl);
		StringWriter w = new StringWriter();
		t.execute("t", data, w);
		return w.toString();
	}

	@Test
	void andShortCircuitsOnFalsy() throws Exception {
		// Go => "false"; the second operand (out-of-range index) is never evaluated.
		assertEquals("false", render("{{and false (index .s 99)}}", Map.of("s", List.of())));
	}

	@Test
	void orShortCircuitsOnTruthy() throws Exception {
		assertEquals("true", render("{{or true (index .s 99)}}", Map.of("s", List.of())));
	}

	@Test
	void andEvaluatesDecidingOperand() {
		// First operand truthy -> Go evaluates the second, which errors.
		Map<String, Object> data = Map.of("s", List.of());
		assertThrows(TemplateExecutionException.class, () -> render("{{and true (index .s 99)}}", data));
	}

	@Test
	void andReturnsLastWhenAllTruthy() throws Exception {
		assertEquals("3", render("{{and 1 2 3}}", Map.of()));
	}

	@Test
	void orReturnsFirstTruthy() throws Exception {
		assertEquals("x", render("{{or 0 \"\" \"x\"}}", Map.of()));
	}

	@Test
	void andReturnsFirstFalsy() throws Exception {
		assertEquals("", render("{{and \"a\" \"\" \"c\"}}", Map.of()));
	}

}
