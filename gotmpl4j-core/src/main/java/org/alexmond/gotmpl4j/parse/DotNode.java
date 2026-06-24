package org.alexmond.gotmpl4j.parse;

/**
 * The cursor node ({@code .}), which evaluates to the current data value in scope.
 */
public class DotNode implements Node {

	@Override
	public String toString() {
		return ".";
	}

}
