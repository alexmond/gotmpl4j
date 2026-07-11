package org.alexmond.gotmpl4j.sprig;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.alexmond.gotmpl4j.GoTemplate;
import org.alexmond.gotmpl4j.GoTemplateRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end check that real Sprig (auto-discovered, template-independent) flows through
 * a shared {@link GoTemplateRegistry}: templates built from the registry render Sprig
 * functions identically to {@code new GoTemplate()}, and many templates can be built and
 * executed concurrently from one shared registry (#119).
 */
class SprigRegistryIntegrationTest {

	@Test
	void registryBuiltTemplateMatchesPlainGoTemplateForSprig() {
		GoTemplateRegistry registry = GoTemplateRegistry.create();

		GoTemplate viaRegistry = GoTemplate.builder().registry(registry).build();
		viaRegistry.parse("t", "{{ upper \"hi\" }}-{{ trim \"  x  \" }}-{{ repeat 3 \"ab\" }}");

		GoTemplate plain = new GoTemplate();
		plain.parse("t", "{{ upper \"hi\" }}-{{ trim \"  x  \" }}-{{ repeat 3 \"ab\" }}");

		assertEquals(plain.render(null), viaRegistry.render(null));
		assertEquals("HI-x-ababab", viaRegistry.render(null));
	}

	@Test
	void manyTemplatesFromOneRegistryBuildAndRenderConcurrently() throws Exception {
		GoTemplateRegistry registry = GoTemplateRegistry.create();
		int threads = Math.max(4, Runtime.getRuntime().availableProcessors());
		int perThread = 100;

		ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			CountDownLatch start = new CountDownLatch(1);
			CountDownLatch done = new CountDownLatch(threads);
			ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();

			for (int i = 0; i < threads; i++) {
				pool.submit(() -> {
					try {
						start.await();
						for (int r = 0; r < perThread; r++) {
							// Each build is a fresh template namespace over the shared
							// registry.
							GoTemplate t = GoTemplate.builder().registry(registry).build();
							t.parse("t", "{{ upper .in }}");
							String value = "v" + r;
							if (!("V" + r).equals(t.render(Map.of("in", value)))) {
								failures.add(new AssertionError("bad output for " + value));
							}
						}
					}
					catch (Throwable ex) {
						failures.add(ex);
					}
					finally {
						done.countDown();
					}
				});
			}
			start.countDown();
			assertTrue(done.await(60, TimeUnit.SECONDS), "threads did not finish");
			assertTrue(failures.isEmpty(), "concurrent registry use failed: " + failures.peek());
		}
		finally {
			pool.shutdownNow();
		}
	}

}
