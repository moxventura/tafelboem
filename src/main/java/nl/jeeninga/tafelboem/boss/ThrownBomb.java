package nl.jeeninga.tafelboem.boss;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.registry.ModBlocks;
import nl.jeeninga.tafelboem.registry.ModEntities;

/**
 * A bomb in flight: thrown by Graaf Fout at a kid, or kicked back at him after a correct answer.
 * Never saved with the world.
 */
public class ThrownBomb extends ThrowableItemProjectile {
	private static final double KICK_SPEED = 0.9;
	private static final int MAX_KICK_TICKS = 80;

	private BombTier tier = BombTier.KNALLETJE;
	private @Nullable BossFight fight;
	private @Nullable UUID targetPlayer;
	private @Nullable GraafFout kickTarget;
	private int kickTicks;
	private boolean done;

	public ThrownBomb(EntityType<? extends ThrownBomb> type, Level level) {
		super(type, level);
	}

	/** Thrown by the boss at a player. */
	static ThrownBomb thrown(ServerLevel level, LivingEntity boss, BombTier tier, BossFight fight, UUID targetPlayer) {
		ThrownBomb bomb = new ThrownBomb(ModEntities.THROWN_BOMB, level);
		bomb.setItem(new ItemStack(ModBlocks.bombItem(tier)));
		bomb.setOwner(boss);
		bomb.tier = tier;
		bomb.fight = fight;
		bomb.targetPlayer = targetPlayer;
		return bomb;
	}

	/** Kicked back at the boss by a player who answered correctly. */
	static ThrownBomb kicked(ServerLevel level, LivingEntity kicker, BombTier tier, BossFight fight, GraafFout boss) {
		ThrownBomb bomb = new ThrownBomb(ModEntities.THROWN_BOMB, level);
		bomb.setItem(new ItemStack(ModBlocks.bombItem(tier)));
		bomb.setOwner(kicker);
		bomb.tier = tier;
		bomb.fight = fight;
		bomb.kickTarget = boss;
		bomb.setNoGravity(true);
		return bomb;
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
		super.tick();
		if (level() instanceof ServerLevel level) {
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
			fight.onBombLanded(level, landing, tier, targetPlayer);
		}
	}

	private void hitBoss() {
		if (done) {
			return;
		}
		done = true;
		discard();
		if (fight != null) {
			fight.onKickHit(position(), tier);
		}
	}
}
