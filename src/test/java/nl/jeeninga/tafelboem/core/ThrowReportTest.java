package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ThrowReportTest {
	@Test
	void aDodgedThrowAtARunningKid() {
		ThrowReport report = new ThrowReport("Kid A", 3, 12.46, 18, 4.3, false, false);

		assertEquals("Kid A: level 3, 12.5 blocks, 18 ticks, kid at 4.3 blocks/s: dodged", report.describe());
	}

	@Test
	void aHitOnAKidWhoStoodStill() {
		ThrowReport report = new ThrowReport("Kid B", 1, 8.0, 22, 0.0, true, true);

		assertEquals("Kid B: level 1, 8.0 blocks, 22 ticks, kid at 0.0 blocks/s, stood still: hit", report.describe());
	}
}
