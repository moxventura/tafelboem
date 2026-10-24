package nl.jeeninga.tafelboem.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.AnswerRecord;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.ChickenCount;
import nl.jeeninga.tafelboem.core.DerivedFactTips;
import nl.jeeninga.tafelboem.core.EscapeModel;
import nl.jeeninga.tafelboem.core.Fact;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.Outcome;
import nl.jeeninga.tafelboem.core.Tip;
import nl.jeeninga.tafelboem.log.Learners;
import nl.jeeninga.tafelboem.net.QuizC2S;
import nl.jeeninga.tafelboem.net.QuizS2C;

/**
 * Server-authoritative question flow: root the player, ask, then boom or correct, then release.
 */
public final class SessionManager {
	/** Safety net: a child is never stuck if a screen fails to open or a client misbehaves. */
	static final int ANSWER_TIMEOUT_TICKS = 20 * 120;
	static final int CORRECTION_TIMEOUT_TICKS = 20 * 60;
	static final int SPECTACLE_LIFETIME_TICKS = 20 * 60;
	/** Counting chickens go sooner than other spectacle animals, so 10 × 10 on Moeilijk never piles up. */
	static final int COUNTING_LIFETIME_TICKS = 20 * 10;
	/** The first generation flies for a second before it pops into the groups. */
	static final int COUNTING_POP_TICKS = 20;
	/** Above this many spectacle animals the oldest poof first, so nothing lags the PC. */
	public static final int MAX_SPECTACLES = 150;

	private final MinecraftServer server;
	private final Learners learners;
	private final QuizClient client;
	private final TafelBoemConfig config;
	private final Map<UUID, QuizSession> sessions = new HashMap<>();
	private final List<PendingBoom> booms = new ArrayList<>();
	private final List<Spectacle> spectacles = new ArrayList<>();
	private final List<ChickenBurst> bursts = new ArrayList<>();
	private int nextSessionId = 1;

	public SessionManager(MinecraftServer server, Learners learners, QuizClient client) {
		this(server, learners, client, new TafelBoemConfig());
	}

	public SessionManager(MinecraftServer server, Learners learners, QuizClient client, TafelBoemConfig config) {
		this.server = server;
		this.learners = learners;
		this.client = client;
		this.config = config;
	}

	public Learners learners() {
		return learners;
	}

	public QuizClient client() {
		return client;
	}

	/** The parent's switches; GameTests pass their own. */
	public TafelBoemConfig config() {
		return config;
	}

	public boolean isBusy(ServerPlayer player) {
		return sessions.containsKey(player.getUUID());
	}

	public void startFromBlock(ServerPlayer player, BombTier tier, BlockPos bombPos) {
		if (isArmed(bombPos)) {
			return;
		}
		start(player, tier, bombPos, "free", null);
	}

	public @Nullable QuizSession start(ServerPlayer player, BombTier tier, BlockPos bombPos, String context, @Nullable QuizListener listener) {
		if (isBusy(player) || !client.canReceive(player)) {
			return null;
		}

		Fact fact = learners.nextQuestion(player, levelOf(listener));
		QuizSession session = new QuizSession(nextSessionId++, player.getUUID(), fact, tier, bombPos.immutable(),
				player.position(), context, listener);
		sessions.put(player.getUUID(), session);

		RootLock.apply(player);
		BombEffects.fuseStarted(player.level(), bombPos);
		client.send(player, QuizS2C.ask(session.id(), fact.a(), fact.b(), tier.tier()));
		return session;
	}

	public void onClientAction(ServerPlayer player, QuizC2S action) {
		QuizSession session = sessions.get(player.getUUID());
		if (session == null || session.id() != action.sessionId()) {
			return;
		}

		String via = action.via() == QuizC2S.VIA_CLICK ? AnswerRecord.VIA_CLICK : AnswerRecord.VIA_KEY;
		switch (action.action()) {
			case QuizC2S.ANSWER -> {
				if (session.state() == QuizSession.State.ASKING) {
					if (action.value() == session.fact().product()) {
						correct(player, session, via);
					} else {
						miss(player, session, Outcome.WRONG, action.value(), via, 0);
					}
				}
			}
			case QuizC2S.DONT_KNOW -> {
				if (session.state() == QuizSession.State.ASKING) {
					miss(player, session, Outcome.DONT_KNOW, -1, via, action.value());
				}
			}
			case QuizC2S.CHICKEN_OUT -> {
				if (session.state() == QuizSession.State.ASKING) {
					QuizListener listener = session.listener();
					boolean allowed = listener == null || listener.allowChickenOut();
					miss(player, session, allowed ? Outcome.CHICKEN_OUT : Outcome.DONT_KNOW, -1, via, 0);
				}
			}
			case QuizC2S.CORRECTION_DONE -> {
				if (session.state() == QuizSession.State.CORRECTING) {
					finish(player, session);
				}
			}
			default -> {
			}
		}
	}

	private void correct(ServerPlayer player, QuizSession session, String via) {
		long millis = session.elapsedMillis();
		learners.record(player, session.fact(), Outcome.CORRECT, session.fact().product(), millis, session.tier(), session.context(),
				via, 0, levelOf(session.listener()));

		sessions.remove(player.getUUID());
		RootLock.release(player);
		client.send(player, QuizS2C.close(session.id()));
		BombEffects.correctAnswer(player.level(), player);

		QuizListener listener = session.listener();
		if (listener != null) {
			listener.onCorrect(player, session);
			return;
		}

		int escapeTicks = EscapeModel.escapeTicks(session.tier().blastRadius(), millis);
		booms.add(new PendingBoom(player.level(), session.bombPos(), session.tier(), session.fact(), escapeTicks, player.getUUID()));
		actionBar(player, Component.translatable("tafelboem.message.run"));
	}

	private void miss(ServerPlayer player, QuizSession session, Outcome outcome, int answer, String via, int typedDigits) {
		learners.record(player, session.fact(), outcome, answer, session.elapsedMillis(), session.tier(), session.context(),
				via, typedDigits, levelOf(session.listener()));

		ServerLevel level = player.level();
		switch (outcome) {
			case WRONG -> {
				BombEffects.fizzle(level, session.bombPos());
				BombEffects.penalty(level, player, session.tier());
				if (session.tier() == BombTier.KIPPENBOM && config.kippenbomChickens) {
					BombEffects.hoppingChickens(level, player.position(),
							config.countingKippenbom ? ChickenCount.WRONG_ANSWER : BombEffects.WRONG_ANSWER_CHICKENS);
					collectSpectacles(level, player.blockPosition());
				}
			}
			case CHICKEN_OUT -> {
				BombEffects.chickenOut(level, session.bombPos());
				collectSpectacles(level, session.bombPos());
			}
			default -> BombEffects.fizzle(level, session.bombPos());
		}

		QuizListener listener = session.listener();
		if (listener != null) {
			listener.onMiss(player, session, outcome);
		}

		Fact fact = session.fact();
		Tip tip = DerivedFactTips.tipFor(fact);
		int reason = switch (outcome) {
			case WRONG -> QuizS2C.REASON_WRONG;
			case CHICKEN_OUT -> QuizS2C.REASON_CHICKEN_OUT;
			default -> QuizS2C.REASON_DONT_KNOW;
		};
		int tier = session.tier().tier();

		if (config.correctionRetype) {
			// R0 behaviour: stay rooted until the correct fact is typed once.
			session.startCorrecting();
			client.send(player, QuizS2C.correct(session.id(), fact.a(), fact.b(), tier, reason, tip.key(), tip.args()));
			return;
		}

		// The child is free straight away and sees the penalty; the fact follows on the HUD. The
		// re-ask a question later is the real check that it stuck.
		sessions.remove(player.getUUID());
		RootLock.release(player);
		client.send(player, QuizS2C.feedback(session.id(), fact.a(), fact.b(), tier, reason, tip.key(), tip.args()));
	}

	private static FightLevel levelOf(@Nullable QuizListener listener) {
		return listener == null ? FightLevel.CLASSIC : listener.fightLevel();
	}

	private void finish(ServerPlayer player, QuizSession session) {
		sessions.remove(player.getUUID());
		RootLock.release(player);
		client.send(player, QuizS2C.close(session.id()));
	}

	public void onDisconnect(ServerPlayer player) {
		QuizSession session = sessions.remove(player.getUUID());
		if (session != null) {
			RootLock.release(player);
		}
	}

	public void tick() {
		tickSessions();
		tickBooms();
		tickBursts();
		tickSpectacles();
	}

	private void tickSessions() {
		for (Iterator<QuizSession> it = sessions.values().iterator(); it.hasNext(); ) {
			QuizSession session = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(session.playerId());
			if (player == null) {
				it.remove();
				continue;
			}

			RootLock.pin(player, session.anchor());
			int age = session.tickAge();
			int timeout = session.state() == QuizSession.State.ASKING ? ANSWER_TIMEOUT_TICKS : CORRECTION_TIMEOUT_TICKS;
			if (age > timeout) {
				it.remove();
				RootLock.release(player);
				BombEffects.fizzle(player.level(), session.bombPos());
				client.send(player, QuizS2C.close(session.id()));
			} else if (session.state() == QuizSession.State.ASKING && age % 5 == 0) {
				BombEffects.fuseTick(player.level(), session.bombPos(), 100);
			}
		}
	}

	private void tickBooms() {
		for (Iterator<PendingBoom> it = booms.iterator(); it.hasNext(); ) {
			PendingBoom boom = it.next();
			boom.ticksLeft--;
			if (boom.ticksLeft % 2 == 0) {
				BombEffects.fuseTick(boom.level, boom.pos, boom.ticksLeft);
			}

			ServerPlayer player = server.getPlayerList().getPlayer(boom.playerId);
			if (player != null && boom.ticksLeft % 20 == 0 && boom.ticksLeft > 0) {
				actionBar(player, Component.translatable("tafelboem.message.run_countdown", boom.ticksLeft / 20));
			}

			if (boom.ticksLeft <= 0) {
				it.remove();
				if (BombEffects.boom(boom.level, boom.pos, boom.tier) && boom.tier == BombTier.KIPPENBOM) {
					Vec3 center = Vec3.atCenterOf(boom.pos);
					if (config.countingKippenbom) {
						countChickens(boom.level, center, boom.fact, "free play");
					} else {
						BombEffects.flyingChickens(boom.level, center);
					}
				}
				collectSpectacles(boom.level, boom.pos);
			}
		}
	}

	/** The spectacle animals around this spot poof after a minute, like every bomb's. */
	public void collectSpectacles(ServerLevel level, BlockPos pos) {
		for (Entity entity : level.getEntities((Entity) null, new AABB(pos).inflate(3),
				entity -> entity.entityTags().contains(BombEffects.SPECTACLE_TAG))) {
			track(entity, SPECTACLE_LIFETIME_TICKS);
		}
	}

	/**
	 * This spectacle animal poofs after {@code ticks}. Animals that appear after the boom (the counting
	 * Kippenbom's groups, the chase) come here when they spawn: {@link #collectSpectacles} only finds
	 * what is there at the boom. Above {@link #MAX_SPECTACLES} the oldest go first.
	 */
	public void track(Entity entity, int ticks) {
		if (spectacles.stream().anyMatch(spectacle -> spectacle.entity == entity)) {
			return;
		}
		spectacles.add(new Spectacle(entity, ticks));
		while (spectacles.size() > MAX_SPECTACLES) {
			poof(spectacles.removeFirst().entity);
		}
	}

	/**
	 * The counting Kippenbom: one chicken per group flies out, and a second later each pops into its
	 * group, so the kids see a × b as a groups of b.
	 *
	 * @param where "fight" or "free play", for the game log
	 */
	public void countChickens(ServerLevel level, Vec3 at, Fact fact, String where) {
		ChickenCount count = ChickenCount.of(fact);
		List<Chicken> firsts = BombEffects.countingChickens(level, at, count.groups(), count.ringSpeed());
		firsts.forEach(chicken -> track(chicken, COUNTING_LIFETIME_TICKS));
		bursts.add(new ChickenBurst(level, firsts, count.perGroup()));
		TafelBoem.LOGGER.info("TafelBoem chickens: {}", count.describe(where));
	}

	private void tickBursts() {
		for (Iterator<ChickenBurst> it = bursts.iterator(); it.hasNext(); ) {
			ChickenBurst burst = it.next();
			if (--burst.ticksLeft > 0) {
				continue;
			}
			it.remove();
			for (Chicken first : burst.firsts) {
				BombEffects.popChicken(burst.level, first, burst.perGroup).forEach(chicken -> track(chicken, COUNTING_LIFETIME_TICKS));
			}
		}
	}

	/** Chickens and friends poof away after their time so nothing piles up and lags the PC. */
	private void tickSpectacles() {
		for (Iterator<Spectacle> it = spectacles.iterator(); it.hasNext(); ) {
			Spectacle spectacle = it.next();
			if (!spectacle.entity.isAlive()) {
				it.remove();
			} else if (--spectacle.ticksLeft <= 0) {
				poof(spectacle.entity);
				it.remove();
			}
		}
	}

	private static void poof(Entity entity) {
		if (entity.isAlive() && entity.level() instanceof ServerLevel level) {
			BombEffects.poof(level, entity.position());
			entity.discard();
		}
	}

	private boolean isArmed(BlockPos pos) {
		return sessions.values().stream().anyMatch(session -> session.bombPos().equals(pos))
				|| booms.stream().anyMatch(boom -> boom.pos.equals(pos));
	}

	private void actionBar(ServerPlayer player, Component message) {
		client.actionBar(player, message);
	}

	private static final class PendingBoom {
		final ServerLevel level;
		final BlockPos pos;
		final BombTier tier;
		final Fact fact;
		final UUID playerId;
		int ticksLeft;

		PendingBoom(ServerLevel level, BlockPos pos, BombTier tier, Fact fact, int ticksLeft, UUID playerId) {
			this.level = level;
			this.pos = pos;
			this.tier = tier;
			this.fact = fact;
			this.ticksLeft = ticksLeft;
			this.playerId = playerId;
		}
	}

	private static final class ChickenBurst {
		final ServerLevel level;
		final List<Chicken> firsts;
		final int perGroup;
		int ticksLeft = COUNTING_POP_TICKS;

		ChickenBurst(ServerLevel level, List<Chicken> firsts, int perGroup) {
			this.level = level;
			this.firsts = firsts;
			this.perGroup = perGroup;
		}
	}

	private static final class Spectacle {
		final Entity entity;
		int ticksLeft;

		Spectacle(Entity entity, int ticksLeft) {
			this.entity = entity;
			this.ticksLeft = ticksLeft;
		}
	}
}
