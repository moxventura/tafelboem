package nl.jeeninga.tafelboem.boss.yeet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.YeetPose;
import nl.jeeninga.tafelboem.core.YeetStyle;

/** Rolls and flips head over heels while he spins; the client draws the roll and the flips. */
final class TumbleYeet extends Yeet {
	TumbleYeet(GraafFout boss, Vec3 away, double power) {
		super(YeetStyle.TUMBLE, boss, away, power);
	}

	@Override
	public YeetPose startPose() {
		return YeetPose.TUMBLE;
	}

	@Override
	protected void launch() {
		fling(power * 0.85, 0.55 + power * 0.55, false);
	}

	@Override
	protected boolean fly(ServerLevel level) {
		// Slower spin than the classic, so the roll and the flips show.
		boss.spin(20F);
		return boss.onGround() && ticks() > 5;
	}
}
