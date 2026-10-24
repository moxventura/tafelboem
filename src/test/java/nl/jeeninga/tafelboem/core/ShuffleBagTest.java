package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ShuffleBagTest {
	private static final List<Integer> ITEMS = List.of(1, 2, 3, 4, 5);

	@Test
	void everyItemOnceBeforeAnyComesAgain() {
		ShuffleBag<Integer> bag = new ShuffleBag<>(ITEMS, new Random(3));
		for (int round = 0; round < 4; round++) {
			Set<Integer> seen = new HashSet<>();
			for (int i = 0; i < ITEMS.size(); i++) {
				seen.add(bag.next(item -> true));
			}
			assertEquals(Set.copyOf(ITEMS), seen, "round " + round);
		}
	}

	@Test
	void anItemThatIsNotAllowedNeverComes() {
		ShuffleBag<Integer> bag = new ShuffleBag<>(ITEMS, new Random(5));
		for (int i = 0; i < 50; i++) {
			assertNotEquals(1, bag.next(item -> item != 1));
		}
	}

	@Test
	void nothingAllowedIsAMistake() {
		ShuffleBag<Integer> bag = new ShuffleBag<>(ITEMS, new Random(7));
		assertThrows(IllegalArgumentException.class, () -> bag.next(item -> false));
	}
}
