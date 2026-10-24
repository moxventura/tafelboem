package nl.jeeninga.tafelboem.boss.landing;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.LandingStyle;
import nl.jeeninga.tafelboem.core.YeetPose;

/** Stars circle his head; after the boomerang they circle where his head ended up, face down. */
final class DizzyLanding extends Landing {
	DizzyLanding(GraafFout boss, Scene scene) {
		super(LandingStyle.DIZZY, boss, scene);
	}

	@Override
	protected void act(ServerLevel level) {
		double angle = boss.tickCount * 0.5;
		Vec3 head = new Vec3(boss.getX(), boss.getEyeY() + 0.6, boss.getZ());
		if (boss.yeetPose() == YeetPose.FACEPLANT) {
			head = boss.position().add(Vec3.directionFromRotation(0, boss.getYRot()).scale(1.6)).add(0, 0.4, 0);
		}
		level.sendParticles(ParticleTypes.CRIT, head.x + Math.cos(angle) * 0.5, head.y, head.z + Math.sin(angle) * 0.5, 1, 0, 0, 0, 0);
	}
}
