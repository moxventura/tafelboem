package nl.jeeninga.tafelboem.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.world.WorldLayout;

/**
 * A TafelBoem world inside a GameTest: the hub slot at the test's corner and the arena slots beside
 * it, 56 blocks apart. Use {@code padding = 232} so neighbouring tests stay clear.
 */
final class TestWorld {
	static final int SPACING = 56;
	static final int PADDING = 232;

	final GameTestHelper helper;
	final ServerLevel level;
	final WorldLayout layout;

	TestWorld(GameTestHelper helper) {
		this.helper = helper;
		this.level = helper.getLevel();
		this.layout = WorldLayout.at(helper.absolutePos(BlockPos.ZERO), SPACING);
		// Only the test's own chunks are force-loaded; keep the slots ticking too (see OpenField).
		helper.onEachTick(() -> {
			keepLoaded(layout.hubSlot());
			for (FightLevel fightLevel : FightLevel.DOORS) {
				keepLoaded(layout.arenaSlot(fightLevel));
			}
		});
	}

	private void keepLoaded(ArenaBox slot) {
		BlockPos centre = new BlockPos((int) slot.middleX(), slot.minY(), (int) slot.middleZ());
		level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, ChunkPos.containing(centre), 4);
	}
}
