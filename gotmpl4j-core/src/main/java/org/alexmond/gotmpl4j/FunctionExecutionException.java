package org.alexmond.gotmpl4j;

/**
 * Unchecked exception thrown when a template function fails during execution, so
 * functions can signal failure without a checked-exception signature. Optionally carries
 * the function name for diagnostic context; the engine wraps it in a
 * {@link TemplateExecutionException} when it surfaces from
 * {@code execute}/{@code render}. Under the common {@link GoTemplateException} root.
 */
public class FunctionExecutionException extends GoTemplateException {

	private final String functionName;

	/**
	 * Creates a function-execution exception with no function name.
	 * @param message the detail message
	 */
	public FunctionExecutionException(String message) {
		super(message);
		this.functionName = null;
	}

	/**
	 * Creates a function-execution exception with no function name.
	 * @param message the detail message
	 * @param cause the underlying cause
	 */
	public FunctionExecutionException(String message, Throwable cause) {
		super(message, cause);
		this.functionName = null;
	}

	/**
	 * Creates a function-execution exception for a named function. The function name is
	 * prepended to the message.
	 * @param functionName the name of the failing function
	 * @param message the detail message
	 * @param cause the underlying cause
	 */
	public FunctionExecutionException(String functionName, String message, Throwable cause) {
		super(functionName + ": " + message, cause);
		this.functionName = functionName;
	}

	/**
	 * Returns the name of the function that failed.
	 * @return the function name, or {@code null} if not supplied
	 */
	public String getFunctionName() {
		return this.functionName;
	}

}
