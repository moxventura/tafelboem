package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MotionTest {
	@Test
	void aKidWithNoHistoryOrOneSampleIsStandingStill() {
		Motion motion = new Motion();
		assertEquals(0, motion.velX());
		motion.record(10, 10);
		assertEquals(0, motion.velX());
		assertEquals(0, motion.velZ());
		assertEquals(0, motion.speed());
	}

	@Test
	void theVelocityIsTheMovementOverTheLastFiveTicks() {
		Motion motion = new Motion();
		for (int tick = 0; tick <= 8; tick++) {
			// Nine positions of running east at 0.2 a tick: the velocity is the movement over the last five ticks (six positions).
			motion.record(100 + 0.2 * tick, 50);
		}
		assertEquals(0.2, motion.velX(), 1e-9);
		assertEquals(0, motion.velZ(), 1e-9);
		assertEquals(4.0, motion.speed(), 1e-9, "0.2 blocks a tick is 4 blocks a second");
	}

	@Test
	void aKidWhoTurnedRecentlyShowsTheNetMovementOfTheWindow() {
		Motion motion = new Motion();
		double[] xs = {0, 1, 2, 3, 2.5, 2.0, 1.5};
		for (double x : xs) {
			motion.record(x, 0);
		}
		// The window holds 1, 2, 3, 2.5, 2.0, 1.5: still 0.5 east over five ticks, though he turned two ticks ago.
		assertEquals(0.1, motion.velX(), 1e-9);
	}

	@Test
	void aTeleportIsNoRunning() {
		Motion motion = new Motion();
		motion.record(0, 0);
		motion.record(0.2, 0);
		motion.record(400, 0);
		assertEquals(0, motion.velX(), "the history before the jump is dropped");
		motion.record(400.2, 0);
		assertEquals(0.2, motion.velX(), 1e-9, "and the kid is read again from where they landed");
	}
}
