package nl.jeeninga.tafelboem.core;

/**
 * How long a child gets to run after a correct answer. Generous on purpose: a correct answer should
 * never feel like a trap.
 */
public final class EscapeModel {
	public static final double SPRINT_BLOCKS_PER_SECOND = 5.6;
	public static final double SAFETY_FACTOR = 1.5;
	public static final int BASE_TICKS = 20;
	public static final int MIN_TICKS = 30;
	public static final int FAST_BONUS_TICKS = 20;
	public static final long FAST_ANSWER_MILLIS = 5000;

	private EscapeModel() {
	}

	public static int escapeTicks(double blastRadius, long answerMillis) {
		int sprintTicks = (int) Math.ceil(blastRadius / SPRINT_BLOCKS_PER_SECOND * 20 * SAFETY_FACTOR);
		int ticks = Math.max(MIN_TICKS, BASE_TICKS + sprintTicks);
		if (answerMillis <= FAST_ANSWER_MILLIS) {
			ticks += FAST_BONUS_TICKS;
		}
		return ticks;
	}
}
