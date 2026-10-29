package com.anexohorror.director;

import com.anexohorror.HorrorConfig;
import com.anexohorror.entity.HorrorEntity;
import com.anexohorror.entity.HorrorMode;
import com.anexohorror.entity.HorrorVariant;
import com.anexohorror.entity.VanishReason;
import com.anexohorror.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** Estado psicologico y de adaptacion por jugador. */
public final class PlayerState {

    @FunctionalInterface
    public interface Action { void run(ServerPlayer p, ServerLevel l); }

    private record Scheduled(long at, Action action) {}

    ServerPlayer player;
    int cooldown;
    double fear;        // 0..1 sube al ser observado, baja con el tiempo
    double bold;        // 0..1 el jugador sostiene la mirada / se acerca
    double jumpy;       // 0..1 el jugador aparta la vista rapido
    int idleTicks;
    Vec3 lastPos = Vec3.ZERO;
    long silenceUntil;
    long lastSleepScare = -1000L;
    long lastHunt = -1000000L;
    long now;
    HorrorVariant lastVariant;
    final ArrayDeque<EventType> recent = new ArrayDeque<>();
    final ArrayDeque<Integer> recentSounds = new ArrayDeque<>();
    private final List<Scheduled> scheduled = new ArrayList<>();

    PlayerState(ServerPlayer p) {
        this.player = p;
        this.cooldown = 400 + p.getRandom().nextInt(800); // gracia inicial de 20 a 60 s
    }

    public void schedule(int delayTicks, Action a) {
        scheduled.add(new Scheduled(now + Math.max(1, delayTicks), a));
    }

    void tick(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        if (p != player) { // reaparicion / cambio de instancia
            player = p;
            scheduled.clear();
        }
        now = l.getGameTime();

        if (!scheduled.isEmpty()) {
            List<Scheduled> due = new ArrayList<>();
            for (Scheduled s : scheduled) if (now >= s.at()) due.add(s);
            if (!due.isEmpty()) {
                scheduled.removeAll(due);
                for (Scheduled s : due) s.action().run(p, l);
            }
        }

        if (!HorrorConfig.ENABLED.get()) return;
        if (p.isSpectator() || !p.isAlive() || l.dimension() != Level.OVERWORLD) return;

        fear = Math.max(0.0D, fear - 0.00015D);
        bold += (0.3D - bold) * 0.0002D;
        jumpy += (0.3D - jumpy) * 0.0002D;

        Vec3 pos = p.position();
        if (pos.distanceToSqr(lastPos) < 0.0004D) idleTicks++; else idleTicks = 0;
        lastPos = pos;

        if (silenceUntil > now && now % 10 == 0) {
            p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));
            p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.AMBIENT));
        }

        if (--cooldown > 0) return;
        boolean ok = triggerRandom(p, l);
        cooldown = ok ? nextInterval(p, l) : 120;
    }

    static double nightFactor(ServerLevel l) {
        long t = l.getDayTime() % 24000L;
        double f;
        if (t < 12000L) f = 0.0D;
        else if (t < 13000L) f = (t - 12000L) / 1000.0D;
        else if (t < 23000L) f = 1.0D;
        else f = 1.0D - (t - 23000L) / 1000.0D;
        if (l.isThundering()) f = Math.max(f, 0.55D);
        else if (l.isRaining()) f = Math.max(f, 0.25D);
        return f;
    }

    boolean isNight(ServerLevel l) { return nightFactor(l) > 0.5D; }

    boolean isUnderground(ServerPlayer p, ServerLevel l) {
        BlockPos bp = p.blockPosition();
        return !l.canSeeSky(bp) && (p.getY() < l.getSeaLevel() || l.getMaxLocalRawBrightness(bp) <= 6);
    }

    boolean isIndoors(ServerPlayer p, ServerLevel l) {
        return !l.canSeeSky(p.blockPosition()) && !isUnderground(p, l);
    }

    int nextInterval(ServerPlayer p, ServerLevel l) {
        RandomSource r = p.getRandom();
        double night = nightFactor(l);
        int light = l.getMaxLocalRawBrightness(p.blockPosition());
        double dark = Mth.clamp((8 - light) / 8.0D, 0.0D, 1.0D);
        double intensity = (0.35D + 0.65D * night + 0.25D * dark) * HorrorConfig.INTENSITY.get();
        if (HorrorConfig.NIGHT_ESCALATION.get()) intensity *= 1.0D + Math.min(nights(p), 12) * 0.08D;
        double dayBase = 1500 + r.nextInt(3000);   // 1.2 a 3.7 min (solo sonidos de dia)
        double nightBase = 260 + r.nextInt(640);   // 13 a 45 s: la noche esta viva
        double base = HorrorUtil.lerp(dayBase, nightBase, night);
        double ticks = base / Math.max(0.1D, intensity);
        int roll = r.nextInt(100);
        if (roll < 18) ticks = 100 + r.nextInt(200);     // rafaga: otro evento casi seguido
        else if (roll < 23) ticks *= 2.0D;                // calma larga e impredecible
        if (idleTicks > 600) ticks *= 0.75D;              // el jugador quieto atrae mas atencion
        return (int) Math.max(80, ticks);
    }

    private boolean triggerRandom(ServerPlayer p, ServerLevel l) {
        EnumMap<EventType, Double> w = weights(p, l);
        while (!w.isEmpty()) {
            EventType t = HorrorUtil.pickWeighted(p.getRandom(), w);
            if (HorrorEvents.run(t, p, l, this)) {
                recent.addLast(t);
                while (recent.size() > 6) recent.removeFirst();
                return true;
            }
            w.remove(t);
        }
        return false;
    }

    EnumMap<EventType, Double> weights(ServerPlayer p, ServerLevel l) {
        boolean night = isNight(l);
        boolean ug = isUnderground(p, l);
        boolean in = isIndoors(p, l);
        boolean idle = idleTicks > 600;
        EnumMap<EventType, Double> w = new EnumMap<>(EventType.class);
        w.put(EventType.STARE, (ug ? 1.0D : in ? 0.8D : 3.0D) * (1.0D + bold * 0.3D));
        w.put(EventType.PERIPHERAL, 2.2D * (1.0D + jumpy * 0.8D));
        w.put(EventType.PEEK, 2.5D * (1.0D + bold * 0.6D) * (in || ug ? 1.5D : 1.0D) * (idle ? 1.4D : 1.0D));
        w.put(EventType.DOOR, in ? 3.5D : 1.2D);
        w.put(EventType.BED, (night ? 2.4D : 1.0D) * (in ? 2.0D : 0.8D));
        w.put(EventType.CAVE, ug ? 5.0D : 0.0D);
        boolean fake = HorrorConfig.FAKE_SOUNDS.get();
        w.put(EventType.FAKE_SOUND, fake ? 3.0D * (1.0D + jumpy * 0.5D) : 0.0D);
        w.put(EventType.FOOTSTEPS, fake ? (night ? 2.2D : 1.0D) * (idle ? 1.5D : 1.0D) : 0.0D);
        w.put(EventType.SILENCE, HorrorConfig.SILENCE_EVENTS.get() ? 1.0D + fear * 0.8D : 0.0D);
        if (HorrorUtil.isNightTime(l)) {
            for (EventType t : new EventType[]{EventType.STARE, EventType.PERIPHERAL, EventType.PEEK, EventType.DOOR}) {
                w.computeIfPresent(t, (k, v) -> v * 1.5D);          // mas presencias de noche
            }
            w.computeIfPresent(EventType.FAKE_SOUND, (k, v) -> v * 0.6D);
            w.computeIfPresent(EventType.FOOTSTEPS, (k, v) -> v * 0.7D);
            w.put(EventType.WINDOW, in ? 3.0D : 1.2D);
            w.put(EventType.HUNT, 0.45D);
            w.put(EventType.BLACKOUT, 0.9D + fear * 0.6D);
            w.put(EventType.STATIC_BURST, 1.0D);
            w.put(EventType.FAR_CALL, HorrorConfig.FAKE_SOUNDS.get() ? 1.4D : 0.0D);
            w.put(EventType.FAKE_CHAT, 0.5D);
        }
        if (!HorrorUtil.isNightTime(l)) { // las presencias solo salen de noche; de dia quedan los eventos sin entidad
            w.remove(EventType.STARE);
            w.remove(EventType.PERIPHERAL);
            w.remove(EventType.PEEK);
            w.remove(EventType.BED);
            w.remove(EventType.CAVE);
        }
        for (EventType t : recent) w.computeIfPresent(t, (k, v) -> v * 0.4D); // evita patrones predecibles
        w.entrySet().removeIf(e -> e.getValue() <= 0.0D);
        return w;
    }

    static int nights(ServerPlayer p) {
        return p.getPersistentData().getCompound("PlayerPersisted").getInt("anexohorror_nights");
    }

    /** Se llama al empezar cada noche (13000): cuenta la noche y programa la musica turbia tras el video. */
    void onNightfall(ServerPlayer p, ServerLevel l) {
        var data = p.getPersistentData();
        var persisted = data.getCompound("PlayerPersisted");
        persisted.putInt("anexohorror_nights", persisted.getInt("anexohorror_nights") + 1);
        data.put("PlayerPersisted", persisted);
        cooldown = Math.max(cooldown, 900 + p.getRandom().nextInt(900)); // los eventos empiezan despues del video
        if (HorrorConfig.FAKE_SOUNDS.get()) {
            schedule(760, (pl, lv) -> {
                if (HorrorUtil.isNightTime(lv)) {
                    HorrorUtil.sound(pl, ModSounds.DARK_TAPE.get(), SoundSource.AMBIENT, pl.position(), 0.45F, 1.0F);
                }
            });
        }
    }

    /** Adaptacion: el director aprende como reacciona este jugador. */
    void onVanish(HorrorEntity e, VanishReason r) {
        switch (r) {
            case LOOKED_AWAY -> { jumpy = HorrorUtil.lerp(jumpy, 1.0D, 0.2D); bold = HorrorUtil.lerp(bold, 0.0D, 0.1D); fear = Math.min(1.0D, fear + 0.06D); }
            case TOO_CLOSE -> { bold = HorrorUtil.lerp(bold, 1.0D, 0.3D); jumpy = HorrorUtil.lerp(jumpy, 0.0D, 0.1D); }
            case STARE_DONE -> { bold = HorrorUtil.lerp(bold, 1.0D, 0.25D); fear = Math.min(1.0D, fear + 0.12D); }
            case GLIMPSED -> { jumpy = HorrorUtil.lerp(jumpy, 1.0D, 0.15D); fear = Math.min(1.0D, fear + 0.05D); }
            case EXPIRED -> fear = Math.max(0.0D, fear - 0.02D);
            default -> { }
        }
        if (r == VanishReason.LOOKED_AWAY && e.getChainLeft() > 0 && e.getMode() == HorrorMode.STARE
                && HorrorConfig.ESCALATION.get() && player != null) {
            HorrorEvents.scheduleChain(this, player, e);
        }
    }
}
