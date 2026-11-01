package nl.jeeninga.tafelboem.boss.landing;

import net.minecraft.server.level.ServerLevel;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.LandingStyle;
import nl.jeeninga.tafelboem.core.YeetPose;

/** A trap landing that starts with him upright and his arms up, hopping or wriggling: the prickles and the cobweb. */
abstract class ArmsUpLanding extends Landing {
	protected ArmsUpLanding(LandingStyle style, GraafFout boss, Scene scene) {
		super(style, boss, scene);
	}

	@Override
	protected void begin(ServerLevel level) {
		boss.setYeetPose(YeetPose.NONE);
		boss.armsUp(true);
	}
}
