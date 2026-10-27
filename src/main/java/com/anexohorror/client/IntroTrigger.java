package com.anexohorror.client;

import com.anexohorror.AnexoHorror;
import com.anexohorror.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Decide cuando mostrar el video de introduccion al entrar a un mundo. */
@Mod.EventBusSubscriber(modid = AnexoHorror.MODID, value = Dist.CLIENT)
public final class IntroTrigger {
    private static boolean pending;
    private static boolean forced;
    private static int wait;

    /** Para el comando de prueba: muestra el video ya, sin mirar la configuracion. */
    public static void request() {
        pending = true;
        forced = true;
        wait = 2;
    }

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        pending = true;
        forced = false;
        wait = 80; // deja que el mundo termine de cargar
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !pending) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (--wait > 0 || mc.screen != null) return;
        pending = false;
        if (forced || shouldShow(mc)) {
            mc.setScreen(new IntroVideoScreen());
        }
    }

    private static String worldKey(Minecraft mc) {
        IntegratedServer s = mc.getSingleplayerServer();
        if (s != null) return "sp:" + s.getWorldData().getLevelName();
        if (mc.getCurrentServer() != null) return "mp:" + mc.getCurrentServer().ip;
        return "unknown";
    }

    private static boolean shouldShow(Minecraft mc) {
        ClientConfig.IntroMode mode = ClientConfig.INTRO_MODE.get();
        if (mode == ClientConfig.IntroMode.OFF) return false;
        if (mode == ClientConfig.IntroMode.ALWAYS) return true;
        String key = mode == ClientConfig.IntroMode.ONCE ? "global" : worldKey(mc);
        List<String> seen = new ArrayList<>(ClientConfig.SEEN_WORLDS.get());
        if (seen.contains(key)) return false;
        seen.add(key);
        while (seen.size() > 300) seen.remove(0);
        ClientConfig.SEEN_WORLDS.set(seen);
        ClientConfig.SEEN_WORLDS.save();
        return true;
    }

    private IntroTrigger() {}
}
