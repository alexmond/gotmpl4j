package org.alexmond.gotmpl4j.benchmarks.model;

/**
 * A row in the "stocks table" benchmark workload, accessed by all engines through bean
 * getters. gotmpl4j resolves {@code .Symbol} to {@link #getSymbol()} via its Go-style
 * property mapping, so its template matches Go's exactly; FreeMarker/Thymeleaf use
 * {@code item.symbol}.
 */
public class Stock {

	private final String symbol;

	private final String name;

	private final double price;

	private final double change;

	private final double ratio;

	public Stock(String symbol, String name, double price, double change) {
		this.symbol = symbol;
		this.name = name;
		this.price = price;
		this.change = change;
		this.ratio = (price != 0.0) ? (change / price * 100.0) : 0.0;
	}

	public String getSymbol() {
		return this.symbol;
	}

	public String getName() {
		return this.name;
	}

	public double getPrice() {
		return this.price;
	}

	public double getChange() {
		return this.change;
	}

	public double getRatio() {
		return this.ratio;
	}

	public boolean isUp() {
		return this.change >= 0;
	}

	/** A loss row ({@code change < 0}); drives the conditional "minus" styling branch. */
	public boolean isMinus() {
		return this.change < 0;
	}

}
