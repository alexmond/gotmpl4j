package org.alexmond.gotmpl4j.spring.context;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.alexmond.gotmpl4j.GoTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringSecurityFunctionsTest {

	private String render(String template) {
		GoTemplate t = GoTemplate.builder().withProvider(new SpringSecurityFunctions()).build();
		t.parse("t", template);
		return t.render("t", Map.of());
	}

	private void authenticate(String name, String... authorities) {
		List<SimpleGrantedAuthority> granted = Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(name, "n/a", granted));
	}

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void unauthenticatedByDefault() {
		assertEquals("false", render("{{ isAuthenticated }}"));
		assertEquals("false", render("{{ hasRole \"ADMIN\" }}"));
		assertEquals("", render("{{ username }}"));
	}

	@Test
	void rolesAndAuthorities() {
		authenticate("alice", "ROLE_ADMIN", "SCOPE_read");
		assertEquals("true", render("{{ isAuthenticated }}"));
		assertEquals("alice", render("{{ username }}"));
		assertEquals("true", render("{{ hasRole \"ADMIN\" }}")); // ROLE_ prefix added
		assertEquals("false", render("{{ hasRole \"USER\" }}"));
		assertEquals("true", render("{{ hasAnyRole \"USER\" \"ADMIN\" }}"));
		assertEquals("true", render("{{ hasAuthority \"SCOPE_read\" }}")); // exact, no
																			// prefix
		assertEquals("false", render("{{ hasAuthority \"ADMIN\" }}"));
	}

	@Test
	void conditionalRendering() {
		authenticate("bob", "ROLE_USER");
		assertEquals("hi bob", render("{{ if hasRole \"USER\" }}hi {{ username }}{{ else }}denied{{ end }}"));
	}

}
