package org.alexmond.gotmpl4j.spring;

import java.util.Locale;
import java.util.Map;

import org.alexmond.gotmpl4j.Function;
import org.alexmond.gotmpl4j.FunctionProvider;
import org.alexmond.gotmpl4j.GoTemplate;
import org.alexmond.gotmpl4j.GoTemplateException;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Gotmpl4jAutoConfigurationTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(Gotmpl4jAutoConfiguration.class));

	@Test
	void autoConfiguresServiceAndRendersWithSprigFunction() {
		runner.run((context) -> {
			GoTemplateService service = context.getBean(GoTemplateService.class);
			// `.Name` proves data binding; `upper` proves sprig functions resolve via the
			// ServiceLoader through the starter.
			String out = service.render("t", "Hello {{ .Name | upper }}", Map.of("Name", "world"));
			assertEquals("Hello WORLD", out);
		});
	}

	@Test
	void rendersViewLoadedByNameWithReferencedTemplate() {
		runner.run((context) -> {
			GoTemplateService service = context.getBean(GoTemplateService.class);
			// `hello.gotmpl` lives under the default classpath:/templates/ location and
			// pulls in `layouts/footer.gotmpl` via {{ template }} — proving the loader
			// assembles the whole location into one set and resolves a view by name.
			String out = service.render("hello", Map.of("Name", "world", "Site", "gotmpl4j"));
			assertEquals("Hi WORLD from gotmpl4j", out);
		});
	}

	@Test
	void renderWrapsEngineFailuresInGoTemplateException() {
		runner.run((context) -> {
			GoTemplateService service = context.getBean(GoTemplateService.class);
			// An unterminated action is a parse error; the checked engine exception is
			// wrapped as an unchecked GoTemplateException for callers.
			assertThrows(GoTemplateException.class, () -> service.render("bad", "{{ .Name ", Map.of()));
		});
	}

	@Test
	void backsOffEntirelyWhenDisabled() {
		runner.withPropertyValues("gotmpl4j.enabled=false").run((context) -> {
			assertEquals(0, context.getBeanNamesForType(GoTemplateService.class).length);
			assertEquals(0, context.getBeanNamesForType(GoTemplateFactory.class).length);
		});
	}

	@Test
	void htmlModeEnablesContextualEscaping() {
		runner.withPropertyValues("gotmpl4j.mode=html").run((context) -> {
			GoTemplateService service = context.getBean(GoTemplateService.class);
			// In HTML mode the interpolation is escaped for its context; `<b>` becomes
			// `&lt;b&gt;` in element text.
			assertEquals("<p>&lt;b&gt;</p>", service.render("t", "<p>{{ .X }}</p>", Map.of("X", "<b>")));
		});
	}

	@Test
	void textModeIsTheDefaultAndDoesNotEscape() {
		runner.run((context) -> {
			GoTemplateService service = context.getBean(GoTemplateService.class);
			assertEquals("<p><b></p>", service.render("t", "<p>{{ .X }}</p>", Map.of("X", "<b>")));
		});
	}

	@Test
	void registersExtraFunctionMapBeanWithTheEngine() {
		runner.withUserConfiguration(ExtraFunctionsConfiguration.class).run((context) -> {
			GoTemplateService service = context.getBean(GoTemplateService.class);
			// A Map<String, Function> bean contributes named one-off functions, the way
			// the
			// factory collects extra functions (distinct from a whole FunctionProvider
			// bean).
			String out = service.render("t", "{{ greet .Name }}", Map.of("Name", "world"));
			assertEquals("Hello, world", out);
		});
	}

	@Test
	void registersFunctionProviderBeanWithTheEngine() {
		runner.withUserConfiguration(CustomFunctionsConfiguration.class).run((context) -> {
			GoTemplateService service = context.getBean(GoTemplateService.class);
			// `shout` is contributed by a Spring FunctionProvider bean, proving the
			// starter
			// bridges context function beans into the engine (alongside sprig's `upper`).
			String out = service.render("t", "{{ shout .Name }}", Map.of("Name", "world"));
			assertEquals("WORLD!", out);
		});
	}

	@Configuration
	static class ExtraFunctionsConfiguration {

		@Bean
		Map<String, Function> extraTemplateFunctions() {
			return Map.of("greet", (args) -> "Hello, " + args[0]);
		}

	}

	@Configuration
	static class CustomFunctionsConfiguration {

		@Bean
		FunctionProvider shoutProvider() {
			return new FunctionProvider() {
				@Override
				public Map<String, Function> getFunctions(GoTemplate template) {
					return Map.of("shout", (args) -> String.valueOf(args[0]).toUpperCase(Locale.ROOT) + "!");
				}

				@Override
				public int priority() {
					return 300;
				}
			};
		}

	}

}
