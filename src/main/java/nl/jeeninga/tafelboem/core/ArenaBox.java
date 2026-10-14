package nl.jeeninga.tafelboem.core;

/**
 * A box of whole blocks with both corners included: a hub or arena slot, or the area a placed
 * arena takes up.
 */
public record ArenaBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
	public static ArenaBox of(int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
		return new ArenaBox(x, y, z, x + sizeX - 1, y + sizeY - 1, z + sizeZ - 1);
	}

	public boolean contains(int x, int y, int z) {
		return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
	}

	/** Above the footprint, at any height: the open sky over an arena still belongs to it. */
	public boolean containsColumn(double x, double z) {
		return x >= minX && x < maxX + 1 && z >= minZ && z < maxZ + 1;
	}

	/** A kid or Graaf Fout in the arena: over its footprint and not fallen through its bottom. */
	public boolean holds(double x, double y, double z) {
		return containsColumn(x, z) && y >= minY - 2;
	}

	public double middleX() {
		return (minX + maxX + 1) / 2.0;
	}

	public double middleZ() {
		return (minZ + maxZ + 1) / 2.0;
	}
}
