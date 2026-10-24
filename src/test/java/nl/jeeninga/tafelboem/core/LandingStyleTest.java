package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class LandingStyleTest {
	@Test
	void everyBombHasASignatureOfItsOwn() {
		Set<LandingStyle> signatures = Arrays.stream(BombTier.values()).map(LandingStyle::signatureOf).collect(Collectors.toSet());
		assertEquals(BombTier.values().length, signatures.size());
		signatures.forEach(signature -> assertTrue(signature.isSignature(), signature.name()));
		assertEquals(EnumSet.complementOf(EnumSet.copyOf(LandingStyle.GENERIC)), EnumSet.copyOf(signatures));
	}

	@Test
	void runningRoundAndTheChaseLookAlike() {
		assertTrue(LandingStyle.FRANTIC_RUN.looksLike(LandingStyle.CHASED));
		assertTrue(LandingStyle.CHASED.looksLike(LandingStyle.FRANTIC_RUN));
		assertTrue(LandingStyle.PANCAKE.looksLike(LandingStyle.PANCAKE));
		assertFalse(LandingStyle.PANCAKE.looksLike(LandingStyle.HEAD_FIRST));
		assertFalse(LandingStyle.CHASED.looksLike(null), "nothing came before the first landing");
	}

	@Test
	void noLandingLastsLongerThanTwoSeconds() {
		for (LandingStyle landing : LandingStyle.values()) {
			assertTrue(landing.ticks() <= 40, landing.name());
		}
		assertEquals(30, LandingStyle.DIZZY.ticks(), "the R1 dizzy spell");
	}
}
