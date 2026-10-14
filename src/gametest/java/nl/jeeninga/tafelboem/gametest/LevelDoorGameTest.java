package nl.jeeninga.tafelboem.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/** The hub's doors: you walk through them, they remember their level, and nobody can break them. */
public class LevelDoorGameTest {
	@GameTest
	public void aLevelDoorIsWalkThroughUnbreakableAndRemembersItsLevel(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, ModBlocks.LEVEL_DOOR.defaultBlockState().setValue(LevelDoorBlock.LEVEL, 2));
		BlockState door = helper.getBlockState(pos);
		helper.assertTrue(door.getValue(LevelDoorBlock.LEVEL) == 2, "the door leads to level 2");
		helper.assertTrue(door.getCollisionShape(helper.getLevel(), helper.absolutePos(pos), CollisionContext.empty()).isEmpty(),
				"kids walk through it");
		helper.assertTrue(door.getDestroySpeed(helper.getLevel(), helper.absolutePos(pos)) < 0, "nobody can break it");
		helper.succeed();
	}
}
