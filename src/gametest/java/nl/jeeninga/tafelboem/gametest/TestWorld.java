package nl.jeeninga.tafelboem.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.boss.BossFights;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.game.ProtectedAreas;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.log.AnswerLog;
import nl.jeeninga.tafelboem.log.FightLog;
import nl.jeeninga.tafelboem.log.Learners;
import nl.jeeninga.tafelboem.net.QuizC2S;
import nl.jeeninga.tafelboem.net.QuizS2C;
import nl.jeeninga.tafelboem.world.ArenaCatalog;
import nl.jeeninga.tafelboem.world.TafelBoemWorld;
import nl.jeeninga.tafelboem.world.WorldLayout;

/**
 * A TafelBoem world inside a GameTest: the hub slot and the three arena slots in a square, 56 blocks
 * apart and centred on the test's corner. Use {@code padding = TestWorld.PADDING} so neighbouring tests
 * stay clear. Before each test the runner clears its whole padded box, which grows with the cube of
 * the padding, so the slots sit as close around the corner as they fit: padding 128 made the runner
 * clear about 17 million blocks per world test, 56 about 1.5 million. Client game tests accept at
 * most 128 anyway.
 */
final class TestWorld {
	static final int SPACING = 56;
	/** The slots reach 52 blocks to one side of the corner and 51 to the other. */
	static final int PADDING = 56;
	/** No template is ever saved under these, so a hub or arena the mod ships can't change the tests. */
	static final Identifier HUB = TafelBoem.id("test_world_hub");
	static final String ARENAS = "test_world_level_";

	final GameTestHelper helper;
	final ServerLevel level;
	final WorldLayout layout;
	final RecordingQuizClient client = new RecordingQuizClient();
	SessionManager sessions;
	BossFights fights;
	FightLog log;
	TafelBoemWorld world;
	private final Set<Integer> answered = new HashSet<>();
	private final Set<UUID> wrongOnce = new HashSet<>();

	TestWorld(GameTestHelper helper) {
		this.helper = helper;
		this.level = helper.getLevel();
		BlockPos hub = helper.absolutePos(BlockPos.ZERO).offset(-(SPACING - 4), 0, -(SPACING - 4));
		this.layout = new WorldLayout(hub, Map.of(
				FightLevel.MAKKELIJK, hub.offset(SPACING, 0, 0),
				FightLevel.GEMIDDELD, hub.offset(0, 0, SPACING),
				FightLevel.MOEILIJK, hub.offset(SPACING, 0, SPACING)));
		// Placing clears a slot, entities included, so a slot must never reach into a neighbouring test.
		AABB padded = helper.getBoundsWithPadding();
		helper.assertTrue(inside(padded, layout.hubSlot()), "the hub slot stays inside the test's padded area " + padded);
		for (FightLevel fightLevel : FightLevel.DOORS) {
			helper.assertTrue(inside(padded, layout.arenaSlot(fightLevel)), "the arena slot of level " + fightLevel.number() + " stays inside the test's padded area " + padded);
		}
		// Only the test's own chunks are force-loaded; keep the slots ticking too (see OpenField).
		keepSlotsLoaded();
		helper.onEachTick(this::keepSlotsLoaded);
	}

	/**
	 * Runs the test body a few ticks later, once the slots' chunks have loaded: an entity in a chunk
	 * that isn't loaded yet (an item frame, say) can't be found.
	 */
	void whenLoaded(Runnable body) {
		helper.runAfterDelay(5, body);
	}

	private void keepSlotsLoaded() {
		keepLoaded(layout.hubSlot());
		for (FightLevel fightLevel : FightLevel.DOORS) {
			keepLoaded(layout.arenaSlot(fightLevel));
		}
	}

	/** Runs a TafelBoem world in the layout: the hub placed, every question answered right. */
	TestWorld running() {
		Path dir = tempDir();
		sessions = new SessionManager(level.getServer(), new Learners(new AnswerLog(dir)), client);
		fights = new BossFights(sessions);
		log = new FightLog(dir);
		world = new TafelBoemWorld(level, layout, fights, log,
				new ArenaCatalog(level.getServer().getStructureManager(), new Random(1), ARENAS), client, false);
		world.placeHub(HUB);
		helper.onEachTick(() -> {
			sessions.tick();
			fights.tick();
			world.tick();
			answerQuestions();
		});
		return this;
	}

	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	ServerPlayer kidInTheHub() {
		ServerPlayer kid = helper.makeMockServerPlayerInLevel();
		BlockPos at = world.hub().standAt();
		kid.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		return kid;
	}

	/** This kid's next answer is wrong, the ones after it right. */
	void answerWrongOnce(ServerPlayer kid) {
		wrongOnce.add(kid.getUUID());
	}

	void close() {
		fights.endAll();
		ProtectedAreas.unprotect(level, world.hub().bounds());
	}

	private void answerQuestions() {
		for (RecordingQuizClient.Sent sent : List.copyOf(client.sentTo)) {
			QuizS2C question = sent.payload();
			if (question.mode() == QuizS2C.ASK && answered.add(question.sessionId())) {
				int answer = wrongOnce.remove(sent.player().getUUID()) ? question.a() * question.b() + 1 : question.a() * question.b();
				sessions.onClientAction(sent.player(), new QuizC2S(question.sessionId(), QuizC2S.ANSWER, answer, QuizC2S.VIA_KEY));
			}
		}
	}

	private static Path tempDir() {
		try {
			return Files.createTempDirectory("tafelboem-gametest");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static boolean inside(AABB padded, ArenaBox slot) {
		return padded.contains(slot.minX(), slot.minY(), slot.minZ()) && padded.contains(slot.maxX() + 0.5, slot.maxY() + 0.5, slot.maxZ() + 0.5);
	}

	private void keepLoaded(ArenaBox slot) {
		BlockPos centre = new BlockPos((int) slot.middleX(), slot.minY(), (int) slot.middleZ());
		level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, ChunkPos.containing(centre), 4);
	}
}
