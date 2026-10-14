package nl.jeeninga.tafelboem.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.game.BombEffects;
import nl.jeeninga.tafelboem.game.ProtectedAreas;
import nl.jeeninga.tafelboem.registry.ModBlocks;
import nl.jeeninga.tafelboem.world.ArenaCatalog;
import nl.jeeninga.tafelboem.world.Hub;
import nl.jeeninga.tafelboem.world.PlacedArea;

/**
 * The hub and the arenas of a TafelBoem world, placed from code or from structure templates.
 */
public class HubAndArenasGameTest {
	@GameTest(padding = TestWorld.PADDING)
	public void theHubHasADoorPerLevelWithItsSignAndBombs(GameTestHelper helper) {
		TestWorld world = new TestWorld(helper);
		try {
			PlacedArea hub = Hub.place(world.level, world.layout, TestWorld.HUB);
			for (int level = 1; level <= 3; level++) {
				helper.assertTrue(hub.doors().getOrDefault(level, List.of()).size() == 12, "a 3 × 4 door to level " + level);
			}
			List<BombTier> bombs = new ArrayList<>();
			for (ItemFrame frame : world.level.getEntitiesOfClass(ItemFrame.class, aabb(world.layout.hubSlot()))) {
				for (BombTier tier : BombTier.values()) {
					if (frame.getItem().is(ModBlocks.bombItem(tier))) {
						bombs.add(tier);
					}
				}
			}
			helper.assertTrue(bombs.size() == 5, "five bombs in frames beside the doors, found " + bombs);
			int signs = 0;
			for (BlockPos pos : BlockPos.betweenClosed(corner(world.layout.hubSlot()), far(world.layout.hubSlot()))) {
				if (world.level.getBlockEntity(pos) instanceof SignBlockEntity sign
						&& sign.getFrontText().getMessage(1, false).getContents() instanceof TranslatableContents text
						&& text.getKey().equals("tafelboem.door.sign")) {
					signs++;
				}
			}
			helper.assertTrue(signs == 3, "a sign above each door, found " + signs);
		} finally {
			ProtectedAreas.unprotect(world.level, world.layout.hubSlot());
		}
		helper.succeed();
	}

	@GameTest(padding = TestWorld.PADDING)
	public void aBoomInTheHubBreaksNothingAndTheHubRepairsItself(GameTestHelper helper) {
		TestWorld world = new TestWorld(helper);
		try {
			PlacedArea hub = Hub.place(world.level, world.layout, TestWorld.HUB);
			BlockPos bomb = hub.standAt();
			BlockPos floor = bomb.below();
			BlockPos broken = floor.east(3);
			world.level.setBlockAndUpdate(broken, Blocks.AIR.defaultBlockState());
			world.level.setBlockAndUpdate(bomb, ModBlocks.bomb(BombTier.GEWONE_TNT).defaultBlockState());
			BombEffects.boom(world.level, bomb, BombTier.GEWONE_TNT);
			helper.assertTrue(world.level.getBlockState(floor).is(Blocks.STONE), "the hub floor under the boom stays");

			Hub.place(world.level, world.layout, TestWorld.HUB);
			helper.assertTrue(world.level.getBlockState(broken).is(Blocks.STONE), "placing the hub again repairs it");
		} finally {
			ProtectedAreas.unprotect(world.level, world.layout.hubSlot());
		}
		helper.succeed();
	}

	@GameTest(padding = TestWorld.PADDING)
	public void withTwoTemplatesInALevelOneOfThemIsUsedAndNeverTwiceInARow(GameTestHelper helper) {
		TestWorld world = new TestWorld(helper);
		String folder = "test_choice_level_";
		saveTemplate(world, folder + "2/gold", Blocks.GOLD_BLOCK, true);
		saveTemplate(world, folder + "2/diamond", Blocks.DIAMOND_BLOCK, true);
		ArenaCatalog catalog = new ArenaCatalog(world.level.getServer().getStructureManager(), new Random(7), folder);

		String previous = null;
		for (int round = 0; round < 6; round++) {
			PlacedArea arena = catalog.place(world.level, world.layout, FightLevel.GEMIDDELD, name -> helper.fail("skipped " + name));
			boolean gold = contains(world, world.layout.arenaSlot(FightLevel.GEMIDDELD), Blocks.GOLD_BLOCK);
			boolean diamond = contains(world, world.layout.arenaSlot(FightLevel.GEMIDDELD), Blocks.DIAMOND_BLOCK);
			helper.assertTrue(gold != diamond, "exactly one of the two templates is placed");
			helper.assertTrue(!arena.name().equals(previous), "never the same arena twice in a row");
			previous = arena.name();
		}
		helper.succeed();
	}

	@GameTest(padding = TestWorld.PADDING)
	public void aTemplateWithoutAnExitDoorIsSkipped(GameTestHelper helper) {
		TestWorld world = new TestWorld(helper);
		String folder = "test_skip_level_";
		saveTemplate(world, folder + "3/no_exit", Blocks.GOLD_BLOCK, false);
		ArenaCatalog catalog = new ArenaCatalog(world.level.getServer().getStructureManager(), new Random(8), folder);
		List<String> skipped = new ArrayList<>();

		PlacedArea arena = catalog.place(world.level, world.layout, FightLevel.MOEILIJK, skipped::add);

		helper.assertTrue(skipped.equals(List.of("tafelboem:" + folder + "3/no_exit")), "the template is skipped: " + skipped);
		helper.assertTrue(arena.name().equals("default"), "the plain arena is used instead");
		helper.assertTrue(world.level.getBlockState(arena.standAt().below()).is(Blocks.COARSE_DIRT), "level 3's darker floor");
		helper.succeed();
	}

	/** Builds a 9 × 3 × 9 arena at the test's corner and saves it the way a structure block does. */
	private static void saveTemplate(TestWorld world, String path, Block marker, boolean exitDoor) {
		ServerLevel level = world.level;
		// 60 blocks above the test's corner: clear of the hub slot, which is 48 high.
		BlockPos build = world.helper.absolutePos(new BlockPos(0, 60, 0));
		for (BlockPos pos : BlockPos.betweenClosed(build, build.offset(8, 2, 8))) {
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
		for (BlockPos pos : BlockPos.betweenClosed(build, build.offset(8, 0, 8))) {
			level.setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
		}
		level.setBlockAndUpdate(build.offset(4, 1, 4), marker.defaultBlockState());
		if (exitDoor) {
			level.setBlockAndUpdate(build.offset(4, 1, 8), ModBlocks.LEVEL_DOOR.defaultBlockState().setValue(LevelDoorBlock.LEVEL, 0));
		}
		StructureTemplateManager manager = level.getServer().getStructureManager();
		Identifier id = TafelBoem.id(path);
		StructureTemplate template = manager.getOrCreate(id);
		template.fillFromWorld(level, build, new Vec3i(9, 3, 9), false, List.of());
		manager.save(id);
		for (BlockPos pos : BlockPos.betweenClosed(build, build.offset(8, 2, 8))) {
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
	}

	private static boolean contains(TestWorld world, ArenaBox slot, Block block) {
		for (BlockPos pos : BlockPos.betweenClosed(corner(slot), far(slot))) {
			if (world.level.getBlockState(pos).is(block)) {
				return true;
			}
		}
		return false;
	}

	private static BlockPos corner(ArenaBox box) {
		return new BlockPos(box.minX(), box.minY(), box.minZ());
	}

	private static BlockPos far(ArenaBox box) {
		return new BlockPos(box.maxX(), box.maxY(), box.maxZ());
	}

	private static AABB aabb(ArenaBox box) {
		return new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
	}
}
