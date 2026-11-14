package nl.jeeninga.tafelboem.gametest;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.boss.YeetRecords;
import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.FightTally;
import nl.jeeninga.tafelboem.core.YeetStyle;
import nl.jeeninga.tafelboem.core.YeetKind;
import nl.jeeninga.tafelboem.core.YeetReport;
import nl.jeeninga.tafelboem.net.QuizS2C;

/**
 * R1.4's throw-back: right-click throws a caught bomb at him, once a second, through kids and chickens; it homes in
 * on him; a hit staggers him and asks the Terug! question; he skips a throw but never two in a row; a right answer
 * is a hit and a throw-back yeet, a wrong one fizzles; and in every state he can't be hit in, it drops back.
 */
public class ThrowBackGameTest {
	@GameTest(maxTicks = 80, padding = 48)
	public void aThrowBackHitsHimStaggersHimAndAsksTheTerugQuestion(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		helper.runAfterDelay(2, () -> {
			ArenaFight.giveInHand(arena.kid, BombTier.GEWONE_TNT);
			ArenaFight.aimAt(arena.kid, arena.hisMiddle());
			helper.assertTrue(arena.fight.throwBack(arena.kid, InteractionHand.MAIN_HAND), "the throw flies");
			helper.assertTrue(arena.kid.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "and the bomb is used up");
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).backHits() == 1, "wait for it to reach him");
			helper.assertTrue(arena.fight.boss().isStaggered(), "he wobbles");
			helper.assertTrue(arena.asked(arena.kid, BombTier.GEWONE_TNT), "the thrower is asked a question on the caught bomb's tier");
			helper.assertTrue(arena.sessions.isBusy(arena.kid), "rooted, as after a hit");
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).thrownBack() == 1, "counted as thrown back");
			arena.end();
		});
	}

	@GameTest(maxTicks = 80, padding = 48)
	public void aKidThrowsBackAtMostOnceASecond(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		// 9 blocks from him, throwing straight east: out of his homing range, so nothing hits him and the kid is never asked.
		arena.kid.teleportTo(arena.box.middleX(), arena.floorY, arena.box.middleZ() + 9);
		ArenaFight.giveInHand(arena.kid, BombTier.KNALLETJE);
		ArenaFight.aimAt(arena.kid, arena.kid.getEyePosition().add(30, 0, 0));
		helper.assertTrue(arena.fight.throwBack(arena.kid, InteractionHand.MAIN_HAND), "the first throw flies");

		ArenaFight.giveInHand(arena.kid, BombTier.KNALLETJE);
		helper.assertFalse(arena.fight.throwBack(arena.kid, InteractionHand.MAIN_HAND), "a second in the same tick doesn't");
		helper.runAfterDelay(19, () -> helper.assertFalse(arena.fight.throwBack(arena.kid, InteractionHand.MAIN_HAND), "nor one tick short of a second"));
		helper.runAfterDelay(21, () -> {
			helper.assertTrue(arena.fight.throwBack(arena.kid, InteractionHand.MAIN_HAND), "a second later it does");
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 80, padding = 48)
	public void itPassesThroughAKidAndAChickenOnTheWay(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		ServerPlayer sibling = arena.addKid(arena.box.middleX(), arena.box.middleZ() + 2.5);
		// The throw climbs from the kid's eyes to his middle: 1.5 blocks out it is about 1.8 above the floor. A chicken
		// on the floor is lower than that, so this one floats in the line, at the bomb's height.
		Chicken chicken = EntityTypes.CHICKEN.create(arena.level, EntitySpawnReason.TRIGGERED);
		chicken.setPos(arena.box.middleX(), arena.floorY + 1.4, arena.box.middleZ() + 3.5);
		chicken.setNoAi(true);
		chicken.setNoGravity(true);
		arena.level.addFreshEntity(chicken);
		helper.runAfterDelay(2, () -> {
			ArenaFight.giveInHand(arena.kid, BombTier.KNALLETJE);
			ArenaFight.aimAt(arena.kid, arena.hisMiddle());
			arena.fight.throwBack(arena.kid, InteractionHand.MAIN_HAND);
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).backHits() == 1, "wait for it to reach him, past the sibling and the chicken");
			helper.assertFalse(arena.asked(sibling, BombTier.KNALLETJE), "the sibling was not hit");
			helper.assertTrue(chicken.isAlive(), "and the chicken is fine");
			helper.assertTrue(chicken.getY() > arena.floorY + 1, "and still in the line, in the air: " + chicken.getY());
			arena.end();
		});
	}

	/**
	 * The spec asks for 5 blocks off-aim, but a straight throw 5 blocks beside him never comes within the homing range
	 * (5 blocks) of his middle, so nothing bends it in; it catches throws up to just under 5 blocks off (4.9 in
	 * {@code HomingTest}'s model of the flight). This checks 4, with room to spare. R1.4.md's watch list says so.
	 */
	@GameTest(maxTicks = 80, padding = 48)
	public void homingHitsAStandingGraafFoutFromFourBlocksOffAim(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		// 9 blocks from him, aiming 4 blocks beside him: the straight flight misses by 4, the homing range is 5.
		arena.kid.teleportTo(arena.box.middleX(), arena.floorY, arena.box.middleZ() + 9);
		helper.runAfterDelay(2, () -> {
			ArenaFight.giveInHand(arena.kid, BombTier.KNALLETJE);
			ArenaFight.aimAt(arena.kid, arena.hisMiddle().add(4, 0, 0));
			arena.fight.throwBack(arena.kid, InteractionHand.MAIN_HAND);
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).backHits() == 1, "it bent in on him and hit");
			arena.end();
		});
	}

	@GameTest(maxTicks = 120, padding = 48)
	@Fight
	public void aThrowBackThatMissesPoofsAndAsksNothing(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		helper.runAfterDelay(2, () -> {
			ArenaFight.giveInHand(arena.kid, BombTier.KNALLETJE);
			// Straight east, level, far from him: nothing to home in on.
			ArenaFight.aimAt(arena.kid, arena.kid.getEyePosition().add(30, 0, 0));
			arena.fight.throwBack(arena.kid, InteractionHand.MAIN_HAND);
		});

		helper.runAfterDelay(4, () -> helper.assertTrue(arena.thrownBackInTheAir(arena.kid).size() == 1, "the throw flies"));
		helper.runAfterDelay(58, () -> {
			helper.assertTrue(arena.thrownBackInTheAir(arena.kid).isEmpty(), "it poofed: nothing flies any more");
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).backHits() == 0, "no hit");
			helper.assertFalse(arena.askedAnything(), "no question");
			helper.assertTrue(arena.terugbommen().isEmpty(), "and it doesn't drop back: a miss is gone");
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 300, padding = 48)
	@Fight
	public void aHitSkipsHisNextThrowButNotTwoInARow(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		Set<Integer> answered = new HashSet<>();
		long[] firstBombAt = {-1};
		helper.onEachTick(() -> {
			// Every question is answered wrong at once, so the kid is free again and nothing yeets him.
			for (QuizS2C question : arena.questionsTo(arena.kid)) {
				if (answered.add(question.sessionId())) {
					arena.answerWrong(arena.kid, question);
				}
			}
			if (firstBombAt[0] < 0 && !arena.bombsInTheAir().isEmpty()) {
				firstBombAt[0] = helper.getTick();
			}
		});
		// Two hits, the second just before the throw he would then make: it must not skip a second one.
		for (int tick : new int[] {2, 90}) {
			helper.runAfterDelay(tick, () -> {
				ArenaFight.giveInHand(arena.kid, BombTier.KNALLETJE);
				ArenaFight.aimAt(arena.kid, arena.hisMiddle());
				arena.fight.throwBack(arena.kid, InteractionHand.MAIN_HAND);
			});
		}

		helper.succeedWhen(() -> {
			helper.assertTrue(firstBombAt[0] >= 0, "wait for his first throw");
			// Without the stagger it comes at tick 60; the hit skips it, the pause after it is 50 ticks.
			helper.assertTrue(firstBombAt[0] >= 100, "the first throw was skipped: it came at tick " + firstBombAt[0]);
			helper.assertTrue(firstBombAt[0] <= 135, "but not a second one in a row: it came at tick " + firstBombAt[0]);
			arena.end();
		});
	}

	@GameTest(maxTicks = 400, padding = 48)
	@Fight
	public void aRightTerugAnswerIsAHitAndAThrowBackYeet(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		int startHits = arena.fight.hitsRemaining();
		Set<Integer> answered = new HashSet<>();
		helper.runAfterDelay(2, () -> {
			ArenaFight.giveInHand(arena.kid, BombTier.KNALLETJE);
			ArenaFight.aimAt(arena.kid, arena.hisMiddle());
			arena.fight.throwBack(arena.kid, InteractionHand.MAIN_HAND);
		});
		helper.onEachTick(() -> {
			for (QuizS2C question : arena.questionsTo(arena.kid)) {
				if (arena.fight.tally(arena.kid.getUUID()).backHits() == 1 && answered.add(question.sessionId())) {
					arena.answerRight(arena.kid, question);
				}
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(arena.fight.lastYeet() != null, "wait for his flight to end");
			helper.assertTrue(arena.fight.hitsRemaining() == startHits - 1, "one hit: " + arena.fight.hitsRemaining() + " left of " + startHits);
			helper.assertTrue(arena.fight.lastYeet().factor() >= 1.25, "a throw-back yeet, x1.25 at least: " + arena.fight.lastYeet().describe());
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).terugRight() == 1, "counted as a right Terug! answer");
			helper.assertTrue(arena.fight.creditedKid().equals(arena.kid.getUUID()), "credited to the kid who threw it");
			arena.end();
		});
	}

	@GameTest(maxTicks = 100, padding = 48)
	public void aWrongTerugAnswerFizzlesCostsHeartsOnLevel2AndHitsNothing(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig(), FightLevel.GEMIDDELD, true);
		ArenaFight.survival(arena.kid);
		int startHits = arena.fight.hitsRemaining();
		helper.runAfterDelay(2, () -> {
			ArenaFight.giveInHand(arena.kid, BombTier.KNALLETJE);
			ArenaFight.aimAt(arena.kid, arena.hisMiddle());
			arena.fight.throwBack(arena.kid, InteractionHand.MAIN_HAND);
		});
		boolean[] answered = {false};
		helper.onEachTick(() -> {
			if (!answered[0] && arena.fight.tally(arena.kid.getUUID()).backHits() == 1 && arena.sessions.isBusy(arena.kid)) {
				answered[0] = true;
				arena.answerWrong(arena.kid, arena.questionsTo(arena.kid).getFirst());
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(answered[0] && !arena.sessions.isBusy(arena.kid), "wait for the wrong answer");
			helper.assertTrue(arena.kid.getHealth() == 18F, "a typed wrong answer costs a heart on level 2: " + arena.kid.getHealth());
			helper.assertTrue(arena.fight.hitsRemaining() == startHits, "no hit");
			helper.assertFalse(arena.fight.boss().isReacting(), "he is not yeeted");
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).terugWrong() == 1, "counted as a wrong Terug! answer");
			arena.end();
		});
	}

	@GameTest(maxTicks = 40, padding = 48)
	public void aThrowBackBouncesOffAStaggeredHim(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		ServerPlayer sibling = arena.addKid(arena.box.middleX() + 6, arena.box.middleZ() + 6);
		helper.runAfterDelay(2, () -> {
			// The first throw-back hits and staggers him; the sibling, free and not answering, throws right after it.
			arena.fight.backReached(arena.kid.getUUID(), BombTier.KNALLETJE);
			helper.assertTrue(arena.fight.boss().isStaggered(), "the first hit staggers him");
			arena.fight.backReached(sibling.getUUID(), BombTier.KNALLETJE);
			helper.assertTrue(arena.terugbommen().size() == 1, "the sibling's bounced off and dropped below him");
			helper.assertFalse(arena.sessions.isBusy(sibling), "and asks nothing");
			helper.assertTrue(arena.fight.tally(sibling.getUUID()).backHits() == 0, "and is no hit");
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40, padding = 48)
	public void aThrowBackOfAKidWhoIsAnsweringBouncesOff(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		helper.runAfterDelay(2, () -> {
			// Neither staggered nor beaten: only the thrower is busy, with a bomb that landed on them while it flew.
			arena.sessions.start(arena.kid, BombTier.GEWONE_TNT, arena.kid.blockPosition().east(), "test", null);
			arena.fight.backReached(arena.kid.getUUID(), BombTier.KNALLETJE);
			helper.assertTrue(arena.terugbommen().size() == 1, "it bounced off and dropped below him");
			helper.assertFalse(arena.fight.boss().isStaggered(), "no stagger");
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).backHits() == 0, "and no hit");
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40, padding = 48)
	public void aThrowBackInHisFinaleBouncesOff(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		helper.runAfterDelay(2, () -> {
			// Not staggered, the thrower free: only his finale yeet.
			arena.fight.boss().finalYeet();
			arena.fight.backReached(arena.kid.getUUID(), BombTier.KIPPENBOM);
			helper.assertTrue(arena.terugbommen().size() == 1, "in his finale it bounces off");
			helper.assertFalse(arena.sessions.isBusy(arena.kid), "and asks nothing");
			helper.assertFalse(arena.fight.boss().isStaggered(), "no stagger");
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 6000, padding = 48)
	@Fight
	public void aThrowBackThatReachesHimAfterTheLastHitBouncesOff(GameTestHelper helper) {
		// The test's floor is one block of stone: eleven kicks' craters would leave no floor to drop it on (an arena has bedrock).
		TafelBoemConfig config = new TafelBoemConfig();
		config.arenaCraters = false;
		ArenaFight arena = new ArenaFight(helper, config);
		Set<Integer> answered = new HashSet<>();
		boolean[] reached = {false};
		helper.onEachTick(() -> {
			if (arena.fight.isWon()) {
				if (!reached[0]) {
					// Beaten: a throw-back in the air arrives now.
					reached[0] = true;
					arena.fight.backReached(arena.kid.getUUID(), BombTier.KNALLETJE);
					helper.assertTrue(arena.terugbommen().size() == 1, "it bounced off: " + arena.terugbommen().size());
					helper.assertFalse(arena.sessions.isBusy(arena.kid), "and asks nothing");
					helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).backHits() == 0, "and is no hit");
				}
				return;
			}
			// Until then the kid stands still and answers every bomb right.
			for (QuizS2C question : arena.questionsTo(arena.kid)) {
				if (answered.add(question.sessionId())) {
					arena.answerRight(arena.kid, question);
				}
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(reached[0], "wait until he is beaten");
			helper.assertTrue(arena.fight.isOver(), "and his finale ends as usual");
		});
	}

	@GameTest(maxTicks = 40, padding = 48)
	public void aBouncedTerugbomOverAWallLandsOnTheArenaFloorWhereAKidCanReachIt(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		// A 4-high obsidian wall along the arena's east edge, and him right above it.
		for (int z = arena.box.minZ(); z <= arena.box.maxZ(); z++) {
			for (int y = arena.floorY; y < arena.floorY + 4; y++) {
				arena.level.setBlockAndUpdate(new BlockPos(arena.box.maxX(), y, z), Blocks.OBSIDIAN.defaultBlockState());
			}
		}
		helper.runAfterDelay(2, () -> {
			arena.fight.boss().teleportTo(arena.box.maxX() + 0.5, arena.floorY + 5, arena.box.middleZ());
			// The first throw-back staggers him; the second, while he wobbles, bounces off.
			arena.fight.backReached(arena.kid.getUUID(), BombTier.KNALLETJE);
			arena.fight.backReached(arena.kid.getUUID(), BombTier.KNALLETJE);
		});
		helper.runAfterDelay(6, () -> {
			helper.assertTrue(arena.terugbommen().size() == 1, "it bounced off: " + arena.terugbommen().size());
			ItemEntity bomb = arena.terugbommen().getFirst();
			helper.assertTrue(bomb.getY() < arena.floorY + 1, "on the floor, not on top of the wall: y " + bomb.getY() + ", floor " + arena.floorY);
			helper.assertTrue(bomb.getX() <= arena.box.maxX() - 1, "a block in from the wall: x " + bomb.getX() + ", wall " + arena.box.maxX());
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 300, padding = 48)
	@Fight
	public void aThrowBackWhileHeLandsBouncesOff(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		// A short yeet on cue; the throw-back arrives once he is down and still reacting (his landing).
		helper.runAfterDelay(2, () -> arena.fight.boss().yeet(arena.kid.position(), 0.3, YeetStyle.CLASSIC, arena.kid.position()));
		boolean[] reached = {false};
		helper.onEachTick(() -> {
			if (!reached[0] && arena.fight.boss().landingStyle() != null) {
				reached[0] = true;
				arena.fight.backReached(arena.kid.getUUID(), BombTier.KNALLETJE);
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(reached[0], "wait until he is down");
			helper.assertTrue(arena.terugbommen().size() == 1, "it bounced off him and dropped below him");
			helper.assertFalse(arena.sessions.isBusy(arena.kid), "and asked nothing");
			helper.assertFalse(arena.fight.boss().isStaggered(), "he is not staggered");
			arena.end();
		});
	}

	@GameTest(maxTicks = 6000, padding = 48)
	@Fight
	public void aRightTerugAnswerThatIsTheLastHitWinsTheFight(GameTestHelper helper) {
		lastHitFromAThrowBack(helper, false);
	}

	/**
	 * A fight played to its last hit, the kid standing still and answering every bomb right; then a throw-back that
	 * reaches him, standing (a Terug! question) or in mid-yeet (with {@code juggle}, a juggle question), answered right,
	 * is that last hit: he is beaten, the finale ends and the kid gets the reward.
	 */
	static void lastHitFromAThrowBack(GameTestHelper helper, boolean juggle) {
		// The test's floor is one block of stone: ten kicks' craters could leave him nothing to stand on.
		TafelBoemConfig config = new TafelBoemConfig();
		config.arenaCraters = false;
		ArenaFight arena = new ArenaFight(helper, config);
		Set<Integer> answered = new HashSet<>();
		int[] stage = {0};
		long[] yeetedAt = {-1};
		helper.onEachTick(() -> {
			GraafFout boss = arena.fight.boss();
			if (stage[0] == 0 && arena.fight.hitsRemaining() == 1 && !boss.isReacting() && !arena.sessions.isBusy(arena.kid)) {
				if (juggle) {
					boss.yeet(arena.kid.position(), 1.0, YeetStyle.HELICOPTER, arena.kid.position());
					yeetedAt[0] = helper.getTick();
					stage[0] = 1;
				} else {
					arena.fight.backReached(arena.kid.getUUID(), BombTier.KNALLETJE);
					stage[0] = 2;
				}
			} else if (stage[0] == 1 && helper.getTick() - yeetedAt[0] >= 6) {
				arena.fight.backReached(arena.kid.getUUID(), BombTier.KNALLETJE);
				helper.assertTrue(boss.isFrozen(), "the throw-back juggled him");
				stage[0] = 2;
			}
			// Every question is answered right at once: the bombs until the last hit, then the Terug! or juggle question.
			for (QuizS2C question : arena.questionsTo(arena.kid)) {
				if (answered.add(question.sessionId())) {
					arena.answerRight(arena.kid, question);
				}
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(stage[0] == 2, "wait for the last hit to come from a throw-back");
			helper.assertTrue(arena.fight.isOver() && arena.fight.isWon(), "wait for the win and the end of his finale");
			FightTally tally = arena.fight.tally(arena.kid.getUUID());
			helper.assertTrue(tally.terugRight() == 1 && tally.juggles() == (juggle ? 1 : 0), "the last hit was a right " + (juggle ? "juggle" : "Terug!")
					+ " answer: " + tally.terugRight() + " right, " + tally.juggles() + " juggles");
			helper.assertTrue(arena.kid.getInventory().countItem(Items.COOKED_CHICKEN) == 4, "the win is rewarded");
		});
	}

	@GameTest(maxTicks = 40, padding = 48)
	public void withDodgingOffATerugbomDoesNothing(GameTestHelper helper) {
		TafelBoemConfig config = new TafelBoemConfig();
		config.dodging = false;
		ArenaFight arena = new ArenaFight(helper, config);
		ArenaFight.giveInHand(arena.kid, BombTier.KNALLETJE);
		ArenaFight.aimAt(arena.kid, arena.hisMiddle());

		helper.assertFalse(arena.fight.throwBack(arena.kid, InteractionHand.MAIN_HAND), "1.2.0 has no throw-back");
		helper.assertTrue(arena.bombsInTheAir().isEmpty(), "nothing flies");
		arena.end();
		helper.succeed();
	}

	@GameTest(maxTicks = 80, padding = 48)
	public void aFightThatEndsWithATerugQuestionOpenClosesIt(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		helper.runAfterDelay(2, () -> arena.fight.backReached(arena.kid.getUUID(), BombTier.KNALLETJE));
		helper.runAfterDelay(6, () -> {
			helper.assertTrue(arena.sessions.isBusy(arena.kid), "the Terug! question is open");
			arena.end();
			helper.assertFalse(arena.sessions.isBusy(arena.kid), "and closes with the fight");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 400, padding = 48)
	@Fight
	public void aThrowBackYeetHasItsOwnRecordAndIsMarkedOnTheYeetLine(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		Map<YeetKind, Integer> bests = new EnumMap<>(YeetKind.class);
		// A huge kick record that no throw-back may touch, and a small throw-back best to beat.
		bests.put(YeetKind.KICK, 999);
		bests.put(YeetKind.BACK, 1);
		List<YeetKind> beaten = new ArrayList<>();
		arena.fight.useYeetRecords(new YeetRecords() {
			@Override
			public int best(ServerPlayer kid, YeetKind kind) {
				return bests.getOrDefault(kind, 0);
			}

			@Override
			public void beaten(ServerPlayer kid, YeetKind kind, int blocks) {
				bests.put(kind, blocks);
				beaten.add(kind);
			}
		});
		Set<Integer> answered = new HashSet<>();
		helper.runAfterDelay(2, () -> {
			ArenaFight.giveInHand(arena.kid, BombTier.KNALLETJE);
			ArenaFight.aimAt(arena.kid, arena.hisMiddle());
			arena.fight.throwBack(arena.kid, InteractionHand.MAIN_HAND);
		});
		helper.onEachTick(() -> {
			for (QuizS2C question : arena.questionsTo(arena.kid)) {
				if (arena.fight.tally(arena.kid.getUUID()).backHits() == 1 && answered.add(question.sessionId())) {
					arena.answerRight(arena.kid, question);
				}
			}
		});

		helper.succeedWhen(() -> {
			YeetReport report = arena.fight.lastYeet();
			helper.assertTrue(report != null, "wait for his flight to end");
			helper.assertTrue(report.kind() == YeetKind.BACK, "the yeet line marks a throw-back: " + report.describe());
			helper.assertTrue(report.describe().contains(", back"), "in its text: " + report.describe());
			helper.assertTrue(bests.get(YeetKind.KICK) == 999 && !beaten.contains(YeetKind.KICK), "a throw-back never touches the kick record");
			if (report.high()) {
				// A helicopter says how high he flew, and a height is no record.
				helper.assertFalse(report.record(), "a height is never a record");
			} else {
				helper.assertTrue(report.record() && beaten.contains(YeetKind.BACK), "it beat the throw-back best of 1 block: " + report.describe());
				helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).farthestBack() == report.blocks(), "the fights log gets it as the farthest throw-back");
				helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).farthestYeet() == 0, "and the farthest kick stays 0");
				helper.assertTrue(arena.client.actionBarsTo.stream().anyMatch(bar -> bar.player() == arena.kid
						&& bar.message().getContents() instanceof TranslatableContents says && says.getKey().equals("tafelboem.message.yeet_record_back")),
						"the kid hears about their throw-back record");
			}
			arena.end();
		});
	}
}
