package nl.jeeninga.tafelboem.boss.landing;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.LandingStyle;
import nl.jeeninga.tafelboem.core.YeetPose;

/** The Gewone TNT's signature: a soot face with white eyes, and he coughs smoke. */
final class SootLanding extends Landing {
	private static final int FIRST_COUGH = 8;
	private static final int SECOND_COUGH = 22;

	SootLanding(GraafFout boss, Scene scene) {
		super(LandingStyle.SOOT, boss, scene);
	}

	@Override
	protected void begin(ServerLevel level) {
		boss.setYeetPose(YeetPose.SOOT);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getEyeY(), boss.getZ(), 20, 0.3, 0.3, 0.3, 0.02);
	}

	@Override
	protected void act(ServerLevel level) {
		if (ticks() == FIRST_COUGH || ticks() == SECOND_COUGH) {
			Vec3 mouth = boss.getEyePosition().add(Vec3.directionFromRotation(0, boss.getYRot()).scale(0.6)).subtract(0, 0.2, 0);
			level.sendParticles(ParticleTypes.SMOKE, mouth.x, mouth.y, mouth.z, 12, 0.1, 0.1, 0.1, 0.05);
			level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.PANDA_SNEEZE, SoundSource.HOSTILE, 1.0F, 0.6F);
		}
	}
}
