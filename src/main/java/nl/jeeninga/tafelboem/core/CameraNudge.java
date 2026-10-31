package nl.jeeninga.tafelboem.core;

/**
 * Playtest 3 (P29): the kids felt the view was taken from them while Graaf Fout flew. So after a
 * right answer the view only gets a short nudge towards him, once at the kick and once when he
 * lands if he is out of view, and then it is theirs again. Angles are Minecraft's yaw and pitch in
 * degrees.
 */
public final class CameraNudge {
	/** A nudge stops after this many ticks (half a second), centred or not. */
	public static final int MAX_TICKS = 10;
	/** This close to the middle of the view, he counts as centred and the nudge stops. */
	public static final float CENTRED_DEGREES = 5F;
	/** This close, he is in view when he lands, so there is no nudge. */
	public static final float IN_VIEW_DEGREES = 35F;

	private CameraNudge() {
	}

	/** How far the view is off the target: the larger of the turn and the tilt, the short way round. */
	public static float offBy(float yaw, float pitch, float targetYaw, float targetPitch) {
		return Math.max(Math.abs(wrapDegrees(targetYaw - yaw)), Math.abs(targetPitch - pitch));
	}

	/** He landed this far off the view: only nudge when the kid can't see him. */
	public static boolean landingNeedsNudge(float offBy) {
		return offBy > IN_VIEW_DEGREES;
	}

	/** After {@code ticks} ticks of nudging with the view {@code offBy} degrees off him, let go. */
	public static boolean done(int ticks, float offBy) {
		return ticks >= MAX_TICKS || offBy <= CENTRED_DEGREES;
	}

	private static float wrapDegrees(float degrees) {
		float wrapped = degrees % 360F;
		if (wrapped >= 180F) {
			wrapped -= 360F;
		} else if (wrapped < -180F) {
			wrapped += 360F;
		}
		return wrapped;
	}
}
