package nl.jeeninga.tafelboem.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.boss.BossFights;
import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.YeetStyle;
import nl.jeeninga.tafelboem.game.PlayerSafeExplosion;
import nl.jeeninga.tafelboem.game.ProtectedAreas;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.log.AnswerLog;
import nl.jeeninga.tafelboem.log.Learners;

/**
 * Graaf Fout in an arena: he never falls into the void for good, and craters spare the outside and
 * the floor under the kids.
 */
public class ArenaFightGameTest {
	@GameTest(maxTicks = 400, padding = 48)
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	public void whenHeFliesOutOfTheArenaHePoofsBackNearTheKids(GameTestHelper helper) {
		OpenField.open(helper);
		RecordingQuizClient client = new RecordingQuizClient();
		SessionManager sessions = new SessionManager(helper.getLevel().getServer(), new Learners(new AnswerLog(tempDir())), client);
		BossFights fights = new BossFights(sessions);
		// OpenField's stone floor is one block below the test's corner: that is the arena's bottom.
		BlockPos corner = helper.absolutePos(new BlockPos(-10, -1, -10));
		ArenaBox arena = ArenaBox.of(corner.getX(), corner.getY(), corner.getZ(), 21, 6, 21);

		ServerPlayer kid = helper.makeMockServerPlayerInLevel();
		kid.teleportTo(arena.middleX(), corner.getY() + 1, arena.middleZ() + 5);
		BossFight fight = fights.summon(helper.getLevel(), new Vec3(arena.middleX(), corner.getY() + 2.5, arena.middleZ()), FightLevel.MAKKELIJK, arena);
		helper.assertTrue(fight != null, "Graaf Fout should appear");
		GraafFout boss = fight.boss();
		boss.yeet(kid.position(), 1.0, YeetStyle.CLASSIC, kid.position());
		boss.teleportTo(arena.middleX(), arena.minY() - 4, arena.middleZ());

		helper.onEachTick(() -> {
			sessions.tick();
			fights.tick();
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(arena.holds(boss.getX(), boss.getY(), boss.getZ()), "he should be back in the arena");
			helper.assertTrue(client.actionBars.stream().anyMatch(ArenaFightGameTest::isFlewOut), "the yeet-o-meter says he flew out");
			helper.assertFalse(fight.isOver(), "the fight goes on");
			fights.endAll();
		});
	}

	@GameTest(padding = 48)
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	public void aCraterSparesObsidianBedrockAndTheFloorNearAKid(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos centre = helper.absolutePos(new BlockPos(5, 2, 5));
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				level.setBlockAndUpdate(centre.offset(dx, -2, dz), Blocks.BEDROCK.defaultBlockState());
				level.setBlockAndUpdate(centre.offset(dx, -1, dz), Blocks.DIRT.defaultBlockState());
			}
		}
		BlockPos obsidian = centre.offset(2, 0, 0);
		level.setBlockAndUpdate(obsidian, Blocks.OBSIDIAN.defaultBlockState());
		ServerPlayer kid = helper.makeMockServerPlayerInLevel();
		kid.teleportTo(centre.getX() + 0.5, centre.getY(), centre.getZ() - 2.5);

		level.explode(null, null, PlayerSafeExplosion.sparing(List.of(kid.position()), 3), centre.getX() + 0.5, centre.getY(),
				centre.getZ() + 0.5, 4.0F, false, Level.ExplosionInteraction.TNT);

		helper.assertTrue(level.getBlockState(centre.below()).isAir(), "the dirt under the boom is gone");
		helper.assertTrue(level.getBlockState(centre.offset(0, -1, -3)).is(Blocks.DIRT), "the floor under the kid stays");
		helper.assertTrue(level.getBlockState(obsidian).is(Blocks.OBSIDIAN), "obsidian never breaks");
		helper.assertTrue(level.getBlockState(centre.offset(0, -2, 0)).is(Blocks.BEDROCK), "bedrock never breaks");
		helper.succeed();
	}

	@GameTest(padding = 48)
	public void nothingBreaksInsideAProtectedArea(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos centre = helper.absolutePos(new BlockPos(5, 2, 5));
		ArenaBox hub = ArenaBox.of(centre.getX() - 5, centre.getY() - 2, centre.getZ() - 5, 11, 6, 11);
		for (int dx = -3; dx <= 3; dx++) {
			level.setBlockAndUpdate(centre.offset(dx, -1, 0), Blocks.STONE.defaultBlockState());
		}
		ProtectedAreas.protect(level, hub);
		try {
			level.explode(null, null, PlayerSafeExplosion.INSTANCE, centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, 4.0F, false,
					Level.ExplosionInteraction.TNT);
			helper.assertTrue(level.getBlockState(centre.below()).is(Blocks.STONE), "the hub floor stays");
		} finally {
			ProtectedAreas.unprotect(level, hub);
		}
		helper.succeed();
	}

	@GameTest
	public void unprotectingAnUnknownAreaChangesNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// Nothing protects anything in the nether, so this takes the path for a dimension without areas,
		// without clearing the areas other tests are using right now.
		ServerLevel nether = level.getServer().getLevel(Level.NETHER);
		helper.assertTrue(nether != null, "the test server has a nether");
		BlockPos corner = helper.absolutePos(BlockPos.ZERO);
		ArenaBox here = ArenaBox.of(corner.getX(), corner.getY(), corner.getZ(), 1, 1, 1);
		ProtectedAreas.protect(level, here);
		try {
			ProtectedAreas.unprotect(nether, here);

			helper.assertFalse(ProtectedAreas.isProtected(nether, corner), "nothing is protected in the nether");
			helper.assertTrue(ProtectedAreas.isProtected(level, corner), "the same box here is still protected");
		} finally {
			ProtectedAreas.unprotect(level, here);
		}
		helper.succeed();
	}

	private static boolean isFlewOut(Component message) {
		return message.getContents() instanceof TranslatableContents translatable
				&& translatable.getKey().equals("tafelboem.message.yeet_out");
	}

	private static java.nio.file.Path tempDir() {
		try {
			return Files.createTempDirectory("tafelboem-gametest");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
