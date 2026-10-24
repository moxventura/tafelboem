package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChickenCountTest {
	@Test
	void threeTimesFourIsThreeGroupsOfFour() {
		ChickenCount count = ChickenCount.of(new Fact(3, 4));

		assertEquals(3, count.groups());
		assertEquals(4, count.perGroup());
		assertEquals(12, count.total());
	}

	@Test
	void everySumShowsItsProductAndNeverFewerChickensThanAWrongAnswer() {
		for (Fact fact : Fact.all()) {
			ChickenCount count = ChickenCount.of(fact);
			assertEquals(fact.product(), count.total(), fact.toString());
			assertTrue(count.total() >= ChickenCount.WRONG_ANSWER, fact + " gives fewer chickens than a wrong answer");
		}
	}

	@Test
	void moreGroupsFlyFurtherSoTheyStayApart() {
		assertTrue(ChickenCount.of(new Fact(10, 2)).ringSpeed() > ChickenCount.of(new Fact(2, 10)).ringSpeed());
	}

	@Test
	void aLineForTheGameLog() {
		assertEquals("3x4: 3 groups of 4, 12 chickens (fight)", ChickenCount.of(new Fact(3, 4)).describe("fight"));
		assertEquals("1x1: 1 group of 1, 1 chicken (free play)", ChickenCount.of(new Fact(1, 1)).describe("free play"));
	}
}
