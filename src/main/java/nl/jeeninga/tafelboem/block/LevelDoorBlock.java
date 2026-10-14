package nl.jeeninga.tafelboem.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A walk-through door in the hub (level 1–3) or an arena (level 0, back to the hub). It faces where
 * the kids come from: in an arena, the kids arrive 3 blocks in front of it.
 */
public class LevelDoorBlock extends Block {
	public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 3);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** Who walked into which door is the TafelBoem world's business; the block only reports it. */
	public interface DoorUser {
		void walkedInto(ServerPlayer player, int level);
	}

	public static final DoorUser NOBODY = (player, level) -> {
	};
	private static DoorUser user = NOBODY;

	public LevelDoorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LEVEL, 1).setValue(FACING, Direction.NORTH));
	}

	public static void use(DoorUser door) {
		user = door;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LEVEL, FACING);
	}

	/** It faces the builder, so a door placed in an arena wall faces into the arena. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** Picking a placed door gives a door to the same level, as the creative tab has it. */
	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return withLevel(super.getCloneItemStack(level, pos, state, includeData), state.getValue(LEVEL));
	}

	/** Makes this door item lead to the level (0 is the exit), with its name from the creative tab. */
	public static ItemStack withLevel(ItemStack door, int level) {
		door.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(LEVEL, level));
		door.set(DataComponents.ITEM_NAME, level == 0
				? Component.translatable("tafelboem.door.item.exit")
				: Component.translatable("tafelboem.door.item.level", Component.translatable("tafelboem.level." + level)));
		return door;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects,
			boolean pastEdges) {
		if (entity instanceof ServerPlayer player) {
			user.walkedInto(player, state.getValue(LEVEL));
		}
	}
}
