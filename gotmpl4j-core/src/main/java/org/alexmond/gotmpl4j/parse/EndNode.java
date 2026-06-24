package org.alexmond.gotmpl4j.parse;

/**
 * An {@code {{end}}} delimiter node marking the close of an {@code if}, {@code range},
 * {@code with}, {@code block}, or {@code define} action.
 */
public class EndNode implements Node {

	@Override
	public String toString() {
		return "{{end}}";
	}

}
