package nl.jeeninga.tafelboem.core;

import java.util.List;

/**
 * Picks the strategy a Dutch primary school would teach for working a fact out from easier ones
 * ("steuntafels"): ×1, ×10, ×2, ×5, ×9, ×4, ×3, and splitting 6, 7 and 8 into 5 + the rest.
 */
public final class DerivedFactTips {
	public static final String ONE = "tafelboem.tip.one";
	public static final String TEN = "tafelboem.tip.ten";
	public static final String TWO = "tafelboem.tip.two";
	public static final String FIVE = "tafelboem.tip.five";
	public static final String NINE = "tafelboem.tip.nine";
	public static final String FOUR = "tafelboem.tip.four";
	public static final String THREE = "tafelboem.tip.three";
	public static final String FIVE_SPLIT = "tafelboem.tip.five_split";

	/** The easiest factor to reason from comes first. */
	private static final int[] STRATEGY_ORDER = {1, 10, 2, 5, 9, 4, 3};

	private DerivedFactTips() {
	}

	public static Tip tipFor(Fact fact) {
		for (int factor : STRATEGY_ORDER) {
			if (fact.a() == factor) {
				return tip(factor, fact.b());
			}
			if (fact.b() == factor) {
				return tip(factor, fact.a());
			}
		}

		// Both factors are 6, 7 or 8: 7 × 8 = 5 × 8 + 2 × 8.
		int f = Math.min(fact.a(), fact.b());
		int n = Math.max(fact.a(), fact.b());
		return new Tip(FIVE_SPLIT, List.of(f, n, f - 5, 5 * n, (f - 5) * n, f * n));
	}

	private static Tip tip(int factor, int n) {
		return switch (factor) {
			case 1 -> new Tip(ONE, List.of(n, n));
			case 10 -> new Tip(TEN, List.of(n, n * 10));
			case 2 -> new Tip(TWO, List.of(n, n * 2));
			case 5 -> new Tip(FIVE, List.of(n, n * 10, n * 5));
			case 9 -> new Tip(NINE, List.of(n, n * 10, n * 9));
			case 4 -> new Tip(FOUR, List.of(n, n * 2, n * 4));
			case 3 -> new Tip(THREE, List.of(n, n * 2, n * 3));
			default -> throw new IllegalStateException("No strategy for factor " + factor);
		};
	}
}
