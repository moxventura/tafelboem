package nl.jeeninga.tafelboem.core;

import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

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
	private int dodged;
	private int pickedUp;
	private final Map<TrapKind, Integer> trapped = new EnumMap<>(TrapKind.class);

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

	/** A bomb thrown at this kid missed them (R1.3): they ran, or the build stopped it. */
	public void dodge() {
		dodged++;
	}

	public int dodged() {
		return dodged;
	}

	/** This kid walked into a lying bomb and got its question. */
	public void pickUp() {
		pickedUp++;
	}

	public int pickedUp() {
		return pickedUp;
	}

	/** Graaf Fout landed in a trap after this kid's kick. */
	public void trap(TrapKind kind) {
		trapped.merge(kind, 1, Integer::sum);
	}

	/** Trap landings after this kid's kicks, by {@link TrapKind#id()}. */
	public Map<String, Integer> trapped() {
		Map<String, Integer> byId = new TreeMap<>();
		trapped.forEach((kind, count) -> byId.put(kind.id(), count));
		return byId;
	}

	/** No stars without a win, and none for a kid who never answered in this fight. */
	public int stars(boolean won) {
		return won && answers > 0 ? Stars.forWin(wrong) : 0;
	}
}
