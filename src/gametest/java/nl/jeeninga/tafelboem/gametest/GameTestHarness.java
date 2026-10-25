package nl.jeeninga.tafelboem.gametest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestBatchFactory;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What the test mod changes in how Minecraft runs the GameTests, so they take less time; the mixins
 * in {@code gametest.mixin} call in here. The GameTest server never saves its world (Gradle deletes
 * it before every run) and runs all its tests in one batch.
 */
public final class GameTestHarness {
	public static final Logger LOGGER = LoggerFactory.getLogger("tafelboem-test");

	private GameTestHarness() {
	}

	/** Like {@code /save-off}: no chunk is written while the tests run. */
	public static void neverSave(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			level.noSave = true;
		}
		LOGGER.info("TafelBoem GameTests: the test world is never saved");
	}

	/**
	 * Minecraft runs at most 50 tests at a time and the batches one after the other, so each batch
	 * waits for its slowest test. One batch per test environment starts every test at once.
	 */
	public static List<GameTestBatch> oneBatch(List<GameTestBatch> batches) {
		Map<Holder<TestEnvironmentDefinition<?>>, List<GameTestInfo>> byEnvironment = new LinkedHashMap<>();
		for (GameTestBatch batch : batches) {
			byEnvironment.computeIfAbsent(batch.environment(), environment -> new ArrayList<>()).addAll(batch.gameTestInfos());
		}
		List<GameTestBatch> merged = new ArrayList<>();
		byEnvironment.forEach((environment, tests) -> merged.add(GameTestBatchFactory.toGameTestBatch(tests, environment, merged.size())));
		return merged;
	}
}
