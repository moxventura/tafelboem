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

	/** @param folder {@link #DEFAULT_FOLDER}; GameTests use their own, so their templates don't mix */
	public ArenaCatalog(StructureTemplateManager templates, Random random, String folder) {
		this.templates = templates;
		this.random = random;
		this.folder = folder;
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

		ShapePlacer.clear(level, slot);
		if (!usable.isEmpty()) {
			ArenaPicker picker = pickers.computeIfAbsent(fightLevel, key -> new ArenaPicker(random));
			String name = picker.pick(List.copyOf(usable.keySet()));
			StructureTemplate found = usable.get(name);
			return ShapePlacer.placeTemplate(level, WorldLayout.cornerFor(slot, found.getSize().getX(), found.getSize().getZ()), name, found);
		}
		Shape shape = ArenaShape.forLevel(fightLevel.number());
		return ShapePlacer.placeShape(level, WorldLayout.cornerFor(slot, shape.sizeX(), shape.sizeZ()), shape);
	}

	public static boolean hasExitDoor(StructureTemplate template) {
		return template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), ModBlocks.LEVEL_DOOR).stream()
				.anyMatch(info -> info.state().getValue(LevelDoorBlock.LEVEL) == 0);
	}
}
