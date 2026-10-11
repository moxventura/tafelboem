package nl.jeeninga.tafelboem.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Every GameTest is boxed in by invisible barrier walls and a barrier floor, floating above the
 * world. Graaf Fout bounced off those walls after about a block, so yeet tests measured nothing.
 * This knocks the walls and ceiling down and lays a stone floor big enough for a full yeet.
 */
final class OpenField {
	/** Further than the longest yeet, but within the test's padding so he keeps ticking. */
	static final int RADIUS = 32;
	/** Chunks within this radius minus two get entity ticking: three chunks covers the floor. */
	private static final int TICKET_RADIUS = 5;

	private OpenField() {
	}

	static void open(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		AABB bounds = helper.getBounds();
		BlockPos low = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ).offset(-1, -1, -1);
		BlockPos high = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ);

		for (BlockPos pos : BlockPos.betweenClosed(low.above(), high)) {
			if (level.getBlockState(pos).is(Blocks.BARRIER)) {
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			}
		}

		BlockPos centre = BlockPos.containing(bounds.getCenter()).atY(low.getY());
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-RADIUS, 0, -RADIUS), centre.offset(RADIUS, 0, RADIUS))) {
			level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
		}

		// Only the test's own chunks are force-loaded, and entities outside them stop moving mid-air.
		// A ticket that expires by itself keeps the whole field ticking without un-forcing chunks
		// that a neighbouring test still needs.
		ChunkPos chunk = ChunkPos.containing(centre);
		helper.onEachTick(() -> level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, chunk, TICKET_RADIUS));
	}
}
