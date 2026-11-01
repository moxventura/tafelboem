package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrapKindTest {
	@Test
	void eachTrapHasItsOwnReaction() {
		assertEquals(LandingStyle.PRICKLED, TrapKind.CACTUS.reaction());
		assertEquals(LandingStyle.PRICKLED, TrapKind.BERRY_BUSH.reaction());
		assertEquals(LandingStyle.FRANTIC_RUN, TrapKind.MAGMA.reaction(), "a hot-feet dance");
		assertEquals(LandingStyle.WEBBED, TrapKind.COBWEB.reaction());
		assertEquals(LandingStyle.BOING, TrapKind.SLIME.reaction());
		assertEquals(LandingStyle.SOOT, TrapKind.LYING_BOMB.reaction(), "it goes off under him");
	}

	@Test
	void heLandsOnSolidTrapsAndInSoftOnes() {
		assertTrue(TrapKind.CACTUS.landsOnTop());
		assertTrue(TrapKind.MAGMA.landsOnTop());
		assertTrue(TrapKind.SLIME.landsOnTop());
		assertTrue(TrapKind.LYING_BOMB.landsOnTop());
		assertFalse(TrapKind.COBWEB.landsOnTop());
		assertFalse(TrapKind.BERRY_BUSH.landsOnTop());
	}

	@Test
	void onlyCactusAndBerriesPrickAndTheLogSaysWhichTrap() {
		assertTrue(TrapKind.CACTUS.isPrickly() && TrapKind.BERRY_BUSH.isPrickly());
		assertFalse(TrapKind.COBWEB.isPrickly());
		assertEquals("lying_bomb", TrapKind.LYING_BOMB.id());
		assertEquals("berry_bush", TrapKind.BERRY_BUSH.id());
	}
}
