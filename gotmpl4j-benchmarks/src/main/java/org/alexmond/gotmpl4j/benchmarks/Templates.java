package org.alexmond.gotmpl4j.benchmarks;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Loads the per-engine template sources from {@code src/main/resources/templates/}.
 */
final class Templates {

	private Templates() {
	}

	static String load(String name) {
		try (InputStream in = Templates.class.getResourceAsStream("/templates/" + name)) {
			if (in == null) {
				throw new IllegalStateException("template not found: " + name);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
