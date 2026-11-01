package nl.jeeninga.tafelboem.core;

/**
 * R1.3's dodge rule: a bomb Graaf Fout threw hits its kid only when it lands within 2 blocks of
 * where that kid stands then, horizontally; further away it lies still until a kid touches it. Cover
 * and running away are one rule: there is no line-of-sight check.
 */
public final class Dodging {
	/** From the middle of the block the bomb lands in to the kid's feet, horizontally. */
	public static final double HIT_RADIUS = 2;

	private Dodging() {
	}

	public static boolean isHit(double bombX, double bombZ, double kidX, double kidZ) {
		double dx = bombX - kidX;
		double dz = bombZ - kidZ;
		return dx * dx + dz * dz <= HIT_RADIUS * HIT_RADIUS;
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
