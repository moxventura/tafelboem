package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class YeetPickerTest {
	@ParameterizedTest
	@ValueSource(longs = {1, 2, 3, 42})
	void neverTheSameYeetTwiceInARow(long seed) {
		YeetPicker picker = new YeetPicker(new Random(seed));
		YeetStyle previous = picker.next();
		for (int i = 0; i < 500; i++) {
			YeetStyle current = picker.next();
			assertNotEquals(previous, current, "repeat at yeet " + i);
			previous = current;
		}
	}

	@Test
	void everyYeetShowsUpWithinOneFight() {
		// A level-1 fight has 11 hits; the kids should see every kind of yeet in it.
		YeetPicker picker = new YeetPicker(new Random(7));
		Set<YeetStyle> seen = EnumSet.noneOf(YeetStyle.class);
		for (int i = 0; i < YeetStyle.values().length; i++) {
			seen.add(picker.next());
		}
		assertEquals(EnumSet.allOf(YeetStyle.class), seen);
	}
}
