package nl.jeeninga.tafelboem.gametest;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.client.QuestionScreen;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.game.BombEffects;
import nl.jeeninga.tafelboem.world.TafelBoemWorld;
import nl.jeeninga.tafelboem.world.TafelBoemWorlds;

/**
 * R1 in a real client: a TafelBoem world from the World Type button, the hub, a walk through each
 * door into its arena, a fight on level 2, the exit door home, and the same world loaded again.
 * It checks the rows of the load contract in AGENTS.md that only a real reload can show.
 * Screenshots land in {@code build/run/clientGameTest/screenshots}.
 */
@SuppressWarnings("UnstableApiUsage")
public class WorldClientGameTest implements FabricClientGameTest {
	private static final ResourceKey<WorldPreset> PRESET = ResourceKey.create(Registries.WORLD_PRESET, TafelBoem.id("tafelboem"));

	@Override
	public void runTest(ClientGameTestContext context) {
		TestWorldSave save;
		BlockPos[] beside = new BlockPos[1];
		BlockPos[] broken = new BlockPos[1];
		BlockPos[] leftOver = new BlockPos[1];
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

				if (level == FightLevel.MAKKELIJK) {
					leftOver[0] = leaveItem(world, level);
				}
				if (level == FightLevel.GEMIDDELD) {
					context.waitForScreen(QuestionScreen.class);
					answerQuestion(context);
					context.waitTicks(24);
					context.takeScreenshot("r1-05-yeet-in-arena");
				}
				walkOut(context, world, level);
			}

			// The arenas are 512+ blocks away, so the hub's chunks were saved and loaded again meanwhile.
			waitUntil(context, () -> world.getServer().computeOnServer(server -> hubFrames(server).size() == 5), "the hub's 5 bomb frames back");
			check(world.getServer().computeOnServer(server -> hubFrames(server).stream().allMatch(ItemFrame::isLoadedFromDisk)),
					"the hub's frames were loaded from disk again (or this check proves nothing)");

			// P19: the arena's chunks were saved meanwhile, and the item comes back from disk when a kid walks in again.
			waitUntil(context, () -> world.getServer().computeOnServer(server -> !server.overworld().areEntitiesLoaded(ChunkPos.pack(leftOver[0]))),
					"arena 1 unloaded (or the next check proves nothing)");
			walkInto(context, world, door(world, 1));
			waitUntil(context, () -> world.getServer().computeOnServer(server -> tafelBoem().run(FightLevel.MAKKELIJK).isPresent()),
					"into arena 1 again");
			context.waitTicks(40);
			check(itemsIn(world, FightLevel.MAKKELIJK) == 0, "no item of the last visit in arena 1 (P19)");
			leaveItem(world, FightLevel.MAKKELIJK);
			walkOut(context, world, FightLevel.MAKKELIJK);

			// Something for the reload to keep, repair or throw away: a kid's block beside the hub, a hole in its
			// floor, a spectacle chicken, and (above) an item in arena 1.
			world.getServer().runOnServer(server -> {
				ArenaBox hub = tafelBoem().hub().bounds();
				beside[0] = new BlockPos(hub.maxX() + 2, hub.minY(), (int) hub.middleZ());
				broken[0] = tafelBoem().hub().standAt().below().east(3);
				server.overworld().setBlockAndUpdate(beside[0], Blocks.OAK_PLANKS.defaultBlockState());
				server.overworld().setBlockAndUpdate(broken[0], Blocks.AIR.defaultBlockState());
				Chicken chicken = EntityTypes.CHICKEN.create(server.overworld(), EntitySpawnReason.TRIGGERED);
				BlockPos spot = tafelBoem().hub().standAt().east(2);
				chicken.setPos(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
				chicken.setNoAi(true);
				chicken.addTag(BombEffects.SPECTACLE_TAG);
				server.overworld().addFreshEntity(chicken);
			});
			context.waitTicks(5);
			save = world.getWorldSave();
		}

		// GameTests can't load a world twice, so the reload is checked here: the hub is placed again, its saved
		// frames come back from disk and must go, and the arena slots are cleared once.
		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			waitUntil(context, () -> world.getServer().computeOnServer(server -> hubFrames(server).size() >= 5), "the hub's frames after the reload");
			context.waitTicks(40);
			check(world.getServer().computeOnServer(server -> hubFrames(server).size() == 5), "still exactly 5 bomb frames after a reload, not 10");
			check(world.getServer().computeOnServer(server -> server.overworld().getBlockState(beside[0]).is(Blocks.OAK_PLANKS)),
					"the kid's block beside the hub survives the reload");
			check(world.getServer().computeOnServer(server -> server.overworld().getBlockState(broken[0]).is(Blocks.STONE)),
					"the reload repairs the hub");
			check(world.getServer().computeOnServer(server -> hubEntities(server, entity -> entity.entityTags().contains(BombEffects.SPECTACLE_TAG)).isEmpty()),
					"the spectacle chicken of the last session is gone");
			context.getInput().lookAt(door(world, 2).above(2));
			context.waitTicks(5);
			context.takeScreenshot("r1-06-hub-after-reload");

			walkInto(context, world, door(world, 1));
			waitUntil(context, () -> world.getServer().computeOnServer(server -> tafelBoem().run(FightLevel.MAKKELIJK).isPresent()),
					"into arena 1 after the reload");
			context.waitTicks(40);
			check(itemsIn(world, FightLevel.MAKKELIJK) == 0, "no item of the last session in arena 1 (P19)");
			walkOut(context, world, FightLevel.MAKKELIJK);
		}
	}

	private static List<ItemFrame> hubFrames(MinecraftServer server) {
		return server.overworld().getEntitiesOfClass(ItemFrame.class, aabb(tafelBoem().hub().bounds()));
	}

	private static List<Entity> hubEntities(MinecraftServer server, Predicate<Entity> which) {
		return server.overworld().getEntities((Entity) null, aabb(tafelBoem().hub().bounds()), which);
	}

	/** Drops an item in a corner of the arena, away from the kid's path and from where Graaf Fout's booms go. */
	private static BlockPos leaveItem(TestSingleplayerContext world, FightLevel level) {
		return world.getServer().computeOnServer(server -> {
			ServerLevel overworld = server.overworld();
			ArenaBox arena = tafelBoem().run(level).orElseThrow().arena().bounds();
			BlockPos spot = BlockPos.containing(arena.middleX() - 11, arena.minY() + 4, arena.middleZ() - 11);
			ItemEntity item = new ItemEntity(overworld, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, new ItemStack(Items.FEATHER));
			item.setNeverPickUp();
			overworld.addFreshEntity(item);
			return spot;
		});
	}

	private static int itemsIn(TestSingleplayerContext world, FightLevel level) {
		return world.getServer().computeOnServer(server -> {
			ArenaBox arena = tafelBoem().run(level).orElseThrow().arena().bounds();
			return server.overworld().getEntitiesOfClass(ItemEntity.class, aabb(arena)).size();
		});
	}

	private static AABB aabb(ArenaBox box) {
		return new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
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
