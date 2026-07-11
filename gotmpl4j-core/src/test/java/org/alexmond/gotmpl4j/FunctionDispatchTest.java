package org.alexmond.gotmpl4j;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the two error paths of the single-lookup function dispatch (#116): a function
 * name mapped to {@code null} reports "call of null", an unknown name reports "not a
 * defined function", and a null-mapped identifier used as a bare argument falls through
 * to its string form (matching the pre-collapse behaviour).
 */
class FunctionDispatchTest {

	@Test
	void nullMappedFunctionReportsCallOfNull() {
		Map<String, Function> functions = new HashMap<>();
		functions.put("nullfn", null);
		GoTemplate t = GoTemplate.builder().withFunctions(functions).build();
		t.parse("t", "{{ nullfn }}");

		TemplateExecutionException ex = assertThrows(TemplateExecutionException.class, () -> t.render(null));
		assertTrue(ex.getMessage().contains("call of null for nullfn"), ex.getMessage());
	}

	@Test
	void functionRemovedAfterParseReportsNotDefined() {
		// Unknown names are rejected at parse time, so the runtime "not a defined
		// function" path is the safety net for a function removed from the live map after
		// parse (the integrator getFunctions() surface that jhelm mutates).
		Map<String, Function> functions = new HashMap<>();
		functions.put("temp", (args) -> "x");
		GoTemplate t = GoTemplate.builder().withFunctions(functions).build();
		t.parse("t", "{{ temp }}");
		t.getFunctions().remove("temp");

		TemplateExecutionException ex = assertThrows(TemplateExecutionException.class, () -> t.render(null));
		assertTrue(ex.getMessage().contains("not a defined function"), ex.getMessage());
	}

	@Test
	void nullMappedIdentifierArgumentFallsThroughToString() {
		Map<String, Function> functions = new HashMap<>();
		functions.put("nullid", null);
		GoTemplate t = GoTemplate.builder().withFunctions(functions).build();
		// `nullid` as a bare argument to print: not an invokable function, so it renders
		// as its identifier string.
		t.parse("t", "{{ print nullid }}");

		assertEquals("nullid", t.render(null));
	}

}
