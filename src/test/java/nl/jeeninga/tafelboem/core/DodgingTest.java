package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DodgingTest {
	@Test
	void threeBlocksAwayIsStillAHitAndFurtherIsDodged() {
		assertTrue(Dodging.isHit(10.5, 10.5, 10.5, 10.5), "right at the kid");
		assertTrue(Dodging.isHit(10.5, 10.5, 12.5, 10.5), "2 blocks: a hit as it was in 1.3.0, and still");
		assertTrue(Dodging.isHit(10.5, 10.5, 13.5, 10.5), "exactly 3 blocks");
		assertFalse(Dodging.isHit(10.5, 10.5, 13.51, 10.5), "just over 3 blocks: dodged");
	}

	@Test
	void theDiagonalAtTheEdgeOfTheZone() {
		assertTrue(Dodging.isHit(10.5, 10.5, 12.5, 12.5), "2.83 blocks diagonally: a hit");
		assertFalse(Dodging.isHit(10.5, 10.5, 12.7, 12.7), "3.11 blocks diagonally: dodged");
	}

	@Test
	void onlyTheHorizontalDistanceCounts() {
		// The spec measures horizontally: a bomb on a roof right above the kid hits; there is no line of sight.
		assertTrue(Dodging.isHit(0.5, 0.5, 1.5, 0.5));
	}

	@Test
	void theRingIsInsideTheBlastZone() {
		assertTrue(Dodging.RING_RADIUS < Dodging.HIT_RADIUS);
		assertEquals(2.8, Dodging.RING_RADIUS);
		for (int i = 0; i < 24; i++) {
			double[] point = Dodging.ringPoint(10.5, 20.5, i, 24);
			assertEquals(Dodging.RING_RADIUS, Math.hypot(point[0] - 10.5, point[1] - 20.5), 1e-9, "point " + i);
			assertTrue(Dodging.isHit(10.5, 20.5, point[0], point[1]), "a kid standing on the ring is hit: " + i);
		}
	}

	@Test
	void theRingPointsAreSpreadAroundTheCircle() {
		double[] first = Dodging.ringPoint(0, 0, 0, 24);
		double[] opposite = Dodging.ringPoint(0, 0, 12, 24);
		assertEquals(2.8, first[0], 1e-9);
		assertEquals(-2.8, opposite[0], 1e-9);
		assertEquals(0, opposite[1], 1e-9);
	}

	@Test
	void aKidWhoMovedLessThanABlockStoodStill() {
		assertTrue(Dodging.isStill(0));
		assertTrue(Dodging.isStill(0.99));
		assertFalse(Dodging.isStill(1.0));
		assertFalse(Dodging.isStill(4.5));
	}

	@Test
	void standingInOrNextToALyingBombTouchesIt() {
		assertTrue(Dodging.touches(0, 64, 0, 0.5, 64, 0.5), "in its block");
		assertTrue(Dodging.touches(0, 64, 0, 1.9, 64, 0.5), "in the block next to it");
		assertTrue(Dodging.touches(0, 64, 0, 1.4, 64, 1.4), "diagonally next to it");
		assertTrue(Dodging.touches(0, 64, 0, 0.5, 65, 0.5), "standing on it");
		assertFalse(Dodging.touches(0, 64, 0, 2.0, 64, 0.5), "two blocks away");
		assertFalse(Dodging.touches(0, 64, 0, 0.5, 66, 0.5), "a block above it");
		assertFalse(Dodging.touches(0, 64, 0, 0.5, 63, 0.5), "under it");
	}

	@Test
	void theWindowIsTheSameBelowZero() {
		// A bomb in block -5 has its middle at -4.5; the window is strictly between -6 and -3: blocks -6, -5 and -4.
		assertTrue(Dodging.touches(-5, 64, -5, -4.5, 64, -4.5), "in its block");
		assertTrue(Dodging.touches(-5, 64, -5, -5.99, 64, -5.99), "at the low end of the block before it, both ways");
		assertTrue(Dodging.touches(-5, 64, -5, -3.01, 64, -3.01), "at the high end of the block after it, both ways");
		assertFalse(Dodging.touches(-5, 64, -5, -6.0, 64, -4.5), "exactly on the low edge of the block before it: the window is open");
		assertFalse(Dodging.touches(-5, 64, -5, -4.5, 64, -6.0), "the same on the other axis");
		assertFalse(Dodging.touches(-5, 64, -5, -6.01, 64, -4.5), "in the block two before it");
		assertFalse(Dodging.touches(-5, 64, -5, -3.0, 64, -4.5), "in the second block after it");
		assertTrue(Dodging.touches(-1, 64, -1, 0.99, 64, 0.0), "across zero, from block -1 into block 0");
		assertFalse(Dodging.touches(-1, 64, -1, 1.0, 64, 0.0), "but not into block 1");
	}
}
