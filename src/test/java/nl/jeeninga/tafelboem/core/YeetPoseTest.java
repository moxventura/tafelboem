package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nl.jeeninga.tafelboem.core.YeetPose.Transform;

/**
 * The tumble, the faceplant, head-first and the pancake turn, sink or squash Graaf Fout on top of
 * his usual spin; these are what the renderer applies.
 */
class YeetPoseTest {
	@Test
	void aSplatIsFlatAgainstTheWall() {
		Transform splat = YeetPose.SPLAT.transform(2);
		assertEquals(YeetPose.SPLAT_DEPTH, splat.depth());
		assertEquals(1F, splat.height(), "flat front to back, not top to bottom");
	}

	@Test
	void standingUpHasNoExtraTurnOrSquash() {
		assertEquals(Transform.UPRIGHT, YeetPose.NONE.transform(17));
	}

	@Test
	void aTumbleRollsAndFlipsAtDifferentSpeeds() {
		Transform transform = YeetPose.TUMBLE.transform(1);
		assertNotEquals(0F, transform.roll(), "he rolls sideways");
		assertNotEquals(0F, transform.pitch(), "he flips head over heels");
		assertNotEquals(transform.roll(), transform.pitch(), "two speeds make it look like a real tumble, not a wheel");
	}

	@Test
	void tumbleAnglesStayBetweenMinusAndPlusHalfATurn() {
		for (float ticks = 0; ticks < 100; ticks += 0.25F) {
			Transform transform = YeetPose.TUMBLE.transform(ticks);
			assertTrue(transform.roll() >= -180F && transform.roll() < 180F, "roll " + transform.roll() + " at " + ticks);
			assertTrue(transform.pitch() >= -180F && transform.pitch() < 180F, "pitch " + transform.pitch() + " at " + ticks);
		}
	}

	@Test
	void aFaceplantTipsForwardOntoHisFaceAndStaysDown() {
		assertEquals(0F, YeetPose.FACEPLANT.transform(0).pitch());
		assertEquals(-90F, YeetPose.FACEPLANT.transform(YeetPose.FACEPLANT_TICKS).pitch());
		assertEquals(-90F, YeetPose.FACEPLANT.transform(30).pitch(), "he lies on his face while he is dizzy");
		assertEquals(0F, YeetPose.FACEPLANT.transform(30).roll());
	}

	@Test
	void headFirstHeIsUpsideDownAndSunkIntoTheGround() {
		assertTrue(YeetPose.HEAD_FIRST.upsideDown());
		assertEquals(YeetPose.HEAD_FIRST_SINK, YeetPose.HEAD_FIRST.transform(5).sink());
		assertFalse(YeetPose.PANCAKE.upsideDown());
	}

	@Test
	void theSignaturePosesChangeHowHeLooksNotHisShape() {
		assertEquals(Transform.UPRIGHT, YeetPose.SOOT.transform(10));
		assertEquals(Transform.UPRIGHT, YeetPose.TOE_HOP.transform(10));
	}

	@Test
	void aPancakeIsFlatButNotThin() {
		Transform flat = YeetPose.PANCAKE.transform(3);
		assertEquals(YeetPose.PANCAKE_HEIGHT, flat.height());
		assertEquals(1F, flat.depth());
	}

	@Test
	void afterAPoseHeSettlesBackTheShortWayRoundAndBoingsBackIntoShape() {
		Transform from = new Transform(-20F, 160F, 0.25F, 1F, 0.6F);

		assertEquals(from, YeetPose.settle(from, 0));
		Transform halfway = YeetPose.settle(from, YeetPose.SETTLE_TICKS / 2F);
		assertEquals(-10F, halfway.roll(), 0.001F);
		assertEquals(80F, halfway.pitch(), 0.001F);
		assertEquals(0.625F, halfway.height(), 0.001F);
		assertEquals(0.3F, halfway.sink(), 0.001F);
		assertEquals(Transform.UPRIGHT, YeetPose.settle(from, YeetPose.SETTLE_TICKS));
		assertEquals(Transform.UPRIGHT, YeetPose.settle(from, 100));
	}

	@Test
	void onlyTheTumbleTurnsAroundHisMiddle() {
		assertEquals(0.5F, YeetPose.TUMBLE.pivotShare());
		assertEquals(0F, YeetPose.FACEPLANT.pivotShare(), "the faceplant tips over his feet");
	}

	@Test
	void aPoseFromANewerVersionShowsAsNone() {
		assertEquals(YeetPose.NONE, YeetPose.byId(99));
		assertEquals(YeetPose.NONE, YeetPose.byId(-1));
		assertEquals(YeetPose.PANCAKE, YeetPose.byId(YeetPose.PANCAKE.ordinal()));
	}
}
