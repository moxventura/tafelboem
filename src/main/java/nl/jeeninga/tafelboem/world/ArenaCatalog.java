package nl.jeeninga.tafelboem.world;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.ArenaPicker;
import nl.jeeninga.tafelboem.core.ArenaShape;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.Shape;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/**
 * Each level's arenas: the templates named {@code tafelboem:level_<n>/<name>} (in the world's own
 * folder or in the mod), or the plain arena from code when there are none. A template without an
 * exit door is skipped.
 */
public final class ArenaCatalog {
	public static final String DEFAULT_FOLDER = "level_";

	private final StructureTemplateManager templates;
	private final Random random;
	private final String folder;
	private final Map<FightLevel, ArenaPicker> pickers = new EnumMap<>(FightLevel.class);
	/** Where each level's last arena stands: empty when its slot is known to be empty, missing when not known. */
	private final Map<FightLevel, Optional<ArenaBox>> lastPlaced = new EnumMap<>(FightLevel.class);

	/** @param folder {@link #DEFAULT_FOLDER}; GameTests use their own, so their templates don't mix */
	public ArenaCatalog(StructureTemplateManager templates, Random random, String folder) {
		this.templates = templates;
		this.random = random;
		this.folder = folder;
	}

	/**
	 * Empties every arena slot. A world load does this once, before anyone plays: from then on a door
	 * only clears where that level's last arena stood, so walking through it doesn't stall the game.
	 */
	public void clearSlots(ServerLevel level, WorldLayout layout) {
		for (FightLevel fightLevel : FightLevel.DOORS) {
			ShapePlacer.clear(level, layout.arenaSlot(fightLevel));
			lastPlaced.put(fightLevel, Optional.empty());
		}
	}

	public List<Identifier> templatesFor(FightLevel level) {
		String prefix = folder + level.number() + "/";
		return templates.listTemplates()
				.filter(id -> id.getNamespace().equals(TafelBoem.MOD_ID) && id.getPath().startsWith(prefix))
				.sorted()
				.toList();
	}

	/** Clears the level's slot and places one of its arenas, picked at random, never twice in a row. */
	public PlacedArea place(ServerLevel level, WorldLayout layout, FightLevel fightLevel, Consumer<String> skipped) {
		ArenaBox slot = layout.arenaSlot(fightLevel);
		// Check them all before picking: a pick that is then skipped would break "never twice in a row".
		Map<String, StructureTemplate> usable = new LinkedHashMap<>();
		for (Identifier id : templatesFor(fightLevel)) {
			Optional<StructureTemplate> template = templates.get(id);
			if (template.isPresent() && hasExitDoor(template.get())) {
				usable.put(id.toString(), template.get());
			} else {
				skipped.accept(id.toString());
			}
		}

		// Blocks only where the last arena stood (all of the slot when that isn't known); stray entities anywhere in it.
		lastPlaced.getOrDefault(fightLevel, Optional.of(slot)).ifPresent(box -> ShapePlacer.clearBlocks(level, box));
		ShapePlacer.clearEntities(level, slot);
		PlacedArea arena;
		if (!usable.isEmpty()) {
			ArenaPicker picker = pickers.computeIfAbsent(fightLevel, key -> new ArenaPicker(random));
			String name = picker.pick(List.copyOf(usable.keySet()));
			StructureTemplate found = usable.get(name);
			arena = ShapePlacer.placeTemplate(level, WorldLayout.cornerFor(slot, found.getSize().getX(), found.getSize().getZ()), name, found);
		} else {
			Shape shape = ArenaShape.forLevel(fightLevel.number());
			arena = ShapePlacer.placeShape(level, WorldLayout.cornerFor(slot, shape.sizeX(), shape.sizeZ()), shape);
		}
		lastPlaced.put(fightLevel, Optional.of(arena.bounds()));
		return arena;
	}

	public static boolean hasExitDoor(StructureTemplate template) {
		return template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), ModBlocks.LEVEL_DOOR).stream()
				.anyMatch(info -> info.state().getValue(LevelDoorBlock.LEVEL) == 0);
	}
}
