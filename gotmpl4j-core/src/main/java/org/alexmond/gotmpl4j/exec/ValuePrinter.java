package org.alexmond.gotmpl4j.exec;

import java.io.IOException;
import java.io.Writer;

import org.alexmond.gotmpl4j.GoFmt;
import org.alexmond.gotmpl4j.MissingKeyMode;
import org.alexmond.gotmpl4j.TemplateExecutionException;

final class ValuePrinter {

	private ValuePrinter() {
	}

	/**
	 * Writes a value to output the way Go's {@code fmt.Sprint} would: a values
	 * {@code float64} uses Go's %g ({@code 1000000 -> 1e+06}), a map renders as
	 * {@code map[k:v …]} with sorted keys, a slice as {@code [a b c]}. A {@code null}
	 * renders per the {@link MissingKeyMode}: Go's {@code <no value>} marker by default,
	 * an empty string under {@link MissingKeyMode#ZERO} (Helm semantics), or an error
	 * under {@link MissingKeyMode#ERROR}. This is the bare-action sink only; the
	 * {@code print}/ {@code printf} functions apply {@code fmt} semantics ({@code <nil>})
	 * elsewhere.
	 */
	static void printValue(Writer writer, Object value, MissingKeyMode missingKey)
			throws IOException, TemplateExecutionException {
		if (value == null) {
			if (missingKey == MissingKeyMode.ZERO) {
				return;
			}
			if (missingKey == MissingKeyMode.ERROR) {
				throw new TemplateExecutionException("nil value in template output (missingkey=error)");
			}
			// MissingKeyMode.DEFAULT
			writer.write("<no value>");
			return;
		}
		writer.write(GoFmt.sprint(value));
	}

}
