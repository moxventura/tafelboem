package nl.jeeninga.tafelboem.game;

import java.util.List;
import java.util.function.BiPredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.block.BombBlock;

/**
 * Our booms never hurt players: a correct answer must never hurt the child who gave it, and a
 * sibling on LAN must never be hurt by someone else's bomb.
 */
public final class PlayerSafeExplosion extends ExplosionDamageCalculator {
	/** Knocks players around, harmlessly. */
	public static final PlayerSafeExplosion INSTANCE = new PlayerSafeExplosion(true, (pos, state) -> false);
	/** Only a show: the kicked bomb blowing up on Graaf Fout must not push the kids around. */
	public static final PlayerSafeExplosion SHOW = new PlayerSafeExplosion(false, (pos, state) -> false);

	private final boolean knockPlayers;
	private final BiPredicate<BlockPos, BlockState> spared;

	private PlayerSafeExplosion(boolean knockPlayers, BiPredicate<BlockPos, BlockState> spared) {
		this.knockPlayers = knockPlayers;
		this.spared = spared;
	}

	/**
	 * A show that breaks blocks, but never the floor within {@code radius} blocks of a kid (the safe
	 * floor), and never a bomb: a crater sets off no lying bomb (R1.3, no chain reactions).
	 */
	public static PlayerSafeExplosion sparing(List<Vec3> kids, double radius) {
		double radiusSqr = radius * radius;
		return new PlayerSafeExplosion(false, (pos, state) -> state.getBlock() instanceof BombBlock
				|| kids.stream().anyMatch(kid -> Vec3.atCenterOf(pos).distanceToSqr(kid) < radiusSqr));
	}

	@Override
	public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
		if (spared.test(pos, state) || level instanceof ServerLevel server && ProtectedAreas.isProtected(server, pos)) {
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
