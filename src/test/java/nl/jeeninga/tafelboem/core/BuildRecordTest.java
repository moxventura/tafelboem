package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class BuildRecordTest {
	@Test
	void sayWhoRangAndWhatEachKidPlacedMostFirst() {
		Map<String, Map<String, Integer>> placed = new LinkedHashMap<>();
		placed.put("Sanne", Map.of("glass", 14, "oak_planks", 20));
		placed.put("Daan", Map.of());
		assertEquals("level 1, built 87 s, the bell (Sanne), 2 kids; placed: Sanne 34 (oak_planks 20, glass 14), Daan nothing",
				new BuildRecord(1, 87, BuildRecord.BELL, "Sanne", 2, placed).describe());
	}

	@Test
	void theTimeRanOut() {
		assertEquals("level 3, built 180 s, time ran out, 1 kid; placed: Daan 3 (lever 1, red_wool 1, slime_block 1)",
				new BuildRecord(3, 180, BuildRecord.TIME, null, 1, Map.of("Daan", Map.of("slime_block", 1, "red_wool", 1, "lever", 1))).describe());
	}

	@Test
	void everyoneLeftBeforeHeCame() {
		assertEquals("level 2, built 41 s, everyone left, 1 kid; placed: Daan nothing",
				new BuildRecord(2, 41, BuildRecord.EVERYONE_LEFT, null, 1, Map.of("Daan", Map.of())).describe());
	}
}
