package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReactionPickerTest {
	@ParameterizedTest
	@ValueSource(longs = {1, 2, 3, 42})
	void neverTheSameFlightTwiceInARow(long seed) {
		ReactionPicker picker = new ReactionPicker(new Random(seed));
		YeetStyle previous = picker.nextFlight(true, true);
		for (int i = 0; i < 500; i++) {
			YeetStyle current = picker.nextFlight(true, true);
			assertNotEquals(previous, current, "repeat at yeet " + i);
			previous = current;
		}
	}

	@Test
	void everyFlightShowsUpWithinOneFight() {
		// A level-1 fight has 11 hits; the kids should see every kind of yeet in it.
		ReactionPicker picker = new ReactionPicker(new Random(7));
		Set<YeetStyle> seen = EnumSet.noneOf(YeetStyle.class);
		for (int i = 0; i < YeetStyle.values().length; i++) {
			seen.add(picker.nextFlight(true, true));
		}
		assertEquals(EnumSet.allOf(YeetStyle.class), seen);
	}

	@Test
	void theWallSplatOnlyComesInAnArena() {
		ReactionPicker picker = new ReactionPicker(new Random(9));
		for (int i = 0; i < 200; i++) {
			assertNotEquals(YeetStyle.WALL_SPLAT, picker.nextFlight(false, true));
		}
	}

	@Test
	void withoutTheCatalogueOnlyTheR1Flights() {
		ReactionPicker picker = new ReactionPicker(new Random(10));
		for (int i = 0; i < 200; i++) {
			assertNotEquals(YeetStyle.WALL_SPLAT, picker.nextFlight(true, false));
		}
	}

	@Test
	void theWallSplatKeepsItsOwnEnding() {
		ReactionPicker picker = new ReactionPicker(new Random(11));
		assertEquals(LandingStyle.DIZZY, picker.nextLanding(BombTier.KNALLETJE, YeetStyle.WALL_SPLAT, true, true));
	}

	@Test
	void withTheCatalogueAndSignaturesOffEveryLandingIsTheDizzySpell() {
		ReactionPicker picker = new ReactionPicker(new Random(1));
		for (int i = 0; i < 50; i++) {
			assertEquals(LandingStyle.DIZZY, picker.nextLanding(BombTier.KIPPENBOM, YeetStyle.CLASSIC, false, false));
		}
	}

	@ParameterizedTest
	@ValueSource(longs = {1, 2, 3, 42})
	void neverTheSameLandingTwiceInARowWithRunningAndTheChaseAsOne(long seed) {
		Random random = new Random(seed);
		ReactionPicker picker = new ReactionPicker(random);
		LandingStyle previous = null;
		for (int i = 0; i < 500; i++) {
			BombTier bomb = BombTier.values()[random.nextInt(BombTier.values().length)];
			LandingStyle landing = picker.nextLanding(bomb, YeetStyle.CLASSIC, true, true);
			assertFalse(landing.looksLike(previous), previous + " then " + landing + " at landing " + i);
			previous = landing;
		}
	}

	@Test
	void aBombsSignatureComesEveryOtherKick() {
		ReactionPicker picker = new ReactionPicker(new Random(2));
		List<LandingStyle> landings = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			landings.add(picker.nextLanding(BombTier.KNALLETJE, YeetStyle.CLASSIC, true, true));
		}
		for (int i = 0; i < landings.size(); i++) {
			assertEquals(i % 2 == 0, landings.get(i) == LandingStyle.TOE_HOP, landings.toString());
		}
	}

	@Test
	void theChaseWaitsWhenHeJustRanRound() {
		ReactionPicker picker = new ReactionPicker(new Random(4));
		picker.played(LandingStyle.FRANTIC_RUN);
		assertNotEquals(LandingStyle.CHASED, picker.nextLanding(BombTier.KIPPENBOM, YeetStyle.CLASSIC, true, true));
	}

	@Test
	void theBoomerangKeepsItsOwnEndingAndTheSignatureWaitsForTheNextKick() {
		ReactionPicker picker = new ReactionPicker(new Random(5));
		assertEquals(LandingStyle.DIZZY, picker.nextLanding(BombTier.KNALLETJE, YeetStyle.BOOMERANG, true, true));
		assertEquals(LandingStyle.TOE_HOP, picker.nextLanding(BombTier.KNALLETJE, YeetStyle.CLASSIC, true, true));
	}

	@Test
	void signaturesWithoutTheCatalogueTakeTurnsWithTheDizzySpell() {
		ReactionPicker picker = new ReactionPicker(new Random(6));
		assertEquals(LandingStyle.SOOT, picker.nextLanding(BombTier.GEWONE_TNT, YeetStyle.CLASSIC, false, true));
		assertEquals(LandingStyle.DIZZY, picker.nextLanding(BombTier.GEWONE_TNT, YeetStyle.CLASSIC, false, true));
		assertEquals(LandingStyle.SOOT, picker.nextLanding(BombTier.GEWONE_TNT, YeetStyle.CLASSIC, false, true));
	}

	@Test
	void afterFlyingOutTheNextLandingIsNoDizzySpell() {
		ReactionPicker picker = new ReactionPicker(new Random(8));
		picker.played(LandingStyle.DIZZY);
		assertNotEquals(LandingStyle.DIZZY, picker.nextLanding(BombTier.KNALLETJE, YeetStyle.CLASSIC, true, false));
	}
}
