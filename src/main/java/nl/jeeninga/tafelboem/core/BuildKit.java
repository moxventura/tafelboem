package nl.jeeninga.tafelboem.core;

import java.util.List;

/**
 * What the build phase's double chest holds, in chest order and in groups so the kids can find
 * things: building, colour, bouncing and sticking, traps, redstone, light and decoration, tools. Enough for
 * two kids for 3–5 minutes; whatever a kid doesn't place stays theirs.
 */
public final class BuildKit {
	/** A stack in the chest: a vanilla item id without its namespace, and how many. */
	public record Stack(String item, int count) {
	}

	public static final int SLOTS = 54;

	public static final List<Stack> CHEST = List.of(
			// Building
			new Stack("stone_bricks", 64), new Stack("oak_planks", 64), new Stack("glass", 64),
			new Stack("stone_brick_stairs", 32), new Stack("oak_stairs", 32), new Stack("stone_brick_slab", 32), new Stack("oak_slab", 32),
			new Stack("scaffolding", 32), new Stack("ladder", 32), new Stack("oak_fence", 32), new Stack("oak_fence_gate", 32),
			new Stack("oak_door", 32), new Stack("oak_trapdoor", 32),
			// Colour
			new Stack("red_wool", 32), new Stack("yellow_wool", 32), new Stack("light_blue_wool", 32),
			new Stack("red_stained_glass", 32), new Stack("yellow_stained_glass", 32), new Stack("light_blue_stained_glass", 32),
			new Stack("white_concrete", 32), new Stack("lime_concrete", 32), new Stack("magenta_concrete", 32),
			// Bouncing and sticking
			new Stack("slime_block", 32), new Stack("honey_block", 32),
			// Traps (R1.3), with what cactus and berries grow on
			new Stack("cactus", 16), new Stack("sand", 16), new Stack("sweet_berries", 16), new Stack("dirt", 16),
			new Stack("magma_block", 16), new Stack("cobweb", 16),
			// Redstone
			new Stack("lever", 8), new Stack("stone_button", 8), new Stack("stone_pressure_plate", 8), new Stack("redstone", 8),
			new Stack("repeater", 4), new Stack("piston", 4), new Stack("sticky_piston", 4), new Stack("redstone_lamp", 4),
			new Stack("note_block", 4), new Stack("target", 2), new Stack("observer", 2),
			// Light and decoration
			new Stack("lantern", 8), new Stack("sea_lantern", 8), new Stack("end_rod", 8), new Stack("candle", 8),
			new Stack("poppy", 4), new Stack("dandelion", 4), new Stack("flower_pot", 4), new Stack("oak_sign", 4),
			new Stack("item_frame", 4), new Stack("armor_stand", 1), new Stack("white_banner", 1),
			// Tools
			new Stack("stone_pickaxe", 1), new Stack("stone_axe", 1));

	private static final List<String> TOOLS = List.of("stone_pickaxe", "stone_axe");

	/** What the build line counts as placed: the kit without the tools (a tool's "used" counts mining). */
	public static final List<String> PLACEABLE = CHEST.stream().map(Stack::item).filter(item -> !TOOLS.contains(item)).toList();

	/**
	 * Never in the kit, as parts of an id: our bombs are the only explosives; no fire (it spreads
	 * through planks and wool), floods or a floating arena; no beds or spawn eggs.
	 */
	public static final List<String> LEFT_OUT = List.of("tnt", "lava", "water", "flint_and_steel", "fire_charge", "dispenser", "_bed",
			"spawn_egg");

	private BuildKit() {
	}
}
