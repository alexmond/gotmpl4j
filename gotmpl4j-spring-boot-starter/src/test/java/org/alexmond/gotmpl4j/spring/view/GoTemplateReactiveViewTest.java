package org.alexmond.gotmpl4j.spring.view;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.alexmond.gotmpl4j.GoTemplateException;
import org.alexmond.gotmpl4j.spring.GoTemplateFactory;
import org.alexmond.gotmpl4j.spring.GoTemplateLoader;
import org.alexmond.gotmpl4j.spring.GoTemplateService;
import org.alexmond.gotmpl4j.spring.Gotmpl4jProperties;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Exercises the reactive view directly — body, content-type charset, error propagation —
 * rather than only transitively through the WebFlux integration test (issue #28).
 */
class GoTemplateReactiveViewTest {

	private static GoTemplateReactiveView view(String url) {
		Gotmpl4jProperties properties = new Gotmpl4jProperties();
		GoTemplateFactory factory = new GoTemplateFactory();
		GoTemplateLoader loader = new GoTemplateLoader(new DefaultResourceLoader(), properties, factory);
		GoTemplateReactiveView view = new GoTemplateReactiveView();
		view.setUrl(url);
		view.setService(new GoTemplateService(loader, factory, true));
		return view;
	}

	@Test
	void writesBody() {
		GoTemplateReactiveView view = view("hello");
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

		view.renderInternal(Map.of("Name", "world", "Site", "gotmpl4j"), MediaType.TEXT_HTML, exchange).block();

		assertEquals("Hi WORLD from gotmpl4j", exchange.getResponse().getBodyAsString().block());
	}

	@Test
	void honoursContentTypeCharsetForNonAscii() {
		GoTemplateReactiveView view = view("unicode");
		MediaType utf8Html = new MediaType("text", "html", StandardCharsets.UTF_8);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

		view.renderInternal(Map.of("Name", "world"), utf8Html, exchange).block();

		assertEquals("Café WORLD ☕", exchange.getResponse().getBodyAsString().block());
	}

	@Test
	void propagatesRenderErrors() {
		GoTemplateReactiveView view = view("does-not-exist");
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

		var rendered = view.renderInternal(Map.of(), MediaType.TEXT_HTML, exchange);
		assertThrows(GoTemplateException.class, rendered::block);
	}

}
