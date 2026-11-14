package nl.jeeninga.tafelboem.core;

/**
 * How Graaf Fout aims a bomb at a kid (R1.4): ahead of where they are going, at a lead and a flight
 * time that depend on the level. A kid who stands still is aimed at where they stand.
 */
public final class Aim {
	/** Never a shorter flight: there is always time to react. */
	public static final int MIN_FLIGHT_TICKS = 12;
	/** The aim point stays this far inside the arena's edge, on the floor. */
	private static final double INSET = 0.5;
	/** The aim point depends on the flight time and the flight time on the aim point; at most this many rounds settle it. */
	private static final int ROUNDS = 6;

	private Aim() {
	}

	/**
	 * What a level asks of his throw.
	 *
	 * @param lead         how much of the kid's movement he aims ahead, 0–1
	 * @param flightFactor times today's flight time
	 * @param minTicks     the shortest flight
	 */
	public record Tuning(double lead, double flightFactor, int minTicks) {
		/** The numbers of the spec: lead 70, 85 and 95%, flight ×0.85, ×0.75 and ×0.68. */
		public static Tuning defaults(int level) {
			return switch (Math.clamp(level, 1, 3)) {
				case 1 -> new Tuning(0.70, 0.85, MIN_FLIGHT_TICKS);
				case 2 -> new Tuning(0.85, 0.75, MIN_FLIGHT_TICKS);
				default -> new Tuning(0.95, 0.68, MIN_FLIGHT_TICKS);
			};
		}
	}

	/** Where the bomb is aimed (the floor, x and z) and how many ticks it flies. */
	public record Plan(double x, double z, int ticks) {
	}

	/** Today's (1.3.0) flight time: from his distance to the aim point, between 15 and 40 ticks. */
	public static int baseTicks(double horizontalDistance) {
		return (int) Math.max(15, Math.min(40, horizontalDistance * 1.6 + 10));
	}

	/** The flight time for a level: today's times its factor, never below the floor. */
	public static int flightTicks(double horizontalDistance, Tuning tuning) {
		return Math.max(tuning.minTicks(), (int) Math.round(baseTicks(horizontalDistance) * tuning.flightFactor()));
	}

	/**
	 * The throw: where the kid will be when the bomb lands, times the lead, kept inside the arena's
	 * floor, and the flight time to that spot.
	 *
	 * @param velX the kid's speed in blocks per tick (see {@link Motion}); 0 for a kid who stands still
	 */
	public static Plan plan(double fromX, double fromZ, double kidX, double kidZ, double velX, double velZ, Tuning tuning, ArenaBox floor) {
		int ticks = flightTicks(Math.hypot(kidX - fromX, kidZ - fromZ), tuning);
		double aimX = kidX;
		double aimZ = kidZ;
		for (int round = 0; round < ROUNDS; round++) {
			aimX = clamp(kidX + velX * ticks * tuning.lead(), floor.minX() + INSET, floor.maxX() + 1 - INSET);
			aimZ = clamp(kidZ + velZ * ticks * tuning.lead(), floor.minZ() + INSET, floor.maxZ() + 1 - INSET);
			int settled = flightTicks(Math.hypot(aimX - fromX, aimZ - fromZ), tuning);
			if (settled == ticks) {
				break;
			}
			ticks = settled;
		}
		return new Plan(aimX, aimZ, ticks);
	}

	private static double clamp(double value, double low, double high) {
		return Math.max(low, Math.min(high, value));
	}
}
