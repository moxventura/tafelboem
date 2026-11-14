package nl.jeeninga.tafelboem.boss;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.block.BombBlock;
import nl.jeeninga.tafelboem.boss.landing.Landing;
import nl.jeeninga.tafelboem.boss.yeet.Traps;
import nl.jeeninga.tafelboem.boss.yeet.Yeet;
import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.Aim;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.Dodging;
import nl.jeeninga.tafelboem.core.Fight;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.FightTally;
import nl.jeeninga.tafelboem.core.LandingStyle;
import nl.jeeninga.tafelboem.core.Motion;
import nl.jeeninga.tafelboem.core.Outcome;
import nl.jeeninga.tafelboem.core.Pacing;
import nl.jeeninga.tafelboem.core.ReactionPicker;
import nl.jeeninga.tafelboem.core.Stagger;
import nl.jeeninga.tafelboem.core.ThrowReport;
import nl.jeeninga.tafelboem.core.TrapKind;
import nl.jeeninga.tafelboem.core.TrapPick;
import nl.jeeninga.tafelboem.core.YeetKind;
import nl.jeeninga.tafelboem.core.YeetPose;
import nl.jeeninga.tafelboem.core.YeetPower;
import nl.jeeninga.tafelboem.core.YeetReport;
import nl.jeeninga.tafelboem.game.BombEffects;
import nl.jeeninga.tafelboem.game.PlayerSafeExplosion;
import nl.jeeninga.tafelboem.game.QuizListener;
import nl.jeeninga.tafelboem.game.QuizSession;
import nl.jeeninga.tafelboem.item.TerugbomItem;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/**
 * One fight against Graaf Fout: he throws bombs at the players in turn, each landed bomb becomes a
 * question, and every correct answer kicks the bomb back and yeets him.
 */
public final class BossFight implements QuizListener {
	static final int FIRST_THROW_DELAY = 60;
	static final int COOLDOWN_TICKS = Pacing.COOLDOWN_TICKS;
	/**
	 * Long enough for the penalty and the correction on the HUD (6 s) before the next bomb flies: the
	 * next question would clear it. Reading time, not a show, so a miss stays less fun than a kick.
	 */
	static final int AFTER_MISS_COOLDOWN = 120;
	static final int FLIGHT_TIMEOUT = 100;
	static final int REACTING_RETRY_TICKS = 10;
	static final int RING_POINTS = 24;
	static final float BACK_SPEED = 1.5F;
	/** A kick's reaction is never over before this: the fight only looks at it after a second. */
	static final int REACTION_MIN_TICKS = 20;
	static final int FINALE_TICKS = 60;
	static final int LOOT_COUNT = 4;
	static final double MAX_THROW_DISTANCE = 16;
	/** A crater never takes the floor this close to a kid. */
	static final double SAFE_FLOOR_RADIUS = 3;
	/**
	 * After the kick the view follows Graaf Fout until he has landed; this is only the safety net
	 * for a kick that never arrives. Landing then shortens it to the time he lies dizzy. With
	 * {@code cameraNudge} on, the client lets go much sooner (after half a second at most).
	 */
	public static final int KICK_CAM_MAX_TICKS = 140;
	/**
	 * After flying out he lies dizzy this long; a landing keeps the view on him for as long as it
	 * lasts.
	 */
	public static final int LANDED_CAM_TICKS = LandingStyle.DIZZY.ticks();
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
	private final TafelBoemConfig config;
	private final FightLevel fightLevel;
	private final @Nullable ArenaBox arena;
	private final Fight fight;
	private final Map<UUID, FightTally> tallies = new HashMap<>();
	private final Participants participants;
	private final FightGameModes modes = new FightGameModes();
	private final Terugbommen terugbommen;
	/** The kids alive in the fight last tick: one who is not any more drops out, and loses their Terugbommen. */
	private final Set<UUID> present = new HashSet<>();
	/** Kids told how to throw a Terugbom in this fight, for a fight without {@link Hints}. */
	private final Set<UUID> hinted = new HashSet<>();
	private @Nullable Hints hints;
	private Phase phase = Phase.COOLDOWN;
	private int phaseTicks = FIRST_THROW_DELAY;
	private int throwCount;
	private UUID currentTarget;
	/** The kick that sent him on the flight he is on, or on the last one he flew: it credits the yeet. */
	private @Nullable Kick yeetKick;
	/** Who answers which open question, and what a right answer becomes (R1.4): two questions at once is normal play. */
	private final Map<UUID, QuestionKind> open = new HashMap<>();
	/** How many throws his stagger still costs him (R1.4). */
	private final Stagger stagger = new Stagger();
	/** When each kid last threw a Terugbom, in game ticks. */
	private final Map<UUID, Long> lastBack = new HashMap<>();
	/** The flight he is on was juggled already (or is a super yeet): a second throw-back in it drops back. */
	private boolean flightJuggled;
	/** Each kid's movement over the last 5 ticks (R1.4), read once a tick; he aims ahead of it. */
	private final Map<UUID, Motion> motions = new HashMap<>();
	/** The landing rings in the air: where he guessed the bomb would land, until it does. */
	private final List<Ring> rings = new ArrayList<>();
	private @Nullable ThrowReport lastThrow;

	private static final class Ring {
		final Vec3 centre;
		final BombTier tier;
		int ticksLeft;

		Ring(Vec3 centre, BombTier tier, int ticksLeft) {
			this.centre = centre;
			this.tier = tier;
			this.ticksLeft = ticksLeft;
		}
	}
	private double yeetFactor = 1.0;
	private final YeetMeter meter;
	private final ReactionPicker reactions = new ReactionPicker(new Random());
	private final Deque<LandingStyle> queuedLandings = new ArrayDeque<>();
	/** The pause after this landing, so a kick-to-throw takes as long whatever the landing. */
	private int reactionCooldown = COOLDOWN_TICKS;
	/** The ground at the arena's middle as it was placed, before any crater: the floor's height, for {@link #floor}. */
	private final int middleGround;
	/** Bombs that missed their kid (R1.3), where they lie and which bomb each is. */
	private final Map<BlockPos, BombTier> lying = new LinkedHashMap<>();

	BossFight(ServerLevel level, GraafFout boss, SessionManager sessions, FightLevel fightLevel, @Nullable ArenaBox arena) {
		this.level = level;
		this.boss = boss;
		this.sessions = sessions;
		this.config = sessions.config();
		this.fightLevel = fightLevel;
		this.arena = arena;
		this.fight = Fight.of(fightLevel);
		this.participants = new Participants(level, boss, arena, sessions);
		this.middleGround = arena == null ? 0 : groundAt(arena.middleX(), arena.middleZ());
		this.terugbommen = new Terugbommen(level, sessions.client());
		this.meter = new YeetMeter(boss, participants, sessions.client(), config);
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

	/** Whether this kid is in this fight now: in its arena, or near it when it has none. */
	private boolean juggleOpen() {
		return open.containsValue(QuestionKind.JUGGLE);
	}

	boolean holds(ServerPlayer kid) {
		return participants.players().contains(kid);
	}

	public List<UUID> participants() {
		return participants.ids();
	}

	/** The kids' records in a TafelBoem world; without them (any other world) the meter shows none. */
	public void useYeetRecords(@Nullable YeetRecords records) {
		meter.useRecords(records);
	}

	/** Where the kids' hint memory is: in a TafelBoem world the fights log, else once per fight. */
	public void useHints(@Nullable Hints hints) {
		this.hints = hints;
	}

	/** A Terugbom lies here, glowing. A dodge drops one; the GameTests and the client test put one where they need it. */
	public void dropTerugbom(Vec3 at, BombTier tier) {
		terugbommen.drop(at, tier);
	}

	/** A kid caught a Terugbom (the cap and the slot are settled): the tally, and the first time the hint. */
	private void caught(ServerPlayer kid, BombTier tier) {
		tally(kid.getUUID()).catches();
		boolean first = hints == null ? hinted.add(kid.getUUID()) : !hints.seen(kid);
		if (first) {
			sessions.client().actionBar(kid, Component.translatable("tafelboem.terugbom.hint").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
			if (hints != null) {
				hints.shown(kid);
			}
		}
	}

	/**
	 * The next yeet lands like this instead of a picked landing. GameTests and the client test use it to
	 * see every landing; play never does.
	 */
	public void queueLanding(LandingStyle landing) {
		queuedLandings.add(landing);
	}

	/** Where this fight's dodged bombs lie now. */
	public Set<BlockPos> lyingBombs() {
		return Set.copyOf(lying.keySet());
	}

	/** A bomb lies here as if it missed its kid. Play gets them from dodges; the GameTests and the client test put them where they need them. */
	public void placeLyingBomb(BlockPos pos, BombTier tier) {
		level.setBlock(pos, ModBlocks.bomb(tier).defaultBlockState(), Block.UPDATE_ALL);
		lying.put(pos.immutable(), tier);
		BombEffects.poof(level, Vec3.atCenterOf(pos));
	}

	boolean hasLyingBomb(BlockPos pos) {
		return lying.containsKey(pos);
	}

	/**
	 * The traps a yeet in this fight may land in (R1.3): none with the switch off, and none outside an arena, where
	 * 1.2.0's yeets stay. Public for the GameTests' yeets on cue.
	 */
	public Traps traps() {
		return config.traps && arena != null ? this::trapsAround : Traps.NONE;
	}

	/** Trap blocks with room above them, around this spot, in this fight's arena. */
	private List<TrapPick.Candidate> trapsAround(Vec3 at) {
		List<TrapPick.Candidate> found = new ArrayList<>();
		BlockPos centre = BlockPos.containing(at);
		int reach = (int) Math.ceil(TrapPick.REACH);
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-reach, -3, -reach), centre.offset(reach, 1, reach))) {
			if (!arena.containsColumn(pos.getX() + 0.5, pos.getZ() + 0.5)) {
				continue;
			}
			TrapKind kind = trapAt(pos);
			// Only the top of a stack: he has to fit on or in it.
			if (kind != null && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
				found.add(new TrapPick.Candidate(pos.getX(), pos.getY(), pos.getZ(), kind));
			}
		}
		return found;
	}

	private @Nullable TrapKind trapAt(BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.is(Blocks.CACTUS)) {
			return TrapKind.CACTUS;
		}
		if (state.is(Blocks.SWEET_BERRY_BUSH)) {
			return TrapKind.BERRY_BUSH;
		}
		if (state.is(Blocks.MAGMA_BLOCK)) {
			return TrapKind.MAGMA;
		}
		if (state.is(Blocks.COBWEB)) {
			return TrapKind.COBWEB;
		}
		if (state.is(Blocks.SLIME_BLOCK)) {
			return TrapKind.SLIME;
		}
		// A bomb someone is answering is no trap; only a lying one is.
		return state.getBlock() instanceof BombBlock && lying.containsKey(pos) ? TrapKind.LYING_BOMB : null;
	}

	/**
	 * A kid who left the arena or died has no business with a Terugbom: it goes from their pocket, once, as they drop
	 * out. Not every tick after: a kid who went on to another arena's fight catches their own there.
	 */
	private void stripWhoDroppedOut() {
		Set<UUID> now = new HashSet<>();
		for (ServerPlayer kid : participants.players()) {
			if (kid.isAlive()) {
				now.add(kid.getUUID());
			}
		}
		for (UUID id : present) {
			ServerPlayer kid = now.contains(id) ? null : level.getServer().getPlayerList().getPlayer(id);
			if (kid != null) {
				Terugbommen.strip(kid);
			}
		}
		present.clear();
		present.addAll(now);
	}

	private void recordMotions() {
		for (ServerPlayer kid : participants.players()) {
			motions.computeIfAbsent(kid.getUUID(), id -> new Motion()).record(kid.getX(), kid.getZ());
		}
	}

	/** Every other tick a ring of dust at the radius just inside the blast zone, in the colour of the bomb's trail. */
	private void showRings() {
		rings.removeIf(ring -> --ring.ticksLeft < 0);
		if (level.getGameTime() % 2 != 0) {
			return;
		}
		for (Ring ring : rings) {
			DustParticleOptions dust = new DustParticleOptions(ring.tier.trailColor(), 1.0F);
			for (int i = 0; i < RING_POINTS; i++) {
				double[] point = Dodging.ringPoint(ring.centre.x, ring.centre.z, i, RING_POINTS);
				level.sendParticles(dust, point[0], ring.centre.y + 0.1, point[1], 1, 0, 0, 0, 0);
			}
		}
	}

	/** The rings showing now: the middles of the landing blocks he aimed at. */
	public List<Vec3> landingRings() {
		return rings.stream().map(ring -> ring.centre).toList();
	}

	/**
	 * A ring shows around this spot for this many ticks, or until a bomb lands. His own throws call it, for the flight of
	 * the bomb; the GameTests and the client test put one where they need it.
	 */
	public void showLandingRing(Vec3 centre, BombTier tier, int ticks) {
		rings.add(new Ring(centre, tier, ticks));
	}

	/** The last "TafelBoem throw:" line this fight wrote, or null before the first; for the GameTests. */
	public @Nullable ThrowReport lastThrow() {
		return lastThrow;
	}

	/**
	 * A kid right-clicked a Terugbom: it flies at him like a snowball, at most once a second (R1.4). Not for a kid who is
	 * answering, not in a fight that is decided, and not with dodging off.
	 *
	 * @return whether it flew
	 */
	public boolean throwBack(ServerPlayer kid, InteractionHand hand) {
		ItemStack stack = kid.getItemInHand(hand);
		if (!(stack.getItem() instanceof TerugbomItem terugbom) || !dodging() || decided() || !kid.isAlive() || sessions.isBusy(kid)
				|| !participants.players().contains(kid)) {
			return false;
		}
		long now = level.getGameTime();
		if (now - lastBack.getOrDefault(kid.getUUID(), now - Pacing.BACK_COOLDOWN_TICKS) < Pacing.BACK_COOLDOWN_TICKS) {
			return false;
		}
		lastBack.put(kid.getUUID(), now);
		// Used up, in creative too: a Terugbom is ammunition, not an item to keep.
		kid.setItemInHand(hand, ItemStack.EMPTY);
		ThrownBomb bomb = ThrownBomb.thrownBack(level, kid, terugbom.tier(), this);
		bomb.setPos(kid.getX(), kid.getEyeY() - 0.1, kid.getZ());
		bomb.shootFromRotation(kid, kid.getXRot(), kid.getYRot(), 0.0F, BACK_SPEED, 0.0F);
		level.addFreshEntity(bomb);
		level.playSound(null, kid.blockPosition(), SoundEvents.WITCH_THROW, SoundSource.PLAYERS, 0.6F, 1.4F);
		tally(kid.getUUID()).throwBack();
		return true;
	}

	public void backReached(UUID throwerId, BombTier tier) {
		ServerPlayer thrower = level.getServer().getPlayerList().getPlayer(throwerId);
		if (bounces(thrower)) {
			dropBack(tier);
		} else if (boss.isFlying()) {
			juggle(thrower, tier);
		} else {
			tally(throwerId).backHit();
			boss.stagger(Pacing.STAGGER_TICKS);
			stagger.hit();
			if (ask(thrower, tier, boss.blockPosition(), QuestionKind.TERUG) == null) {
				BombEffects.fizzle(level, boss.blockPosition());
			}
		}
	}

	/**
	 * Why a throw-back can't hit him now: he is beaten, staggered or landing, the kid who threw it is answering something
	 * else, has left or has died, or he flies from a yeet that was juggled already (a super yeet included) or hangs in a
	 * juggle now (two kids at once: the first throw-back counts).
	 */
	private boolean bounces(@Nullable ServerPlayer thrower) {
		return thrower == null || !thrower.isAlive() || sessions.isBusy(thrower) || !participants.players().contains(thrower)
				|| decided() || boss.isStaggered() || boss.isFinale()
				|| boss.isReacting() && !boss.isFlying()
				|| boss.isFlying() && (flightJuggled || juggleOpen());
	}

	/** A throw-back hit him in mid-yeet: he freezes, and the question comes while he hangs there. */
	private void juggle(ServerPlayer thrower, BombTier tier) {
		boss.freeze();
		if (ask(thrower, tier, boss.blockPosition(), QuestionKind.JUGGLE) == null) {
			boss.unfreeze();
			BombEffects.fizzle(level, boss.blockPosition());
			return;
		}
		flightJuggled = true;
		FightTally tally = tally(thrower.getUUID());
		tally.backHit();
		tally.juggle();
	}

	/** It bounces off him and drops below him as a Terugbom anyone can catch. */
	private void dropBack(BombTier tier) {
		terugbommen.drop(floorBelow(boss.position()), tier);
	}

	/**
	 * The arena floor below this spot, where a kid can walk to it: straight below him when that is floor, else the first
	 * floor on the way to the arena's middle and a block further in. Never over the void, and never on top of a wall (a
	 * wall's top is higher than a step above the middle's floor).
	 */
	private Vec3 floorBelow(Vec3 at) {
		if (arena == null) {
			return new Vec3(at.x, groundAt(at.x, at.z), at.z);
		}
		double middleX = arena.middleX();
		double middleZ = arena.middleZ();
		YeetPower.Floor floor = floor();
		double dx = middleX - at.x;
		double dz = middleZ - at.z;
		double distance = Math.hypot(dx, dz);
		for (double step = 0; step < distance; step++) {
			double x = at.x + dx / distance * step;
			double z = at.z + dz / distance * step;
			if (floor.at(x, z)) {
				if (step == 0) {
					return new Vec3(x, groundAt(x, z), z);
				}
				double inX = at.x + dx / distance * Math.min(step + 1, distance);
				double inZ = at.z + dz / distance * Math.min(step + 1, distance);
				return new Vec3(inX, groundAt(inX, inZ), inZ);
			}
		}
		return new Vec3(middleX, groundAt(middleX, middleZ), middleZ);
	}

	/**
	 * This fight's arena floor, column by column: in the arena, with ground above its bottom (not the void), and no
	 * higher than a step above the middle's floor when the fight began (not a wall; a crater there since changes nothing).
	 */
	private YeetPower.Floor floor() {
		return (x, z) -> {
			int ground = groundAt(x, z);
			return arena.containsColumn(x, z) && ground > arena.minY() && ground <= middleGround + 1;
		};
	}

	private int groundAt(double x, double z) {
		return level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
	}

	/** The kid whose answer sent him on the flight he is on, or on his last one; null before the first. For the GameTests. */
	public @Nullable UUID creditedKid() {
		return yeetKick == null ? null : yeetKick.kicker();
	}

	/** The last "TafelBoem yeet:" line this fight wrote, or null before the first; for the GameTests. */
	public @Nullable YeetReport lastYeet() {
		return meter.lastReport();
	}

	/**
	 * He is beaten (the finale) or gone. With two questions open at once, the second kid's answer or a bomb still in
	 * the air arrives after the last hit: it must not start anything new, or his finale never ends.
	 */
	private boolean decided() {
		return phase == Phase.FINALE || phase == Phase.OVER;
	}

	/** Dodging and lying bombs belong to an arena's fight; a fight from the summon item plays as 1.2.0 whatever the switch says. */
	private boolean dodging() {
		return config.dodging && arena != null;
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
		// While a juggle question is open nothing in the fight times out (R1.4): he hangs there, however long the kid thinks.
		// Only while he really hangs: a juggle question he no longer hangs for must never hold the fight still.
		if (juggleOpen() && !boss.isFrozen()) {
			closeJuggle();
		}
		boolean frozen = juggleOpen();
		if (!frozen && arena != null && phase != Phase.FINALE && boss.getY() < arena.minY()) {
			flewOut();
		}

		participants.update();
		if (arena != null && config.noBuildInFight) {
			modes.update(level.getServer(), participants.players());
		}
		stripWhoDroppedOut();
		recordMotions();
		showRings();
		if (dodging()) {
			terugbommen.tick(participants.players(), this::caught, frozen);
			for (ServerPlayer kid : participants.players()) {
				Terugbommen.returnFromMenu(sessions.client(), kid);
			}
			pickUpTouchedBombs();
		}
		if (frozen) {
			return;
		}
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
					cooldown(reactionCooldown);
				} else if (boss.isReacting() && phaseTicks <= -Pacing.STUCK_TICKS) {
					recoverFromStall();
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
		// Never while he flies or lands: a throw then would overlap his reaction (two questions at once can get here).
		if (boss.isReacting()) {
			cooldown(REACTING_RETRY_TICKS);
			return;
		}
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

		// A throw-back staggered him: he skips this throw (R1.4), but never two in a row.
		if (dodging() && stagger.skipsThrow()) {
			cooldown(COOLDOWN_TICKS);
			return;
		}

		throwCount++;
		// Every third throw he pops up somewhere new, and after a big yeet he comes back to the kids.
		if (throwCount % 3 == 0 || target.distanceToSqr(boss) > MAX_THROW_DISTANCE * MAX_THROW_DISTANCE) {
			boss.teleportNear(level, target.position(), arena);
		}
		BombTier tier = fightLevel.bombForThrow(throwCount);

		boss.getLookControl().setLookAt(target);
		Vec3 start = new Vec3(boss.getX(), boss.getEyeY() - 0.3, boss.getZ());
		ThrownBomb bomb;
		if (dodging()) {
			// R1.4: where the kid will be when the bomb lands, times the lead of this level, in a flight of the level's time.
			Motion motion = motions.computeIfAbsent(target.getUUID(), id -> new Motion());
			Aim.Plan plan = Aim.plan(boss.getX(), boss.getZ(), target.getX(), target.getZ(), motion.velX(), motion.velZ(),
					config.aimFor(fightLevel), arena);
			Vec3 aim = new Vec3(plan.x(), target.getY(), plan.z());
			ThrowInfo info = new ThrowInfo(Math.hypot(aim.x - boss.getX(), aim.z - boss.getZ()), plan.ticks(), motion.speed(), target.getX(),
					target.getZ());
			bomb = ThrownBomb.thrown(level, boss, tier, this, target.getUUID(), info);
			bomb.setDeltaMovement(throwVelocity(start, aim, bomb.getGravity(), plan.ticks()));
			if (config.landingRing) {
				BlockPos block = BlockPos.containing(aim);
				showLandingRing(new Vec3(block.getX() + 0.5, aim.y, block.getZ() + 0.5), tier, plan.ticks() + 2);
			}
		} else {
			// 1.2.0: 1.2 blocks in front of where the kid stands.
			bomb = ThrownBomb.thrown(level, boss, tier, this, target.getUUID(), null);
			Vec3 inFront = target.position().add(boss.position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(1.2));
			bomb.setDeltaMovement(throwVelocity(start, inFront, bomb.getGravity()));
		}
		bomb.setPos(start);
		level.addFreshEntity(bomb);
		level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 1.0F, 0.8F);
		announce(target, tier);

		currentTarget = target.getUUID();
		phase = Phase.BOMB_IN_FLIGHT;
		phaseTicks = FLIGHT_TIMEOUT;
	}

	/** A drag-free parabola with today's flight time: each tick vy -= g, then pos += v. Lands on {@code to}. */
	static Vec3 throwVelocity(Vec3 from, Vec3 to, double gravity) {
		Vec3 delta = to.subtract(from);
		return throwVelocity(from, to, gravity, Aim.baseTicks(Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
	}

	/** The same parabola, landing on {@code to} after exactly {@code ticks}. */
	static Vec3 throwVelocity(Vec3 from, Vec3 to, double gravity, int ticks) {
		Vec3 delta = to.subtract(from);
		double vy = (delta.y + gravity * ticks * (ticks + 1) / 2.0) / ticks;
		return new Vec3(delta.x / ticks, vy, delta.z / ticks);
	}

	void onBombLanded(ServerLevel level, BlockPos landing, BombTier tier, UUID targetId, @Nullable ThrowInfo info) {
		rings.clear();
		ServerPlayer target = level.getServer().getPlayerList().getPlayer(targetId);
		BlockPos pos = findAir(level, landing);
		// Nowhere to put it, or he was beaten while it was in the air: it poofs.
		if (pos == null || decided()) {
			poofAndPause(landing);
			return;
		}
		if (!dodging()) {
			// 1.2.0: wherever it lands, its kid answers, as long as they are there and free.
			if (target != null && !sessions.isBusy(target)) {
				root(target, pos, tier);
			} else {
				poofAndPause(landing);
			}
			return;
		}
		boolean free = target != null && target.isAlive() && !sessions.isBusy(target) && participants.players().contains(target);
		boolean hit = free && Dodging.isHit(pos.getX() + 0.5, pos.getZ() + 0.5, target.getX(), target.getZ());
		if (free) {
			countThrow(target, info, hit);
		}
		if (hit) {
			root(target, pos, tier);
			return;
		}
		// The kid ran, or a block stopped it; also for a kid who has gone, died or is answering another bomb.
		terugbommen.drop(Vec3.atBottomCenterOf(restingSpot(pos)), tier);
		if (free) {
			participants.sayTo(target, Component.translatable("tafelboem.boss.graaf_fout.dodged"));
		}
		// Time to catch it, turn round and throw it back.
		pause(Pacing.AFTER_DODGE_COOLDOWN_TICKS);
	}

	/** A throw at a free kid has landed: the tally, the throw line and the report for the GameTests. */
	private void countThrow(ServerPlayer target, @Nullable ThrowInfo info, boolean hit) {
		boolean still = info == null || Dodging.isStill(Math.hypot(target.getX() - info.kidX(), target.getZ() - info.kidZ()));
		tally(target.getUUID()).thrownAt(still, !hit);
		if (info != null) {
			lastThrow = new ThrowReport(target.getPlainTextName(), fightLevel.number(), info.distance(), info.ticks(), info.speed(), still, hit);
			TafelBoem.LOGGER.info("TafelBoem throw: {}", lastThrow.describe());
		}
	}

	/** The bomb is gone without a question, and he throws again after the usual pause. */
	private void poofAndPause(BlockPos landing) {
		BombEffects.poof(level, Vec3.atCenterOf(landing));
		pause(COOLDOWN_TICKS);
	}

	/** A bomb landed on its kid: they are rooted and the question comes. */
	private void root(ServerPlayer target, BlockPos pos, BombTier tier) {
		level.setBlock(pos, ModBlocks.bomb(tier).defaultBlockState(), Block.UPDATE_ALL);
		if (ask(target, tier, pos, QuestionKind.BOMB) == null) {
			BombEffects.fizzle(level, pos);
			pause(COOLDOWN_TICKS);
			return;
		}
		if (betweenThrows()) {
			phase = Phase.ANSWERING;
		}
	}

	/**
	 * Not while he flies or lands from a kick or a Terug! answer that came meanwhile (two questions at once): that
	 * reaction keeps its phase, its stall net and the pause after its landing.
	 */
	private boolean betweenThrows() {
		return phase == Phase.COOLDOWN || phase == Phase.BOMB_IN_FLIGHT || phase == Phase.ANSWERING;
	}

	/** A bomb of his came down: the pause before his next throw, unless a reaction runs (see {@link #betweenThrows}). */
	private void pause(int ticks) {
		if (betweenThrows()) {
			cooldown(ticks);
		}
	}

	/** Asks this kid a question about a bomb; it stays open until it is answered or ends, and the fight remembers what it is for. */
	private @Nullable QuizSession ask(ServerPlayer kid, BombTier tier, BlockPos at, QuestionKind kind) {
		QuizSession session = sessions.start(kid, tier, at, kind.context(), this);
		if (session != null) {
			open.put(kid.getUUID(), kind);
			sessions.client().track(kid, boss, 0, false);
		}
		return session;
	}

	/** Where a bomb that hit a wall in the air comes to rest: straight down onto the first block below, never under the arena floor. */
	private BlockPos restingSpot(BlockPos pos) {
		BlockPos rest = pos;
		int floor = arena != null ? arena.minY() : level.getMinY();
		for (int i = 0; i < 16 && rest.getY() > floor; i++) {
			BlockState below = level.getBlockState(rest.below());
			if (!below.isAir() && !below.canBeReplaced()) {
				break;
			}
			rest = rest.below();
		}
		return rest;
	}

	/** A kid who stands in or next to a lying bomb picks it up. */
	private void pickUpTouchedBombs() {
		// A piston pushed it away, say: it isn't lying there any more.
		lying.keySet().removeIf(pos -> !(level.getBlockState(pos).getBlock() instanceof BombBlock));
		for (ServerPlayer kid : participants.players()) {
			for (BlockPos pos : List.copyOf(lying.keySet())) {
				if (Dodging.touches(pos.getX(), pos.getY(), pos.getZ(), kid.getX(), kid.getY(), kid.getZ()) && pickUp(kid, pos)) {
					break;
				}
			}
		}
	}

	/**
	 * A kid touched a lying bomb: its question, the same rooted flow as a hit, with that bomb's tier.
	 * Not while he is being kicked or yeeted (it comes once his reaction is over), not for a kid
	 * who is answering, and not for a dead kid's body.
	 *
	 * @return whether the question started
	 */
	boolean pickUp(ServerPlayer kid, BlockPos pos) {
		BombTier tier = lying.get(pos);
		if (tier == null || !betweenThrows() || !kid.isAlive() || kid.isSpectator() || !participants.players().contains(kid)) {
			return false;
		}
		if (ask(kid, tier, pos, QuestionKind.BOMB) == null) {
			return false;
		}
		lying.remove(pos);
		tally(kid.getUUID()).pickUp();
		return true;
	}

	/** The fight is over: every lying bomb poofs. */
	private void poofLyingBombs() {
		for (BlockPos pos : lying.keySet()) {
			if (level.getBlockState(pos).getBlock() instanceof BombBlock) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				BombEffects.poof(level, Vec3.atCenterOf(pos));
			}
		}
		lying.clear();
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
		QuestionKind kind = open.remove(player.getUUID());
		if (kind == null) {
			kind = QuestionKind.BOMB;
		}
		FightTally tally = tally(player.getUUID());
		tally.record(Outcome.CORRECT);
		Kick kick = new Kick(player.getUUID(), session.fact(), session.tier(), session.answerMillis(), session.usualMillis(), session.wasFast(),
				tally.streak(), kind.yeetKind());
		if (kind != QuestionKind.BOMB) {
			// The Terug! question: it blows up on him, the same as a kick that has just hit him.
			tally.terug(true);
			sessions.client().track(player, boss, KICK_CAM_MAX_TICKS, true);
			onKickHit(boss.position(), session.tier(), kick);
			return;
		}
		BlockPos pos = session.bombPos();
		if (level.getBlockState(pos).getBlock() instanceof BombBlock) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		ThrownBomb thrown = ThrownBomb.kicked(level, player, session.tier(), this, boss, kick);
		thrown.setPos(Vec3.atCenterOf(pos));
		level.addFreshEntity(thrown);

		// The simple kick: a vanilla swing everyone sees, a whoosh, and a camera bump on the kid's screen.
		player.swing(InteractionHand.MAIN_HAND, true);
		level.playSound(null, pos, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.8F);
		sessions.client().track(player, boss, KICK_CAM_MAX_TICKS, true);

		if (!decided()) {
			phase = Phase.KICK_IN_FLIGHT;
			phaseTicks = FLIGHT_TIMEOUT;
		}
	}

	@Override
	public void onMiss(ServerPlayer player, QuizSession session, Outcome outcome) {
		QuestionKind kind = open.remove(player.getUUID());
		tally(player.getUUID()).record(outcome);
		if (kind != null && kind != QuestionKind.BOMB && outcome == Outcome.WRONG) {
			tally(player.getUUID()).terug(false);
		}
		if (kind == QuestionKind.JUGGLE) {
			// Wrong, or "?", or ESC: no hit, and he falls the normal way.
			boss.unfreeze();
		}
		// Only a typed wrong answer: "?" must stay better than guessing.
		if (outcome == Outcome.WRONG && config.wrongAnswerDamage) {
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
		// Only a question about a bomb he threw restarts his pause; a Terug! answer leaves his rhythm alone, and so does
		// a sibling's miss while he flies or lands.
		if ((kind == null || kind == QuestionKind.BOMB) && phase != Phase.REACTING) {
			cooldown(AFTER_MISS_COOLDOWN);
		}
	}

	/** A question of this fight ended without an answer: the kid timed out, died or disconnected. */
	@Override
	public void onEnded(UUID player, QuizSession session) {
		QuestionKind kind = open.remove(player);
		if (kind == QuestionKind.JUGGLE) {
			// Timed out, died or disconnected: he falls the normal way.
			boss.unfreeze();
		}
		// ANSWERING also notices by itself that nobody is busy; doing it now spares the kids a tick of waiting.
		if (phase == Phase.ANSWERING && player.equals(currentTarget)) {
			cooldown(COOLDOWN_TICKS);
		}
	}

	/**
	 * A sibling's right answer sent him flying while he hung for a juggle question (or he stopped hanging some other
	 * way): that question closes without an answer. No hit, no hearts, no yeet; the juggler is free again, and the
	 * answer log has nothing for it, as for a timeout. The juggle and the hit that reached him stay in the tally.
	 */
	private void closeJuggle() {
		for (Map.Entry<UUID, QuestionKind> question : List.copyOf(open.entrySet())) {
			if (question.getValue() == QuestionKind.JUGGLE) {
				open.remove(question.getKey());
				sessions.close(question.getKey());
			}
		}
	}

	/** The fight is over or decided: every question still open closes, so nobody stays rooted over a bomb that is gone. */
	private void closeOpenQuestions() {
		for (UUID kid : List.copyOf(open.keySet())) {
			sessions.close(kid);
		}
		open.clear();
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

	void onKickHit(Vec3 where, BombTier tier, @Nullable Kick kick) {
		if (decided()) {
			return;
		}
		YeetKind kind = kick == null ? YeetKind.KICK : kick.kind();
		// The bigger the bomb, the bigger the bang and the further he flies.
		boom(new Vec3(boss.getX(), boss.getY() + 1, boss.getZ()), tier);
		if (tier == BombTier.KIPPENBOM && config.kippenbomChickens) {
			// A juggle's chickens come down at the arena floor, not in the air where he hangs.
			Vec3 chickensAt = kind == YeetKind.SUPER ? floorBelow(boss.position()).add(0, 1, 0) : boss.position();
			if (config.countingKippenbom && kick != null) {
				sessions.countChickens(level, chickensAt, kick.fact(), "fight");
			} else {
				BombEffects.flyingChickens(level, chickensAt);
				sessions.collectSpectacles(level, BlockPos.containing(chickensAt));
			}
		}
		if (hit()) {
			return;
		}
		if (kind == YeetKind.SUPER) {
			// The super yeet's own sound and sparks.
			level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 2.0F, 1.0F);
			level.sendParticles(ParticleTypes.FIREWORK, boss.getX(), boss.getY() + 1, boss.getZ(), 80, 0.8, 0.8, 0.8, 0.2);
		}
		ServerPlayer kicker = kick == null ? null : level.getServer().getPlayerList().getPlayer(kick.kicker());
		Vec3 kickedFrom = kicker != null ? kicker.position() : where;
		yeetKick = kick;
		yeetFactor = kick == null ? 1.0 : kick.factor(config.yeetBoost);
		double wanted = YeetPower.wanted(0.9 + level.getRandom().nextDouble() * 0.5, tier.yeetPower(), yeetFactor);
		// A kicked bomb sends him away from where it hit; a throw-back or a super yeet from the kid who threw it, or through
		// the middle when that has the room he needs (R1.4).
		Vec3 from = kind == YeetKind.KICK ? where : kickedFrom;
		double power = wanted;
		if (kind != YeetKind.KICK && arena != null) {
			Vec3 away = boss.position().subtract(kickedFrom).multiply(1, 0, 1);
			if (away.lengthSqr() < 1.0E-4) {
				away = new Vec3(0, 0, 1);
			}
			Vec3 toMiddle = new Vec3(arena.middleX() - boss.getX(), 0, arena.middleZ() - boss.getZ());
			YeetPower.Floor floor = floor();
			double roomAway = YeetPower.room(boss.getX(), boss.getZ(), away.x, away.z, floor);
			// Right at the middle there is no way through it: away is the only way.
			double roomThroughTheMiddle = toMiddle.lengthSqr() < 1 ? 0 : YeetPower.room(boss.getX(), boss.getZ(), toMiddle.x, toMiddle.z, floor);
			YeetPower.Way way = YeetPower.way(wanted, roomAway, roomThroughTheMiddle);
			Vec3 direction = (way.throughTheMiddle() ? toMiddle : away).normalize();
			from = boss.position().subtract(direction);
			power = YeetPower.power(kind, wanted, way.room());
		}
		reactionCooldown = COOLDOWN_TICKS;
		// A super yeet is the end of a juggle: it can't be juggled again. Every other new flight can be, once.
		flightJuggled = kind == YeetKind.SUPER;
		// A sibling's right answer wins over an open juggle: he flies off on it, so that juggle question closes.
		closeJuggle();
		boss.yeet(from, power, reactions.nextFlight(arena != null, config.yeetCatalogue, kind), kickedFrom, traps());
		phase = Phase.REACTING;
		phaseTicks = REACTION_MIN_TICKS;
	}

	/** Our safe boom: never hurts a kid; in an arena with craters on, it breaks blocks, never the floor by a kid. */
	private void boom(Vec3 at, BombTier tier) {
		boolean craters = arena != null && config.arenaCraters;
		ExplosionDamageCalculator calculator = craters
				? PlayerSafeExplosion.sparing(participants.players().stream().map(ServerPlayer::position).toList(), SAFE_FLOOR_RADIUS)
				: PlayerSafeExplosion.SHOW;
		level.explode(null, null, calculator, at.x, at.y, at.z, tier.blastRadius(), false,
				craters ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
	}

	/**
	 * One hit, from a kick or a trap: the health bar goes down, and the last one starts the finale.
	 *
	 * @return whether that beat him
	 */
	private boolean hit() {
		fight.registerHit();
		participants.showHealth(fight.healthFraction());
		if (!fight.isDefeated()) {
			return false;
		}
		// One last enormous yeet into the sky, and the reward after the finale. A question still open is about a
		// Graaf Fout who is gone: it closes now, a juggle's too, so nobody stays rooted and nothing holds the finale.
		closeOpenQuestions();
		boss.finalYeet();
		level.sendParticles(ParticleTypes.FIREWORK, boss.getX(), boss.getY() + 1, boss.getZ(), 200, 1.5, 1.5, 1.5, 0.3);
		level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.HOSTILE, 3.0F, 1.0F);
		participants.say(Component.translatable("tafelboem.boss.graaf_fout.defeated"));
		followKickerFor(FINALE_CAM_TICKS);
		phase = Phase.FINALE;
		phaseTicks = FINALE_TICKS;
		return true;
	}

	/** He is down: pick how he lands (a trap decides for itself), keep the view on him for that long, and show the yeet-o-meter. */
	void onYeetLanded(Yeet yeet) {
		if (decided()) {
			return;
		}
		TrapPick.Candidate trap = yeet.caught();
		LandingStyle landing = trap != null ? trap.kind().reaction() : queuedLandings.poll();
		if (trap != null) {
			// A boomerang's faceplant mustn't carry on into the trap's reaction.
			boss.setYeetPose(YeetPose.NONE);
			reactions.played(landing);
		} else if (landing == null) {
			landing = reactions.nextLanding(yeetKick == null ? BombTier.KNALLETJE : yeetKick.tier(), yeet.style(), config.yeetCatalogue,
					yeetKick != null && config.bombSignatures && (yeetKick.tier() != BombTier.KIPPENBOM || config.kippenbomChickens));
		} else {
			reactions.played(landing);
		}
		boss.startLanding(landing, scene());
		reactionCooldown = Pacing.cooldownAfter(landing);
		followKickerFor(landing.ticks());
		farthest(yeet);
		// A trap's landing is the trap's, never a signature: a lying bomb's soot included.
		meter.landed(yeet, false, yeetKick, landing.id(), trap == null && landing.isSignature(), yeetFactor,
				trap == null ? null : trap.kind().id());
		if (trap != null) {
			caught(trap);
		}
	}

	/** He landed in a trap the kids built: the trap goes, so it counts once, and the kick that sent him gets one more hit. */
	private void caught(TrapPick.Candidate trap) {
		BlockPos pos = new BlockPos(trap.x(), trap.y(), trap.z());
		if (trap.kind() == TrapKind.LYING_BOMB) {
			// A kid may have picked it up during the hop: then it is their question's bomb, and stays.
			BombTier tier = lying.remove(pos);
			if (tier != null) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				boom(Vec3.atCenterOf(pos), tier);
			}
		} else {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			BombEffects.poof(level, Vec3.atCenterOf(pos));
		}
		if (trap.kind().isPrickly()) {
			participants.say(Component.translatable("tafelboem.boss.graaf_fout.trap.prickly"));
		}
		if (yeetKick != null) {
			tally(yeetKick.kicker()).trap(trap.kind());
		}
		hit();
	}

	/** Out of the arena and into the void: say how far he got, then he comes straight back. */
	private void flewOut() {
		Yeet cut = boss.comeBack(level, whereToComeBack(), arena);
		if (cut == null) {
			return;
		}
		// He comes back dizzy: that is his landing this time.
		reactions.played(LandingStyle.DIZZY);
		reactionCooldown = Pacing.cooldownAfter(LandingStyle.DIZZY);
		farthest(cut);
		meter.landed(cut, true, yeetKick, LandingStyle.DIZZY.id(), false, yeetFactor, null);
		followKickerFor(LANDED_CAM_TICKS);
	}

	/** Where he pops back to: next to the kicker, else the first kid, else the arena's middle, else where he is. */
	private Vec3 whereToComeBack() {
		ServerPlayer kid = kicker() != null ? kicker() : participants.players().stream().findFirst().orElse(null);
		if (kid != null) {
			return kid.position();
		}
		return arena != null ? new Vec3(arena.middleX(), arena.minY() + 3, arena.middleZ()) : boss.position();
	}

	/**
	 * Safety net: he has been reacting far longer than any yeet or landing lasts, so he stopped being
	 * ticked (an entity in a chunk nobody simulates). Bring him back next to a kid and carry on; it
	 * wasn't a real landing, so no meter, no record.
	 */
	private void recoverFromStall() {
		TafelBoem.LOGGER.warn("TafelBoem stuck: boss={} yeet={} landing={} waited={}", boss.position(), boss.yeetStyle(), boss.landingStyle(),
				REACTION_MIN_TICKS - phaseTicks);
		Vec3 near = whereToComeBack();
		if (boss.comeBack(level, near, arena) != null) {
			reactions.played(LandingStyle.DIZZY);
		}
		cooldown(COOLDOWN_TICKS);
	}

	/** Where a landing happens: this fight's arena, its kids, and the session's poof timers. */
	private Landing.Scene scene() {
		return new Landing.Scene(arena, () -> participants.players().stream().map(ServerPlayer::position).toList(), sessions::track);
	}

	/** The kicker's farthest yeet in this fight, for the fights log; helicopter heights don't count. */
	/** The kicker's farthest yeet of this kind in this fight, for the fights log; helicopter heights don't count. */
	private void farthest(Yeet yeet) {
		if (yeetKick != null && !yeet.measuresHeight()) {
			tally(yeetKick.kicker()).yeeted(yeetKick.kind(), yeet.blocks());
		}
	}

	/** Keeps the kicker's view on Graaf Fout for a little longer, then lets go. */
	private void followKickerFor(int ticks) {
		ServerPlayer kicker = kicker();
		if (kicker != null) {
			sessions.client().track(kicker, boss, ticks, false);
		}
	}

	private @Nullable ServerPlayer kicker() {
		return yeetKick == null ? null : level.getServer().getPlayerList().getPlayer(yeetKick.kicker());
	}

	private void reward() {
		// On the door levels a wrong answer costs hearts, so the reward is food that heals them. The R0.1 fight keeps its bombs.
		Item item = fightLevel != FightLevel.CLASSIC && config.foodReward ? Items.COOKED_CHICKEN : ModBlocks.bombItem(BombTier.KIPPENBOM);
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
		modes.restoreAll(level.getServer());
		closeOpenQuestions();
		rings.clear();
		poofLyingBombs();
		terugbommen.poofAll();
		// Only the kids still in this fight: one who dropped out lost theirs then, and may be catching in another fight.
		for (UUID id : present) {
			ServerPlayer kid = level.getServer().getPlayerList().getPlayer(id);
			if (kid != null) {
				Terugbommen.strip(kid);
			}
		}
		for (ServerPlayer kid : participants.players()) {
			Terugbommen.strip(kid);
		}
		if (boss.isAlive()) {
			// Ending the landing here sends a chase's chickens off with him.
			boss.endLanding();
			BombEffects.poof(level, boss.position());
			boss.discard();
		}
	}

	private void cooldown(int ticks) {
		if (decided()) {
			return;
		}
		phase = Phase.COOLDOWN;
		phaseTicks = ticks;
	}
}
