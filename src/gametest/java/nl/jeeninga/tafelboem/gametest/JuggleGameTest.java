package nl.jeeninga.tafelboem.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.AnswerRecord;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.FightTally;
import nl.jeeninga.tafelboem.core.Pacing;
import nl.jeeninga.tafelboem.core.YeetKind;
import nl.jeeninga.tafelboem.core.YeetReport;
import nl.jeeninga.tafelboem.core.YeetStyle;
import nl.jeeninga.tafelboem.net.QuizS2C;
import nl.jeeninga.tafelboem.world.ArenaRun;

/**
 * R1.4's juggle: a throw-back that hits him mid-yeet freezes him in the air with the Terug! question open, however
 * long the kid thinks. Right: a hit and a super yeet. Wrong, a timeout or a disconnect: he falls the normal way.
 * One juggle per yeet, never on a super yeet, and the first of two kids' throw-backs counts.
 */
public class JuggleGameTest {
	/** The floor of a level 3 arena ends 22.5 blocks from its middle; he must land a block short of that at least. */
	private static final double LANDS_WITHIN = 21.5;

	@GameTest(maxTicks = 100, padding = 48)
	public void aThrowBackMidYeetFreezesHimAndAsksTheJuggleQuestion(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		startJuggle(helper, arena, BombTier.GEWONE_TNT);

		helper.succeedWhen(() -> {
			helper.assertTrue(arena.fight.boss().isFrozen(), "wait for the hit: he hangs in the air");
			helper.assertTrue(arena.asked(arena.kid, BombTier.GEWONE_TNT) && arena.sessions.isBusy(arena.kid), "the thrower is asked, rooted");
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).juggles() == 1 && arena.fight.tally(arena.kid.getUUID()).backHits() == 1,
					"counted as a juggle and a hit that reached him");
			helper.assertFalse(arena.fight.boss().isStaggered(), "flying, he doesn't stagger");
			arena.end();
		});
	}

	@GameTest(maxTicks = 500, padding = 48)
	@Fight
	public void heStaysFrozenPast300TicksWhileTheQuestionIsOpen(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		startJuggle(helper, arena, BombTier.KNALLETJE);
		long[] frozenAt = {-1};
		Vec3[] hangingAt = {null};
		helper.onEachTick(() -> {
			if (frozenAt[0] < 0 && arena.fight.boss().isFrozen()) {
				frozenAt[0] = helper.getTick();
				hangingAt[0] = arena.fight.boss().position();
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(frozenAt[0] >= 0, "wait for the freeze");
			helper.assertTrue(helper.getTick() - frozenAt[0] >= Pacing.STUCK_TICKS + 20, "wait past the stall net's 300 ticks");
			helper.assertTrue(arena.fight.boss().isFrozen(), "he still hangs there");
			helper.assertTrue(arena.fight.boss().position().distanceTo(hangingAt[0]) < 0.05, "in the same place: the stall net didn't bring him back");
			helper.assertTrue(arena.sessions.isBusy(arena.kid), "the question is still open, and no timer ran out");
			helper.assertTrue(arena.bombsInTheAir().isEmpty(), "he threw nothing: no fight timer runs");
			arena.end();
		});
	}

	@GameTest(maxTicks = 600, padding = TestWorld.PADDING)
	@Fight
	public void aRightJuggleAnswerIsAHitAndASuperYeetThatLandsInsideTheArenaOnLevel3(GameTestHelper helper) {
		superYeetOnLevel3(helper, 0);
	}

	/**
	 * The juggle where it is hardest to stay in: he hangs about 15 blocks out from the middle of a level 3 arena (its
	 * floor ends 22.5 out), and the kid stands behind him, between him and the middle, so the super yeet takes him
	 * further out.
	 */
	@GameTest(maxTicks = 600, padding = TestWorld.PADDING)
	@Fight
	public void aSuperYeetFrom15BlocksOutWithTheKidBehindHimLandsInsideTheArenaOnLevel3(GameTestHelper helper) {
		superYeetOnLevel3(helper, 16.85);
	}

	/**
	 * A juggle in a level 3 arena: he starts {@code out} blocks south of the middle, the kid 6 blocks north of him, and a
	 * helicopter yeet takes him up and about 1.5 blocks towards the kid; the kid's throw-back juggles him, TestWorld
	 * answers right at once, and the super yeet flies south, away from the kid. He must land on the floor: never out of
	 * the arena.
	 */
	private static void superYeetOnLevel3(GameTestHelper helper, double out) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		test.world.enterDoor(kid, 3);
		int[] step = {0};
		int[] waited = {0};
		int[] startHits = {0};
		double[] hanging = {-1};
		double[] farthest = {0};
		YeetReport[] superYeet = {null};
		helper.onEachTick(() -> {
			BossFight fight = test.world.run(FightLevel.MOEILIJK).map(ArenaRun::fight).orElse(null);
			if (fight == null) {
				return;
			}
			ArenaBox box = fight.arena();
			GraafFout boss = fight.boss();
			double fromMiddle = Math.hypot(boss.getX() - box.middleX(), boss.getZ() - box.middleZ());
			if (step[0] == 0) {
				int ground = test.level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) box.middleX(), (int) box.middleZ());
				boss.teleportTo(box.middleX(), ground + 1.5, box.middleZ() + out);
				kid.teleportTo(box.middleX(), ground, box.middleZ() + out - 6);
				Vec3 south = new Vec3(box.middleX(), ground, box.middleZ() + out + 3);
				boss.yeet(south, 1.0, YeetStyle.HELICOPTER, south);
				startHits[0] = fight.hitsRemaining();
				step[0] = 1;
			} else if (step[0] == 1 && ++waited[0] >= 12) {
				ArenaFight.giveInHand(kid, BombTier.KNALLETJE);
				ArenaFight.aimAt(kid, boss.position().add(0, boss.getBbHeight() / 2, 0));
				fight.throwBack(kid, InteractionHand.MAIN_HAND);
				step[0] = 2;
			} else if (step[0] == 2) {
				// Until the super yeet starts (it credits the kid), he flies or hangs from the helicopter; then he is on the super yeet.
				if (fight.creditedKid() == null) {
					hanging[0] = fromMiddle;
				} else {
					farthest[0] = Math.max(farthest[0], fromMiddle);
				}
				// The first yeet that lands is the super yeet, or the helicopter when the throw-back missed: then the test fails.
				if (fight.lastYeet() != null) {
					superYeet[0] = fight.lastYeet();
					step[0] = 3;
				}
			}
		});

		// TestWorld answers every question right at once: the juggle question too.
		helper.succeedWhen(() -> {
			BossFight fight = test.world.run(FightLevel.MOEILIJK).map(ArenaRun::fight).orElse(null);
			helper.assertTrue(fight != null && superYeet[0] != null, "wait for the super yeet to land");
			YeetReport yeet = superYeet[0];
			helper.assertTrue(yeet.kind() == YeetKind.SUPER, "it was a super yeet: " + yeet.describe());
			helper.assertTrue(yeet.flight() == YeetStyle.CLASSIC || yeet.flight() == YeetStyle.TUMBLE, "classic or tumbling: " + yeet.describe());
			helper.assertTrue(fight.hitsRemaining() == startHits[0] - 1, "and one hit: " + fight.hitsRemaining() + " left of " + startHits[0]);
			helper.assertTrue(hanging[0] >= out - 2.5, "he hung " + hanging[0] + " blocks from the middle");
			helper.assertFalse(yeet.flewOut(), "he never flew out of the arena: " + yeet.describe() + ", hung " + hanging[0] + " out");
			helper.assertTrue(farthest[0] < LANDS_WITHIN, "he landed on the floor, " + farthest[0] + " blocks from its middle (hung " + hanging[0] + " out)");
			List<AnswerRecord> answers = test.sessions.learners().log().read(kid.getPlainTextName());
			helper.assertTrue(answers.stream().anyMatch(answer -> answer.context().equals("juggle") && answer.correct()),
					"the answer log says the sum was a juggle's: " + answers);
			test.close();
		});
	}

	@GameTest(maxTicks = 200, padding = 48)
	@Fight
	public void aWrongAnswerLetsHimFallTheNormalWayWithNoHit(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		int startHits = arena.fight.hitsRemaining();
		startJuggle(helper, arena, BombTier.KNALLETJE);
		boolean[] answered = {false};
		helper.onEachTick(() -> {
			if (!answered[0] && arena.fight.boss().isFrozen()) {
				answered[0] = true;
				arena.answerWrong(arena.kid, arena.questionsTo(arena.kid).getFirst());
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(answered[0], "wait for the freeze and the wrong answer");
			helper.assertFalse(arena.fight.boss().isFrozen(), "he unfreezes");
			helper.assertTrue(arena.fight.hitsRemaining() == startHits, "no hit");
			helper.assertTrue(arena.fight.boss().onGround(), "and comes down: " + arena.fight.boss().position());
			arena.end();
		});
	}

	@GameTest(maxTicks = 60, padding = 48)
	public void aDisconnectLetsHimFall(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		startJuggle(helper, arena, BombTier.KNALLETJE);
		boolean[] disconnected = {false};
		helper.onEachTick(() -> {
			if (!disconnected[0] && arena.fight.boss().isFrozen()) {
				disconnected[0] = true;
				arena.sessions.onDisconnect(arena.kid);
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(disconnected[0], "wait for the freeze");
			helper.assertFalse(arena.fight.boss().isFrozen(), "the question ended without an answer: he falls");
			arena.end();
		});
	}

	@GameTest(maxTicks = 60, padding = 48)
	public void aTimeoutOfTheJuggleQuestionLetsHimFallToo(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		startJuggle(helper, arena, BombTier.KNALLETJE);
		boolean[] timedOut = {false};
		helper.onEachTick(() -> {
			if (!timedOut[0] && arena.fight.boss().isFrozen()) {
				timedOut[0] = true;
				// SessionManager.ANSWER_TIMEOUT_TICKS is 2400: tick it that far in one go.
				for (int i = 0; i < 2500; i++) {
					arena.sessions.tick();
				}
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(timedOut[0], "wait for the freeze");
			helper.assertFalse(arena.fight.boss().isFrozen(), "the question timed out: he falls");
			arena.end();
		});
	}

	@GameTest(maxTicks = 80, padding = 48)
	public void thereIsNoSecondJuggleInOneFlightAndASecondKidsThrowBackDropsBack(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		ServerPlayer sibling = arena.addKid(arena.box.middleX() + 6, arena.box.middleZ() + 6);
		startJuggle(helper, arena, BombTier.KNALLETJE);
		int[] stage = {0};
		helper.onEachTick(() -> {
			if (stage[0] == 0 && arena.fight.boss().isFrozen()) {
				stage[0] = 1;
				// While he hangs: the sibling's throw-back drops back, it is the first one's juggle that counts.
				arena.fight.backReached(sibling.getUUID(), BombTier.GEWONE_TNT);
				helper.assertTrue(arena.terugbommen().size() == 1, "the sibling's bounced off: " + arena.terugbommen().size());
				helper.assertFalse(arena.asked(sibling, BombTier.GEWONE_TNT), "and asked nothing");
				arena.answerWrong(arena.kid, arena.questionsTo(arena.kid).getFirst());
			} else if (stage[0] == 1 && !arena.fight.boss().isFrozen()) {
				stage[0] = 2;
				// He flies on after the wrong answer: a second juggle in the same flight is not allowed.
				arena.fight.backReached(sibling.getUUID(), BombTier.KNALLETJE);
				helper.assertTrue(arena.terugbommen().size() == 2, "a second throw-back in the same flight drops back too");
				helper.assertFalse(arena.fight.boss().isFrozen(), "he is not frozen again");
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(stage[0] == 2, "wait for both throw-backs");
			arena.end();
		});
	}

	@GameTest(maxTicks = 100, padding = 48)
	public void aSuperYeetCantBeJuggled(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		startJuggle(helper, arena, BombTier.KNALLETJE);
		int[] stage = {0};
		helper.onEachTick(() -> {
			if (stage[0] == 0 && arena.fight.boss().isFrozen()) {
				stage[0] = 1;
				arena.answerRight(arena.kid, arena.questionsTo(arena.kid).getFirst());
			} else if (stage[0] == 1 && !arena.fight.boss().isFrozen() && arena.fight.boss().isFlying()) {
				stage[0] = 2;
				// He flies from the super yeet now: a throw-back that reaches him drops back.
				arena.fight.backReached(arena.kid.getUUID(), BombTier.KNALLETJE);
				helper.assertTrue(arena.terugbommen().size() == 1, "it dropped back");
				helper.assertFalse(arena.fight.boss().isFrozen(), "he stays in his super yeet");
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(stage[0] == 2, "wait for the super yeet and the second throw-back");
			arena.end();
		});
	}

	@GameTest(maxTicks = 120, padding = 48)
	public void aKippenbomJugglesChickensComeDownAtTheFloor(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		startJuggle(helper, arena, BombTier.KIPPENBOM);
		double[] hangingY = {0};
		boolean[] answered = {false};
		helper.onEachTick(() -> {
			if (!answered[0] && arena.fight.boss().isFrozen()) {
				answered[0] = true;
				hangingY[0] = arena.fight.boss().getY();
				arena.answerRight(arena.kid, arena.questionsTo(arena.kid).getFirst());
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(answered[0], "wait for the freeze and the right answer");
			helper.assertTrue(hangingY[0] >= arena.floorY + 3, "he hung well above the floor: " + hangingY[0]);
			List<Chicken> chickens = arena.level.getEntitiesOfClass(Chicken.class,
					new AABB(arena.box.minX(), arena.box.minY(), arena.box.minZ(), arena.box.maxX() + 1, arena.box.maxY() + 30, arena.box.maxZ() + 1));
			helper.assertFalse(chickens.isEmpty(), "chickens came out");
			helper.assertTrue(chickens.stream().allMatch(chicken -> chicken.getY() < arena.floorY + 2.6), "all of them at the arena floor, not in the air");
			arena.end();
		});
	}

	/**
	 * Kid B juggles him while kid A has a Terug! question open, and A answers first: A's answer wins. He flies off on
	 * A's throw-back yeet, B's juggle question closes quietly (no hit, no hearts, B is free, the answer log has nothing
	 * for it), and the fight goes on: he lands, throws again, and is beaten in the end.
	 */
	@GameTest(maxTicks = 6000, padding = 48)
	@Fight
	public void aSiblingsRightAnswerWhileTheJuggleHangsWinsAndTheFightGoesOn(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		ServerPlayer kidA = arena.kid;
		ServerPlayer kidB = arena.addKid(arena.box.middleX() + 6, arena.box.middleZ() + 6);
		int startHits = arena.fight.hitsRemaining();
		int[] stage = {0};
		long[] answeredAt = {-1};
		boolean[] threwAgain = {false};
		Set<Integer> answered = new HashSet<>();
		helper.onEachTick(() -> {
			if (stage[0] == 0 && helper.getTick() >= 2) {
				// A's throw-back reaches him while he stands: A's Terug! question opens.
				arena.fight.backReached(kidA.getUUID(), BombTier.KNALLETJE);
				helper.assertTrue(arena.sessions.isBusy(kidA), "A has the Terug! question");
				arena.fight.boss().yeet(kidA.position(), 1.0, YeetStyle.HELICOPTER, kidA.position());
				stage[0] = 1;
			} else if (stage[0] == 1 && arena.fight.boss().isFlying()) {
				// B's throw-back reaches him in mid-yeet: the juggle.
				arena.fight.backReached(kidB.getUUID(), BombTier.KNALLETJE);
				helper.assertTrue(arena.fight.boss().isFrozen() && arena.sessions.isBusy(kidB), "B juggled him: he hangs, B is asked");
				stage[0] = 2;
			} else if (stage[0] == 2) {
				answered.add(arena.questionsTo(kidB).getFirst().sessionId());
				QuizS2C terug = arena.questionsTo(kidA).getFirst();
				answered.add(terug.sessionId());
				arena.answerRight(kidA, terug);
				answeredAt[0] = helper.getTick();
				helper.assertFalse(arena.fight.boss().isFrozen(), "A's answer sends him off: he no longer hangs");
				helper.assertTrue(arena.fight.boss().isFlying(), "he flies on A's yeet");
				helper.assertFalse(arena.sessions.isBusy(kidB), "B's juggle question closed: B is free");
				helper.assertTrue(kidA.getUUID().equals(arena.fight.creditedKid()), "the yeet is A's");
				helper.assertTrue(arena.fight.hitsRemaining() == startHits - 1, "one hit, A's: " + arena.fight.hitsRemaining());
				stage[0] = 3;
			} else if (stage[0] == 3) {
				// From here on both kids answer everything right, until he is beaten.
				for (ServerPlayer kid : List.of(kidA, kidB)) {
					for (QuizS2C question : arena.questionsTo(kid)) {
						if (answered.add(question.sessionId())) {
							threwAgain[0] = true;
							arena.answerRight(kid, question);
						}
					}
				}
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(stage[0] == 3, "wait for the juggle and A's answer");
			helper.assertTrue(threwAgain[0] || arena.fight.lastThrow() != null, "wait for his next throw: the fight's timers run");
			helper.assertTrue(arena.fight.isOver() && arena.fight.isWon(), "wait until he is beaten and gone");
			FightTally b = arena.fight.tally(kidB.getUUID());
			helper.assertTrue(b.terugRight() == 0 && b.terugWrong() == 0, "B's juggle question was never answered: " + b.terugRight() + "/" + b.terugWrong());
			helper.assertTrue(b.juggles() == 1, "B's juggle still counts as a juggle");
			helper.assertTrue(arena.fight.tally(kidA.getUUID()).terugRight() == 1, "A's Terug! answer counts");
			List<AnswerRecord> answers = arena.sessions.learners().log().read(kidB.getPlainTextName());
			helper.assertTrue(answers.stream().noneMatch(answer -> answer.context().equals("juggle")), "no juggle answer is logged for B: " + answers);
		});
	}

	/** A helicopter yeet on cue (he hovers up over the kids for a while), and a throw-back of this tier into him in mid-air. */
	private static void startJuggle(GameTestHelper helper, ArenaFight arena, BombTier tier) {
		helper.runAfterDelay(2, () -> arena.fight.boss().yeet(arena.kid.position(), 1.0, YeetStyle.HELICOPTER, arena.kid.position()));
		helper.runAfterDelay(14, () -> {
			ArenaFight.giveInHand(arena.kid, tier);
			ArenaFight.aimAt(arena.kid, arena.hisMiddle());
			arena.fight.throwBack(arena.kid, InteractionHand.MAIN_HAND);
		});
	}
}
