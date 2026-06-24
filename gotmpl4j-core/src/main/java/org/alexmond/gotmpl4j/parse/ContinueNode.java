package org.alexmond.gotmpl4j.parse;

/**
 * A {@code {{continue}}} action that skips to the next iteration of the innermost
 * enclosing {@code range} loop.
 */
public class ContinueNode implements Node {

	@Override
	public String toString() {
		return "{{continue}}";
	}

}
