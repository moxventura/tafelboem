package nl.jeeninga.tafelboem.boss;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.core.Ammo;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.Pacing;
import nl.jeeninga.tafelboem.game.BombEffects;
import nl.jeeninga.tafelboem.game.QuizClient;
import nl.jeeninga.tafelboem.item.TerugbomItem;
import nl.jeeninga.tafelboem.registry.ModItems;

/**
 * The Terugbommen of one fight (R1.4): glowing bomb items on the floor that poof after 15 s, and the rules for
 * catching them. The fight hands a bomb over itself instead of leaving it to Minecraft's pick-up, because the cap
 * of 3, the slot and the count in the log are the fight's. Also what a kid's pockets may hold.
 */
final class Terugbommen {
	/** Tags the bomb items this class dropped, so one a kid dropped (Q) is told from one lying here. */
	static final String TAG = "tafelboem_terugbom";

	private static final class Lying {
		final ItemEntity item;
		final BombTier tier;
		int ticksLeft = Pacing.TERUGBOM_LIFETIME_TICKS;

		Lying(ItemEntity item, BombTier tier) {
			this.item = item;
			this.tier = tier;
		}
	}

	private final ServerLevel level;
	private final QuizClient client;
	private final List<Lying> lying = new ArrayList<>();

	Terugbommen(ServerLevel level, QuizClient client) {
		this.level = level;
		this.client = client;
	}

	/** A bomb lies here as an item, with a small poof: glowing, so it is easy to spot. */
	void drop(Vec3 at, BombTier tier) {
		ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(ModItems.terugbom(tier)));
		item.setNeverPickUp();
		item.setUnlimitedLifetime();
		item.setGlowingTag(true);
		item.setDeltaMovement(Vec3.ZERO);
		// Before it enters the world, so the hook for dropped Terugbommen doesn't take it for a kid's.
		item.addTag(TAG);
		level.addFreshEntity(item);
		BombEffects.poof(level, at);
		lying.add(new Lying(item, tier));
	}

	/** Once a tick: the ones that are over poof (not while a juggle holds the fight), and a kid who walks onto one they have room for catches it. */
	void tick(List<ServerPlayer> kids, BiConsumer<ServerPlayer, BombTier> caught, boolean paused) {
		for (Iterator<Lying> it = lying.iterator(); it.hasNext(); ) {
			Lying bomb = it.next();
			if (!bomb.item.isAlive()) {
				it.remove();
				continue;
			}
			if (!paused && --bomb.ticksLeft <= 0) {
				poof(bomb);
				it.remove();
				continue;
			}
			for (ServerPlayer kid : kids) {
				if (reaches(kid, bomb.item) && Ammo.canCatch(carried(kid)) && give(client, kid, bomb.tier)) {
					bomb.item.discard();
					it.remove();
					caught.accept(kid, bomb.tier);
					break;
				}
			}
		}
	}

	/** The fight is over: everything on the floor poofs. */
	void poofAll() {
		lying.forEach(Terugbommen::poof);
		lying.clear();
	}

	private static void poof(Lying bomb) {
		if (bomb.item.isAlive() && bomb.item.level() instanceof ServerLevel level) {
			BombEffects.poof(level, bomb.item.position());
			bomb.item.discard();
		}
	}

	/** Within Minecraft's own pick-up reach: the kid's box, a block wider and half a block higher. */
	private static boolean reaches(ServerPlayer kid, ItemEntity item) {
		return kid.isAlive() && !kid.isSpectator() && kid.getBoundingBox().inflate(1.0, 0.5, 1.0).intersects(item.getBoundingBox());
	}

	/**
	 * Puts a Terugbom in this kid's pocket where {@link Ammo#slotFor} says, and selects that slot when it is on the
	 * hotbar. The cap is the caller's to check.
	 *
	 * @return false when every slot is taken
	 */
	static boolean give(QuizClient client, ServerPlayer kid, BombTier tier) {
		Inventory inventory = kid.getInventory();
		boolean[] empty = new boolean[Inventory.INVENTORY_SIZE];
		for (int slot = 0; slot < empty.length; slot++) {
			empty[slot] = inventory.getItem(slot).isEmpty();
		}
		int slot = Ammo.slotFor(inventory.getSelectedSlot(), empty);
		if (slot < 0) {
			return false;
		}
		inventory.setItem(slot, new ItemStack(ModItems.terugbom(tier)));
		if (Ammo.isHotbar(slot)) {
			inventory.setSelectedSlot(slot);
			client.selectSlot(kid, slot);
		}
		kid.level().playSound(null, kid.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 1.0F);
		return true;
	}

	/** How many Terugbommen this kid carries, over all slots and the offhand. */
	static int carried(ServerPlayer kid) {
		Inventory inventory = kid.getInventory();
		int count = 0;
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			count += terugbommenIn(inventory.getItem(slot));
		}
		return count + terugbommenIn(inventory.getItem(Inventory.SLOT_OFFHAND));
	}

	private static int terugbommenIn(ItemStack stack) {
		return stack.getItem() instanceof TerugbomItem ? stack.getCount() : 0;
	}

	/**
	 * Takes every Terugbom out of this kid's pockets, the offhand and the cursor: not dropped, just gone (the kids keep
	 * their inventory on death, so a dying kid's would otherwise stay with them).
	 */
	static void strip(ServerPlayer kid) {
		Inventory inventory = kid.getInventory();
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			if (inventory.getItem(slot).getItem() instanceof TerugbomItem) {
				inventory.setItem(slot, ItemStack.EMPTY);
			}
		}
		if (inventory.getItem(Inventory.SLOT_OFFHAND).getItem() instanceof TerugbomItem) {
			inventory.setItem(Inventory.SLOT_OFFHAND, ItemStack.EMPTY);
		}
		if (kid.containerMenu.getCarried().getItem() instanceof TerugbomItem) {
			kid.containerMenu.setCarried(ItemStack.EMPTY);
		}
	}

	/** A Terugbom that ended up in a chest or the crafting grid goes back to the kid's pocket. */
	static void returnFromMenu(QuizClient client, ServerPlayer kid) {
		for (Slot slot : kid.containerMenu.slots) {
			if (slot.container != kid.getInventory() && slot.getItem().getItem() instanceof TerugbomItem terugbom) {
				slot.set(ItemStack.EMPTY);
				give(client, kid, terugbom.tier());
			}
		}
	}
}
