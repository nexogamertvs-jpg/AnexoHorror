package com.anexohorror.registry;

import com.anexohorror.AnexoHorror;
import com.anexohorror.entity.HorrorEntity;
import com.anexohorror.entity.OtherEntity;
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
                    .sized(1.0F, 2.8F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .noSave()
                    .fireImmune()
                    .build(AnexoHorror.MODID + ":watcher"));

    public static final RegistryObject<EntityType<OtherEntity>> OTHER = ENTITIES.register("other",
            () -> EntityType.Builder.<OtherEntity>of(OtherEntity::new, MobCategory.MONSTER)
                    .sized(1.8F, 3.0F)
                    .clientTrackingRange(10)
                    .noSave()
                    .fireImmune()
                    .build(AnexoHorror.MODID + ":other"));

    private ModEntities() {}
}
