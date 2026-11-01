package nl.jeeninga.tafelboem.core;

/**
 * Half a heart that nothing takes: on Peaceful and Easy in an arena (R1.3, switch
 * {@code arenaSafety}), and always for a typed wrong answer. On Normal and Hard an arena is vanilla.
 */
public final class SafetyFloor {
	/** Half a heart, in Minecraft's health points. */
	public static final float HALF_HEART = 1F;
	/** Minecraft's difficulty ids: 0 peaceful, 1 easy, 2 normal, 3 hard. */
	public static final int EASY = 1;

	private SafetyFloor() {
	}

	public static boolean protects(boolean arenaSafety, int difficultyId) {
		return arenaSafety && difficultyId <= EASY;
	}

	/** How much of a hit may land: all of it while it leaves half a heart; never more, and nothing at half a heart. */
	public static float allowed(float health, float damage) {
		return Math.max(0F, Math.min(damage, health - HALF_HEART));
	}
}
