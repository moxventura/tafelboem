package nl.jeeninga.tafelboem.world;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.core.ArenaBox;

/**
 * A hub or arena as it was placed: where the kids stand (the spawn, or 3 blocks in front of the
 * exit door), where Graaf Fout appears, and every level door in it.
 *
 * @param name the template's name, or "default" for the plain one from code
 */
public record PlacedArea(String name, ArenaBox bounds, BlockPos standAt, float yaw, Vec3 middle, Map<Integer, List<BlockPos>> doors) {
}
