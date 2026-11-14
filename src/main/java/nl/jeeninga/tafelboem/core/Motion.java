package nl.jeeninga.tafelboem.core;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Where a kid is going, read from their position over the last 5 ticks (R1.4): a server player's own
 * velocity is near zero, so Graaf Fout can't use it. One per kid, fed once a tick.
 */
public final class Motion {
	/** The window: this many ticks back. */
	public static final int TICKS = 5;
	/** A kid who jumps further than this in one tick was teleported (the void catch, a door): that is no running. */
	static final double TELEPORT = 3;

	private final Deque<double[]> samples = new ArrayDeque<>();

	/** The kid's position now; call once a tick. */
	public void record(double x, double z) {
		double[] last = samples.peekLast();
		if (last != null && Math.hypot(x - last[0], z - last[1]) > TELEPORT) {
			samples.clear();
		}
		samples.addLast(new double[] {x, z});
		while (samples.size() > TICKS + 1) {
			samples.removeFirst();
		}
	}

	/** Blocks per tick along x; 0 with fewer than two positions. */
	public double velX() {
		return samples.size() < 2 ? 0 : (samples.peekLast()[0] - samples.peekFirst()[0]) / (samples.size() - 1);
	}

	/** Blocks per tick along z; 0 with fewer than two positions. */
	public double velZ() {
		return samples.size() < 2 ? 0 : (samples.peekLast()[1] - samples.peekFirst()[1]) / (samples.size() - 1);
	}

	/** Blocks per second, for the throw line of the log. */
	public double speed() {
		return Math.hypot(velX(), velZ()) * 20;
	}
}
