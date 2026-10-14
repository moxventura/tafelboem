package nl.jeeninga.tafelboem.world;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.core.Shape;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/** The vanilla block for each material of a {@link Shape}. */
final class Materials {
	private Materials() {
	}

	static BlockState state(Shape.Cell cell) {
		return switch (cell.material()) {
			case BEDROCK -> Blocks.BEDROCK.defaultBlockState();
			case DIRT -> Blocks.DIRT.defaultBlockState();
			case GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
			case COARSE_DIRT -> Blocks.COARSE_DIRT.defaultBlockState();
			case STONE -> Blocks.STONE.defaultBlockState();
			case STONE_BRICKS -> Blocks.STONE_BRICKS.defaultBlockState();
			case OBSIDIAN -> Blocks.OBSIDIAN.defaultBlockState();
			case DOOR -> ModBlocks.LEVEL_DOOR.defaultBlockState()
					.setValue(LevelDoorBlock.LEVEL, cell.doorLevel())
					.setValue(LevelDoorBlock.FACING, direction(cell.facing()));
		};
	}

	static Direction direction(Shape.Facing facing) {
		return switch (facing) {
			case NORTH -> Direction.NORTH;
			case SOUTH -> Direction.SOUTH;
			case EAST -> Direction.EAST;
			case WEST -> Direction.WEST;
		};
	}
}
