package nl.jeeninga.tafelboem.core;

/**
 * How hard a yeet is (R1.4). A throw-back is ×1.25 and a super yeet ×1.5 on top of the yeet-o-meter's own
 * factor ({@link YeetBoost}, at most ×1.3), and their total power is capped so he still lands in the arena.
 * A kicked bomb is as it was: its power is never capped.
 */
public final class YeetPower {
	/**
	 * The most power a throw-back or super yeet gets: bomb power × the random 0.9–1.4 roll × the factors. A
	 * classic yeet from the middle of an arena (the longest flight on the ground) travels about 11.4 blocks per
	 * unit of power, and the floor of a level 3 arena ends 22.5 blocks from its middle: at 1.9 he lands about
	 * 21 blocks out. {@code YeetCapGameTest} yeets him at this power in a level 3 arena; change the constant
	 * only together with that test.
	 */
	public static final double MAX_POWER = 1.9;

	private YeetPower() {
	}

	/** The factor of a yeet: the yeet-o-meter's factor (1 with the boost switched off) times the kind's. */
	public static double factor(YeetKind kind, double boost) {
		return boost * kind.multiplier();
	}

	/**
	 * @param rolled    the random 0.9–1.4 of this yeet
	 * @param tierPower {@link BombTier#yeetPower()}
	 * @param factor    from {@link #factor}
	 */
	public static double power(YeetKind kind, double rolled, double tierPower, double factor) {
		double power = rolled * tierPower * factor;
		return kind == YeetKind.KICK ? power : Math.min(MAX_POWER, power);
	}
}
