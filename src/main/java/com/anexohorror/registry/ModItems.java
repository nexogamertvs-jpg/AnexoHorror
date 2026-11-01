package com.anexohorror.registry;

import com.anexohorror.AnexoHorror;
import com.anexohorror.entity.HorrorVariant;
import com.anexohorror.item.HorrorEggItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

/** Un huevo generador por cada presencia: egg_peeker, egg_grin, ... */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AnexoHorror.MODID);
    public static final Map<HorrorVariant, RegistryObject<Item>> EGGS = new EnumMap<>(HorrorVariant.class);

    static {
        for (HorrorVariant v : HorrorVariant.values()) {
            EGGS.put(v, ITEMS.register("egg_" + v.id, () -> new HorrorEggItem(v, new Item.Properties())));
        }
    }

    private ModItems() {}
}
