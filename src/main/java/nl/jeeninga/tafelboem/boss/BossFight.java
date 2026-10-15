package nl.jeeninga.tafelboem.boss;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.block.BombBlock;
import nl.jeeninga.tafelboem.boss.yeet.Yeet;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.Fight;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.FightTally;
import nl.jeeninga.tafelboem.core.Outcome;
import nl.jeeninga.tafelboem.core.YeetPicker;
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
	static final int FIRST_THROW_DELAY = 60;
	static final int COOLDOWN_TICKS = 50;
	/** Long enough for the penalty and the correction on the HUD before the next bomb flies. */
	static final int AFTER_MISS_COOLDOWN = 70;
	static final int FLIGHT_TIMEOUT = 100;
	static final int FINALE_TICKS = 60;
	static final int LOOT_COUNT = 4;
	static final double MAX_THROW_DISTANCE = 16;
	/** A crater never takes the floor this close to a kid. */
	static final double SAFE_FLOOR_RADIUS = 3;
	/**
	 * After the kick the view follows Graaf Fout until he has landed; this is only the safety net
	 * for a kick that never arrives. Landing then shortens it to the time he lies dizzy.
	 */
	public static final int KICK_CAM_MAX_TICKS = 140;
	public static final int LANDED_CAM_TICKS = GraafFout.DIZZY_TICKS;
	/** The final launch goes straight up; follow the start of it, not minutes of empty sky. */
	static final int FINALE_CAM_TICKS = 40;

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
	private final FightLevel fightLevel;
	private final @Nullable ArenaBox arena;
	private final Fight fight;
	private final Map<UUID, FightTally> tallies = new HashMap<>();
	private final Participants participants;
	private Phase phase = Phase.COOLDOWN;
	private int phaseTicks = FIRST_THROW_DELAY;
	private int throwCount;
	private UUID currentTarget;
	private @Nullable UUID lastKicker;
	private final YeetPicker yeets = new YeetPicker(new Random());

	BossFight(ServerLevel level, GraafFout boss, SessionManager sessions, FightLevel fightLevel, @Nullable ArenaBox arena) {
		this.level = level;
		this.boss = boss;
		this.sessions = sessions;
		this.fightLevel = fightLevel;
		this.arena = arena;
		this.fight = Fight.of(fightLevel);
		this.participants = new Participants(level, boss, arena, sessions);
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

	@Override
	public FightLevel fightLevel() {
		return fightLevel;
	}

	/** The arena this fight is in; null for a fight from the summon item. Used from R1 on. */
	public @Nullable ArenaBox arena() {
		return arena;
	}

	public boolean isWon() {
		return fight.isDefeated();
	}

	/** This kid's answers in this fight so far (empty for a kid who hasn't answered). */
	public FightTally tally(UUID kid) {
		return tallies.computeIfAbsent(kid, id -> new FightTally());
	}

	public List<UUID> participants() {
		return participants.ids();
	}

	void start() {
		participants.update();
		participants.say(Component.translatable("tafelboem.boss.graaf_fout.intro"));
	}

	void tick() {
		if (phase == Phase.OVER) {
			return;
		}
		if (!boss.isAlive() && phase != Phase.FINALE) {
			end();
			return;
		}
		if (arena != null && phase != Phase.FINALE && boss.getY() < arena.minY()) {
			flewOut();
		}

		participants.update();
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
		ServerPlayer target = participants.nextTarget();
		if (target == null) {
			// A big yeet can land him out of everyone's reach (P13); outside an arena he then comes back to a kid.
			ServerPlayer kid = arena == null ? participants.nearestToComeBackTo() : null;
			if (kid != null) {
				boss.teleportNear(level, kid.position(), arena);
			}
			cooldown(20);
			return;
		}

		throwCount++;
		// Every third throw he pops up somewhere new, and after a big yeet he comes back to the kids.
		if (throwCount % 3 == 0 || target.distanceToSqr(boss) > MAX_THROW_DISTANCE * MAX_THROW_DISTANCE) {
			boss.teleportNear(level, target.position(), arena);
		}
		BombTier tier = fightLevel.bombForThrow(throwCount);

		boss.getLookControl().setLookAt(target);
		ThrownBomb bomb = ThrownBomb.thrown(level, boss, tier, this, target.getUUID());
		Vec3 start = new Vec3(boss.getX(), boss.getEyeY() - 0.3, boss.getZ());
		Vec3 inFront = target.position().add(boss.position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(1.2));
		bomb.setPos(start);
		bomb.setDeltaMovement(throwVelocity(start, inFront, bomb.getGravity()));
		level.addFreshEntity(bomb);
		level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 1.0F, 0.8F);
		announce(target, tier);

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
		sessions.client().track(target, boss, 0, false);
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
		tally(player.getUUID()).record(Outcome.CORRECT);
		BlockPos pos = session.bombPos();
		if (level.getBlockState(pos).getBlock() instanceof BombBlock) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		ThrownBomb kick = ThrownBomb.kicked(level, player, session.tier(), this, boss);
		kick.setPos(Vec3.atCenterOf(pos));
		lastKicker = player.getUUID();
		level.addFreshEntity(kick);

		// The simple kick: a vanilla swing everyone sees, a whoosh, and a camera bump on the kid's screen.
		player.swing(InteractionHand.MAIN_HAND, true);
		level.playSound(null, pos, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.8F);
		sessions.client().track(player, boss, KICK_CAM_MAX_TICKS, true);

		phase = Phase.KICK_IN_FLIGHT;
		phaseTicks = FLIGHT_TIMEOUT;
	}

	@Override
	public void onMiss(ServerPlayer player, QuizSession session, Outcome outcome) {
		tally(player.getUUID()).record(outcome);
		// Only a typed wrong answer: "?" must stay better than guessing.
		if (outcome == Outcome.WRONG && TafelBoem.config().wrongAnswerDamage) {
			BombEffects.hurt(player, fightLevel.wrongAnswerDamage());
		}
		int a = session.fact().a();
		int b = session.fact().b();
		Component taunt = switch (outcome) {
			case WRONG -> Component.translatable("tafelboem.boss.graaf_fout.taunt.wrong", a, b, a * b);
			case CHICKEN_OUT -> Component.translatable("tafelboem.boss.graaf_fout.taunt.chicken_out");
			default -> Component.translatable("tafelboem.boss.graaf_fout.taunt.dont_know", a, b);
		};
		participants.say(taunt);
		boss.playSound(SoundEvents.EVOKER_CELEBRATE, 1.0F, 1.2F);
		cooldown(AFTER_MISS_COOLDOWN);
	}

	/** Playtest 1: the kids never noticed he threw different bombs, so he says which one is coming. */
	private void announce(ServerPlayer target, BombTier tier) {
		Component name = Component.translatable("block.tafelboem." + tier.id());
		Component warning = Component.translatable("tafelboem.boss.graaf_fout.throws", name);
		sessions.client().actionBar(target, warning.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		if (tier != BombTier.KNALLETJE) {
			participants.say(warning);
		}
	}

	void onKickHit(Vec3 where, BombTier tier) {
		if (phase == Phase.FINALE || phase == Phase.OVER) {
			return;
		}
		// The bigger the bomb, the bigger the bang and the further he flies.
		boolean craters = arena != null && TafelBoem.config().arenaCraters;
		ExplosionDamageCalculator boom = craters
				? PlayerSafeExplosion.sparing(participants.players().stream().map(ServerPlayer::position).toList(), SAFE_FLOOR_RADIUS)
				: PlayerSafeExplosion.SHOW;
		level.explode(null, null, boom, boss.getX(), boss.getY() + 1, boss.getZ(), tier.blastRadius(), false,
				craters ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
		if (tier == BombTier.KIPPENBOM && TafelBoem.config().kippenbomChickens) {
			BombEffects.flyingChickens(level, boss.position());
			sessions.collectSpectacles(level, boss.blockPosition());
		}
		fight.registerHit();
		participants.showHealth(fight.healthFraction());

		if (fight.isDefeated()) {
			boss.finalYeet();
			level.sendParticles(ParticleTypes.FIREWORK, boss.getX(), boss.getY() + 1, boss.getZ(), 200, 1.5, 1.5, 1.5, 0.3);
			level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.HOSTILE, 3.0F, 1.0F);
			participants.say(Component.translatable("tafelboem.boss.graaf_fout.defeated"));
			followKickerFor(FINALE_CAM_TICKS);
			phase = Phase.FINALE;
			phaseTicks = FINALE_TICKS;
			return;
		}

		ServerPlayer kicker = kicker();
		Vec3 kickedFrom = kicker != null ? kicker.position() : where;
		boss.yeet(where, (0.9 + level.getRandom().nextDouble() * 0.5) * tier.yeetPower(), yeets.next(), kickedFrom);
		phase = Phase.REACTING;
		phaseTicks = 20;
	}

	/** The yeet-o-meter: everyone in the fight sees how far (or for the helicopter, how high) he flew. */
	void onYeetLanded(Yeet yeet) {
		if (phase == Phase.FINALE || phase == Phase.OVER) {
			return;
		}
		followKickerFor(LANDED_CAM_TICKS);
		int blocks = yeet.blocks();
		if (blocks < 1) {
			return;
		}
		String key = yeet.measuresHeight() ? "tafelboem.message.yeet_height" : "tafelboem.message.yeet_distance";
		Component message = Component.translatable(key, boss.getDisplayName(), blocks)
				.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
		for (ServerPlayer player : participants.nearBoss()) {
			sessions.client().actionBar(player, message);
		}
	}

	/** Out of the arena and into the void: say how far he got, then he comes straight back. */
	private void flewOut() {
		ServerPlayer kid = kicker() != null ? kicker() : participants.players().stream().findFirst().orElse(null);
		Vec3 near = kid != null ? kid.position() : new Vec3(arena.middleX(), arena.minY() + 3, arena.middleZ());
		int blocks = boss.comeBack(level, near, arena);
		if (blocks < 0) {
			return;
		}
		Component message = Component.translatable("tafelboem.message.yeet_out", boss.getDisplayName(), blocks)
				.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
		for (ServerPlayer player : participants.players()) {
			sessions.client().actionBar(player, message);
		}
		followKickerFor(LANDED_CAM_TICKS);
	}

	/** Keeps the kicker's view on Graaf Fout for a little longer, then lets go. */
	private void followKickerFor(int ticks) {
		ServerPlayer kicker = kicker();
		if (kicker != null) {
			sessions.client().track(kicker, boss, ticks, false);
		}
	}

	private @Nullable ServerPlayer kicker() {
		return lastKicker == null ? null : level.getServer().getPlayerList().getPlayer(lastKicker);
	}

	private void reward() {
		// On the door levels a wrong answer costs hearts, so the reward is food that heals them. The R0.1 fight keeps its bombs.
		Item item = fightLevel != FightLevel.CLASSIC && TafelBoem.config().foodReward ? Items.COOKED_CHICKEN : ModBlocks.bombItem(BombTier.KIPPENBOM);
		Component name = new ItemStack(item).getHoverName();
		for (ServerPlayer player : participants.players()) {
			ItemStack loot = new ItemStack(item, LOOT_COUNT);
			if (!player.getInventory().add(loot)) {
				player.drop(loot, false, false);
			}
			player.sendSystemMessage(Component.translatable("tafelboem.message.loot", LOOT_COUNT, name).withStyle(ChatFormatting.GOLD));
			level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 60, 0.5, 1, 0.5, 0.3);
			level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
	}

	void end() {
		phase = Phase.OVER;
		participants.hideBossBar();
		if (boss.isAlive()) {
			BombEffects.poof(level, boss.position());
			boss.discard();
		}
	}

	private void cooldown(int ticks) {
		phase = Phase.COOLDOWN;
		phaseTicks = ticks;
	}
}
