package org.alexmond.gotmpl4j.sprig.functions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import lombok.extern.slf4j.Slf4j;

import org.alexmond.gotmpl4j.Function;
import org.alexmond.gotmpl4j.FunctionExecutionException;

/**
 * Date and time manipulation functions from Sprig library. Includes date formatting,
 * parsing, and manipulation operations.
 *
 * @see <a href="https://masterminds.github.io/sprig/date.html">Sprig Date Functions</a>
 */
@Slf4j
public final class DateFunctions {

	private DateFunctions() {
	}

	public static Map<String, Function> getFunctions() {
		Map<String, Function> functions = new HashMap<>();

		// Current time
		functions.put("now", now());

		// Date formatting
		functions.put("date", date());
		functions.put("dateInZone", dateInZone());
		functions.put("htmlDate", htmlDate());
		functions.put("htmlDateInZone", htmlDateInZone());

		// Date parsing
		functions.put("toDate", toDate());
		functions.put("mustToDate", mustToDate());

		// Date manipulation
		functions.put("dateModify", dateModify());
		functions.put("mustDateModify", mustDateModify());
		functions.put("durationRound", durationRound());
		functions.put("ago", ago());
		functions.put("duration", duration());

		// Unix epoch
		functions.put("unixEpoch", unixEpoch());

		// Underscore aliases
		functions.put("date_in_zone", dateInZone());
		functions.put("date_modify", dateModify());
		functions.put("must_date_modify", mustDateModify());

		return functions;
	}

	// ========== Current Time Functions ==========

	/**
	 * Returns the current date/time.
	 */
	private static Function now() {
		return (args) -> Date.from(Instant.now());
	}

	// ========== Date Formatting Functions ==========

	/**
	 * Formats a date using the given layout. Go date format is converted to Java
	 * SimpleDateFormat.
	 */
	private static Function date() {
		return (args) -> {
			if (args.length < 2) {
				return "";
			}
			String layout = String.valueOf(args[0]);
			Object dateObj = args[1];

			Date date = convertToDate(dateObj);
			if (date == null) {
				return "";
			}

			String javaLayout = convertGoLayoutToJava(layout);
			try {
				SimpleDateFormat sdf = new SimpleDateFormat(javaLayout, Locale.ENGLISH);
				return sdf.format(date);
			}
			catch (Exception ex) {
				log.debug("date failed: {}", ex.getMessage());
				return "";
			}
		};
	}

	/**
	 * Formats a date using the given layout in a specific timezone.
	 */
	private static Function dateInZone() {
		return (args) -> {
			if (args.length < 3) {
				return "";
			}
			String layout = String.valueOf(args[0]);
			Object dateObj = args[1];
			String timezone = String.valueOf(args[2]);

			Date date = convertToDate(dateObj);
			if (date == null) {
				return "";
			}

			String javaLayout = convertGoLayoutToJava(layout);
			try {
				SimpleDateFormat sdf = new SimpleDateFormat(javaLayout, Locale.ENGLISH);
				sdf.setTimeZone(TimeZone.getTimeZone(timezone));
				return sdf.format(date);
			}
			catch (Exception ex) {
				log.debug("dateInZone failed: {}", ex.getMessage());
				return "";
			}
		};
	}

	/**
	 * Formats a date in HTML date format (YYYY-MM-DD).
	 */
	private static Function htmlDate() {
		return (args) -> {
			if (args.length == 0) {
				return "";
			}
			Date date = convertToDate(args[0]);
			if (date == null) {
				return "";
			}

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
			return sdf.format(date);
		};
	}

	/**
	 * Formats a date in HTML date format (YYYY-MM-DD) in a specific timezone.
	 */
	private static Function htmlDateInZone() {
		return (args) -> {
			if (args.length < 2) {
				return "";
			}
			Date date = convertToDate(args[0]);
			if (date == null) {
				return "";
			}

			String timezone = String.valueOf(args[1]);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
			sdf.setTimeZone(TimeZone.getTimeZone(timezone));
			return sdf.format(date);
		};
	}

	// ========== Date Parsing Functions ==========

	/**
	 * Parses a date string using the given layout.
	 * @return Date object or null on error
	 */
	private static Function toDate() {
		return (args) -> {
			if (args.length < 2) {
				return null;
			}
			String layout = String.valueOf(args[0]);
			String dateStr = String.valueOf(args[1]);

			String javaLayout = convertGoLayoutToJava(layout);
			try {
				SimpleDateFormat sdf = new SimpleDateFormat(javaLayout, Locale.ENGLISH);
				return sdf.parse(dateStr);
			}
			catch (ParseException ex) {
				log.debug("toDate failed: {}", ex.getMessage());
				return null;
			}
		};
	}

	/**
	 * Parses a date string using the given layout. Throws an exception on error.
	 * @return Date object
	 * @throws RuntimeException if parsing fails
	 */
	private static Function mustToDate() {
		return (args) -> {
			if (args.length < 2) {
				throw new FunctionExecutionException("mustToDate: insufficient arguments");
			}
			String layout = String.valueOf(args[0]);
			String dateStr = String.valueOf(args[1]);

			String javaLayout = convertGoLayoutToJava(layout);
			try {
				SimpleDateFormat sdf = new SimpleDateFormat(javaLayout, Locale.ENGLISH);
				return sdf.parse(dateStr);
			}
			catch (ParseException ex) {
				throw new FunctionExecutionException("mustToDate: failed to parse date: " + ex.getMessage(), ex);
			}
		};
	}

	// ========== Date Manipulation Functions ==========

	/**
	 * Modifies a date by adding or subtracting duration.
	 * <p>
	 * Simplified implementation - supports basic duration strings.
	 */
	private static Function dateModify() {
		return (args) -> {
			if (args.length < 2) {
				return null;
			}
			String modification = String.valueOf(args[0]);
			Date date = convertToDate(args[1]);
			if (date == null) {
				return null;
			}

			try {
				// Parse modification string (e.g., "+1h", "-30m", "+24h")
				long millisToAdd = parseDurationToMillis(modification);
				return new Date(date.getTime() + millisToAdd);
			}
			catch (Exception ex) {
				log.debug("dateModify failed: {}", ex.getMessage());
				return date;
			}
		};
	}

	private static Function mustDateModify() {
		return (args) -> {
			if (args.length < 2) {
				throw new FunctionExecutionException("mustDateModify: insufficient arguments");
			}
			String modification = String.valueOf(args[0]);
			Date date = convertToDate(args[1]);
			if (date == null) {
				throw new FunctionExecutionException("mustDateModify: invalid date");
			}
			try {
				long millisToAdd = parseDurationToMillis(modification);
				return new Date(date.getTime() + millisToAdd);
			}
			catch (Exception ex) {
				throw new FunctionExecutionException("mustDateModify: " + ex.getMessage(), ex);
			}
		};
	}

	private static Function ago() {
		return (args) -> {
			if (args.length == 0 || args[0] == null) {
				return "";
			}
			Date date = convertToDate(args[0]);
			if (date == null) {
				return "";
			}
			Duration d = Duration.between(date.toInstant(), Instant.now());
			return formatDuration(d);
		};
	}

	private static Function duration() {
		return (args) -> {
			if (args.length == 0 || args[0] == null) {
				return "0s";
			}
			long seconds;
			if (args[0] instanceof Number number) {
				seconds = number.longValue();
			}
			else {
				try {
					seconds = Long.parseLong(String.valueOf(args[0]).trim());
				}
				catch (NumberFormatException ex) {
					log.debug("duration failed: {}", ex.getMessage());
					return "0s";
				}
			}
			return formatDuration(Duration.ofSeconds(seconds));
		};
	}

	/**
	 * Rounds a duration to the nearest unit.
	 * <p>
	 * Simplified implementation.
	 * @return rounded duration
	 */
	private static Function durationRound() {
		return (args) -> {
			if (args.length == 0) {
				return "0s";
			}
			Object durationObj = args[0];

			long seconds;
			if (durationObj instanceof Duration duration) {
				seconds = duration.getSeconds();
			}
			else {
				try {
					seconds = parseDurationToMillis(String.valueOf(durationObj)) / 1000;
				}
				catch (Exception ex) {
					log.debug("durationRound failed: {}", ex.getMessage());
					return "0s";
				}
			}
			return roundDuration(seconds);
		};
	}

	// ========== Unix Epoch Functions ==========

	/**
	 * Returns the Unix epoch timestamp (seconds since Jan 1, 1970 UTC).
	 */
	private static Function unixEpoch() {
		return (args) -> {
			Date date;
			if (args.length == 0) {
				date = Date.from(Instant.now());
			}
			else {
				date = convertToDate(args[0]);
			}
			if (date == null) {
				return 0L;
			}
			return date.getTime() / 1000;
		};
	}

	// ========== Helper Methods ==========

	// Go reference-time tokens mapped to SimpleDateFormat pattern letters. The scanner in
	// convertGoLayoutToJava picks the longest token that matches at each position (so
	// "January" beats "Jan", "2006" beats "06", "15" beats "1"), exactly as Go's layout
	// parser does.
	private static final String[][] LAYOUT_TOKENS = { { "2006", "yyyy" }, { "06", "yy" }, { "January", "MMMM" },
			{ "Jan", "MMM" }, { "01", "MM" }, { "1", "M" }, { "Monday", "EEEE" }, { "Mon", "EEE" }, { "02", "dd" },
			{ "_2", "d" }, { "2", "d" }, { "15", "HH" }, { "03", "hh" }, { "3", "h" }, { "04", "mm" }, { "4", "m" },
			{ "05", "ss" }, { "5", "s" }, { "PM", "a" }, { "pm", "a" }, { "Z07:00", "XXX" }, { "Z0700", "XX" },
			{ "-07:00", "XXX" }, { "-0700", "Z" }, { "-07", "X" }, { "MST", "zzz" } };

	/**
	 * Converts a Go reference-time layout (e.g. {@code 2006-01-02 15:04:05}) to a
	 * {@link SimpleDateFormat} pattern. Unlike a flat search-and-replace, this scans the
	 * layout token by token (longest match wins) and quotes any literal text, so a layout
	 * containing letters that are pattern characters — {@code 15h04}, {@code Day 2006} —
	 * is not corrupted. A {@code .000}/{@code .999} run after the seconds becomes a
	 * fractional-second field.
	 */
	private static String convertGoLayoutToJava(String goLayout) {
		StringBuilder out = new StringBuilder();
		StringBuilder literal = new StringBuilder();
		int i = 0;
		int n = goLayout.length();
		while (i < n) {
			String java = null;
			int matchLen = 0;
			for (String[] token : LAYOUT_TOKENS) {
				if (token[0].length() > matchLen && goLayout.startsWith(token[0], i)) {
					java = token[1];
					matchLen = token[0].length();
				}
			}
			if (java != null) {
				flushLiteral(out, literal);
				out.append(java);
				i += matchLen;
			}
			else if (goLayout.charAt(i) == '.' && i + 1 < n
					&& (goLayout.charAt(i + 1) == '0' || goLayout.charAt(i + 1) == '9')) {
				char frac = goLayout.charAt(i + 1);
				int j = i + 1;
				while (j < n && goLayout.charAt(j) == frac) {
					j++;
				}
				flushLiteral(out, literal);
				out.append('.').append("S".repeat(j - (i + 1)));
				i = j;
			}
			else {
				literal.append(goLayout.charAt(i));
				i++;
			}
		}
		flushLiteral(out, literal);
		return out.toString();
	}

	// Emits a buffered literal run: quoted if it contains a letter (so SimpleDateFormat
	// treats it as text, not pattern), otherwise verbatim.
	private static void flushLiteral(StringBuilder out, StringBuilder literal) {
		if (literal.length() == 0) {
			return;
		}
		String run = literal.toString();
		literal.setLength(0);
		boolean hasLetter = false;
		for (int k = 0; k < run.length(); k++) {
			if (Character.isLetter(run.charAt(k))) {
				hasLetter = true;
				break;
			}
		}
		if (hasLetter) {
			out.append('\'').append(run.replace("'", "''")).append('\'');
		}
		else {
			out.append(run);
		}
	}

	/**
	 * Converts various types to Date object.
	 */
	private static Date convertToDate(Object dateObj) {
		if (dateObj instanceof Date date) {
			return date;
		}
		else if (dateObj instanceof Number number) {
			// Helm/Sprig unix timestamps are seconds since the epoch (Go time.Unix).
			return new Date(number.longValue() * 1000L);
		}
		else if (dateObj instanceof Instant instant) {
			return Date.from(instant);
		}
		return null;
	}

	private static String formatDuration(Duration d) {
		long totalSeconds = Math.abs(d.getSeconds());
		if (totalSeconds == 0) {
			return "0s";
		}
		StringBuilder sb = new StringBuilder();
		if (d.isNegative()) {
			sb.append('-');
		}
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;
		// Go's time.Duration.String() emits every component from the largest non-zero
		// unit
		// down to seconds, including zeros in between (e.g. 3600s -> "1h0m0s", not "1h").
		if (hours > 0) {
			sb.append(hours).append('h').append(minutes).append('m').append(seconds).append('s');
		}
		else if (minutes > 0) {
			sb.append(minutes).append('m').append(seconds).append('s');
		}
		else {
			sb.append(seconds).append('s');
		}
		return sb.toString();
	}

	/**
	 * Collapses a duration to the single largest unit it exceeds, mirroring Sprig's
	 * durationRound (which defines month as 30 days and year as 365 days). Operates on
	 * the magnitude so e.g. 7205s -&gt; "2h", 86405s -&gt; "1d", 8640005s -&gt; "3mo".
	 */
	private static String roundDuration(long secs) {
		long u = Math.abs(secs);
		if (u > 365L * 24 * 3600) {
			return (u / (365L * 24 * 3600)) + "y";
		}
		if (u > 30L * 24 * 3600) {
			return (u / (30L * 24 * 3600)) + "mo";
		}
		if (u > 24L * 3600) {
			return (u / (24L * 3600)) + "d";
		}
		if (u > 3600) {
			return (u / 3600) + "h";
		}
		if (u > 60) {
			return (u / 60) + "m";
		}
		if (u > 1) {
			return u + "s";
		}
		return "0s";
	}

	/**
	 * Parses duration string to milliseconds. Supports formats like: "+1h", "-30m",
	 * "+24h30m", "1h30m45s"
	 */
	private static long parseDurationToMillis(String duration) {
		String dur = duration.trim();
		boolean negative = dur.startsWith("-");
		if (negative || dur.startsWith("+")) {
			dur = dur.substring(1);
		}

		long totalMillis = 0;
		StringBuilder number = new StringBuilder();

		for (int i = 0; i < dur.length(); i++) {
			char c = dur.charAt(i);
			if (Character.isDigit(c)) {
				number.append(c);
			}
			else {
				if (number.length() > 0) {
					long value = Long.parseLong(number.toString());
					long millisToAdd = switch (c) {
						case 'h' -> value * 3600000L;
						case 'm' -> value * 60000L;
						case 's' -> value * 1000L;
						case 'd' -> value * 86400000L;
						default -> 0L;
					};
					totalMillis += millisToAdd;
					number = new StringBuilder();
				}
			}
		}

		return (negative) ? -totalMillis : totalMillis;
	}

}
