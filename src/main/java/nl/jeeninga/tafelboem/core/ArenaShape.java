package nl.jeeninga.tafelboem.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The plain arenas, used while a level has no template: level 1 round on grass, level 2 square on
 * grass, level 3 round on a darker floor. Bedrock below and obsidian walls down to the bedrock: the
 * outside never breaks, the inside may.
 */
public final class ArenaShape {
	public static final int SIZE = 47;
	private static final int CENTRE = 23;
	private static final int WALL_BOTTOM = 3;
	private static final int WALL_HEIGHT = 4;

	/** Whether a column (x, z) belongs to a part of the arena. */
	private interface Area {
		boolean test(int x, int z);
	}

	private ArenaShape() {
	}

	public static Shape forLevel(int level) {
		return switch (level) {
			case 1 -> round(Shape.Material.GRASS);
			case 2 -> square(Shape.Material.GRASS);
			case 3 -> round(Shape.Material.COARSE_DIRT);
			default -> throw new IllegalArgumentException("No arena for level " + level);
		};
	}

	private static Shape round(Shape.Material top) {
		return build(top, (x, z) -> distance(x, z) <= CENTRE + 0.5, (x, z) -> distance(x, z) > CENTRE - 0.5);
	}

	private static Shape square(Shape.Material top) {
		return build(top, (x, z) -> true, (x, z) -> x == 0 || z == 0 || x == SIZE - 1 || z == SIZE - 1);
	}

	private static Shape build(Shape.Material top, Area floor, Area wall) {
		List<Shape.Cell> cells = new ArrayList<>();
		for (int x = 0; x < SIZE; x++) {
			for (int z = 0; z < SIZE; z++) {
				if (!floor.test(x, z)) {
					continue;
				}
				cells.add(Shape.Cell.of(x, 0, z, Shape.Material.BEDROCK));
				if (!wall.test(x, z)) {
					cells.add(Shape.Cell.of(x, 1, z, Shape.Material.DIRT));
					cells.add(Shape.Cell.of(x, 2, z, top));
					continue;
				}
				// The wall stands on obsidian down to the bedrock: on dirt, a crater beside it opened a way out (P18).
				cells.add(Shape.Cell.of(x, 1, z, Shape.Material.OBSIDIAN));
				cells.add(Shape.Cell.of(x, 2, z, Shape.Material.OBSIDIAN));
				for (int y = WALL_BOTTOM; y < WALL_BOTTOM + WALL_HEIGHT; y++) {
					boolean door = x == CENTRE && z == SIZE - 1 && y < WALL_BOTTOM + 2;
					if (door) {
						cells.add(new Shape.Cell(x, y, z, Shape.Material.DOOR, 0, Shape.Facing.NORTH));
					} else {
						cells.add(Shape.Cell.of(x, y, z, Shape.Material.OBSIDIAN));
					}
				}
			}
		}
		return new Shape("default", SIZE, WALL_BOTTOM + WALL_HEIGHT, SIZE, List.copyOf(cells),
				new Shape.Spot(CENTRE, WALL_BOTTOM, SIZE - 4, Shape.Facing.NORTH),
				new Shape.Spot(CENTRE, WALL_BOTTOM, CENTRE, Shape.Facing.NORTH), List.of(), List.of());
	}

	private static double distance(int x, int z) {
		return Math.hypot(x - CENTRE, z - CENTRE);
	}
}
