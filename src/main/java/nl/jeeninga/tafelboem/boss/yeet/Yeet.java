package nl.jeeninga.tafelboem.boss.yeet;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.TrapPick;
import nl.jeeninga.tafelboem.core.YeetPose;
import nl.jeeninga.tafelboem.core.YeetStyle;

/**
 * One flight of Graaf Fout after a kicked bomb hit him: how he is launched, how he flies, and how
 * far he got, for the yeet-o-meter. Each style is its own class; {@link #start} picks it.
 */
public abstract class Yeet {
	/** Whatever happens in the air, after 5 s he has landed. */
	private static final int MAX_AIRBORNE_TICKS = 100;
	/** On the last part of his flight, from this high above the ground a trap can pull him in. */
	private static final double PULL_HEIGHT = 3;
	/** The pull is a short hop of about 3 ticks a block, between these. */
	private static final int MIN_PULL_TICKS = 6;
	private static final int MAX_PULL_TICKS = 14;
	/** How high the hop into the trap arcs. */
	private static final double PULL_ARC = 1.2;

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
	private Traps traps = Traps.NONE;
	private final TrapPick pick = new TrapPick();
	private TrapPick.@Nullable Candidate caught;
	private Vec3 pullFrom = Vec3.ZERO;
	private Vec3 pullTo = Vec3.ZERO;
	private int pullTicks;
	private int pulled;

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
	 * @param away   horizontal and of length 1
	 * @param kicker where the kid who kicked stands; the boomerang comes back to them
	 * @param traps  where the end of the flight may find a trap to land in
	 */
	public static Yeet start(YeetStyle style, GraafFout boss, Vec3 away, double power, Vec3 kicker, Traps traps) {
		Yeet yeet = switch (style) {
			case CLASSIC -> new ClassicYeet(boss, away, power);
			case BOUNCE -> new BounceYeet(boss, away, power);
			case HELICOPTER -> new HelicopterYeet(boss, away, power);
			case BOOMERANG -> new BoomerangYeet(boss, away, power, kicker);
			case TUMBLE -> new TumbleYeet(boss, away, power);
			case WALL_SPLAT -> new WallSplatYeet(boss, away, power);
		};
		yeet.traps = traps;
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
		if (caught != null) {
			return pull();
		}
		boolean landed = fly(level) || ticks > MAX_AIRBORNE_TICKS;
		if (mayBePulled() && (landed || comingDown(level)) && startPull()) {
			return false;
		}
		return landed;
	}

	/** The trap he was pulled into at the end of this yeet, or null (R1.3). */
	public TrapPick.@Nullable Candidate caught() {
		return caught;
	}

	/**
	 * Whether this is the last part of the flight, where a trap may pull him in: by default once he
	 * is on his way. Flights with stages (a bounce, the boomerang's way back) say when.
	 */
	protected boolean mayBePulled() {
		return ticks > 5;
	}

	private boolean comingDown(ServerLevel level) {
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, boss.getBlockX(), boss.getBlockZ());
		return boss.getDeltaMovement().y < 0 && boss.getY() - ground <= PULL_HEIGHT;
	}

	/** The nearest trap around where he comes down bends the rest of the flight into it. */
	private boolean startPull() {
		TrapPick.Candidate trap = pick.pick(boss.getX(), boss.getZ(), traps.around(boss.position()));
		if (trap == null) {
			return false;
		}
		caught = trap;
		pullFrom = boss.position();
		pullTo = new Vec3(trap.x() + 0.5, trap.y() + (trap.kind().landsOnTop() ? 1 : 0), trap.z() + 0.5);
		pullTicks = Mth.clamp((int) Math.round(pullFrom.distanceTo(pullTo) * 3), MIN_PULL_TICKS, MAX_PULL_TICKS);
		boss.setNoGravity(true);
		return true;
	}

	/**
	 * One tick of the hop into the trap: a little arc that goes over whatever stands in between, so
	 * a wall by the trap can't keep him out.
	 *
	 * @return true once he is in it
	 */
	private boolean pull() {
		pulled++;
		double t = Math.min(1.0, (double) pulled / pullTicks);
		Vec3 at = pullFrom.lerp(pullTo, t).add(0, PULL_ARC * 4 * t * (1 - t), 0);
		boss.setDeltaMovement(Vec3.ZERO);
		boss.setPos(at.x, at.y, at.z);
		boss.spin(30F);
		return t >= 1.0;
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
