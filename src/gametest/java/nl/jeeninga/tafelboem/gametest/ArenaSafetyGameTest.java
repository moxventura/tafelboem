package nl.jeeninga.tafelboem.gametest;

import java.util.List;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.FightRecord;
import nl.jeeninga.tafelboem.world.ArenaRun;

/**
 * R1.3's safety by difficulty: on Easy a fall in an arena stops at half a heart; on Normal it kills,
 * and dying counts as leaving ("died"), while a sibling's fight goes on. The difficulty is the
 * world's own choice in a test (the server's is shared by every test).
 */
public class ArenaSafetyGameTest {
	@GameTest(maxTicks = 100, padding = TestWorld.PADDING)
	public void onEasyAFallInAnArenaStopsAtHalfAHeart(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		test.world.useDifficulty(() -> Difficulty.EASY);
		ServerPlayer kid = survivalKid(test);
		test.whenLoaded(() -> {
			helper.assertTrue(test.world.allowDamage(kid, fall(test), 100F), "in the hub, damage stays vanilla");
			test.world.enterDoor(kid, 1);
			hurt(test, kid, 100F);
			helper.assertTrue(kid.isAlive() && kid.getHealth() == 1F, "half a heart left: " + kid.getHealth());
			hurt(test, kid, 3F);
			helper.assertTrue(kid.getHealth() == 1F, "and nothing takes the last half heart: " + kid.getHealth());
			test.close();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 300, padding = TestWorld.PADDING)
	@Fight
	public void onNormalAFallInAnArenaKillsAndCountsAsDied(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		test.world.useDifficulty(() -> Difficulty.NORMAL);
		diesAndCountsAsDied(helper, test, "normal");
	}

	@GameTest(maxTicks = 300, padding = TestWorld.PADDING)
	@Fight
	public void withArenaSafetyOffEasyIsVanillaToo(GameTestHelper helper) {
		TafelBoemConfig config = new TafelBoemConfig();
		config.arenaSafety = false;
		TestWorld test = new TestWorld(helper).withConfig(config).running();
		test.world.useDifficulty(() -> Difficulty.EASY);
		diesAndCountsAsDied(helper, test, "easy");
	}

	private static void diesAndCountsAsDied(GameTestHelper helper, TestWorld test, String difficulty) {
		ServerPlayer kid = survivalKid(test);
		ServerPlayer sibling = test.kidInTheHub();
		test.whenLoaded(() -> {
			test.world.enterDoor(kid, 1);
			test.world.enterDoor(sibling, 1);
		});
		// He comes 60 ticks after the first kid (TestWorld has no build phase).
		helper.runAfterDelay(5 + 80, () -> hurt(test, kid, 100F));

		helper.succeedWhen(() -> {
			helper.assertTrue(kid.isDeadOrDying(), "wait for the fall: it kills");
			List<FightRecord> lines = test.log.read(kid.getPlainTextName());
			helper.assertTrue(lines.size() == 1 && lines.getFirst().result().equals(FightRecord.DIED), "logged as died: " + lines);
			helper.assertTrue(lines.getFirst().difficulty().equals(difficulty), "with the world's difficulty: " + lines.getFirst().difficulty());
			helper.assertTrue(test.client.chatsTo.stream().anyMatch(chat -> chat.player() == kid
					&& chat.message().getContents() instanceof TranslatableContents contents
					&& contents.getKey().equals("tafelboem.message.left_no_stars")), "the leaving line: no stars");
			helper.assertTrue(test.world.run(FightLevel.MAKKELIJK).isPresent(), "the arena is still running");
			ArenaRun run = test.world.run(FightLevel.MAKKELIJK).orElseThrow();
			helper.assertFalse(run.kids().contains(kid.getUUID()), "the kid has left the run");
			helper.assertTrue(run.kids().contains(sibling.getUUID()) && run.fight() != null && !run.fight().isOver(),
					"the sibling's fight goes on");
			test.close();
		});
	}

	/**
	 * A kid in survival (mock players start in creative, which nothing hurts), whose client has loaded:
	 * until it says so (a mock one never does) or 60 ticks pass, Minecraft keeps a player invulnerable.
	 */
	private static ServerPlayer survivalKid(TestWorld test) {
		ServerPlayer kid = test.kidInTheHub();
		kid.setGameMode(GameType.SURVIVAL);
		kid.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		return kid;
	}

	private static DamageSource fall(TestWorld test) {
		return test.level.damageSources().fall();
	}

	/**
	 * Damage as a fall deals it, through the world's safety check: the mod hooks that check into
	 * Fabric's {@code ALLOW_DAMAGE} for the real TafelBoem world, which a GameTest's world isn't.
	 */
	private static void hurt(TestWorld test, ServerPlayer kid, float amount) {
		if (test.world.allowDamage(kid, fall(test), amount)) {
			kid.hurtServer(test.level, fall(test), amount);
		}
	}
}
