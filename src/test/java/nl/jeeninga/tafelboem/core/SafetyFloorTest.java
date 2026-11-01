package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SafetyFloorTest {
	/** Minecraft's ids: 0 peaceful, 1 easy, 2 normal, 3 hard. */
	@Test
	void peacefulAndEasyAreSafeNormalAndHardAreVanilla() {
		assertTrue(SafetyFloor.protects(true, 0));
		assertTrue(SafetyFloor.protects(true, 1));
		assertFalse(SafetyFloor.protects(true, 2));
		assertFalse(SafetyFloor.protects(true, 3));
	}

	@Test
	void withTheSwitchOffNoDifficultyIsSafe() {
		for (int difficulty = 0; difficulty <= 3; difficulty++) {
			assertFalse(SafetyFloor.protects(false, difficulty), "difficulty " + difficulty);
		}
	}

	@Test
	void aHitNeverTakesTheLastHalfHeart() {
		assertEquals(4F, SafetyFloor.allowed(20F, 4F), "a small hit lands whole");
		assertEquals(19F, SafetyFloor.allowed(20F, 100F), "a deadly fall leaves half a heart");
		assertEquals(0F, SafetyFloor.allowed(1F, 3F), "at half a heart nothing lands");
		assertEquals(0F, SafetyFloor.allowed(0.5F, 3F), "never a negative hit");
	}
}
