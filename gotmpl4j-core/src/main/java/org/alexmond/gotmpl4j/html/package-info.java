/**
 * Contextual auto-escaping, a port of Go's {@code html/template} package.
 *
 * <p>
 * This package tracks the HTML/CSS/JS/URL context of each template action and applies the
 * appropriate escaping so output is safe against injection, mirroring Go's state machine,
 * transition tables, escapers, and typed {@code Content} (safe-string) wrappers.
 *
 * <p>
 * <strong>Internal implementation detail.</strong> These types are {@code public} only
 * because of Java package boundaries; they are not part of the stable public API and may
 * change without notice between releases. HTML escaping is enabled through
 * {@link org.alexmond.gotmpl4j.GoTemplate}.
 */
package org.alexmond.gotmpl4j.html;
