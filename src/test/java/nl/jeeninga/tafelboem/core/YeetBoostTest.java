package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class YeetBoostTest {
	/** The table in the spec. */
	@ParameterizedTest
	@CsvSource({"false, 1, 1.00", "true, 1, 1.15", "false, 4, 1.15", "true, 4, 1.30", "true, 9, 1.30", "false, 0, 1.00"})
	void fastAnswersAndStreaksAddUp(boolean fast, int streak, double factor) {
		assertEquals(factor, YeetBoost.factor(fast, streak), 1e-9);
	}

	@Test
	void aBoostOnlyEverAddsAndNeverPassesTheCap() {
		for (int streak = 0; streak < 50; streak++) {
			assertTrue(YeetBoost.factor(true, streak) <= YeetBoost.MAX, "streak " + streak);
			assertTrue(YeetBoost.factor(false, streak) >= 1.0, "streak " + streak);
		}
	}
}
