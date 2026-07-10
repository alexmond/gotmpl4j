package org.alexmond.gotmpl4j.concurrency;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

import org.alexmond.gotmpl4j.GoTemplate;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Reusable concurrency-correctness harness for the engine.
 *
 * <p>
 * The single invariant it enforces is the one every shared-state optimization can break:
 * <strong>N threads render the <em>same</em> compiled {@link GoTemplate} with
 * <em>different</em> data, and each thread must get exactly its own correct
 * output.</strong> Crossed variable bindings, a corrupted
 * {@code variables}/{@code scopeStack}, or an unsafely-published cache surface here as a
 * per-thread output mismatch or a thrown exception (e.g.
 * {@code ConcurrentModificationException}).
 *
 * <p>
 * Each (thread, round) pair is assigned a unique id, so a render that accidentally
 * observed another thread's state produces a <em>different</em> string than this id's
 * expected output — making the leak detectable rather than silently equal. Threads start
 * on a shared latch to maximise the contention window.
 *
 * <p>
 * Extend by writing a new {@link IntFunction} of {@link Case} (id → data + expected) for
 * the shape a given fix touches — a variable-binding {@code range}, an {@code include}, a
 * POJO/method-dispatch model, and so on — and calling {@link #run(IntFunction)}.
 *
 * <p>
 * Test-scope utility (not shipped). Modelled on {@code ConcurrentHtmlEscapeTest}.
 */
public final class ConcurrencyStress {

	private final GoTemplate template;

	private final String name;

	private int threads = Math.max(4, Runtime.getRuntime().availableProcessors());

	private int iterations = 200;

	private long timeoutSeconds = 60;

	private ConcurrencyStress(GoTemplate template, String name) {
		this.template = template;
		this.name = name;
	}

	/**
	 * Start a stress run against a named template on the given (already compiled, shared)
	 * engine instance.
	 * @param template the shared, compiled template rendered from every thread
	 * @param name the named template to render
	 * @return a configurable harness
	 */
	public static ConcurrencyStress on(GoTemplate template, String name) {
		return new ConcurrencyStress(template, name);
	}

	public ConcurrencyStress threads(int value) {
		this.threads = value;
		return this;
	}

	public ConcurrencyStress iterations(int value) {
		this.iterations = value;
		return this;
	}

	public ConcurrencyStress timeoutSeconds(long value) {
		this.timeoutSeconds = value;
		return this;
	}

	/**
	 * Run the stress and assert race-freedom: no thread threw, and every render produced
	 * exactly the output expected for its own id.
	 * @param caseFor maps a unique work id to the data + expected output for that id
	 */
	public void run(IntFunction<Case> caseFor) {
		ExecutorService pool = Executors.newFixedThreadPool(this.threads);
		try {
			CountDownLatch start = new CountDownLatch(1);
			CountDownLatch done = new CountDownLatch(this.threads);
			ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
			AtomicReference<String> mismatch = new AtomicReference<>();

			for (int t = 0; t < this.threads; t++) {
				final int threadIndex = t;
				pool.submit(() -> workerLoop(threadIndex, caseFor, start, done, failures, mismatch));
			}

			start.countDown();
			boolean finished = awaitDone(done);
			assertTrue(finished, "threads did not finish within " + this.timeoutSeconds + "s (possible deadlock)");
			reportFailures(failures);
			assertNull(mismatch.get(), "crossed/corrupted render output: " + mismatch.get());
		}
		finally {
			pool.shutdownNow();
		}
	}

	private void workerLoop(int threadIndex, IntFunction<Case> caseFor, CountDownLatch start, CountDownLatch done,
			ConcurrentLinkedQueue<Throwable> failures, AtomicReference<String> mismatch) {
		try {
			start.await();
			for (int round = 0; round < this.iterations; round++) {
				int id = threadIndex * this.iterations + round;
				Case work = caseFor.apply(id);
				String actual = this.template.render(this.name, work.data());
				if (!work.expected().equals(actual)) {
					mismatch.compareAndSet(null,
							"id=" + id + " expected=<" + work.expected() + "> actual=<" + actual + ">");
				}
			}
		}
		catch (Throwable ex) {
			failures.add(ex);
		}
		finally {
			done.countDown();
		}
	}

	private boolean awaitDone(CountDownLatch done) {
		try {
			return done.await(this.timeoutSeconds, TimeUnit.SECONDS);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private static void reportFailures(ConcurrentLinkedQueue<Throwable> failures) {
		if (!failures.isEmpty()) {
			Throwable first = failures.peek();
			fail("concurrent render threw " + failures.size() + " time(s); first: " + first, first);
		}
	}

	/** A single unit of work: the data to render and the exact output it must produce. */
	public record Case(Object data, String expected) {
	}

}
