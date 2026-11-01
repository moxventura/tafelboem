package nl.jeeninga.tafelboem.world;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.boss.BossFights;
import nl.jeeninga.tafelboem.boss.YeetRecords;
import nl.jeeninga.tafelboem.command.ArenaSetup;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.BuildCountdown;
import nl.jeeninga.tafelboem.core.BuildPhase;
import nl.jeeninga.tafelboem.core.BuildRecord;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.FightRecord;
import nl.jeeninga.tafelboem.core.FightTally;
import nl.jeeninga.tafelboem.core.Stars;
import nl.jeeninga.tafelboem.game.BombEffects;
import nl.jeeninga.tafelboem.game.ProtectedAreas;
import nl.jeeninga.tafelboem.game.QuizClient;
import nl.jeeninga.tafelboem.log.FightLog;

/**
 * Everything that happens in a TafelBoem world: the hub, the doors, a fight per arena, the stars,
 * the void and the way home.
 */
public final class TafelBoemWorld {
	static final int START_DELAY_TICKS = 60;
	static final int RETURN_DELAY_TICKS = 100;
	static final int DOOR_COOLDOWN_TICKS = 40;
	static final double STARS_DISTANCE = 3;
	/** While building, a kid who climbed or bridged over the wall stays in the run this far from the arena. */
	static final int BUILD_MARGIN = 8;

	private record QueuedDoor(ServerPlayer player, int door) {
	}

	private final ServerLevel level;
	private final WorldLayout layout;
	private final BossFights fights;
	private final FightLog fightLog;
	private final ArenaCatalog catalog;
	private final QuizClient client;
	private final boolean catchEveryone;
	private final BuildPhase buildPhase;
	private final Map<FightLevel, ArenaRun> runs = new EnumMap<>(FightLevel.class);
	private final List<Consumer<BuildRecord>> buildListeners = new ArrayList<>();
	private LongSupplier clock = System::currentTimeMillis;
	private final Map<UUID, QueuedDoor> queuedDoors = new LinkedHashMap<>();
	private final Map<UUID, Integer> doorCooldown = new HashMap<>();
	private final Map<String, int[]> bestStars = new HashMap<>();
	private final Map<String, Integer> bestYeets = new HashMap<>();
	/** Each kid's farthest yeet, from the fights log, kept up to date during the session. */
	private final YeetRecords yeetRecords = new YeetRecords() {
		@Override
		public int best(ServerPlayer kid) {
			return bestYeets.computeIfAbsent(kid.getPlainTextName(),
					name -> fightLog.read(name).stream().mapToInt(FightRecord::farthestYeet).max().orElse(0));
		}

		@Override
		public void beaten(ServerPlayer kid, int blocks) {
			bestYeets.put(kid.getPlainTextName(), Math.max(best(kid), blocks));
		}
	};
	private PlacedArea hub;
	private int ticks;

	/**
	 * @param catchEveryone true in a real TafelBoem world: everyone who falls into the void is caught,
	 *     wherever they are. GameTests share their level with other tests, so they only catch the
	 *     players above their own hub and arenas.
	 * @param buildPhase whether kids build first, and how long; {@link BuildPhase#OFF} is 1.1.2
	 */
	public TafelBoemWorld(ServerLevel level, WorldLayout layout, BossFights fights, FightLog fightLog, ArenaCatalog catalog, QuizClient client,
			boolean catchEveryone, BuildPhase buildPhase) {
		this.level = level;
		this.layout = layout;
		this.fights = fights;
		this.fightLog = fightLog;
		this.catalog = catalog;
		this.client = client;
		this.catchEveryone = catchEveryone;
		this.buildPhase = buildPhase;
	}

	/** On every world load: the world rules, a fresh hub, and the spawn in the middle of its spot. */
	public void setUp() {
		ArenaSetup.applyRules(level);
		placeHub(Hub.TEMPLATE);
		// Now, while nobody plays yet, so walking through a door later doesn't stall the game.
		catalog.clearSlots(level, layout);
		level.setRespawnData(LevelData.RespawnData.of(level.dimension(), hub.standAt(), hub.yaw(), 0F));
		level.getGameRules().set(GameRules.RESPAWN_RADIUS, 0, level.getServer());
	}

	/** @param template {@link Hub#TEMPLATE}; GameTests use their own */
	public PlacedArea placeHub(Identifier template) {
		if (hub != null) {
			// Another hub can take up another box; only the one standing now is protected.
			ProtectedAreas.unprotect(level, hub.bounds());
		}
		hub = Hub.place(level, layout, template);
		return hub;
	}

	public PlacedArea hub() {
		return hub;
	}

	/** Where run and fight times come from: wall time in a real world; GameTests, whose server runs faster than 20 ticks a second, use game ticks. */
	public void useClock(LongSupplier clock) {
		this.clock = clock;
	}

	/** Hears every build phase as it ends: he came, or everyone left. */
	public void onBuildEnded(Consumer<BuildRecord> listener) {
		buildListeners.add(listener);
	}

	public ServerLevel level() {
		return level;
	}

	/**
	 * Whether an entity that just loaded from disk is left over from an earlier session or fight (the
	 * load contract in AGENTS.md). Spectacle animals and old hub frames are, in any world. In a
	 * TafelBoem world so is anything but a player over an arena slot: every arena puts its own
	 * entities in fresh, and the door can't clear what was saved with an unloaded chunk (P19).
	 */
	public static boolean isLeftOver(Entity entity, @Nullable TafelBoemWorld world) {
		if (!entity.isLoadedFromDisk() || entity instanceof Player) {
			return false;
		}
		if (entity.entityTags().contains(BombEffects.SPECTACLE_TAG) || Hub.isLeftOverFrame(entity, world == null ? null : world.hub())) {
			return true;
		}
		return world != null && entity.level() == world.level && world.layout.arenaAt(entity.getX(), entity.getZ()).isPresent();
	}

	public Optional<ArenaRun> run(FightLevel fightLevel) {
		return Optional.ofNullable(runs.get(fightLevel));
	}

	/**
	 * The door block reports a kid while the kid is still being moved, where a teleport can be
	 * undone: only remember it (the latest door per kid) and act at the start of the next tick.
	 */
	public void queueDoor(ServerPlayer player, int door) {
		queuedDoors.put(player.getUUID(), new QueuedDoor(player, door));
	}

	/** A kid walked into a door: 1–3 lead to that level's arena, 0 back to the hub. */
	public void enterDoor(ServerPlayer player, int door) {
		if (player.isSpectator() || player.level() != level
				|| doorCooldown.getOrDefault(player.getUUID(), Integer.MIN_VALUE) > ticks) {
			return;
		}
		ArenaRun fighting = runOf(player).orElse(null);
		if (door == 0 && fighting == null) {
			// An exit door only acts for a kid who is in a fight, so a builder isn't thrown to the hub by their own door.
			return;
		}
		doorCooldown.put(player.getUUID(), ticks + DOOR_COOLDOWN_TICKS);
		if (door == 0) {
			if (!fighting.isReturning() && TafelBoem.config().stars) {
				// Playtest 3 (P28): the kids didn't know that leaving gives no stars.
				client.chat(player, Component.translatable("tafelboem.message.left_no_stars"));
			}
			leave(player, fighting, FightRecord.LEFT);
			teleport(player, hub.standAt(), hub.yaw());
			return;
		}

		FightLevel fightLevel = FightLevel.byNumber(door);
		ArenaRun run = runs.get(fightLevel);
		if (run != null && run.isReturning()) {
			player.sendSystemMessage(Component.translatable("tafelboem.door.busy"));
			return;
		}
		if (run == null) {
			long now = clock.getAsLong();
			if (buildPhase.on()) {
				PlacedArea arena = catalog.placePlain(level, layout, fightLevel);
				BuildCorner.place(level, arena);
				run = new ArenaRun(fightLevel, arena, new BuildCountdown(buildPhase.seconds(fightLevel) * BuildCountdown.TICKS_PER_SECOND), now,
						new Building());
			} else {
				PlacedArea arena = catalog.place(level, layout, fightLevel,
						name -> player.sendSystemMessage(Component.translatable("tafelboem.arena.skipped", name).withStyle(ChatFormatting.GOLD)));
				run = new ArenaRun(fightLevel, arena, new BuildCountdown(START_DELAY_TICKS), now, null);
			}
			runs.put(fightLevel, run);
		}
		run.join(player.getUUID());
		if (run.building() != null && run.fight() == null) {
			run.building().joined(player);
		}
		teleport(player, run.arena().standAt(), run.arena().yaw());
	}

	public void tick() {
		ticks++;
		enterQueuedDoors();
		catchFallingKids();
		for (ArenaRun run : List.copyOf(runs.values())) {
			tick(run);
		}
		if (ticks % 10 == 0) {
			showStarsAtDoors();
		}
	}

	private void enterQueuedDoors() {
		List<QueuedDoor> queued = List.copyOf(queuedDoors.values());
		queuedDoors.clear();
		for (QueuedDoor door : queued) {
			if (!door.player().isRemoved()) {
				enterDoor(door.player(), door.door());
			}
		}
	}

	private void tick(ArenaRun run) {
		if (run.fight() == null && !run.kids().isEmpty()) {
			int call = run.countdown().tick();
			Building building = run.building();
			// Taken before he comes: from then on a kid beyond the wall is out, but what they built still counts.
			List<ServerPlayer> builders = building == null ? List.of() : kidsIn(run);
			if (building != null) {
				building.show(builders, run.countdown());
				if (call > 0) {
					tellRun(run, call == 1 ? Component.translatable("tafelboem.build.one_second")
							: Component.translatable("tafelboem.build.seconds", call));
				}
			}
			if (run.countdown().isOver()) {
				// On top of whatever the kids built in the middle, the way he hovers (GraafFout.hover).
				Vec3 middle = run.arena().middle();
				int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(middle.x), Mth.floor(middle.z));
				Vec3 spot = new Vec3(middle.x, Math.max(middle.y + 1.5, top + 1.5), middle.z);
				BossFight fight = fights.summon(level, spot, run.level(), run.arena().bounds());
				if (fight != null) {
					fight.useYeetRecords(yeetRecords);
				}
				run.start(fight, clock.getAsLong());
				if (building != null && fight != null) {
					building.hide();
					endBuild(run, building.rangBy() != null ? BuildRecord.BELL : BuildRecord.TIME, builders);
				}
			}
		}

		// A kid who is somewhere else now (a command, another dimension) has left the fight.
		for (UUID id : run.kids()) {
			ServerPlayer kid = level.getServer().getPlayerList().getPlayer(id);
			if (kid != null && !stillIn(run, kid)) {
				leave(kid, run, FightRecord.LEFT);
			}
		}

		BossFight fight = run.fight();
		if (fight != null && fight.isOver() && !run.isReturning()) {
			if (!fight.isWon()) {
				// He is gone without a win (a command, say). The run ends, and an exit only works in a run, so the kids go home now.
				for (ServerPlayer kid : kidsIn(run)) {
					leave(kid, run, FightRecord.LEFT);
					teleport(kid, hub.standAt(), hub.yaw());
				}
				removeRun(run);
				return;
			}
			award(run, fight);
			run.returnHomeAt(ticks + RETURN_DELAY_TICKS);
		}
		if (run.isReturning() && ticks >= run.returnAt()) {
			for (ServerPlayer kid : kidsIn(run)) {
				teleport(kid, hub.standAt(), hub.yaw());
			}
			removeRun(run);
			return;
		}
		if (run.kids().isEmpty() && !run.isReturning()) {
			if (fight != null && !fight.isOver()) {
				fights.end(fight);
			}
			removeRun(run);
		}
	}

	/**
	 * A kid used a block. A bell in their own arena, before he came, brings him within 10 s, for
	 * everyone in that arena; any bell there counts, so one the kids moved still rings. Only a click
	 * that rings the bell in vanilla counts: this runs before Minecraft's own use of the block, so a
	 * block placed against the bell or a click on its top must not end the build.
	 */
	public void onUseBlock(Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(player instanceof ServerPlayer kid) || kid.level() != level || kid.isSpectator()) {
			return;
		}
		BlockPos pos = hit.getBlockPos();
		BlockState state = level.getBlockState(pos);
		if (!state.is(Blocks.BELL)) {
			return;
		}
		// Vanilla (ServerPlayerGameMode.useItemOn): a sneaking kid with something in a hand places or uses
		// the item instead of the block, and only the main hand's click uses the block without an item.
		boolean somethingInHand = !kid.getMainHandItem().isEmpty() || !kid.getOffhandItem().isEmpty();
		if (hand != InteractionHand.MAIN_HAND || kid.isSecondaryUseActive() && somethingInHand || !BuildCorner.ringsBell(state, hit)) {
			return;
		}
		ArenaRun run = runOf(kid).orElse(null);
		if (run == null || run.fight() != null || run.building() == null || !run.arena().bounds().containsColumn(pos.getX(), pos.getZ())) {
			return;
		}
		if (run.countdown().ring()) {
			run.building().rungBy(kid.getPlainTextName());
			tellRun(run, Component.translatable("tafelboem.build.bell", kid.getDisplayName(), run.countdown().secondsLeft()));
		}
	}

	/** A line in the chat of the kids in this arena only, never the whole LAN. */
	private void tellRun(ArenaRun run, Component line) {
		Component gold = line.copy().withStyle(ChatFormatting.GOLD);
		for (ServerPlayer kid : kidsIn(run)) {
			client.chat(kid, gold);
		}
	}

	/** The run is over: the countdown's boss bar goes with it. */
	private void removeRun(ArenaRun run) {
		runs.remove(run.level());
		if (run.building() != null) {
			run.building().hide();
			// Everyone left before he came: the build line still goes to the log.
			endBuild(run, BuildRecord.EVERYONE_LEFT, kidsIn(run));
		}
	}

	/** Once per run: how long they built, how it ended, and what each kid placed. */
	private void endBuild(ArenaRun run, String ending, List<ServerPlayer> builders) {
		Building building = run.building();
		if (building == null || !building.finish()) {
			return;
		}
		BuildRecord record = new BuildRecord(run.level().number(), run.buildSeconds(clock.getAsLong()), ending,
				BuildRecord.BELL.equals(ending) ? building.rangBy() : null, run.everyone().size(), building.placed(builders));
		buildListeners.forEach(listener -> listener.accept(record));
	}

	/** Stars and a "won" line for every kid still in the arena; a new best gets a message. */
	private void award(ArenaRun run, BossFight fight) {
		for (ServerPlayer kid : kidsIn(run)) {
			int stars = fight.tally(kid.getUUID()).stars(true);
			int before = bestStars(kid, run.level());
			write(kid, run, FightRecord.WON, stars);
			if (stars > before) {
				bestStars.get(kid.getPlainTextName())[run.level().number()] = stars;
				if (TafelBoem.config().stars) {
					kid.sendSystemMessage(Component.translatable("tafelboem.stars.new_best", "★".repeat(stars),
							Component.translatable("tafelboem.level." + run.level().number())).withStyle(ChatFormatting.GOLD));
				}
			}
		}
	}

	private void leave(ServerPlayer kid, ArenaRun run, String result) {
		// After a win every kid's line is written already; going home early doesn't add a second one.
		if (!run.isReturning()) {
			write(kid, run, result, 0);
		}
		if (run.building() != null && run.fight() == null) {
			run.building().left(kid);
		}
		run.remove(kid.getUUID());
	}

	public void onDisconnect(ServerPlayer player) {
		runOf(player).ifPresent(run -> {
			leave(player, run, FightRecord.DISCONNECTED);
			// Closing the world disconnects everyone and never ticks again, so a build nobody is left in ends here
			// or its build line is lost. A fight that is running still ends through the tick, with his fight line.
			if (run.kids().isEmpty() && run.fight() == null) {
				removeRun(run);
			}
		});
	}

	/** A kid who logs in inside an arena without a running fight goes to the hub. */
	public void onJoin(ServerPlayer player) {
		if (player.level() == level && runOf(player).isEmpty() && layout.arenaAt(player.getX(), player.getZ()).isPresent()) {
			teleport(player, hub.standAt(), hub.yaw());
		}
	}

	private void write(ServerPlayer kid, ArenaRun run, String result, int stars) {
		FightTally tally = run.fight() == null ? new FightTally() : run.fight().tally(kid.getUUID());
		long now = clock.getAsLong();
		// The record's timestamp is a date, not a duration: always wall time.
		fightLog.append(new FightRecord(System.currentTimeMillis(), kid.getPlainTextName(), run.level().number(), run.arena().name(), result,
				tally.answers(), tally.wrong(), tally.dontKnow(), tally.chickenOut(), stars, run.fightSeconds(now), run.everyone().size(),
				TafelBoem.version(), tally.farthestYeet(), run.buildSeconds(now), tally.dodged(), tally.pickedUp(), tally.trapped()));
	}

	/** Falling is never fatal: back to the arena's arrival spot in a fight, otherwise to the hub. */
	private void catchFallingKids() {
		for (ServerPlayer player : level.players()) {
			if (player.getY() >= layout.voidY() || !catchEveryone && !isOurs(player)) {
				continue;
			}
			PlacedArea to = runOf(player).map(ArenaRun::arena).orElse(hub);
			teleport(player, to.standAt(), to.yaw());
		}
	}

	/** Above the hub or an arena slot; in a GameTest, other tests' players are none of our business. */
	private boolean isOurs(ServerPlayer player) {
		return layout.hubSlot().containsColumn(player.getX(), player.getZ()) || layout.arenaAt(player.getX(), player.getZ()).isPresent();
	}

	private void showStarsAtDoors() {
		for (ServerPlayer player : level.players()) {
			if (!hub.bounds().containsColumn(player.getX(), player.getZ())) {
				continue;
			}
			for (FightLevel fightLevel : FightLevel.DOORS) {
				boolean near = hub.doors().getOrDefault(fightLevel.number(), List.of()).stream()
						.anyMatch(door -> Vec3.atCenterOf(door).distanceToSqr(player.position()) <= STARS_DISTANCE * STARS_DISTANCE);
				if (near) {
					Component name = Component.translatable("tafelboem.level." + fightLevel.number());
					client.actionBar(player, TafelBoem.config().stars
							? Component.translatable("tafelboem.door.stars", name, Stars.show(bestStars(player, fightLevel)))
							: name);
					break;
				}
			}
		}
	}

	private int bestStars(ServerPlayer kid, FightLevel fightLevel) {
		int[] stars = bestStars.computeIfAbsent(kid.getPlainTextName(), name -> {
			List<FightRecord> fights = fightLog.read(name);
			return new int[] {0, Stars.best(fights, 1), Stars.best(fights, 2), Stars.best(fights, 3)};
		});
		return stars[fightLevel.number()];
	}

	private List<ServerPlayer> kidsIn(ArenaRun run) {
		return run.kids().stream()
				.map(id -> level.getServer().getPlayerList().getPlayer(id))
				.filter(kid -> kid != null && stillIn(run, kid))
				.toList();
	}

	/**
	 * Over the arena's footprint, in this world. During the build, also within {@link #BUILD_MARGIN}
	 * blocks of it: a kid who climbed or bridged over the wall is still building, and the void catch
	 * brings them back; once he has come, the footprint is the arena.
	 */
	private boolean stillIn(ArenaRun run, ServerPlayer kid) {
		if (kid.level() != level) {
			return false;
		}
		ArenaBox box = run.arena().bounds();
		if (run.building() != null && run.fight() == null) {
			box = box.grown(BUILD_MARGIN);
		}
		return box.containsColumn(kid.getX(), kid.getZ());
	}

	private Optional<ArenaRun> runOf(ServerPlayer player) {
		return runs.values().stream().filter(run -> run.kids().contains(player.getUUID())).findFirst();
	}

	private void teleport(ServerPlayer player, BlockPos to, float yaw) {
		player.teleportTo(level, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, Set.of(), yaw, 0F, true);
		player.resetFallDistance();
	}
}
