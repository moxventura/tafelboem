package nl.jeeninga.tafelboem.world;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.HubShape;
import nl.jeeninga.tafelboem.core.Shape;
import nl.jeeninga.tafelboem.game.BombEffects;
import nl.jeeninga.tafelboem.game.ProtectedAreas;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/**
 * The hub of a TafelBoem world: built from code unless a template {@code tafelboem:hub} with all
 * three level doors exists. It is placed on every world load, so it repairs itself, and booms
 * break nothing in it. Both are only about the hub's own box: what the kids build beside it is theirs.
 */
public final class Hub {
	public static final Identifier TEMPLATE = TafelBoem.id("hub");

	private Hub() {
	}

	/** @param templateId {@link #TEMPLATE}; GameTests use their own, so a hub the mod ships can't change them */
	public static PlacedArea place(ServerLevel level, WorldLayout layout, Identifier templateId) {
		ArenaBox slot = layout.hubSlot();
		Optional<StructureTemplate> template = level.getServer().getStructureManager().get(templateId)
				.filter(found -> hasAllDoors(templateId, found));
		PlacedArea hub;
		if (template.isPresent()) {
			hub = ShapePlacer.placeTemplate(level, clearFor(level, slot, template.get().getSize()), templateId.toString(), template.get());
		} else {
			Shape shape = HubShape.standard();
			hub = ShapePlacer.placeShape(level, clearFor(level, slot, new Vec3i(shape.sizeX(), shape.sizeY(), shape.sizeZ())), shape);
		}
		ProtectedAreas.protect(level, hub.bounds());
		return hub;
	}

	/**
	 * A hub frame saved by an earlier world load. Every load places the frames again, but the saved
	 * ones only load after that, with their chunk, so clearing can't catch them: they go when they
	 * load. The frames this hub placed are saved and loaded with their chunk too, and stay.
	 */
	public static boolean isLeftOverFrame(Entity entity, @Nullable PlacedArea hub) {
		return entity.entityTags().contains(BombEffects.HUB_TAG) && (hub == null || !hub.frames().contains(entity.getUUID()));
	}

	/**
	 * Clears only the box the hub is about to take up, and returns its corner: whatever the kids
	 * built beside or above the hub stays.
	 */
	private static BlockPos clearFor(ServerLevel level, ArenaBox slot, Vec3i size) {
		BlockPos corner = WorldLayout.cornerFor(slot, size.getX(), size.getZ());
		ShapePlacer.clear(level, ArenaBox.of(corner.getX(), corner.getY(), corner.getZ(), size.getX(), size.getY(), size.getZ()));
		return corner;
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
