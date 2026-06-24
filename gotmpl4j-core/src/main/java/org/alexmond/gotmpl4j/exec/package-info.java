/**
 * Template executor: walks the parsed AST and writes rendered output.
 *
 * <p>
 * The {@code Executor} evaluates pipelines, resolves fields and variables against the
 * data model, invokes registered functions, and formats values the way Go's {@code fmt}
 * package does. Loop control is carried by {@code BreakSignal} and
 * {@code ContinueSignal}.
 *
 * <p>
 * <strong>Internal implementation detail.</strong> These types are {@code public} only
 * because of Java package boundaries; they are not part of the stable public API and may
 * change without notice between releases. Drive execution through
 * {@link org.alexmond.gotmpl4j.GoTemplate} or
 * {@link org.alexmond.gotmpl4j.CompiledTemplate}.
 */
package org.alexmond.gotmpl4j.exec;
