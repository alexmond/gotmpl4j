package org.alexmond.gotmpl4j;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@link GoTemplateRegistry} sharing contract (#119): a
 * {@linkplain FunctionProvider#isTemplateIndependent() template-independent} provider is
 * built <em>once</em> and shared across templates; a template-dependent provider is
 * re-bound <em>per</em> template; and the reflection caches are shared.
 */
class GoTemplateRegistryTest {

	@Test
	void independentBuiltOnce_dependentReboundPerTemplate() {
		CountingProvider independent = new CountingProvider("indep", "I", true, 100);
		CountingProvider dependent = new CountingProvider("dep", "D", false, 200);

		GoTemplateRegistry registry = GoTemplateRegistry.builder()
			.noAutoDiscovery()
			.withProvider(independent)
			.withProvider(dependent)
			.build();

		// Independent provider is invoked once, at registry build; dependent not yet.
		assertEquals(1, independent.calls.get());
		assertEquals(0, dependent.calls.get());

		GoTemplate a = GoTemplate.builder().registry(registry).build();
		GoTemplate b = GoTemplate.builder().registry(registry).build();

		// Independent stays built-once (shared verbatim); dependent re-bound per
		// template.
		assertEquals(1, independent.calls.get());
		assertEquals(2, dependent.calls.get());

		a.parse("t", "{{ indep }}-{{ dep }}");
		b.parse("t", "{{ indep }}-{{ dep }}");
		assertEquals("I-D", a.render(null));
		assertEquals("I-D", b.render(null));
	}

	@Test
	void dependentProviderReceivesItsOwnTemplateInstance() {
		// A dependent provider returns a function that reports which template it was
		// bound
		// to; two templates from one registry must each see their own instance.
		FunctionProvider identity = new FunctionProvider() {
			@Override
			public Map<String, Function> getFunctions(GoTemplate template) {
				String id = String.valueOf(System.identityHashCode(template));
				return Map.of("boundId", (Function) (args) -> id);
			}

			@Override
			public int priority() {
				return 200;
			}
		};
		GoTemplateRegistry registry = GoTemplateRegistry.builder().noAutoDiscovery().withProvider(identity).build();

		GoTemplate a = GoTemplate.builder().registry(registry).build();
		GoTemplate b = GoTemplate.builder().registry(registry).build();
		a.parse("t", "{{ boundId }}");
		b.parse("t", "{{ boundId }}");

		assertEquals(String.valueOf(System.identityHashCode(a)), a.render(null));
		assertEquals(String.valueOf(System.identityHashCode(b)), b.render(null));
	}

	@Test
	void reflectionCachesAreSharedAcrossTemplates() {
		GoTemplateRegistry registry = GoTemplateRegistry.builder().noAutoDiscovery().build();
		GoTemplate t = GoTemplate.builder().registry(registry).build();
		t.parse("p", "{{ .Name }}");

		assertEquals("X", t.render(new Bean("X")));
		// The instance used the registry's shared cache, so the class is now warm there.
		assertTrue(registry.accessorCache().containsKey(Bean.class),
				"template should populate the registry's shared accessor cache");
	}

	/** Provider whose getFunctions call count is observable. */
	static final class CountingProvider implements FunctionProvider {

		private final AtomicInteger calls = new AtomicInteger();

		private final String funcName;

		private final String value;

		private final boolean independent;

		private final int priority;

		CountingProvider(String funcName, String value, boolean independent, int priority) {
			this.funcName = funcName;
			this.value = value;
			this.independent = independent;
			this.priority = priority;
		}

		@Override
		public Map<String, Function> getFunctions(GoTemplate template) {
			this.calls.incrementAndGet();
			return Map.of(this.funcName, (args) -> this.value);
		}

		@Override
		public boolean isTemplateIndependent() {
			return this.independent;
		}

		@Override
		public int priority() {
			return this.priority;
		}

	}

	/** POJO with a Go-style getter for the cache-sharing check. */
	public static final class Bean {

		private final String name;

		Bean(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

	}

}
