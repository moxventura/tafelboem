package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

class YeetReportTest {
	@Test
	void aBoostedKickThatBeatTheRecordOnOneLine() {
		YeetReport report = new YeetReport("Mark", BombTier.KIPPENBOM, new Fact(3, 4), 2300, OptionalLong.of(3100), true, 4, 1.3,
				YeetStyle.CLASSIC, "pancake", false, 31, false, false, true, 25);

		assertEquals("Mark kicked a kippenbom on 3x4 in 2300 ms (usual 3100 ms, fast, 4 in a row, x1.30): classic + pancake,"
				+ " 31 blocks, a record (was 25)", report.describe());
	}

	@Test
	void aHelicopterOutOfTheArenaWithoutHistory() {
		YeetReport report = new YeetReport("Daan", BombTier.KNALLETJE, new Fact(2, 5), 5400, OptionalLong.empty(), false, 1, 1.0,
				YeetStyle.HELICOPTER, "toe_hop", true, 9, true, true, false, 0);

		assertEquals("Daan kicked a knalletje on 2x5 in 5400 ms (usual none, 1 in a row, x1.00): helicopter + toe_hop (signature),"
				+ " 9 blocks high, out of the arena", report.describe());
	}
}
