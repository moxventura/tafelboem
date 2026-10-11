package nl.jeeninga.tafelboem.core;

/**
 * How Graaf Fout's body is turned on top of his usual spin, for the yeets that need more than a
 * spin: the tumble (rolling and flipping head over heels) and the boomerang's faceplant. Only the
 * angles live here; the renderer applies them.
 */
public enum YeetPose {
	NONE,
	TUMBLE,
	FACEPLANT;

	/** Roll and flip at different speeds, so he tumbles instead of turning like a wheel. */
	public static final float TUMBLE_ROLL_PER_TICK = 35F;
	public static final float TUMBLE_PITCH_PER_TICK = 22F;
	/** How long tipping over onto his face takes. */
	public static final int FACEPLANT_TICKS = 5;
	/** How long getting back upright takes after a pose ends. */
	public static final int SETTLE_TICKS = 6;

	/** Degrees of roll (sideways) and pitch (negative is forward, onto his face). */
	public record Angles(float roll, float pitch) {
		public static final Angles UPRIGHT = new Angles(0F, 0F);
	}

	/** The angles {@code ticks} after this pose started. */
	public Angles angles(float ticks) {
		return switch (this) {
			case NONE -> Angles.UPRIGHT;
			case TUMBLE -> new Angles(halfTurn(ticks * TUMBLE_ROLL_PER_TICK), halfTurn(ticks * TUMBLE_PITCH_PER_TICK));
			case FACEPLANT -> faceplant(Math.min(1F, ticks / FACEPLANT_TICKS));
		};
	}

	private static Angles faceplant(float tipped) {
		return tipped <= 0F ? Angles.UPRIGHT : new Angles(0F, -90F * tipped);
	}

	/**
	 * Back to upright, {@code ticks} after a pose ended at {@code from}. The angles are always within
	 * half a turn, so he never spins all the way round to get up.
	 */
	public static Angles settle(Angles from, float ticks) {
		float left = 1F - ticks / SETTLE_TICKS;
		if (left <= 0F) {
			return Angles.UPRIGHT;
		}
		return new Angles(from.roll() * left, from.pitch() * left);
	}

	/** Wraps degrees into [-180, 180). */
	private static float halfTurn(float degrees) {
		float wrapped = degrees % 360F;
		if (wrapped >= 180F) {
			wrapped -= 360F;
		} else if (wrapped < -180F) {
			wrapped += 360F;
		}
		return wrapped;
	}
}
