package org.alexmond.gotmpl4j;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the lazy scope-frame allocation (#115): a declaration-free block keeps the
 * shared empty frame (popped as a no-op), and a block that declares a variable promotes
 * the frame to a real list and unwinds it on exit — so a block-scoped {@code :=}
 * redeclaration restores the outer binding.
 */
class ScopeFrameTest {

	@Test
	void blockScopedRedeclarationUnwindsToOuterBinding() {
		GoTemplate t = new GoTemplate();
		// Outer $v, then each range iteration redeclares $v (block-scoped) — after the
		// range the outer binding must be restored.
		t.parse("t", "{{ $v := \"out\" }}{{ range .items }}{{ $v := . }}{{ $v }} {{ end }}{{ $v }}");

		Map<String, Object> data = Map.of("items", List.of("a", "b", "c"));
		assertEquals("a b c out", t.render(data));
	}

	@Test
	void declarationFreeBlocksRenderCorrectly() {
		GoTemplate t = new GoTemplate();
		// with + nested if, neither declares a variable — the empty-frame fast path.
		t.parse("t", "{{ with .m }}{{ if .on }}{{ .v }}{{ end }}{{ end }}");

		Map<String, Object> data = Map.of("m", Map.of("on", true, "v", "X"));
		assertEquals("X", t.render(data));
	}

}
