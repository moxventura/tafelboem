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
	private int farthestBack;
	private int farthestSuper;
	private int thrownAt;
	private int still;
	private int dodged;
	private int caught;
	private int thrownBack;
	private int backHits;
	private int juggles;
	private int terugRight;
	private int terugWrong;
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
		yeeted(YeetKind.KICK, blocks);
	}

	/** A yeet this kid's answer sent Graaf Fout on; each kind keeps its own farthest (R1.4). */
	public void yeeted(YeetKind kind, int blocks) {
		switch (kind) {
			case KICK -> farthestYeet = Math.max(farthestYeet, blocks);
			case BACK -> farthestBack = Math.max(farthestBack, blocks);
			case SUPER -> farthestSuper = Math.max(farthestSuper, blocks);
		}
	}

	public int farthestYeet() {
		return farthestYeet;
	}

	/** The farthest yeet after a right answer on a Terug! question, in blocks. */
	public int farthestBack() {
		return farthestBack;
	}

	/** The farthest super yeet, after a right answer on a juggle question, in blocks. */
	public int farthestSuper() {
		return farthestSuper;
	}

	/**
	 * A bomb thrown at this kid landed while they were free (R1.4), and either hit them or was dodged.
	 *
	 * @param stood  they moved less than a block between the throw and the landing
	 * @param dodged it missed them
	 */
	public void thrownAt(boolean stood, boolean dodged) {
		thrownAt++;
		if (stood) {
			still++;
		}
		if (dodged) {
			this.dodged++;
		}
	}

	/** Throws at this kid while they were free. */
	public int thrownAt() {
		return thrownAt;
	}

	/** Of those, the throws where they moved less than a block. */
	public int still() {
		return still;
	}

	/** Of those, the bombs that missed them. */
	public int dodged() {
		return dodged;
	}

	/** This kid caught a dodged bomb as a Terugbom. */
	public void catches() {
		caught++;
	}

	public int caught() {
		return caught;
	}

	/** This kid threw a Terugbom back. */
	public void throwBack() {
		thrownBack++;
	}

	public int thrownBack() {
		return thrownBack;
	}

	/** A throw-back of this kid's hit him. */
	public void backHit() {
		backHits++;
	}

	public int backHits() {
		return backHits;
	}

	/** A throw-back of this kid hit him in mid-yeet and froze him. */
	public void juggle() {
		juggles++;
	}

	public int juggles() {
		return juggles;
	}

	/** An answer to a Terug! or juggle question, right or wrong ("?" and ESC are no answer to count here). */
	public void terug(boolean right) {
		if (right) {
			terugRight++;
		} else {
			terugWrong++;
		}
	}

	public int terugRight() {
		return terugRight;
	}

	public int terugWrong() {
		return terugWrong;
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
