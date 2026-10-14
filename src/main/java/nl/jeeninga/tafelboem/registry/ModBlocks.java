package nl.jeeninga.tafelboem.registry;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.block.BombBlock;
import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.core.BombTier;

public final class ModBlocks {
	private static final Map<BombTier, BombBlock> BOMBS = new EnumMap<>(BombTier.class);
	private static final Map<BombTier, Item> BOMB_ITEMS = new EnumMap<>(BombTier.class);

	public static LevelDoorBlock LEVEL_DOOR;
	public static Item LEVEL_DOOR_ITEM;

	private ModBlocks() {
	}

	public static void register() {
		for (BombTier tier : BombTier.values()) {
			ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, TafelBoem.id(tier.id()));
			BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
					.mapColor(MapColor.FIRE)
					.instabreak()
					.sound(SoundType.GRASS)
					.setId(blockKey);
			BombBlock block = Registry.register(BuiltInRegistries.BLOCK, blockKey, new BombBlock(tier, properties));

			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, TafelBoem.id(tier.id()));
			Item item = Registry.register(BuiltInRegistries.ITEM, itemKey,
					new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(itemKey)));

			BOMBS.put(tier, block);
			BOMB_ITEMS.put(tier, item);
		}

		ResourceKey<Block> doorKey = ResourceKey.create(Registries.BLOCK, TafelBoem.id("level_door"));
		BlockBehaviour.Properties doorProperties = BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_PURPLE)
				.noCollision()
				.strength(-1.0F, 3600000.0F)
				.noLootTable()
				.lightLevel(state -> 11)
				.sound(SoundType.GLASS)
				.pushReaction(PushReaction.BLOCK)
				.setId(doorKey);
		LEVEL_DOOR = Registry.register(BuiltInRegistries.BLOCK, doorKey, new LevelDoorBlock(doorProperties));

		ResourceKey<Item> doorItemKey = ResourceKey.create(Registries.ITEM, TafelBoem.id("level_door"));
		LEVEL_DOOR_ITEM = Registry.register(BuiltInRegistries.ITEM, doorItemKey,
				new BlockItem(LEVEL_DOOR, new Item.Properties().useBlockDescriptionPrefix().setId(doorItemKey)));
	}

	public static BombBlock bomb(BombTier tier) {
		return BOMBS.get(tier);
	}

	public static Item bombItem(BombTier tier) {
		return BOMB_ITEMS.get(tier);
	}
}
