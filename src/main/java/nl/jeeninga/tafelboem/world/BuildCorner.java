package nl.jeeninga.tafelboem.world;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.ChestType;

import nl.jeeninga.tafelboem.core.BuildKit;

/**
 * The build phase's corner of an arena: a double chest with the {@link BuildKit} and a bell, a few
 * blocks ahead of where the kids arrive, so they see both first and the exit door is behind them.
 */
public final class BuildCorner {
	/** Blocks from the arrival spot, in the way the kids look. */
	static final int AHEAD = 3;

	private BuildCorner() {
	}

	/** The chest's first half (the one {@link #place} fills from), three blocks ahead of the arrival spot. */
	public static BlockPos chestAt(PlacedArea arena) {
		return arena.standAt().relative(ahead(arena), AHEAD);
	}

	public static BlockPos bellAt(PlacedArea arena) {
		return chestAt(arena).relative(front(arena).getCounterClockWise(), 2);
	}

	/** Places the chest, fills it, and places the bell; whatever stood there goes. */
	static void place(ServerLevel level, PlacedArea arena) {
		Direction front = front(arena);
		BlockPos left = chestAt(arena);
		// A chest's LEFT half finds its RIGHT half clockwise of where it faces (ChestBlock.getConnectedDirection).
		BlockPos right = left.relative(front.getClockWise());
		BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, front);
		level.setBlock(left, chest.setValue(ChestBlock.TYPE, ChestType.LEFT), Block.UPDATE_CLIENTS);
		level.setBlock(right, chest.setValue(ChestBlock.TYPE, ChestType.RIGHT), Block.UPDATE_CLIENTS);
		Container container = ChestBlock.getContainer((ChestBlock) Blocks.CHEST, level.getBlockState(left), level, left, true);
		if (container == null || container.getContainerSize() < BuildKit.SLOTS) {
			throw new IllegalStateException("No double chest at " + left);
		}
		List<BuildKit.Stack> kit = BuildKit.CHEST;
		for (int slot = 0; slot < kit.size(); slot++) {
			container.setItem(slot, stack(kit.get(slot)));
		}

		// A floor bell rings when hit on a side along its facing axis: the side the kids see from the arrival spot.
		level.setBlock(bellAt(arena), Blocks.BELL.defaultBlockState()
				.setValue(BellBlock.FACING, front)
				.setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR), Block.UPDATE_CLIENTS);
	}

	static ItemStack stack(BuildKit.Stack stack) {
		return new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(stack.item()))
				.orElseThrow(() -> new IllegalStateException("No item " + stack.item() + " in this Minecraft")), stack.count());
	}

	/** The way the kids look when they arrive. */
	private static Direction ahead(PlacedArea arena) {
		return Direction.fromYRot(arena.yaw());
	}

	/** The way the chest's lid and the bell face: back at the kids. */
	private static Direction front(PlacedArea arena) {
		return ahead(arena).getOpposite();
	}
}
