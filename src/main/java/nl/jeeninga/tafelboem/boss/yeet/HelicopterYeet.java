package nl.jeeninga.tafelboem.boss.yeet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.YeetStyle;

/**
 * Spins up slowly on his cape like a propeller, then gravity remembers him. He goes up more than
 * away, so the yeet-o-meter says how high he got.
 */
final class HelicopterYeet extends Yeet {
	/** He rises this many ticks per unit of yeet power: a bigger bomb, a higher flight. */
	private static final double RISE_TICKS_PER_POWER = 22;

	private final int riseTicks;

	HelicopterYeet(GraafFout boss, Vec3 away, double power) {
		super(YeetStyle.HELICOPTER, boss, away, power);
		this.riseTicks = (int) Math.round(RISE_TICKS_PER_POWER * power);
	}

	@Override
	public boolean measuresHeight() {
		return true;
	}

	@Override
	protected void launch() {
		fling(0.15, 0.22, true);
	}

	@Override
	protected boolean fly(ServerLevel level) {
		boss.spin(90F);
		if (ticks() <= riseTicks) {
			boss.setDeltaMovement(away.x * 0.12, 0.22, away.z * 0.12);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, boss.getX(), boss.getY() + 2.1, boss.getZ(), 1, 0, 0, 0, 0);
			if (ticks() == riseTicks) {
				boss.setNoGravity(false);
				boss.playSound(SoundEvents.EVOKER_HURT, 1.5F, 1.4F);
			}
			return false;
		}
		return boss.onGround();
	}
}
