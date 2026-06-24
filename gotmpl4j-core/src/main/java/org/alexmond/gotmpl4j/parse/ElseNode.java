package org.alexmond.gotmpl4j.parse;

/**
 * An {@code {{else}}} delimiter node separating the primary body of an {@code if},
 * {@code range}, or {@code with} action from its alternative body.
 */
public class ElseNode implements Node {

	@Override
	public String toString() {
		return "{{else}}";
	}

}
