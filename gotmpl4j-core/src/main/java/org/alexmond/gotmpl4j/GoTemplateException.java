package org.alexmond.gotmpl4j;

/**
 * Root of every exception the engine throws. All template failures &mdash; parse,
 * execution, missing-template, and function errors &mdash; extend this type, so a caller
 * can {@code catch (GoTemplateException)} to handle anything the engine can raise.
 *
 * <p>
 * It is unchecked ({@link RuntimeException}), mirroring Go's {@code text/template}, which
 * surfaces these as returned {@code error} values rather than forcing a throws signature
 * on every call. Subtypes such as {@link TemplateParseException},
 * {@link TemplateExecutionException}, {@link TemplateNotFoundException}, and
 * {@link FunctionExecutionException} narrow the failure; catch them individually when you
 * need to distinguish, or this root to catch them all.
 */
public class GoTemplateException extends RuntimeException {

	/**
	 * Creates the exception.
	 * @param message the detail message
	 */
	public GoTemplateException(String message) {
		super(message);
	}

	/**
	 * Creates the exception with an underlying cause.
	 * @param message the detail message
	 * @param cause the underlying cause
	 */
	public GoTemplateException(String message, Throwable cause) {
		super(message, cause);
	}

}
