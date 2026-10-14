package nl.jeeninga.tafelboem.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The plain hub: a round stone platform with a door per level on its north side, a sign above each
 * door and that level's bombs beside it, and the spawn on the south side facing the doors.
 */
public final class HubShape {
	public static final int SIZE = 25;
	private static final int CENTRE = 12;
	private static final int RADIUS = 12;
	private static final int DOOR_Z = 5;

	private HubShape() {
	}

	public static Shape standard() {
		List<Shape.Cell> cells = new ArrayList<>();
		for (int x = 0; x < SIZE; x++) {
			for (int z = 0; z < SIZE; z++) {
				if (Math.hypot(x - CENTRE, z - CENTRE) <= RADIUS + 0.5) {
					cells.add(Shape.Cell.of(x, 0, z, Shape.Material.STONE));
				}
			}
		}

		List<Shape.Sign> signs = new ArrayList<>();
		List<Shape.Frame> frames = new ArrayList<>();
		for (FightLevel level : FightLevel.DOORS) {
			int centre = 4 + 8 * (level.number() - 1);
			for (int y = 1; y <= 5; y++) {
				cells.add(Shape.Cell.of(centre - 2, y, DOOR_Z, Shape.Material.STONE_BRICKS));
				cells.add(Shape.Cell.of(centre + 2, y, DOOR_Z, Shape.Material.STONE_BRICKS));
			}
			for (int x = centre - 1; x <= centre + 1; x++) {
				cells.add(Shape.Cell.of(x, 5, DOOR_Z, Shape.Material.STONE_BRICKS));
				for (int y = 1; y <= 4; y++) {
					cells.add(new Shape.Cell(x, y, DOOR_Z, Shape.Material.DOOR, level.number(), Shape.Facing.SOUTH));
				}
			}
			signs.add(new Shape.Sign(centre, 5, DOOR_Z + 1, Shape.Facing.SOUTH, level.number()));
			List<BombTier> bombs = level.bombs();
			frames.add(new Shape.Frame(centre - 2, 2, DOOR_Z + 1, Shape.Facing.SOUTH, bombs.getFirst()));
			if (bombs.size() > 1) {
				frames.add(new Shape.Frame(centre + 2, 2, DOOR_Z + 1, Shape.Facing.SOUTH, bombs.get(1)));
			}
		}
		return new Shape("default", SIZE, 6, SIZE, List.copyOf(cells), new Shape.Spot(CENTRE, 1, 21, Shape.Facing.NORTH),
				new Shape.Spot(CENTRE, 1, CENTRE, Shape.Facing.NORTH), List.copyOf(signs), List.copyOf(frames));
	}
}
