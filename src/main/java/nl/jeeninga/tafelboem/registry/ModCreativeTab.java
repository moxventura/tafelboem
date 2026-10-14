package nl.jeeninga.tafelboem.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.core.BombTier;

public final class ModCreativeTab {
	private static final ResourceKey<CreativeModeTab> KEY = ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), TafelBoem.id("tafelboem"));

	private ModCreativeTab() {
	}

	public static void register() {
		CreativeModeTab tab = FabricCreativeModeTab.builder()
				.icon(() -> new ItemStack(ModBlocks.bombItem(BombTier.KNALLETJE)))
				.title(Component.translatable("itemGroup.tafelboem"))
				.displayItems((parameters, output) -> {
					// R0 play-test: all bombs are available. From R1 on, only the Knalletje is free and
					// the rest is earned as loot.
					output.accept(ModItems.SUMMON_GRAAF_FOUT);
					for (BombTier tier : BombTier.values()) {
						output.accept(ModBlocks.bombItem(tier));
					}
					for (int level = 1; level <= 3; level++) {
						output.accept(door(level, Component.translatable("tafelboem.door.item.level", Component.translatable("tafelboem.level." + level))));
					}
					output.accept(door(0, Component.translatable("tafelboem.door.item.exit")));
				})
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, KEY, tab);
	}

	private static ItemStack door(int level, Component name) {
		ItemStack door = new ItemStack(ModBlocks.LEVEL_DOOR_ITEM);
		door.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(LevelDoorBlock.LEVEL, level));
		door.set(DataComponents.ITEM_NAME, name);
		return door;
	}
}
