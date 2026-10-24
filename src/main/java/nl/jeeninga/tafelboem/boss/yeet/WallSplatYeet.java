package nl.jeeninga.tafelboem.boss.yeet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.YeetPose;
import nl.jeeninga.tafelboem.core.YeetStyle;

/**
 * Flies low and fast, slaps flat against the first wall he hits, sticks there for a moment and
 * slides down. With no wall in the way he just falls, like the classic yeet.
 */
final class WallSplatYeet extends Yeet {
	private static final int STUCK_TICKS = 12;
	private static final double SLIDE = 0.12;
	/** Pressed against the wall while he sticks and slides, so he doesn't drift off it. */
	private static final double PRESS = 0.05;
	/** Ticks since he hit the wall; -1 while he is still flying. */
	private int stuck = -1;

	WallSplatYeet(GraafFout boss, Vec3 away, double power) {
		super(YeetStyle.WALL_SPLAT, boss, away, power);
	}

	@Override
	protected void launch() {
		fling(power * 0.9, 0.3, false);
	}

	@Override
	protected boolean fly(ServerLevel level) {
		if (stuck < 0) {
			if (boss.horizontalCollision) {
				splat(level);
				return false;
			}
			boss.spin(30F);
			return boss.onGround() && ticks() > 5;
		}
		stuck++;
		if (stuck <= STUCK_TICKS) {
			boss.setDeltaMovement(away.scale(PRESS));
			return false;
		}
		boss.setDeltaMovement(away.x * PRESS, -SLIDE, away.z * PRESS);
		return boss.onGround();
	}

	private void splat(ServerLevel level) {
		stuck = 0;
		boss.setNoGravity(true);
		boss.face(away);
		boss.setYeetPose(YeetPose.SPLAT);
		level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 2.0F, 0.6F);
		level.sendParticles(ParticleTypes.CLOUD, boss.getX() + away.x * 0.5, boss.getY() + 1, boss.getZ() + away.z * 0.5, 12, 0.2, 0.6, 0.2, 0.02);
	}
}
