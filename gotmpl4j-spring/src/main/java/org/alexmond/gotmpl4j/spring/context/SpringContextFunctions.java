package org.alexmond.gotmpl4j.spring.context;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.alexmond.gotmpl4j.Function;
import org.alexmond.gotmpl4j.FunctionProvider;
import org.alexmond.gotmpl4j.GoTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;

/**
 * Contributes template functions that read from the running Spring application:
 *
 * <ul>
 * <li>{@code msg} — resolve an i18n message via {@link MessageSource} using the request
 * locale ({@code {{ msg "greeting.hello" .name }}}); a missing key falls back to the code
 * itself.</li>
 * <li>{@code env} — read a property from the Spring {@link Environment} ({@code {{ env
 * "app.title" }}} or {@code {{ env "x" "fallback" }}}).</li>
 * <li>{@code bean} — resolve a bean from the {@link ApplicationContext} ({@code {{ (bean
 * "userService").currentUser }}}); <strong>off by default</strong> — see
 * {@link GoTemplateSpringProperties}.</li>
 * </ul>
 *
 * <p>
 * Registered as a Spring bean by {@link GoTemplateSpringAutoConfiguration}, so it sees
 * the live context; {@code gotmpl4j-core} stays Spring-free. Per-request state (the
 * locale) is read from {@link LocaleContextHolder}, so a single shared engine renders
 * correctly per request. This targets <em>developer-authored</em> templates —
 * {@code bean} is a template-injection vector for untrusted input.
 */
public class SpringContextFunctions implements FunctionProvider {

	/**
	 * Above Sprig (100) and Helm (200): application-context functions win on name clash.
	 */
	private static final int PRIORITY = 300;

	private final MessageSource messageSource;

	private final Environment environment;

	private final ApplicationContext applicationContext;

	private final boolean exposeBeans;

	private final List<String> allowedBeans;

	public SpringContextFunctions(MessageSource messageSource, Environment environment,
			ApplicationContext applicationContext, GoTemplateSpringProperties properties) {
		this.messageSource = messageSource;
		this.environment = environment;
		this.applicationContext = applicationContext;
		this.exposeBeans = properties.isExposeBeans();
		this.allowedBeans = properties.getAllowedBeans();
	}

	@Override
	public Map<String, Function> getFunctions(GoTemplate template) {
		Map<String, Function> functions = new HashMap<>();
		functions.put("msg", this::msg);
		functions.put("env", this::env);
		functions.put("bean", this::bean);
		return functions;
	}

	@Override
	public int priority() {
		return PRIORITY;
	}

	@Override
	public String name() {
		return "SpringContext";
	}

	// {{ msg "greeting.hello" .name }} — args after the code are the message arguments; a
	// missing key renders the code itself (never throws mid-render).
	private Object msg(Object... args) {
		if (args.length == 0) {
			return "";
		}
		String code = String.valueOf(args[0]);
		Object[] messageArgs = (args.length > 1) ? Arrays.copyOfRange(args, 1, args.length) : null;
		Locale locale = LocaleContextHolder.getLocale();
		return this.messageSource.getMessage(code, messageArgs, code, locale);
	}

	// {{ env "app.title" }} / {{ env "app.title" "fallback" }} — Spring Environment
	// property,
	// with an optional default; missing and no default renders empty.
	private Object env(Object... args) {
		if (args.length == 0) {
			return "";
		}
		String value = this.environment.getProperty(String.valueOf(args[0]));
		if (value != null) {
			return value;
		}
		return (args.length > 1) ? String.valueOf(args[1]) : "";
	}

	// {{ (bean "userService").currentUser }} — ApplicationContext bean. Disabled unless
	// gotmpl4j.spring.expose-beans=true, and (optionally) restricted to an allow-list.
	private Object bean(Object... args) {
		if (!this.exposeBeans) {
			throw new IllegalStateException(
					"bean() is disabled; set gotmpl4j.spring.expose-beans=true to allow template bean access");
		}
		if (args.length == 0) {
			return null;
		}
		String name = String.valueOf(args[0]);
		if (!this.allowedBeans.isEmpty() && !this.allowedBeans.contains(name)) {
			throw new IllegalStateException("bean '" + name + "' is not in gotmpl4j.spring.allowed-beans");
		}
		return this.applicationContext.getBean(name);
	}

}
