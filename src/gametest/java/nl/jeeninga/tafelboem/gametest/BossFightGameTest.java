package nl.jeeninga.tafelboem.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.boss.BossFights;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.log.AnswerLog;
import nl.jeeninga.tafelboem.log.Learners;
import nl.jeeninga.tafelboem.net.QuizC2S;
import nl.jeeninga.tafelboem.net.QuizS2C;

/**
 * End to end: Graaf Fout throws, the bomb lands as a question, a correct answer kicks it back and
 * costs him a hit.
 */
public class BossFightGameTest {
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
					sessions.onClientAction(sent.player(), new QuizC2S(question.sessionId(), QuizC2S.ANSWER, question.a() * question.b()));
				}
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(fight.hitsRemaining() < startHits, "a kicked-back bomb should hit Graaf Fout");
			fights.endAll();
		});
	}

	private static java.nio.file.Path tempDir() {
		try {
			return Files.createTempDirectory("tafelboem-gametest");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
