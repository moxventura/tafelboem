package nl.jeeninga.tafelboem.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.function.ToIntFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.Fact;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.log.AnswerLog;
import nl.jeeninga.tafelboem.log.Learners;
import nl.jeeninga.tafelboem.net.QuizC2S;
import nl.jeeninga.tafelboem.net.QuizS2C;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/**
 * The counting Kippenbom: a × b shows as a groups of b, and every chicken goes again, so 10 × 10 on
 * Moeilijk never piles up. Each test counts only its own chickens ({@link OwnChickens}).
 */
public class ChickenGameTest {
	/** The first generation pops where it lands, at the latest 40 ticks after the burst; every chicken then lives 10 s. */
	@GameTest(maxTicks = 320, padding = 48)
	public void threeTimesFourShowsThreeGroupsOfFourThatPoofAway(GameTestHelper helper) {
		OpenField.open(helper);
		SessionManager sessions = sessions(helper);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		OwnChickens chickens = new OwnChickens(helper).followPops();
		List<UUID> firsts = chickens.claimDuring(() -> sessions.countChickens(helper.getLevel(), at, new Fact(3, 4), "test"),
				at, OwnChickens.SPAWN_RADIUS);

		helper.assertTrue(firsts.size() == 3, "first one chicken per group, not " + firsts.size());
		boolean[] shown = {false};
		helper.onEachTick(() -> {
			sessions.tick();
			chickens.tick();
			int now = chickens.alive();
			helper.assertTrue(now <= 12, "never more than the sum: " + now);
			shown[0] |= now == 12;
		});
		helper.runAfterDelay(20 * 12 + 40, () -> {
			helper.assertTrue(shown[0], "3 groups of 4 should have been there");
			helper.assertTrue(chickens.alive() == 0, "after 10 s they are gone, not " + chickens.alive());
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 100, padding = 48)
	public void noMoreThan150SpectacleAnimalsLiveAtOnce(GameTestHelper helper) {
		OpenField.open(helper);
		SessionManager sessions = sessions(helper);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		Vec3 next = at.add(8, 0, 0);
		OwnChickens chickens = new OwnChickens(helper).followPops();
		chickens.claimDuring(() -> sessions.countChickens(helper.getLevel(), at, new Fact(10, 10), "test"), at, OwnChickens.SPAWN_RADIUS);
		chickens.claimDuring(() -> sessions.countChickens(helper.getLevel(), next, new Fact(10, 10), "test"), next, OwnChickens.SPAWN_RADIUS);
		helper.onEachTick(() -> {
			sessions.tick();
			chickens.tick();
			helper.assertTrue(chickens.alive() <= SessionManager.MAX_SPECTACLES, "the oldest poof first: " + chickens.alive() + " chickens");
		});

		// Every first chicken has popped by 40 ticks; the cap then keeps a crowd, not a handful.
		helper.runAfterDelay(50, () -> {
			int alive = chickens.alive();
			helper.assertTrue(alive >= 100, "after the cap 100 or more chickens are left: " + alive + " chickens");
			helper.succeed();
		});
	}

	/**
	 * A first chicken that a boom took pops where it was last, and one held in the air pops at the
	 * limit, so the groups still add up to the sum.
	 */
	@GameTest(maxTicks = 120, padding = 48)
	public void aChickenThatIsGoneOrStuckInTheAirStillPopsIntoItsGroup(GameTestHelper helper) {
		OpenField.open(helper);
		SessionManager sessions = sessions(helper);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		OwnChickens chickens = new OwnChickens(helper).followPops();
		List<UUID> firsts = chickens.claimDuring(() -> sessions.countChickens(helper.getLevel(), at, new Fact(3, 4), "test"),
				at, OwnChickens.SPAWN_RADIUS);
		helper.assertTrue(firsts.size() == 3, "first one chicken per group");
		helper.getLevel().getEntity(firsts.get(0)).discard();
		helper.getLevel().getEntity(firsts.get(1)).setNoGravity(true);

		int[] tick = {0};
		int[] fullAt = {-1};
		helper.onEachTick(() -> {
			sessions.tick();
			chickens.tick();
			tick[0]++;
			int now = chickens.alive();
			helper.assertTrue(now <= 12, "never more than the sum: " + now);
			if (now == 12 && fullAt[0] < 0) {
				fullAt[0] = tick[0];
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(fullAt[0] >= 0, "3 groups of 4 in the end, seen " + chickens.alive());
			helper.assertTrue(fullAt[0] >= 38, // the limit is 40 ticks after the burst
					"the chicken in the air pops at the limit, not before: tick " + fullAt[0]);
		});
	}

	/**
	 * Another test's chickens next to this test's spot are not this test's: they were there first, and
	 * the ones they pop into appear where none of this test's chickens went.
	 */
	@GameTest(maxTicks = 120, padding = 48)
	public void aNeighboursChickensNextToTheSpotAreNotCounted(GameTestHelper helper) {
		OpenField.open(helper);
		SessionManager mine = sessions(helper);
		SessionManager neighbour = sessions(helper);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		neighbour.countChickens(helper.getLevel(), at.add(16, 0, 0), new Fact(2, 5), "test");
		OwnChickens chickens = new OwnChickens(helper).followPops();
		helper.onEachTick(() -> {
			mine.tick();
			neighbour.tick();
			chickens.tick();
			helper.assertTrue(chickens.alive() <= 12, "never the neighbour's: " + chickens.alive());
		});
		// The neighbour's chickens are out and popping before this test's appear.
		helper.runAfterDelay(20, () -> chickens.claimDuring(() -> mine.countChickens(helper.getLevel(), at, new Fact(3, 4), "test"),
				at, OwnChickens.SPAWN_RADIUS));
		helper.runAfterDelay(20 + 45, () -> {
			helper.assertTrue(chickens.alive() == 12, "3 groups of 4, and none of the neighbour's 10: " + chickens.alive());
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
		// The boom lets the chickens out where the bomb stood.
		OwnChickens chickens = new OwnChickens(helper).followPops();
		chickens.watch(() -> bomb);

		helper.setBlock(bombPos, ModBlocks.bomb(BombTier.KIPPENBOM));
		sessions.startFromBlock(kid, BombTier.KIPPENBOM, helper.absolutePos(bombPos));
		QuizS2C question = client.last();
		helper.assertValueEqual(question.mode(), QuizS2C.ASK, "the bomb asks its question");
		sessions.onClientAction(kid, new QuizC2S(question.sessionId(), QuizC2S.ANSWER, question.a() * question.b(), QuizC2S.VIA_KEY));

		int wanted = expected.applyAsInt(question);
		helper.onEachTick(() -> {
			sessions.tick();
			chickens.tick();
			helper.assertTrue(chickens.alive() <= wanted,
					question.a() + " x " + question.b() + " should show at most " + wanted + " chickens, not " + chickens.alive());
		});
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(ModBlocks.bomb(BombTier.KIPPENBOM), bombPos);
			helper.assertTrue(chickens.alive() == wanted, question.a() + " x " + question.b() + " should show " + wanted
					+ " chickens, not " + chickens.alive());
		});
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
