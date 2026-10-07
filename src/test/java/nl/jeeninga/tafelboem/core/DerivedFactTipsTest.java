package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class DerivedFactTipsTest {
	@Test
	void everyTipEndsInTheCorrectProduct() {
		for (Fact fact : Fact.all()) {
			Tip tip = DerivedFactTips.tipFor(fact);
			assertEquals(fact.product(), tip.args().getLast(), () -> fact + " -> " + tip);
		}
	}

	@Test
	void everyTipIsArithmeticallyConsistent() {
		for (Fact fact : Fact.all()) {
			Tip tip = DerivedFactTips.tipFor(fact);
			List<Integer> x = tip.args();
			switch (tip.key()) {
				case DerivedFactTips.ONE -> assertEquals(x.get(0), x.get(1));
				case DerivedFactTips.TEN -> assertEquals(x.get(0) * 10, x.get(1));
				case DerivedFactTips.TWO -> assertEquals(x.get(0) + x.get(0), x.get(1));
				case DerivedFactTips.FIVE -> {
					assertEquals(x.get(0) * 10, x.get(1));
					assertEquals(x.get(1) / 2, x.get(2));
				}
				case DerivedFactTips.NINE -> {
					assertEquals(x.get(0) * 10, x.get(1));
					assertEquals(x.get(1) - x.get(0), x.get(2));
				}
				case DerivedFactTips.FOUR -> {
					assertEquals(x.get(0) * 2, x.get(1));
					assertEquals(x.get(1) * 2, x.get(2));
				}
				case DerivedFactTips.THREE -> {
					assertEquals(x.get(0) * 2, x.get(1));
					assertEquals(x.get(1) + x.get(0), x.get(2));
				}
				case DerivedFactTips.FIVE_SPLIT -> {
					int f = x.get(0), n = x.get(1);
					assertEquals(f - 5, x.get(2));
					assertEquals(5 * n, x.get(3));
					assertEquals((f - 5) * n, x.get(4));
					assertEquals(x.get(3) + x.get(4), x.get(5));
				}
				default -> throw new AssertionError("Unknown tip key " + tip.key());
			}
		}
	}

	@Test
	void sevenTimesEightSplitsOnFive() {
		assertEquals(new Tip(DerivedFactTips.FIVE_SPLIT, List.of(7, 8, 2, 40, 16, 56)), DerivedFactTips.tipFor(new Fact(7, 8)));
		assertEquals(new Tip(DerivedFactTips.FIVE_SPLIT, List.of(7, 8, 2, 40, 16, 56)), DerivedFactTips.tipFor(new Fact(8, 7)));
	}

	@Test
	void easierFactorWins() {
		assertEquals(DerivedFactTips.TEN, DerivedFactTips.tipFor(new Fact(7, 10)).key());
		assertEquals(DerivedFactTips.NINE, DerivedFactTips.tipFor(new Fact(9, 7)).key());
		assertEquals(DerivedFactTips.ONE, DerivedFactTips.tipFor(new Fact(1, 10)).key());
		assertEquals(new Tip(DerivedFactTips.NINE, List.of(6, 60, 54)), DerivedFactTips.tipFor(new Fact(6, 9)));
	}
}
