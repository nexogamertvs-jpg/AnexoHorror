package com.anexohorror.registry;

import com.anexohorror.AnexoHorror;
import com.anexohorror.entity.HorrorEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnexoHorror.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEvents {
    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.WATCHER.get(), HorrorEntity.createAttributes().build());
        event.put(ModEntities.OTHER.get(), com.anexohorror.entity.OtherEntity.createAttributes().build());
    }

    private ModEvents() {}
}
