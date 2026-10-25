package nl.jeeninga.tafelboem.gametest;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What the test mod changes in how Minecraft runs the GameTests, so they take less time; the mixins
 * in {@code gametest.mixin} call in here. The GameTest server never saves its world: Gradle deletes
 * it before every run.
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
}
