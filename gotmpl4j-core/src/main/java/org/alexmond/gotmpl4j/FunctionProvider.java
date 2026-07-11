package org.alexmond.gotmpl4j;

import java.util.Map;

/**
 * Service Provider Interface for registering template functions with {@link GoTemplate}.
 * Implementations can be discovered automatically via {@link java.util.ServiceLoader} or
 * registered explicitly via {@link GoTemplate.Builder#withProvider(FunctionProvider)}.
 *
 * <p>
 * Priority ordering controls override behavior when multiple providers register functions
 * with the same name. Higher priority wins.
 *
 * <p>
 * Built-in priorities: Go builtins=0, Sprig=100, Helm=200.
 *
 * @since 1.0
 * @see GoTemplate.Builder
 */
@FunctionalInterface
public interface FunctionProvider {

	/**
	 * Return the functions this provider contributes.
	 * @param template the GoTemplate being constructed (needed by functions like
	 * {@code include} and {@code tpl} that execute templates)
	 * @return map of function name to implementation
	 */
	Map<String, Function> getFunctions(GoTemplate template);

	/**
	 * Priority for ordering and override resolution. Lower values are loaded first;
	 * higher values override on name collision.
	 * @return the priority (default 0)
	 */
	default int priority() {
		return 0;
	}

	/**
	 * Human-readable name for diagnostics and logging.
	 * @return the provider name
	 */
	default String name() {
		return getClass().getSimpleName();
	}

	/**
	 * Whether this provider's functions ignore the {@code template} argument passed to
	 * {@link #getFunctions(GoTemplate)} — i.e. none of them captures the template
	 * instance (as {@code include}/{@code tpl} do to execute sub-templates).
	 *
	 * <p>
	 * When {@code true}, the provider's functions are template-independent and can be
	 * built <em>once</em> and shared across many {@link GoTemplate} instances via a
	 * {@link GoTemplateRegistry}; the registry may invoke
	 * {@link #getFunctions(GoTemplate)} with a {@code null} template, so an independent
	 * provider must not dereference it. Pure function libraries (e.g. Sprig) are
	 * template-independent and should override this to {@code true}.
	 *
	 * <p>
	 * The default is {@code false} (conservative): a provider is assumed to capture the
	 * template, so it is re-invoked per instance and never shared. Overriding to
	 * {@code true} is a correctness assertion — only do so if every returned function
	 * ignores its {@code template} argument.
	 * @return {@code true} if the provider's functions ignore the template argument
	 * @since 1.3
	 */
	default boolean isTemplateIndependent() {
		return false;
	}

}
