package nl.jeeninga.tafelboem.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.boss.BossFights;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.ChickenCount;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.FightTally;
import nl.jeeninga.tafelboem.game.BombEffects;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.log.AnswerLog;
import nl.jeeninga.tafelboem.log.Learners;
import nl.jeeninga.tafelboem.net.QuizC2S;
import nl.jeeninga.tafelboem.net.QuizS2C;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/**
 * End to end: Graaf Fout throws, the bomb lands as a question, a correct answer kicks it back and
 * costs him a hit.
 */
public class BossFightGameTest {
	/** Out of the boss bar's reach (48), but within the 96 he may come back from. */
	private static final int FAR_AWAY = 60;

	@GameTest(maxTicks = 600)
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	public void aCorrectAnswerKicksTheBombBackAndHitsGraafFout(GameTestHelper helper) {
		RecordingQuizClient client = new RecordingQuizClient();
		SessionManager sessions = new SessionManager(helper.getLevel().getServer(), new Learners(new AnswerLog(tempDir())), client);
		BossFights fights = new BossFights(sessions);

		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		player.teleportTo(stand.x, stand.y, stand.z);
		helper.assertTrue(fights.summon(helper.getLevel(), stand.add(6, 1.5, 0)), "Graaf Fout should appear");
		BossFight fight = fights.active().getFirst();
		int startHits = fight.hitsRemaining();

		Set<Integer> answered = new HashSet<>();
		helper.onEachTick(() -> {
			sessions.tick();
			fights.tick();
			// Answer every question correctly, whichever nearby player the boss picked.
			for (RecordingQuizClient.Sent sent : List.copyOf(client.sentTo)) {
				QuizS2C question = sent.payload();
				if (question.mode() == QuizS2C.ASK && answered.add(question.sessionId())) {
					sessions.onClientAction(sent.player(), new QuizC2S(question.sessionId(), QuizC2S.ANSWER, question.a() * question.b(), QuizC2S.VIA_KEY));
				}
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(fight.hitsRemaining() < startHits, "a kicked-back bomb should hit Graaf Fout");
			int boss = fight.boss().getId();
			helper.assertTrue(client.tracks.stream().anyMatch(t -> t.entityId() == boss && t.ticksAfterScreen() == 0 && !t.kick()),
					"while answering, the view turns to Graaf Fout");
			helper.assertTrue(client.tracks.stream().anyMatch(t -> t.entityId() == boss && t.ticksAfterScreen() == BossFight.KICK_CAM_MAX_TICKS && t.kick()),
					"after the kick, the view follows him through the yeet");
			fights.endAll();
		});
	}

	@GameTest(maxTicks = 600, padding = 48)
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	public void theViewFollowsGraafFoutUntilHeIsDizzyOnTheGround(GameTestHelper helper) {
		OpenField.open(helper);
		RecordingQuizClient client = new RecordingQuizClient();
		SessionManager sessions = new SessionManager(helper.getLevel().getServer(), new Learners(new AnswerLog(tempDir())), client);
		BossFights fights = new BossFights(sessions);

		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		player.teleportTo(stand.x, stand.y, stand.z);
		helper.assertTrue(fights.summon(helper.getLevel(), stand.add(6, 1.5, 0)), "Graaf Fout should appear");
		BossFight fight = fights.active().getFirst();

		Set<Integer> answered = new HashSet<>();
		helper.onEachTick(() -> {
			sessions.tick();
			fights.tick();
			for (RecordingQuizClient.Sent sent : List.copyOf(client.sentTo)) {
				QuizS2C question = sent.payload();
				if (question.mode() == QuizS2C.ASK && answered.add(question.sessionId())) {
					sessions.onClientAction(sent.player(), new QuizC2S(question.sessionId(), QuizC2S.ANSWER, question.a() * question.b(), QuizC2S.VIA_KEY));
				}
			}
		});

		helper.succeedWhen(() -> {
			int boss = fight.boss().getId();
			helper.assertFalse(client.actionBars.stream().noneMatch(BossFightGameTest::isYeetOMeter), "wait until he has landed");
			helper.assertTrue(client.tracks.stream().anyMatch(t -> t.player() == player && t.entityId() == boss
					&& t.ticksAfterScreen() == BossFight.LANDED_CAM_TICKS && !t.kick()),
					"once he lands, the view stays on him while he is dizzy and then lets go");
			fights.endAll();
		});
	}

	/**
	 * Two kids, one Graaf Fout who takes turns: every camera message goes to the kid whose bomb it
	 * is, and the other kid's view is never touched. Runs until both have kicked, so a stale kicker
	 * from the previous yeet would show up too.
	 */
	@GameTest(maxTicks = 1200, padding = 48)
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	public void inMultiplayerOnlyTheKidWhoseBombItIsGetsTheirViewMoved(GameTestHelper helper) {
		OpenField.open(helper);
		RecordingQuizClient client = new RecordingQuizClient();
		SessionManager sessions = new SessionManager(helper.getLevel().getServer(), new Learners(new AnswerLog(tempDir())), client);
		BossFights fights = new BossFights(sessions);

		ServerPlayer first = helper.makeMockServerPlayerInLevel();
		ServerPlayer second = helper.makeMockServerPlayerInLevel();
		Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		first.teleportTo(stand.x, stand.y, stand.z);
		second.teleportTo(stand.x, stand.y, stand.z + 3);
		helper.assertTrue(fights.summon(helper.getLevel(), stand.add(6, 1.5, 1.5)), "Graaf Fout should appear");
		BossFight fight = fights.active().getFirst();

		Set<Integer> answered = new HashSet<>();
		int[] tracksChecked = {0};
		helper.onEachTick(() -> {
			sessions.tick();
			fights.tick();
			for (RecordingQuizClient.Sent sent : List.copyOf(client.sentTo)) {
				QuizS2C question = sent.payload();
				if (question.mode() == QuizS2C.ASK && answered.add(question.sessionId())) {
					sessions.onClientAction(sent.player(), new QuizC2S(question.sessionId(), QuizC2S.ANSWER, question.a() * question.b(), QuizC2S.VIA_KEY));
				}
			}
			// Whose turn it is: the kid who got the latest question, until the next one is asked.
			ServerPlayer turn = client.sentTo.stream().filter(sent -> sent.payload().mode() == QuizS2C.ASK)
					.reduce((a, b) -> b).map(RecordingQuizClient.Sent::player).orElse(null);
			for (; tracksChecked[0] < client.tracks.size(); tracksChecked[0]++) {
				RecordingQuizClient.Track track = client.tracks.get(tracksChecked[0]);
				String to = track.player() == first ? "the first kid" : "the second kid";
				String whoseTurn = turn == null ? "nobody's" : turn == first ? "the first kid's" : "the second kid's";
				helper.assertTrue(track.player() == turn, "a camera message went to " + to + " while it was " + whoseTurn + " turn");
			}
		});

		helper.succeedWhen(() -> {
			for (ServerPlayer kid : List.of(first, second)) {
				helper.assertTrue(client.tracks.stream().anyMatch(t -> t.player() == kid && t.kick()), "wait until both kids have kicked");
				helper.assertTrue(client.tracks.stream().anyMatch(t -> t.player() == kid && t.ticksAfterScreen() == BossFight.LANDED_CAM_TICKS),
						"wait until both yeets have landed, each followed by the kid who kicked");
			}
			fights.endAll();
		});
	}

	/**
	 * Two kids: a kid's second right answer in a row yeets him further, and only that kid's meter says
	 * so. The other kid sees the plain meter: no ranking between siblings.
	 */
	@GameTest(maxTicks = 2400, padding = 48)
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	public void onlyTheKidWhoKickedSeesWhyHeFlewFurther(GameTestHelper helper) {
		OpenField.open(helper);
		RecordingQuizClient client = new RecordingQuizClient();
		SessionManager sessions = new SessionManager(helper.getLevel().getServer(), new Learners(new AnswerLog(tempDir())), client);
		BossFights fights = new BossFights(sessions);

		ServerPlayer first = helper.makeMockServerPlayerInLevel();
		ServerPlayer second = helper.makeMockServerPlayerInLevel();
		Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		first.teleportTo(stand.x, stand.y, stand.z);
		second.teleportTo(stand.x, stand.y, stand.z + 3);
		helper.assertTrue(fights.summon(helper.getLevel(), stand.add(6, 1.5, 1.5)), "Graaf Fout should appear");

		Set<Integer> answered = new HashSet<>();
		ServerPlayer[] kicker = {null};
		int[] checked = {0};
		boolean[] whySeen = {false};
		helper.onEachTick(() -> {
			sessions.tick();
			fights.tick();
			for (RecordingQuizClient.Sent sent : List.copyOf(client.sentTo)) {
				QuizS2C question = sent.payload();
				if (question.mode() == QuizS2C.ASK && answered.add(question.sessionId())) {
					sessions.onClientAction(sent.player(), new QuizC2S(question.sessionId(), QuizC2S.ANSWER, question.a() * question.b(), QuizC2S.VIA_KEY));
					kicker[0] = sent.player();
				}
			}
			for (; checked[0] < client.actionBarsTo.size(); checked[0]++) {
				RecordingQuizClient.Bar bar = client.actionBarsTo.get(checked[0]);
				if (!isYeetOMeter(bar.message())) {
					continue;
				}
				boolean why = !bar.message().getSiblings().isEmpty();
				helper.assertTrue(!why || bar.player() == kicker[0], "only the kid who kicked sees why he flew further");
				whySeen[0] |= why;
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(whySeen[0], "wait until a kid has two right answers in a row and sees it on their meter");
			fights.endAll();
		});
	}

	/**
	 * Playtest 2: a kid beat him and got nothing. The reward came after he had flown off into the
	 * sky, and by then nobody counted as near him any more.
	 */
	@GameTest(maxTicks = 6000, padding = 48)
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	public void beatingGraafFoutGivesTheKidFourKippenbommen(GameTestHelper helper) {
		OpenField.open(helper);
		RecordingQuizClient client = new RecordingQuizClient();
		SessionManager sessions = new SessionManager(helper.getLevel().getServer(), new Learners(new AnswerLog(tempDir())), client);
		BossFights fights = new BossFights(sessions);

		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		player.teleportTo(stand.x, stand.y, stand.z);
		helper.assertTrue(fights.summon(helper.getLevel(), stand.add(6, 1.5, 0)), "Graaf Fout should appear");
		BossFight fight = fights.active().getFirst();

		Set<Integer> answered = new HashSet<>();
		helper.onEachTick(() -> {
			sessions.tick();
			fights.tick();
			for (RecordingQuizClient.Sent sent : List.copyOf(client.sentTo)) {
				QuizS2C question = sent.payload();
				if (question.mode() == QuizS2C.ASK && answered.add(question.sessionId())) {
					sessions.onClientAction(sent.player(), new QuizC2S(question.sessionId(), QuizC2S.ANSWER, question.a() * question.b(), QuizC2S.VIA_KEY));
				}
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(fight.isOver(), "wait until Graaf Fout is beaten and gone");
			int loot = player.getInventory().countItem(ModBlocks.bombItem(BombTier.KIPPENBOM));
			helper.assertTrue(loot == 4, "after " + answered.size() + " answers he was beaten, but the kid got " + loot
					+ " Kippenbommen instead of 4 (he was " + Math.round(player.distanceTo(fight.boss())) + " blocks away)");
		});
	}

	/**
	 * Playtest 2 (P13): the strongest yeets land him 58 to 62 blocks from the kids, further than the
	 * boss bar reaches, and he used to hover out there for good. Here he starts out on a pad that far
	 * away; he has to come back to the kid and throw.
	 */
	@GameTest(maxTicks = 400, padding = 72)
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	public void afterAYeetThatLandsHimFarAwayHeComesBackAndThrowsAgain(GameTestHelper helper) {
		OpenField.open(helper);
		ServerLevel level = helper.getLevel();
		RecordingQuizClient client = new RecordingQuizClient();
		SessionManager sessions = new SessionManager(level.getServer(), new Learners(new AnswerLog(tempDir())), client);
		BossFights fights = new BossFights(sessions);

		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		player.teleportTo(stand.x, stand.y, stand.z);
		helper.assertTrue(fights.summon(level, stand.add(6, 1.5, 0)), "Graaf Fout should appear");
		BossFight fight = fights.active().getFirst();

		// Past OpenField's floor the test world is void, so he needs a pad of his own to hover over.
		BlockPos pad = helper.absolutePos(new BlockPos(1 + FAR_AWAY, -1, 1));
		for (BlockPos pos : BlockPos.betweenClosed(pad.offset(-1, 0, -1), pad.offset(1, 0, 1))) {
			level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
		}
		helper.assertTrue(level.getHeight(Heightmap.Types.MOTION_BLOCKING, pad.getX(), pad.getZ())
				== level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(stand.x), Mth.floor(stand.z)),
				"the pad lies at the height of the floor");
		// Same trick as OpenField: only the test's own chunks are force-loaded.
		helper.onEachTick(() -> level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, ChunkPos.containing(pad), 5));

		// Before his first throw: where a big yeet would have left him, out of the boss bar's reach (48 blocks).
		fight.boss().teleportTo(pad.getX() + 0.5, pad.getY() + 1 + 1.5, pad.getZ() + 0.5);
		helper.assertTrue(fight.boss().distanceTo(player) > 48 && fight.boss().distanceTo(player) < 96,
				"he starts out of reach but not too far: " + Math.round(fight.boss().distanceTo(player)) + " blocks");

		Set<Integer> answered = new HashSet<>();
		helper.onEachTick(() -> {
			sessions.tick();
			fights.tick();
			for (RecordingQuizClient.Sent sent : List.copyOf(client.sentTo)) {
				QuizS2C question = sent.payload();
				if (question.mode() == QuizS2C.ASK && answered.add(question.sessionId())) {
					if (answered.size() == 1) {
						helper.assertTrue(fight.boss().distanceToSqr(Vec3.atCenterOf(pad)) > 30 * 30,
								"he asked the first question but is still over the pad, " + Math.round(fight.boss().distanceTo(player)) + " blocks from the kid");
					}
					sessions.onClientAction(sent.player(), new QuizC2S(question.sessionId(), QuizC2S.ANSWER, question.a() * question.b(), QuizC2S.VIA_KEY));
				}
			}
		});

		helper.succeedWhen(() -> {
			helper.assertFalse(answered.isEmpty(), "wait until the kid is asked a question; Graaf Fout hovers "
					+ Math.round(fight.boss().distanceTo(player)) + " blocks away");
			fights.endAll();
		});
	}

	@GameTest(maxTicks = 1200, padding = 48)
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	public void onTheHardLevelHeThrowsOnlyKippenbommenAndCountsEveryAnswer(GameTestHelper helper) {
		OpenField.open(helper);
		RecordingQuizClient client = new RecordingQuizClient();
		SessionManager sessions = new SessionManager(helper.getLevel().getServer(), new Learners(new AnswerLog(tempDir())), client);
		BossFights fights = new BossFights(sessions);

		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		player.teleportTo(stand.x, stand.y, stand.z);
		BossFight fight = fights.summon(helper.getLevel(), stand.add(6, 1.5, 0), FightLevel.MOEILIJK, null);
		helper.assertTrue(fight != null, "Graaf Fout should appear");
		helper.assertTrue(fight.hitsRemaining() == 13, "level 3 needs 13 hits, not " + fight.hitsRemaining());

		Set<Integer> answered = new HashSet<>();
		int[] chickensAfterWrong = {-1};
		int[] mostChickens = {0};
		int[] firstRightProduct = {-1};
		helper.onEachTick(() -> {
			sessions.tick();
			fights.tick();
			for (RecordingQuizClient.Sent sent : List.copyOf(client.sentTo)) {
				QuizS2C question = sent.payload();
				if (question.mode() == QuizS2C.ASK && answered.add(question.sessionId())) {
					helper.assertTrue(question.tier() == BombTier.KIPPENBOM.tier(), "only Kippenbommen on level 3, got tier " + question.tier());
					// The first answer is wrong, the rest right.
					int product = question.a() * question.b();
					int answer = answered.size() == 1 ? product + 1 : product;
					sessions.onClientAction(sent.player(), new QuizC2S(question.sessionId(), QuizC2S.ANSWER, answer, QuizC2S.VIA_KEY));
					if (answered.size() == 1) {
						chickensAfterWrong[0] = spectacleChickens(helper, player.position(), 3);
					}
					if (answered.size() == 2) {
						firstRightProduct[0] = product;
					}
				}
			}
			mostChickens[0] = Math.max(mostChickens[0], spectacleChickens(helper, stand, 40));
		});

		helper.succeedWhen(() -> {
			FightTally tally = fight.tally(player.getUUID());
			helper.assertTrue(tally.answers() >= 3, "wait for three answers");
			helper.assertTrue(tally.wrong() == 1, "one wrong answer counted, not " + tally.wrong());
			helper.assertTrue(chickensAfterWrong[0] == ChickenCount.WRONG_ANSWER,
					"a wrong answer on a Kippenbom lets 1 chicken hop out, not " + chickensAfterWrong[0]);
			helper.assertTrue(firstRightProduct[0] > 0 && mostChickens[0] >= firstRightProduct[0],
					"a Kippenbom kicked into him counts out " + firstRightProduct[0] + " chickens; at most " + mostChickens[0] + " seen");
			fights.endAll();
		});
	}

	/**
	 * On Moeilijk a typed wrong answer costs 2 hearts, but never the last half heart, and "?" costs
	 * nothing, so "?" stays better than guessing.
	 */
	@GameTest(maxTicks = 1600, padding = 48)
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	public void onTheHardLevelAWrongAnswerCostsTwoHeartsButNeverTheLast(GameTestHelper helper) {
		Survival kid = answerInSurvival(helper, FightLevel.MOEILIJK, (player, count) -> {
			if (count == 3) {
				player.setHealth(1F);
			}
			return count == 2 ? QuizC2S.DONT_KNOW : QuizC2S.ANSWER;
		});
		List<float[]> health = kid.health();
		helper.succeedWhen(() -> {
			helper.assertTrue(health.size() >= 3, "wait for three answers");
			helper.assertTrue(health.get(0)[0] - health.get(0)[1] == 4F, "a wrong answer costs 2 hearts: " + health.get(0)[0] + " → " + health.get(0)[1]);
			helper.assertTrue(health.get(1)[0] == health.get(1)[1], "\"?\" costs nothing: " + health.get(1)[0] + " → " + health.get(1)[1]);
			helper.assertTrue(health.get(2)[1] == 1F, "never the last half heart, but " + health.get(2)[1] + " left");
			kid.fights().endAll();
		});
	}

	@GameTest(maxTicks = 600, padding = 48)
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	public void onTheEasyLevelAWrongAnswerCostsNoHearts(GameTestHelper helper) {
		Survival kid = answerInSurvival(helper, FightLevel.MAKKELIJK, (player, count) -> QuizC2S.ANSWER);
		List<float[]> health = kid.health();
		helper.succeedWhen(() -> {
			helper.assertTrue(!health.isEmpty(), "wait for a wrong answer");
			helper.assertTrue(health.get(0)[0] == health.get(0)[1], "no hearts lost on Makkelijk: " + health.get(0)[0] + " → " + health.get(0)[1]);
			kid.fights().endAll();
		});
	}

	/** What a kid does with their next question: {@link QuizC2S#ANSWER} answers it wrong. */
	private interface NextAnswer {
		int act(ServerPlayer player, int count);
	}

	/** The health before and after each answer, and the fight to end. */
	private record Survival(List<float[]> health, BossFights fights) {
	}

	/** A survival kid in a fight at this level, answering as told. */
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	private static Survival answerInSurvival(GameTestHelper helper, FightLevel level, NextAnswer next) {
		OpenField.open(helper);
		RecordingQuizClient client = new RecordingQuizClient();
		SessionManager sessions = new SessionManager(helper.getLevel().getServer(), new Learners(new AnswerLog(tempDir())), client);
		BossFights fights = new BossFights(sessions);

		// makeMockServerPlayer(SURVIVAL) has no connection, and rooting a kid moves them through it.
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		player.teleportTo(stand.x, stand.y, stand.z);
		helper.assertTrue(fights.summon(helper.getLevel(), stand.add(6, 1.5, 0), level, null) != null, "Graaf Fout should appear");

		List<float[]> health = new ArrayList<>();
		Set<Integer> answered = new HashSet<>();
		helper.onEachTick(() -> {
			sessions.tick();
			fights.tick();
			for (RecordingQuizClient.Sent sent : List.copyOf(client.sentTo)) {
				QuizS2C question = sent.payload();
				if (question.mode() == QuizS2C.ASK && answered.add(question.sessionId())) {
					int action = next.act(player, answered.size());
					float before = player.getHealth();
					int value = action == QuizC2S.ANSWER ? question.a() * question.b() + 1 : 0;
					sessions.onClientAction(sent.player(), new QuizC2S(question.sessionId(), action, value, QuizC2S.VIA_KEY));
					health.add(new float[] {before, player.getHealth()});
				}
			}
		});
		return new Survival(health, fights);
	}

	private static int spectacleChickens(GameTestHelper helper, Vec3 around, double radius) {
		return helper.getLevel().getEntitiesOfClass(Chicken.class, new AABB(around, around).inflate(radius),
				chicken -> chicken.entityTags().contains(BombEffects.SPECTACLE_TAG)).size();
	}

	private static boolean isYeetOMeter(Component message) {
		return message.getContents() instanceof TranslatableContents translatable
				&& translatable.getKey().startsWith("tafelboem.message.yeet_");
	}

	private static java.nio.file.Path tempDir() {
		try {
			return Files.createTempDirectory("tafelboem-gametest");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
