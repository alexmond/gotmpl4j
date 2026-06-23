package org.alexmond.gotmpl4j.exec;

import java.io.IOException;
import java.io.Writer;

import org.alexmond.gotmpl4j.GoFmt;

final class ValuePrinter {

	private ValuePrinter() {
	}

	/**
	 * Writes a value to output the way Go's {@code fmt.Sprint} would: a values
	 * {@code float64} uses Go's %g ({@code 1000000 -> 1e+06}), a map renders as
	 * {@code map[k:v …]} with sorted keys, a slice as {@code [a b c]}. A {@code null}
	 * renders Go's {@code <no value>} marker, matching {@code text/template}'s output for
	 * a nil/absent action (a missing map key, a nil map value, or a nil dot). This is the
	 * bare-action sink only; the {@code print}/{@code printf} functions apply {@code fmt}
	 * semantics ({@code <nil>}) and are handled elsewhere.
	 */
	static void printValue(Writer writer, Object value) throws IOException {
		if (value == null) {
			writer.write("<no value>");
			return;
		}
		writer.write(GoFmt.sprint(value));
	}

}
