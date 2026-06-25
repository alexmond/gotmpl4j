package org.alexmond.gotmpl4j.spring;

import java.util.Map;

import org.alexmond.gotmpl4j.GoTemplate;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.DefaultResourceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Coverage for the loader: prefix/suffix resolution, partial includes, and charset (issue
 * #28).
 */
class GoTemplateLoaderTest {

	private static GoTemplateLoader loaderFor(Gotmpl4jProperties properties) {
		return new GoTemplateLoader(new DefaultResourceLoader(), properties, new GoTemplateFactory());
	}

	@Test
	void loadsLocationAndResolvesNamesWithoutSuffix() {
		GoTemplate set = loaderFor(new Gotmpl4jProperties()).load();
		// hello.gotmpl pulls in layouts/footer.gotmpl via {{ template }} — proving the
		// whole
		// location is assembled into one set, named by suffix-stripped relative path.
		assertEquals("Hi WORLD from gotmpl4j", set.render("hello", Map.of("Name", "world", "Site", "gotmpl4j")));
		assertEquals("gotmpl4j", set.render("layouts/footer", Map.of("Site", "gotmpl4j")));
	}

	@Test
	void prefixScopesTheLoadedSet() {
		Gotmpl4jProperties properties = new Gotmpl4jProperties();
		properties.setPrefix("classpath:/templates/layouts/");
		GoTemplate set = loaderFor(properties).load();
		// Scoped to the subdirectory, the partial is now named relative to that prefix.
		assertEquals("gotmpl4j", set.render("footer", Map.of("Site", "gotmpl4j")));
	}

	@Test
	void decodesConfiguredCharset() {
		GoTemplate set = loaderFor(new Gotmpl4jProperties()).load();
		assertEquals("Café WORLD ☕", set.render("unicode", Map.of("Name", "world")));
	}

}
