package nl.jeeninga.tafelboem.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.game.BombEffects;

/**
 * A test's own spectacle chickens, told apart by id. GameTests share one level and run side by side,
 * and a fight reaches into the next test's field, so counting the chickens near a spot can count a
 * neighbour's. A chicken is this test's when it appears where this test's chickens come from: next
 * to a spot the test names, on the tick it appears, or (with {@link #followPops}) where one of the
 * test's own chickens just popped into its group.
 *
 * <p>Call {@link #tick} once every tick, in the same {@code onEachTick} as the checks that use it:
 * a test's {@code onEachTick} actions run in no fixed order.
 */
final class OwnChickens {
	/** A new chicken is at most this far from where it came from on the first tick it is seen. */
	static final double SPAWN_RADIUS = 2.5;
	/** A chicken pops into its group within a few ticks of the tick it goes. */
	private static final int POP_TICKS = 3;
	/** Graaf Fout flies up to about 45 blocks, and chickens follow him. */
	private static final double REACH = 64;

	private final GameTestHelper helper;
	private final AABB area;
	private final List<Supplier<Vec3>> spots = new ArrayList<>();
	private final List<Vec3> spotsBefore = new ArrayList<>();
	private boolean followPops;
	/** Every spectacle chicken in the area at the last look, with where it stood. */
	private Map<UUID, Vec3> seen;
	/** The chickens that appeared since the tick before. */
	private Map<UUID, Vec3> fresh = Map.of();
	/** This test's chickens, with where they stand, or stood last. */
	private final Map<UUID, Vec3> own = new LinkedHashMap<>();
	/** This test's chickens that went in the last few ticks, with the ticks since. */
	private final Map<UUID, Integer> gone = new HashMap<>();

	OwnChickens(GameTestHelper helper) {
		this.helper = helper;
		this.area = helper.getBounds().inflate(REACH);
		this.seen = look();
	}

	/** From now on the chickens that appear where one of this test's chickens just went are this test's too. */
	OwnChickens followPops() {
		followPops = true;
		return this;
	}

	/** From now on, every tick, the chickens that appear next to this spot are this test's: where it was a tick ago, or is now. */
	void watch(Supplier<Vec3> spot) {
		spots.add(spot);
		spotsBefore.add(spot.get());
	}

	/** Takes stock, once a tick: which chickens are new, which of this test's went, and which new ones are this test's. */
	void tick() {
		Map<UUID, Vec3> now = look();
		fresh = new LinkedHashMap<>(now);
		fresh.keySet().removeAll(seen.keySet());

		gone.replaceAll((id, ticks) -> ticks + 1);
		gone.values().removeIf(ticks -> ticks > POP_TICKS);
		for (Map.Entry<UUID, Vec3> chicken : own.entrySet()) {
			Vec3 at = now.get(chicken.getKey());
			if (at != null) {
				chicken.setValue(at);
			} else if (seen.containsKey(chicken.getKey())) {
				gone.put(chicken.getKey(), 0);
			}
		}
		if (followPops) {
			claim(at -> gone.keySet().stream().anyMatch(went -> own.get(went).distanceTo(at) <= SPAWN_RADIUS));
		}
		for (int i = 0; i < spots.size(); i++) {
			Vec3 before = spotsBefore.get(i);
			Vec3 spot = spots.get(i).get();
			claim(at -> at.distanceTo(spot) <= SPAWN_RADIUS || at.distanceTo(before) <= SPAWN_RADIUS);
			spotsBefore.set(i, spot);
		}
		seen = now;
	}

	/** Claims the chickens that appeared since the tick before within {@code radius} of {@code at}. */
	List<UUID> claimNew(Vec3 at, double radius) {
		return claim(pos -> pos.distanceTo(at) <= radius);
	}

	/** Runs {@code spawn} and claims the chickens it lets out within {@code radius} of {@code at}. */
	List<UUID> claimDuring(Runnable spawn, Vec3 at, double radius) {
		Set<UUID> before = look().keySet();
		spawn.run();
		List<UUID> claimed = new ArrayList<>();
		look().forEach((id, pos) -> {
			if (!before.contains(id) && pos.distanceTo(at) <= radius && !own.containsKey(id)) {
				own.put(id, pos);
				claimed.add(id);
			}
		});
		// Seen now, so the next tick knows when one of them goes.
		claimed.forEach(id -> seen.put(id, own.get(id)));
		return claimed;
	}

	/** How many of this test's chickens are alive. */
	int alive() {
		return (int) own.keySet().stream()
				.filter(id -> helper.getLevel().getEntity(id) instanceof Chicken chicken && chicken.isAlive())
				.count();
	}

	private List<UUID> claim(Predicate<Vec3> mine) {
		List<UUID> claimed = new ArrayList<>();
		fresh.forEach((id, at) -> {
			if (!own.containsKey(id) && mine.test(at)) {
				own.put(id, at);
				claimed.add(id);
			}
		});
		return claimed;
	}

	private Map<UUID, Vec3> look() {
		Map<UUID, Vec3> found = new LinkedHashMap<>();
		for (Chicken chicken : helper.getLevel().getEntitiesOfClass(Chicken.class, area,
				chicken -> chicken.isAlive() && chicken.entityTags().contains(BombEffects.SPECTACLE_TAG))) {
			found.put(chicken.getUUID(), chicken.position());
		}
		return found;
	}
}
