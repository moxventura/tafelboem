package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class HubShapeTest {
	private final Shape hub = HubShape.standard();

	@Test
	void fitsOneStructureBlock() {
		assertTrue(hub.sizeX() <= 48 && hub.sizeY() <= 48 && hub.sizeZ() <= 48);
	}

	@Test
	void aDoorPerLevelOnThePlatformFacingTheSpawn() {
		for (FightLevel level : FightLevel.DOORS) {
			List<Shape.Cell> door = hub.cells().stream()
					.filter(cell -> cell.material() == Shape.Material.DOOR && cell.doorLevel() == level.number())
					.toList();
			assertEquals(12, door.size(), "a 3 × 4 doorway for " + level);
			for (Shape.Cell cell : door) {
				assertEquals(Shape.Facing.SOUTH, cell.facing());
				assertEquals(Shape.Material.STONE, hub.cellAt(cell.x(), 0, cell.z()).orElseThrow().material(), "on the platform");
			}
		}
	}

	@Test
	void aSignAboveEachDoorAndItsBombsBesideIt() {
		assertEquals(List.of(1, 2, 3), hub.signs().stream().map(Shape.Sign::level).toList());
		assertEquals(List.of(BombTier.KNALLETJE, BombTier.GEWONE_TNT, BombTier.GEWONE_TNT, BombTier.KIPPENBOM, BombTier.KIPPENBOM),
				hub.frames().stream().map(Shape.Frame::bomb).toList());
	}

	@Test
	void theKidsSpawnOnThePlatformFacingTheDoors() {
		Shape.Spot spawn = hub.standAt();
		assertEquals(Shape.Facing.NORTH, spawn.facing());
		assertTrue(hub.cellAt(spawn.x(), 0, spawn.z()).isPresent());
		assertTrue(hub.cellAt(spawn.x(), 1, spawn.z()).isEmpty() && hub.cellAt(spawn.x(), 2, spawn.z()).isEmpty());
	}
}
