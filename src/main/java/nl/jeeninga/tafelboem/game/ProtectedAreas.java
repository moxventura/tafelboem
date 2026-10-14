package nl.jeeninga.tafelboem.game;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import nl.jeeninga.tafelboem.core.ArenaBox;

/**
 * Where our booms break no blocks, such as the hub of a TafelBoem world: a Kippenbom there still
 * goes boom, but doesn't blow a hole through to the void.
 */
public final class ProtectedAreas {
	private static final Map<ResourceKey<Level>, List<ArenaBox>> AREAS = new ConcurrentHashMap<>();

	private ProtectedAreas() {
	}

	public static void protect(ServerLevel level, ArenaBox box) {
		List<ArenaBox> boxes = AREAS.computeIfAbsent(level.dimension(), dimension -> new CopyOnWriteArrayList<>());
		if (!boxes.contains(box)) {
			boxes.add(box);
		}
	}

	public static void unprotect(ServerLevel level, ArenaBox box) {
		List<ArenaBox> boxes = AREAS.get(level.dimension());
		if (boxes != null) {
			boxes.remove(box);
		}
	}

	public static boolean isProtected(ServerLevel level, BlockPos pos) {
		return AREAS.getOrDefault(level.dimension(), List.of()).stream().anyMatch(box -> box.contains(pos.getX(), pos.getY(), pos.getZ()));
	}

	public static void clear() {
		AREAS.clear();
	}
}
