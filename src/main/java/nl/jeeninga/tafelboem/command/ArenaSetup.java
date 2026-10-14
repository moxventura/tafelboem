package nl.jeeninga.tafelboem.command;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;

import nl.jeeninga.tafelboem.boss.GraafFout;

/**
 * {@code /tafelboem arena}: makes any world fit for TafelBoem. Playtest 1: the kids disliked the
 * superflat world with its slimes and nights. Always day, no weather, no mobs, keep inventory, and
 * a round arena floor around the player so they know where the fight happens.
 */
public final class ArenaSetup {
	static final int FLOOR_RADIUS = 16;
	static final int CLEAR_RADIUS = 64;

	private ArenaSetup() {
	}

	/**
	 * @return how many hostile mobs were removed
	 */
	public static int apply(ServerLevel level, BlockPos center) {
		applyRules(level);

		List<Entity> enemies = level.getEntities((Entity) null, new AABB(center).inflate(CLEAR_RADIUS),
				entity -> entity instanceof Enemy && !(entity instanceof GraafFout));
		enemies.forEach(Entity::discard);

		layFloor(level, center);
		return enemies.size();
	}

	/** Always day, no weather, no mobs, keep inventory: also the rules of a TafelBoem world. */
	public static void applyRules(ServerLevel level) {
		MinecraftServer server = level.getServer();
		GameRules rules = level.getGameRules();
		rules.set(GameRules.ADVANCE_TIME, false, server);
		rules.set(GameRules.ADVANCE_WEATHER, false, server);
		rules.set(GameRules.SPAWN_MOBS, false, server);
		rules.set(GameRules.SPAWN_MONSTERS, false, server);
		rules.set(GameRules.KEEP_INVENTORY, true, server);
		// Time and weather go through the vanilla commands: their APIs change between versions, the commands don't.
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(level).withSuppressedOutput(), "time set day");
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(level).withSuppressedOutput(), "weather clear");
	}

	/** Purple and white rings, one block below the player, only where there is ground already. */
	private static void layFloor(ServerLevel level, BlockPos center) {
		for (int dx = -FLOOR_RADIUS; dx <= FLOOR_RADIUS; dx++) {
			for (int dz = -FLOOR_RADIUS; dz <= FLOOR_RADIUS; dz++) {
				double distance = Math.sqrt(dx * dx + dz * dz);
				if (distance > FLOOR_RADIUS + 0.5) {
					continue;
				}
				BlockPos pos = center.offset(dx, -1, dz);
				BlockState ground = level.getBlockState(pos);
				if (!ground.isSolid()) {
					continue;
				}
				BlockState ring = ((int) Math.round(distance) / 2) % 2 == 0
						? Blocks.CONCRETE.purple().defaultBlockState()
						: Blocks.CONCRETE.white().defaultBlockState();
				level.setBlock(pos, ring, Block.UPDATE_CLIENTS);
			}
		}
	}
}
