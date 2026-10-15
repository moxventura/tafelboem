package nl.jeeninga.tafelboem.boss.yeet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.YeetStyle;

/** Boing, boing, boing: bounces three times like a ball, each bounce a bit lower, then he stays down. */
final class BounceYeet extends Yeet {
	private static final int MAX_BOUNCES = 3;
	/** Each bounce is lower and shorter, so even the strongest bomb keeps him near the arena (about 18 blocks). */
	private static final double LAUNCH = 0.6;
	private static final double REBOUND = 0.45;
	private static final double DAMPING = 0.6;

	private int bounces;
	/** Ticks since the launch or the last bounce. */
	private int stageTicks;

	BounceYeet(GraafFout boss, Vec3 away, double power) {
		super(YeetStyle.BOUNCE, boss, away, power);
	}

	@Override
	protected void launch() {
		fling(power * LAUNCH, 0.5 + power * 0.3, false);
	}

	@Override
	protected boolean fly(ServerLevel level) {
		stageTicks++;
		boss.spin(30F);
		if (!boss.onGround() || stageTicks <= 3) {
			return false;
		}
		if (bounces >= MAX_BOUNCES) {
			return true;
		}
		bounces++;
		stageTicks = 0;
		double horizontal = power * REBOUND * Math.pow(DAMPING, bounces);
		boss.setDeltaMovement(away.x * horizontal, 0.55 - 0.1 * bounces, away.z * horizontal);
		level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 1.5F, 0.9F + 0.15F * bounces);
		level.sendParticles(ParticleTypes.CLOUD, boss.getX(), boss.getY() + 0.1, boss.getZ(), 8, 0.4, 0.05, 0.4, 0.02);
		return false;
	}
}
