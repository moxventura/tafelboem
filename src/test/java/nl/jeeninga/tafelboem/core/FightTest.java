package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FightTest {
	@Test
	void levelOneNeedsElevenHits() {
		Fight fight = new Fight(1);
		assertEquals(11, fight.hitsRequired());
		for (int i = 0; i < 10; i++) {
			fight.registerHit();
		}
		assertFalse(fight.isDefeated());
		assertEquals(1, fight.hitsRemaining());
		fight.registerHit();
		assertTrue(fight.isDefeated());
	}

	@Test
	void hitsAfterDefeatAreIgnored() {
		Fight fight = new Fight(1);
		for (int i = 0; i < 20; i++) {
			fight.registerHit();
		}
		assertEquals(0, fight.hitsRemaining());
	}

	@Test
	void onlyThreeChickenOutsPerFight() {
		Fight fight = new Fight(1);
		assertTrue(fight.tryChickenOut());
		assertTrue(fight.tryChickenOut());
		assertTrue(fight.tryChickenOut());
		assertFalse(fight.tryChickenOut());
	}

	@Test
	void progressIsAFractionForTheBossBar() {
		Fight fight = new Fight(1);
		assertEquals(1.0f, fight.healthFraction());
		fight.registerHit();
		assertEquals(10f / 11f, fight.healthFraction(), 1e-6);
	}
}
