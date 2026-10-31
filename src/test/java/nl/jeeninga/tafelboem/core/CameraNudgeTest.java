package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CameraNudgeTest {
	@Test
	void offByIsTheLargerOfTheTurnAndTheTilt() {
		assertEquals(30F, CameraNudge.offBy(0F, 10F, 30F, 0F), 0.001F);
		assertEquals(40F, CameraNudge.offBy(0F, 40F, 10F, 0F), 0.001F);
	}

	@Test
	void offByTurnsTheShortWayRound() {
		assertEquals(20F, CameraNudge.offBy(170F, 0F, -170F, 0F), 0.001F);
		assertEquals(20F, CameraNudge.offBy(-350F, 0F, 30F, 0F), 0.001F);
	}

	@Test
	void aLandingInViewNeedsNoNudge() {
		assertFalse(CameraNudge.landingNeedsNudge(CameraNudge.IN_VIEW_DEGREES));
		assertTrue(CameraNudge.landingNeedsNudge(CameraNudge.IN_VIEW_DEGREES + 1F));
	}

	@Test
	void aNudgeEndsOnceHeIsCentred() {
		assertFalse(CameraNudge.done(1, CameraNudge.CENTRED_DEGREES + 1F));
		assertTrue(CameraNudge.done(1, CameraNudge.CENTRED_DEGREES));
	}

	@Test
	void aNudgeNeverLastsLongerThanHalfASecond() {
		assertTrue(CameraNudge.MAX_TICKS <= 10);
		assertFalse(CameraNudge.done(CameraNudge.MAX_TICKS - 1, 90F));
		assertTrue(CameraNudge.done(CameraNudge.MAX_TICKS, 90F));
	}
}
