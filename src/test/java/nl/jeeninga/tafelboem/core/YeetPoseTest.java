package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The tumble and the faceplant turn Graaf Fout further than his usual spin; these are the angles the
 * renderer applies.
 */
class YeetPoseTest {
	@Test
	void standingUpHasNoExtraTurn() {
		assertEquals(YeetPose.Angles.UPRIGHT, YeetPose.NONE.angles(17));
	}

	@Test
	void aTumbleRollsAndFlipsAtDifferentSpeeds() {
		YeetPose.Angles angles = YeetPose.TUMBLE.angles(1);
		assertNotEquals(0F, angles.roll(), "he rolls sideways");
		assertNotEquals(0F, angles.pitch(), "he flips head over heels");
		assertNotEquals(angles.roll(), angles.pitch(), "two speeds make it look like a real tumble, not a wheel");
	}

	@Test
	void tumbleAnglesStayBetweenMinusAndPlusHalfATurn() {
		for (float ticks = 0; ticks < 100; ticks += 0.25F) {
			YeetPose.Angles angles = YeetPose.TUMBLE.angles(ticks);
			assertTrue(angles.roll() >= -180F && angles.roll() < 180F, "roll " + angles.roll() + " at " + ticks);
			assertTrue(angles.pitch() >= -180F && angles.pitch() < 180F, "pitch " + angles.pitch() + " at " + ticks);
		}
	}

	@Test
	void aFaceplantTipsForwardOntoHisFaceAndStaysDown() {
		assertEquals(0F, YeetPose.FACEPLANT.angles(0).pitch());
		assertEquals(-90F, YeetPose.FACEPLANT.angles(YeetPose.FACEPLANT_TICKS).pitch());
		assertEquals(-90F, YeetPose.FACEPLANT.angles(30).pitch(), "he lies on his face while he is dizzy");
		assertEquals(0F, YeetPose.FACEPLANT.angles(30).roll());
	}

	@Test
	void afterAPoseHeSettlesBackUprightTheShortWayRound() {
		YeetPose.Angles from = new YeetPose.Angles(-20F, 160F);

		assertEquals(from, YeetPose.settle(from, 0));
		YeetPose.Angles halfway = YeetPose.settle(from, YeetPose.SETTLE_TICKS / 2F);
		assertEquals(-10F, halfway.roll(), 0.001F);
		assertEquals(80F, halfway.pitch(), 0.001F);
		assertEquals(YeetPose.Angles.UPRIGHT, YeetPose.settle(from, YeetPose.SETTLE_TICKS));
		assertEquals(YeetPose.Angles.UPRIGHT, YeetPose.settle(from, 100));
	}
}
