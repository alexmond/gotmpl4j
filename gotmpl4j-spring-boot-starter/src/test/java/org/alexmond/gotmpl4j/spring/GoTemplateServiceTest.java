package org.alexmond.gotmpl4j.spring;

import java.util.Map;

import org.alexmond.gotmpl4j.GoTemplate;
import org.alexmond.gotmpl4j.GoTemplateException;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.DefaultResourceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GoTemplateServiceTest {

	@Test
	void cacheReusesTheCompiledSetAcrossRenders() {
		CountingLoader loader = new CountingLoader();
		GoTemplateService service = new GoTemplateService(loader, new GoTemplateFactory(), true);

		service.render("hello", Map.of("Name", "a", "Site", "s"));
		service.render("hello", Map.of("Name", "b", "Site", "s"));

		assertEquals(1, loader.loads);
	}

	@Test
	void cacheDisabledRecompilesOnEveryRender() {
		CountingLoader loader = new CountingLoader();
		GoTemplateService service = new GoTemplateService(loader, new GoTemplateFactory(), false);

		service.render("hello", Map.of("Name", "a", "Site", "s"));
		service.render("hello", Map.of("Name", "b", "Site", "s"));

		assertEquals(2, loader.loads);
	}

	@Test
	void inlineRenderResolvesData() {
		GoTemplateService service = new GoTemplateService();

		assertEquals("Hello world", service.render("t", "Hello {{ .Name }}", Map.of("Name", "world")));
	}

	@Test
	void inlineRenderWrapsParseFailure() {
		GoTemplateService service = new GoTemplateService();

		assertThrows(GoTemplateException.class, () -> service.render("bad", "{{ .Name ", Map.of()));
	}

	@Test
	void viewRenderWithoutLoaderIsRejected() {
		GoTemplateService service = new GoTemplateService();

		assertThrows(GoTemplateException.class, () -> service.render("view", Map.of()));
	}

	/** A loader that counts how many times the template set is compiled. */
	private static final class CountingLoader extends GoTemplateLoader {

		private int loads;

		CountingLoader() {
			super(new DefaultResourceLoader(), new Gotmpl4jProperties(), new GoTemplateFactory());
		}

		@Override
		public GoTemplate load() {
			this.loads++;
			return super.load();
		}

	}

}
