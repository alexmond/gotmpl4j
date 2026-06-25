package org.alexmond.gotmpl4j.benchmarks.model;

/**
 * Trivial single-field model for the interpolation workload. gotmpl4j reads {@code .Name}
 * as {@link #getName()}; FreeMarker reads {@code name}.
 */
public class Person {

	private final String name;

	public Person(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

}
