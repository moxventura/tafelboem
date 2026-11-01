package nl.jeeninga.tafelboem.boss.landing;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.LandingStyle;
import nl.jeeninga.tafelboem.core.YeetPose;

/** Landed on a slime block: boing, one more bounce straight up, spinning, and down he comes. */
final class BoingLanding extends Landing {
	/** About 3 blocks up: he is down again well within the landing's 30 ticks. */
	private static final double BOUNCE = 0.7;

	BoingLanding(GraafFout boss, Scene scene) {
		super(LandingStyle.BOING, boss, scene);
	}

	@Override
	protected void begin(ServerLevel level) {
		boss.setYeetPose(YeetPose.NONE);
		boss.setDeltaMovement(0, BOUNCE, 0);
		level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 1.5F, 1.0F);
		level.sendParticles(ParticleTypes.ITEM_SLIME, boss.getX(), boss.getY() + 0.1, boss.getZ(), 12, 0.4, 0.05, 0.4, 0.05);
	}

	@Override
	protected void act(ServerLevel level) {
		boss.spin(20F);
	}
}
