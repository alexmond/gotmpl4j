package org.alexmond.gotmpl4j;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Parity with Go's {@code text/template}: a variadic method on a value is callable with
 * any number of trailing arguments — Go's {@code evalCall} packs them into the variadic
 * slice via {@code reflect.Type#IsVariadic}. Method cases are excluded from the
 * conformance corpus, so this behaviour is guarded here rather than by the Go oracle.
 *
 * @see PipelineMethodCallTest
 */
class VarargsMethodCallTest {

	private String render(String body, Object data) throws IOException, TemplateException {
		GoTemplate template = new GoTemplate();
		template.parse("t", body);
		StringWriter writer = new StringWriter();
		template.execute("t", data, writer);
		return writer.toString();
	}

	private Map<String, Object> data() {
		return Map.of("ns", new Ns());
	}

	@Test
	void testPureVarargsWithManyArgs() throws IOException, TemplateException {
		assertEquals("3:a,b,c", render("{{ .ns.Slice \"a\" \"b\" \"c\" }}", data()));
	}

	@Test
	void testPureVarargsWithOneArg() throws IOException, TemplateException {
		assertEquals("1:a", render("{{ .ns.Slice \"a\" }}", data()));
	}

	@Test
	void testPureVarargsWithZeroArgs() throws IOException, TemplateException {
		assertEquals("0:", render("{{ .ns.Slice }}", data()));
	}

	@Test
	void testFixedPlusVarargs() throws IOException, TemplateException {
		assertEquals("a,b,c", render("{{ .ns.Join \",\" \"a\" \"b\" \"c\" }}", data()));
	}

	@Test
	void testFixedPlusZeroVarargs() throws IOException, TemplateException {
		assertEquals("", render("{{ .ns.Join \",\" }}", data()));
	}

	@Test
	void testNonVarargsOverloadTakesPrecedence() throws IOException, TemplateException {
		// One argument matches the fixed-arity Pick(Object) ahead of the variadic
		// overload.
		assertEquals("fixed:x", render("{{ .ns.Pick \"x\" }}", data()));
		assertEquals("varargs:2", render("{{ .ns.Pick \"x\" \"y\" }}", data()));
	}

	@Test
	void testPipedValueBecomesFinalVarargsArg() throws IOException, TemplateException {
		// Go threads the upstream pipeline value as the method's last argument, which
		// here
		// lands inside the varargs array: Slice("a", "b", "c").
		assertEquals("3:a,b,c", render("{{ \"c\" | .ns.Slice \"a\" \"b\" }}", data()));
	}

	/** Stand-in namespace object exposing variadic (and overloaded) methods. */
	public static class Ns {

		public String Slice(Object... items) {
			return items.length + ":" + Arrays.stream(items).map(String::valueOf).collect(Collectors.joining(","));
		}

		public String Join(Object sep, Object... parts) {
			return Arrays.stream(parts).map(String::valueOf).collect(Collectors.joining(String.valueOf(sep)));
		}

		public String Pick(Object only) {
			return "fixed:" + only;
		}

		public String Pick(Object... many) {
			return "varargs:" + many.length;
		}

	}

}
