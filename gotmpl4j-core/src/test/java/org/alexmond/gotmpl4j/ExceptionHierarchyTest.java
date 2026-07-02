package org.alexmond.gotmpl4j;

import java.io.StringWriter;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Locks the 1.0 exception contract (issue #49): every engine exception shares the single
 * {@link GoTemplateException} root and is unchecked, so a caller can catch the root to
 * handle anything the engine raises.
 */
class ExceptionHierarchyTest {

	@Test
	void everyEngineExceptionIsUncheckedUnderTheCommonRoot() {
		assertInstanceOf(RuntimeException.class, new GoTemplateException("x"));
		for (GoTemplateException ex : new GoTemplateException[] { new TemplateException("x"),
				new TemplateParseException("x"), new TemplateExecutionException("x"),
				new TemplateNotFoundException("x"), new FunctionExecutionException("x") }) {
			assertInstanceOf(GoTemplateException.class, ex);
			assertInstanceOf(RuntimeException.class, ex);
		}
	}

	@Test
	void parseFailureIsCatchableAsTheRoot() {
		GoTemplate t = new GoTemplate();
		assertThrows(GoTemplateException.class, () -> t.parse("oops", "{{ .x "));
	}

	@Test
	void missingTemplateIsCatchableAsTheRoot() {
		GoTemplate t = new GoTemplate();
		assertThrows(GoTemplateException.class, () -> t.render("absent", Map.of()));
	}

	@Test
	void functionFailureSurfacesUnderTheRoot() {
		Function boom = (args) -> {
			throw new FunctionExecutionException("boom failed");
		};
		GoTemplate t = new GoTemplate(Map.of("boom", boom));
		t.parse("t", "{{ boom }}");
		StringWriter w = new StringWriter();
		assertThrows(GoTemplateException.class, () -> t.execute("t", Map.of(), w));
	}

}
