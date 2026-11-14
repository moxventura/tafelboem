package nl.jeeninga.tafelboem.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.boss.ThrownBomb;
import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.Outcome;
import nl.jeeninga.tafelboem.core.YeetStyle;
import nl.jeeninga.tafelboem.game.QuizListener;
import nl.jeeninga.tafelboem.game.QuizSession;
import nl.jeeninga.tafelboem.net.QuizS2C;

/**
 * R1.4: two questions at once is normal play, so the fight keeps who answers what. A yeet credits the kid whose
 * answer started it, a fight that ends closes every open question, and a question that ends without an answer
 * tells its fight.
 */
public class BookkeepingGameTest {
	@GameTest(maxTicks = 400, padding = 48)
	@Fight
	public void aYeetCreditsTheKidWhoseAnswerStartedItEvenWhenTheSiblingAnswersWhileHeFlies(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		ServerPlayer sibling = arena.addKid(arena.box.middleX() + 7, arena.box.middleZ() + 5);
		int startHits = arena.fight.hitsRemaining();
		// Before his first throw (60 ticks): the sibling is already answering, so his bomb comes for the other kid.
		helper.runAfterDelay(5, () -> arena.fight.placeLyingBomb(sibling.blockPosition().east(), BombTier.GEWONE_TNT));

		boolean[] bothAsked = {false};
		boolean[] siblingAnswered = {false};
		boolean[] firstOwnsHisFlight = {false};
		boolean[] stillTheFirstAfterTheSiblingAnswered = {false};
		boolean[] siblingOwnsTheNext = {false};
		Set<ServerPlayer> answered = new HashSet<>();
		helper.onEachTick(() -> {
			bothAsked[0] |= arena.sessions.isBusy(arena.kid) && arena.sessions.isBusy(sibling);
			if (!bothAsked[0]) {
				return;
			}
			// Both questions are open: the thrown kid answers now, the sibling once that kick has hit and he is flying.
			if (answered.add(arena.kid)) {
				arena.answerRight(arena.kid, arena.questionsTo(arena.kid).getFirst());
			}
			if (arena.fight.hitsRemaining() == startHits - 1 && !siblingAnswered[0]) {
				siblingAnswered[0] = true;
				firstOwnsHisFlight[0] = arena.kid.getUUID().equals(arena.fight.creditedKid());
				arena.answerRight(sibling, arena.questionsTo(sibling).getFirst());
				// The sibling's answer used to overwrite the one shared kick, so the first kid's flight got credited to them.
				stillTheFirstAfterTheSiblingAnswered[0] = arena.kid.getUUID().equals(arena.fight.creditedKid());
			}
			if (arena.fight.hitsRemaining() == startHits - 2) {
				siblingOwnsTheNext[0] = sibling.getUUID().equals(arena.fight.creditedKid());
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(siblingAnswered[0], "wait for both kicks");
			helper.assertTrue(arena.fight.hitsRemaining() == startHits - 2, "wait until the sibling's kick has hit too");
			helper.assertTrue(firstOwnsHisFlight[0], "his first flight is the first kid's");
			helper.assertTrue(stillTheFirstAfterTheSiblingAnswered[0], "the sibling's answer doesn't take over the first kid's flight");
			helper.assertTrue(siblingOwnsTheNext[0], "once the sibling's kick has hit, the new flight is the sibling's");
			arena.end();
		});
	}

	@GameTest(maxTicks = 300, padding = 48)
	@Fight
	public void aFightThatEndsWithAQuestionOpenClosesIt(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		int[] openSession = {-1};

		helper.succeedWhen(() -> {
			if (openSession[0] < 0) {
				helper.assertTrue(arena.sessions.isBusy(arena.kid), "wait for his first question");
				openSession[0] = arena.questionsTo(arena.kid).getFirst().sessionId();
				arena.end();
			}
			helper.assertFalse(arena.sessions.isBusy(arena.kid), "the question is closed with the fight");
			helper.assertTrue(arena.client.sentTo.stream().anyMatch(sent -> sent.player() == arena.kid
					&& sent.payload().mode() == QuizS2C.CLOSE && sent.payload().sessionId() == openSession[0]), "and its screen is told to close");
			helper.assertFalse(arena.kid.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(TafelBoem.id("rooted")), "the kid can walk again");
		});
	}

	@GameTest(maxTicks = 40, padding = 48)
	public void aQuestionThatEndsWhenTheKidDisconnectsTellsItsFight(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		Ended listener = new Ended();
		arena.sessions.start(arena.kid, BombTier.KNALLETJE, arena.kid.blockPosition().east(), "test", listener);

		arena.sessions.onDisconnect(arena.kid);

		helper.assertTrue(listener.ended.equals(List.of(arena.kid.getUUID())), "the owner was told once: " + listener.ended);
		helper.assertFalse(arena.sessions.isBusy(arena.kid), "and the question is gone");
		arena.end();
		helper.succeed();
	}

	@GameTest(maxTicks = 40, padding = 48)
	public void aQuestionThatEndsWhenTheKidDiesTellsItsFight(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		ArenaFight.survival(arena.kid);
		Ended listener = new Ended();
		arena.sessions.start(arena.kid, BombTier.KNALLETJE, arena.kid.blockPosition().east(), "test", listener);
		arena.kid.kill(arena.level);

		helper.runAfterDelay(5, () -> {
			helper.assertTrue(listener.ended.equals(List.of(arena.kid.getUUID())), "the owner was told once: " + listener.ended);
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40, padding = 48)
	public void aQuestionThatTimesOutTellsItsFight(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		Ended listener = new Ended();
		arena.sessions.start(arena.kid, BombTier.KNALLETJE, arena.kid.blockPosition().east(), "test", listener);

		// SessionManager.ANSWER_TIMEOUT_TICKS is 2400: tick it that far in one go instead of waiting two minutes.
		for (int i = 0; i < 2500; i++) {
			arena.sessions.tick();
		}

		helper.assertTrue(listener.ended.equals(List.of(arena.kid.getUUID())), "the owner was told once: " + listener.ended);
		helper.assertFalse(arena.sessions.isBusy(arena.kid), "the question is gone");
		arena.end();
		helper.succeed();
	}

	@GameTest(maxTicks = 300, padding = 48)
	@Fight
	public void heNeverThrowsWhileHeFliesOrLands(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		AABB around = new AABB(arena.box.minX(), arena.box.minY(), arena.box.minZ(), arena.box.maxX() + 1, arena.box.maxY() + 40,
				arena.box.maxZ() + 1);
		boolean[] reacted = {false};
		int[] thrownWhileReacting = {0};
		// A helicopter flight and its landing take longer than the 60 ticks before his first throw.
		helper.runAfterDelay(5, () -> arena.fight.boss().yeet(arena.kid.position(), 1.0, YeetStyle.HELICOPTER, arena.kid.position()));
		helper.onEachTick(() -> {
			if (arena.fight.boss().isReacting()) {
				reacted[0] = true;
				thrownWhileReacting[0] += arena.level.getEntitiesOfClass(ThrownBomb.class, around).size();
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(reacted[0] && !arena.fight.boss().isReacting(), "wait until his flight and landing are over");
			helper.assertTrue(thrownWhileReacting[0] == 0, "he threw " + thrownWhileReacting[0] + " bomb-ticks while he flew or landed");
			arena.end();
		});
	}

	/** Remembers every question that ended without an answer. */
	private static final class Ended implements QuizListener {
		final List<UUID> ended = new ArrayList<>();

		@Override
		public void onCorrect(ServerPlayer player, QuizSession session) {
		}

		@Override
		public void onMiss(ServerPlayer player, QuizSession session, Outcome outcome) {
		}

		@Override
		public void onEnded(UUID player, QuizSession session) {
			ended.add(player);
		}
	}
}
