package nl.jeeninga.tafelboem.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.boss.ThrownBomb;

public final class ModEntities {
	public static final EntityType<GraafFout> GRAAF_FOUT = register("graaf_fout",
			EntityType.Builder.of(GraafFout::new, MobCategory.MONSTER)
					.sized(0.6F, 1.95F)
					.clientTrackingRange(10));

	public static final EntityType<ThrownBomb> THROWN_BOMB = register("thrown_bomb",
			EntityType.Builder.<ThrownBomb>of(ThrownBomb::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.5F, 0.5F)
					.clientTrackingRange(8)
					.updateInterval(2));

	private ModEntities() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(GRAAF_FOUT, GraafFout.createAttributes());
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, TafelBoem.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}
}
