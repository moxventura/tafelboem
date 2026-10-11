package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Playtest 1: the kids couldn't tell the bombs apart, so each one must look and hit differently.
 */
class BombTierTest {
	@Test
	void everyBombHasItsOwnTrailColour() {
		long distinct = Arrays.stream(BombTier.values()).mapToInt(BombTier::trailColor).distinct().count();
		assertEquals(BombTier.values().length, distinct);
	}

	@Test
	void aBiggerBombYeetsGraafFoutFurther() {
		BombTier[] bombs = BombTier.values();
		for (int i = 1; i < bombs.length; i++) {
			assertTrue(bombs[i].tier() > bombs[i - 1].tier(), "bombs are listed by tier");
			assertTrue(bombs[i].yeetPower() > bombs[i - 1].yeetPower(), bombs[i] + " yeets further than " + bombs[i - 1]);
		}
	}
}
