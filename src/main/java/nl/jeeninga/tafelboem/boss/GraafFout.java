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

import nl.jeeninga.tafelboem.core.YeetStyle;

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
	private static final int MAX_BOUNCES = 3;
	private static final int HELICOPTER_RISE_TICKS = 30;
	private static final int BOOMERANG_OUT_TICKS = 12;
	private static final int BOOMERANG_MAX_TICKS = 45;

	private @Nullable BossFight fight;
	private boolean airborne;
	private int airborneTicks;
	private int dizzyTicks;
	private boolean finale;

	/** The current yeet: how he flies, where from, and how far he got (for the yeet-o-meter). */
	private YeetStyle style = YeetStyle.CLASSIC;
	private Vec3 launchPos = Vec3.ZERO;
	private Vec3 away = Vec3.ZERO;
	private Vec3 returnTo = Vec3.ZERO;
	private double power;
	private double furthest;
	private int bounces;
	private int stageTicks;

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
	 *
	 * @param kicker where the kid who kicked stands; the boomerang comes back to them
	 */
	public void yeet(Vec3 from, double power, YeetStyle style, Vec3 kicker) {
		Vec3 direction = position().subtract(from).multiply(1, 0, 1);
		if (direction.lengthSqr() < 1.0E-4) {
			direction = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5);
		}
		this.away = direction.normalize();
		this.style = style;
		this.power = power;
		this.launchPos = position();
		this.furthest = 0;
		this.bounces = 0;
		this.stageTicks = 0;
		// The boomerang faceplants three blocks in front of the kid, never on top of them.
		this.returnTo = kicker.add(launchPos.subtract(kicker).multiply(1, 0, 1).normalize().scale(3));

		airborne = true;
		airborneTicks = 0;
		dizzyTicks = 0;
		switch (style) {
			case CLASSIC -> launch(power, 0.6 + power * 0.6, false);
			case BOUNCE -> launch(power * 0.8, 0.5 + power * 0.4, false);
			case HELICOPTER -> launch(0.15, 0.22, true);
			case BOOMERANG -> launch(power * 0.9, 0.35, true);
		}
		needsSync = true;
		armsUp(true);
		playSound(SoundEvents.EVOKER_HURT, 1.5F, 1.9F);
	}

	private void launch(double horizontal, double up, boolean floating) {
		setNoGravity(floating);
		setDeltaMovement(away.x * horizontal, up, away.z * horizontal);
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
		airborneTicks++;
		stageTicks++;
		furthest = Math.max(furthest, position().subtract(launchPos).horizontalDistance());
		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 1, 0.1, 0.1, 0.1, 0.0);

		boolean landed = switch (style) {
			case CLASSIC -> {
				spin(45F);
				yield onGround() && airborneTicks > 5;
			}
			case BOUNCE -> {
				spin(30F);
				yield bounce(level);
			}
			case HELICOPTER -> {
				spin(90F);
				yield helicopter(level);
			}
			case BOOMERANG -> {
				spin(60F);
				yield boomerang();
			}
		};
		if (landed || airborneTicks > MAX_AIRBORNE_TICKS) {
			land(level);
		}
	}

	/** Boing, boing, boing: each bounce a bit lower, then he stays down. */
	private boolean bounce(ServerLevel level) {
		if (!onGround() || stageTicks <= 3) {
			return false;
		}
		if (bounces >= MAX_BOUNCES) {
			return true;
		}
		bounces++;
		stageTicks = 0;
		double horizontal = power * 0.8 * Math.pow(0.8, bounces);
		setDeltaMovement(away.x * horizontal, 0.55 - 0.1 * bounces, away.z * horizontal);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 1.5F, 0.9F + 0.15F * bounces);
		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 8, 0.4, 0.05, 0.4, 0.02);
		return false;
	}

	/** Spins up slowly on his cape like a propeller, then gravity remembers him. */
	private boolean helicopter(ServerLevel level) {
		if (stageTicks <= HELICOPTER_RISE_TICKS) {
			setDeltaMovement(away.x * 0.12, 0.22, away.z * 0.12);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 2.1, getZ(), 1, 0, 0, 0, 0);
			if (stageTicks == HELICOPTER_RISE_TICKS) {
				setNoGravity(false);
				playSound(SoundEvents.EVOKER_HURT, 1.5F, 1.4F);
			}
			return false;
		}
		return onGround();
	}

	/** Flies off, slows down, turns around and comes back to the kid, face first. */
	private boolean boomerang() {
		if (stageTicks <= BOOMERANG_OUT_TICKS) {
			double slowdown = 1.0 - (double) stageTicks / BOOMERANG_OUT_TICKS;
			setDeltaMovement(away.x * power * 0.9 * slowdown, 0.35 * slowdown, away.z * power * 0.9 * slowdown);
			return false;
		}
		if (isNoGravity()) {
			Vec3 back = returnTo.add(0, 1.5, 0).subtract(position());
			if (back.horizontalDistance() < 1.5 || stageTicks > BOOMERANG_MAX_TICKS) {
				setNoGravity(false);
				setDeltaMovement(back.multiply(0.1, 0, 0.1).add(0, -0.4, 0));
			} else {
				setDeltaMovement(back.normalize().scale(0.6));
			}
			return false;
		}
		return onGround();
	}

	private void land(ServerLevel level) {
		airborne = false;
		dizzyTicks = DIZZY_TICKS;
		setNoGravity(false);
		setDeltaMovement(Vec3.ZERO);
		armsUp(false);
		for (int i = 0; i < 24; i++) {
			double angle = Math.PI * 2 * i / 24;
			level.sendParticles(ParticleTypes.CLOUD, getX() + Math.cos(angle) * 1.2, getY() + 0.1, getZ() + Math.sin(angle) * 1.2,
					1, 0, 0, 0, 0.05);
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_BIG_FALL, SoundSource.HOSTILE, 1.5F, 0.8F);
		if (fight != null) {
			fight.onYeetLanded((int) Math.round(furthest));
		}
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
