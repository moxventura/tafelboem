package nl.jeeninga.tafelboem.core;

/**
 * One kid's answers in one fight. Only typed wrong answers cost stars: "?" and ESC never do.
 */
public final class FightTally {
	private int answers;
	private int wrong;
	private int dontKnow;
	private int chickenOut;

	public void record(Outcome outcome) {
		answers++;
		switch (outcome) {
			case WRONG -> wrong++;
			case DONT_KNOW -> dontKnow++;
			case CHICKEN_OUT -> chickenOut++;
			default -> {
			}
		}
	}

	public int answers() {
		return answers;
	}

	public int wrong() {
		return wrong;
	}

	public int dontKnow() {
		return dontKnow;
	}

	public int chickenOut() {
		return chickenOut;
	}

	/** No stars without a win, and none for a kid who never answered in this fight. */
	public int stars(boolean won) {
		return won && answers > 0 ? Stars.forWin(wrong) : 0;
	}
}
