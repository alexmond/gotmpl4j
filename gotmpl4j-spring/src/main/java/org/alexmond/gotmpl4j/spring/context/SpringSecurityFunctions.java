package org.alexmond.gotmpl4j.spring.context;

import java.util.HashMap;
import java.util.Map;

import org.alexmond.gotmpl4j.Function;
import org.alexmond.gotmpl4j.FunctionProvider;
import org.alexmond.gotmpl4j.GoTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Contributes Spring Security functions that read the current {@link Authentication} from
 * the {@link SecurityContextHolder} (a per-thread/-request holder, so a shared engine
 * renders correctly per request):
 *
 * <ul>
 * <li>{@code isAuthenticated} — {@code true} for a non-anonymous, authenticated
 * principal.</li>
 * <li>{@code hasRole "ADMIN"} / {@code hasAnyRole "A" "B"} — authority check with the
 * {@code ROLE_} prefix added (Spring convention).</li>
 * <li>{@code hasAuthority "SCOPE_read"} — exact authority check (no prefix).</li>
 * <li>{@code principal} — the principal object; {@code username} — its name.</li>
 * </ul>
 *
 * <p>
 * Only registered when Spring Security is on the classpath (see
 * {@link GoTemplateSpringAutoConfiguration}); {@code spring-security-core} is an
 * <em>optional</em> dependency of {@code gotmpl4j-spring}. The
 * {@code thymeleaf-extras-springsecurity} equivalent, as template functions.
 */
public class SpringSecurityFunctions implements FunctionProvider {

	private static final int PRIORITY = 300;

	@Override
	public Map<String, Function> getFunctions(GoTemplate template) {
		Map<String, Function> functions = new HashMap<>();
		functions.put("isAuthenticated", (args) -> isAuthenticated());
		functions.put("hasRole", this::hasRole);
		functions.put("hasAnyRole", this::hasAnyRole);
		functions.put("hasAuthority", this::hasAuthority);
		functions.put("principal", (args) -> principal());
		functions.put("username", (args) -> username());
		return functions;
	}

	@Override
	public int priority() {
		return PRIORITY;
	}

	@Override
	public String name() {
		return "SpringSecurity";
	}

	private static Authentication authentication() {
		return SecurityContextHolder.getContext().getAuthentication();
	}

	private Object isAuthenticated() {
		Authentication auth = authentication();
		return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
	}

	private Object hasRole(Object... args) {
		return (args.length > 0) && hasAuthorityValue(roleAuthority(String.valueOf(args[0])));
	}

	private Object hasAnyRole(Object... args) {
		for (Object role : args) {
			if (hasAuthorityValue(roleAuthority(String.valueOf(role)))) {
				return true;
			}
		}
		return false;
	}

	private Object hasAuthority(Object... args) {
		return (args.length > 0) && hasAuthorityValue(String.valueOf(args[0]));
	}

	private static String roleAuthority(String role) {
		return role.startsWith("ROLE_") ? role : "ROLE_" + role;
	}

	private boolean hasAuthorityValue(String authority) {
		Authentication auth = authentication();
		if (auth == null) {
			return false;
		}
		for (GrantedAuthority granted : auth.getAuthorities()) {
			if (authority.equals(granted.getAuthority())) {
				return true;
			}
		}
		return false;
	}

	private Object principal() {
		Authentication auth = authentication();
		return (auth != null) ? auth.getPrincipal() : null;
	}

	private Object username() {
		Authentication auth = authentication();
		return (auth != null) ? auth.getName() : "";
	}

}
