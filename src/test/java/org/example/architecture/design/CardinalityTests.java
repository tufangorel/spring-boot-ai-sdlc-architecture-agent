package org.example.architecture.design;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardinalityTests {

	@Test
	void acceptsNamesAndShorthands() {
		assertThat(Cardinality.fromJson("one-to-one")).isEqualTo(Cardinality.ONE_TO_ONE);
		assertThat(Cardinality.fromJson("1:1")).isEqualTo(Cardinality.ONE_TO_ONE);
		assertThat(Cardinality.fromJson("ONE_TO_MANY")).isEqualTo(Cardinality.ONE_TO_MANY);
		assertThat(Cardinality.fromJson("1..n")).isEqualTo(Cardinality.ONE_TO_MANY);
		assertThat(Cardinality.fromJson("1:m")).isEqualTo(Cardinality.ONE_TO_MANY);
		assertThat(Cardinality.fromJson("Many to one")).isEqualTo(Cardinality.MANY_TO_ONE);
		assertThat(Cardinality.fromJson("n:1")).isEqualTo(Cardinality.MANY_TO_ONE);
		assertThat(Cardinality.fromJson("M:1")).isEqualTo(Cardinality.MANY_TO_ONE);
		assertThat(Cardinality.fromJson("manyToMany")).isEqualTo(Cardinality.MANY_TO_MANY);
		assertThat(Cardinality.fromJson("n:n")).isEqualTo(Cardinality.MANY_TO_MANY);
		assertThat(Cardinality.fromJson("m:n")).isEqualTo(Cardinality.MANY_TO_MANY);
		assertThat(Cardinality.fromJson("N:M")).isEqualTo(Cardinality.MANY_TO_MANY);
	}

	@Test
	void returnsNullForAnythingElse() {
		assertThat(Cardinality.fromJson(null)).isNull();
		assertThat(Cardinality.fromJson("several")).isNull();
	}

}
