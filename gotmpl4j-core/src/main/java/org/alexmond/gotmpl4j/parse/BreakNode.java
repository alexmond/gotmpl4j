package org.alexmond.gotmpl4j.parse;

/**
 * A {@code {{break}}} action that terminates the innermost enclosing {@code range} loop.
 */
public class BreakNode implements Node {

	@Override
	public String toString() {
		return "{{break}}";
	}

}
