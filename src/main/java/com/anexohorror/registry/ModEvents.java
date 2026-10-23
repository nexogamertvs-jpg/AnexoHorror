package com.anexohorror.registry;

import com.anexohorror.AnexoHorror;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnexoHorror.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEvents {
    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.WATCHER.get(), Mob.createMobAttributes().build());
    }

    private ModEvents() {}
}
