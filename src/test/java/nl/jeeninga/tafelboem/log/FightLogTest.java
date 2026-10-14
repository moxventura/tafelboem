package nl.jeeninga.tafelboem.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nl.jeeninga.tafelboem.core.FightRecord;

class FightLogTest {
	@TempDir
	Path dir;

	private static FightRecord fight(String player, int level, String result, int stars) {
		return new FightRecord(1_000L, player, level, "default", result, 12, 1, 0, 0, stars, 150, 2, "1.0.0");
	}

	@Test
	void eachKidHasTheirOwnFileInOrder() {
		FightLog log = new FightLog(dir);
		log.append(fight("Sanne", 1, FightRecord.WON, 2));
		log.append(fight("Daan", 3, FightRecord.LEFT, 0));
		log.append(fight("Sanne", 2, FightRecord.WON, 3));

		assertEquals(List.of(fight("Sanne", 1, FightRecord.WON, 2), fight("Sanne", 2, FightRecord.WON, 3)), log.read("Sanne"));
		assertEquals(1, log.read("Daan").size());
		assertTrue(Files.exists(dir.resolve("fights-Sanne.jsonl")));
	}

	@Test
	void aBrokenLineIsSkippedAndAMissingFileIsEmpty() throws IOException {
		FightLog log = new FightLog(dir);
		assertEquals(List.of(), log.read("Nobody"));
		log.append(fight("Sanne", 1, FightRecord.WON, 1));
		Files.writeString(dir.resolve("fights-Sanne.jsonl"), "{ not json\n", StandardOpenOption.APPEND);
		assertEquals(1, log.read("Sanne").size());
	}

	@Test
	void aNullLineIsSkipped() throws IOException {
		FightLog log = new FightLog(dir);
		log.append(fight("Sanne", 1, FightRecord.WON, 1));
		Files.writeString(dir.resolve("fights-Sanne.jsonl"), "null\n", StandardOpenOption.APPEND);
		assertEquals(List.of(fight("Sanne", 1, FightRecord.WON, 1)), log.read("Sanne"));
	}
}
