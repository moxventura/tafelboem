package nl.jeeninga.tafelboem.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.world.ArenaRun;

/**
 * R1.4: a ring of dirt dodged every bomb in 1.3.0, so kids in a fight are in adventure mode. Their own mode
 * comes back at the end of the fight, when they leave the arena, when they die, and when they log in again.
 */
public class FightModeGameTest {
	@GameTest(maxTicks = 40, padding = 48)
	public void aKidInAFightCantBreakOrPlace(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		BlockPos block = arena.kid.blockPosition().east(3);
		arena.level.setBlockAndUpdate(block, Blocks.DIRT.defaultBlockState());

		helper.runAfterDelay(3, () -> {
			helper.assertTrue(mode(arena.kid) == GameType.ADVENTURE, "adventure mode while the fight runs, not " + mode(arena.kid));
			helper.assertFalse(arena.kid.getAbilities().mayBuild, "so he may not build");
			helper.assertFalse(arena.kid.gameMode.destroyBlock(block), "and breaking a block does nothing");
			helper.assertTrue(arena.level.getBlockState(block).is(Blocks.DIRT), "the block is still there");
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40, padding = 48)
	public void theirOwnModeComesBackWhenTheFightEnds(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		arena.kid.setGameMode(GameType.SURVIVAL);

		helper.runAfterDelay(3, () -> {
			helper.assertTrue(mode(arena.kid) == GameType.ADVENTURE, "adventure in the fight");
			arena.end();
			helper.assertTrue(mode(arena.kid) == GameType.SURVIVAL, "survival again, the mode he came with: " + mode(arena.kid));
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40, padding = 48)
	public void aKidWhoLeavesTheArenaGetsTheirOwnModeBack(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(mode(arena.kid) == GameType.ADVENTURE, "adventure in the fight");
			arena.kid.teleportTo(arena.box.minX() - 15, arena.floorY, arena.box.middleZ());
		});
		helper.runAfterDelay(8, () -> {
			helper.assertTrue(mode(arena.kid) == GameType.CREATIVE, "creative again, the mode a mock kid comes with: " + mode(arena.kid));
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 60, padding = 48)
	public void aKidWhoDiesGetsTheirOwnModeBack(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		ArenaFight.survival(arena.kid);
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(mode(arena.kid) == GameType.ADVENTURE, "adventure in the fight");
			arena.kid.kill(arena.level);
		});
		helper.runAfterDelay(12, () -> {
			helper.assertTrue(arena.kid.isDeadOrDying(), "the kid died");
			helper.assertTrue(mode(arena.kid) == GameType.SURVIVAL, "survival again, so a respawn is not in adventure mode: " + mode(arena.kid));
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40, padding = 48)
	public void aKidWhoLogsInStillInAdventureModeGetsTheirOwnModeBack(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		// Not in the fight: a kid who was saved in adventure mode when the game stopped mid-fight.
		ServerPlayer saved = arena.addKid(arena.box.middleX() + 15, arena.box.middleZ());
		saved.setGameMode(GameType.SURVIVAL);
		saved.setGameMode(GameType.ADVENTURE);

		helper.runAfterDelay(3, () -> {
			arena.fights.onJoin(saved);
			helper.assertTrue(mode(saved) == GameType.SURVIVAL, "back to the mode before: " + mode(saved));
			arena.fights.onJoin(arena.kid);
			helper.assertTrue(mode(arena.kid) == GameType.ADVENTURE, "a kid who is in the running fight is left alone");
			arena.end();
			helper.succeed();
		});
	}

	/**
	 * A kid drops out of a fight on LAN (the PC went off) and logs in again while the sibling still fights. Minecraft
	 * saved them in adventure, in the arena, and the fight still counts them; they are not in the run any more, so they
	 * go to the hub, and must get their own mode back there.
	 */
	@GameTest(maxTicks = 400, padding = TestWorld.PADDING)
	@Fight
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	public void aKidWhoLogsInWhileTheSiblingsFightRunsGoesToTheHubInTheirOwnMode(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer sibling = test.kidInTheHub();
		test.world.enterDoor(sibling, 1);
		ServerPlayer[] back = {null};
		long[] joinedAt = {-1};
		helper.onEachTick(() -> {
			BossFight fight = test.world.run(FightLevel.MAKKELIJK).map(ArenaRun::fight).orElse(null);
			if (fight == null) {
				return;
			}
			if (back[0] == null) {
				// As saved when the PC went off: in the arena, in adventure, survival before it.
				ServerPlayer kid = helper.makeMockServerPlayerInLevel();
				kid.setGameMode(GameType.SURVIVAL);
				kid.setGameMode(GameType.ADVENTURE);
				kid.teleportTo(fight.arena().middleX(), sibling.getY(), fight.arena().middleZ());
				back[0] = kid;
			} else if (joinedAt[0] < 0 && fight.participants().contains(back[0].getUUID())) {
				// The fight holds them, as it holds a kid whose saved spot is in its arena when they log in.
				test.world.onJoin(back[0]);
				joinedAt[0] = helper.getTick();
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(joinedAt[0] >= 0, "wait for the fight and the login");
			helper.assertTrue(helper.getTick() - joinedAt[0] >= 5, "and a few fight ticks after it");
			helper.assertTrue(test.world.hub().bounds().containsColumn(back[0].getX(), back[0].getZ()), "the kid is in the hub");
			helper.assertTrue(mode(back[0]) == GameType.SURVIVAL, "in their own mode again: " + mode(back[0]));
			helper.assertTrue(mode(sibling) == GameType.ADVENTURE, "the sibling still fights in adventure");
			test.close();
		});
	}

	@GameTest(maxTicks = 40, padding = 48)
	public void withNoBuildInFightOffTheKidKeepsTheirOwnMode(GameTestHelper helper) {
		TafelBoemConfig config = new TafelBoemConfig();
		config.noBuildInFight = false;
		ArenaFight arena = new ArenaFight(helper, config);

		helper.runAfterDelay(3, () -> {
			helper.assertTrue(mode(arena.kid) == GameType.CREATIVE, "no switch, no adventure mode: " + mode(arena.kid));
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 400, padding = TestWorld.PADDING)
	@Fight
	public void aFightStartsWithoutABuildPhaseAndTheKidIsBackInTheirModeOnLeaving(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		test.world.enterDoor(kid, 1);
		boolean[] leaving = {false};

		helper.succeedWhen(() -> {
			if (!leaving[0]) {
				ArenaRun run = test.world.run(FightLevel.MAKKELIJK).orElse(null);
				helper.assertTrue(run != null && run.fight() != null, "wait for him: about 3 s after the kid came in, as in 1.1.2");
				helper.assertTrue(run.building() == null, "no build phase");
				helper.assertTrue(mode(kid) == GameType.ADVENTURE, "adventure in the fight: " + mode(kid));
				// The exit door. With the only kid gone the run ends, so from here on only his mode is checked.
				test.world.enterDoor(kid, 0);
				leaving[0] = true;
			}
			helper.assertTrue(mode(kid) == GameType.CREATIVE, "back to creative once he has left: " + mode(kid));
			test.close();
		});
	}

	private static GameType mode(ServerPlayer kid) {
		return kid.gameMode.getGameModeForPlayer();
	}
}
