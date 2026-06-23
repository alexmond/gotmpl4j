package org.alexmond.gotmpl4j;

import java.io.StringWriter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Stresses concurrent first-execution of an html-escaping template set where two entry
 * views share a partial (issue #6). Lazy escaping mutates the shared AST on first use, so
 * if escaping were not properly serialized against concurrent reads this would surface as
 * a ConcurrentModificationException / IndexOutOfBoundsException or half-escaped output.
 */
class ConcurrentHtmlEscapeTest {

	private static final int ITERATIONS = 300;

	private static final int THREADS = 8;

	@Test
	void concurrentColdStartEscapingIsRaceFree() throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(THREADS);
		try {
			for (int iter = 0; iter < ITERATIONS; iter++) {
				// Fresh template each iteration so escaping starts cold and races with
				// the first concurrent executions.
				GoTemplate t = GoTemplate.builder().htmlEscaping().build();
				t.parse("a", "<p>{{template \"shared\" .}}|{{.}}</p>");
				t.parse("b", "<div>{{template \"shared\" .}}</div>");
				t.parse("shared", "{{.}}");

				CountDownLatch start = new CountDownLatch(1);
				CountDownLatch done = new CountDownLatch(THREADS);
				ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
				AtomicReference<String> badOutput = new AtomicReference<>();

				for (int i = 0; i < THREADS; i++) {
					final boolean useA = (i % 2 == 0);
					pool.submit(() -> {
						try {
							start.await();
							StringWriter w = new StringWriter();
							if (useA) {
								t.execute("a", "<x>", w);
								if (!"<p>&lt;x&gt;|&lt;x&gt;</p>".equals(w.toString())) {
									badOutput.set(w.toString());
								}
							}
							else {
								t.execute("b", "<x>", w);
								if (!"<div>&lt;x&gt;</div>".equals(w.toString())) {
									badOutput.set(w.toString());
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
				assertTrue(done.await(30, TimeUnit.SECONDS), "threads did not finish");

				if (!failures.isEmpty()) {
					Throwable first = failures.peek();
					fail("concurrent escaping threw " + failures.size() + " time(s); first: " + first, first);
				}
				assertEquals(null, badOutput.get(), "concurrent escaping produced wrong/half-escaped output");
			}
		}
		finally {
			pool.shutdownNow();
		}
	}

}
