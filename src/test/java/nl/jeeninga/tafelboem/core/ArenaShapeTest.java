package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ArenaShapeTest {
	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3})
	void fitsOneStructureBlock(int level) {
		Shape shape = ArenaShape.forLevel(level);
		assertTrue(shape.sizeX() <= 48 && shape.sizeY() <= 48 && shape.sizeZ() <= 48);
		for (Shape.Cell cell : shape.cells()) {
			assertTrue(cell.x() >= 0 && cell.x() < shape.sizeX() && cell.y() >= 0 && cell.y() < shape.sizeY()
					&& cell.z() >= 0 && cell.z() < shape.sizeZ(), "outside the shape: " + cell);
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3})
	void bedrockAndDirtUnderEveryFloorBlock(int level) {
		Shape shape = ArenaShape.forLevel(level);
		for (Shape.Cell top : shape.cells().stream().filter(cell -> cell.y() == 2 && shape.cellAt(cell.x(), 3, cell.z()).isEmpty()).toList()) {
			assertEquals(Shape.Material.BEDROCK, shape.cellAt(top.x(), 0, top.z()).orElseThrow().material());
			assertEquals(Shape.Material.DIRT, shape.cellAt(top.x(), 1, top.z()).orElseThrow().material());
		}
	}

	/** P18: a wall on dirt and grass let a crater beside it open a way out under it, into the void. */
	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3})
	void wallsStandOnObsidianDownToTheBedrock(int level) {
		Shape shape = ArenaShape.forLevel(level);
		for (Shape.Cell wall : shape.cells().stream().filter(cell -> cell.y() == 3).toList()) {
			assertEquals(Shape.Material.BEDROCK, shape.cellAt(wall.x(), 0, wall.z()).orElseThrow().material());
			for (int y = 1; y <= 2; y++) {
				assertEquals(Shape.Material.OBSIDIAN, shape.cellAt(wall.x(), y, wall.z()).orElseThrow().material(),
						"under the wall at " + wall.x() + "," + y + "," + wall.z());
			}
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3})
	void obsidianWallsFourHighWithOneExitDoor(int level) {
		Shape shape = ArenaShape.forLevel(level);
		List<Shape.Cell> doors = shape.cells().stream().filter(cell -> cell.material() == Shape.Material.DOOR).toList();
		assertEquals(2, doors.size(), "one exit door, two blocks high");
		for (Shape.Cell door : doors) {
			assertEquals(0, door.doorLevel(), "the exit door leads back to the hub");
			assertEquals(Shape.Facing.NORTH, door.facing(), "it faces into the arena");
			assertEquals(ArenaShape.SIZE - 1, door.z(), "in the south wall");
		}
		for (Shape.Cell wall : shape.cells().stream().filter(cell -> cell.y() == 3).toList()) {
			for (int y = 3; y <= 6; y++) {
				Shape.Material material = shape.cellAt(wall.x(), y, wall.z()).orElseThrow().material();
				assertTrue(material == Shape.Material.OBSIDIAN || material == Shape.Material.DOOR && y <= 4,
						"wall at " + wall.x() + "," + y + "," + wall.z() + " is " + material);
			}
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3})
	void theKidsArriveThreeBlocksInFrontOfTheExitDoor(int level) {
		Shape shape = ArenaShape.forLevel(level);
		Shape.Spot spot = shape.standAt();
		assertEquals(new Shape.Spot(23, 3, ArenaShape.SIZE - 4, Shape.Facing.NORTH), spot);
		assertTrue(shape.cellAt(spot.x(), 3, spot.z()).isEmpty() && shape.cellAt(spot.x(), 4, spot.z()).isEmpty(), "room to stand");
		assertTrue(shape.cellAt(spot.x(), 2, spot.z()).isPresent(), "ground to stand on");
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3})
	void graafFoutAppearsInTheFreeMiddle(int level) {
		Shape shape = ArenaShape.forLevel(level);
		assertEquals(new Shape.Spot(23, 3, 23, Shape.Facing.NORTH), shape.middle());
		assertTrue(shape.cellAt(23, 3, 23).isEmpty());
		assertTrue(shape.cellAt(23, 2, 23).isPresent());
	}

	@Test
	void eachLevelLooksDifferent() {
		assertTrue(ArenaShape.forLevel(1).cellAt(0, 0, 0).isEmpty(), "level 1 is round");
		assertTrue(ArenaShape.forLevel(2).cellAt(0, 0, 0).isPresent(), "level 2 is square");
		assertTrue(ArenaShape.forLevel(3).cellAt(0, 0, 0).isEmpty(), "level 3 is round");
		assertEquals(Shape.Material.GRASS, ArenaShape.forLevel(1).cellAt(23, 2, 23).orElseThrow().material());
		assertEquals(Shape.Material.GRASS, ArenaShape.forLevel(2).cellAt(23, 2, 23).orElseThrow().material());
		assertEquals(Shape.Material.COARSE_DIRT, ArenaShape.forLevel(3).cellAt(23, 2, 23).orElseThrow().material());
		assertFalse(ArenaShape.forLevel(2).cells().isEmpty());
	}
}
