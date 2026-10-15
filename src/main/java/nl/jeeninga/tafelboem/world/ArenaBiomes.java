package nl.jeeninga.tafelboem.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.FightLevel;

/**
 * An arena gets its level's biome when it is placed, and the biome brings the level's battle music:
 * in 26.2 background music is a biome attribute ({@code data/tafelboem/worldgen/biome/arena_<n>.json}).
 * The hub's void biome brings the normal music back. With {@code arenaMusic} off, an arena gets the
 * void biome too.
 */
final class ArenaBiomes {
	/** One fill may change at most {@code max_block_modifications} (32768) blocks; 48 × 48 × 8 is well under. */
	private static final int LAYER = 8;

	private ArenaBiomes() {
	}

	static void set(ServerLevel level, ArenaBox box, FightLevel fightLevel, boolean music) {
		ResourceKey<Biome> key = music ? ResourceKey.create(Registries.BIOME, TafelBoem.id("arena_" + fightLevel.number())) : TafelBoemWorlds.VOID;
		Holder<Biome> biome = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key);
		for (int y = box.minY(); y <= box.maxY(); y += LAYER) {
			BlockPos from = new BlockPos(box.minX(), y, box.minZ());
			BlockPos to = new BlockPos(box.maxX(), Math.min(y + LAYER - 1, box.maxY()), box.maxZ());
			FillBiomeCommand.fill(level, from, to, biome)
					.ifRight(error -> TafelBoem.LOGGER.warn("No music in arena {}: {}", fightLevel.number(), error.getMessage()));
		}
	}
}
