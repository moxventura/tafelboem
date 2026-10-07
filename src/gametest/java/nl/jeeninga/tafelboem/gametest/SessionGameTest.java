package nl.jeeninga.tafelboem.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.Outcome;
import nl.jeeninga.tafelboem.game.BombEffects;
import nl.jeeninga.tafelboem.game.QuizListener;
import nl.jeeninga.tafelboem.game.QuizSession;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.log.AnswerLog;
import nl.jeeninga.tafelboem.log.Learners;
import nl.jeeninga.tafelboem.net.QuizC2S;
import nl.jeeninga.tafelboem.net.QuizS2C;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/**
 * Smoke tests for the question flow: rooted → ask → boom or correction → released.
 */
public class SessionGameTest {
	private static final BlockPos BOMB = new BlockPos(2, 1, 2);

	@GameTest(maxTicks = 200)
	public void correctAnswerReleasesThePlayerAndTheBombGoesOff(GameTestHelper helper) {
		Fixture f = new Fixture(helper);
		QuizS2C question = f.ask();

		helper.assertTrue(f.isRooted(), "player should be rooted while answering");
		f.answer(question, question.a() * question.b());

		helper.assertFalse(f.isRooted(), "player should be free to run after a correct answer");
		helper.assertValueEqual(f.client.last().mode(), QuizS2C.CLOSE, "screen mode after correct answer");
		helper.assertBlockPresent(ModBlocks.bomb(BombTier.KNALLETJE), BOMB);
		helper.succeedWhen(() -> helper.assertBlockNotPresent(ModBlocks.bomb(BombTier.KNALLETJE), BOMB));
	}

	@GameTest
	public void wrongAnswerShowsTheCorrectionAndKeepsThePlayerRootedUntilRetyped(GameTestHelper helper) {
		Fixture f = new Fixture(helper);
		QuizS2C question = f.ask();

		f.answer(question, question.a() * question.b() + 1);

		QuizS2C correction = f.client.last();
		helper.assertValueEqual(correction.mode(), QuizS2C.CORRECT, "screen mode after wrong answer");
		helper.assertValueEqual(correction.reason(), QuizS2C.REASON_WRONG, "correction reason");
		helper.assertFalse(correction.tipKey().isEmpty(), "a derived-fact tip should be shown");
		helper.assertTrue(f.isRooted(), "player stays rooted until the fact is retyped");
		helper.assertBlockNotPresent(ModBlocks.bomb(BombTier.KNALLETJE), BOMB);
		helper.assertTrue(f.player.hasEffect(MobEffects.BLINDNESS), "the Knalletje penalty is a soot cloud");

		f.manager.onClientAction(f.player, new QuizC2S(question.sessionId(), QuizC2S.CORRECTION_DONE, 1500));
		helper.assertFalse(f.isRooted(), "retyping the fact releases the player");
		helper.assertValueEqual(f.client.last().mode(), QuizS2C.CLOSE, "screen closes after the retype");
		helper.succeed();
	}

	@GameTest
	public void dontKnowCostsNothing(GameTestHelper helper) {
		Fixture f = new Fixture(helper);
		QuizS2C question = f.ask();

		f.manager.onClientAction(f.player, new QuizC2S(question.sessionId(), QuizC2S.DONT_KNOW, 0));

		helper.assertValueEqual(f.client.last().reason(), QuizS2C.REASON_DONT_KNOW, "correction reason");
		helper.assertFalse(f.player.hasEffect(MobEffects.BLINDNESS), "'?' must not be punished");
		helper.succeed();
	}

	@GameTest
	public void chickeningOutLetsAChickenOut(GameTestHelper helper) {
		Fixture f = new Fixture(helper);
		QuizS2C question = f.ask();

		f.manager.onClientAction(f.player, new QuizC2S(question.sessionId(), QuizC2S.CHICKEN_OUT, 0));

		helper.assertValueEqual(f.client.last().reason(), QuizS2C.REASON_CHICKEN_OUT, "correction reason");
		AABB around = new AABB(helper.absolutePos(BOMB)).inflate(3);
		List<Entity> chickens = helper.getLevel().getEntities((Entity) null, around,
				entity -> entity instanceof Chicken && entity.entityTags().contains(BombEffects.SPECTACLE_TAG));
		helper.assertFalse(chickens.isEmpty(), "a chicken should pop out");
		helper.succeed();
	}

	@GameTest
	public void aFightCanRefuseMoreChickenOuts(GameTestHelper helper) {
		Fixture f = new Fixture(helper);
		QuizListener noMoreChickens = new QuizListener() {
			@Override
			public boolean allowChickenOut() {
				return false;
			}

			@Override
			public void onCorrect(ServerPlayer player, QuizSession session) {
			}

			@Override
			public void onMiss(ServerPlayer player, QuizSession session, Outcome outcome) {
			}
		};
		helper.setBlock(BOMB, ModBlocks.bomb(BombTier.KNALLETJE));
		f.manager.start(f.player, BombTier.KNALLETJE, helper.absolutePos(BOMB), "fight", noMoreChickens);
		QuizS2C question = f.client.last();

		f.manager.onClientAction(f.player, new QuizC2S(question.sessionId(), QuizC2S.CHICKEN_OUT, 0));

		helper.assertValueEqual(f.client.last().reason(), QuizS2C.REASON_DONT_KNOW, "ESC counts as '?' after the limit");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	public void anArmedBombCannotBeStartedTwice(GameTestHelper helper) {
		Fixture f = new Fixture(helper);
		f.ask();
		ServerPlayer sibling = helper.makeMockServerPlayerInLevel();

		f.manager.startFromBlock(sibling, BombTier.KNALLETJE, helper.absolutePos(BOMB));

		helper.assertFalse(f.manager.isBusy(sibling), "a second player can't arm the same bomb");
		helper.succeed();
	}

	private static final class Fixture {
		final GameTestHelper helper;
		final RecordingQuizClient client = new RecordingQuizClient();
		final ServerPlayer player;
		final SessionManager manager;

		@SuppressWarnings("removal") // mock players are still the only way to test without a client
		Fixture(GameTestHelper helper) {
			this.helper = helper;
			this.player = helper.makeMockServerPlayerInLevel();
			Vec3 standHere = Vec3.atBottomCenterOf(helper.absolutePos(BOMB.east(3)));
			player.teleportTo(standHere.x, standHere.y, standHere.z);
			this.manager = new SessionManager(helper.getLevel().getServer(), new Learners(new AnswerLog(tempDir())), client);
			helper.onEachTick(manager::tick);
		}

		QuizS2C ask() {
			helper.setBlock(BOMB, ModBlocks.bomb(BombTier.KNALLETJE));
			manager.startFromBlock(player, BombTier.KNALLETJE, helper.absolutePos(BOMB));
			QuizS2C question = client.last();
			helper.assertValueEqual(question.mode(), QuizS2C.ASK, "first message opens the question");
			return question;
		}

		void answer(QuizS2C question, int value) {
			manager.onClientAction(player, new QuizC2S(question.sessionId(), QuizC2S.ANSWER, value));
		}

		boolean isRooted() {
			return player.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(TafelBoem.id("rooted"));
		}

		private static java.nio.file.Path tempDir() {
			try {
				return Files.createTempDirectory("tafelboem-gametest");
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}
}
