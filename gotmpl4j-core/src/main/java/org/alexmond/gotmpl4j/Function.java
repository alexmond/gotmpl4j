package org.alexmond.gotmpl4j;

/**
 * A template function: the Java implementation behind a Go-template function name (e.g.
 * {@code printf}, {@code upper}). Functions receive their pipeline arguments as an
 * {@code Object} array and return the value to be used by the next stage or rendered.
 * Register implementations through a {@link FunctionProvider} or
 * {@link GoTemplate.Builder#withFunctions(java.util.Map)}.
 *
 * <p>
 * Implementations may throw {@link FunctionExecutionException} (an unchecked exception)
 * to signal failure; the engine wraps it as a {@link TemplateExecutionException}.
 *
 * @since 1.0
 */
@FunctionalInterface
public interface Function {

	/**
	 * Invokes the function with the evaluated pipeline arguments.
	 * @param args the arguments passed in the template (may be empty)
	 * @return the result value, used by the next pipeline stage or rendered to output
	 */
	Object invoke(Object... args);

}
