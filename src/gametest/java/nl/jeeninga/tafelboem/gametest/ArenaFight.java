package nl.jeeninga.tafelboem.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.boss.BossFights;
import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.log.AnswerLog;
import nl.jeeninga.tafelboem.log.Learners;
import nl.jeeninga.tafelboem.net.QuizC2S;
import nl.jeeninga.tafelboem.net.QuizS2C;

/**
 * A fight in a 21 × 21 arena on {@link OpenField}'s floor: Graaf Fout hovering in the middle, one
 * kid 5 blocks south of him, sessions and fights ticking, nobody answering. The R1.3 tests need an
 * arena's rules (who is in the fight, craters, traps only inside it) without a whole TafelBoem
 * world. Use {@code padding = 48}.
 */
final class ArenaFight {
	final GameTestHelper helper;
	final ServerLevel level;
	final RecordingQuizClient client = new RecordingQuizClient();
	final SessionManager sessions;
	final BossFights fights;
	final ArenaBox box;
	/** The first layer of air above the floor, where the kid's feet are. */
	final int floorY;
	final ServerPlayer kid;
	final BossFight fight;

	ArenaFight(GameTestHelper helper, TafelBoemConfig config) {
		this(helper, config, true);
	}

	/**
	 * @param inArena false: he is summoned with no arena, as the summon item does outside a TafelBoem world;
	 *                {@link #box} then only says where the 21 × 21 floor is
	 */
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	ArenaFight(GameTestHelper helper, TafelBoemConfig config, boolean inArena) {
		OpenField.open(helper);
		this.helper = helper;
		this.level = helper.getLevel();
		this.sessions = new SessionManager(level.getServer(), new Learners(new AnswerLog(tempDir())), client, config);
		this.fights = new BossFights(sessions);
		// OpenField's stone floor is one block below the test's corner: that is the arena's bottom.
		BlockPos corner = helper.absolutePos(new BlockPos(-10, -1, -10));
		this.box = ArenaBox.of(corner.getX(), corner.getY(), corner.getZ(), 21, 6, 21);
		this.floorY = corner.getY() + 1;
		this.kid = helper.makeMockServerPlayerInLevel();
		kid.teleportTo(box.middleX(), floorY, box.middleZ() + 5);
		BossFight summoned = fights.summon(level, new Vec3(box.middleX(), floorY + 1.5, box.middleZ()), FightLevel.MAKKELIJK,
				inArena ? box : null);
		helper.assertTrue(summoned != null, "Graaf Fout should appear");
		this.fight = summoned;
		helper.onEachTick(() -> {
			sessions.tick();
			fights.tick();
		});
	}

	/** A sibling, standing on the floor at this spot. */
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	ServerPlayer addKid(double x, double z) {
		ServerPlayer sibling = helper.makeMockServerPlayerInLevel();
		sibling.teleportTo(x, floorY, z);
		return sibling;
	}

	/** Whether this kid was asked a sum on a bomb of this tier. */
	boolean asked(ServerPlayer player, BombTier tier) {
		return client.sentTo.stream().anyMatch(sent -> sent.player() == player && sent.payload().mode() == QuizS2C.ASK
				&& sent.payload().tier() == tier.tier());
	}

	boolean askedAnything() {
		return client.sentTo.stream().anyMatch(sent -> sent.payload().mode() == QuizS2C.ASK);
	}

	/** Every question this kid was asked so far, oldest first. */
	List<QuizS2C> questionsTo(ServerPlayer player) {
		return client.sentTo.stream().filter(sent -> sent.player() == player && sent.payload().mode() == QuizS2C.ASK)
				.map(RecordingQuizClient.Sent::payload).toList();
	}

	/** The kid types the right product for this question. */
	void answerRight(ServerPlayer player, QuizS2C question) {
		sessions.onClientAction(player, new QuizC2S(question.sessionId(), QuizC2S.ANSWER, question.a() * question.b(), QuizC2S.VIA_KEY));
	}

	/** The kid types a product that is wrong. */
	void answerWrong(ServerPlayer player, QuizS2C question) {
		sessions.onClientAction(player, new QuizC2S(question.sessionId(), QuizC2S.ANSWER, question.a() * question.b() + 1, QuizC2S.VIA_KEY));
	}

	/** Graaf Fout said the line with this key in this kid's chat. */
	boolean heard(ServerPlayer player, String key) {
		return client.chatsTo.stream().anyMatch(chat -> chat.player() == player
				&& chat.message().getContents() instanceof TranslatableContents says && says.getKey().equals("tafelboem.boss.says")
				&& says.getArgs().length == 2 && says.getArgs()[1] instanceof Component line
				&& line.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key));
	}

	void end() {
		fights.endAll();
	}

	private static Path tempDir() {
		try {
			return Files.createTempDirectory("tafelboem-gametest");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
