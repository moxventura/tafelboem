package nl.jeeninga.tafelboem.boss.landing;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.LandingStyle;
import nl.jeeninga.tafelboem.core.YeetPose;

/** Stuck head-first in the ground with his legs wiggling, then he pops out. */
final class HeadFirstLanding extends Landing {
	/** He pops out with a few ticks to spare, so the kids see him upright before he hovers off. */
	private static final int STUCK_TICKS = 32;

	HeadFirstLanding(GraafFout boss, Scene scene) {
		super(LandingStyle.HEAD_FIRST, boss, scene);
	}

	@Override
	protected void begin(ServerLevel level) {
		boss.setYeetPose(YeetPose.HEAD_FIRST);
		BlockState ground = level.getBlockState(boss.blockPosition().below());
		if (!ground.isAir()) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), boss.getX(), boss.getY() + 0.2, boss.getZ(),
					30, 0.4, 0.1, 0.4, 0.1);
		}
		level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.GRAVEL_BREAK, SoundSource.HOSTILE, 1.5F, 0.8F);
	}

	@Override
	protected void act(ServerLevel level) {
		if (ticks() == STUCK_TICKS) {
			boss.setYeetPose(YeetPose.NONE);
			boss.setDeltaMovement(0, 0.3, 0);
			level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.HOSTILE, 1.5F, 0.5F);
		}
	}
}
