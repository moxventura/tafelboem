package nl.jeeninga.tafelboem.world;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.core.FightLevel;

/**
 * One level's arena while kids are in it: the arena, the fight once he's there, who is in it now
 * and who took part, and when everyone goes home after a win.
 */
public final class ArenaRun {
	private final FightLevel level;
	private final PlacedArea arena;
	private final int startAt;
	private final long startedAtMillis;
	private final Set<UUID> kids = new LinkedHashSet<>();
	private final Set<UUID> everyone = new HashSet<>();
	private @Nullable BossFight fight;
	private int returnAt = -1;

	ArenaRun(FightLevel level, PlacedArea arena, int startAt, long startedAtMillis) {
		this.level = level;
		this.arena = arena;
		this.startAt = startAt;
		this.startedAtMillis = startedAtMillis;
	}

	public FightLevel level() {
		return level;
	}

	public PlacedArea arena() {
		return arena;
	}

	public @Nullable BossFight fight() {
		return fight;
	}

	public Set<UUID> kids() {
		return Set.copyOf(kids);
	}

	public Set<UUID> everyone() {
		return Set.copyOf(everyone);
	}

	public boolean isReturning() {
		return returnAt >= 0;
	}

	int startAt() {
		return startAt;
	}

	long startedAtMillis() {
		return startedAtMillis;
	}

	int returnAt() {
		return returnAt;
	}

	void join(UUID kid) {
		kids.add(kid);
		everyone.add(kid);
	}

	void remove(UUID kid) {
		kids.remove(kid);
	}

	void start(@Nullable BossFight fight) {
		this.fight = fight;
	}

	void returnHomeAt(int tick) {
		this.returnAt = tick;
	}
}
