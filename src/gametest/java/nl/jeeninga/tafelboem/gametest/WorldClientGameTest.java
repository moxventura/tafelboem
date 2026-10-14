package nl.jeeninga.tafelboem.gametest;

import java.util.List;
import java.util.function.BooleanSupplier;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.client.QuestionScreen;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.world.TafelBoemWorld;
import nl.jeeninga.tafelboem.world.TafelBoemWorlds;

/**
 * R1 in a real client: a TafelBoem world from the World Type button, the hub, a walk through each
 * door into its arena, a fight on level 2, and the exit door home. Screenshots land in
 * {@code build/run/clientGameTest/screenshots}.
 */
@SuppressWarnings("UnstableApiUsage")
public class WorldClientGameTest implements FabricClientGameTest {
	private static final ResourceKey<WorldPreset> PRESET = ResourceKey.create(Registries.WORLD_PRESET, TafelBoem.id("tafelboem"));

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(WorldClientGameTest::chooseTafelBoem).create()) {
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);
			check(world.getServer().computeOnServer(server -> TafelBoemWorlds.isTafelBoemWorld(server.overworld())), "a TafelBoem world");
			check(world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				return tafelBoem().hub().bounds().holds(player.getX(), player.getY(), player.getZ());
			}), "the kid spawns in the hub");
			context.getInput().lookAt(door(world, 2).above(2));
			context.waitTicks(5);
			context.takeScreenshot("r1-01-hub");

			for (FightLevel level : FightLevel.DOORS) {
				walkInto(context, world, door(world, level.number()));
				waitUntil(context, () -> world.getServer().computeOnServer(server -> tafelBoem().run(level).isPresent()), "into arena " + level);
				world.getConnection().waitForChunksRender();
				// He appears 3 s after the kid arrives, and throws his first bomb 3 s after that.
				waitUntil(context, () -> world.getServer().computeOnServer(server -> tafelBoem().run(level).orElseThrow().fight() != null),
						"Graaf Fout in arena " + level);
				context.waitTicks(10);
				context.takeScreenshot("r1-0" + (level.number() + 1) + "-arena-" + level.number());

				if (level == FightLevel.GEMIDDELD) {
					context.waitForScreen(QuestionScreen.class);
					answerQuestion(context);
					context.waitTicks(24);
					context.takeScreenshot("r1-05-yeet-in-arena");
				}
				walkOut(context, world, level);
			}
		}
	}

	/** Walks through the arena's exit door. His bomb may have rooted the kid by now: answer, then walk on. */
	private static void walkOut(ClientGameTestContext context, TestSingleplayerContext world, FightLevel level) {
		BlockPos exit = world.getServer().computeOnServer(server -> tafelBoem().run(level).orElseThrow().arena().doors().get(0).getFirst());
		for (int attempt = 0; world.getServer().computeOnServer(server -> tafelBoem().run(level).isPresent()); attempt++) {
			check(attempt < 5, "still in arena " + level + " after walking into its exit door");
			if (context.computeOnClient(client -> client.gui.screen() instanceof QuestionScreen)) {
				answerQuestion(context);
			}
			walkInto(context, world, exit);
			context.waitTicks(5);
		}
	}

	private static void answerQuestion(ClientGameTestContext context) {
		QuestionScreen screen = (QuestionScreen) context.computeOnClient(client -> client.gui.screen());
		context.getInput().typeChars(Integer.toString(screen.a() * screen.b()));
		context.waitTicks(5);
		context.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
		context.waitFor(client -> client.gui.screen() == null);
	}

	/** Stands the kid two blocks in front of the door, facing it, and walks forward. */
	private static void walkInto(ClientGameTestContext context, TestSingleplayerContext world, BlockPos door) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			BlockPos front = door.relative(server.overworld().getBlockState(door).getValue(LevelDoorBlock.FACING), 2);
			player.teleportTo(front.getX() + 0.5, door.getY(), front.getZ() + 0.5);
		});
		context.waitTicks(5);
		context.getInput().lookAt(door);
		context.getInput().holdKeyFor(options -> options.keyUp, 30);
	}

	private static BlockPos door(TestSingleplayerContext world, int level) {
		return world.getServer().computeOnServer(server -> {
			List<BlockPos> blocks = tafelBoem().hub().doors().get(level);
			return blocks.stream().min((a, b) -> Integer.compare(a.getY(), b.getY())).orElseThrow();
		});
	}

	private static TafelBoemWorld tafelBoem() {
		return TafelBoem.world().orElseThrow(() -> new AssertionError("no TafelBoem world running"));
	}

	/** Polls from the test thread, as the playthrough test does: asking the server from a client wait can deadlock. */
	private static void waitUntil(ClientGameTestContext context, BooleanSupplier condition, String what) {
		for (int tick = 0; !condition.getAsBoolean(); tick++) {
			check(tick < 200, "timed out waiting: " + what);
			context.waitTicks(1);
		}
	}

	private static void chooseTafelBoem(WorldCreationUiState state) {
		WorldCreationUiState.WorldTypeEntry tafelboem = state.getNormalPresetList().stream()
				.filter(type -> type.preset().is(PRESET))
				.findFirst()
				.orElseThrow(() -> new AssertionError("TafelBoem is not on the World Type button"));
		state.setWorldType(tafelboem);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
