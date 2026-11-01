package nl.jeeninga.tafelboem.boss.landing;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.LandingStyle;

/**
 * Landed in a cobweb: stuck for 2 s, wriggling left and right with his arms up, threads flying off.
 * The cobweb itself poofed when it caught him (a trap counts once), so the threads show it.
 */
final class WebbedLanding extends ArmsUpLanding {
	WebbedLanding(GraafFout boss, Scene scene) {
		super(LandingStyle.WEBBED, boss, scene);
	}

	@Override
	protected void act(ServerLevel level) {
		Vec3 motion = boss.getDeltaMovement();
		boss.setDeltaMovement(0, Math.min(0, motion.y), 0);
		boss.spin(ticks() % 6 < 3 ? 15F : -15F);
		if (ticks() % 4 == 0) {
			level.sendParticles(ParticleTypes.ITEM_COBWEB, boss.getX(), boss.getY() + 1, boss.getZ(), 4, 0.4, 0.6, 0.4, 0.02);
		}
		if (ticks() % 10 == 1) {
			level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.EVOKER_HURT, SoundSource.HOSTILE, 0.8F, 1.3F);
		}
	}
}
