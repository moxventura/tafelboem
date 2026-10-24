package nl.jeeninga.tafelboem.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.function.ToIntFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.Fact;
import nl.jeeninga.tafelboem.game.BombEffects;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.log.AnswerLog;
import nl.jeeninga.tafelboem.log.Learners;
import nl.jeeninga.tafelboem.net.QuizC2S;
import nl.jeeninga.tafelboem.net.QuizS2C;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/**
 * The counting Kippenbom: a × b shows as a groups of b, and every chicken goes again, so 10 × 10 on
 * Moeilijk never piles up.
 */
public class ChickenGameTest {
	/** The first generation pops where it lands, at the latest 40 ticks after the burst; every chicken then lives 10 s. */
	@GameTest(maxTicks = 320, padding = 48)
	public void threeTimesFourShowsThreeGroupsOfFourThatPoofAway(GameTestHelper helper) {
		OpenField.open(helper);
		SessionManager sessions = sessions(helper);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		sessions.countChickens(helper.getLevel(), at, new Fact(3, 4), "test");
		helper.onEachTick(sessions::tick);

		helper.assertTrue(chickens(helper, at) == 3, "first one chicken per group");
		boolean[] shown = {false};
		helper.onEachTick(() -> {
			int now = chickens(helper, at);
			helper.assertTrue(now <= 12, "never more than the sum: " + now);
			shown[0] |= now == 12;
		});
		helper.runAfterDelay(20 * 12 + 40, () -> {
			helper.assertTrue(shown[0], "3 groups of 4 should have been there");
			helper.assertTrue(chickens(helper, at) == 0, "after 10 s they are gone, not " + chickens(helper, at));
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 100, padding = 48)
	public void noMoreThan150SpectacleAnimalsLiveAtOnce(GameTestHelper helper) {
		OpenField.open(helper);
		SessionManager sessions = sessions(helper);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		sessions.countChickens(helper.getLevel(), at, new Fact(10, 10), "test");
		sessions.countChickens(helper.getLevel(), at.add(8, 0, 0), new Fact(10, 10), "test");
		helper.onEachTick(sessions::tick);
		helper.onEachTick(() -> helper.assertTrue(chickens(helper, at) <= SessionManager.MAX_SPECTACLES,
				"the oldest poof first: " + chickens(helper, at) + " chickens"));

		// Every first chicken has popped by 40 ticks.
		helper.runAfterDelay(50, () -> {
			int alive = chickens(helper, at);
			helper.assertTrue(alive >= 100, "the newest burst is whole: " + alive + " chickens");
			helper.succeed();
		});
	}

	/** The whole free-play path: a kid answers right, the bomb goes off after the escape time, the first generation pops on landing. */
	@GameTest(maxTicks = 200, padding = 48)
	public void aPlacedKippenbomCountsOutItsSum(GameTestHelper helper) {
		placedKippenbom(helper, new TafelBoemConfig(), question -> question.a() * question.b());
	}

	@GameTest(maxTicks = 200, padding = 48)
	public void withCountingOffAPlacedKippenbomLets12ChickensFly(GameTestHelper helper) {
		TafelBoemConfig off = new TafelBoemConfig();
		off.countingKippenbom = false;
		placedKippenbom(helper, off, question -> 12);
	}

	/**
	 * A kid answers a placed Kippenbom right; the test passes once the bomb is gone (escape time at
	 * most 57 ticks) and exactly {@code expected} chickens stand (the pop takes at most 40 more), and
	 * fails when ever more do.
	 */
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	private static void placedKippenbom(GameTestHelper helper, TafelBoemConfig config, ToIntFunction<QuizS2C> expected) {
		OpenField.open(helper);
		RecordingQuizClient client = new RecordingQuizClient();
		SessionManager sessions = sessions(helper, client, config);
		BlockPos bombPos = new BlockPos(2, 1, 2);
		Vec3 bomb = Vec3.atCenterOf(helper.absolutePos(bombPos));
		ServerPlayer kid = helper.makeMockServerPlayerInLevel();
		Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(bombPos.east(3)));
		kid.teleportTo(stand.x, stand.y, stand.z);
		helper.onEachTick(sessions::tick);

		helper.setBlock(bombPos, ModBlocks.bomb(BombTier.KIPPENBOM));
		sessions.startFromBlock(kid, BombTier.KIPPENBOM, helper.absolutePos(bombPos));
		QuizS2C question = client.last();
		helper.assertValueEqual(question.mode(), QuizS2C.ASK, "the bomb asks its question");
		sessions.onClientAction(kid, new QuizC2S(question.sessionId(), QuizC2S.ANSWER, question.a() * question.b(), QuizC2S.VIA_KEY));

		int wanted = expected.applyAsInt(question);
		helper.onEachTick(() -> helper.assertTrue(chickens(helper, bomb, 16) <= wanted,
				question.a() + " x " + question.b() + " should show at most " + wanted + " chickens, not " + chickens(helper, bomb, 16)));
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(ModBlocks.bomb(BombTier.KIPPENBOM), bombPos);
			helper.assertTrue(chickens(helper, bomb, 16) == wanted, question.a() + " x " + question.b() + " should show " + wanted
					+ " chickens, not " + chickens(helper, bomb, 16));
		});
	}

	private static int chickens(GameTestHelper helper, Vec3 around) {
		return chickens(helper, around, 24);
	}

	private static int chickens(GameTestHelper helper, Vec3 around, double radius) {
		return helper.getLevel().getEntitiesOfClass(Chicken.class, new AABB(around, around).inflate(radius),
				chicken -> chicken.entityTags().contains(BombEffects.SPECTACLE_TAG)).size();
	}

	private static SessionManager sessions(GameTestHelper helper) {
		return sessions(helper, new RecordingQuizClient(), new TafelBoemConfig());
	}

	private static SessionManager sessions(GameTestHelper helper, RecordingQuizClient client, TafelBoemConfig config) {
		try {
			return new SessionManager(helper.getLevel().getServer(), new Learners(new AnswerLog(Files.createTempDirectory("tafelboem-gametest"))),
					client, config);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
