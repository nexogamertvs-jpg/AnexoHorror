package com.anexohorror.client;

import com.anexohorror.AnexoHorror;
import com.anexohorror.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Decide cuando mostrar el video: cada vez que anochece (por defecto) o al entrar a un mundo nuevo. */
@Mod.EventBusSubscriber(modid = AnexoHorror.MODID, value = Dist.CLIENT)
public final class IntroTrigger {
    // entrada a un mundo (modo NEW_WORLD) y comando de prueba
    private static boolean pending;
    private static boolean forced;
    private static int wait;

    // anochecer (modo NIGHTFALL)
    private static long prevTime = -1L;
    private static long lastNightDay = -1L;
    private static boolean nightPending;

    /** Para el comando de prueba: muestra el video ya, sin mirar la configuracion. */
    public static void request() {
        pending = true;
        forced = true;
        wait = 2;
    }

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        pending = ClientConfig.INTRO_MODE.get() == ClientConfig.IntroMode.NEW_WORLD;
        forced = false;
        wait = 80; // deja que el mundo termine de cargar
        prevTime = -1L;
        nightPending = false;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            prevTime = -1L;
            nightPending = false;
            return;
        }

        // ---- anochecer
        if (ClientConfig.INTRO_MODE.get() == ClientConfig.IntroMode.NIGHTFALL
                && mc.level.dimension() == Level.OVERWORLD) {
            long dt = mc.level.getDayTime();
            long t = dt % 24000L;
            long day = dt / 24000L;
            if (prevTime >= 0L && prevTime < 13000L && t >= 13000L && t < 14500L && day != lastNightDay) {
                nightPending = true;
                lastNightDay = day;
            }
            prevTime = t;
            if (nightPending) {
                boolean stillNight = t >= 13000L && t < 23000L;
                if (!stillNight) {
                    nightPending = false;            // se salto la noche durmiendo, etc.
                } else if (mc.screen == null && mc.player.isAlive() && !mc.isPaused()) {
                    nightPending = false;
                    mc.setScreen(new IntroVideoScreen());
                    return;
                }
            }
        }

        // ---- primera entrada a un mundo / comando
        if (!pending) return;
        if (--wait > 0 || mc.screen != null) return;
        pending = false;
        if (forced || shouldShowOnJoin(mc)) {
            mc.setScreen(new IntroVideoScreen());
        }
    }

    private static String worldKey(Minecraft mc) {
        IntegratedServer s = mc.getSingleplayerServer();
        if (s != null) return "sp:" + s.getWorldData().getLevelName();
        if (mc.getCurrentServer() != null) return "mp:" + mc.getCurrentServer().ip;
        return "unknown";
    }

    private static boolean shouldShowOnJoin(Minecraft mc) {
        if (ClientConfig.INTRO_MODE.get() != ClientConfig.IntroMode.NEW_WORLD) return false;
        String key = worldKey(mc);
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
