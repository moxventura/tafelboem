package nl.jeeninga.tafelboem.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
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
	void aListenerHearsEveryFightThatIsWritten() {
		FightLog log = new FightLog(dir);
		List<FightRecord> heard = new ArrayList<>();
		log.listen(heard::add);
		log.append(fight("Sanne", 1, FightRecord.WON, 2));
		log.append(fight("Daan", 3, FightRecord.LEFT, 0));
		assertEquals(List.of(fight("Sanne", 1, FightRecord.WON, 2), fight("Daan", 3, FightRecord.LEFT, 0)), heard);
	}

	@Test
	void aNullLineIsSkipped() throws IOException {
		FightLog log = new FightLog(dir);
		log.append(fight("Sanne", 1, FightRecord.WON, 1));
		Files.writeString(dir.resolve("fights-Sanne.jsonl"), "null\n", StandardOpenOption.APPEND);
		assertEquals(List.of(fight("Sanne", 1, FightRecord.WON, 1)), log.read("Sanne"));
	}

	@Test
	void aLineFromBefore11HasNoFarthestYeet() throws IOException {
		Files.writeString(dir.resolve("fights-Sanne.jsonl"), "{\"timestamp\":0,\"player\":\"Sanne\",\"level\":2,\"arena\":\"default\","
				+ "\"result\":\"won\",\"answers\":12,\"wrong\":1,\"dontKnow\":0,\"chickenOut\":0,\"stars\":2,\"seconds\":120,\"kids\":1,"
				+ "\"version\":\"1.0.2\"}\n");

		FightRecord old = new FightLog(dir).read("Sanne").getFirst();
		assertEquals(0, old.farthestYeet());
		assertEquals(2, old.stars());
	}

	@Test
	void theFarthestYeetIsWrittenAndReadBack() {
		FightLog log = new FightLog(dir);
		log.append(new FightRecord(0L, "Sanne", 1, "default", FightRecord.WON, 12, 0, 0, 0, 3, 100, 1, "1.1.0", 31));

		assertEquals(31, log.read("Sanne").getFirst().farthestYeet());
	}
}
