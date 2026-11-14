package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class YeetPowerTest {
	/** A round floor of radius 22.5 round (0, 0), as a level 3 arena's. */
	private static final YeetPower.Floor ROUND = (x, z) -> Math.hypot(x, z) <= 22.5;

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
		double strongest = YeetPower.wanted(1.4, BombTier.KIPPENBOM.yeetPower(), YeetBoost.MAX);
		assertEquals(1.4 * 1.8 * 1.3, strongest, 1e-9);
		assertEquals(strongest, YeetPower.power(YeetKind.KICK, strongest, 0), 1e-9, "not even with no room at all");
	}

	@Test
	void theCapIsThePowerWhoseFlightFitsTheRoom() {
		// A flight goes at most power / 0.09 across: the cap times that is the room.
		assertEquals(20.5, YeetPower.capForRoom(20.5) * YeetPower.BLOCKS_PER_POWER, 1e-9);
		assertEquals(1.845, YeetPower.capForRoom(20.5), 1e-9, "from a level 3 arena's middle: about the old cap of 1.9");
		assertEquals(YeetPower.MIN_POWER, YeetPower.capForRoom(0), 1e-9, "no room still gives a small yeet");
	}

	@Test
	void aThrowBackOrSuperYeetGetsWhatItAsksWhenItFitsAndTheCapWhenItDoesnt() {
		double back = YeetPower.wanted(1.4, BombTier.KIPPENBOM.yeetPower(), YeetPower.factor(YeetKind.BACK, YeetBoost.MAX));
		assertEquals(back, YeetPower.power(YeetKind.BACK, back, 50), 1e-9, "with room for it, it fits");
		assertEquals(YeetPower.capForRoom(10), YeetPower.power(YeetKind.SUPER, back * 1.2, 10), 1e-9, "with 10 blocks of room it is capped");
	}

	@Test
	void withRoomForAPlainYeetOfTheBombItNeverFliesShorterThanOne() {
		for (BombTier bomb : BombTier.values()) {
			for (double rolled = 0.9; rolled <= 1.4; rolled += 0.1) {
				double plain = YeetPower.wanted(rolled, bomb.yeetPower(), 1.0);
				double room = plain * YeetPower.BLOCKS_PER_POWER;
				for (YeetKind kind : new YeetKind[] {YeetKind.BACK, YeetKind.SUPER}) {
					double power = YeetPower.power(kind, YeetPower.wanted(rolled, bomb.yeetPower(), YeetPower.factor(kind, 1.0)), room);
					assertTrue(power >= plain - 1e-9, bomb + " " + kind + " at " + rolled + ": " + power + " < " + plain);
				}
			}
		}
	}

	@Test
	void theRoomIsTheFloorAheadLessTheMargin() {
		assertEquals(22.5 - YeetPower.ROOM_MARGIN, YeetPower.room(0, 0, 0, 1, ROUND), 0.25, "from the middle to the edge");
		assertEquals(7.5 - YeetPower.ROOM_MARGIN, YeetPower.room(0, 15, 0, 1, ROUND), 0.25, "15 blocks out, outwards");
		assertEquals(37.5 - YeetPower.ROOM_MARGIN, YeetPower.room(0, 15, 0, -1, ROUND), 0.25, "15 blocks out, through the middle to the far edge");
		assertEquals(0, YeetPower.room(0, 30, 0, 1, ROUND), 1e-9, "beyond the edge, outwards: none");
		assertEquals(52.5 - YeetPower.ROOM_MARGIN, YeetPower.room(0, 30, 0, -1, ROUND), 0.25, "beyond the edge, inwards: the gap is crossed, the room counts from him");
		assertEquals(0, YeetPower.room(0, 0, 0, 0, ROUND), 1e-9, "no direction, no room");
	}

	@Test
	void heFliesAwayWhenItFitsAndThroughTheMiddleWhenAwayIsTooShort() {
		double superYeet = YeetPower.wanted(1.4, BombTier.KNALLETJE.yeetPower(), YeetPower.factor(YeetKind.SUPER, YeetBoost.MAX));
		YeetPower.Way fromNearTheEdge = YeetPower.way(superYeet, 5.5, 35.5);
		assertTrue(fromNearTheEdge.throughTheMiddle(), "15 blocks out, the kid behind him: across the middle");
		assertEquals(35.5, fromNearTheEdge.room(), 1e-9);
		assertFalse(YeetPower.way(superYeet, 31, 35.5).throughTheMiddle(), "away fits: away, as the kid expects");
		assertFalse(YeetPower.way(superYeet, 10, 8).throughTheMiddle(), "away doesn't fit, but has the most room: away");
	}
}
