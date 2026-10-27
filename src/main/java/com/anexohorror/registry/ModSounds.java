package com.anexohorror.registry;

import com.anexohorror.AnexoHorror;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AnexoHorror.MODID);

    public static final RegistryObject<SoundEvent> CRACK = reg("crack");
    public static final RegistryObject<SoundEvent> WHISPER = reg("whisper");
    public static final RegistryObject<SoundEvent> BREATH = reg("breath");
    public static final RegistryObject<SoundEvent> KNOCK = reg("knock");
    public static final RegistryObject<SoundEvent> DRONE = reg("drone");
    public static final RegistryObject<SoundEvent> STATIC = reg("static");
    public static final RegistryObject<SoundEvent> HEARTBEAT = reg("heartbeat");
    public static final RegistryObject<SoundEvent> CREAK = reg("creak");
    public static final RegistryObject<SoundEvent> SCRAPE = reg("scrape");
    public static final RegistryObject<SoundEvent> INTRO = reg("intro");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AnexoHorror.MODID, name)));
    }

    private ModSounds() {}
}
