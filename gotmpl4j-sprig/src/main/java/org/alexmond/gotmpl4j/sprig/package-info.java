/**
 * Sprig function library for gotmpl4j, a Java port of the Go
 * <a href="https://masterminds.github.io/sprig/">Sprig</a> template functions.
 *
 * <p>
 * {@link org.alexmond.gotmpl4j.sprig.SprigFunctionProvider} is a
 * {@link org.alexmond.gotmpl4j.FunctionProvider} (priority {@code 100}) discovered
 * automatically via {@link java.util.ServiceLoader} when this module is on the classpath,
 * making the full Sprig function set (strings, lists, dicts, math, crypto, date, semver,
 * encoding, and more) available to every {@link org.alexmond.gotmpl4j.GoTemplate}. The
 * functions themselves are assembled by
 * {@link org.alexmond.gotmpl4j.sprig.SprigFunctionsRegistry}.
 */
package org.alexmond.gotmpl4j.sprig;
