/**
 * Public API for gotmpl4j, a pure-Java implementation of Go's {@code text/template}
 * engine.
 *
 * <p>
 * The primary entry point is {@link org.alexmond.gotmpl4j.GoTemplate}: construct one (the
 * no-arg constructor auto-discovers {@link org.alexmond.gotmpl4j.FunctionProvider}
 * implementations on the classpath, such as Sprig, via {@link java.util.ServiceLoader},
 * or use {@code GoTemplate.builder()} for explicit control), parse one or more named
 * templates, then execute them against a data model.
 * {@link org.alexmond.gotmpl4j.CompiledTemplate} is a thread-safe, reusable handle to a
 * single named template.
 *
 * <p>
 * Functions are pluggable: implement {@link org.alexmond.gotmpl4j.Function} and
 * contribute them through a {@link org.alexmond.gotmpl4j.FunctionProvider}. Rendering
 * behavior is tuned with {@link org.alexmond.gotmpl4j.MissingKeyMode}. All recoverable
 * failures are reported as subclasses of {@link org.alexmond.gotmpl4j.TemplateException}.
 */
package org.alexmond.gotmpl4j;
