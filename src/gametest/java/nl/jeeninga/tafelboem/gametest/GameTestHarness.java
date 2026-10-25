package nl.jeeninga.tafelboem.gametest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestBatchFactory;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What the test mod changes in how Minecraft runs the GameTests, so they take less time; the mixins
 * in {@code gametest.mixin} call in here. The GameTest server never saves its world (Gradle deletes
 * it before every run), runs all its tests in one batch, and runs one group when Gradle names it
 * ({@value #GROUP}): the fights ({@link Fight}) or the quick tests, so the two can run on two
 * servers side by side.
 */
public final class GameTestHarness {
	public static final Logger LOGGER = LoggerFactory.getLogger("tafelboem-test");
	/** Gradle's choice of group, {@value #QUICK} or {@value #FIGHTS}; without it every test runs. */
	public static final String GROUP = "tafelboem.gametest.group";
	/** How many times each selected test runs: Minecraft's own repeat, which Fabric leaves at 0. */
	public static final String REPEAT = "tafelboem.gametest.repeat";
	static final String QUICK = "quick";
	static final String FIGHTS = "fights";
	private static final String TEST_MOD = "tafelboem-test";

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

	/**
	 * Keeps only the chosen group's tests: the ones marked {@link Fight}, or all the others. Fails the
	 * run when a mark is on something that is no GameTest, or when no test of the group is selected.
	 */
	public static List<GameTestBatch> keepChosenGroup(List<GameTestBatch> batches, ServerLevel level) {
		String group = System.getProperty(GROUP);
		if (group == null) {
			return batches;
		}
		if (!group.equals(QUICK) && !group.equals(FIGHTS)) {
			throw new IllegalStateException(GROUP + " is \"" + group + "\", not \"" + QUICK + "\" or \"" + FIGHTS + "\"");
		}
		Set<Identifier> fights = fights();
		Set<Identifier> tests = level.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE).keySet();
		List<Identifier> noTest = fights.stream().filter(id -> !tests.contains(id)).sorted().toList();
		if (!noTest.isEmpty()) {
			throw new IllegalStateException("@Fight on a method that is no GameTest: " + noTest);
		}
		boolean wantFights = group.equals(FIGHTS);
		List<GameTestBatch> kept = new ArrayList<>();
		int count = 0;
		for (GameTestBatch batch : batches) {
			List<GameTestInfo> inGroup = batch.gameTestInfos().stream().filter(test -> fights.contains(test.id()) == wantFights).toList();
			if (!inGroup.isEmpty()) {
				kept.add(GameTestBatchFactory.toGameTestBatch(inGroup, batch.environment(), kept.size()));
				count += inGroup.size();
			}
		}
		if (kept.isEmpty()) {
			throw new IllegalStateException("No GameTest in the " + group + " group matches "
					+ System.getProperty("fabric-api.gametest.filter", "*:*"));
		}
		LOGGER.info("TafelBoem GameTests: the {} group, {} tests", group, count);
		return kept;
	}

	/** Minecraft's repeat for the selected tests, which Fabric always sets to 0. */
	public static int repeat(int fabricsRepeat) {
		return Integer.getInteger(REPEAT, fabricsRepeat);
	}

	/** The ids Fabric gives the methods marked {@link Fight}: the class and method name in snake case. */
	private static Set<Identifier> fights() {
		Set<Identifier> fights = new HashSet<>();
		for (EntrypointContainer<Object> entrypoint : FabricLoader.getInstance().getEntrypointContainers("fabric-gametest", Object.class)) {
			if (!entrypoint.getProvider().getMetadata().getId().equals(TEST_MOD)) {
				continue;
			}
			Class<?> tests = entrypoint.getEntrypoint().getClass();
			for (Method method : tests.getDeclaredMethods()) {
				if (method.isAnnotationPresent(Fight.class)) {
					String name = (tests.getSimpleName() + "_" + method.getName()).replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
					fights.add(Identifier.fromNamespaceAndPath(TEST_MOD, name));
				}
			}
		}
		return fights;
	}
}
