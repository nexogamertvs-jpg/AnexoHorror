package com.anexohorror.director;

import com.anexohorror.AnexoHorror;
import com.anexohorror.net.ClientEffectPacket;
import com.anexohorror.net.ModNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Cronometro del anochecer: el SERVIDOR vigila el reloj del mundo y, en el instante exacto en que
 * empieza la noche (tick 13000 del ciclo de 24000), manda al jugador el video VHS.
 * Asi no depende de la configuracion ni del reloj del cliente.
 */
@Mod.EventBusSubscriber(modid = AnexoHorror.MODID)
public final class NightDirector {
    private static final Map<ServerLevel, Long> PREV = new WeakHashMap<>();

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        if (!(event.level instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) return;
        long t = sl.getDayTime() % 24000L;
        Long prev = PREV.put(sl, t);
        if (prev != null && prev < 13000L && t >= 13000L && t < 14500L) {
            nightfall(sl);
        }
    }

    /** Tambien sirve para /time set: cualquier salto que cruce 13000 cuenta como anochecer. */
    public static void nightfall(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isSpectator()) continue;
            HorrorDirector.stateFor(p).onNightfall(p, sl);
            ModNetwork.send(p, ClientEffectPacket.VIDEO, 0, 0, "");
        }
    }

    private NightDirector() {}
}
