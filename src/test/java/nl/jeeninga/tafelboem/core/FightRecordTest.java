package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class FightRecordTest {
	private static FightRecord r14() {
		return new FightRecord(0L, "Kid A", 3, "default", FightRecord.WON, 14, 2, 1, 0, 2, 180, 2, "1.4.0", 31, 0, 11, 0, Map.of(), "normal",
				40, 9, 6, 5, 3, 1, 5, 1, 17, 22);
	}

	@Test
	void theFightLineCountsThrowsDodgesCatchesAndThrowBacksPerKid() {
		assertEquals("Kid A: level 3 in arena default, won ★★ (14 answers, 2 wrong, 1 \"?\", 0 ESC, 180 s, 2 kids, difficulty normal,"
				+ " farthest yeet 31 blocks, farthest throw-back 17 blocks, farthest super yeet 22 blocks, thrown at 40 (9 still), dodged 11,"
				+ " caught 6, threw back 5 (3 hit, 1 juggled), Terug! 5 right 1 wrong)", r14().describe());
	}

	@Test
	void aLineFromBefore14HasNoR14CountsAndDescribesAsItDid() {
		FightRecord old = new FightRecord(0L, "Kid A", 1, "default", FightRecord.WON, 12, 0, 0, 0, 3, 100, 1, "1.3.0", 31, 300, 4, 2,
				Map.of("cactus", 1), "easy");

		assertEquals(0, old.thrownAt());
		assertEquals(0, old.caught());
		assertEquals(0, old.farthestBack());
		assertFalse(old.describe().contains("thrown at") || old.describe().contains("Terug!") || old.describe().contains("caught"), old.describe());
		assertTrue(old.describe().contains("dodged 4, picked up 2, trapped 1 (cactus 1)"), old.describe());
	}

	@Test
	void theOlderConstructorsStillWork() {
		FightRecord oldest = new FightRecord(0L, "Kid A", 1, "default", FightRecord.LEFT, 3, 0, 0, 0, 0, 40, 1, "1.0.1");

		assertEquals(0, oldest.farthestYeet());
		assertEquals("", oldest.difficulty());
		assertEquals(Map.of(), oldest.trapped());
	}
}
