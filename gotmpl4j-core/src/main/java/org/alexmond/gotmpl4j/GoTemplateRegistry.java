package org.alexmond.gotmpl4j;

import java.beans.BeanInfo;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

/**
 * A pre-built, shareable source of template functions and reflection caches, so many
 * {@link GoTemplate} instances can be constructed cheaply against one expensive build.
 *
 * <p>
 * Constructing a {@code GoTemplate} normally runs {@link ServiceLoader} and rebuilds the
 * entire function map (Go builtins plus every provider's functions — hundreds of Sprig
 * lambdas) from scratch, with fresh, cold reflection caches. A caller that needs a clean
 * template namespace per operation (e.g. a chart rendered per request) otherwise re-pays
 * that whole construction every time. A registry runs {@code ServiceLoader} once, builds
 * every {@linkplain FunctionProvider#isTemplateIndependent() template-independent}
 * provider's functions once, and holds the reflection caches — then hands them to each
 * {@link GoTemplate.Builder#registry(GoTemplateRegistry) template built from it}.
 *
 * <p>
 * {@linkplain FunctionProvider#isTemplateIndependent() Template-dependent} providers
 * (those whose functions capture the template, such as {@code include}/{@code tpl}) are
 * <em>not</em> shared — they are re-invoked against each new template, so every instance
 * keeps its own correct binding while still reusing the shared, independent base.
 *
 * <p>
 * Thread-safe: build a registry once and share it across threads. Each
 * {@link GoTemplate.Builder#registry(GoTemplateRegistry)} build produces an independent
 * template (its own {@code rootNodes} and variable state) over the shared, immutable
 * function contributions and {@link ConcurrentHashMap} reflection caches.
 *
 * @since 1.3
 * @see FunctionProvider#isTemplateIndependent()
 * @see GoTemplate.Builder#registry(GoTemplateRegistry)
 */
@Slf4j
public final class GoTemplateRegistry {

	private final List<Contribution> contributions;

	// Shared across every GoTemplate built from this registry (and every Executor those
	// spawn), so reflection is done once per data class and stays warm across instances.
	private final Map<Class<?>, BeanInfo> beanInfoCache = new ConcurrentHashMap<>();

	private final Map<Class<?>, Map<String, Method>> accessorCache = new ConcurrentHashMap<>();

	private GoTemplateRegistry(List<Contribution> contributions) {
		this.contributions = contributions;
	}

	/**
	 * Build a registry from the {@link FunctionProvider}s discovered on the classpath via
	 * {@link ServiceLoader} (the same auto-discovery {@code new GoTemplate()} uses).
	 * @return a shareable registry
	 */
	public static GoTemplateRegistry create() {
		return builder().build();
	}

	/**
	 * Create a builder for explicit control over which providers back the registry.
	 * @return a new builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	List<Contribution> contributions() {
		return this.contributions;
	}

	Map<Class<?>, BeanInfo> beanInfoCache() {
		return this.beanInfoCache;
	}

	Map<Class<?>, Map<String, Method>> accessorCache() {
		return this.accessorCache;
	}

	/**
	 * One provider's contribution to the function map: either a pre-built immutable map
	 * (template-independent, shared verbatim) or a provider re-invoked per template
	 * (template-dependent). Exactly one field is non-null.
	 */
	record Contribution(Map<String, Function> cached, FunctionProvider dependent) {
	}

	/**
	 * Builds a {@link GoTemplateRegistry} from auto-discovered and/or explicit providers.
	 */
	public static final class Builder {

		private final List<FunctionProvider> providers = new ArrayList<>();

		private boolean autoDiscovery = true;

		Builder() {
		}

		/**
		 * Add an explicit function provider (in addition to any auto-discovered ones).
		 * @param provider the provider to add
		 * @return this builder
		 */
		public Builder withProvider(FunctionProvider provider) {
			this.providers.add(provider);
			return this;
		}

		/**
		 * Disable {@link ServiceLoader} auto-discovery; only explicitly added providers
		 * back the registry.
		 * @return this builder
		 */
		public Builder noAutoDiscovery() {
			this.autoDiscovery = false;
			return this;
		}

		/**
		 * Build the registry: run discovery once, sort by priority, and pre-build every
		 * template-independent provider's functions.
		 * @return a shareable registry
		 */
		public GoTemplateRegistry build() {
			List<FunctionProvider> all = new ArrayList<>();
			if (this.autoDiscovery) {
				for (FunctionProvider discovered : ServiceLoader.load(FunctionProvider.class)) {
					all.add(discovered);
				}
			}
			all.addAll(this.providers);
			all.sort(Comparator.comparingInt(FunctionProvider::priority));

			List<Contribution> built = new ArrayList<>(all.size());
			for (FunctionProvider provider : all) {
				if (provider.isTemplateIndependent()) {
					// Built once; the null template is the isTemplateIndependent()
					// contract.
					built.add(new Contribution(Map.copyOf(provider.getFunctions(null)), null));
					logContribution(provider, "shared");
				}
				else {
					built.add(new Contribution(null, provider));
					logContribution(provider, "per-instance");
				}
			}
			return new GoTemplateRegistry(List.copyOf(built));
		}

		private static void logContribution(FunctionProvider provider, String kind) {
			if (log.isDebugEnabled()) {
				log.debug("Registry: {} provider {} (priority={})", kind, provider.name(), provider.priority());
			}
		}

	}

}
