package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StatsFormatTest {
	@Test
	void dutchGetsADecimalComma() {
		assertEquals("4,0", StatsFormat.seconds(4000, "nl_nl"));
	}

	@Test
	void englishGetsADecimalPoint() {
		assertEquals("4.0", StatsFormat.seconds(4000, "en_us"));
	}

	@Test
	void theServerConsoleGetsAPointToo() {
		assertEquals("6.5", StatsFormat.seconds(6500, null));
	}
}
