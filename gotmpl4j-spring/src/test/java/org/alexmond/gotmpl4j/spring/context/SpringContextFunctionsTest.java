package org.alexmond.gotmpl4j.spring.context;

import java.util.Map;

import org.alexmond.gotmpl4j.GoTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = SpringContextFunctionsTest.TestApp.class,
		properties = { "gotmpl4j.spring.expose-beans=true", "app.title=gotmpl4j" })
class SpringContextFunctionsTest {

	@Autowired
	private SpringContextFunctions functions;

	private String render(String template) {
		GoTemplate t = GoTemplate.builder().withProvider(this.functions).build();
		t.parse("t", template);
		return t.render("t", Map.of());
	}

	@Test
	void msgResolvesMessageSourceWithArgs() {
		assertEquals("Hello, World!", render("{{ msg \"greet.hello\" \"World\" }}"));
	}

	@Test
	void msgFallsBackToTheCodeWhenMissing() {
		assertEquals("no.such.key", render("{{ msg \"no.such.key\" }}"));
	}

	@Test
	void envReadsEnvironmentPropertyWithOptionalDefault() {
		assertEquals("gotmpl4j", render("{{ env \"app.title\" }}"));
		assertEquals("fallback", render("{{ env \"missing.prop\" \"fallback\" }}"));
	}

	@Test
	void beanResolvesFromContextAndAccessesItsProperty() {
		assertEquals("hi there", render("{{ $g := bean \"greeter\" }}{{ $g.Greeting }}"));
		// Parenthesized form too — the syntax the docs advertise.
		assertEquals("hi there", render("{{ (bean \"greeter\").Greeting }}"));
	}

	@Configuration
	@Import(GoTemplateSpringAutoConfiguration.class)
	static class TestApp {

		@Bean
		MessageSource messageSource() {
			ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
			ms.setBasename("messages");
			ms.setDefaultEncoding("UTF-8");
			return ms;
		}

		@Bean
		Greeter greeter() {
			return new Greeter();
		}

	}

	public static class Greeter {

		public String getGreeting() {
			return "hi there";
		}

	}

}
