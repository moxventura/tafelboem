package nl.jeeninga.tafelboem.core;

/**
 * Keeps the fight's rhythm: whatever landing he gets, the time from a kick to his next throw stays
 * what it was in R1. A longer landing takes its extra time off the pause before the next throw.
 */
public final class Pacing {
	/** The pause before his next throw after the R1 dizzy spell. */
	public static final int COOLDOWN_TICKS = 50;

	/**
	 * After a bomb was dodged his next throw comes this much later (R1.4), so there is time to catch it, turn
	 * round and throw it back.
	 */
	public static final int AFTER_DODGE_COOLDOWN_TICKS = 90;

	/** The wobble after a throw-back hits him; he also skips a throw, see {@link Stagger}. */
	public static final int STAGGER_TICKS = 20;

	/** A kid throws a Terugbom back at most once a second. */
	public static final int BACK_COOLDOWN_TICKS = 20;

	/** A Terugbom nobody picks up poofs after 15 s. */
	public static final int TERUGBOM_LIFETIME_TICKS = 300;

	/**
	 * How long (15 s) the fight waits for a kick's reaction to end before it brings Graaf Fout back
	 * itself. A yeet ends within 100 ticks (the boss package's {@code Yeet.MAX_AIRBORNE_TICKS}) and a
	 * landing within 40 ({@link LandingStyle#ticks()}), so a real reaction never comes near it: only a
	 * boss who stopped being ticked can. It counts from a second after the kick, the earliest the fight
	 * looks at whether the reaction is over.
	 */
	public static final int STUCK_TICKS = 300;

	private Pacing() {
	}

	public static int cooldownAfter(LandingStyle landing) {
		return COOLDOWN_TICKS - Math.max(0, landing.ticks() - LandingStyle.DIZZY.ticks());
	}
}
