package nl.jeeninga.tafelboem.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

public class SmokeGameTest {
	@GameTest
	public void modLoadsAndServerTicks(GameTestHelper context) {
		context.setBlock(0, 1, 0, Blocks.TNT);
		context.assertBlockPresent(Blocks.TNT, 0, 1, 0);
		context.succeed();
	}
}
