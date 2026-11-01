package nl.jeeninga.tafelboem.core;

import java.util.List;

/**
 * Which trap pulls Graaf Fout in at the end of one yeet: the nearest within 4 blocks (horizontally)
 * of where he would land, and at most one per yeet. One instance per yeet.
 */
public final class TrapPick {
	/** Horizontally, from where he would land to the middle of the trap block. */
	public static final double REACH = 4;

	/** A trap block, as Minecraft counts blocks (its corner), and what it is. */
	public record Candidate(int x, int y, int z, TrapKind kind) {
		double distanceSqr(double fromX, double fromZ) {
			double dx = x + 0.5 - fromX;
			double dz = z + 0.5 - fromZ;
			return dx * dx + dz * dz;
		}
	}

	private boolean picked;

	/** The nearest candidate in reach the first time there is one; null otherwise, and ever after. */
	public Candidate pick(double x, double z, List<Candidate> candidates) {
		if (picked) {
			return null;
		}
		Candidate nearest = nearest(x, z, candidates);
		picked = nearest != null;
		return nearest;
	}

	/** The nearest candidate within {@link #REACH}, the first of equals; null when none is in reach. */
	public static Candidate nearest(double x, double z, List<Candidate> candidates) {
		Candidate best = null;
		double bestSqr = REACH * REACH;
		for (Candidate candidate : candidates) {
			double distanceSqr = candidate.distanceSqr(x, z);
			if (best == null ? distanceSqr <= bestSqr : distanceSqr < bestSqr) {
				best = candidate;
				bestSqr = distanceSqr;
			}
		}
		return best;
	}
}
