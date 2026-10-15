package nl.jeeninga.tafelboem.boss.yeet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.YeetPose;
import nl.jeeninga.tafelboem.core.YeetStyle;

/** Flies off, slows down, turns around and comes back to the kid, face first. */
final class BoomerangYeet extends Yeet {
	private static final int OUT_TICKS = 12;
	private static final int MAX_TICKS = 45;
	/** He drops this far in front of the kid, so his face lands just short of their feet. */
	private static final double STOP = 4;

	private final Vec3 returnTo;

	BoomerangYeet(GraafFout boss, Vec3 away, double power, Vec3 kicker) {
		super(YeetStyle.BOOMERANG, boss, away, power);
		// He faceplants in front of the kid, never on top of them.
		this.returnTo = kicker.add(launchPos().subtract(kicker).multiply(1, 0, 1).normalize().scale(STOP));
	}

	@Override
	protected void launch() {
		fling(power * 0.9, 0.35, true);
	}

	@Override
	protected boolean fly(ServerLevel level) {
		if (boss.yeetPose() != YeetPose.FACEPLANT) {
			boss.spin(60F);
		}
		if (ticks() <= OUT_TICKS) {
			double slowdown = 1.0 - (double) ticks() / OUT_TICKS;
			boss.setDeltaMovement(away.x * power * 0.9 * slowdown, 0.35 * slowdown, away.z * power * 0.9 * slowdown);
			return false;
		}
		if (boss.isNoGravity()) {
			Vec3 back = returnTo.add(0, 1.5, 0).subtract(boss.position());
			if (back.horizontalDistance() < 1.5 || ticks() > MAX_TICKS) {
				boss.setNoGravity(false);
				boss.setDeltaMovement(back.multiply(0.1, 0, 0.1).add(0, -0.4, 0));
				boss.faceplantTowards(away.reverse());
			} else {
				boss.setDeltaMovement(back.normalize().scale(0.6));
			}
			return false;
		}
		return boss.onGround();
	}
}
