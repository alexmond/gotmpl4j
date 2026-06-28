package org.alexmond.gotmpl4j;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Formats values the way Go's {@code fmt} package does, so template output matches
 * {@code helm template}. Helm loads chart values via JSON, where every number is a
 * {@code float64}; rendering such a value to text (directly, or through {@code quote},
 * {@code toString}, {@code printf %v}, {@code print}, …) therefore uses Go's float
 * formatting, which switches to scientific notation for magnitudes {@code >= 1e6} (and
 * {@code < 1e-4}). Integer types ({@code int} results of
 * {@code int}/{@code len}/literals) render as plain decimals.
 *
 * <p>
 * This is distinct from {@code toYaml}, whose numbers follow {@code sigs.k8s.io/yaml}
 * (JSON) formatting and never use scientific notation; that path must not call here.
 */
public final class GoFmt {

	private GoFmt() {
	}

	/**
	 * Formats a value the way Go's {@code fmt.Sprint}/{@code %v} does, recursively: a map
	 * renders as {@code map[k1:v1 k2:v2]} with keys sorted (Go sorts map keys for stable
	 * output), a slice/array as {@code [e1 e2 e3]}, {@code nil} as {@code <nil>}, numbers
	 * via {@link #number}, and everything else via {@link String#valueOf}. Used by
	 * {@code toString}/{@code print}/{@code quote}/… so e.g. {@code toString} of a map
	 * matches {@code helm template} instead of Java's {@code {k=v}}.
	 * @param value the value
	 * @return the Go-formatted string
	 */
	public static String sprint(Object value) {
		if (value == null) {
			return "<nil>";
		}
		if (value instanceof Number n) {
			return number(n);
		}
		if (value instanceof Map<?, ?> map) {
			List<Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
			entries.sort(Comparator.comparing((e) -> String.valueOf(e.getKey())));
			StringBuilder sb = new StringBuilder("map[");
			for (int i = 0; i < entries.size(); i++) {
				if (i > 0) {
					sb.append(' ');
				}
				sb.append(sprint(entries.get(i).getKey())).append(':').append(sprint(entries.get(i).getValue()));
			}
			return sb.append(']').toString();
		}
		if (value instanceof Collection<?> coll) {
			StringBuilder sb = new StringBuilder("[");
			boolean first = true;
			for (Object e : coll) {
				if (!first) {
					sb.append(' ');
				}
				sb.append(sprint(e));
				first = false;
			}
			return sb.append(']').toString();
		}
		if (value.getClass().isArray()) {
			StringBuilder sb = new StringBuilder("[");
			int len = Array.getLength(value);
			for (int i = 0; i < len; i++) {
				if (i > 0) {
					sb.append(' ');
				}
				sb.append(sprint(Array.get(value, i)));
			}
			return sb.append(']').toString();
		}
		return String.valueOf(value);
	}

	/**
	 * Formats a number the way Go's {@code fmt.Sprint} would: a floating-point value
	 * ({@link Double}/{@link Float}) uses {@link #floatString}, any other number renders
	 * as a plain decimal.
	 * @param number the number
	 * @return the Go-formatted string
	 */
	public static String number(Number number) {
		if (number instanceof Double d) {
			return floatString(d);
		}
		if (number instanceof Float f) {
			return floatString(f);
		}
		return String.valueOf(number);
	}

	/**
	 * Renders a {@code double} the way Go's {@code fmt} prints a {@code float64} (the
	 * shortest %g form): scientific notation when the decimal exponent is {@code < -4} or
	 * {@code >= 6} (e.g. {@code 1000000 -> 1e+06}, {@code 0.00001 -> 1e-05}), otherwise a
	 * plain decimal with no trailing zeros ({@code 3.0 -> 3}, {@code 999999 -> 999999}).
	 * @param d the value
	 * @return the Go-formatted string
	 */
	public static String floatString(double d) {
		if (Double.isNaN(d)) {
			return "NaN";
		}
		if (Double.isInfinite(d)) {
			return (d > 0) ? "+Inf" : "-Inf";
		}
		if (d == 0.0) {
			return "0";
		}
		boolean neg = d < 0;
		// Double.toString already yields the shortest round-tripping decimal; parse its
		// digits and exponent directly instead of routing through BigDecimal/BigInteger
		// (a
		// hot-path allocation sink). Its output is either "i.f" or "d.fE±exp". Pull the
		// significant digits into one small char buffer and let renderDigits emit them
		// into
		// a single presized StringBuilder, avoiding the intermediate substring/concat
		// churn
		// that dominated this method's allocation (the mantissa/combined/digits
		// temporaries).
		String s = Double.toString(Math.abs(d));
		int len = s.length();
		int eIdx = s.indexOf('E');
		int javaExp = (eIdx >= 0) ? Integer.parseInt(s, eIdx + 1, len, 10) : 0;
		int mantEnd = (eIdx >= 0) ? eIdx : len;
		// digits[] holds the mantissa's digit characters (the '.' skipped); dotLogical is
		// how many of them sit left of the decimal point.
		char[] digits = new char[mantEnd];
		int n = 0;
		int dotLogical = mantEnd;
		for (int i = 0; i < mantEnd; i++) {
			char c = s.charAt(i);
			if (c == '.') {
				dotLogical = n;
			}
			else {
				digits[n++] = c;
			}
		}
		// Position of the decimal point, counted in digits from the left of digits[0..n).
		int pointPos = dotLogical + javaExp;
		// Strip leading zeros (each shifts the point left) and trailing zeros.
		int lead = 0;
		while (lead < n - 1 && digits[lead] == '0') {
			lead++;
		}
		pointPos -= lead;
		int end = n;
		while (end > lead + 1 && digits[end - 1] == '0') {
			end--;
		}
		return renderDigits(neg, digits, lead, end - lead, pointPos);
	}

	/**
	 * Emits the trimmed significant digits {@code digits[lead, lead+dlen)} in Go's %g
	 * form: scientific notation when the decimal exponent ({@code pointPos - 1}) is
	 * {@code < -4} or {@code >= 6}, otherwise plain decimal. The StringBuilder is
	 * presized to a safe upper bound so it never grows.
	 */
	private static String renderDigits(boolean neg, char[] digits, int lead, int dlen, int pointPos) {
		int exp = pointPos - 1;
		StringBuilder sb = new StringBuilder(dlen + Math.abs(pointPos) + 8);
		if (neg) {
			sb.append('-');
		}
		if (exp < -4 || exp >= 6) {
			sb.append(digits[lead]);
			if (dlen > 1) {
				sb.append('.').append(digits, lead + 1, dlen - 1);
			}
			sb.append('e').append((exp < 0) ? '-' : '+');
			int absExp = Math.abs(exp);
			if (absExp < 10) {
				sb.append('0');
			}
			sb.append(absExp);
		}
		else if (pointPos <= 0) {
			sb.append("0.");
			for (int i = 0; i < -pointPos; i++) {
				sb.append('0');
			}
			sb.append(digits, lead, dlen);
		}
		else if (pointPos >= dlen) {
			sb.append(digits, lead, dlen);
			for (int i = 0; i < pointPos - dlen; i++) {
				sb.append('0');
			}
		}
		else {
			sb.append(digits, lead, pointPos).append('.').append(digits, lead + pointPos, dlen - pointPos);
		}
		return sb.toString();
	}

}
