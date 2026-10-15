package nl.jeeninga.tafelboem.boss.yeet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.YeetStyle;

/** Launched up and away in a high arc, spinning. */
final class ClassicYeet extends Yeet {
	ClassicYeet(GraafFout boss, Vec3 away, double power) {
		super(YeetStyle.CLASSIC, boss, away, power);
	}

	@Override
	protected void launch() {
		fling(power, 0.6 + power * 0.6, false);
	}

	@Override
	protected boolean fly(ServerLevel level) {
		boss.spin(45F);
		return boss.onGround() && ticks() > 5;
	}
}
