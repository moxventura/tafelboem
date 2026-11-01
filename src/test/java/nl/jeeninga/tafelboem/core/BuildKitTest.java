package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class BuildKitTest {
	@Test
	void theKitFitsInADoubleChest() {
		assertEquals(54, BuildKit.SLOTS);
		assertTrue(BuildKit.CHEST.size() <= BuildKit.SLOTS, BuildKit.CHEST.size() + " stacks");
	}

	@Test
	void everyStackIsAVanillaStackOfOneToSixtyFour() {
		Set<String> seen = new HashSet<>();
		for (BuildKit.Stack stack : BuildKit.CHEST) {
			assertTrue(stack.count() >= 1 && stack.count() <= 64, stack.toString());
			assertTrue(stack.item().matches("[a-z_]+"), "a vanilla id without namespace: " + stack.item());
			assertTrue(seen.add(stack.item()), "once in the kit: " + stack.item());
		}
	}

	@Test
	void nothingThatHurtsFloodsOrBurnsIsInTheKit() {
		for (BuildKit.Stack stack : BuildKit.CHEST) {
			for (String leftOut : BuildKit.LEFT_OUT) {
				assertFalse(stack.item().contains(leftOut), stack.item() + " is left out on purpose (" + leftOut + ")");
			}
		}
		assertTrue(BuildKit.LEFT_OUT.containsAll(List.of("tnt", "lava", "water", "flint_and_steel", "cactus", "sweet_berries", "magma",
				"dispenser", "_bed", "spawn_egg")));
	}

	@Test
	void theKitHasSomethingFromEveryGroup() {
		List<String> items = BuildKit.CHEST.stream().map(BuildKit.Stack::item).toList();
		assertTrue(items.containsAll(List.of("stone_bricks", "oak_planks", "glass", "scaffolding", "ladder")), "building");
		assertTrue(items.containsAll(List.of("red_wool", "yellow_stained_glass", "lime_concrete")), "colour");
		assertTrue(items.containsAll(List.of("slime_block", "honey_block")), "bouncing and sticking");
		assertTrue(items.containsAll(List.of("lever", "redstone", "piston", "sticky_piston", "redstone_lamp", "note_block")), "redstone");
		assertTrue(items.containsAll(List.of("lantern", "sea_lantern", "flower_pot", "armor_stand", "white_banner")), "light and decoration");
		assertTrue(items.containsAll(List.of("stone_pickaxe", "stone_axe")), "tools");
	}

	@Test
	void theBigBuildingBlocksComeInFullStacks() {
		for (String full : List.of("stone_bricks", "oak_planks", "glass")) {
			assertEquals(64, BuildKit.CHEST.stream().filter(stack -> stack.item().equals(full)).findFirst().orElseThrow().count(), full);
		}
	}

	@Test
	void everythingButTheToolsCountsAsPlaced() {
		assertFalse(BuildKit.PLACEABLE.contains("stone_pickaxe"));
		assertFalse(BuildKit.PLACEABLE.contains("stone_axe"));
		assertEquals(BuildKit.CHEST.size() - 2, BuildKit.PLACEABLE.size());
	}
}
