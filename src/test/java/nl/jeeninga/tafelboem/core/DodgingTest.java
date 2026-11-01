package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DodgingTest {
	@Test
	void twoBlocksAwayIsStillAHitAndFurtherIsDodged() {
		assertTrue(Dodging.isHit(10.5, 10.5, 10.5, 10.5), "right at the kid");
		assertTrue(Dodging.isHit(10.5, 10.5, 12.5, 10.5), "exactly 2 blocks");
		assertFalse(Dodging.isHit(10.5, 10.5, 12.51, 10.5), "just over 2 blocks: dodged");
		assertFalse(Dodging.isHit(10.5, 10.5, 12.0, 12.0), "2.1 blocks diagonally: dodged");
	}

	@Test
	void onlyTheHorizontalDistanceCounts() {
		// The spec measures horizontally: a bomb on a roof right above the kid hits; there is no line of sight.
		assertTrue(Dodging.isHit(0.5, 0.5, 1.5, 0.5));
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
}
