package org.alexmond.gotmpl4j.parse;

import org.alexmond.gotmpl4j.TemplateParseException;
import org.alexmond.gotmpl4j.util.CharUtils;
import org.alexmond.gotmpl4j.util.Complex;

/**
 * Parses numeric tokens (integers, floats, complex numbers, character constants) into
 * {@link NumberNode} AST nodes.
 */
final class NumberParser {

	private NumberParser() {
	}

	static NumberNode parse(Token token) throws TemplateParseException {
		NumberNode numberNode = new NumberNode(token.value());
		parseInto(numberNode, token);
		return numberNode;
	}

	private static void parseInto(NumberNode numberNode, Token token) throws TemplateParseException {
		String text = token.value();
		TokenType type = token.type();
		if (type == TokenType.CHAR_CONSTANT) {
			parseCharConstant(numberNode, text);
			return;
		}

		if (type == TokenType.COMPLEX) {
			try {
				Complex complex = Complex.parseComplex(text);
				numberNode.setIsComplex(true);
				numberNode.setComplex(complex);
				simplifyComplex(numberNode, complex);
				return;
			}
			catch (NumberFormatException ignored) {
			}
		}

		int length = text.length();
		if (length > 0 && text.charAt(length - 1) == 'i') {
			try {
				Complex complex = Complex.parseComplex(text);
				numberNode.setIsComplex(true);
				numberNode.setComplex(complex);
				simplifyComplex(numberNode, complex);
				return;
			}
			catch (NumberFormatException ignored) {
			}
		}

		try {
			long intValue = parseIntValue(text);
			numberNode.setIsInt(true);
			numberNode.setIntValue(intValue);
			numberNode.setIsFloat(true);
			numberNode.setFloatValue(intValue);
		}
		catch (NumberFormatException ignored) {
			if (looksLikeInteger(text)) {
				// An integer-form literal that does not fit in a 64-bit long. Go rejects
				// these rather than silently widening to a lossy float.
				throw new TemplateParseException(String.format("integer overflow: %s, line: %d, column: %d", text,
						token.line(), token.column()));
			}
			try {
				double floatValue = parseFloatValue(text);
				numberNode.setIsFloat(true);
				numberNode.setFloatValue(floatValue);
				simplifyFloat(numberNode, floatValue);
			}
			catch (NumberFormatException ignoredAgain) { // NOPMD EmptyCatchBlock - not a
															// float either; falls through
															// to validation below
			}
		}

		if (!numberNode.isInt() && !numberNode.isFloat() && !numberNode.isComplex()) {
			throw new TemplateParseException(String.format("illegal number syntax: %s, line: %d, column: %d", text,
					token.line(), token.column()));
		}
	}

	private static void parseCharConstant(NumberNode numberNode, String text) throws TemplateParseException {
		if (text.charAt(0) != '\'') {
			throw new TemplateParseException(String.format("malformed character constant: %s", text));
		}

		int ch;
		try {
			ch = CharUtils.unquoteChar(text);
		}
		catch (IllegalArgumentException ex) {
			throw new TemplateParseException("invalid syntax: " + text, ex);
		}

		numberNode.setIsInt(true);
		numberNode.setIntValue(ch);
		numberNode.setIsFloat(true);
		numberNode.setFloatValue(ch);
	}

	/**
	 * True when {@code text} is an integer-form literal (optional sign, then
	 * decimal/hex/octal/binary digits) with no float markers — so a failure to parse it
	 * as a {@code long} means it overflowed rather than that it is a float.
	 */
	private static boolean looksLikeInteger(String text) {
		String s = text;
		if (!s.isEmpty() && (s.charAt(0) == '+' || s.charAt(0) == '-')) {
			s = s.substring(1);
		}
		if (s.isEmpty() || s.indexOf('.') >= 0) {
			return false;
		}
		boolean hex = s.length() > 1 && s.charAt(0) == '0' && (s.charAt(1) == 'x' || s.charAt(1) == 'X');
		if (hex) {
			// hex floats use a p/P exponent; e/E are valid hex digits
			return s.indexOf('p') < 0 && s.indexOf('P') < 0;
		}
		// decimal/octal/binary integers; an e/E exponent would make it a float
		return s.indexOf('e') < 0 && s.indexOf('E') < 0;
	}

	private static long parseIntValue(String text) {
		boolean signed = false;
		boolean negative = false;

		char firstChar = text.charAt(0);
		if (firstChar == '+') {
			signed = true;
		}
		if (firstChar == '-') {
			signed = true;
			negative = true;
		}

		if (signed && text.length() > 1) {
			char secondChar = text.charAt(1);
			if (secondChar == '+' || secondChar == '-') {
				throw new NumberFormatException("invalid number: multiple sign characters");
			}
		}

		long intValue = parseIntWithBase(text, signed);
		return (negative) ? -intValue : intValue;
	}

	private static long parseIntWithBase(String text, boolean signed) {
		int offset = (signed) ? 1 : 0;
		String trimmed = trimUnderscore(text);

		if (text.startsWith("0b", offset) || text.startsWith("0B", offset)) {
			return Long.parseLong(trimmed.substring(offset + 2), 2);
		}

		if (text.startsWith("0o", offset) || text.startsWith("0O", offset)) {
			return Long.parseLong(trimmed.substring(offset + 2), 8);
		}

		if (text.startsWith("0x", offset) || text.startsWith("0X", offset)) {
			return Long.parseLong(trimmed.substring(offset + 2), 16);
		}

		String body = trimmed.substring(offset);
		// Go's legacy octal form: a leading 0 followed only by octal digits (0377 ==
		// 255).
		// The 0b/0o/0x prefixes above are handled first; a lone "0" stays decimal.
		if (isLegacyOctal(body)) {
			return Long.parseLong(body, 8);
		}

		return Long.parseLong(body, 10);
	}

	private static boolean isLegacyOctal(String body) {
		if (body.length() < 2 || body.charAt(0) != '0') {
			return false;
		}
		for (int i = 1; i < body.length(); i++) {
			char c = body.charAt(i);
			if (c < '0' || c > '7') {
				return false;
			}
		}
		return true;
	}

	private static double parseFloatValue(String text) {
		return Double.parseDouble(trimUnderscore(text));
	}

	private static void simplifyComplex(NumberNode numberNode, Complex complex) {
		if (complex.getImaginary() == 0) {
			double floatValue = complex.getReal();
			numberNode.setIsFloat(true);
			numberNode.setFloatValue(floatValue);
			simplifyFloat(numberNode, floatValue);
		}
	}

	private static void simplifyFloat(NumberNode numberNode, double floatValue) {
		long intValue = (long) floatValue;
		if (floatValue == intValue) {
			numberNode.setIsInt(true);
			numberNode.setIntValue(intValue);
		}
	}

	private static String trimUnderscore(String text) {
		return text.replace("_", "");
	}

}
