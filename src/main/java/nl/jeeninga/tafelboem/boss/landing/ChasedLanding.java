package nl.jeeninga.tafelboem.boss.landing;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.LandingStyle;
import nl.jeeninga.tafelboem.game.BombEffects;

/**
 * The Kippenbom's signature: 4 angry chickens appear behind him where he landed and chase him while
 * he runs round, and poof when the chase ends. They aren't the counted chickens: those stay in their
 * groups where the bomb hit.
 */
final class ChasedLanding extends FranticRunLanding {
	private static final int CHICKENS = 4;
	private static final int CHICKEN_LIFETIME_TICKS = 20 * 10;
	private static final int CHASE_EVERY = 5;
	private static final double CHASE_SPEED = 1.6;
	private final List<Chicken> chickens = new ArrayList<>();

	ChasedLanding(GraafFout boss, Scene scene) {
		super(LandingStyle.CHASED, boss, scene);
	}

	@Override
	protected void begin(ServerLevel level) {
		super.begin(level);
		Vec3 behind = boss.position().subtract(Vec3.directionFromRotation(0, boss.getYRot()).scale(2));
		chickens.addAll(BombEffects.chasingChickens(level, behind, CHICKENS));
		chickens.forEach(chicken -> scene.spectacle().accept(chicken, CHICKEN_LIFETIME_TICKS));
	}

	@Override
	protected void act(ServerLevel level) {
		super.act(level);
		if (ticks() % CHASE_EVERY == 0) {
			for (Chicken chicken : chickens) {
				if (chicken.isAlive()) {
					chicken.getNavigation().moveTo(boss, CHASE_SPEED);
				}
			}
		}
		if (ticks() % 10 == 0) {
			for (Chicken chicken : chickens) {
				if (chicken.isAlive()) {
					level.sendParticles(ParticleTypes.ANGRY_VILLAGER, chicken.getX(), chicken.getY() + 0.8, chicken.getZ(), 1, 0, 0, 0, 0);
				}
			}
			level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 1.0F, 1.8F);
		}
	}

	@Override
	public void finish() {
		super.finish();
		// Left standing they would look like one more group next to the counting Kippenbom's.
		for (Chicken chicken : chickens) {
			if (chicken.isAlive() && chicken.level() instanceof ServerLevel level) {
				BombEffects.poof(level, chicken.position());
				chicken.discard();
			}
		}
	}
}
