package nl.jeeninga.tafelboem.boss;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.block.BombBlock;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.Fight;
import nl.jeeninga.tafelboem.core.Outcome;
import nl.jeeninga.tafelboem.game.BombEffects;
import nl.jeeninga.tafelboem.game.PlayerSafeExplosion;
import nl.jeeninga.tafelboem.game.QuizListener;
import nl.jeeninga.tafelboem.game.QuizSession;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/**
 * One fight against Graaf Fout: he throws bombs at the players in turn, each landed bomb becomes a
 * question, and every correct answer kicks the bomb back and yeets him.
 */
public final class BossFight implements QuizListener {
	static final double ARENA_RADIUS = 32;
	static final double BOSS_BAR_RADIUS = 48;
	static final int FIRST_THROW_DELAY = 60;
	static final int COOLDOWN_TICKS = 50;
	static final int AFTER_MISS_COOLDOWN = 40;
	static final int FLIGHT_TIMEOUT = 100;
	static final int FINALE_TICKS = 60;
	static final int LOOT_COUNT = 4;

	private enum Phase {
		COOLDOWN,
		BOMB_IN_FLIGHT,
		ANSWERING,
		KICK_IN_FLIGHT,
		REACTING,
		FINALE,
		OVER
	}

	private final ServerLevel level;
	private final GraafFout boss;
	private final SessionManager sessions;
	private final Fight fight = new Fight(1);
	private final ServerBossEvent bossBar;
	private final List<UUID> participants = new ArrayList<>();
	private Phase phase = Phase.COOLDOWN;
	private int phaseTicks = FIRST_THROW_DELAY;
	private int throwCount;
	private int turn;
	private UUID currentTarget;

	BossFight(ServerLevel level, GraafFout boss, SessionManager sessions) {
		this.level = level;
		this.boss = boss;
		this.sessions = sessions;
		this.bossBar = new ServerBossEvent(UUID.randomUUID(), boss.getDisplayName(), BossEvent.BossBarColor.PURPLE,
				BossEvent.BossBarOverlay.NOTCHED_10);
		boss.attach(this);
	}

	public GraafFout boss() {
		return boss;
	}

	public int hitsRemaining() {
		return fight.hitsRemaining();
	}

	public boolean isOver() {
		return phase == Phase.OVER;
	}

	void start() {
		updateParticipants();
		say(Component.translatable("tafelboem.boss.graaf_fout.intro"));
	}

	void tick() {
		if (phase == Phase.OVER) {
			return;
		}
		if (!boss.isAlive() && phase != Phase.FINALE) {
			end();
			return;
		}

		updateParticipants();
		phaseTicks--;
		switch (phase) {
			case COOLDOWN -> {
				if (phaseTicks <= 0) {
					throwNextBomb();
				}
			}
			case BOMB_IN_FLIGHT, KICK_IN_FLIGHT -> {
				if (phaseTicks <= 0) {
					cooldown(COOLDOWN_TICKS);
				}
			}
			case ANSWERING -> {
				ServerPlayer target = level.getServer().getPlayerList().getPlayer(currentTarget);
				if (target == null || !sessions.isBusy(target)) {
					// Timed out or disconnected: just carry on.
					cooldown(COOLDOWN_TICKS);
				}
			}
			case REACTING -> {
				if (!boss.isReacting() && phaseTicks <= 0) {
					cooldown(COOLDOWN_TICKS);
				}
			}
			case FINALE -> {
				if (phaseTicks <= 0) {
					reward();
					end();
				}
			}
			default -> {
			}
		}
	}

	private void throwNextBomb() {
		ServerPlayer target = nextTarget();
		if (target == null) {
			cooldown(20);
			return;
		}

		throwCount++;
		if (throwCount % 3 == 0) {
			boss.teleportNear(level, target.position());
		}
		BombTier tier = throwCount % 3 == 2 ? BombTier.GEWONE_TNT : BombTier.KNALLETJE;

		boss.getLookControl().setLookAt(target);
		ThrownBomb bomb = ThrownBomb.thrown(level, boss, tier, this, target.getUUID());
		Vec3 start = new Vec3(boss.getX(), boss.getEyeY() - 0.3, boss.getZ());
		Vec3 inFront = target.position().add(boss.position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(1.2));
		bomb.setPos(start);
		bomb.setDeltaMovement(throwVelocity(start, inFront, bomb.getGravity()));
		level.addFreshEntity(bomb);
		level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 1.0F, 0.8F);

		currentTarget = target.getUUID();
		phase = Phase.BOMB_IN_FLIGHT;
		phaseTicks = FLIGHT_TIMEOUT;
	}

	/**
	 * A drag-free parabola: each tick vy -= g, then pos += v. Lands on {@code to} after {@code ticks}.
	 */
	static Vec3 throwVelocity(Vec3 from, Vec3 to, double gravity) {
		Vec3 delta = to.subtract(from);
		double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
		int ticks = (int) Math.max(15, Math.min(40, horizontal * 1.6 + 10));
		double vy = (delta.y + gravity * ticks * (ticks + 1) / 2.0) / ticks;
		return new Vec3(delta.x / ticks, vy, delta.z / ticks);
	}

	void onBombLanded(ServerLevel level, BlockPos landing, BombTier tier, UUID targetId) {
		ServerPlayer target = level.getServer().getPlayerList().getPlayer(targetId);
		BlockPos pos = findAir(level, landing);
		if (target == null || pos == null || sessions.isBusy(target)) {
			BombEffects.poof(level, Vec3.atCenterOf(landing));
			cooldown(COOLDOWN_TICKS);
			return;
		}

		level.setBlock(pos, ModBlocks.bomb(tier).defaultBlockState(), Block.UPDATE_ALL);
		QuizSession session = sessions.start(target, tier, pos, "fight", this);
		if (session == null) {
			BombEffects.fizzle(level, pos);
			cooldown(COOLDOWN_TICKS);
			return;
		}
		phase = Phase.ANSWERING;
	}

	private static BlockPos findAir(Level level, BlockPos start) {
		BlockPos pos = start;
		for (int i = 0; i < 4; i++) {
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || state.canBeReplaced()) {
				return pos;
			}
			pos = pos.above();
		}
		return null;
	}

	@Override
	public boolean allowChickenOut() {
		return fight.tryChickenOut();
	}

	@Override
	public void onCorrect(ServerPlayer player, QuizSession session) {
		BlockPos pos = session.bombPos();
		if (level.getBlockState(pos).getBlock() instanceof BombBlock) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		ThrownBomb kick = ThrownBomb.kicked(level, player, session.tier(), this, boss);
		kick.setPos(Vec3.atCenterOf(pos));
		level.addFreshEntity(kick);
		level.playSound(null, pos, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.2F, 1.0F);

		phase = Phase.KICK_IN_FLIGHT;
		phaseTicks = FLIGHT_TIMEOUT;
	}

	@Override
	public void onMiss(ServerPlayer player, QuizSession session, Outcome outcome) {
		int a = session.fact().a();
		int b = session.fact().b();
		Component taunt = switch (outcome) {
			case WRONG -> Component.translatable("tafelboem.boss.graaf_fout.taunt.wrong", a, b, a * b);
			case CHICKEN_OUT -> Component.translatable("tafelboem.boss.graaf_fout.taunt.chicken_out");
			default -> Component.translatable("tafelboem.boss.graaf_fout.taunt.dont_know", a, b);
		};
		say(taunt);
		boss.playSound(SoundEvents.EVOKER_CELEBRATE, 1.0F, 1.2F);
		cooldown(AFTER_MISS_COOLDOWN);
	}

	void onKickHit(Vec3 where, BombTier tier) {
		if (phase == Phase.FINALE || phase == Phase.OVER) {
			return;
		}
		level.explode(null, null, PlayerSafeExplosion.INSTANCE, boss.getX(), boss.getY() + 1, boss.getZ(), 1.5F, false,
				Level.ExplosionInteraction.NONE);
		fight.registerHit();
		bossBar.setProgress(fight.healthFraction());

		if (fight.isDefeated()) {
			boss.finalYeet();
			level.sendParticles(ParticleTypes.FIREWORK, boss.getX(), boss.getY() + 1, boss.getZ(), 200, 1.5, 1.5, 1.5, 0.3);
			level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.HOSTILE, 3.0F, 1.0F);
			say(Component.translatable("tafelboem.boss.graaf_fout.defeated"));
			phase = Phase.FINALE;
			phaseTicks = FINALE_TICKS;
			return;
		}

		boss.yeet(where, 0.9 + level.getRandom().nextDouble() * 0.5);
		phase = Phase.REACTING;
		phaseTicks = 20;
	}

	private void reward() {
		for (ServerPlayer player : participantPlayers()) {
			ItemStack loot = new ItemStack(ModBlocks.bombItem(BombTier.KIPPENBOM), LOOT_COUNT);
			if (!player.getInventory().add(loot)) {
				player.drop(loot, false, false);
			}
			player.sendSystemMessage(Component.translatable("tafelboem.message.loot", LOOT_COUNT,
					Component.translatable("block.tafelboem.kippenbom")).withStyle(ChatFormatting.GOLD));
			level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 60, 0.5, 1, 0.5, 0.3);
			level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
	}

	void end() {
		phase = Phase.OVER;
		bossBar.removeAllPlayers();
		if (boss.isAlive()) {
			BombEffects.poof(level, boss.position());
			boss.discard();
		}
	}

	private void cooldown(int ticks) {
		phase = Phase.COOLDOWN;
		phaseTicks = ticks;
	}

	private ServerPlayer nextTarget() {
		List<ServerPlayer> players = participantPlayers().stream()
				.filter(player -> player.isAlive() && !player.isSpectator() && !sessions.isBusy(player))
				.toList();
		if (players.isEmpty()) {
			return null;
		}
		return players.get(turn++ % players.size());
	}

	private List<ServerPlayer> participantPlayers() {
		return participants.stream()
				.map(id -> level.getServer().getPlayerList().getPlayer(id))
				.filter(player -> player != null && player.level() == level && player.distanceToSqr(boss) < ARENA_RADIUS * ARENA_RADIUS)
				.toList();
	}

	private void updateParticipants() {
		for (ServerPlayer player : level.players()) {
			boolean near = player.distanceToSqr(boss) < BOSS_BAR_RADIUS * BOSS_BAR_RADIUS;
			if (near && !bossBar.getPlayers().contains(player)) {
				bossBar.addPlayer(player);
			} else if (!near && bossBar.getPlayers().contains(player)) {
				bossBar.removePlayer(player);
			}
			if (near && player.distanceToSqr(boss) < ARENA_RADIUS * ARENA_RADIUS && !participants.contains(player.getUUID())) {
				participants.add(player.getUUID());
			}
		}
	}

	private void say(Component message) {
		Component line = Component.translatable("tafelboem.boss.says", boss.getDisplayName(), message).withStyle(ChatFormatting.LIGHT_PURPLE);
		for (ServerPlayer player : bossBar.getPlayers()) {
			player.sendSystemMessage(line);
		}
	}
}
