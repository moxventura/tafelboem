package nl.jeeninga.tafelboem.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.core.BombTier;

/**
 * A bomb that can only be set off by answering a sum. It is a plain {@link Block} on purpose, so
 * fire and redstone can't light it.
 */
public class BombBlock extends Block {
	private final BombTier tier;

	public BombBlock(BombTier tier, Properties properties) {
		super(properties);
		this.tier = tier;
	}

	public BombTier tier() {
		return tier;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (player instanceof ServerPlayer serverPlayer) {
			TafelBoem.sessions().startFromBlock(serverPlayer, tier, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0.0, 0.02, 0.0);
		}
	}
}
