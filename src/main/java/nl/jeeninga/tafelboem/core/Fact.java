package nl.jeeninga.tafelboem.core;

import java.util.ArrayList;
import java.util.List;

/**
 * One multiplication fact, as shown to the child: {@code a × b}.
 */
public record Fact(int a, int b) {
	public static final int MIN_FACTOR = 1;
	public static final int MAX_FACTOR = 10;

	private static final List<Fact> ALL = createAll();

	public Fact {
		if (a < MIN_FACTOR || a > MAX_FACTOR || b < MIN_FACTOR || b > MAX_FACTOR) {
			throw new IllegalArgumentException("Factors must be between 1 and 10: " + a + " x " + b);
		}
	}

	public int product() {
		return a * b;
	}

	public Fact commuted() {
		return new Fact(b, a);
	}

	/**
	 * 3 × 7 and 7 × 3 belong to the same family; progress on one counts for the other.
	 */
	public Fact family() {
		return a <= b ? this : commuted();
	}

	public static List<Fact> all() {
		return ALL;
	}

	private static List<Fact> createAll() {
		List<Fact> facts = new ArrayList<>();
		for (int a = MIN_FACTOR; a <= MAX_FACTOR; a++) {
			for (int b = MIN_FACTOR; b <= MAX_FACTOR; b++) {
				facts.add(new Fact(a, b));
			}
		}
		return List.copyOf(facts);
	}

	@Override
	public String toString() {
		return a + "x" + b;
	}
}
