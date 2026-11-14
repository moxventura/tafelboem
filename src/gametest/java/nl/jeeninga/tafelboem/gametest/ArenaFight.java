package nl.jeeninga.tafelboem.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.boss.BossFights;
import nl.jeeninga.tafelboem.boss.ThrownBomb;
import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.log.AnswerLog;
import nl.jeeninga.tafelboem.log.Learners;
import nl.jeeninga.tafelboem.item.TerugbomItem;
import nl.jeeninga.tafelboem.net.QuizC2S;
import nl.jeeninga.tafelboem.net.QuizS2C;
import nl.jeeninga.tafelboem.registry.ModItems;

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

	private final Map<ServerPlayer, double[]> walking = new HashMap<>();

	ArenaFight(GameTestHelper helper, TafelBoemConfig config) {
		this(helper, config, FightLevel.MAKKELIJK, true);
	}

	/**
	 * @param inArena false: he is summoned with no arena, as the summon item does outside a TafelBoem world;
	 *                {@link #box} then only says where the 21 × 21 floor is
	 */
	ArenaFight(GameTestHelper helper, TafelBoemConfig config, boolean inArena) {
		this(helper, config, FightLevel.MAKKELIJK, inArena);
	}

	/** @param fightLevel the level he fights at: its bombs, its tables and its aim */
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	ArenaFight(GameTestHelper helper, TafelBoemConfig config, FightLevel fightLevel, boolean inArena) {
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
		BossFight summoned = fights.summon(level, new Vec3(box.middleX(), floorY + 1.5, box.middleZ()), fightLevel, inArena ? box : null);
		helper.assertTrue(summoned != null, "Graaf Fout should appear");
		this.fight = summoned;
		helper.onEachTick(() -> {
			walking.forEach((walker, step) -> walker.teleportTo(walker.getX() + step[0], walker.getY(), walker.getZ() + step[1]));
			sessions.tick();
			fights.tick();
		});
	}

	/** From now on this kid walks this far every tick (a mock player is moved by teleporting it once a tick); (0, 0) stops them. */
	void walk(ServerPlayer kid, double dx, double dz) {
		walking.put(kid, new double[] {dx, dz});
	}

	/** His bombs in the air over this arena now: not the kicks and the throw-backs of the kids, which have a kid for an owner. */
	List<ThrownBomb> bombsInTheAir() {
		return level.getEntitiesOfClass(ThrownBomb.class,
				new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 40, box.maxZ() + 1),
				bomb -> bomb.getOwner() == fight.boss());
	}

	/** This kid's throw-backs in the air over this arena now. */
	List<ThrownBomb> thrownBackInTheAir(ServerPlayer thrower) {
		return level.getEntitiesOfClass(ThrownBomb.class,
				new AABB(box.minX() - 20, box.minY(), box.minZ() - 20, box.maxX() + 21, box.maxY() + 40, box.maxZ() + 21),
				bomb -> bomb.getOwner() == thrower);
	}

	/** The Terugbommen lying on the floor of this arena now. */
	List<ItemEntity> terugbommen() {
		return level.getEntitiesOfClass(ItemEntity.class,
				new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 40, box.maxZ() + 1),
				item -> item.getItem().getItem() instanceof TerugbomItem);
	}

	/** The kid turns to look at this point: a mock player's view is set directly. */
	static void aimAt(ServerPlayer kid, Vec3 point) {
		Vec3 to = point.subtract(kid.getEyePosition());
		kid.setYRot((float) (Mth.atan2(-to.x, to.z) * Mth.RAD_TO_DEG));
		kid.setXRot((float) -(Mth.atan2(to.y, to.horizontalDistance()) * Mth.RAD_TO_DEG));
	}

	/** A Terugbom of this tier in the kid's main hand. */
	static void giveInHand(ServerPlayer kid, BombTier tier) {
		kid.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.terugbom(tier)));
	}

	/** The middle of Graaf Fout, where a thrown-back bomb aims. */
	Vec3 hisMiddle() {
		return fight.boss().position().add(0, fight.boss().getBbHeight() / 2, 0);
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

	/**
	 * Mock players start in creative, which nothing kills; and until their client has loaded (a mock one
	 * never says so) Minecraft keeps a player invulnerable, even to {@code kill()}.
	 */
	static void survival(ServerPlayer kid) {
		kid.setGameMode(GameType.SURVIVAL);
		kid.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
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
