package com.anexohorror.director;

import com.anexohorror.AnexoHorror;
import com.anexohorror.entity.HorrorEntity;
import com.anexohorror.entity.VanishReason;
import com.anexohorror.HorrorConfig;
import com.anexohorror.net.ClientEffectPacket;
import com.anexohorror.net.ModNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Director del terror: un estado por jugador, eventos aleatorios y adaptativos. */
@Mod.EventBusSubscriber(modid = AnexoHorror.MODID)
public final class HorrorDirector {
    private static final Map<UUID, PlayerState> STATES = new HashMap<>();

    public static PlayerState stateFor(ServerPlayer p) {
        return STATES.computeIfAbsent(p.getUUID(), id -> new PlayerState(p));
    }

    public static void onVanish(UUID owner, HorrorEntity e, VanishReason reason) {
        PlayerState st = STATES.get(owner);
        if (st != null) st.onVanish(e, reason);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        stateFor(sp).tick(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }

    /** Al despertar, a veces hay algo junto a la cama. */
    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        ServerLevel l = sp.serverLevel();
        if (l.dimension() != Level.OVERWORLD) return;
        PlayerState st = stateFor(sp);
        if (sp.getRandom().nextInt(100) < 35) {
            st.schedule(20 + sp.getRandom().nextInt(40),
                    (pl, lv) -> HorrorEvents.bed(pl, lv, st, pl.blockPosition(), 6, 2.0D));
        }
    }

    /**
     * No se puede dormir: cada intento se cancela y provoca un screamer con el mensaje.
     * La cama sigue fijando el punto de reaparicion (configurable).
     */
    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!HorrorConfig.BLOCK_SLEEP.get()) return;
        ServerLevel l = sp.serverLevel();
        if (l.dimension() != Level.OVERWORLD) return;

        event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);

        if (HorrorConfig.BED_SETS_SPAWN.get()) {
            HitResult hit = sp.pick(6.0D, 0.0F, false);
            if (hit instanceof BlockHitResult bhr && l.getBlockState(bhr.getBlockPos()).getBlock() instanceof BedBlock) {
                sp.setRespawnPosition(l.dimension(), bhr.getBlockPos(), sp.getYRot(), false, true);
            }
        }

        PlayerState st = stateFor(sp);
        long now = l.getGameTime();
        if (now - st.lastSleepScare < 100L) return; // un solo susto por intento
        st.lastSleepScare = now;
        ModNetwork.send(sp, ClientEffectPacket.SCREAMER, -1, 0, "NO ESCAPAR\u00c1S DE ESTA REALIDAD");
    }

    private HorrorDirector() {}
}
