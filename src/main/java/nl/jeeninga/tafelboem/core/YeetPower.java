package nl.jeeninga.tafelboem.core;

/**
 * How hard a yeet is (R1.4). A throw-back is ×1.25 and a super yeet ×1.5 on top of the yeet-o-meter's own
 * factor ({@link YeetBoost}, at most ×1.3). Their power is capped by the room he has: the floor in the way he
 * flies, so he still lands in the arena wherever the throw-back found him. When there is more room through the
 * arena's middle than away from the kid, and away is too short, he flies through the middle. A kicked bomb is as
 * it was: its power is never capped.
 */
public final class YeetPower {
	/**
	 * In the air his speed across keeps 91% each tick ({@code vx *= 0.91}), so a flight launched at power p goes at
	 * most p / 0.09 = 11.1 blocks across, however high it starts: the classic yeet's horizontal speed is the power,
	 * the tumble's 0.85 of it. Simulated (plan note 7) and measured: 6.2 blocks at 0.6 from 5 blocks up, 20.7 at
	 * 1.9 from the ground, always a little short of this.
	 */
	public static final double BLOCKS_PER_POWER = 1 / (1 - 0.91);
	/** Floor he keeps between where he lands and the floor's edge (a wall, or the void). */
	public static final double ROOM_MARGIN = 2;
	/** Never less, so a yeet with no room at all still is one; it can then fly out, and the fight brings him back. */
	public static final double MIN_POWER = 0.3;
	/** How far the floor is walked to find its edge; further than any arena is wide. */
	private static final double MAX_WALK = 128;
	private static final double STEP = 0.25;

	private YeetPower() {
	}

	/** Whether a column is floor he may land on: in the arena, not a wall, not the void. */
	@FunctionalInterface
	public interface Floor {
		boolean at(double x, double z);
	}

	/** Which way a capped yeet goes, and how much room it has there. */
	public record Way(boolean throughTheMiddle, double room) {
	}

	/** The factor of a yeet: the yeet-o-meter's factor (1 with the boost switched off) times the kind's. */
	public static double factor(YeetKind kind, double boost) {
		return boost * kind.multiplier();
	}

	/**
	 * The power a yeet asks for, before any cap: bomb × roll × factor.
	 *
	 * @param rolled    the random 0.9–1.4 of this yeet
	 * @param tierPower {@link BombTier#yeetPower()}
	 * @param factor    from {@link #factor}
	 */
	public static double wanted(double rolled, double tierPower, double factor) {
		return rolled * tierPower * factor;
	}

	/** The most power whose flight fits this much room (already less {@link #ROOM_MARGIN}); never below {@link #MIN_POWER}. */
	public static double capForRoom(double room) {
		return Math.max(MIN_POWER, room / BLOCKS_PER_POWER);
	}

	/**
	 * The power of a yeet of this kind with this much room. A kick gets what it asks; a throw-back or super yeet at
	 * most what fits the room. So with room for a plain yeet of the bomb, it never flies shorter than one: it asks
	 * for ×1.25 or more.
	 */
	public static double power(YeetKind kind, double wanted, double room) {
		return kind == YeetKind.KICK ? wanted : Math.min(wanted, capForRoom(room));
	}

	/**
	 * Away from the kid when the yeet fits there or that way has at least as much room; otherwise through the
	 * arena's middle, towards its far side. Only these two ways are compared.
	 */
	public static Way way(double wanted, double roomAway, double roomThroughTheMiddle) {
		if (capForRoom(roomAway) >= wanted || roomAway >= roomThroughTheMiddle) {
			return new Way(false, roomAway);
		}
		return new Way(true, roomThroughTheMiddle);
	}

	/**
	 * The room from (x, z) in direction (dx, dz): how far the floor goes that way, less {@link #ROOM_MARGIN}, never
	 * below 0. Columns that are no floor before the first floor (he hangs over a wall or beyond it) are crossed; the
	 * first gap after floor ends it.
	 */
	public static double room(double x, double z, double dx, double dz, Floor floor) {
		double length = Math.hypot(dx, dz);
		if (length < 1.0E-6) {
			return 0;
		}
		double ux = dx / length;
		double uz = dz / length;
		double last = -1;
		for (double d = 0; d <= MAX_WALK; d += STEP) {
			if (floor.at(x + ux * d, z + uz * d)) {
				last = d;
			} else if (last >= 0) {
				break;
			}
		}
		return Math.max(0, last - ROOM_MARGIN);
	}
}
