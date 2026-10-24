package nl.jeeninga.tafelboem.boss.landing;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.LandingStyle;
import nl.jeeninga.tafelboem.core.RunCircle;

/**
 * Runs a small circle in a panic: smoke coming off his pants, arms waving. Over ground only, never
 * into a crater deeper than 2 blocks, never closer than 2 blocks to a kid, and he pushes nobody.
 */
class FranticRunLanding extends Landing {
	private static final double SPEED = 0.3;
	private static final double JUMP = 0.42;
	private RunCircle circle;

	FranticRunLanding(GraafFout boss, Scene scene) {
		this(LandingStyle.FRANTIC_RUN, boss, scene);
	}

	protected FranticRunLanding(LandingStyle style, GraafFout boss, Scene scene) {
		super(style, boss, scene);
	}

	@Override
	protected void begin(ServerLevel level) {
		circle = new RunCircle(boss.getX(), boss.getZ(), boss.getRandom().nextDouble() * Math.PI * 2);
		boss.armsUp(true);
	}

	@Override
	protected void act(ServerLevel level) {
		List<RunCircle.Point> kids = scene.kids().get().stream().map(kid -> new RunCircle.Point(kid.x, kid.z)).toList();
		RunCircle.Point to = circle.next(scene.arena(), kids, point -> hasGround(level, point));
		Vec3 motion = boss.getDeltaMovement();
		if (to == null) {
			boss.setDeltaMovement(0, motion.y, 0);
		} else {
			Vec3 step = new Vec3(to.x() - boss.getX(), 0, to.z() - boss.getZ());
			Vec3 run = step.lengthSqr() > 1.0E-4 ? step.normalize().scale(SPEED) : Vec3.ZERO;
			boss.setDeltaMovement(run.x, boss.horizontalCollision && boss.onGround() ? JUMP : motion.y, run.z);
			if (run.lengthSqr() > 0) {
				boss.face(run);
			}
		}
		level.sendParticles(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY() + 0.7, boss.getZ(), 1, 0.15, 0.1, 0.15, 0.01);
		if (ticks() % 10 == 1) {
			level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.EVOKER_HURT, SoundSource.HOSTILE, 0.8F, 1.7F);
		}
	}

	/** Ground he can run on: not the void, and not more than 2 blocks up or down from where he is. */
	private boolean hasGround(ServerLevel level, RunCircle.Point point) {
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(point.x()), Mth.floor(point.z()));
		boolean overFloor = scene.arena() == null ? ground > level.getMinY() + 1 : ground > scene.arena().minY();
		return overFloor && Math.abs(ground - boss.getY()) <= 2;
	}
}
