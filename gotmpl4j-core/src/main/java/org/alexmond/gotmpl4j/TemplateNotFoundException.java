package org.alexmond.gotmpl4j;

/**
 * Thrown when a template is requested by a name that is not present in the
 * {@link GoTemplate} set, or when execution is attempted before any main template has
 * been parsed. Subclass of {@link TemplateException}.
 *
 * @since 1.0
 */
public class TemplateNotFoundException extends TemplateException {

	/**
	 * Creates a not-found exception.
	 * @param message the detail message
	 */
	public TemplateNotFoundException(String message) {
		super(message);
	}

	/**
	 * Creates a not-found exception wrapping an underlying cause.
	 * @param message the detail message
	 * @param cause the underlying cause
	 */
	public TemplateNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
