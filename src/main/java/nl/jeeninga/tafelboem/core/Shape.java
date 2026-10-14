package nl.jeeninga.tafelboem.core;

import java.util.List;
import java.util.Optional;

/**
 * A plain hub or arena built from code: blocks relative to its corner, and the spots the world
 * needs (where the kids stand, the middle, signs and item frames). Pure Java, so the shapes can be
 * tested without Minecraft.
 */
public record Shape(String name, int sizeX, int sizeY, int sizeZ, List<Cell> cells, Spot standAt, Spot middle,
		List<Sign> signs, List<Frame> frames) {
	/** The blocks a plain shape is built from. */
	public enum Material {
		BEDROCK,
		DIRT,
		GRASS,
		COARSE_DIRT,
		STONE,
		STONE_BRICKS,
		OBSIDIAN,
		DOOR
	}

	/** The way something looks: a door faces the kids, a spot faces where they look. */
	public enum Facing {
		NORTH(0, -1),
		SOUTH(0, 1),
		EAST(1, 0),
		WEST(-1, 0);

		private final int dx;
		private final int dz;

		Facing(int dx, int dz) {
			this.dx = dx;
			this.dz = dz;
		}

		public int dx() {
			return dx;
		}

		public int dz() {
			return dz;
		}
	}

	/** One block. A door leads to its level (1–3) or back to the hub (0) and faces where the kids come from. */
	public record Cell(int x, int y, int z, Material material, int doorLevel, Facing facing) {
		static Cell of(int x, int y, int z, Material material) {
			return new Cell(x, y, z, material, 0, Facing.NORTH);
		}
	}

	/** A place to stand, looking in the given direction. */
	public record Spot(int x, int y, int z, Facing facing) {
	}

	/** A sign naming a level, on the front of the block behind it. */
	public record Sign(int x, int y, int z, Facing facing, int level) {
	}

	/** An item frame showing a bomb, hanging on the block behind it. */
	public record Frame(int x, int y, int z, Facing facing, BombTier bomb) {
	}

	public Optional<Cell> cellAt(int x, int y, int z) {
		return cells.stream().filter(cell -> cell.x() == x && cell.y() == y && cell.z() == z).findFirst();
	}
}
