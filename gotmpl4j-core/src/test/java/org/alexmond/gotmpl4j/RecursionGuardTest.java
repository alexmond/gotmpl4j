package org.alexmond.gotmpl4j;

import java.io.StringWriter;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Untrusted templates must not be able to crash execution or parsing with a
 * {@link StackOverflowError}; deep recursion is converted to the normal checked
 * exceptions. See issue #7.
 */
class RecursionGuardTest {

	@Test
	void selfReferentialTemplateThrowsExecutionExceptionNotStackOverflow() throws TemplateParseException {
		GoTemplate template = new GoTemplate();
		template.parse("main", "{{define \"x\"}}{{template \"x\" .}}{{end}}{{template \"x\" .}}");
		StringWriter w = new StringWriter();
		assertThrows(TemplateExecutionException.class, () -> template.execute("main", Map.of(), w));
	}

	@Test
	void deeplyNestedActionsThrowParseExceptionNotStackOverflow() {
		StringBuilder sb = new StringBuilder();
		int depth = 50_000;
		for (int i = 0; i < depth; i++) {
			sb.append("{{if 1}}");
		}
		sb.append('x');
		for (int i = 0; i < depth; i++) {
			sb.append("{{end}}");
		}
		String deeplyNested = sb.toString();
		GoTemplate t = new GoTemplate();
		assertThrows(TemplateParseException.class, () -> t.parse("deep", deeplyNested));
	}

}
