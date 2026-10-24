package nl.jeeninga.tafelboem.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TafelBoemConfigTest {
	@TempDir
	Path dir;

	@Test
	void aMissingFileGivesDefaultsAndIsWrittenForTheParent() throws IOException {
		Path file = dir.resolve("tafelboem.json");

		TafelBoemConfig config = TafelBoemConfig.load(file);

		assertFalse(config.correctionRetype);
		assertTrue(config.cameraAssist);
		assertTrue(config.stars);
		assertTrue(config.arenaCraters);
		assertTrue(config.wrongAnswerDamage);
		assertTrue(config.foodReward);
		assertTrue(config.kippenbomChickens);
		assertTrue(config.countingKippenbom);
		assertTrue(config.yeetBoost);
		assertTrue(config.yeetRecords);
		assertTrue(config.yeetCatalogue);
		assertTrue(config.arenaMusic);
		assertTrue(Files.readString(file).contains("correctionRetype"), "the parent can find the toggles in the file");
	}

	@Test
	void keysMissingFromTheFileKeepTheirDefaults() throws IOException {
		Path file = dir.resolve("tafelboem.json");
		Files.writeString(file, "{\"correctionRetype\": true}");

		TafelBoemConfig config = TafelBoemConfig.load(file);

		assertTrue(config.correctionRetype);
		assertTrue(config.cameraAssist);
	}

	@Test
	void anOlderFileGainsTheNewKeysAndKeepsItsOwnValues() throws IOException {
		Path file = dir.resolve("tafelboem.json");
		Files.writeString(file, "{\"correctionRetype\": true}");

		TafelBoemConfig.load(file);

		String written = Files.readString(file);
		assertTrue(written.contains("\"stars\"") && written.contains("\"arenaCraters\""), "the parent can find the new toggles: " + written);
		assertTrue(TafelBoemConfig.load(file).correctionRetype, "the parent's own value stays");
	}

	@Test
	void aBrokenFileFallsBackToDefaults() throws IOException {
		Path file = dir.resolve("tafelboem.json");
		Files.writeString(file, "{ this is not json");

		TafelBoemConfig config = TafelBoemConfig.load(file);

		assertFalse(config.correctionRetype);
		assertTrue(config.cameraAssist);
		assertEquals("{ this is not json", Files.readString(file), "the parent's file stays as it was, to fix by hand");
	}

	@Test
	void theGameLogGetsEverySwitchOnOneLine() {
		String line = new TafelBoemConfig().describe();

		assertFalse(line.contains("\n"), "one line: " + line);
		assertTrue(line.contains("\"correctionRetype\":false") && line.contains("\"arenaMusic\":true"), line);
	}
}
