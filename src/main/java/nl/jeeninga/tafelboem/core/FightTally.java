package nl.jeeninga.tafelboem.core;

/**
 * One kid's answers in one fight. Only typed wrong answers cost stars and end a streak: "?" and ESC
 * never do.
 */
public final class FightTally {
	private int answers;
	private int wrong;
	private int dontKnow;
	private int chickenOut;
	private int streak;
	private int farthestYeet;

	public void record(Outcome outcome) {
		answers++;
		switch (outcome) {
			case CORRECT -> streak++;
			case WRONG -> {
				wrong++;
				streak = 0;
			}
			case DONT_KNOW -> dontKnow++;
			case CHICKEN_OUT -> chickenOut++;
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

	/** Right answers in a row; only a typed wrong answer ends it. */
	public int streak() {
		return streak;
	}

	/** A yeet this kid's kick sent Graaf Fout on, in blocks (helicopter heights don't count). */
	public void yeeted(int blocks) {
		farthestYeet = Math.max(farthestYeet, blocks);
	}

	public int farthestYeet() {
		return farthestYeet;
	}

	/** No stars without a win, and none for a kid who never answered in this fight. */
	public int stars(boolean won) {
		return won && answers > 0 ? Stars.forWin(wrong) : 0;
	}
}
