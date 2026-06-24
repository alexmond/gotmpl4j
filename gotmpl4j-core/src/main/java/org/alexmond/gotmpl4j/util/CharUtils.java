package org.alexmond.gotmpl4j.util;

/**
 * Character classification helpers and named character constants (EOF, whitespace, and
 * other delimiters) used by the lexer. Not instantiable.
 */
public final class CharUtils {

	public static final char EOF = (char) -1;

	public static final char SPACE = ' ';

	public static final char TAB = '\t';

	public static final char RETURN = '\r';

	public static final char NEW_LINE = '\n';

	public static final char UNDERSCORE = '_';

	public static final String DECIMAL_DIGITS = "0123456789_";

	public static final String HEX_DIGITS = "0123456789abcdefABCDEF_";

	public static final String OCTET_DIGITS = "01234567_";

	public static final String BINARY_DIGITS = "01_";

	private CharUtils() {
	}

	public static boolean isSpace(char ch) {
		return ch == SPACE || ch == TAB || ch == RETURN || isNewline(ch);
	}

	public static boolean isNewline(char ch) {
		return ch == NEW_LINE;
	}

	public static boolean isAscii(char ch) {
		return ch < 0x7F;
	}

	public static boolean isVisible(char ch) {
		Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
		return !Character.isISOControl(ch) && block != null && block != Character.UnicodeBlock.SPECIALS;
	}

	/**
	 * Is number a single digit in 0-9 ?
	 * @param ch Character to be checked
	 * @return true if character is a single digit in 0-9
	 */
	public static boolean isNumeric(char ch) {
		return '0' <= ch && ch <= '9';
	}

	/**
	 * Is letter of '_' or a unicode letter or unicode digit?
	 * @param ch Character to be checked
	 * @return true if letter is '_' or a unicode letter or unicode digit
	 */
	public static boolean isAlphabetic(char ch) {
		return ch == UNDERSCORE || Character.isLetterOrDigit(ch);
	}

	/**
	 * Check if single character is one of the characters you expect
	 * @param ch Character to be checked
	 * @param chars An array including characters which you expect
	 * @return true if ch is what you want
	 */
	public static boolean isAnyOf(char ch, char... chars) {
		for (char c : chars) {
			if (c == ch) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if single character is one of the characters you expect
	 * <p>
	 * This is same with {@link CharUtils#isAnyOf(char, char...)} but accept a
	 * CharSequence to indicate expected characters
	 * @param ch Character to be checked
	 * @param chars A CharSequence including characters which you expect
	 * @return true if ch is what you want
	 */
	public static boolean isAnyOf(char ch, CharSequence chars) {
		return chars.chars().anyMatch((c) -> c == ch);
	}

	/**
	 * Decodes a Go single-quoted rune literal to its code point, following Go's
	 * strconv.UnquoteChar. Supports a single rune, the standard one-character escapes
	 * (such as bell, backspace, form-feed, newline, return, tab, vertical-tab, backslash
	 * and single-quote), a two-digit hex escape, four- and eight-digit Unicode escapes,
	 * and a three-digit octal escape.
	 * @param text the quoted character literal
	 * @return the rune as a Unicode code point
	 * @throws IllegalArgumentException if the literal is malformed
	 */
	public static int unquoteChar(String text) throws IllegalArgumentException {
		if (text.length() < 3 || text.charAt(0) != '\'' || text.charAt(text.length() - 1) != '\'') {
			throw new IllegalArgumentException("invalid syntax: " + text);
		}
		String body = text.substring(1, text.length() - 1);
		if (body.charAt(0) != '\\') {
			int cp = body.codePointAt(0);
			if (body.length() != Character.charCount(cp)) {
				throw new IllegalArgumentException("invalid character literal: " + text);
			}
			return cp;
		}
		if (body.length() < 2) {
			throw new IllegalArgumentException("invalid escape: " + text);
		}
		char esc = body.charAt(1);
		switch (esc) {
			case 'a':
				return single(text, body, 0x07);
			case 'b':
				return single(text, body, 0x08);
			case 'f':
				return single(text, body, 0x0C);
			case 'n':
				return single(text, body, 0x0A);
			case 'r':
				return single(text, body, 0x0D);
			case 't':
				return single(text, body, 0x09);
			case 'v':
				return single(text, body, 0x0B);
			case '\\':
				return single(text, body, 0x5C);
			case '\'':
				return single(text, body, 0x27);
			case 'x':
				return hexEscape(text, body, 2);
			case 'u':
				return hexEscape(text, body, 4);
			case 'U':
				return hexEscape(text, body, 8);
			default:
				if (esc >= '0' && esc <= '7') {
					return octalEscape(text, body);
				}
				throw new IllegalArgumentException("unknown escape: " + text);
		}
	}

	private static int single(String text, String body, int value) {
		if (body.length() != 2) {
			throw new IllegalArgumentException("invalid escape: " + text);
		}
		return value;
	}

	private static int hexEscape(String text, String body, int digits) {
		if (body.length() != 2 + digits) {
			throw new IllegalArgumentException("invalid escape: " + text);
		}
		int value = 0;
		for (int i = 2; i < body.length(); i++) {
			int d = Character.digit(body.charAt(i), 16);
			if (d < 0) {
				throw new IllegalArgumentException("invalid escape: " + text);
			}
			value = (value << 4) + d;
		}
		// The 2-digit hex form is a raw byte (unrestricted); the 4/8-digit forms denote
		// Unicode code points, so reject surrogates and anything past U+10FFFF.
		if (digits > 2 && (value > Character.MAX_CODE_POINT || (value >= 0xD800 && value <= 0xDFFF))) {
			throw new IllegalArgumentException("invalid Unicode code point: " + text);
		}
		return value;
	}

	private static int octalEscape(String text, String body) {
		if (body.length() != 4) {
			throw new IllegalArgumentException("invalid escape: " + text);
		}
		int value = 0;
		for (int i = 1; i < 4; i++) {
			char c = body.charAt(i);
			if (c < '0' || c > '7') {
				throw new IllegalArgumentException("invalid escape: " + text);
			}
			value = (value << 3) + (c - '0');
		}
		if (value > 0xFF) {
			throw new IllegalArgumentException("invalid escape: " + text);
		}
		return value;
	}

}
