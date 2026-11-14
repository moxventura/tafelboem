package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	void theStuckLimitIsFarAboveAnyRealReaction() {
		// 100 is Yeet.MAX_AIRBORNE_TICKS, which core can't see.
		int longestYeet = 100;
		int longestLanding = 0;
		for (LandingStyle landing : LandingStyle.values()) {
			longestLanding = Math.max(longestLanding, landing.ticks());
		}
		assertTrue(Pacing.STUCK_TICKS >= 2 * (longestYeet + longestLanding), "a real reaction must never reach it");
	}

	@Test
	void theR14Constants() {
		assertEquals(90, Pacing.AFTER_DODGE_COOLDOWN_TICKS);
		assertTrue(Pacing.AFTER_DODGE_COOLDOWN_TICKS > Pacing.COOLDOWN_TICKS);
		assertEquals(15 * 20, Pacing.TERUGBOM_LIFETIME_TICKS);
		assertEquals(20, Pacing.STAGGER_TICKS);
		assertEquals(20, Pacing.BACK_COOLDOWN_TICKS);
	}
}
