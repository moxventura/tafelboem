package nl.jeeninga.tafelboem.world;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.HubShape;
import nl.jeeninga.tafelboem.game.ProtectedAreas;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/**
 * The hub of a TafelBoem world: built from code unless a template {@code tafelboem:hub} with all
 * three level doors exists. It is placed on every world load, so it repairs itself, and booms
 * break nothing in it.
 */
public final class Hub {
	public static final Identifier TEMPLATE = TafelBoem.id("hub");

	private Hub() {
	}

	public static PlacedArea place(ServerLevel level, WorldLayout layout) {
		return place(level, layout, TEMPLATE);
	}

	/** @param templateId {@link #TEMPLATE}; GameTests use their own, so a hub the mod ships can't change them */
	public static PlacedArea place(ServerLevel level, WorldLayout layout, Identifier templateId) {
		ArenaBox slot = layout.hubSlot();
		ShapePlacer.clear(level, slot);
		Optional<StructureTemplate> template = level.getServer().getStructureManager().get(templateId)
				.filter(found -> hasAllDoors(templateId, found));
		PlacedArea hub = template
				.map(found -> ShapePlacer.placeTemplate(level, WorldLayout.cornerFor(slot, found.getSize().getX(), found.getSize().getZ()),
						templateId.toString(), found))
				.orElseGet(() -> ShapePlacer.placeShape(level, WorldLayout.cornerFor(slot, HubShape.SIZE, HubShape.SIZE), HubShape.standard()));
		ProtectedAreas.protect(level, slot);
		return hub;
	}

	private static boolean hasAllDoors(Identifier templateId, StructureTemplate template) {
		Set<Integer> levels = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), ModBlocks.LEVEL_DOOR).stream()
				.map(info -> info.state().getValue(LevelDoorBlock.LEVEL))
				.collect(Collectors.toSet());
		if (!levels.containsAll(Set.of(1, 2, 3))) {
			TafelBoem.LOGGER.warn("The hub template {} needs a door to level 1, 2 and 3; using the plain hub", templateId);
			return false;
		}
		return true;
	}
}
