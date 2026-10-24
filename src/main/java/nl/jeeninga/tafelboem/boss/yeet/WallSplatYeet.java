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
 * Flies straight and level and fast, slaps flat against the first wall he hits, sticks there for a
 * moment and slides down. With no wall within reach he just falls, like the classic yeet.
 */
final class WallSplatYeet extends Yeet {
	/**
	 * How far he flies level, in blocks: about the longest classic yeet, so the splat never beats the
	 * other flights for the records. From near the exit door the far wall of an arena is about this far.
	 */
	private static final double REACH = 36;
	private static final int STUCK_TICKS = 12;
	private static final double SLIDE = 0.12;
	/** Pressed against the wall while he sticks and slides, so he doesn't drift off it. */
	private static final double PRESS = 0.05;
	/** Ticks since he hit the wall; -1 while he is still flying. */
	private int stuck = -1;
	/** He has flown {@link #REACH} blocks without a wall and drops. */
	private boolean falling;

	WallSplatYeet(GraafFout boss, Vec3 away, double power) {
		super(YeetStyle.WALL_SPLAT, boss, away, power);
	}

	@Override
	protected void launch() {
		fling(speed(), 0, true);
	}

	@Override
	protected boolean fly(ServerLevel level) {
		if (stuck < 0) {
			if (boss.horizontalCollision) {
				splat(level);
				return false;
			}
			if (!falling) {
				if (boss.position().subtract(launchPos()).horizontalDistance() >= REACH) {
					falling = true;
					boss.setNoGravity(false);
				} else {
					// Drag would slow him down, and he must reach the wall at full speed.
					boss.setDeltaMovement(away.x * speed(), 0, away.z * speed());
				}
			}
			boss.spin(30F);
			return falling && boss.onGround();
		}
		stuck++;
		if (stuck <= STUCK_TICKS) {
			boss.setDeltaMovement(away.scale(PRESS));
			return false;
		}
		boss.setDeltaMovement(away.x * PRESS, -SLIDE, away.z * PRESS);
		return boss.onGround();
	}

	private double speed() {
		return power * 0.9;
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
