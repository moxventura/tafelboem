package nl.jeeninga.tafelboem.boss;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Graaf Fout (Count Wrong): an Evoker body with all its spells removed. He hovers, teleports, throws
 * bombs, and gets hilariously yeeted every time a kid kicks a bomb back at him.
 *
 * <p>He can't be hurt by anything else and is never saved with the world; a fight lives only as
 * long as the server session.
 */
public class GraafFout extends Evoker {
	private static final double HOVER_HEIGHT = 1.5;
	private static final int DIZZY_TICKS = 30;
	private static final int MAX_AIRBORNE_TICKS = 100;

	private @Nullable BossFight fight;
	private boolean airborne;
	private int airborneTicks;
	private int dizzyTicks;
	private boolean finale;

	public GraafFout(EntityType<? extends GraafFout> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Evoker.createAttributes();
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 32.0F, 1.0F));
	}

	void attach(BossFight fight) {
		this.fight = fight;
		setNoGravity(true);
	}

	public boolean isReacting() {
		return airborne || dizzyTicks > 0;
	}

	@Override
	public void checkDespawn() {
		// Not even peaceful difficulty makes him disappear mid-fight.
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// Only answers hurt Graaf Fout: swords, arrows and lava do nothing.
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel level) {
			if (fight == null) {
				discard();
			} else if (finale) {
				flyAway(level);
			} else if (airborne) {
				tumble(level);
			} else if (dizzyTicks > 0) {
				dizzy(level);
			} else {
				hover(level);
			}
		}
	}

	/**
	 * A kid kicked a bomb back: launch him away from the explosion, spinning and flailing.
	 */
	public void yeet(Vec3 from, double power) {
		Vec3 away = position().subtract(from).multiply(1, 0, 1);
		if (away.lengthSqr() < 1.0E-4) {
			away = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5);
		}
		away = away.normalize();

		airborne = true;
		airborneTicks = 0;
		dizzyTicks = 0;
		setNoGravity(false);
		setDeltaMovement(away.x * power, 0.6 + power * 0.6, away.z * power);
		needsSync = true;
		armsUp(true);
		playSound(SoundEvents.EVOKER_HURT, 1.5F, 1.9F);
	}

	/**
	 * Defeated: one last enormous yeet, straight into the sky.
	 */
	public void finalYeet() {
		finale = true;
		airborne = false;
		setNoGravity(true);
		armsUp(true);
		playSound(SoundEvents.EVOKER_HURT, 2.0F, 0.6F);
	}

	public void teleportNear(ServerLevel level, Vec3 around) {
		double angle = random.nextDouble() * Math.PI * 2;
		double distance = 7 + random.nextDouble() * 3;
		double x = around.x + Math.cos(angle) * distance;
		double z = around.z + Math.sin(angle) * distance;
		double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)) + HOVER_HEIGHT;

		level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
		teleportTo(x, y, z);
		level.sendParticles(ParticleTypes.POOF, x, y + 1, z, 20, 0.3, 0.6, 0.3, 0.02);
		level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.4F);
	}

	private void hover(ServerLevel level) {
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ());
		double targetY = ground + HOVER_HEIGHT + Math.sin(tickCount * 0.1) * 0.25;
		double dy = Mth.clamp((targetY - getY()) * 0.1, -0.15, 0.15);
		setDeltaMovement(0, dy, 0);
	}

	private void tumble(ServerLevel level) {
		spin(45F);
		airborneTicks++;
		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 1, 0.1, 0.1, 0.1, 0.0);
		if ((onGround() && airborneTicks > 5) || airborneTicks > MAX_AIRBORNE_TICKS) {
			land(level);
		}
	}

	private void land(ServerLevel level) {
		airborne = false;
		dizzyTicks = DIZZY_TICKS;
		setDeltaMovement(Vec3.ZERO);
		armsUp(false);
		for (int i = 0; i < 24; i++) {
			double angle = Math.PI * 2 * i / 24;
			level.sendParticles(ParticleTypes.CLOUD, getX() + Math.cos(angle) * 1.2, getY() + 0.1, getZ() + Math.sin(angle) * 1.2,
					1, 0, 0, 0, 0.05);
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_BIG_FALL, SoundSource.HOSTILE, 1.5F, 0.8F);
	}

	private void dizzy(ServerLevel level) {
		dizzyTicks--;
		double angle = tickCount * 0.5;
		level.sendParticles(ParticleTypes.CRIT, getX() + Math.cos(angle) * 0.5, getEyeY() + 0.6, getZ() + Math.sin(angle) * 0.5,
				1, 0, 0, 0, 0);
		if (dizzyTicks == 0) {
			setNoGravity(true);
		}
	}

	private void flyAway(ServerLevel level) {
		spin(60F);
		setDeltaMovement(0, 1.2, 0);
		level.sendParticles(ParticleTypes.FIREWORK, getX(), getY(), getZ(), 4, 0.2, 0.2, 0.2, 0.05);
	}

	private void spin(float degrees) {
		float yaw = getYRot() + degrees;
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
	}

	private void armsUp(boolean up) {
		spellCastingTickCount = up ? 200 : 0;
		setIsCastingSpell(up ? IllagerSpell.WOLOLO : IllagerSpell.NONE);
	}
}
