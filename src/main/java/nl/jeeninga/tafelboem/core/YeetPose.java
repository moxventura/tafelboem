package nl.jeeninga.tafelboem.core;

/**
 * How Graaf Fout's body is turned, squashed or sunk on top of his usual spin, for the yeets and
 * landings that need more than a spin: the tumble, the boomerang's faceplant, head-first in the
 * ground and the pancake and the wall splat. The soot face and the sore toe only change how he looks;
 * the renderer picks a texture and a leg pose for them. Only the numbers live here; the renderer
 * applies them.
 */
public enum YeetPose {
	NONE,
	TUMBLE,
	FACEPLANT,
	HEAD_FIRST,
	PANCAKE,
	SPLAT,
	SOOT,
	TOE_HOP;

	/** Roll and flip at different speeds, so he tumbles instead of turning like a wheel. */
	public static final float TUMBLE_ROLL_PER_TICK = 35F;
	public static final float TUMBLE_PITCH_PER_TICK = 22F;
	/** How long tipping over onto his face takes. */
	public static final int FACEPLANT_TICKS = 5;
	/** How long getting back upright (and back into shape) takes after a pose ends. */
	public static final int SETTLE_TICKS = 6;
	/** How far his head goes into the ground, in blocks. */
	public static final float HEAD_FIRST_SINK = 0.6F;
	/** A pancake is a quarter of his height. */
	public static final float PANCAKE_HEIGHT = 0.25F;

	/** Flat against a wall: a third of his depth. */
	public static final float SPLAT_DEPTH = 0.3F;

	/**
	 * Roll (sideways) and pitch (negative is forward, onto his face) in degrees, height and depth as
	 * scale factors, and how far he is sunk into the ground in blocks.
	 */
	public record Transform(float roll, float pitch, float height, float depth, float sink) {
		public static final Transform UPRIGHT = new Transform(0F, 0F, 1F, 1F, 0F);
	}

	/** The transform {@code ticks} after this pose started. */
	public Transform transform(float ticks) {
		return switch (this) {
			case NONE -> Transform.UPRIGHT;
			case TUMBLE -> new Transform(halfTurn(ticks * TUMBLE_ROLL_PER_TICK), halfTurn(ticks * TUMBLE_PITCH_PER_TICK), 1F, 1F, 0F);
			case FACEPLANT -> faceplant(Math.min(1F, ticks / FACEPLANT_TICKS));
			case HEAD_FIRST -> new Transform(0F, 0F, 1F, 1F, HEAD_FIRST_SINK);
			case PANCAKE -> new Transform(0F, 0F, PANCAKE_HEIGHT, 1F, 0F);
			case SPLAT -> new Transform(0F, 0F, 1F, SPLAT_DEPTH, 0F);
			case SOOT, TOE_HOP -> Transform.UPRIGHT;
		};
	}

	/** Where he turns round, as a share of his height: his middle for the tumble, his feet otherwise. */
	public float pivotShare() {
		return this == TUMBLE ? 0.5F : 0F;
	}

	/** Head-first is drawn upside down, the vanilla way. */
	public boolean upsideDown() {
		return this == HEAD_FIRST;
	}

	/** The pose a synced id stands for; an unknown one (a newer mod on the host) shows as none. */
	public static YeetPose byId(int id) {
		YeetPose[] poses = values();
		return id >= 0 && id < poses.length ? poses[id] : NONE;
	}

	private static Transform faceplant(float tipped) {
		return tipped <= 0F ? Transform.UPRIGHT : new Transform(0F, -90F * tipped, 1F, 1F, 0F);
	}

	/**
	 * Back to upright and back into shape, {@code ticks} after a pose ended at {@code from}. The angles
	 * are always within half a turn, so he never spins all the way round to get up; a pancake springs
	 * back to his height, which is the boing.
	 */
	public static Transform settle(Transform from, float ticks) {
		float left = 1F - ticks / SETTLE_TICKS;
		if (left <= 0F) {
			return Transform.UPRIGHT;
		}
		return new Transform(from.roll() * left, from.pitch() * left, 1F + (from.height() - 1F) * left, 1F + (from.depth() - 1F) * left,
				from.sink() * left);
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
