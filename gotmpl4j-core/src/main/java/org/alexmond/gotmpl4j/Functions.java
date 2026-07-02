package org.alexmond.gotmpl4j;

import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Go's built-in {@code text/template} functions ({@link #GO_BUILTINS}) and the helpers
 * behind them — the base set every {@link GoTemplate} starts with, before any
 * {@link FunctionProvider}. Not instantiable.
 */
public final class Functions {

	/**
	 * Go built-in template functions (Go {@code text/template} builtins). Does not
	 * include Sprig or Helm functions — those are loaded via {@link FunctionProvider}
	 * implementations.
	 */
	public static final Map<String, Function> GO_BUILTINS;

	static {
		LinkedHashMap<String, Function> go = new LinkedHashMap<>();
		go.put("call", call());
		go.put("html", htmlEscape());
		go.put("index", index());
		go.put("slice", slice());
		go.put("js", jsEscape());
		go.put("len", len());
		go.put("print", print());
		go.put("printf", PrintfFunction.printf());
		go.put("println", println());
		go.put("urlquery", urlquery());

		// Logical operations
		go.put("and", and());
		go.put("or", or());
		go.put("not", not());

		// Comparisons
		go.put("eq", eq());
		go.put("ge", ge());
		go.put("gt", gt());
		go.put("le", le());
		go.put("lt", lt());
		go.put("ne", ne());

		GO_BUILTINS = Map.copyOf(go);
	}

	private Functions() {
	}

	private static Function index() {
		return (args) -> {
			if (args.length == 0) {
				return null;
			}
			// Go's `index x` with no keys returns x itself; only indexing keys descend.
			Object result = args[0];
			for (int i = 1; i < args.length; i++) {
				Object key = args[i];
				if (result instanceof Map) {
					if (key == null) {
						return null;
					}
					result = ((Map<?, ?>) result).get(key);
				}
				else if (result instanceof List) {
					result = ((List<?>) result).get(((Number) key).intValue());
				}
				else if (result != null && result.getClass().isArray()) {
					result = Array.get(result, ((Number) key).intValue());
				}
				else {
					return null;
				}
			}
			return result;
		};
	}

	private static Function len() {
		return (args) -> {
			if (args.length == 0 || args[0] == null) {
				return 0;
			}
			Object o = args[0];
			if (o instanceof Collection) {
				return ((Collection<?>) o).size();
			}
			if (o instanceof Map) {
				return ((Map<?, ?>) o).size();
			}
			if (o instanceof String string) {
				return string.length();
			}
			if (o.getClass().isArray()) {
				return Array.getLength(o);
			}
			return 0;
		};
	}

	private static Function and() {
		return (args) -> {
			for (Object arg : args) {
				if (!isTrue(arg)) {
					return arg;
				}
			}
			return (args.length > 0) ? args[args.length - 1] : false;
		};
	}

	private static Function or() {
		return (args) -> {
			for (Object arg : args) {
				if (isTrue(arg)) {
					return arg;
				}
			}
			return (args.length > 0) ? args[args.length - 1] : false;
		};
	}

	private static boolean valuesEqual(Object a, Object b) {
		if (a instanceof Number n1 && b instanceof Number n2) {
			return n1.doubleValue() == n2.doubleValue();
		}
		return Objects.equals(a, b);
	}

	private static Function eq() {
		return (args) -> {
			if (args.length < 2) {
				return false;
			}
			// Go's multi-arg eq compares the first argument to each of the rest and
			// returns true if it equals ANY of them (arg1==arg2 || arg1==arg3 || ...).
			Object first = args[0];
			for (int i = 1; i < args.length; i++) {
				if (valuesEqual(first, args[i])) {
					return true;
				}
			}
			return false;
		};
	}

	private static Function ne() {
		return (args) -> args.length >= 2 && !valuesEqual(args[0], args[1]);
	}

	private static Function lt() {
		return (args) -> {
			if (args.length < 2) {
				return false;
			}
			if (args[0] == null || args[1] == null) {
				return false;
			}

			// Handle numeric comparisons by converting to double
			if (args[0] instanceof Number n1 && args[1] instanceof Number n2) {
				double v1 = n1.doubleValue();
				double v2 = n2.doubleValue();
				return v1 < v2;
			}

			// Handle string and other comparable types
			if (!(args[0] instanceof Comparable)) {
				return false;
			}
			try {
				@SuppressWarnings("unchecked")
				Comparable<Object> comparable = (Comparable<Object>) args[0];
				return comparable.compareTo(args[1]) < 0;
			}
			catch (ClassCastException ex) {
				return false;
			}
		};
	}

	private static Function le() {
		return (args) -> {
			if (args.length < 2) {
				return false;
			}
			if (args[0] == null || args[1] == null) {
				return false;
			}

			// Handle numeric comparisons by converting to double
			if (args[0] instanceof Number n1 && args[1] instanceof Number n2) {
				double v1 = n1.doubleValue();
				double v2 = n2.doubleValue();
				return v1 <= v2;
			}

			// Handle string and other comparable types
			if (!(args[0] instanceof Comparable)) {
				return false;
			}
			try {
				@SuppressWarnings("unchecked")
				Comparable<Object> comparable = (Comparable<Object>) args[0];
				return comparable.compareTo(args[1]) <= 0;
			}
			catch (ClassCastException ex) {
				return false;
			}
		};
	}

	private static Function gt() {
		return (args) -> {
			if (args.length < 2) {
				return false;
			}
			if (args[0] == null || args[1] == null) {
				return false;
			}

			// Handle numeric comparisons by converting to double
			if (args[0] instanceof Number n1 && args[1] instanceof Number n2) {
				double v1 = n1.doubleValue();
				double v2 = n2.doubleValue();
				return v1 > v2;
			}

			// Handle string and other comparable types
			if (!(args[0] instanceof Comparable)) {
				return false;
			}
			try {
				@SuppressWarnings("unchecked")
				Comparable<Object> comparable = (Comparable<Object>) args[0];
				return comparable.compareTo(args[1]) > 0;
			}
			catch (ClassCastException ex) {
				return false;
			}
		};
	}

	private static Function ge() {
		return (args) -> {
			if (args.length < 2) {
				return false;
			}
			if (args[0] == null || args[1] == null) {
				return false;
			}

			// Handle numeric comparisons by converting to double
			if (args[0] instanceof Number n1 && args[1] instanceof Number n2) {
				double v1 = n1.doubleValue();
				double v2 = n2.doubleValue();
				return v1 >= v2;
			}

			// Handle string and other comparable types
			if (!(args[0] instanceof Comparable)) {
				return false;
			}
			try {
				@SuppressWarnings("unchecked")
				Comparable<Object> comparable = (Comparable<Object>) args[0];
				return comparable.compareTo(args[1]) >= 0;
			}
			catch (ClassCastException ex) {
				return false;
			}
		};
	}

	private static Function call() {
		return (args) -> {
			if (args.length == 0 || args[0] == null) {
				return null;
			}
			if (!(args[0] instanceof Function fn)) {
				throw new FunctionExecutionException("call: first argument must be a function");
			}
			Object[] fnArgs = new Object[args.length - 1];
			System.arraycopy(args, 1, fnArgs, 0, args.length - 1);
			return fn.invoke(fnArgs);
		};
	}

	private static Function htmlEscape() {
		return (args) -> {
			if (args.length == 0 || args[0] == null) {
				return "";
			}
			String s = String.valueOf(args[0]);
			StringBuilder sb = new StringBuilder(s.length());
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				switch (c) {
					case '&' -> sb.append("&amp;");
					case '<' -> sb.append("&lt;");
					case '>' -> sb.append("&gt;");
					case '"' -> sb.append("&#34;");
					case '\'' -> sb.append("&#39;");
					default -> sb.append(c);
				}
			}
			return sb.toString();
		};
	}

	private static Function jsEscape() {
		return (args) -> {
			if (args.length == 0 || args[0] == null) {
				return "";
			}
			String s = String.valueOf(args[0]);
			StringBuilder sb = new StringBuilder(s.length());
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				switch (c) {
					case '\\' -> sb.append("\\\\");
					case '\'' -> sb.append("\\'");
					case '"' -> sb.append("\\\"");
					case '\n' -> sb.append("\\n");
					case '\r' -> sb.append("\\r");
					case '\t' -> sb.append("\\t");
					case '<' -> sb.append("\\u003C");
					case '>' -> sb.append("\\u003E");
					case '&' -> sb.append("\\u0026");
					case '=' -> sb.append("\\u003D");
					// Go's JSEscapeString escapes any non-printable rune to \\uXXXX;
					// surrogates pass through so astral chars (e.g. emoji) stay intact.
					default -> sb.append((!Character.isSurrogate(c) && !isPrintableForJs(c))
							? String.format(Locale.ROOT, "\\u%04X", (int) c) : String.valueOf(c));
				}
			}
			return sb.toString();
		};
	}

	// Mirror Go's unicode.IsPrint: only the ASCII space is printable among separators;
	// control/format/surrogate/private-use/unassigned code points are not.
	private static boolean isPrintableForJs(char c) {
		if (c == ' ') {
			return true;
		}
		return switch (Character.getType(c)) {
			case Character.CONTROL, Character.FORMAT, Character.SURROGATE, Character.PRIVATE_USE, Character.UNASSIGNED,
					Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR, Character.SPACE_SEPARATOR ->
				false;
			default -> true;
		};
	}

	@SuppressWarnings("unchecked")
	private static Function slice() {
		return (args) -> {
			if (args.length == 0 || args[0] == null) {
				return null;
			}
			Object container = args[0];
			if (container instanceof String str) {
				// Go's slice also works on strings (a byte slice): slice "xyz" 1 2 ->
				// "y".
				int from = (args.length > 1) ? ((Number) args[1]).intValue() : 0;
				int to = (args.length > 2) ? ((Number) args[2]).intValue() : str.length();
				return str.substring(from, to);
			}
			int size;
			if (container instanceof List) {
				size = ((List<?>) container).size();
			}
			else if (container.getClass().isArray()) {
				size = Array.getLength(container);
			}
			else {
				return null;
			}
			int from = (args.length > 1) ? ((Number) args[1]).intValue() : 0;
			int to = (args.length > 2) ? ((Number) args[2]).intValue() : size;
			if (container instanceof List) {
				return new ArrayList<>(((List<Object>) container).subList(from, to));
			}
			Object[] result = new Object[to - from];
			for (int i = from; i < to; i++) {
				result[i - from] = Array.get(container, i);
			}
			return List.of(result);
		};
	}

	private static Function urlquery() {
		return (args) -> {
			if (args.length == 0 || args[0] == null) {
				return "";
			}
			return URLEncoder.encode(String.valueOf(args[0]), StandardCharsets.UTF_8);
		};
	}

	private static Function print() {
		// Go's fmt.Sprint inserts a space between two adjacent operands when neither is a
		// string. (Helm values are usually strings, so this rarely adds spaces in
		// practice.)
		return (args) -> {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < args.length; i++) {
				if (i > 0 && !(args[i - 1] instanceof String) && !(args[i] instanceof String)) {
					sb.append(' ');
				}
				sb.append(sprintValue(args[i]));
			}
			return sb.toString();
		};
	}

	private static Function println() {
		return (args) -> Arrays.stream(args).map(Functions::sprintValue).collect(Collectors.joining(" ")) + "\n";
	}

	private static Function not() {
		return (args) -> args.length > 0 && !isTrue(args[0]);
	}

	/**
	 * Convert a value to its string representation for print/println. Matches Go's
	 * {@code fmt.Sprint(nil)} which produces {@code "<nil>"} for nil values.
	 */
	static String sprintValue(Object value) {
		return GoFmt.sprint(value);
	}

	/**
	 * Reports Go's truthiness of a value, as used by {@code if}, {@code and}, {@code or}
	 * and {@code not}: {@code null} is false; a {@link Boolean} is itself; a
	 * {@link String}, {@link java.util.Collection}, {@link Map} or array is true when
	 * non-empty; a {@link Number} is true when non-zero; any other object is true.
	 * @param arg the value to test
	 * @return {@code true} if the value is considered "true" by Go's template rules
	 */
	public static boolean isTrue(Object arg) {
		if (arg == null) {
			return false;
		}
		if (arg instanceof Boolean boolValue) {
			return boolValue;
		}
		if (arg instanceof String string) {
			return !string.isEmpty();
		}
		if (arg instanceof Number number) {
			return number.doubleValue() != 0;
		}
		if (arg instanceof Collection) {
			return !((Collection<?>) arg).isEmpty();
		}
		if (arg instanceof Map) {
			return !((Map<?, ?>) arg).isEmpty();
		}
		return !arg.getClass().isArray() || Array.getLength(arg) > 0;
	}

}
