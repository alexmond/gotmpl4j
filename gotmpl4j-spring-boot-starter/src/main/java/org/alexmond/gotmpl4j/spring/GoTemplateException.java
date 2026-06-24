package org.alexmond.gotmpl4j.spring;

/**
 * Unchecked wrapper for the engine's checked template exceptions, so
 * {@link GoTemplateService#render} stays convenient to call from Spring code.
 */
public class GoTemplateException extends RuntimeException {

	/**
	 * Creates the exception.
	 * @param message the detail message
	 * @param cause the underlying checked template exception
	 */
	public GoTemplateException(String message, Throwable cause) {
		super(message, cause);
	}

}
