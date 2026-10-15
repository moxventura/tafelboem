package nl.jeeninga.tafelboem.boss.yeet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.YeetPose;
import nl.jeeninga.tafelboem.core.YeetStyle;

/**
 * One flight of Graaf Fout after a kicked bomb hit him: how he is launched, how he flies, and how
 * far he got, for the yeet-o-meter. Each style is its own class; {@link #start} picks it.
 */
public abstract class Yeet {
	/** Whatever happens in the air, after 5 s he has landed. */
	private static final int MAX_AIRBORNE_TICKS = 100;

	protected final GraafFout boss;
	/** Horizontal and of length 1: away from the boom. */
	protected final Vec3 away;
	protected final double power;
	private final YeetStyle style;
	private final Vec3 launchPos;
	private final double launchGround;
	private double furthest;
	private double highest;
	private int ticks;

	protected Yeet(YeetStyle style, GraafFout boss, Vec3 away, double power) {
		this.style = style;
		this.boss = boss;
		this.away = away;
		this.power = power;
		this.launchPos = boss.position();
		this.launchGround = boss.level().getHeight(Heightmap.Types.MOTION_BLOCKING, boss.getBlockX(), boss.getBlockZ());
	}

	/**
	 * Launches him.
	 *
	 * @param away horizontal and of length 1
	 * @param kicker where the kid who kicked stands; the boomerang comes back to them
	 */
	public static Yeet start(YeetStyle style, GraafFout boss, Vec3 away, double power, Vec3 kicker) {
		Yeet yeet = switch (style) {
			case CLASSIC -> new ClassicYeet(boss, away, power);
			case BOUNCE -> new BounceYeet(boss, away, power);
			case HELICOPTER -> new HelicopterYeet(boss, away, power);
			case BOOMERANG -> new BoomerangYeet(boss, away, power, kicker);
			case TUMBLE -> new TumbleYeet(boss, away, power);
		};
		yeet.launch();
		return yeet;
	}

	public YeetStyle style() {
		return style;
	}

	/** The pose on top of his spin from the launch on. */
	public YeetPose startPose() {
		return YeetPose.NONE;
	}

	/** Whether the yeet-o-meter says how high he flew rather than how far. */
	public boolean measuresHeight() {
		return false;
	}

	/** How far (or for {@link #measuresHeight}, how high) he flew so far, in whole blocks. */
	public int blocks() {
		return (int) Math.round(measuresHeight() ? highest : furthest);
	}

	/**
	 * One tick in the air.
	 *
	 * @return true once he has landed
	 */
	public final boolean tick(ServerLevel level) {
		ticks++;
		furthest = Math.max(furthest, boss.position().subtract(launchPos).horizontalDistance());
		highest = Math.max(highest, boss.getY() - launchGround);
		level.sendParticles(ParticleTypes.CLOUD, boss.getX(), boss.getY() + 1, boss.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
		return fly(level) || ticks > MAX_AIRBORNE_TICKS;
	}

	protected abstract void launch();

	/**
	 * His flight this tick, after the measuring.
	 *
	 * @return true once he has landed
	 */
	protected abstract boolean fly(ServerLevel level);

	/** Ticks in the air so far, this one included. */
	protected int ticks() {
		return ticks;
	}

	protected Vec3 launchPos() {
		return launchPos;
	}

	/** Off he goes: {@code horizontal} away from the boom, {@code up} upwards, floating or falling. */
	protected final void fling(double horizontal, double up, boolean floating) {
		boss.setNoGravity(floating);
		boss.setDeltaMovement(away.x * horizontal, up, away.z * horizontal);
	}
}
