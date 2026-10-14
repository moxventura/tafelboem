package nl.jeeninga.tafelboem.core;

import java.util.List;

/**
 * Stars per kid per level: ★ for a win, ★★ with at most 2 wrong answers, ★★★ without one. Only
 * typed wrong answers count, so the stars reward "?" over guessing.
 */
public final class Stars {
	public static final int MAX = 3;

	private Stars() {
	}

	public static int forWin(int wrongAnswers) {
		return wrongAnswers == 0 ? 3 : wrongAnswers <= 2 ? 2 : 1;
	}

	/** The best stars on a level over all fights; 0 when the level was never won. */
	public static int best(List<FightRecord> fights, int level) {
		return fights.stream()
				.filter(fight -> fight.level() == level && fight.won())
				.mapToInt(FightRecord::stars)
				.max()
				.orElse(0);
	}

	/** For the doors and the stats, such as "★★☆". */
	public static String show(int stars) {
		return "★".repeat(stars) + "☆".repeat(MAX - stars);
	}
}
