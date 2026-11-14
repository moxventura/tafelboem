package nl.jeeninga.tafelboem.core;

/**
 * The dodge rule: a bomb Graaf Fout threw hits the kid it was thrown at when the middle of the block
 * where it lands is within 3 blocks of where that kid stands then, horizontally (R1.4; 2 before).
 * Further away it was dodged, and drops as a Terugbom. Cover and running away are one rule: there is no
 * line-of-sight check, a block in the way just decides where the bomb lands.
 */
public final class Dodging {
	/** From the middle of the block the bomb lands in to the kid's feet, horizontally. */
	public static final double HIT_RADIUS = 3;
	/**
	 * The landing ring's radius: just inside the blast zone. Particles at a radius have a size and the kid
	 * stands somewhere in their block, so the ring is a little smaller than the zone it marks.
	 */
	public static final double RING_RADIUS = 2.8;
	/** A kid who moved less than this between the throw and the landing stood still. */
	public static final double STILL = 1.0;

	private Dodging() {
	}

	public static boolean isHit(double bombX, double bombZ, double kidX, double kidZ) {
		double dx = bombX - kidX;
		double dz = bombZ - kidZ;
		return dx * dx + dz * dz <= HIT_RADIUS * HIT_RADIUS;
	}

	/** Whether a kid who moved this many blocks (horizontally) between the throw and the landing stood still. */
	public static boolean isStill(double moved) {
		return moved < STILL;
	}

	/**
	 * Point {@code i} of {@code n} on the landing ring around the middle of the landing block.
	 *
	 * @return x and z
	 */
	public static double[] ringPoint(double centreX, double centreZ, int i, int n) {
		double angle = 2 * Math.PI * i / n;
		return new double[] {centreX + RING_RADIUS * Math.cos(angle), centreZ + RING_RADIUS * Math.sin(angle)};
	}

	/**
	 * A kid touches a lying bomb when their feet are strictly inside the window around it: {@code x - 1 < kidX < x + 2},
	 * the same for z, and {@code y - 1 < kidY < y + 2}. That is the bomb's own block and the eight blocks around it,
	 * diagonals too, at its height or standing on it. The window is open, so feet exactly on the low edge of the block
	 * before it ({@code kidX == x - 1}) don't count, and feet exactly on {@code y - 1} or {@code y + 2} don't either.
	 *
	 * @param x the bomb block, as Minecraft counts blocks (its corner, also below zero)
	 */
	public static boolean touches(int x, int y, int z, double kidX, double kidY, double kidZ) {
		return Math.abs(kidX - (x + 0.5)) < 1.5 && Math.abs(kidZ - (z + 0.5)) < 1.5 && kidY > y - 1 && kidY < y + 2;
	}
}
