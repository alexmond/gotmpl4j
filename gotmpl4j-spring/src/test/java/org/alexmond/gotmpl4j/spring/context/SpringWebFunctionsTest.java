package org.alexmond.gotmpl4j.spring.context;

import java.util.Map;

import jakarta.servlet.http.Cookie;
import org.alexmond.gotmpl4j.GoTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringWebFunctionsTest {

	private MockHttpServletRequest request;

	@BeforeEach
	void bindRequest() {
		this.request = new MockHttpServletRequest("GET", "/orders");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(this.request));
	}

	@AfterEach
	void unbindRequest() {
		RequestContextHolder.resetRequestAttributes();
	}

	private String render(String template) {
		GoTemplate t = GoTemplate.builder().withProvider(new SpringWebFunctions()).build();
		t.parse("t", template);
		return t.render("t", Map.of());
	}

	@Test
	void paramWithOptionalDefault() {
		this.request.setParameter("q", "shoes");
		assertEquals("shoes", render("{{ param \"q\" }}"));
		assertEquals("none", render("{{ param \"missing\" \"none\" }}"));
	}

	@Test
	void headerCookieSessionUri() {
		this.request.addHeader("X-Tenant", "acme");
		this.request.setCookies(new Cookie("theme", "dark"));
		this.request.getSession().setAttribute("flash", "saved");
		assertEquals("acme", render("{{ header \"X-Tenant\" }}"));
		assertEquals("dark", render("{{ cookie \"theme\" }}"));
		assertEquals("saved", render("{{ session \"flash\" }}"));
		assertEquals("/orders", render("{{ requestUri }}"));
	}

	@Test
	void gracefulOutsideARequest() {
		RequestContextHolder.resetRequestAttributes();
		assertEquals("", render("{{ param \"q\" }}"));
		assertEquals("", render("{{ requestUri }}"));
	}

}
