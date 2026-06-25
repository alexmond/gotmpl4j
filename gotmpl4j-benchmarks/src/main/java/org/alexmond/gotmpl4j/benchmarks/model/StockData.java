package org.alexmond.gotmpl4j.benchmarks.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a deterministic list of {@link Stock} rows for the table workload (no RNG, so
 * runs are reproducible and comparable across engines).
 */
public final class StockData {

	private static final String[] SYMBOLS = { "ADBE", "AMD", "AAPL", "AMZN", "BIDU", "CSCO", "GOOG", "INTC", "MSFT",
			"NFLX" };

	private StockData() {
	}

	public static List<Stock> stocks(int count) {
		List<Stock> stocks = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			String symbol = SYMBOLS[i % SYMBOLS.length];
			double price = 100.0 + (i % 500);
			double change = ((i % 7) - 3) * 1.25;
			stocks.add(new Stock(symbol, symbol + " Inc.", price, change));
		}
		return stocks;
	}

}
