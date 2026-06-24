/**
 * Lexer, parser, and abstract syntax tree (AST) node types for the Go template grammar.
 *
 * <p>
 * The {@code Lexer} tokenizes template source into
 * {@link org.alexmond.gotmpl4j.parse.Token}s, and the {@code Parser} assembles those
 * tokens into a tree of {@link org.alexmond.gotmpl4j.parse.Node} instances (action, pipe,
 * command, branch, and literal nodes) that the executor walks at render time.
 *
 * <p>
 * <strong>Internal implementation detail.</strong> These types are {@code public} only
 * because of Java package boundaries; they are not part of the stable public API and may
 * change without notice between releases. Application code should depend on
 * {@link org.alexmond.gotmpl4j.GoTemplate} instead.
 */
package org.alexmond.gotmpl4j.parse;
