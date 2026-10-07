package nl.jeeninga.tafelboem.game;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;

/**
 * Our booms knock players around but never hurt them: a correct answer must never hurt the child
 * who gave it, and a sibling on LAN must never be hurt by someone else's bomb.
 */
public final class PlayerSafeExplosion extends ExplosionDamageCalculator {
	public static final PlayerSafeExplosion INSTANCE = new PlayerSafeExplosion();

	private PlayerSafeExplosion() {
	}

	@Override
	public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
		return !(entity instanceof Player);
	}
}
