package nl.jeeninga.tafelboem.core;

/**
 * The rules of one boss fight, independent of how the boss looks or moves.
 */
public final class Fight {
	public static final int MAX_CHICKEN_OUTS = 3;

	private final int level;
	private final int hitsRequired;
	private int hits;
	private int chickenOuts;

	public Fight(int level) {
		this.level = level;
		this.hitsRequired = 10 + level;
	}

	public int level() {
		return level;
	}

	public int hitsRequired() {
		return hitsRequired;
	}

	public void registerHit() {
		if (!isDefeated()) {
			hits++;
		}
	}

	public int hitsRemaining() {
		return hitsRequired - hits;
	}

	public boolean isDefeated() {
		return hits >= hitsRequired;
	}

	public float healthFraction() {
		return (float) hitsRemaining() / hitsRequired;
	}

	/**
	 * @return whether this chicken-out is still allowed; after the limit ESC counts as "?".
	 */
	public boolean tryChickenOut() {
		if (chickenOuts >= MAX_CHICKEN_OUTS) {
			return false;
		}
		chickenOuts++;
		return true;
	}
}
