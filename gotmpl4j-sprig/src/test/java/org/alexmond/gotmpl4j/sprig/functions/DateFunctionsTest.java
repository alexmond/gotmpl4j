package org.alexmond.gotmpl4j.sprig.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import org.alexmond.gotmpl4j.GoTemplate;
import org.alexmond.gotmpl4j.TemplateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DateFunctionsTest {

	private void execute(String name, String text, Object data, StringWriter writer)
			throws IOException, TemplateException {
		GoTemplate template = new GoTemplate();
		template.parse(name, text);
		template.execute(name, data, writer);
	}

	private String exec(String template) throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("t", template, new HashMap<>(), writer);
		return writer.toString();
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|',
			value = { "15h04                   | 13h45", "Day 2006                | Day 2024",
					"3:04PM                  | 1:45PM", "02/01/2006              | 15/01/2024",
					"Mon Jan 2 15:04:05 2006 | Mon Jan 15 13:45:07 2024",
					"Monday, January 2, 2006 | Monday, January 15, 2024" })
	void layoutFormatsLikeGo(String layout, String expected) throws Exception {
		// Parse a fixed wall time then format it; the round-trip is zone-independent.
		// Expectations verified against go1.23.4 time.Format. The literal-bearing layouts
		// (15h04, Day 2006) are exactly what the old search-and-replace converter
		// corrupted.
		assertEquals(expected.trim(),
				exec("{{ toDate \"2006-01-02 15:04:05\" \"2024-01-15 13:45:07\" | date \"" + layout.trim() + "\" }}"));
	}

	@Test
	void fractionalSecondsRoundTrip() throws Exception {
		assertEquals("13:45:07.123",
				exec("{{ toDate \"2006-01-02 15:04:05.000\" \"2024-01-15 13:45:07.123\" | date \"15:04:05.000\" }}"));
	}

	@Test
	void testNow() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ now }}", new HashMap<>(), writer);
		assertFalse(writer.toString().isEmpty());
	}

	@Test
	void testDateFormatting() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ now | date \"2006-01-02\" }}", new HashMap<>(), writer);
		assertTrue(writer.toString().matches("\\d{4}-\\d{2}-\\d{2}"));
	}

	@Test
	void testHtmlDate() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ now | htmlDate }}", new HashMap<>(), writer);
		assertTrue(writer.toString().matches("\\d{4}-\\d{2}-\\d{2}"));
	}

	@Test
	void testUnixEpoch() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ now | unixEpoch }}", new HashMap<>(), writer);
		assertTrue(writer.toString().matches("\\d+"));
	}

	@Test
	void testDateModify() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ now | dateModify \"24h\" | unixEpoch }}", new HashMap<>(), writer);
		assertTrue(writer.toString().matches("\\d+"));
	}

	@Test
	void testToDate() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ toDate \"2006-01-02\" \"2024-01-15\" }}", new HashMap<>(), writer);
		assertFalse(writer.toString().isEmpty());
	}

	@Test
	void testDateInZone() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ dateInZone \"2006-01-02\" now \"UTC\" }}", new HashMap<>(), writer);
		assertTrue(writer.toString().matches("\\d{4}-\\d{2}-\\d{2}"));
	}

	@Test
	void testDurationRound() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ durationRound \"2h10m5s\" }}", new HashMap<>(), writer);
		assertFalse(writer.toString().isEmpty());
	}

	@Test
	void testMustToDate() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ mustToDate \"2006-01-02\" \"2024-12-25\" }}", new HashMap<>(), writer);
		assertFalse(writer.toString().isEmpty());
	}

	@Test
	void testHtmlDateInZone() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ htmlDateInZone now \"UTC\" }}", new HashMap<>(), writer);
		assertTrue(writer.toString().matches("\\d{4}-\\d{2}-\\d{2}"));
	}

	@Test
	void testDateWithCustomFormat() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ now | date \"2006\" }}", new HashMap<>(), writer);
		assertTrue(writer.toString().matches("\\d{4}"));
	}

	@Test
	void testDateInZoneWithTimezone() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ dateInZone \"15:04:05\" now \"America/New_York\" }}", new HashMap<>(), writer);
		assertTrue(writer.toString().matches("\\d{2}:\\d{2}:\\d{2}"));
	}

	@Test
	void testToDateWithDifferentFormat() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ toDate \"01/02/2006\" \"12/25/2024\" }}", new HashMap<>(), writer);
		assertFalse(writer.toString().isEmpty());
	}

	@Test
	void testDateModifyWithNegative() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ now | dateModify \"-24h\" | unixEpoch }}", new HashMap<>(), writer);
		assertTrue(writer.toString().matches("\\d+"));
	}

	@Test
	void testUnixEpochWithPipechain() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ now | dateModify \"1h\" | unixEpoch }}", new HashMap<>(), writer);
		assertTrue(writer.toString().matches("\\d+"));
	}

	// --- New functions: ago, duration, mustDateModify, aliases ---

	@Test
	void testAgo() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		// ago on now should produce 0s or a very small duration
		execute("test", "{{ now | ago }}", new HashMap<>(), writer);
		assertFalse(writer.toString().isEmpty());
	}

	@Test
	void testDurationFromSeconds() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ duration 3661 }}", new HashMap<>(), writer);
		assertEquals("1h1m1s", writer.toString());
	}

	@Test
	void testDurationZero() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ duration 0 }}", new HashMap<>(), writer);
		assertEquals("0s", writer.toString());
	}

	@Test
	void testDurationHoursOnly() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ duration 7200 }}", new HashMap<>(), writer);
		// Go's time.Duration.String() keeps the zero components: 2h0m0s, not "2h".
		assertEquals("2h0m0s", writer.toString());
	}

	@Test
	void testMustDateModify() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ now | mustDateModify \"1h\" | unixEpoch }}", new HashMap<>(), writer);
		assertTrue(writer.toString().matches("\\d+"));
	}

	@Test
	void testDateInZoneAlias() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ date_in_zone \"2006-01-02\" now \"UTC\" }}", new HashMap<>(), writer);
		assertTrue(writer.toString().matches("\\d{4}-\\d{2}-\\d{2}"));
	}

	@Test
	void testDateModifyAlias() throws IOException, TemplateException {
		StringWriter writer = new StringWriter();
		execute("test", "{{ now | date_modify \"1h\" | unixEpoch }}", new HashMap<>(), writer);
		assertTrue(writer.toString().matches("\\d+"));
	}

}
