package nl.jeeninga.tafelboem.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.core.BuildKit;
import nl.jeeninga.tafelboem.core.BuildPhase;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.world.ArenaRun;
import nl.jeeninga.tafelboem.world.BuildCorner;
import nl.jeeninga.tafelboem.world.PlacedArea;

/**
 * R1.2's build phase: the plain arena with a chest and a bell, a countdown in the boss bar, the bell
 * bringing him 10 s later, and leaving before he came. GameTests can't wait minutes: the long times
 * are only checked as numbers, the waiting ones ring the bell or use a short build time.
 */
public class BuildPhaseGameTest {
	@GameTest(maxTicks = 100, padding = TestWorld.PADDING)
	public void entryPlacesThePlainArenaWithTheKitTheBellAndTheCountdown(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).withBuildPhase(BuildPhase.STANDARD).running();
		ServerPlayer kid = test.kidInTheHub();
		ServerPlayer late = test.kidInTheHub();
		test.whenLoaded(() -> {
			test.world.enterDoor(kid, 1);
			ArenaRun run = test.world.run(FightLevel.MAKKELIJK).orElseThrow();
			PlacedArea arena = run.arena();
			helper.assertTrue(arena.name().equals("default"), "the plain arena, not " + arena.name());

			BlockPos chest = BuildCorner.chestAt(arena);
			helper.assertTrue(test.level.getBlockState(chest).is(Blocks.CHEST), "a chest 3 blocks ahead of the arrival spot");
			Container container = ChestBlock.getContainer((ChestBlock) Blocks.CHEST, test.level.getBlockState(chest), test.level, chest, true);
			helper.assertTrue(container != null && container.getContainerSize() == 54, "a double chest");
			for (int slot = 0; slot < BuildKit.CHEST.size(); slot++) {
				BuildKit.Stack expected = BuildKit.CHEST.get(slot);
				ItemStack found = container.getItem(slot);
				helper.assertTrue(BuiltInRegistries.ITEM.getKey(found.getItem()).getPath().equals(expected.item()) && found.getCount() == expected.count(),
						"slot " + slot + " holds " + expected + ", not " + found);
			}
			helper.assertTrue(test.level.getBlockState(BuildCorner.bellAt(arena)).is(Blocks.BELL), "a bell beside the chest");
			helper.assertTrue(run.countdown().secondsLeft() == 300, "5 minutes on Makkelijk");
		});
		helper.runAfterDelay(15, () -> test.world.enterDoor(late, 1));

		helper.succeedWhen(() -> {
			// Only GameTest assertions are retried; orElseThrow before the kid is in would fail the test.
			helper.assertTrue(test.world.run(FightLevel.MAKKELIJK).isPresent(), "wait for the kid to go in");
			ArenaRun run = test.world.run(FightLevel.MAKKELIJK).orElseThrow();
			helper.assertTrue(run.kids().contains(late.getUUID()), "the second kid joins the same run");
			helper.assertTrue(run.building().isShownTo(kid) && run.building().isShownTo(late), "both see the countdown");
			helper.assertTrue(run.building().title().getContents() instanceof TranslatableContents contents
					&& contents.getKey().equals("tafelboem.build.countdown") && contents.getArgs()[0].equals("4:59"),
					"the boss bar counts down: " + run.building().title());
			helper.assertTrue(run.fight() == null, "he doesn't come yet");
			test.close();
		});
	}
}
