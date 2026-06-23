package org.alexmond.gotmpl4j;

/**
 * Controls how a nil or absent value is rendered by a bare action, mirroring Go's
 * {@code text/template} {@code missingkey} option. Set via
 * {@link GoTemplate#option(String...)}; the default is {@link #DEFAULT}, so Go
 * conformance is unchanged unless a caller opts in.
 */
public enum MissingKeyMode {

	/**
	 * Render Go's {@code <no value>} marker. This is Go's default and corresponds to
	 * {@code missingkey=default} and {@code missingkey=invalid}.
	 */
	DEFAULT,

	/**
	 * Render an empty string, corresponding to {@code missingkey=zero}. This matches
	 * Helm, whose templates expect a missing/nil value to render as {@code ""}.
	 */
	ZERO,

	/**
	 * Fail execution when a nil/absent value reaches a bare action, corresponding to
	 * {@code missingkey=error}.
	 */
	ERROR

}
