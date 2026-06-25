package org.alexmond.gotmpl4j;

/**
 * Thrown when template text cannot be parsed (lexing or syntax errors). May carry the
 * source line and column of the failure via {@link TemplateException}. Subclass of
 * {@link TemplateException}.
 *
 * @since 1.0
 */
public class TemplateParseException extends TemplateException {

	/**
	 * Creates a parse exception.
	 * @param message the detail message
	 */
	public TemplateParseException(String message) {
		super(message);
	}

	/**
	 * Creates a parse exception wrapping an underlying cause.
	 * @param message the detail message
	 * @param cause the underlying cause
	 */
	public TemplateParseException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates a parse exception located at the given source position.
	 * @param message the detail message
	 * @param line the 1-based line number, or a non-positive value if unknown
	 * @param column the 1-based column number, or a non-positive value if unknown
	 */
	public TemplateParseException(String message, int line, int column) {
		super(message, line, column);
	}

}
