package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class AmmoTest {
	private static boolean[] allEmpty() {
		boolean[] empty = new boolean[36];
		Arrays.fill(empty, true);
		return empty;
	}

	@Test
	void aKidCarriesAtMostThreeInTotal() {
		assertTrue(Ammo.canCatch(0));
		assertTrue(Ammo.canCatch(2));
		assertFalse(Ammo.canCatch(3), "with 3, they can't pick up more");
		assertFalse(Ammo.canCatch(4));
	}

	@Test
	void aCaughtBombGoesInTheSelectedSlotWhenThatIsEmpty() {
		boolean[] empty = allEmpty();
		assertEquals(4, Ammo.slotFor(4, empty));
	}

	@Test
	void otherwiseInTheFirstEmptyHotbarSlot() {
		boolean[] empty = allEmpty();
		empty[4] = false;
		empty[0] = false;
		assertEquals(1, Ammo.slotFor(4, empty));
	}

	@Test
	void aFullHotbarFallsBackToTheFirstEmptySlotOfTheRest() {
		boolean[] empty = allEmpty();
		for (int slot = 0; slot < Ammo.HOTBAR; slot++) {
			empty[slot] = false;
		}
		empty[9] = false;
		assertEquals(10, Ammo.slotFor(3, empty));
		assertFalse(Ammo.isHotbar(10), "which is not selected");
		assertTrue(Ammo.isHotbar(8));
	}

	@Test
	void aFullInventoryHasNoSlot() {
		boolean[] empty = new boolean[36];
		assertEquals(-1, Ammo.slotFor(0, empty));
	}
}
