package com.anexohorror.director;

import com.anexohorror.AnexoHorror;
import com.anexohorror.net.ClientEffectPacket;
import com.anexohorror.net.ModNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * Cronometro del anochecer: el servidor detecta el cruce del tick 13000 del ciclo
 * de 24000 y manda el evento VHS a los clientes. El cliente decide segun IntroMode.
 */
@Mod.EventBusSubscriber(modid = AnexoHorror.MODID)
public final class NightDirector {
    private static final Map<ServerLevel, Long> PREV = new WeakHashMap<>();
    private static final Map<UUID, Long> VIDEO_DAY = new HashMap<>();

    private static void sendNightVideoOnce(ServerPlayer player, long day) {
        if (VIDEO_DAY.getOrDefault(player.getUUID(), Long.MIN_VALUE) == day) return;
        VIDEO_DAY.put(player.getUUID(), day);
        ModNetwork.send(player, ClientEffectPacket.VIDEO, 0, 0, "");
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        if (!(event.level instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) return;

        long now = sl.getDayTime();
        Long previous = PREV.put(sl, now);
        long dayTick = now % 24000L;
        // Si el servidor carga ya de noche, se procesa una sola vez en el primer tick.
        if (previous == null) {
            if (dayTick >= 13000L && dayTick < 23000L) nightfall(sl);
            return;
        }
        if (now <= previous) return; // /time set hacia atras: no simular noches recorridas

        long firstCrossing = (previous / 24000L) * 24000L + 13000L;
        if (firstCrossing <= previous) firstCrossing += 24000L;
        if (dayTick >= 13000L && dayTick < 23000L && firstCrossing <= now) {
            nightfall(sl);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel sl = player.serverLevel();
        if (sl.dimension() != Level.OVERWORLD) return;
        long tick = sl.getDayTime() % 24000L;
        if (tick >= 13000L && tick < 23000L) {
            // Cuenta la noche para quien entra tarde y no pierde el VHS.
            HorrorDirector.stateFor(player).onNightfall(player, sl);
            sendNightVideoOnce(player, sl.getDayTime() / 24000L);
        }
    }

    /** Tambien sirve para /time set: cualquier salto que cruce 13000 cuenta como anochecer. */
    public static void nightfall(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isSpectator()) continue;
            HorrorDirector.stateFor(p).onNightfall(p, sl); // cuenta la noche
            // VIDEO respeta IntroMode.NIGHTFALL en el cliente y aparece una sola vez por noche.
            sendNightVideoOnce(p, sl.getDayTime() / 24000L);
            if (PlayerState.nights(p) > 1) {
                long day = sl.getDayTime() / 24000L + 1L;
                ModNetwork.send(p, ClientEffectPacket.NIGHT_TITLE, (int) Math.min(day, 100000L), 0, "");
            }
        }
    }

    private NightDirector() {}
}
