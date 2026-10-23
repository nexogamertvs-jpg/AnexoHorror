package com.anexohorror.registry;

import com.anexohorror.AnexoHorror;
import com.anexohorror.entity.HorrorEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AnexoHorror.MODID);

    public static final RegistryObject<EntityType<HorrorEntity>> WATCHER = ENTITIES.register("watcher",
            () -> EntityType.Builder.<HorrorEntity>of(HorrorEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .noSave()
                    .fireImmune()
                    .build(AnexoHorror.MODID + ":watcher"));

    private ModEntities() {}
}
