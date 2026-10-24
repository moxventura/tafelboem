package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PacingTest {
	@Test
	void fromAKickToTheNextThrowTakesAsLongWhicheverLandingHeGets() {
		int r1 = LandingStyle.DIZZY.ticks() + Pacing.COOLDOWN_TICKS;
		for (LandingStyle landing : LandingStyle.values()) {
			assertEquals(r1, landing.ticks() + Pacing.cooldownAfter(landing), landing.name());
		}
	}

	@Test
	void afterTheDizzySpellTheR1Pause() {
		assertEquals(50, Pacing.cooldownAfter(LandingStyle.DIZZY));
	}
}
