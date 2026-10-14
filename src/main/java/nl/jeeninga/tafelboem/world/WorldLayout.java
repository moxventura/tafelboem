package nl.jeeninga.tafelboem.world;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;

import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.FightLevel;

/**
 * Where the hub and the arena slots are. Every slot is a 48³ box (what one structure block can
 * save); a hub or arena is placed in the middle of its slot.
 */
public record WorldLayout(BlockPos hub, Map<FightLevel, BlockPos> arenas) {
	public static final int SLOT = 48;
	/** The real world: the hub around spawn, the arenas 512 blocks apart so they never see each other. */
	public static final WorldLayout DEFAULT = at(new BlockPos(-SLOT / 2, 60, -SLOT / 2), 512);

	public static WorldLayout at(BlockPos hubCorner, int spacing) {
		Map<FightLevel, BlockPos> arenas = new EnumMap<>(FightLevel.class);
		for (FightLevel level : FightLevel.DOORS) {
			arenas.put(level, hubCorner.offset(spacing * level.number(), 0, 0));
		}
		return new WorldLayout(hubCorner, Map.copyOf(arenas));
	}

	public ArenaBox hubSlot() {
		return ArenaBox.of(hub.getX(), hub.getY(), hub.getZ(), SLOT, SLOT, SLOT);
	}

	public ArenaBox arenaSlot(FightLevel level) {
		BlockPos corner = arenas.get(level);
		return ArenaBox.of(corner.getX(), corner.getY(), corner.getZ(), SLOT, SLOT, SLOT);
	}

	/** Below this a kid has fallen into the void. */
	public int voidY() {
		return hub.getY() - 8;
	}

	/** The arena slot above or below this spot, if any. */
	public Optional<FightLevel> arenaAt(double x, double z) {
		return FightLevel.DOORS.stream().filter(level -> arenaSlot(level).containsColumn(x, z)).findFirst();
	}

	/** Where something of this size goes so it sits in the middle of the slot, on its bottom. */
	public static BlockPos cornerFor(ArenaBox slot, int sizeX, int sizeZ) {
		return new BlockPos(slot.minX() + (SLOT - sizeX) / 2, slot.minY(), slot.minZ() + (SLOT - sizeZ) / 2);
	}
}
