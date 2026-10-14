package nl.jeeninga.tafelboem.world;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.boss.BossFights;
import nl.jeeninga.tafelboem.command.ArenaSetup;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.FightRecord;
import nl.jeeninga.tafelboem.core.FightTally;
import nl.jeeninga.tafelboem.core.Stars;
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

	private record QueuedDoor(ServerPlayer player, int door) {
	}

	private final ServerLevel level;
	private final WorldLayout layout;
	private final BossFights fights;
	private final FightLog fightLog;
	private final ArenaCatalog catalog;
	private final QuizClient client;
	private final Map<FightLevel, ArenaRun> runs = new EnumMap<>(FightLevel.class);
	private final Map<UUID, QueuedDoor> queuedDoors = new LinkedHashMap<>();
	private final Map<UUID, Integer> doorCooldown = new HashMap<>();
	private final Map<String, int[]> bestStars = new HashMap<>();
	private PlacedArea hub;
	private int ticks;

	public TafelBoemWorld(ServerLevel level, WorldLayout layout, BossFights fights, FightLog fightLog, ArenaCatalog catalog, QuizClient client) {
		this.level = level;
		this.layout = layout;
		this.fights = fights;
		this.fightLog = fightLog;
		this.catalog = catalog;
		this.client = client;
	}

	/** On every world load: the world rules, a fresh hub, and the spawn in the middle of its spot. */
	public void setUp() {
		ArenaSetup.applyRules(level);
		placeHub(Hub.TEMPLATE);
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

	public ServerLevel level() {
		return level;
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
			PlacedArea arena = catalog.place(level, layout, fightLevel,
					name -> player.sendSystemMessage(Component.translatable("tafelboem.arena.skipped", name).withStyle(ChatFormatting.GOLD)));
			run = new ArenaRun(fightLevel, arena, ticks + START_DELAY_TICKS, System.currentTimeMillis());
			runs.put(fightLevel, run);
		}
		run.join(player.getUUID());
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
		if (run.fight() == null && ticks >= run.startAt() && !run.kids().isEmpty()) {
			Vec3 spot = run.arena().middle().add(0, 1.5, 0);
			run.start(fights.summon(level, spot, run.level(), run.arena().bounds()));
		}

		// A kid who is somewhere else now (a command, another dimension) has left the fight.
		for (UUID id : run.kids()) {
			ServerPlayer kid = level.getServer().getPlayerList().getPlayer(id);
			if (kid != null && (kid.level() != level || !run.arena().bounds().containsColumn(kid.getX(), kid.getZ()))) {
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
				runs.remove(run.level());
				return;
			}
			award(run, fight);
			run.returnHomeAt(ticks + RETURN_DELAY_TICKS);
		}
		if (run.isReturning() && ticks >= run.returnAt()) {
			for (ServerPlayer kid : kidsIn(run)) {
				teleport(kid, hub.standAt(), hub.yaw());
			}
			runs.remove(run.level());
			return;
		}
		if (run.kids().isEmpty() && !run.isReturning()) {
			if (fight != null && !fight.isOver()) {
				fights.end(fight);
			}
			runs.remove(run.level());
		}
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
		run.remove(kid.getUUID());
	}

	public void onDisconnect(ServerPlayer player) {
		runOf(player).ifPresent(run -> leave(player, run, FightRecord.DISCONNECTED));
	}

	/** A kid who logs in inside an arena without a running fight goes to the hub. */
	public void onJoin(ServerPlayer player) {
		if (player.level() == level && runOf(player).isEmpty() && layout.arenaAt(player.getX(), player.getZ()).isPresent()) {
			teleport(player, hub.standAt(), hub.yaw());
		}
	}

	private void write(ServerPlayer kid, ArenaRun run, String result, int stars) {
		FightTally tally = run.fight() == null ? new FightTally() : run.fight().tally(kid.getUUID());
		long seconds = (System.currentTimeMillis() - run.startedAtMillis()) / 1000;
		fightLog.append(new FightRecord(System.currentTimeMillis(), kid.getPlainTextName(), run.level().number(), run.arena().name(), result,
				tally.answers(), tally.wrong(), tally.dontKnow(), tally.chickenOut(), stars, seconds, run.everyone().size(), TafelBoem.version()));
	}

	/** Falling is never fatal: back to the arena's arrival spot in a fight, otherwise to the hub. */
	private void catchFallingKids() {
		for (ServerPlayer player : level.players()) {
			if (player.getY() >= layout.voidY() || !isOurs(player)) {
				continue;
			}
			PlacedArea to = runOf(player).map(ArenaRun::arena).orElse(hub);
			teleport(player, to.standAt(), to.yaw());
		}
	}

	/** Above the hub or an arena slot: other worlds' players (and other GameTests') are none of our business. */
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
				.filter(kid -> kid != null && kid.level() == level && run.arena().bounds().containsColumn(kid.getX(), kid.getZ()))
				.toList();
	}

	private Optional<ArenaRun> runOf(ServerPlayer player) {
		return runs.values().stream().filter(run -> run.kids().contains(player.getUUID())).findFirst();
	}

	private void teleport(ServerPlayer player, BlockPos to, float yaw) {
		player.teleportTo(level, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, Set.of(), yaw, 0F, true);
		player.resetFallDistance();
	}
}
