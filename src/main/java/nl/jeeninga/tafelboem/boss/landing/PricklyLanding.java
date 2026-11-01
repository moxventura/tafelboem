package nl.jeeninga.tafelboem.boss.landing;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.LandingStyle;

/** Landed in a cactus or a berry bush: he hops about on the spot, arms up, squeaking at every hop. */
final class PricklyLanding extends ArmsUpLanding {
	private static final int HOP_EVERY = 7;
	/** Small hops, so he stays by the trap and away from the kids. */
	private static final double HOP_SIDEWAYS = 0.06;

	PricklyLanding(GraafFout boss, Scene scene) {
		super(LandingStyle.PRICKLED, boss, scene);
	}

	@Override
	protected void act(ServerLevel level) {
		if (ticks() % HOP_EVERY == 1 && boss.onGround()) {
			double angle = boss.getRandom().nextDouble() * Math.PI * 2;
			boss.setDeltaMovement(Math.cos(angle) * HOP_SIDEWAYS, 0.38, Math.sin(angle) * HOP_SIDEWAYS);
			boss.spin(90F);
			level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.EVOKER_HURT, SoundSource.HOSTILE, 1.0F, 1.8F);
			level.sendParticles(ParticleTypes.CRIT, boss.getX(), boss.getY() + 0.4, boss.getZ(), 6, 0.3, 0.2, 0.3, 0.05);
		}
	}
}
