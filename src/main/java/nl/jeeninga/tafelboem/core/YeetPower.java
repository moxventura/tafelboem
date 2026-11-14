package nl.jeeninga.tafelboem.core;

/**
 * How hard a yeet is (R1.4). A throw-back is ×1.25 and a super yeet ×1.5 on top of the yeet-o-meter's own
 * factor ({@link YeetBoost}, at most ×1.3), and their total power is capped so he still lands in the arena.
 * A kicked bomb is as it was: its power is never capped.
 */
public final class YeetPower {
	/**
	 * The most power a throw-back or super yeet gets: bomb power × the random 0.9–1.4 roll × the factors. A
	 * classic yeet (the longest flight a throw-back or super yeet gets) travels about 10–11 blocks per unit of
	 * power, and the floor of a level 3 arena ends 22.5 blocks from its middle. A juggle can leave him hanging
	 * anywhere: {@code JuggleGameTest} super-yeets him from about 15 blocks out, away from a kid behind him, and he
	 * must land a block short of the floor's end. That holds at 0.6 (21.3 blocks out) and not at 0.65 (21.8); 1.9,
	 * set from the middle ({@code YeetCapGameTest}), took him out of the arena. At 0.6 every throw-back and super
	 * yeet flies at the cap, about 6 blocks, shorter than any kick, and the ×1.25 and ×1.5 change nothing. Change
	 * the constant only together with those tests.
	 */
	public static final double MAX_POWER = 0.6;

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
