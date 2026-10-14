package nl.jeeninga.tafelboem.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.FlatLevelSource;

import nl.jeeninga.tafelboem.TafelBoem;

/**
 * Recognises a TafelBoem world. A world doesn't remember its world type, so it goes by the
 * overworld: a flat generator with our void biome (vanilla's {@code the_void} without its platform).
 */
public final class TafelBoemWorlds {
	public static final ResourceKey<Biome> VOID = ResourceKey.create(Registries.BIOME, TafelBoem.id("void"));

	private TafelBoemWorlds() {
	}

	public static boolean isTafelBoemWorld(ServerLevel level) {
		return level.dimension() == Level.OVERWORLD
				&& level.getChunkSource().getGenerator() instanceof FlatLevelSource flat
				&& flat.settings().getBiome().is(VOID);
	}
}
