/**
 * Internal string, character, and I/O helper utilities used by the engine.
 *
 * <p>
 * Includes Go-compatible string quoting and escaping, character classification constants,
 * reader-draining helpers, and an arbitrary-precision complex-number type, kept here to
 * avoid third-party dependencies.
 *
 * <p>
 * <strong>Internal implementation detail.</strong> These types are {@code public} only
 * because of Java package boundaries; they are not part of the stable public API and may
 * change without notice between releases.
 */
package org.alexmond.gotmpl4j.util;
