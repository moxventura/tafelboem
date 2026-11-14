package nl.jeeninga.tafelboem.registry;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.item.SummonGraafFoutItem;
import nl.jeeninga.tafelboem.item.TerugbomItem;

public final class ModItems {
	public static Item SUMMON_GRAAF_FOUT;
	private static final Map<BombTier, TerugbomItem> TERUGBOMMEN = new EnumMap<>(BombTier.class);

	private ModItems() {
	}

	public static void register() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, TafelBoem.id("summon_graaf_fout"));
		SUMMON_GRAAF_FOUT = Registry.register(BuiltInRegistries.ITEM, key,
				new SummonGraafFoutItem(new Item.Properties().stacksTo(1).setId(key)));
		for (BombTier tier : BombTier.values()) {
			ResourceKey<Item> terugKey = ResourceKey.create(Registries.ITEM, TafelBoem.id("terugbom_" + tier.id()));
			TERUGBOMMEN.put(tier, Registry.register(BuiltInRegistries.ITEM, terugKey,
					new TerugbomItem(tier, new Item.Properties().stacksTo(1).setId(terugKey))));
		}
	}

	/** The item a caught bomb of this tier is: one per stack, so three of them take three slots. */
	public static TerugbomItem terugbom(BombTier tier) {
		return TERUGBOMMEN.get(tier);
	}
}
