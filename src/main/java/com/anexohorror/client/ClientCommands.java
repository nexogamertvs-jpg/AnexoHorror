package com.anexohorror.client;

import com.anexohorror.AnexoHorror;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Comandos de prueba del lado cliente: /ahc intro | flash | text */
@Mod.EventBusSubscriber(modid = AnexoHorror.MODID, value = Dist.CLIENT)
public final class ClientCommands {
    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ahc")
                .then(Commands.literal("intro").executes(c -> { IntroTrigger.request(); return 1; }))
                .then(Commands.literal("flash").executes(c -> { PsychoClientTick.triggerFlash(); return 1; }))
                .then(Commands.literal("text").executes(c -> { PsychoClientTick.triggerText(); return 1; })));
    }

    private ClientCommands() {}
}
