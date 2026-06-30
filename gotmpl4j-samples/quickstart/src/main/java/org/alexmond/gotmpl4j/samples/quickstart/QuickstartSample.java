package org.alexmond.gotmpl4j.samples.quickstart;

import java.util.List;
import java.util.Map;

import org.alexmond.gotmpl4j.GoTemplate;

/**
 * The smallest end-to-end tour of the gotmpl4j core API — no framework, just
 * {@link GoTemplate}. Run it with:
 *
 * <pre>./mvnw -q -pl gotmpl4j-samples/quickstart -am compile exec:java</pre>
 *
 * Each section is a self-contained example you can copy into your own code.
 */
public final class QuickstartSample {

	private QuickstartSample() {
	}

	public static void main(String[] args) {
		oneLiner();
		namedTemplateWithMap();
		beanModel();
		sprigPipeline();
		htmlAutoEscaping();
		goBuiltinsOnly();
	}

	/**
	 * Parse an unnamed template and render it against a single root value ({@code .}).
	 */
	private static void oneLiner() {
		String out = new GoTemplate().parse("Hello, {{ . }}!").render("world");
		print("one-liner", out); // Hello, world!
	}

	/**
	 * Name a template, then render a {@link Map} model addressed Go-style as
	 * {@code .Key}.
	 */
	private static void namedTemplateWithMap() {
		GoTemplate t = new GoTemplate().parse("greeting", "{{ .Greeting }}, {{ .Name }}!");
		String out = t.render("greeting", Map.of("Greeting", "Hi", "Name", "Ada"));
		print("named + map", out); // Hi, Ada!
	}

	/** Go's {@code .Field} maps to the JavaBean getter {@code getField()} on any POJO. */
	private static void beanModel() {
		String tmpl = "{{ .Name }} listens on :{{ .Port }} (tls={{ .Tls }})";
		String out = new GoTemplate().parse(tmpl).render(new Server("api", 8443, true));
		print("bean model", out); // api listens on :8443 (tls=true)
	}

	/** With gotmpl4j-sprig on the classpath, its functions auto-register — no wiring. */
	private static void sprigPipeline() {
		String tmpl = "{{ .name | upper | trunc 3 | repeat 2 | quote }} "
				+ "/ {{ list 1 2 3 | join \"-\" }} / {{ .missing | default \"n/a\" }}";
		String out = new GoTemplate().parse(tmpl).render(Map.of("name", "gotmpl4j"));
		print("sprig pipeline", out); // "GOTGOT" / 1-2-3 / n/a
	}

	/** Opt into contextual {@code html/template} auto-escaping via the builder. */
	private static void htmlAutoEscaping() {
		GoTemplate t = GoTemplate.builder().htmlEscaping().build().parse("<p>{{ . }}</p>");
		String out = t.render("<script>alert(1)</script>");
		print("html escaping", out); // <p>&lt;script&gt;alert(1)&lt;/script&gt;</p>
	}

	/**
	 * Build an engine with auto-discovery off — only Go's built-in functions are
	 * available.
	 */
	private static void goBuiltinsOnly() {
		GoTemplate t = GoTemplate.builder().noAutoDiscovery().build();
		String out = t.parse("{{ len . }} items: {{ index . 0 }}").render(List.of("a", "b"));
		print("go builtins only", out); // 2 items: a
	}

	private static void print(String label, String rendered) {
		System.out.printf("%-18s -> %s%n", label, rendered);
	}

	/**
	 * A plain getter bean — gotmpl4j resolves {@code .Name}/{@code .Port}/{@code .Tls}.
	 */
	public static final class Server {

		private final String name;

		private final int port;

		private final boolean tls;

		public Server(String name, int port, boolean tls) {
			this.name = name;
			this.port = port;
			this.tls = tls;
		}

		public String getName() {
			return this.name;
		}

		public int getPort() {
			return this.port;
		}

		public boolean isTls() {
			return this.tls;
		}

	}

}
