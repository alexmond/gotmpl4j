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
 * {@link org.alexmond.gotmpl4j.GoTemplate#getRootNodes()} is an advanced extension point
 * that higher-level engines (such as Helm's {@code tpl}/{@code include}) use to register
 * and share parsed templates. Along with that getter, it is an integrator surface that is
 * <strong>not covered by the 1.0 API-stability guarantee</strong> and may change in a
 * minor release (see issue #48). The lexer, parser, {@code Token}, and the concrete node
 * internals are likewise implementation detail; application code should drive the engine
 * through {@link org.alexmond.gotmpl4j.GoTemplate}'s
 * {@code parse}/{@code execute}/{@code render}.
 */
package org.alexmond.gotmpl4j.parse;
