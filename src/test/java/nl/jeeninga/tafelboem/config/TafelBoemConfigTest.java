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
		assertTrue(config.cameraNudge);
		assertTrue(config.stars);
		assertTrue(config.arenaCraters);
		assertTrue(config.wrongAnswerDamage);
		assertTrue(config.foodReward);
		assertTrue(config.kippenbomChickens);
		assertTrue(config.countingKippenbom);
		assertTrue(config.yeetBoost);
		assertTrue(config.yeetRecords);
		assertTrue(config.yeetCatalogue);
		assertTrue(config.bombSignatures);
		assertTrue(config.arenaMusic);
		assertFalse(config.buildPhase, "R1.4: the playtest turned building off");
		assertTrue(config.dodging);
		assertFalse(config.traps, "R1.4: nothing to build them from");
		assertTrue(config.arenaSafety);
		assertTrue(config.noBuildInFight);
		assertTrue(config.landingRing);
		assertEquals(0.70, config.dodgeLead1);
		assertEquals(0.85, config.dodgeLead2);
		assertEquals(0.95, config.dodgeLead3);
		assertEquals(0.85, config.dodgeFlight1);
		assertEquals(0.75, config.dodgeFlight2);
		assertEquals(0.68, config.dodgeFlight3);
		assertEquals(12, config.dodgeMinFlightTicks);
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

	@Test
	void anOlderFileKeepsItsOwnBuildPhaseAndGainsTheNewSwitches() throws IOException {
		Path file = dir.resolve("tafelboem.json");
		Files.writeString(file, "{\"buildPhase\": true, \"traps\": true}");

		TafelBoemConfig config = TafelBoemConfig.load(file);

		assertTrue(config.buildPhase, "the parent's own value stays, whatever the new default");
		assertTrue(config.traps);
		assertTrue(config.noBuildInFight);
		String written = Files.readString(file);
		assertTrue(written.contains("\"noBuildInFight\"") && written.contains("\"dodgeLead3\""), "the parent can find the new switches: " + written);
	}

	@Test
	void theDodgeNumbersDefaultToTheSpecsAndTheParentCanTuneThem() throws IOException {
		TafelBoemConfig config = new TafelBoemConfig();
		for (nl.jeeninga.tafelboem.core.FightLevel level : nl.jeeninga.tafelboem.core.FightLevel.DOORS) {
			assertEquals(nl.jeeninga.tafelboem.core.Aim.Tuning.defaults(level.number()), config.aimFor(level), "level " + level.number());
		}
		assertEquals(nl.jeeninga.tafelboem.core.Aim.Tuning.defaults(1), config.aimFor(nl.jeeninga.tafelboem.core.FightLevel.CLASSIC));

		Path file = dir.resolve("tafelboem.json");
		Files.writeString(file, "{\"dodgeLead3\": 0.5, \"dodgeFlight3\": 0.9, \"dodgeMinFlightTicks\": 20}");
		TafelBoemConfig tuned = TafelBoemConfig.load(file);
		assertEquals(new nl.jeeninga.tafelboem.core.Aim.Tuning(0.5, 0.9, 20), tuned.aimFor(nl.jeeninga.tafelboem.core.FightLevel.MOEILIJK));
		assertEquals(nl.jeeninga.tafelboem.core.Aim.Tuning.defaults(1).lead(), tuned.aimFor(nl.jeeninga.tafelboem.core.FightLevel.MAKKELIJK).lead());
	}

	@Test
	void absurdDodgeNumbersAreKeptSane() {
		TafelBoemConfig config = new TafelBoemConfig();
		config.dodgeLead1 = 7;
		config.dodgeFlight1 = 0;
		config.dodgeMinFlightTicks = -4;

		nl.jeeninga.tafelboem.core.Aim.Tuning tuning = config.aimFor(nl.jeeninga.tafelboem.core.FightLevel.MAKKELIJK);

		assertEquals(1.0, tuning.lead());
		assertEquals(0.3, tuning.flightFactor());
		assertEquals(1, tuning.minTicks());
	}
}
