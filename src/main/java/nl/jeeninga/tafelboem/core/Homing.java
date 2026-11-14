package nl.jeeninga.tafelboem.core;

/**
 * The aim help of a Terugbom thrown back (R1.4): near Graaf Fout it bends towards his middle each tick,
 * and its hit box around him is 1 block bigger than his own. The younger kid hits a standing Graaf Fout
 * at least half the time.
 */
public final class Homing {
	/** Within this many blocks of his middle the bomb homes in on him. */
	public static final double RANGE = 5;
	/** While he flies from a yeet. */
	public static final double RANGE_WHILE_FLYING = 6;
	/** His hit box grows by this many blocks on every side. */
	public static final double HIT_MARGIN = 1;
	/** How much of each tick's direction is the direction to him: 0 flies straight, 1 steers at once. */
	public static final double BEND = 0.5;

	private Homing() {
	}

	public static double range(boolean heFlies) {
		return heFlies ? RANGE_WHILE_FLYING : RANGE;
	}

	/**
	 * The bomb's velocity for the next tick: its direction turned towards him by {@link #BEND}, its speed as it was.
	 *
	 * @param vx the bomb's velocity
	 * @param dx from the bomb to his middle
	 */
	public static double[] steer(double vx, double vy, double vz, double dx, double dy, double dz) {
		double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
		double wanted = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (speed == 0 || wanted == 0) {
			return new double[] {vx, vy, vz};
		}
		double x = vx / speed * (1 - BEND) + dx / wanted * BEND;
		double y = vy / speed * (1 - BEND) + dy / wanted * BEND;
		double z = vz / speed * (1 - BEND) + dz / wanted * BEND;
		double length = Math.sqrt(x * x + y * y + z * z);
		if (length == 0) {
			return new double[] {vx, vy, vz};
		}
		return new double[] {x / length * speed, y / length * speed, z / length * speed};
	}
}
