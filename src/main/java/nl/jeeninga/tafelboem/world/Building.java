package nl.jeeninga.tafelboem.world;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.BossEvent;

import nl.jeeninga.tafelboem.core.BuildCountdown;
import nl.jeeninga.tafelboem.core.BuildKit;

/**
 * One arena's build phase: the boss bar with the countdown (where his health shows in the fight),
 * who rang the bell, and what each kid placed from the kit. Placing is counted with Minecraft's own
 * "used" statistic, from when a kid came in until they left or he came.
 */
public final class Building {
	private final ServerBossEvent bar = new ServerBossEvent(UUID.randomUUID(), Component.empty(), BossEvent.BossBarColor.PURPLE,
			BossEvent.BossBarOverlay.PROGRESS);
	private final Map<UUID, Map<String, Integer>> usedAtJoin = new HashMap<>();
	private final Map<String, Map<String, Integer>> placed = new LinkedHashMap<>();
	private @Nullable String rangBy;
	private String shownClock = "";
	private boolean finished;

	void joined(ServerPlayer kid) {
		usedAtJoin.putIfAbsent(kid.getUUID(), used(kid));
		placed.computeIfAbsent(kid.getPlainTextName(), name -> new TreeMap<>());
	}

	/** Adds what the kid placed since they came in; a kid who comes back is counted again from then. */
	void left(ServerPlayer kid) {
		Map<String, Integer> before = usedAtJoin.remove(kid.getUUID());
		if (before == null) {
			return;
		}
		Map<String, Integer> kidPlaced = placed.computeIfAbsent(kid.getPlainTextName(), name -> new TreeMap<>());
		used(kid).forEach((item, now) -> {
			int more = now - before.getOrDefault(item, 0);
			if (more > 0) {
				kidPlaced.merge(item, more, Integer::sum);
			}
		});
	}

	/** Everything placed, per kid in the order they came; counts the kids still building up to now. */
	Map<String, Map<String, Integer>> placed(List<ServerPlayer> stillIn) {
		stillIn.forEach(this::left);
		return new LinkedHashMap<>(placed);
	}

	/** The countdown in the boss bar of exactly these kids. */
	void show(List<ServerPlayer> kids, BuildCountdown countdown) {
		String clock = countdown.clock();
		if (!clock.equals(shownClock)) {
			shownClock = clock;
			bar.setName(Component.translatable("tafelboem.build.countdown", clock));
			bar.setProgress(countdown.progress());
		}
		for (ServerPlayer shown : List.copyOf(bar.getPlayers())) {
			if (!kids.contains(shown)) {
				bar.removePlayer(shown);
			}
		}
		for (ServerPlayer kid : kids) {
			if (!bar.getPlayers().contains(kid)) {
				bar.addPlayer(kid);
			}
		}
	}

	void hide() {
		bar.removeAllPlayers();
	}

	public boolean isShownTo(ServerPlayer kid) {
		return bar.getPlayers().contains(kid);
	}

	public Component title() {
		return bar.getName();
	}

	public @Nullable String rangBy() {
		return rangBy;
	}

	/** Only the first kid who rang counts: the bell can't bring him any sooner after that. */
	void rungBy(String kid) {
		if (rangBy == null) {
			rangBy = kid;
		}
	}

	/** True the first time: the build line is written once per run. */
	boolean finish() {
		if (finished) {
			return false;
		}
		finished = true;
		return true;
	}

	private static Map<String, Integer> used(ServerPlayer kid) {
		Map<String, Integer> used = new HashMap<>();
		for (String item : BuildKit.PLACEABLE) {
			BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(item))
					.ifPresent(found -> used.put(item, kid.getStats().getValue(Stats.ITEM_USED.get(found))));
		}
		return used;
	}
}
