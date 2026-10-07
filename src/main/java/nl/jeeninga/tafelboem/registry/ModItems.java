package nl.jeeninga.tafelboem.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.item.SummonGraafFoutItem;

public final class ModItems {
	public static Item SUMMON_GRAAF_FOUT;

	private ModItems() {
	}

	public static void register() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, TafelBoem.id("summon_graaf_fout"));
		SUMMON_GRAAF_FOUT = Registry.register(BuiltInRegistries.ITEM, key,
				new SummonGraafFoutItem(new Item.Properties().stacksTo(1).setId(key)));
	}
}
