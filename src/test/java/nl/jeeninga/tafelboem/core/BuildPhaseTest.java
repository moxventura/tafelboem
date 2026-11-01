package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BuildPhaseTest {
	@Test
	void theEasierTheLevelTheLongerTheKidsBuild() {
		assertTrue(BuildPhase.STANDARD.on());
		assertEquals(300, BuildPhase.STANDARD.seconds(FightLevel.MAKKELIJK));
		assertEquals(240, BuildPhase.STANDARD.seconds(FightLevel.GEMIDDELD));
		assertEquals(180, BuildPhase.STANDARD.seconds(FightLevel.MOEILIJK));
	}

	@Test
	void offHasNoBuildTime() {
		assertFalse(BuildPhase.OFF.on());
	}

	@Test
	void testsCanUseOneShortTimeForEveryLevel() {
		BuildPhase phase = BuildPhase.everyLevel(5);
		assertTrue(phase.on());
		for (FightLevel level : FightLevel.DOORS) {
			assertEquals(5, phase.seconds(level));
		}
	}

	@Test
	void theClassicFightHasNoArenaAndNoBuildTime() {
		assertThrows(IllegalArgumentException.class, () -> BuildPhase.STANDARD.seconds(FightLevel.CLASSIC));
	}
}
