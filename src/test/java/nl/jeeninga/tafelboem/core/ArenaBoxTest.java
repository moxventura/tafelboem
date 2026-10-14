package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArenaBoxTest {
	private final ArenaBox box = ArenaBox.of(10, 60, -5, 47, 7, 47);

	@Test
	void bothCornersAreInside() {
		assertTrue(box.contains(10, 60, -5));
		assertTrue(box.contains(56, 66, 41));
		assertFalse(box.contains(57, 66, 41));
		assertFalse(box.contains(10, 67, -5));
	}

	@Test
	void theSkyOverTheArenaBelongsToIt() {
		assertTrue(box.containsColumn(30.5, 10.5));
		assertTrue(box.holds(30.5, 200, 10.5));
	}

	@Test
	void besideItOrThroughItsBottomIsOut() {
		assertFalse(box.holds(9.9, 64, 10));
		assertFalse(box.holds(30, 57.9, 10));
		assertTrue(box.holds(30, 58, 10), "a jump or a bump just below the floor still counts");
	}

	@Test
	void theMiddleOfTheFootprint() {
		assertEquals(33.5, box.middleX());
		assertEquals(18.5, box.middleZ());
	}
}
