package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class YeetPowerTest {
	@Test
	void aThrowBackIsOnePointTwoFiveAndASuperYeetOnePointFiveOnTopOfTheBoost() {
		assertEquals(1.0, YeetPower.factor(YeetKind.KICK, 1.0), 1e-9);
		assertEquals(1.25, YeetPower.factor(YeetKind.BACK, 1.0), 1e-9);
		assertEquals(1.5, YeetPower.factor(YeetKind.SUPER, 1.0), 1e-9);
		assertEquals(1.3 * 1.25, YeetPower.factor(YeetKind.BACK, YeetBoost.MAX), 1e-9, "on top of the fastest, longest streak");
		assertEquals(1.3 * 1.5, YeetPower.factor(YeetKind.SUPER, YeetBoost.MAX), 1e-9);
	}

	@Test
	void aKickIsNeverCappedSoItIsAsItWas() {
		double strongest = YeetPower.power(YeetKind.KICK, 1.4, BombTier.KIPPENBOM.yeetPower(), YeetBoost.MAX);
		assertEquals(1.4 * 1.8 * 1.3, strongest, 1e-9);
		assertTrue(strongest > YeetPower.MAX_POWER, "the strongest kick has always gone further than the cap");
	}

	@Test
	void aThrowBackAndASuperYeetAreCapped() {
		double back = YeetPower.power(YeetKind.BACK, 1.4, BombTier.KIPPENBOM.yeetPower(), YeetPower.factor(YeetKind.BACK, YeetBoost.MAX));
		double superYeet = YeetPower.power(YeetKind.SUPER, 1.4, BombTier.KIPPENBOM.yeetPower(), YeetPower.factor(YeetKind.SUPER, YeetBoost.MAX));
		assertEquals(YeetPower.MAX_POWER, back, 1e-9);
		assertEquals(YeetPower.MAX_POWER, superYeet, 1e-9);
	}

	@Test
	void theCapLeavesTheWeakestThrowBacksAlone() {
		double weakest = YeetPower.power(YeetKind.BACK, 0.9, BombTier.KNALLETJE.yeetPower(), YeetPower.factor(YeetKind.BACK, 1.0));
		assertEquals(0.9 * 1.0 * 1.25, weakest, 1e-9);
		assertTrue(weakest < YeetPower.MAX_POWER);
	}

	@Test
	void aSuperYeetBeatsAThrowBackWhereTheCapAllowsIt() {
		double back = YeetPower.power(YeetKind.BACK, 1.15, BombTier.KNALLETJE.yeetPower(), 1.25);
		double superYeet = YeetPower.power(YeetKind.SUPER, 1.15, BombTier.KNALLETJE.yeetPower(), 1.5);
		assertTrue(superYeet > back);
	}

	@Test
	void theKindsHaveTheirOwnLogIds() {
		assertEquals("kick", YeetKind.KICK.id());
		assertEquals("back", YeetKind.BACK.id());
		assertEquals("super", YeetKind.SUPER.id());
	}
}
