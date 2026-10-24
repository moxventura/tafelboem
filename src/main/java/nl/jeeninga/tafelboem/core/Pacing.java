package nl.jeeninga.tafelboem.core;

/**
 * Keeps the fight's rhythm: whatever landing he gets, the time from a kick to his next throw stays
 * what it was in R1. A longer landing takes its extra time off the pause before the next throw.
 */
public final class Pacing {
	/** The pause before his next throw after the R1 dizzy spell. */
	public static final int COOLDOWN_TICKS = 50;

	private Pacing() {
	}

	public static int cooldownAfter(LandingStyle landing) {
		return COOLDOWN_TICKS - Math.max(0, landing.ticks() - LandingStyle.DIZZY.ticks());
	}
}
