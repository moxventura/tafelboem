package nl.jeeninga.tafelboem.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
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

	@GameTest
	public void pickingAnExitDoorGivesAnExitDoor(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, ModBlocks.LEVEL_DOOR.defaultBlockState().setValue(LevelDoorBlock.LEVEL, 0));

		ItemStack picked = helper.getBlockState(pos).getCloneItemStack(helper.getLevel(), helper.absolutePos(pos), false);

		BlockItemStateProperties state = picked.get(DataComponents.BLOCK_STATE);
		helper.assertTrue(picked.is(ModBlocks.LEVEL_DOOR_ITEM), "a door");
		helper.assertTrue(state != null && Integer.valueOf(0).equals(state.get(LevelDoorBlock.LEVEL)), "to level 0, the exit: " + state);
		helper.assertTrue(isNamed(picked.get(DataComponents.ITEM_NAME), "tafelboem.door.item.exit"), "named as in the creative tab");
		helper.succeed();
	}

	private static boolean isNamed(Component name, String key) {
		return name != null && name.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(key);
	}
}
