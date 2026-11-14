package nl.jeeninga.tafelboem.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.item.TerugbomItem;
import nl.jeeninga.tafelboem.registry.ModItems;

/**
 * R1.4's load contract for Terugbommen: they exist only in a running arena fight. When it ends they poof on the
 * ground and in the pockets; a kid who leaves or dies loses theirs (the kids keep their inventory on death, so
 * they are taken out, not dropped); and one a kid has on login is removed.
 */
public class TerugbomLeakGameTest {
	@GameTest(maxTicks = 40, padding = 48)
	public void whenTheFightEndsTheGroundAndThePocketsPoof(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		ServerPlayer sibling = arena.addKid(arena.box.middleX() + 8, arena.box.middleZ() + 8);
		pocket(arena.kid, 4);
		pocket(sibling, Inventory.SLOT_OFFHAND);
		arena.fight.dropTerugbom(arena.kid.position().add(-8, 0, -8), BombTier.GEWONE_TNT);
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(arena.terugbommen().size() == 1 && carried(arena.kid) == 1 && carried(sibling) == 1, "before: one on the ground, one in each pocket");
			arena.end();
			helper.assertTrue(arena.terugbommen().isEmpty(), "after: none on the ground");
			helper.assertTrue(carried(arena.kid) == 0 && carried(sibling) == 0, "and none in a pocket, the offhand included");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40, padding = 48)
	public void aKidWhoLeavesTheArenaLosesTheirs(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		pocket(arena.kid, 2);
		arena.fight.dropTerugbom(arena.kid.position().add(-8, 0, -8), BombTier.KNALLETJE);
		helper.runAfterDelay(3, () -> arena.kid.teleportTo(arena.box.minX() - 15, arena.floorY, arena.box.middleZ()));
		helper.runAfterDelay(8, () -> {
			helper.assertTrue(carried(arena.kid) == 0, "their Terugbom is gone from their pocket");
			helper.assertTrue(arena.terugbommen().size() == 1, "the one on the floor stays: the fight goes on");
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 60, padding = 48)
	public void aKidWhoDiesLosesTheirsAndNothingDrops(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		ArenaFight.survival(arena.kid);
		pocket(arena.kid, 2);
		helper.runAfterDelay(3, () -> arena.kid.kill(arena.level));
		helper.runAfterDelay(12, () -> {
			helper.assertTrue(arena.kid.isDeadOrDying(), "the kid died");
			helper.assertTrue(carried(arena.kid) == 0, "their Terugbom is taken out of their pocket");
			helper.assertTrue(arena.terugbommen().isEmpty(), "and not dropped on the floor");
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40, padding = 48)
	public void aTerugbomInAPocketOnLoginIsRemoved(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		// Not in the fight: a kid who logs in with one, saved there when the game stopped.
		ServerPlayer saved = arena.addKid(arena.box.middleX() + 15, arena.box.middleZ());
		pocket(saved, 3);
		pocket(arena.kid, 3);
		helper.runAfterDelay(3, () -> {
			arena.fights.onJoin(saved);
			helper.assertTrue(carried(saved) == 0, "removed on login");
			arena.fights.onJoin(arena.kid);
			helper.assertTrue(carried(arena.kid) == 1, "a kid who is in the running fight keeps theirs");
			arena.end();
			helper.succeed();
		});
	}

	private static void pocket(ServerPlayer kid, int slot) {
		kid.getInventory().setItem(slot, new ItemStack(ModItems.terugbom(BombTier.KNALLETJE)));
	}

	private static int carried(ServerPlayer kid) {
		int count = 0;
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			count += kid.getInventory().getItem(slot).getItem() instanceof TerugbomItem ? 1 : 0;
		}
		return count + (kid.getInventory().getItem(Inventory.SLOT_OFFHAND).getItem() instanceof TerugbomItem ? 1 : 0);
	}
}
