package nl.jeeninga.tafelboem.boss;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.Homing;
import nl.jeeninga.tafelboem.game.BombEffects;
import nl.jeeninga.tafelboem.registry.ModBlocks;
import nl.jeeninga.tafelboem.registry.ModEntities;

/**
 * A bomb in flight: thrown by Graaf Fout at a kid, or kicked back at him after a correct answer.
 * Never saved with the world.
 */
public class ThrownBomb extends ThrowableItemProjectile {
	private static final double KICK_SPEED = 0.9;
	private static final int MAX_KICK_TICKS = 80;
	private static final int MAX_BACK_TICKS = 60;

	private BombTier tier = BombTier.KNALLETJE;
	private @Nullable BossFight fight;
	private @Nullable UUID targetPlayer;
	private @Nullable ThrowInfo info;
	private @Nullable GraafFout kickTarget;
	private @Nullable Kick kick;
	private @Nullable UUID thrower;
	private int backTicks;
	private int kickTicks;
	private boolean done;

	public ThrownBomb(EntityType<? extends ThrownBomb> type, Level level) {
		super(type, level);
	}

	/** Thrown by the boss at a player. */
	/** Thrown by the boss at a player. */
	static ThrownBomb thrown(ServerLevel level, LivingEntity boss, BombTier tier, BossFight fight, UUID targetPlayer, @Nullable ThrowInfo info) {
		ThrownBomb bomb = new ThrownBomb(ModEntities.THROWN_BOMB, level);
		bomb.setItem(new ItemStack(ModBlocks.bombItem(tier)));
		bomb.setOwner(boss);
		bomb.tier = tier;
		bomb.fight = fight;
		bomb.targetPlayer = targetPlayer;
		bomb.info = info;
		return bomb;
	}

	/** Kicked back at the boss by a player who answered correctly; the kick rides along, so it can't be mixed up with another. */
	static ThrownBomb kicked(ServerLevel level, LivingEntity kicker, BombTier tier, BossFight fight, GraafFout boss, Kick kick) {
		ThrownBomb bomb = new ThrownBomb(ModEntities.THROWN_BOMB, level);
		bomb.setItem(new ItemStack(ModBlocks.bombItem(tier)));
		bomb.setOwner(kicker);
		bomb.tier = tier;
		bomb.fight = fight;
		bomb.kickTarget = boss;
		bomb.kick = kick;
		bomb.setNoGravity(true);
		return bomb;
	}

	/** Thrown back at the boss by a kid who caught it (R1.4): flies like a snowball, homes in on him, hits nothing else. */
	static ThrownBomb thrownBack(ServerLevel level, ServerPlayer kid, BombTier tier, BossFight fight) {
		ThrownBomb bomb = new ThrownBomb(ModEntities.THROWN_BOMB, level);
		bomb.setItem(new ItemStack(ModBlocks.bombItem(tier)));
		bomb.setOwner(kid);
		bomb.tier = tier;
		bomb.fight = fight;
		bomb.thrower = kid.getUUID();
		return bomb;
	}

	/** It passes through kids, chickens and every other mob; only blocks stop it, and Graaf Fout is found in {@link #flyBack}. */
	@Override
	protected boolean canHitEntity(Entity entity) {
		return thrower == null && super.canHitEntity(entity);
	}

	@Override
	protected Item getDefaultItem() {
		return ModBlocks.bombItem(BombTier.KNALLETJE);
	}

	@Override
	protected float getAirDrag() {
		// No drag, so the throw is a clean parabola that lands where Graaf Fout aims.
		return 1.0F;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public void tick() {
		if (level() instanceof ServerLevel level && fight == null) {
			discard();
			return;
		}
		if (kickTarget != null && level() instanceof ServerLevel level) {
			steerTowardsBoss(level);
			if (isRemoved()) {
				return;
			}
		}
		if (thrower != null && level() instanceof ServerLevel level) {
			flyBack(level);
			if (isRemoved()) {
				return;
			}
		}
		super.tick();
		if (level() instanceof ServerLevel level) {
			// Each bomb trails its own colour, so the kids can tell from afar what is coming.
			level.sendParticles(new DustParticleOptions(tier.trailColor(), 1.5F), getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0);
			level.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
		}
	}

	private void steerTowardsBoss(ServerLevel level) {
		GraafFout boss = kickTarget;
		Vec3 toBoss = boss.position().add(0, boss.getBbHeight() / 2, 0).subtract(position());
		if (++kickTicks > MAX_KICK_TICKS || toBoss.lengthSqr() < 1.5 * 1.5 || !boss.isAlive()) {
			hitBoss();
			return;
		}
		setDeltaMovement(toBoss.normalize().scale(KICK_SPEED));
		level.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0.01);
	}

	@Override
	protected void onHit(HitResult hitResult) {
		if (!(level() instanceof ServerLevel level) || done) {
			return;
		}
		if (thrower != null) {
			missed(level);
			return;
		}
		if (kickTarget != null) {
			// A correct answer always counts, even if the kick bumps into a wall on the way.
			hitBoss();
			return;
		}

		BlockPos landing = switch (hitResult) {
			case BlockHitResult blockHit -> blockHit.getDirection() == Direction.DOWN
					? blockHit.getBlockPos().below()
					: blockHit.getBlockPos().relative(blockHit.getDirection());
			case EntityHitResult entityHit -> entityHit.getEntity().blockPosition();
			default -> blockPosition();
		};
		done = true;
		discard();
		if (fight != null && targetPlayer != null) {
			fight.onBombLanded(level, landing, tier, targetPlayer, info);
		}
	}

	/**
	 * One tick of a bomb thrown back: did this tick's path pass through his box (his own, grown by a block), else bend
	 * towards his middle when he is within reach.
	 */
	private void flyBack(ServerLevel level) {
		GraafFout boss = fight.boss();
		if (++backTicks > MAX_BACK_TICKS || !boss.isAlive() || fight.isOver()) {
			missed(level);
			return;
		}
		Vec3 from = position();
		Vec3 velocity = getDeltaMovement();
		AABB box = boss.getBoundingBox().inflate(Homing.HIT_MARGIN);
		if (box.contains(from) || box.clip(from, from.add(velocity)).isPresent()) {
			done = true;
			discard();
			fight.backReached(thrower, tier);
			return;
		}
		Vec3 toHim = boss.position().add(0, boss.getBbHeight() / 2, 0).subtract(from);
		if (toHim.length() <= Homing.range(boss.isFlying())) {
			double[] steered = Homing.steer(velocity.x, velocity.y, velocity.z, toHim.x, toHim.y, toHim.z);
			setDeltaMovement(steered[0], steered[1], steered[2]);
			// Near him it flies straight at him instead of sagging.
			setNoGravity(true);
		}
	}

	/** A miss (a block, or too long in the air) just poofs. */
	private void missed(ServerLevel level) {
		done = true;
		discard();
		BombEffects.poof(level, position());
	}

	private void hitBoss() {
		if (done) {
			return;
		}
		done = true;
		discard();
		if (fight != null) {
			fight.onKickHit(position(), tier, kick);
		}
	}
}
