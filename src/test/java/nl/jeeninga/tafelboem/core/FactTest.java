package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FactTest {
	@Test
	void productIsTheAnswer() {
		assertEquals(56, new Fact(7, 8).product());
	}

	@Test
	void familyIgnoresOrder() {
		assertEquals(new Fact(3, 7).family(), new Fact(7, 3).family());
		assertEquals(new Fact(3, 7), new Fact(7, 3).family());
	}

	@Test
	void commutedSwapsFactors() {
		assertEquals(new Fact(8, 7), new Fact(7, 8).commuted());
	}

	@Test
	void factorsMustBeOneToTen() {
		assertThrows(IllegalArgumentException.class, () -> new Fact(0, 5));
		assertThrows(IllegalArgumentException.class, () -> new Fact(5, 11));
	}

	@Test
	void thereAreOneHundredFactsInFiftyFiveFamilies() {
		assertEquals(100, Fact.all().size());
		assertEquals(55, Fact.all().stream().map(Fact::family).distinct().count());
	}
}
