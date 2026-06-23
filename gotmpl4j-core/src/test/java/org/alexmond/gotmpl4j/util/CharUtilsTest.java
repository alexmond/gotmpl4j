package org.alexmond.gotmpl4j.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class CharUtilsTest {

	@Test
	void unquoteChar() {
		assertThrows(IllegalArgumentException.class, () -> CharUtils.unquoteChar("\\'x"));
		assertThrows(IllegalArgumentException.class, () -> CharUtils.unquoteChar("'xx'"));
	}

	static Stream<Arguments> validCharProvider() {
		// Expected code points verified against go1.23.4 strconv.UnquoteChar.
		return Stream.of(Arguments.of("'a'", 97), Arguments.of("'z'", 122), Arguments.of("'1'", 49),
				// non-ASCII rune
				Arguments.of("'é'", 233),
				// one-character escapes
				Arguments.of("'\\n'", 10), Arguments.of("'\\t'", 9), Arguments.of("'\\a'", 7),
				Arguments.of("'\\v'", 11), Arguments.of("'\\\\'", 92), Arguments.of("'\\''", 39),
				// hex, octal, and Unicode escapes
				Arguments.of("'\\x41'", 65), Arguments.of("'\\101'", 65), Arguments.of("'\\u00e9'", 233),
				Arguments.of("'\\U0001F600'", 0x1F600));
	}

	@ParameterizedTest
	@MethodSource("validCharProvider")
	void unquoteCharValid(String input, int expected) {
		assertEquals(expected, CharUtils.unquoteChar(input));
	}

	@ParameterizedTest
	// too many runes, unknown escape, incomplete hex, octal > 255, code point > U+10FFFF
	@ValueSource(strings = { "'abc'", "'ab'", "'\\z'", "'\\\"'", "'\\x4'", "'\\400'", "'\\U00110000'" })
	void unquoteCharInvalid(String input) {
		assertThrows(IllegalArgumentException.class, () -> CharUtils.unquoteChar(input));
	}

}
