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
 * The {@link org.alexmond.gotmpl4j.parse.Node} hierarchy reachable from
 * {@link org.alexmond.gotmpl4j.GoTemplate#getRootNodes()} is a low-level extension point
 * — higher-level engines (such as Helm's {@code tpl}/{@code include}) register and share
 * parsed templates through it — so {@code Node} is part of the committed surface. The
 * lexer, parser, {@code Token}, and the concrete node internals, however, are
 * implementation detail and may change between releases; application code should drive
 * the engine through {@link org.alexmond.gotmpl4j.GoTemplate}.
 */
package org.alexmond.gotmpl4j.parse;
