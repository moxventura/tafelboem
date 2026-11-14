package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HomingTest {
	/** Graaf Fout's box: 0.6 wide, 1.95 high, standing at the origin on the floor. */
	private static final double HALF_WIDTH = 0.3;
	private static final double HEIGHT = 1.95;
	private static final double SPEED = 1.5;
	private static final double GRAVITY = 0.03;

	@Test
	void steeringKeepsTheSpeedAndTurnsTowardsHim() {
		double[] v = Homing.steer(0, 0, -1.5, 3, 0, -3);

		assertEquals(1.5, Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]), 1e-9, "the speed stays");
		assertTrue(v[0] > 0, "it bends to the side he is on");
		assertTrue(v[2] < 0, "and still flies forward");
	}

	@Test
	void aBombHeadingStraightForHimKeepsItsCourse() {
		double[] v = Homing.steer(0, 0, -1.5, 0, 0, -4);
		assertEquals(0, v[0], 1e-9);
		assertEquals(-1.5, v[2], 1e-9);
	}

	@Test
	void aBombThatStandsStillOrIsOnTopOfHimIsLeftAlone() {
		assertEquals(0, Homing.steer(0, 0, 0, 3, 0, 0)[0]);
		assertEquals(1.5, Homing.steer(1.5, 0, 0, 0, 0, 0)[0]);
	}

	@Test
	void theRangeIsFiveBlocksAndSixWhileHeFlies() {
		assertEquals(5, Homing.range(false));
		assertEquals(6, Homing.range(true));
		assertEquals(1, Homing.HIT_MARGIN);
	}

	/**
	 * A kid 12 blocks off throws straight ahead and misses him by {@code aside} blocks; the bomb sags a little
	 * until it is near him, then homes in. Whether it enters his box with its margin, as the spec's aim help
	 * demands: the younger kid hits a standing Graaf Fout at least half the time.
	 */
	private static boolean hits(double aside) {
		double[] p = {aside, 1.4, 12};
		double[] v = {0, 0, -SPEED};
		double margin = Homing.HIT_MARGIN;
		for (int tick = 0; tick < 60; tick++) {
			double[] next = {p[0] + v[0], p[1] + v[1], p[2] + v[2]};
			for (int step = 0; step <= 10; step++) {
				double f = step / 10.0;
				double x = p[0] + (next[0] - p[0]) * f;
				double y = p[1] + (next[1] - p[1]) * f;
				double z = p[2] + (next[2] - p[2]) * f;
				if (Math.abs(x) <= HALF_WIDTH + margin && Math.abs(z) <= HALF_WIDTH + margin && y >= -margin && y <= HEIGHT + margin) {
					return true;
				}
			}
			double[] toHim = {-p[0], HEIGHT / 2 - p[1], -p[2]};
			if (Math.sqrt(toHim[0] * toHim[0] + toHim[1] * toHim[1] + toHim[2] * toHim[2]) <= Homing.RANGE) {
				v = Homing.steer(v[0], v[1], v[2], toHim[0], toHim[1], toHim[2]);
			} else {
				v[1] -= GRAVITY;
			}
			p = next;
			if (p[2] < -8 || p[1] < -3) {
				return false;
			}
		}
		return false;
	}

	@Test
	void aThrowThatMissesHimByUpToFourBlocksStillHits() {
		for (double aside = 0; aside <= 4.0; aside += 0.5) {
			assertTrue(hits(aside), "missing him by " + aside + " blocks");
		}
	}

	@Test
	void aThrowThatPassesFarOutsideTheRangeMisses() {
		assertFalse(hits(7), "7 blocks beside him is outside the homing range");
	}
}
