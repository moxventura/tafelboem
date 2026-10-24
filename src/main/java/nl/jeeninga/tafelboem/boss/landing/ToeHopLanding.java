package nl.jeeninga.tafelboem.boss.landing;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.LandingStyle;
import nl.jeeninga.tafelboem.core.YeetPose;

/** The Knalletje's signature: hops on one leg holding his sore toe, "au!", with confetti around his head. */
final class ToeHopLanding extends Landing {
	private static final int HOP_EVERY = 8;
	private static final int[] CONFETTI = {0xFF5555, 0x55FF55, 0x5599FF, 0xFFFF55, 0xFF55FF};

	ToeHopLanding(GraafFout boss, Scene scene) {
		super(LandingStyle.TOE_HOP, boss, scene);
	}

	@Override
	protected void begin(ServerLevel level) {
		boss.setYeetPose(YeetPose.TOE_HOP);
	}

	@Override
	protected void act(ServerLevel level) {
		if (ticks() % HOP_EVERY == 1 && boss.onGround()) {
			boss.setDeltaMovement(0, 0.32, 0);
			// "Au!": his yeet squeak, higher.
			level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.EVOKER_HURT, SoundSource.HOSTILE, 1.0F, 1.9F);
		}
		if (ticks() % 2 == 0) {
			int colour = CONFETTI[ticks() / 2 % CONFETTI.length];
			level.sendParticles(new DustParticleOptions(colour, 1.0F), boss.getX(), boss.getEyeY() + 0.5, boss.getZ(), 2, 0.4, 0.2, 0.4, 0);
		}
	}
}
