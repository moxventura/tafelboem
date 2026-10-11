package nl.jeeninga.tafelboem.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.command.ArenaSetup;

/**
 * Playtest 1: slimes and nights in the superflat world. {@code /tafelboem arena} fixes both.
 */
public class ArenaSetupGameTest {
	private static final BlockPos CENTER = new BlockPos(2, 1, 2);

	@GameTest
	public void arenaRemovesSlimesStopsTheNightAndLaysAFloor(GameTestHelper helper) {
		GameRules rules = helper.getLevel().getGameRules();
		MinecraftServer server = helper.getLevel().getServer();
		boolean time = rules.get(GameRules.ADVANCE_TIME);
		boolean mobs = rules.get(GameRules.SPAWN_MOBS);
		boolean monsters = rules.get(GameRules.SPAWN_MONSTERS);
		boolean weather = rules.get(GameRules.ADVANCE_WEATHER);
		boolean keep = rules.get(GameRules.KEEP_INVENTORY);
		Slime slime = helper.spawn(EntityTypes.SLIME, CENTER.east(2));
		helper.setBlock(CENTER.below(), Blocks.STONE);

		try {
			ArenaSetup.apply(helper.getLevel(), helper.absolutePos(CENTER));

			helper.assertTrue(slime.isRemoved(), "the slime is gone");
			helper.assertFalse(rules.get(GameRules.ADVANCE_TIME), "no more day/night cycle");
			helper.assertFalse(rules.get(GameRules.SPAWN_MONSTERS), "no more monsters spawning");
			helper.assertTrue(rules.get(GameRules.KEEP_INVENTORY), "the kids keep their bombs");
			helper.assertBlockPresent(Blocks.CONCRETE.purple(), CENTER.below());
			helper.assertBlockPresent(Blocks.AIR, CENTER.below().east(3));
		} finally {
			// Other tests share this level.
			rules.set(GameRules.ADVANCE_TIME, time, server);
			rules.set(GameRules.SPAWN_MOBS, mobs, server);
			rules.set(GameRules.SPAWN_MONSTERS, monsters, server);
			rules.set(GameRules.ADVANCE_WEATHER, weather, server);
			rules.set(GameRules.KEEP_INVENTORY, keep, server);
		}
		helper.succeed();
	}
}
