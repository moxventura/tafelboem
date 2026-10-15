package nl.jeeninga.tafelboem.boss;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.game.SessionManager;

/**
 * Who is in a fight against Graaf Fout and whose turn it is: the kids who joined, the boss bar and
 * who sees it, and who hears what he says. In an arena, being in it is all it takes; outside one,
 * the fight has a radius around him.
 */
final class Participants {
	static final double ARENA_RADIUS = 32;
	static final double BOSS_BAR_RADIUS = 48;
	/** Outside an arena he pops back to a kid this close; a kid who ran off further stays safe. */
	static final double COME_BACK_RADIUS = 2 * BOSS_BAR_RADIUS;

	private final ServerLevel level;
	private final GraafFout boss;
	private final @Nullable ArenaBox arena;
	private final SessionManager sessions;
	private final ServerBossEvent bossBar;
	private final List<UUID> ids = new ArrayList<>();
	private int turn;

	Participants(ServerLevel level, GraafFout boss, @Nullable ArenaBox arena, SessionManager sessions) {
		this.level = level;
		this.boss = boss;
		this.arena = arena;
		this.sessions = sessions;
		this.bossBar = new ServerBossEvent(UUID.randomUUID(), boss.getDisplayName(), BossEvent.BossBarColor.PURPLE,
				BossEvent.BossBarOverlay.NOTCHED_10);
	}

	/** Everyone who ever joined, in the order they joined. */
	List<UUID> ids() {
		return List.copyOf(ids);
	}

	/** Kids who come near join; the boss bar shows for whoever is near now. */
	void update() {
		for (ServerPlayer player : level.players()) {
			// In an arena, being in it is all it takes; outside one, the boss bar and the fight have a radius.
			boolean near = arena != null ? arena.holds(player.getX(), player.getY(), player.getZ())
					: player.distanceToSqr(boss) < BOSS_BAR_RADIUS * BOSS_BAR_RADIUS;
			boolean joins = arena != null ? near : near && player.distanceToSqr(boss) < ARENA_RADIUS * ARENA_RADIUS;
			if (near && !bossBar.getPlayers().contains(player)) {
				bossBar.addPlayer(player);
			} else if (!near && bossBar.getPlayers().contains(player)) {
				bossBar.removePlayer(player);
			}
			if (joins && !ids.contains(player.getUUID())) {
				ids.add(player.getUUID());
			}
		}
	}

	/**
	 * The kid whose turn it is. Without an arena only kids within the boss bar's reach count; when a
	 * yeet leaves him with nobody in reach, {@link #nearestToComeBackTo} finds the kid he pops back to.
	 */
	@Nullable ServerPlayer nextTarget() {
		List<ServerPlayer> players = ids.stream()
				.map(id -> level.getServer().getPlayerList().getPlayer(id))
				.filter(player -> player != null && player.level() == level)
				.filter(player -> arena != null ? arena.holds(player.getX(), player.getY(), player.getZ())
						: player.distanceToSqr(boss) < BOSS_BAR_RADIUS * BOSS_BAR_RADIUS)
				.filter(this::canBeThrownAt)
				.toList();
		if (players.isEmpty()) {
			return null;
		}
		return players.get(turn++ % players.size());
	}

	/** The nearest kid within {@link #COME_BACK_RADIUS} who could be thrown at, if there is one. */
	@Nullable ServerPlayer nearestToComeBackTo() {
		return ids.stream()
				.map(id -> level.getServer().getPlayerList().getPlayer(id))
				.filter(player -> player != null && player.level() == level)
				.filter(player -> player.distanceToSqr(boss) < COME_BACK_RADIUS * COME_BACK_RADIUS)
				.filter(this::canBeThrownAt)
				.min(Comparator.comparingDouble(player -> player.distanceToSqr(boss)))
				.orElse(null);
	}

	private boolean canBeThrownAt(ServerPlayer player) {
		return player.isAlive() && !player.isSpectator() && !sessions.isBusy(player);
	}

	/**
	 * Everyone who joined and is still in this world, however far away he is. The reward goes to
	 * them: by then he has flown far up into the sky (playtest 2). In an arena, only the kids still
	 * in it.
	 */
	List<ServerPlayer> players() {
		return ids.stream()
				.map(id -> level.getServer().getPlayerList().getPlayer(id))
				.filter(player -> player != null && player.level() == level)
				.filter(player -> arena == null || arena.holds(player.getX(), player.getY(), player.getZ()))
				.toList();
	}

	/** The kids who can see him from where they are, for the yeet-o-meter. */
	List<ServerPlayer> nearBoss() {
		if (arena != null) {
			return players();
		}
		return players().stream()
				.filter(player -> player.distanceToSqr(boss) < ARENA_RADIUS * ARENA_RADIUS)
				.toList();
	}

	/** Graaf Fout speaks, in the chat of everyone who sees his boss bar. */
	void say(Component message) {
		Component line = Component.translatable("tafelboem.boss.says", boss.getDisplayName(), message).withStyle(ChatFormatting.LIGHT_PURPLE);
		for (ServerPlayer player : bossBar.getPlayers()) {
			player.sendSystemMessage(line);
		}
	}

	void showHealth(float fraction) {
		bossBar.setProgress(fraction);
	}

	void hideBossBar() {
		bossBar.removeAllPlayers();
	}
}
