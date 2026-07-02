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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the servlet view directly — content type, charset, body, error propagation —
 * rather than only transitively through the MVC integration test (issue #28).
 */
class GoTemplateViewTest {

	private static GoTemplateService loaderBackedService() {
		Gotmpl4jProperties properties = new Gotmpl4jProperties();
		GoTemplateFactory factory = new GoTemplateFactory();
		GoTemplateLoader loader = new GoTemplateLoader(new DefaultResourceLoader(), properties, factory);
		return new GoTemplateService(loader, factory, true);
	}

	private static GoTemplateView view(String url) {
		GoTemplateView view = new GoTemplateView();
		view.setUrl(url);
		view.setService(loaderBackedService());
		view.setCharset(StandardCharsets.UTF_8);
		view.setContentType("text/html");
		return view;
	}

	@Test
	void writesBodyContentTypeAndCharset() throws Exception {
		GoTemplateView view = view("hello");
		MockHttpServletResponse response = new MockHttpServletResponse();

		view.renderMergedTemplateModel(Map.of("Name", "world", "Site", "gotmpl4j"), new MockHttpServletRequest(),
				response);

		assertEquals("Hi WORLD from gotmpl4j", response.getContentAsString());
		assertTrue(response.getContentType().startsWith("text/html"), response.getContentType());
		assertEquals("UTF-8", response.getCharacterEncoding());
	}

	@Test
	void honoursNonAsciiCharset() throws Exception {
		GoTemplateView view = view("unicode");
		MockHttpServletResponse response = new MockHttpServletResponse();

		view.renderMergedTemplateModel(Map.of("Name", "world"), new MockHttpServletRequest(), response);

		assertEquals("Café WORLD ☕", response.getContentAsString());
	}

	@Test
	void propagatesRenderErrors() {
		GoTemplateView view = view("does-not-exist");
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		assertThrows(GoTemplateException.class, () -> view.renderMergedTemplateModel(Map.of(), request, response));
	}

}
