package nl.jeeninga.tafelboem.core;

/**
 * How much further a kick yeets Graaf Fout: a fast answer and right answers in a row add up, to at
 * most ×1.3. A boost only ever adds, so a slow answer still gets a full yeet.
 */
public final class YeetBoost {
	public static final double FAST = 0.15;
	/** For every right answer in a row after the first. */
	public static final double PER_STREAK = 0.05;
	/** Further than this a boosted helicopter wouldn't come down in time, and most yeets would leave the arena. */
	public static final double MAX = 1.3;

	private YeetBoost() {
	}

	public static double factor(boolean fast, int streak) {
		return Math.min(MAX, 1 + (fast ? FAST : 0) + PER_STREAK * Math.max(0, streak - 1));
	}
}
