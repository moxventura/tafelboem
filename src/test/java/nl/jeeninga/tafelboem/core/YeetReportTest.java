package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

class YeetReportTest {
	@Test
	void aBoostedKickThatBeatTheRecordOnOneLine() {
		YeetReport report = new YeetReport("Kid A", BombTier.KIPPENBOM, new Fact(3, 4), 2300, OptionalLong.of(3100), true, 4, 1.3,
				YeetStyle.CLASSIC, "pancake", false, 31, false, false, true, 25, null);

		assertEquals("Kid A kicked a kippenbom on 3x4 in 2300 ms (usual 3100 ms, fast, 4 in a row, x1.30): classic + pancake,"
				+ " 31 blocks, a record (was 25)", report.describe());
	}

	@Test
	void aHelicopterOutOfTheArenaWithoutHistory() {
		YeetReport report = new YeetReport("Daan", BombTier.KNALLETJE, new Fact(2, 5), 5400, OptionalLong.empty(), false, 1, 1.0,
				YeetStyle.HELICOPTER, "toe_hop", true, 9, true, true, false, 0, null);

		assertEquals("Daan kicked a knalletje on 2x5 in 5400 ms (usual none, 1 in a row, x1.00): helicopter + toe_hop (signature),"
				+ " 9 blocks high, out of the arena", report.describe());
	}

	@Test
	void aKickThatEndedInATrapSaysWhichTrap() {
		YeetReport report = new YeetReport("Kid B", BombTier.KNALLETJE, new Fact(2, 5), 3000, OptionalLong.empty(), false, 1, 1.0,
				YeetStyle.CLASSIC, "prickled", false, 12, false, false, false, 0, "cactus");

		assertEquals("Kid B kicked a knalletje on 2x5 in 3000 ms (usual none, 1 in a row, x1.00): classic + prickled,"
				+ " 12 blocks, trap=cactus", report.describe());
	}

	@Test
	void aTrapLandingIsNeverLoggedAsASignature() {
		YeetReport report = new YeetReport("Kid B", BombTier.KNALLETJE, new Fact(2, 5), 3000, OptionalLong.empty(), false, 1, 1.0,
				YeetStyle.CLASSIC, "soot", true, 12, false, false, false, 0, "lying_bomb");

		assertEquals("Kid B kicked a knalletje on 2x5 in 3000 ms (usual none, 1 in a row, x1.00): classic + soot,"
				+ " 12 blocks, trap=lying_bomb", report.describe());
	}

	@Test
	void aThrowBackAndAJuggleAreMarkedOnTheLine() {
		YeetReport back = new YeetReport("Kid A", BombTier.GEWONE_TNT, new Fact(6, 7), 4100, OptionalLong.empty(), false, 1, 1.25,
				YeetStyle.TUMBLE, "dizzy", false, 17, false, false, false, 0, null, YeetKind.BACK);
		YeetReport superYeet = new YeetReport("Kid A", BombTier.GEWONE_TNT, new Fact(6, 7), 4100, OptionalLong.empty(), false, 1, 1.5,
				YeetStyle.CLASSIC, "dizzy", false, 22, false, false, true, 18, null, YeetKind.SUPER);

		assertEquals("Kid A kicked a gewone_tnt on 6x7 in 4100 ms (usual none, 1 in a row, x1.25): tumble + dizzy, 17 blocks, back", back.describe());
		assertEquals("Kid A kicked a gewone_tnt on 6x7 in 4100 ms (usual none, 1 in a row, x1.50): classic + dizzy, 22 blocks, super,"
				+ " a record (was 18)", superYeet.describe());
	}

	@Test
	void aKickWithoutAKindIsAKickAndHasNoMark() {
		YeetReport report = new YeetReport("Kid A", BombTier.KNALLETJE, new Fact(2, 5), 3000, OptionalLong.empty(), false, 1, 1.0,
				YeetStyle.CLASSIC, "dizzy", false, 12, false, false, false, 0, null);

		assertEquals(YeetKind.KICK, report.kind());
		assertEquals("Kid A kicked a knalletje on 2x5 in 3000 ms (usual none, 1 in a row, x1.00): classic + dizzy, 12 blocks", report.describe());
	}
}
