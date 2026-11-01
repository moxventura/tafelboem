package nl.jeeninga.tafelboem.world;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.core.BuildCountdown;
import nl.jeeninga.tafelboem.core.FightLevel;

/**
 * One level's arena while kids are in it: the arena, the countdown until he comes (the build phase,
 * or 1.1.2's 3 s), the fight once he's there, who is in it now and who took part, and when everyone
 * goes home after a win.
 */
public final class ArenaRun {
	private final FightLevel level;
	private final PlacedArea arena;
	private final BuildCountdown countdown;
	private final long startedAtMillis;
	private final @Nullable Building building;
	private final Set<UUID> kids = new LinkedHashSet<>();
	private final Set<UUID> everyone = new HashSet<>();
	private @Nullable BossFight fight;
	private long fightStartedAtMillis = -1;
	private int returnAt = -1;

	/** @param building null without a build phase */
	ArenaRun(FightLevel level, PlacedArea arena, BuildCountdown countdown, long startedAtMillis, @Nullable Building building) {
		this.level = level;
		this.arena = arena;
		this.countdown = countdown;
		this.startedAtMillis = startedAtMillis;
		this.building = building;
	}

	public FightLevel level() {
		return level;
	}

	public PlacedArea arena() {
		return arena;
	}

	public BuildCountdown countdown() {
		return countdown;
	}

	/** The build phase, or null when it is off. Still there after he came, for the logs. */
	public @Nullable Building building() {
		return building;
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

	void start(@Nullable BossFight fight, long nowMillis) {
		this.fight = fight;
		if (fight != null && fightStartedAtMillis < 0) {
			fightStartedAtMillis = nowMillis;
		}
	}

	/** Since he arrived; 0 when he never came. */
	long fightSeconds(long nowMillis) {
		return fightStartedAtMillis < 0 ? 0 : (nowMillis - fightStartedAtMillis) / 1000;
	}

	/** From the first kid's entry until he came, or until now when he hasn't. */
	long buildSeconds(long nowMillis) {
		return ((fightStartedAtMillis < 0 ? nowMillis : fightStartedAtMillis) - startedAtMillis) / 1000;
	}

	void returnHomeAt(int tick) {
		this.returnAt = tick;
	}
}
