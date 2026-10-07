package nl.jeeninga.tafelboem.core;

/**
 * One line of the answer log: everything needed to evaluate learning afterwards.
 *
 * @param answer the number the child entered, or -1 when they pressed "?" or ESC
 * @param context where it happened, e.g. "fight" or "free"
 */
public record AnswerRecord(long timestamp, String player, int a, int b, int answer, Outcome outcome,
		long millis, int bombTier, String context) {
	public Fact fact() {
		return new Fact(a, b);
	}

	public boolean correct() {
		return outcome == Outcome.CORRECT;
	}
}
