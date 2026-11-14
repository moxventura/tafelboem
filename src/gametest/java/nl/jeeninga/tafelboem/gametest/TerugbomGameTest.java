package nl.jeeninga.tafelboem.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.Pacing;
import nl.jeeninga.tafelboem.item.TerugbomItem;
import nl.jeeninga.tafelboem.registry.ModItems;

/**
 * R1.4's Terugbom: a dodged bomb drops as a glowing item, any kid catches it by walking onto it (at most 3, the
 * slot selected), it poofs after 15 s, and it can't be dropped, placed or put in a chest.
 */
public class TerugbomGameTest {
	@GameTest(maxTicks = 300, padding = 48)
	@Fight
	public void aDodgedBombDropsAsAGlowingTerugbomAndHeTauntsTheKid(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		DodgeGameTest.wallInFrontOf(arena);

		helper.succeedWhen(() -> {
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).dodged() >= 1, "wait for his first throw to hit the wall");
			List<ItemEntity> lying = arena.terugbommen();
			helper.assertTrue(lying.size() == 1, "one Terugbom lies there: " + lying.size());
			ItemEntity bomb = lying.getFirst();
			helper.assertTrue(bomb.hasGlowingTag(), "it glows, so it is easy to spot");
			helper.assertTrue(bomb.getItem().getItem() == ModItems.terugbom(FightLevel.MAKKELIJK.bombForThrow(1)), "it is the bomb he threw");
			helper.assertTrue(arena.fight.lyingBombs().isEmpty(), "no lying bomb, no block");
			helper.assertFalse(arena.askedAnything(), "nobody gets a question");
			helper.assertFalse(arena.sessions.isBusy(arena.kid), "nobody is rooted");
			helper.assertTrue(arena.heard(arena.kid, "tafelboem.boss.graaf_fout.dodged"), "he taunts the kid it missed");
			arena.end();
		});
	}

	@GameTest(maxTicks = 40, padding = 48)
	public void anyKidCatchesItAndItsSlotIsSelected(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		ServerPlayer sibling = arena.addKid(arena.box.middleX() + 8, arena.box.middleZ() + 8);
		// The sibling's selected slot (0) is empty: the bomb goes there. The kid's selected slot (3) is taken, slot 0 is the first empty one.
		arena.kid.getInventory().setItem(3, new ItemStack(Items.DIRT));
		arena.kid.getInventory().setSelectedSlot(3);
		arena.fight.dropTerugbom(sibling.position().add(1, 0, 0), BombTier.GEWONE_TNT);
		arena.fight.dropTerugbom(arena.kid.position().add(-1, 0, 0), BombTier.KNALLETJE);

		helper.runAfterDelay(6, () -> {
			helper.assertTrue(arena.fight.tally(sibling.getUUID()).caught() == 1, "the sibling caught theirs: any kid can");
			helper.assertTrue(sibling.getInventory().getItem(0).getItem() == ModItems.terugbom(BombTier.GEWONE_TNT), "in their selected slot");
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).caught() == 1, "and the kid theirs");
			helper.assertTrue(arena.kid.getInventory().getItem(0).getItem() == ModItems.terugbom(BombTier.KNALLETJE), "in the first empty slot of the hotbar");
			helper.assertTrue(arena.kid.getInventory().getSelectedSlot() == 0, "which is selected now, so one right-click throws it");
			helper.assertTrue(arena.client.heldSlots.stream().anyMatch(held -> held.player() == arena.kid && held.slot() == 0), "and the client is told");
			helper.assertTrue(arena.terugbommen().isEmpty(), "nothing lies there any more");
			helper.assertTrue(hints(arena, arena.kid) == 1 && hints(arena, sibling) == 1, "each kid is told how to throw, once");
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40, padding = 48)
	public void aFourthIsRefusedAlsoOverSeveralSlotsAndTheOffhand(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		Inventory pocket = arena.kid.getInventory();
		pocket.setItem(5, new ItemStack(ModItems.terugbom(BombTier.KNALLETJE)));
		pocket.setItem(20, new ItemStack(ModItems.terugbom(BombTier.KNALLETJE)));
		pocket.setItem(Inventory.SLOT_OFFHAND, new ItemStack(ModItems.terugbom(BombTier.GEWONE_TNT)));
		arena.fight.dropTerugbom(arena.kid.position().add(1, 0, 0), BombTier.KIPPENBOM);

		helper.runAfterDelay(6, () -> {
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).caught() == 0, "with 3 in the slots and the offhand a fourth is refused");
			helper.assertTrue(arena.terugbommen().size() == 1, "it stays on the ground");
			pocket.setItem(20, ItemStack.EMPTY);
		});
		helper.runAfterDelay(12, () -> {
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).caught() == 1, "with room again, it is caught");
			helper.assertTrue(arena.terugbommen().isEmpty(), "and gone from the floor");
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 400, padding = 48)
	@Fight
	public void aTerugbomNobodyPicksUpPoofsAfterFifteenSeconds(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		// Far from the kid, who stands 5 blocks south of the middle.
		arena.fight.dropTerugbom(new Vec3(arena.box.middleX() - 8, arena.floorY, arena.box.middleZ() - 8), BombTier.KNALLETJE);

		helper.runAfterDelay(Pacing.TERUGBOM_LIFETIME_TICKS - 10, () ->
				helper.assertTrue(arena.terugbommen().size() == 1, "still lying there just before the 15 seconds are up"));
		helper.runAfterDelay(Pacing.TERUGBOM_LIFETIME_TICKS + 10, () -> {
			helper.assertTrue(arena.terugbommen().isEmpty(), "poofed after 15 seconds");
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).caught() == 0, "nobody caught it");
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 60, padding = 48)
	public void itCantBePlacedDroppedOrPutInAChest(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		ItemStack bomb = new ItemStack(ModItems.terugbom(BombTier.KNALLETJE));
		helper.assertFalse(bomb.getItem() instanceof BlockItem, "not a block item: it can't be placed, and never starts a free-play question");
		helper.assertTrue(bomb.getItem() instanceof TerugbomItem, "its own item");

		// Q: the stack leaves the slot and drops; it must be back in the kid's pocket a moment later, and nothing lies there.
		arena.kid.getInventory().setItem(4, ItemStack.EMPTY);
		arena.kid.drop(bomb.copy(), false);
		// A chest: a Terugbom put in a slot of the open menu goes back to the kid's pocket.
		Container chest = new SimpleContainer(27);
		arena.kid.containerMenu = ChestMenu.threeRows(7, arena.kid.getInventory(), chest);
		arena.kid.containerMenu.getSlot(0).set(new ItemStack(ModItems.terugbom(BombTier.GEWONE_TNT)));

		helper.runAfterDelay(4, () -> {
			helper.assertTrue(arena.terugbommen().isEmpty(), "the dropped bomb is gone from the floor");
			helper.assertTrue(chest.isEmpty(), "and the chest is empty");
			int carried = 0;
			for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
				carried += arena.kid.getInventory().getItem(slot).getItem() instanceof TerugbomItem ? 1 : 0;
			}
			helper.assertTrue(carried == 2, "both are in the kid's pocket: " + carried);
			arena.end();
			helper.succeed();
		});
	}

	/** How many times this kid was told how to throw. */
	private static long hints(ArenaFight arena, ServerPlayer kid) {
		return arena.client.actionBarsTo.stream().filter(bar -> bar.player() == kid && bar.message().getContents() instanceof TranslatableContents says
				&& says.getKey().equals("tafelboem.terugbom.hint")).count();
	}
}
