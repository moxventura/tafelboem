package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ArenaPickerTest {
	private static final List<String> ARENAS = List.of("tafelboem:level_2/kasteel", "tafelboem:level_2/schip", "tafelboem:level_2/bos");

	@Test
	void neverTheSameArenaTwiceInARow() {
		ArenaPicker picker = new ArenaPicker(new Random(3));
		String previous = picker.pick(ARENAS);
		for (int i = 0; i < 200; i++) {
			String current = picker.pick(ARENAS);
			assertNotEquals(previous, current);
			previous = current;
		}
	}

	@Test
	void everyArenaComesUp() {
		ArenaPicker picker = new ArenaPicker(new Random(4));
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 30; i++) {
			seen.add(picker.pick(ARENAS));
		}
		assertEquals(Set.copyOf(ARENAS), seen);
	}

	@Test
	void aSingleArenaIsAlwaysPicked() {
		ArenaPicker picker = new ArenaPicker(new Random(5));
		for (int i = 0; i < 5; i++) {
			assertEquals("default", picker.pick(List.of("default")));
		}
	}
}
