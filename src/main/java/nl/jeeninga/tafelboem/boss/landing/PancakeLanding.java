package nl.jeeninga.tafelboem.boss.landing;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.LandingStyle;
import nl.jeeninga.tafelboem.core.YeetPose;

/** Squashed flat, waddles from side to side, then boing back into shape. */
final class PancakeLanding extends Landing {
	private static final int FLAT_TICKS = 30;
	private static final int WADDLE_EVERY = 6;

	PancakeLanding(GraafFout boss, Scene scene) {
		super(LandingStyle.PANCAKE, boss, scene);
	}

	@Override
	protected void begin(ServerLevel level) {
		boss.setYeetPose(YeetPose.PANCAKE);
		level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.5F, 0.7F);
	}

	@Override
	protected void act(ServerLevel level) {
		if (ticks() < FLAT_TICKS && ticks() % WADDLE_EVERY == 0) {
			boss.spin(ticks() % (2 * WADDLE_EVERY) == 0 ? 25F : -25F);
		}
		if (ticks() == FLAT_TICKS) {
			// The settle springs him back to his height: the boing.
			boss.setYeetPose(YeetPose.NONE);
			boss.setDeltaMovement(0, 0.35, 0);
			level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 1.5F, 1.4F);
		}
	}
}
