package org.example.architecture.design;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Cardinality {

	ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY;

	@JsonCreator
	static Cardinality fromJson(String value) {
		if (value == null) {
			return null;
		}
		return switch (value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "")) {
			case "ONETOONE", "11" -> ONE_TO_ONE;
			case "ONETOMANY", "1N", "1M" -> ONE_TO_MANY;
			case "MANYTOONE", "N1", "M1" -> MANY_TO_ONE;
			case "MANYTOMANY", "NN", "MN", "NM" -> MANY_TO_MANY;
			default -> null;
		};
	}

}
