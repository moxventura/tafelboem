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
import net.minecraft.world.phys.AABB;

import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.DerivedFactTips;
import nl.jeeninga.tafelboem.core.EscapeModel;
import nl.jeeninga.tafelboem.core.Fact;
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

	private final MinecraftServer server;
	private final Learners learners;
	private final QuizClient client;
	private final Map<UUID, QuizSession> sessions = new HashMap<>();
	private final List<PendingBoom> booms = new ArrayList<>();
	private final List<Spectacle> spectacles = new ArrayList<>();
	private int nextSessionId = 1;

	public SessionManager(MinecraftServer server, Learners learners, QuizClient client) {
		this.server = server;
		this.learners = learners;
		this.client = client;
	}

	public Learners learners() {
		return learners;
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

		Fact fact = learners.nextQuestion(player);
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

		switch (action.action()) {
			case QuizC2S.ANSWER -> {
				if (session.state() == QuizSession.State.ASKING) {
					if (action.value() == session.fact().product()) {
						correct(player, session);
					} else {
						miss(player, session, Outcome.WRONG, action.value());
					}
				}
			}
			case QuizC2S.DONT_KNOW -> {
				if (session.state() == QuizSession.State.ASKING) {
					miss(player, session, Outcome.DONT_KNOW, -1);
				}
			}
			case QuizC2S.CHICKEN_OUT -> {
				if (session.state() == QuizSession.State.ASKING) {
					QuizListener listener = session.listener();
					boolean allowed = listener == null || listener.allowChickenOut();
					miss(player, session, allowed ? Outcome.CHICKEN_OUT : Outcome.DONT_KNOW, -1);
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

	private void correct(ServerPlayer player, QuizSession session) {
		long millis = session.elapsedMillis();
		learners.record(player, session.fact(), Outcome.CORRECT, session.fact().product(), millis, session.tier(), session.context());

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
		booms.add(new PendingBoom(player.level(), session.bombPos(), session.tier(), escapeTicks, player.getUUID()));
		actionBar(player, Component.translatable("tafelboem.message.run"));
	}

	private void miss(ServerPlayer player, QuizSession session, Outcome outcome, int answer) {
		learners.record(player, session.fact(), outcome, answer, session.elapsedMillis(), session.tier(), session.context());
		session.startCorrecting();

		ServerLevel level = player.level();
		switch (outcome) {
			case WRONG -> {
				BombEffects.fizzle(level, session.bombPos());
				BombEffects.penalty(level, player, session.tier());
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
		client.send(player, new QuizS2C(session.id(), QuizS2C.CORRECT, fact.a(), fact.b(), session.tier().tier(),
				reason, tip.key(), tip.args()));
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
				BombEffects.boom(boom.level, boom.pos, boom.tier);
				collectSpectacles(boom.level, boom.pos);
			}
		}
	}

	private void collectSpectacles(ServerLevel level, BlockPos pos) {
		for (Entity entity : level.getEntities((Entity) null, new AABB(pos).inflate(3),
				entity -> entity.entityTags().contains(BombEffects.SPECTACLE_TAG))) {
			if (spectacles.stream().noneMatch(spectacle -> spectacle.entity == entity)) {
				spectacles.add(new Spectacle(entity, SPECTACLE_LIFETIME_TICKS));
			}
		}
	}

	/** Chickens and friends poof away after a minute so nothing piles up and lags the PC. */
	private void tickSpectacles() {
		for (Iterator<Spectacle> it = spectacles.iterator(); it.hasNext(); ) {
			Spectacle spectacle = it.next();
			if (!spectacle.entity.isAlive()) {
				it.remove();
			} else if (--spectacle.ticksLeft <= 0) {
				if (spectacle.entity.level() instanceof ServerLevel level) {
					BombEffects.poof(level, spectacle.entity.position());
				}
				spectacle.entity.discard();
				it.remove();
			}
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
		final UUID playerId;
		int ticksLeft;

		PendingBoom(ServerLevel level, BlockPos pos, BombTier tier, int ticksLeft, UUID playerId) {
			this.level = level;
			this.pos = pos;
			this.tier = tier;
			this.ticksLeft = ticksLeft;
			this.playerId = playerId;
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
