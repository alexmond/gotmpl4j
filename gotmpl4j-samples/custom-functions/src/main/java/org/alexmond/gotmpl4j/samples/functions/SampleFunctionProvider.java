package org.alexmond.gotmpl4j.samples.functions;

import java.util.Map;

import org.alexmond.gotmpl4j.Function;
import org.alexmond.gotmpl4j.FunctionProvider;
import org.alexmond.gotmpl4j.GoTemplate;

/**
 * A custom {@link FunctionProvider} that contributes application-specific template
 * functions.
 *
 * <p>
 * Registered for ServiceLoader auto-discovery via
 * {@code META-INF/services/org.alexmond.gotmpl4j.FunctionProvider}, so it is picked up
 * automatically by {@code new GoTemplate()} whenever this jar is on the classpath —
 * exactly the way gotmpl4j-sprig registers itself.
 *
 * <p>
 * {@link #priority()} returns 300 (Go builtins are 0, Sprig is 100). On a name clash the
 * highest priority wins, so the {@code greet} below is new while {@code quote}
 * deliberately overrides Sprig's {@code quote} to demonstrate the override mechanism.
 */
public class SampleFunctionProvider implements FunctionProvider {

	@Override
	public String name() {
		return "sample";
	}

	@Override
	public int priority() {
		return 300;
	}

	@Override
	public Map<String, Function> getFunctions(GoTemplate template) {
		return Map.of("greet", greet(), "mask", mask(), "quote", singleQuote());
	}

	/** {@code {{ greet "Ada" }}} -> {@code Hello, Ada!} */
	private static Function greet() {
		return (args) -> "Hello, " + str(args, 0) + "!";
	}

	/** {@code {{ "4111111111111234" | mask }}} -> {@code ************1234} */
	private static Function mask() {
		return (args) -> {
			String s = str(args, 0);
			int keep = Math.min(4, s.length());
			return "*".repeat(s.length() - keep) + s.substring(s.length() - keep);
		};
	}

	/**
	 * Overrides Sprig's {@code quote} (single quotes instead of double) purely to show
	 * that a higher-priority provider wins a name clash — not a recommended thing to do
	 * in real code.
	 */
	private static Function singleQuote() {
		return (args) -> "'" + str(args, 0) + "'";
	}

	private static String str(Object[] args, int i) {
		return (args.length > i && args[i] != null) ? String.valueOf(args[i]) : "";
	}

}
