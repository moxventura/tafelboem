package nl.jeeninga.tafelboem.core;

import java.util.List;
import java.util.function.Predicate;

/**
 * Graaf Fout's frantic run: a small circle, starting where he landed. Every step must be over
 * ground, inside the arena and at least 2 blocks from every kid; when the next one isn't, he turns
 * round, and when neither way is free he stands still for a tick.
 */
public final class RunCircle {
	public static final double RADIUS = 2.5;
	/** He runs at 0.3 blocks a tick, so a step round the circle is 0.3 / 2.5 radians. */
	public static final double STEP = 0.12;
	public static final double KID_DISTANCE = 2;

	/** A spot on the ground, as the x and z of its middle. */
	public record Point(double x, double z) {
	}

	private final double centreX;
	private final double centreZ;
	private double angle;
	private double direction = 1;

	/** @param startAngle where on the circle he starts, in radians; the circle goes through (x, z) there */
	public RunCircle(double x, double z, double startAngle) {
		this.centreX = x - Math.cos(startAngle) * RADIUS;
		this.centreZ = z - Math.sin(startAngle) * RADIUS;
		this.angle = startAngle;
	}

	public double centreX() {
		return centreX;
	}

	public double centreZ() {
		return centreZ;
	}

	/**
	 * Where to run to next.
	 *
	 * @param arena  null outside an arena
	 * @param ground whether a spot has ground he can run on
	 * @return the next point, or null to stand still this tick
	 */
	public Point next(ArenaBox arena, List<Point> kids, Predicate<Point> ground) {
		for (int attempt = 0; attempt < 2; attempt++) {
			double next = angle + direction * STEP;
			Point point = new Point(centreX + Math.cos(next) * RADIUS, centreZ + Math.sin(next) * RADIUS);
			if (allowed(point, arena, kids, ground)) {
				angle = next;
				return point;
			}
			direction = -direction;
		}
		return null;
	}

	private static boolean allowed(Point point, ArenaBox arena, List<Point> kids, Predicate<Point> ground) {
		if (arena != null && !arena.containsColumn(point.x(), point.z())) {
			return false;
		}
		for (Point kid : kids) {
			if (Math.hypot(point.x() - kid.x(), point.z() - kid.z()) < KID_DISTANCE) {
				return false;
			}
		}
		return ground.test(point);
	}
}
