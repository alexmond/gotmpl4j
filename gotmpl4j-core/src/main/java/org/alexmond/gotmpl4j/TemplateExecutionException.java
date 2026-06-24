package org.alexmond.gotmpl4j;

/**
 * Thrown when a parsed template fails during execution — for example a function raises an
 * error, a pipeline operates on an incompatible value, or {@code missingkey=error} is set
 * and a nil value reaches a bare action. Subclass of {@link TemplateException}.
 */
public class TemplateExecutionException extends TemplateException {

	/**
	 * Creates an execution exception.
	 * @param message the detail message
	 */
	public TemplateExecutionException(String message) {
		super(message);
	}

	/**
	 * Creates an execution exception wrapping an underlying cause.
	 * @param message the detail message
	 * @param cause the underlying cause
	 */
	public TemplateExecutionException(String message, Throwable cause) {
		super(message, cause);
	}

}
