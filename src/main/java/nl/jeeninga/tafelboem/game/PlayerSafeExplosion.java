package nl.jeeninga.tafelboem.game;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;

/**
 * Our booms never hurt players: a correct answer must never hurt the child who gave it, and a
 * sibling on LAN must never be hurt by someone else's bomb.
 */
public final class PlayerSafeExplosion extends ExplosionDamageCalculator {
	/** Knocks players around, harmlessly. */
	public static final PlayerSafeExplosion INSTANCE = new PlayerSafeExplosion(true);
	/** Only a show: the kicked bomb blowing up on Graaf Fout must not push the kids around. */
	public static final PlayerSafeExplosion SHOW = new PlayerSafeExplosion(false);

	private final boolean knockPlayers;

	private PlayerSafeExplosion(boolean knockPlayers) {
		this.knockPlayers = knockPlayers;
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
