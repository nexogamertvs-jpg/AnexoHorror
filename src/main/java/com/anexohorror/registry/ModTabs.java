package com.anexohorror.registry;

import com.anexohorror.AnexoHorror;
import com.anexohorror.entity.HorrorVariant;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Pestana de creativo "AnexoHorror" con todos los huevos. */
public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AnexoHorror.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.anexohorror"))
            .icon(() -> new ItemStack(ModItems.EGGS.get(HorrorVariant.PEEKER).get()))
            .displayItems((params, output) -> ModItems.EGGS.values().forEach(r -> output.accept(r.get())))
            .build());

    private ModTabs() {}
}
