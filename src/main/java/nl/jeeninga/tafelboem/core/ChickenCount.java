package nl.jeeninga.tafelboem.core;

/**
 * The counting Kippenbom: for a × b, a chickens fly out and each pops into b, so the kids see the
 * sum as a groups of b. Two generations at most.
 *
 * @param groups   the first generation: a chickens, one per group
 * @param perGroup how many chickens each of them pops into
 */
public record ChickenCount(int groups, int perGroup) {
	/** A wrong answer: one chicken hops out, never more than the smallest right answer (1 × 1). */
	public static final int WRONG_ANSWER = 1;

	public static ChickenCount of(Fact fact) {
		return new ChickenCount(fact.a(), fact.b());
	}

	public int total() {
		return groups * perGroup;
	}

	/** How fast the first generation flies out: more groups fly further, so the groups stay apart. */
	public double ringSpeed() {
		return 0.2 + 0.03 * groups;
	}

	/** For the game log, for example "3x4: 3 groups of 4, 12 chickens (fight)". */
	public String describe(String where) {
		return groups + "x" + perGroup + ": " + groups + (groups == 1 ? " group of " : " groups of ") + perGroup + ", "
				+ total() + (total() == 1 ? " chicken" : " chickens") + " (" + where + ")";
	}
}
