package nl.jeeninga.tafelboem.boss;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.registry.ModEntities;

/**
 * All running boss fights on this server.
 */
public final class BossFights {
	private static final double ONE_FIGHT_RADIUS = 64;

	private final SessionManager sessions;
	private final List<BossFight> fights = new ArrayList<>();

	public BossFights(SessionManager sessions) {
		this.sessions = sessions;
	}

	public boolean hasFightNear(ServerLevel level, Vec3 position) {
		return fights.stream().anyMatch(fight -> fight.boss().level() == level
				&& fight.boss().position().distanceToSqr(position) < ONE_FIGHT_RADIUS * ONE_FIGHT_RADIUS);
	}

	public List<BossFight> active() {
		return List.copyOf(fights);
	}

	public boolean summon(ServerLevel level, Vec3 position) {
		if (hasFightNear(level, position)) {
			return false;
		}
		GraafFout boss = ModEntities.GRAAF_FOUT.create(level, EntitySpawnReason.TRIGGERED);
		if (boss == null) {
			return false;
		}
		boss.setPos(position);
		BossFight fight = new BossFight(level, boss, sessions);
		level.addFreshEntity(boss);
		fights.add(fight);
		fight.start();
		return true;
	}

	public void tick() {
		for (BossFight fight : List.copyOf(fights)) {
			fight.tick();
			if (fight.isOver()) {
				fights.remove(fight);
			}
		}
	}

	public void endAll() {
		fights.forEach(BossFight::end);
		fights.clear();
	}
}
