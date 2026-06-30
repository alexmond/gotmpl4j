package org.alexmond.gotmpl4j.samples.functions;

import org.alexmond.gotmpl4j.GoTemplate;

/**
 * Demonstrates the three ways custom functions interact with the engine. Run it with:
 *
 * <pre>./mvnw -q -pl gotmpl4j-samples/custom-functions -am compile exec:java</pre>
 */
public final class CustomFunctionsSample {

	private CustomFunctionsSample() {
	}

	public static void main(String[] args) {
		autoDiscovered();
		priorityOverride();
		explicitOnly();
	}

	/**
	 * {@code new GoTemplate()} auto-discovers every {@code FunctionProvider} on the
	 * classpath, so our {@code greet}/{@code mask} sit alongside all of Sprig's functions
	 * with no wiring.
	 */
	private static void autoDiscovered() {
		String tmpl = "{{ greet \"Ada\" }} card {{ \"4111111111111234\" | mask }}";
		print("auto-discovered", new GoTemplate().parse(tmpl).render(null));
		// Hello, Ada! card ************1234
	}

	/**
	 * Both Sprig (priority 100) and our provider (priority 300) define {@code quote}; the
	 * higher priority wins, so the pipe below uses our single-quote version.
	 */
	private static void priorityOverride() {
		print("priority override", new GoTemplate().parse("{{ \"x\" | quote }}").render(null));
		// 'x' (Sprig's quote would have produced "x")
	}

	/**
	 * Skip ServiceLoader entirely and register the provider by hand — handy when you want
	 * only your functions (no Sprig) or are assembling the engine programmatically.
	 */
	private static void explicitOnly() {
		GoTemplate t = GoTemplate.builder().noAutoDiscovery().withProvider(new SampleFunctionProvider()).build();
		print("explicit only", t.parse("{{ greet \"Grace\" }}").render(null));
		// Hello, Grace!
	}

	private static void print(String label, String rendered) {
		System.out.printf("%-18s -> %s%n", label, rendered);
	}

}
