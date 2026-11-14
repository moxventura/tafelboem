package nl.jeeninga.tafelboem.core;

/**
 * How many Terugbommen a kid carries and which slot a caught one goes in (R1.4). Slots are an
 * inventory's: 0–8 the hotbar, 9–35 the rest.
 */
public final class Ammo {
	/** A kid carries at most this many in total, over all slots. */
	public static final int MAX = 3;
	/** Slots 0–8 are the hotbar. */
	public static final int HOTBAR = 9;

	private Ammo() {
	}

	/** Whether a kid who carries this many can catch another. */
	public static boolean canCatch(int carried) {
		return carried < MAX;
	}

	public static boolean isHotbar(int slot) {
		return slot >= 0 && slot < HOTBAR;
	}

	/**
	 * Where a caught bomb goes: the selected hotbar slot if it is empty, else the first empty hotbar slot, else
	 * (a full hotbar) the first empty slot of the rest. A hotbar slot is then selected, so the kid can throw at once.
	 *
	 * @param empty which of the 36 slots are empty
	 * @return the slot, or -1 when every slot is taken
	 */
	public static int slotFor(int selected, boolean[] empty) {
		if (isHotbar(selected) && empty[selected]) {
			return selected;
		}
		for (int slot = 0; slot < empty.length; slot++) {
			if (empty[slot]) {
				return slot;
			}
		}
		return -1;
	}
}
