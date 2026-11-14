package nl.jeeninga.tafelboem.boss;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.item.TerugbomItem;
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

	/** The parent's switches the fights play with. */
	public TafelBoemConfig config() {
		return sessions.config();
	}

	public boolean hasFightNear(ServerLevel level, Vec3 position) {
		return fights.stream().anyMatch(fight -> fight.boss().level() == level
				&& fight.boss().position().distanceToSqr(position) < ONE_FIGHT_RADIUS * ONE_FIGHT_RADIUS);
	}

	public List<BossFight> active() {
		return List.copyOf(fights);
	}

	public boolean summon(ServerLevel level, Vec3 position) {
		return summon(level, position, FightLevel.CLASSIC, null) != null;
	}

	/**
	 * Starts a fight at a level. Outside an arena two fights may not be within 64 blocks; an arena
	 * keeps its fight apart by itself.
	 */
	public @Nullable BossFight summon(ServerLevel level, Vec3 position, FightLevel fightLevel, @Nullable ArenaBox arena) {
		if (arena == null && hasFightNear(level, position)) {
			return null;
		}
		GraafFout boss = ModEntities.GRAAF_FOUT.create(level, EntitySpawnReason.TRIGGERED);
		if (boss == null) {
			return null;
		}
		boss.setPos(position);
		BossFight fight = new BossFight(level, boss, sessions, fightLevel, arena);
		level.addFreshEntity(boss);
		fights.add(fight);
		fight.start();
		return fight;
	}

	/** Ends one fight early: he poofs away and nobody gets a reward. */
	public void end(BossFight fight) {
		fight.end();
		fights.remove(fight);
	}

	/**
	 * A kid hit or used a bomb block. When it is a fight's lying bomb, that counts as touching it:
	 * its question comes if one can start now.
	 *
	 * @return true when it is a lying bomb, so nothing else may happen to it: no breaking, no free-play question
	 */
	public boolean touchLyingBomb(ServerPlayer kid, BlockPos pos) {
		for (BossFight fight : fights) {
			if (fight.boss().level() == kid.level() && fight.hasLyingBomb(pos)) {
				fight.pickUp(kid, pos);
				return true;
			}
		}
		return false;
	}

	/** A player is about to break a block. A lying bomb is touched instead of broken, so it never drops as an item. */
	public boolean mayBreak(Player player, BlockPos pos) {
		return !(player instanceof ServerPlayer kid && touchLyingBomb(kid, pos));
	}

	/** A kid right-clicked a bomb block: a lying bomb is touched; any other starts its free-play question. */
	public void useBombBlock(ServerPlayer kid, BlockPos pos, BombTier tier) {
		if (!touchLyingBomb(kid, pos)) {
			sessions.startFromBlock(kid, tier, pos);
		}
	}

	/**
	 * A kid logged in (R1.4). No fight can hold them yet, so adventure mode left over from a fight that never got to
	 * end goes; a kid in a running fight is left alone.
	 */
	public void onJoin(ServerPlayer kid) {
		if (fights.stream().noneMatch(fight -> fight.holds(kid))) {
			// A Terugbom in a pocket outside a running fight goes; so does adventure mode a fight left behind.
			Terugbommen.strip(kid);
			if (config().noBuildInFight) {
				FightGameModes.afterLogin(kid);
			}
		}
	}

	/**
	 * An item entity turned up. A Terugbom a kid dropped (Q, or dragged out of the inventory) can't be dropped: it goes
	 * straight back to them. The ones the fight put on the floor carry its tag and are left alone.
	 */
	public void returnDropped(ItemEntity item) {
		if (!(item.getItem().getItem() instanceof TerugbomItem terugbom) || item.entityTags().contains(Terugbommen.TAG)
				|| !(item.level() instanceof ServerLevel level)) {
			return;
		}
		ServerPlayer kid = item.getOwner() instanceof ServerPlayer owner ? owner : null;
		double nearest = 16;
		for (ServerPlayer player : level.players()) {
			if (kid == null && player.distanceToSqr(item) < nearest) {
				kid = player;
				nearest = player.distanceToSqr(item);
			}
		}
		item.discard();
		if (kid != null) {
			Terugbommen.give(sessions.client(), kid, terugbom.tier());
		}
	}

	/** A kid right-clicked a Terugbom: it flies at him, if that kid is in a running fight. */
	public boolean throwBack(ServerPlayer kid, InteractionHand hand) {
		for (BossFight fight : fights) {
			if (fight.holds(kid) && fight.throwBack(kid, hand)) {
				return true;
			}
		}
		return false;
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
