package nl.jeeninga.tafelboem.boss.landing;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.LandingStyle;
import nl.jeeninga.tafelboem.core.YeetPose;

/**
 * What Graaf Fout does once a yeet has put him back on the ground, from {@link #start} until it has
 * run for its {@link LandingStyle#ticks()}. Each style is its own class, like the yeets, so a new
 * landing doesn't grow Graaf Fout.
 */
public abstract class Landing {
	/**
	 * Where a landing happens: the arena (null outside one), where the kids stand now, and how a
	 * spectacle animal gets its poof timer ({@code SessionManager::track}).
	 */
	public record Scene(@Nullable ArenaBox arena, Supplier<List<Vec3>> kids, BiConsumer<Entity, Integer> spectacle) {
		/** For a landing without a fight: no arena, no kids, nothing to poof. */
		public static final Scene NONE = new Scene(null, List::of, (entity, ticks) -> {
		});
	}

	protected final GraafFout boss;
	protected final Scene scene;
	private final LandingStyle style;
	private int ticks;

	protected Landing(LandingStyle style, GraafFout boss, Scene scene) {
		this.style = style;
		this.boss = boss;
		this.scene = scene;
	}

	public static Landing start(LandingStyle style, GraafFout boss, Scene scene, ServerLevel level) {
		Landing landing = switch (style) {
			case DIZZY -> new DizzyLanding(boss, scene);
			case HEAD_FIRST -> new HeadFirstLanding(boss, scene);
			case PANCAKE -> new PancakeLanding(boss, scene);
			case FRANTIC_RUN -> new FranticRunLanding(boss, scene);
			default -> throw new IllegalArgumentException("No landing built for " + style + " yet");
		};
		landing.begin(level);
		return landing;
	}

	public LandingStyle style() {
		return style;
	}

	/**
	 * One tick of the landing.
	 *
	 * @return true once it has run its course
	 */
	public final boolean tick(ServerLevel level) {
		ticks++;
		act(level);
		return ticks >= style.ticks();
	}

	/** Back on his feet: upright, arms down, hovering again. Also when a landing is cut short. */
	public void finish() {
		boss.setYeetPose(YeetPose.NONE);
		boss.armsUp(false);
		boss.setNoGravity(true);
	}

	protected void begin(ServerLevel level) {
	}

	protected abstract void act(ServerLevel level);

	/** Ticks since the landing began, this one included. */
	protected int ticks() {
		return ticks;
	}
}
