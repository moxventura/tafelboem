package nl.jeeninga.tafelboem.core;

/**
 * One line of the answer log: everything needed to evaluate learning afterwards.
 *
 * @param answer      the number the child entered, or -1 when they pressed "?" or ESC
 * @param context     where it happened, e.g. "fight" or "free"
 * @param via         how the answer was given: {@link #VIA_KEY} (Enter) or {@link #VIA_CLICK} (a button);
 *                    null in logs from before R0.1
 * @param typedDigits for "?": how many digits were already typed, a sign of a mis-click; 0 otherwise
 * @param version     the TafelBoem version that asked, such as {@code 0.1.3}; null in logs from before 0.1.3
 * @param level       the fight level: 1–3 in a TafelBoem arena, 0 elsewhere and in logs from before R1
 */
public record AnswerRecord(long timestamp, String player, int a, int b, int answer, Outcome outcome,
		long millis, int bombTier, String context, String via, int typedDigits, String version, int level) {
	public static final String VIA_KEY = "key";
	public static final String VIA_CLICK = "click";

	public Fact fact() {
		return new Fact(a, b);
	}

	public boolean correct() {
		return outcome == Outcome.CORRECT;
	}

	public boolean isLikelyMisclick() {
		return outcome == Outcome.DONT_KNOW && typedDigits > 0;
	}
}
