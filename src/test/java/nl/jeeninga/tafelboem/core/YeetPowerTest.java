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

	/**
	 * The price of a cap that keeps a juggle from 15 blocks out inside the arena (see {@link YeetPower#MAX_POWER}): even
	 * the weakest throw-back flies at the cap, and the weakest kick flies further. For the evaluation's watch list.
	 */
	@Test
	void atThisCapEveryThrowBackAndSuperYeetFliesAtTheCapShorterThanAnyKick() {
		double weakestBack = YeetPower.power(YeetKind.BACK, 0.9, BombTier.KNALLETJE.yeetPower(), YeetPower.factor(YeetKind.BACK, 1.0));
		assertEquals(YeetPower.MAX_POWER, weakestBack, 1e-9);
		double weakestKick = YeetPower.power(YeetKind.KICK, 0.9, BombTier.KNALLETJE.yeetPower(), 1.0);
		assertTrue(weakestKick > YeetPower.MAX_POWER, "the weakest kick, " + weakestKick + ", flies further than the cap");
	}

	@Test
	void theKindsHaveTheirOwnLogIds() {
		assertEquals("kick", YeetKind.KICK.id());
		assertEquals("back", YeetKind.BACK.id());
		assertEquals("super", YeetKind.SUPER.id());
	}
}
