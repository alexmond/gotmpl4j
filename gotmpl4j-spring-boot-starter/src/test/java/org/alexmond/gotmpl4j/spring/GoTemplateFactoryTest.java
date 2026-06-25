package org.alexmond.gotmpl4j.spring;

import java.util.List;
import java.util.Map;

import org.alexmond.gotmpl4j.Function;
import org.alexmond.gotmpl4j.FunctionProvider;
import org.alexmond.gotmpl4j.GoTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit coverage for the factory that builds configured engines for the starter (issue
 * #28).
 */
class GoTemplateFactoryTest {

	private static String render(GoTemplate t, String src, Map<String, Object> data) {
		t.parse("t", src);
		return t.render("t", data);
	}

	@Test
	void defaultFactoryDiscoversSprigFromTheClasspath() {
		GoTemplate t = new GoTemplateFactory().create();
		assertEquals("WORLD", render(t, "{{ .Name | upper }}", Map.of("Name", "world")));
	}

	@Test
	void appliesExtraFunctions() {
		Map<String, Function> extras = Map.of("shout", (args) -> args[0] + "!");
		GoTemplate t = new GoTemplateFactory(List.of(), extras).create();
		assertEquals("hi!", render(t, "{{ shout .X }}", Map.of("X", "hi")));
	}

	@Test
	void appliesProviderBeans() {
		FunctionProvider provider = (template) -> Map.of("twice", (args) -> "" + args[0] + args[0]);
		GoTemplate t = new GoTemplateFactory(List.of(provider), Map.of()).create();
		assertEquals("yoyo", render(t, "{{ twice .X }}", Map.of("X", "yo")));
	}

	@Test
	void extraFunctionsOverrideProviders() {
		FunctionProvider provider = (template) -> Map.of("name", (args) -> "from-provider");
		Map<String, Function> extras = Map.of("name", (args) -> "from-extras");
		GoTemplate t = new GoTemplateFactory(List.of(provider), extras).create();
		assertEquals("from-extras", render(t, "{{ name }}", Map.of()));
	}

}
