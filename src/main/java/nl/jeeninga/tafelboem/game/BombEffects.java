package nl.jeeninga.tafelboem.game;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.block.BombBlock;
import nl.jeeninga.tafelboem.core.BombTier;

/**
 * What the bombs look and sound like. R0 uses vanilla particles; the choreographed shows come in R2.
 */
public final class BombEffects {
	public static final String SPECTACLE_TAG = "tafelboem_spectacle";
	/** On the item frames beside the hub's doors, which every world load places again. */
	public static final String HUB_TAG = "tafelboem_hub";
	private static final int KIPPENBOM_CHICKENS = 12;
	/** After a wrong answer in 1.0.2 (and with the counting Kippenbom off): fewer and lower than after a right one. */
	public static final int WRONG_ANSWER_CHICKENS = 3;

	private BombEffects() {
	}

	public static void fuseStarted(ServerLevel level, BlockPos pos) {
		level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0f, 1.0f);
	}

	public static void fuseTick(ServerLevel level, BlockPos pos, int ticksLeft) {
		int count = ticksLeft < 20 ? 4 : 1;
		level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, count, 0.05, 0.05, 0.05, 0.01);
		level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, count, 0.05, 0.05, 0.05, 0.01);
	}

	/**
	 * The reward: the bomb goes off where it stands. A Kippenbom's chickens come from the caller, who
	 * knows the sum.
	 *
	 * @return false when the bomb was gone already
	 */
	public static boolean boom(ServerLevel level, BlockPos pos, BombTier tier) {
		if (!(level.getBlockState(pos).getBlock() instanceof BombBlock)) {
			return false;
		}
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

		Vec3 center = Vec3.atCenterOf(pos);
		level.explode(null, null, PlayerSafeExplosion.INSTANCE, center.x, center.y, center.z, tier.blastRadius(), false,
				Level.ExplosionInteraction.TNT);

		switch (tier) {
			case KNALLETJE -> {
				level.sendParticles(ParticleTypes.FIREWORK, center.x, center.y + 1, center.z, 80, 0.3, 0.3, 0.3, 0.25);
				level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 1.5f, 1.0f);
			}
			case GEWONE_TNT -> level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 1, center.z, 40, 1.0, 0.5, 1.0, 0.05);
			case KIPPENBOM -> {
				// Its chickens come from the caller.
			}
		}
		return true;
	}

	/** A Kippenbom going off: 12 chickens fly out in a ring. */
	public static void flyingChickens(ServerLevel level, Vec3 center) {
		releaseChickens(level, center, KIPPENBOM_CHICKENS, 0.6, 0.7);
		level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 1, center.z, 30, 0.5, 0.5, 0.5, 0.1);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 2.0f, 0.8f);
	}

	/** The counting Kippenbom's first generation: one chicken per group flies out in a ring. */
	public static List<Chicken> countingChickens(ServerLevel level, Vec3 center, int count, double speed) {
		List<Chicken> chickens = releaseChickens(level, center, count, speed, 0.45);
		level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 1, center.z, 30, 0.5, 0.5, 0.5, 0.1);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 2.0f, 0.8f);
		return chickens;
	}

	/**
	 * A first-generation chicken pops into its group where it landed: a tight cluster with no lift, so
	 * the group lands and stands together. It pops where it was last, even when a boom took it
	 * meanwhile, so the groups always add up to the sum.
	 */
	public static List<Chicken> popChicken(ServerLevel level, Chicken first, int count) {
		Vec3 at = first.position();
		first.discard();
		poof(level, at);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 1.0f, 1.6f);
		return releaseChickens(level, at, count, 0.03, 0);
	}

	/** Chickens that hop out low, for the Kippenbom's signature chase. */
	public static List<Chicken> chasingChickens(ServerLevel level, Vec3 at, int count) {
		return releaseChickens(level, at, count, 0.1, 0.2);
	}

	/** A wrong answer on a Kippenbom: a few chickens hop out at the kid, nothing like the real boom. */
	public static void hoppingChickens(ServerLevel level, Vec3 at, int count) {
		releaseChickens(level, at, count, 0.2, 0.3);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 1.0f, 1.5f);
	}

	private static List<Chicken> releaseChickens(ServerLevel level, Vec3 center, int count, double speed, double lift) {
		List<Chicken> released = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			Chicken chicken = EntityTypes.CHICKEN.create(level, EntitySpawnReason.TRIGGERED);
			if (chicken == null) {
				continue;
			}
			double angle = (Math.PI * 2 * i) / count;
			chicken.setPos(center.x, center.y + 0.5, center.z);
			chicken.setDeltaMovement(Math.cos(angle) * speed, lift + level.getRandom().nextDouble() * lift * 0.5, Math.sin(angle) * speed);
			chicken.addTag(SPECTACLE_TAG);
			level.addFreshEntity(chicken);
			released.add(chicken);
		}
		return released;
	}

	/**
	 * A wrong answer: short, harmless, and deliberately less fun than the real boom.
	 */
	public static void penalty(ServerLevel level, ServerPlayer player, BombTier tier) {
		Vec3 head = player.getEyePosition();
		level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
		switch (tier) {
			case KNALLETJE -> {
				level.sendParticles(ParticleTypes.LARGE_SMOKE, head.x, head.y, head.z, 25, 0.3, 0.3, 0.3, 0.02);
				player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false));
				level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.5f);
			}
			case GEWONE_TNT -> {
				level.sendParticles(ParticleTypes.ITEM_SNOWBALL, head.x, head.y + 0.2, head.z, 30, 0.2, 0.2, 0.2, 0.1);
				level.sendParticles(ParticleTypes.EGG_CRACK, head.x, head.y, head.z, 10, 0.2, 0.2, 0.2, 0.05);
				player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, false));
				level.playSound(null, player.blockPosition(), SoundEvents.EGG_THROW, SoundSource.PLAYERS, 1.0f, 0.6f);
			}
			case KIPPENBOM -> {
				level.sendParticles(ParticleTypes.FALLING_WATER, head.x, head.y + 1.5, head.z, 60, 0.4, 0.2, 0.4, 0.0);
				level.sendParticles(ParticleTypes.SPLASH, head.x, head.y, head.z, 40, 0.4, 0.3, 0.4, 0.1);
				player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, false));
				level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0f, 1.0f);
			}
		}
	}

	/**
	 * A typed wrong answer on a harder level costs hearts, but never the last half heart: a
	 * consequence, never a way out of the fight. Magic damage, so armour and difficulty don't change
	 * it; creative players can't be hurt at all.
	 */
	public static void hurt(ServerPlayer player, float amount) {
		float allowed = Math.min(amount, player.getHealth() - 1F);
		if (allowed > 0F) {
			player.hurtServer(player.level(), player.level().damageSources().magic(), allowed);
		}
	}

	/**
	 * The bomb goes out without a bang ("?" or chickening out).
	 */
	public static void fizzle(ServerLevel level, BlockPos pos) {
		if (level.getBlockState(pos).getBlock() instanceof BombBlock) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 15, 0.2, 0.2, 0.2, 0.02);
		level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 1.0f);
	}

	public static void chickenOut(ServerLevel level, BlockPos pos) {
		fizzle(level, pos);
		Chicken chicken = EntityTypes.CHICKEN.create(level, EntitySpawnReason.TRIGGERED);
		if (chicken != null) {
			chicken.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
			chicken.setDeltaMovement(0, 0.5, 0);
			chicken.addTag(SPECTACLE_TAG);
			level.addFreshEntity(chicken);
		}
		level.playSound(null, pos, SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 1.5f, 1.2f);
	}

	public static void correctAnswer(ServerLevel level, ServerPlayer player) {
		// A ring at the feet, so the sparkles don't block the child's view.
		for (int i = 0; i < 12; i++) {
			double angle = Math.PI * 2 * i / 12;
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX() + Math.cos(angle) * 1.2, player.getY() + 0.2,
					player.getZ() + Math.sin(angle) * 1.2, 1, 0, 0.1, 0, 0);
		}
		level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
	}

	public static void poof(ServerLevel level, Vec3 position) {
		level.sendParticles(ParticleTypes.POOF, position.x, position.y + 0.3, position.z, 8, 0.2, 0.2, 0.2, 0.02);
	}
}
