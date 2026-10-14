package nl.jeeninga.tafelboem.config;

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
	void aBrokenFileFallsBackToDefaults() throws IOException {
		Path file = dir.resolve("tafelboem.json");
		Files.writeString(file, "{ this is not json");

		TafelBoemConfig config = TafelBoemConfig.load(file);

		assertFalse(config.correctionRetype);
		assertTrue(config.cameraAssist);
	}
}
