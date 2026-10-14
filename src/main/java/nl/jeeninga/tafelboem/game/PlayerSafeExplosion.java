package nl.jeeninga.tafelboem.game;

import java.util.List;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Our booms never hurt players: a correct answer must never hurt the child who gave it, and a
 * sibling on LAN must never be hurt by someone else's bomb.
 */
public final class PlayerSafeExplosion extends ExplosionDamageCalculator {
	/** Knocks players around, harmlessly. */
	public static final PlayerSafeExplosion INSTANCE = new PlayerSafeExplosion(true, pos -> false);
	/** Only a show: the kicked bomb blowing up on Graaf Fout must not push the kids around. */
	public static final PlayerSafeExplosion SHOW = new PlayerSafeExplosion(false, pos -> false);

	private final boolean knockPlayers;
	private final Predicate<BlockPos> spared;

	private PlayerSafeExplosion(boolean knockPlayers, Predicate<BlockPos> spared) {
		this.knockPlayers = knockPlayers;
		this.spared = spared;
	}

	/** A show that breaks blocks, but never the floor within {@code radius} blocks of a kid (the safe floor). */
	public static PlayerSafeExplosion sparing(List<Vec3> kids, double radius) {
		double radiusSqr = radius * radius;
		return new PlayerSafeExplosion(false, pos -> kids.stream().anyMatch(kid -> Vec3.atCenterOf(pos).distanceToSqr(kid) < radiusSqr));
	}

	@Override
	public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
		if (spared.test(pos) || level instanceof ServerLevel server && ProtectedAreas.isProtected(server, pos)) {
			return false;
		}
		return super.shouldBlockExplode(explosion, level, pos, state, power);
	}

	@Override
	public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
		return !(entity instanceof Player);
	}

	@Override
	public float getKnockbackMultiplier(Entity entity) {
		return entity instanceof Player && !knockPlayers ? 0.0F : super.getKnockbackMultiplier(entity);
	}
}
