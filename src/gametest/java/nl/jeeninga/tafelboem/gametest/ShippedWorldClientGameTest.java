package nl.jeeninga.tafelboem.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.world.TafelBoemWorlds;

/**
 * The ready-made world the pack ships (P26): the kids couldn't make a TafelBoem world themselves.
 * The pack ships only the two files that make it a TafelBoem world: {@code level.dat} (name, game
 * mode, cheats) and {@code data/minecraft/world_gen_settings.dat} (the World Type; without it
 * Minecraft falls back to the default world). packwiz installs them once, and Minecraft and the mod
 * build the rest on the first load. This test puts those files alone in a save folder, as a new
 * PC gets them, loads it, and checks the world the kids will see: TafelBoem, survival, cheats off, the
 * kid in the hub.
 *
 * <p>With {@code -PwriteShippedWorld} it first makes the file anew: a world from the Create World
 * screen with those settings, saved once. The player's data is in {@code players/}, so it doesn't
 * travel along.
 */
public class ShippedWorldClientGameTest implements FabricClientGameTest {
	static final String NAME = "TafelBoem";
	/** The files that make the shipped world, relative to its folder. */
	static final List<String> FILES = List.of("level.dat", "data/minecraft/world_gen_settings.dat");
	private static final ResourceKey<WorldPreset> PRESET = ResourceKey.create(Registries.WORLD_PRESET, TafelBoem.id("tafelboem"));

	@Override
	public void runTest(ClientGameTestContext context) {
		Path shipped = Path.of(System.getProperty("tafelboem.shippedWorld"));
		if (Boolean.getBoolean("tafelboem.writeShippedWorld")) {
			write(context, shipped);
		}
		for (String file : FILES) {
			check(Files.isRegularFile(shipped.resolve(file)), "no " + file + " in the shipped world at " + shipped + "; make it with -PwriteShippedWorld");
		}

		TestWorldSave save;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			save = world.getWorldSave();
		}
		Path folder = save.getSaveDirectory();
		emptyFolder(folder);
		for (String file : FILES) {
			copy(shipped.resolve(file), folder.resolve(file));
		}

		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);
			check(world.getServer().computeOnServer(server -> TafelBoemWorlds.isTafelBoemWorld(server.overworld())), "a TafelBoem world");
			check(world.getServer().computeOnServer(server -> NAME.equals(server.getWorldData().getLevelName())), "named " + NAME);
			check(world.getServer().computeOnServer(server -> server.getWorldData().getGameType() == GameType.SURVIVAL), "survival");
			check(world.getServer().computeOnServer(server -> !server.getWorldData().isAllowCommands()), "cheats off");
			check(world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				return player.gameMode() == GameType.SURVIVAL
						&& TafelBoem.world().orElseThrow().hub().bounds().holds(player.getX(), player.getY(), player.getZ());
			}), "the kid starts in survival, in the hub");
			context.takeScreenshot("p26-shipped-world");
		}
	}

	/** A new world as the parent would make it, saved once; only {@link #FILES} are kept. */
	private static void write(ClientGameTestContext context, Path shipped) {
		TestWorldSave save;
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(ShippedWorldClientGameTest::settings).create()) {
			world.getConnection().waitForChunksRender();
			save = world.getWorldSave();
		}
		try {
			CompoundTag level = NbtIo.readCompressed(save.getSaveDirectory().resolve("level.dat"), NbtAccounter.unlimitedHeap());
			CompoundTag data = level.getCompoundOrEmpty("Data");
			check(!data.contains("Player"), "no player in level.dat: every kid starts fresh in the hub");
			// Whose world it is: on each PC it becomes that kid's, so the test player's id must not travel along.
			data.remove("singleplayer_uuid");
			// The test mod's data packs aren't on the kids' PCs.
			ListTag enabled = data.getCompoundOrEmpty("DataPacks").getListOrEmpty("Enabled");
			enabled.removeIf(pack -> pack.asString().orElse("").contains("gametest"));
			emptyFolder(shipped);
			for (String file : FILES) {
				copy(save.getSaveDirectory().resolve(file), shipped.resolve(file));
			}
			NbtIo.writeCompressed(level, shipped.resolve("level.dat"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void settings(WorldCreationUiState state) {
		state.setName(NAME);
		state.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
		state.setAllowCommands(false);
		state.setWorldType(state.getNormalPresetList().stream()
				.filter(type -> type.preset().is(PRESET))
				.findFirst()
				.orElseThrow(() -> new AssertionError("TafelBoem is not on the World Type button")));
	}

	private static void emptyFolder(Path folder) {
		if (!Files.isDirectory(folder)) {
			return;
		}
		try (Stream<Path> files = Files.walk(folder)) {
			for (Path file : files.sorted(Comparator.reverseOrder()).filter(path -> !path.equals(folder)).toList()) {
				Files.delete(file);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void copy(Path from, Path to) {
		try {
			Files.createDirectories(to.getParent());
			Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
