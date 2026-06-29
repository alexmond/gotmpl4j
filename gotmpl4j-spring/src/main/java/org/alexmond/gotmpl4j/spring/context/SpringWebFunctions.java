package org.alexmond.gotmpl4j.spring.context;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.alexmond.gotmpl4j.Function;
import org.alexmond.gotmpl4j.FunctionProvider;
import org.alexmond.gotmpl4j.GoTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Contributes functions that read the current servlet HTTP request, resolved from
 * {@link RequestContextHolder} (per-request thread-local):
 *
 * <ul>
 * <li>{@code param "q"} — a request parameter (with optional default).</li>
 * <li>{@code header "Accept"} — a request header.</li>
 * <li>{@code cookie "JSESSIONID"} — a cookie value.</li>
 * <li>{@code session "user"} — a session attribute (does not create a session).</li>
 * <li>{@code requestUri} — the request URI.</li>
 * <li>{@code csrf} — the CSRF token object (when Spring Security set one); {@code {{
 * csrf.Token }}} / {@code {{ csrf.ParameterName }}} / {@code {{ csrf.HeaderName }}}.</li>
 * </ul>
 *
 * <p>
 * Only registered in a servlet web application (see
 * {@link GoTemplateSpringAutoConfiguration}); {@code spring-web} and
 * {@code jakarta.servlet-api} are optional dependencies. Outside a request (e.g.
 * rendering from a background thread) these return an empty value rather than failing.
 */
public class SpringWebFunctions implements FunctionProvider {

	private static final int PRIORITY = 300;

	// Spring Security stores the CsrfToken under both "_csrf" and the CsrfToken class
	// name.
	private static final String CSRF_ATTRIBUTE = "org.springframework.security.web.csrf.CsrfToken";

	@Override
	public Map<String, Function> getFunctions(GoTemplate template) {
		Map<String, Function> functions = new HashMap<>();
		functions.put("param", this::param);
		functions.put("header", this::header);
		functions.put("cookie", this::cookie);
		functions.put("session", this::session);
		functions.put("requestUri", (args) -> requestUri());
		functions.put("csrf", (args) -> csrf());
		return functions;
	}

	@Override
	public int priority() {
		return PRIORITY;
	}

	@Override
	public String name() {
		return "SpringWeb";
	}

	private static HttpServletRequest request() {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		return (attributes instanceof ServletRequestAttributes servlet) ? servlet.getRequest() : null;
	}

	private Object param(Object... args) {
		HttpServletRequest request = request();
		if (request == null || args.length == 0) {
			return defaultArg(args);
		}
		String value = request.getParameter(String.valueOf(args[0]));
		return (value != null) ? value : defaultArg(args);
	}

	private Object header(Object... args) {
		HttpServletRequest request = request();
		if (request == null || args.length == 0) {
			return defaultArg(args);
		}
		String value = request.getHeader(String.valueOf(args[0]));
		return (value != null) ? value : defaultArg(args);
	}

	private Object cookie(Object... args) {
		HttpServletRequest request = request();
		if (request == null || args.length == 0 || request.getCookies() == null) {
			return defaultArg(args);
		}
		String name = String.valueOf(args[0]);
		for (Cookie cookie : request.getCookies()) {
			if (name.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return defaultArg(args);
	}

	private Object session(Object... args) {
		HttpServletRequest request = request();
		if (request == null || args.length == 0) {
			return null;
		}
		HttpSession session = request.getSession(false);
		return (session != null) ? session.getAttribute(String.valueOf(args[0])) : null;
	}

	private Object requestUri() {
		HttpServletRequest request = request();
		return (request != null) ? request.getRequestURI() : "";
	}

	private Object csrf() {
		HttpServletRequest request = request();
		return (request != null) ? request.getAttribute(CSRF_ATTRIBUTE) : null;
	}

	// Optional second argument is the fallback when the value is absent.
	private static Object defaultArg(Object... args) {
		return (args.length > 1) ? String.valueOf(args[1]) : "";
	}

}
