package com.anexohorror.director;

import com.anexohorror.HorrorConfig;
import com.anexohorror.entity.HorrorEntity;
import com.anexohorror.entity.HorrorMode;
import com.anexohorror.entity.HorrorVariant;
import com.anexohorror.entity.VanishReason;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
    long lastHunt = -1000000L;
    long lastOther = -1000000L;
    long lastEntityMusic = -1000000L;
    long now;
    HorrorVariant lastVariant;
    final ArrayDeque<EventType> recent = new ArrayDeque<>();
    final ArrayDeque<EventType> eventDeck = new ArrayDeque<>();
    final ArrayDeque<Integer> recentSounds = new ArrayDeque<>();
    private net.minecraft.resources.ResourceKey<Level> previousDimension;
    private boolean suspended;
    private long recoveryUntil;
    private long lastStrongEvent = -1000000L;
    private EventType lastEvent;
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
        now = l.getGameTime();
        boolean changed = p != player || (previousDimension != null && previousDimension != l.dimension());
        player = p;
        previousDimension = l.dimension();
        boolean eligible = HorrorConfig.ENABLED.get() && !p.isSpectator()
                && p.isAlive() && l.dimension() == Level.OVERWORLD;
        if (changed || !eligible) {
            scheduled.clear();
            silenceUntil = 0;
            idleTicks = 0;
            lastPos = p.position();
            cooldown = Math.max(cooldown, 600);
        }
        if (!eligible) { suspended = true; return; }
        if (suspended) {
            suspended = false;
            cooldown = Math.max(cooldown, 600);
        }

        if (!scheduled.isEmpty()) {
            List<Scheduled> due = new ArrayList<>();
            for (Scheduled task : scheduled) if (now >= task.at()) due.add(task);
            scheduled.removeAll(due);
            for (Scheduled task : due) task.action().run(p, l);
        }

        if (now % 200L == 0L) StoryChests.tick(p, l);
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

        // Deja respirar al jugador antes de iniciar otro encuentro aleatorio.
        if (now < recoveryUntil || p.isSleeping()) return;
        if (--cooldown > 0) return;
        if (hasActiveThreat(p, l)) { cooldown = 100; return; }
        triggerRandom(p, l);
        // Incluso si un evento no encuentra su escenario, la baraja conserva ese evento
        // para volver a intentarlo en el siguiente ciclo, sin hacer intentos cada 6 segundos.
        cooldown = nextInterval(p, l);
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
        // Ritmo estable: de dia hay mas silencio; de noche los eventos llegan
        // con algo mas de frecuencia, pero nunca se convierten en una sucesion continua.
        double intensity = (0.62D + 0.38D * night + 0.15D * dark) * HorrorConfig.INTENSITY.get();
        if (HorrorConfig.NIGHT_ESCALATION.get()) intensity *= 1.0D + Math.min(nights(p), 8) * 0.035D;
        double dayBase = 1100 + r.nextInt(700);   // 55-90 s antes del factor de intensidad
        double nightBase = 850 + r.nextInt(450); // 42-65 s de noche
        double base = HorrorUtil.lerp(dayBase, nightBase, night);
        double ticks = base / Math.max(0.1D, intensity);
        // El miedo elevado prolonga la incertidumbre en lugar de acumular sustos.
        ticks *= 1.0D + fear * 0.35D;
        int roll = r.nextInt(100);
        if (roll < 5 && fear < 0.5D && nights(p) > 1) ticks = Math.max(500, ticks * 0.65D); // pausa corta excepcional, no una rafaga constante
        else if (roll < 18) ticks *= 1.35D;          // silencios largos para conservar tension
        if (idleTicks > 600) ticks *= 0.90D;
        return (int) Mth.clamp(ticks, 300.0D, 5200.0D); // 15 s a 4 min 20 s
    }

    /**
     * Baraja justa por jugador: cada tipo normal se retira de la baraja al ejecutarse,
     * y solo vuelve cuando los demas hayan tenido su oportunidad. Si un evento no puede
     * ejecutarse por el entorno o por el limite de entidades, vuelve al final de la cola.
     */
    private void refillEventDeck(ServerPlayer p) {
        List<EventType> pool = new ArrayList<>(Arrays.asList(EventType.values()));
        Collections.shuffle(pool, new Random(p.getRandom().nextLong()));
        // Evita que la nueva ronda empiece con uno de los ultimos eventos de la ronda anterior.
        int guard = Math.min(recent.size(), Math.max(0, pool.size() - 1));
        for (int i = 0; i < guard && recent.contains(pool.get(0)); i++) {
            Collections.rotate(pool, -1);
        }
        eventDeck.addAll(pool);
    }

    private void refillRoutineEvents(ServerPlayer p) {
        List<EventType> pool = new ArrayList<>();
        for (EventType type : EventType.values()) {
            if (type != EventType.HUNT && type != EventType.OTHER && !eventDeck.contains(type)) {
                pool.add(type);
            }
        }
        if (pool.isEmpty()) return;
        Collections.shuffle(pool, new Random(p.getRandom().nextLong()));
        int guard = Math.min(recent.size(), Math.max(0, pool.size() - 1));
        for (int i = 0; i < guard && recent.contains(pool.get(0)); i++) {
            Collections.rotate(pool, -1);
        }
        eventDeck.addAll(pool);
    }

    private boolean temporarilyLocked(EventType type, ServerPlayer p, ServerLevel l) {
        long gameTime = l.getGameTime();
        if (isStrong(type) && (nights(p) == 0 || gameTime - lastStrongEvent < 1800L)) return true;
        if (type == EventType.HUNT || type == EventType.OTHER) {
            if (hasActiveThreat(p, l)) return true;
        }
        if (type == EventType.SILENCE && !HorrorConfig.SILENCE_EVENTS.get()) return true;
        if (type == EventType.HUNT) {
            return !HorrorConfig.HUNT_ENABLED.get()
                    || !HorrorUtil.entitiesAllowed(l)
                    || nights(p) < HorrorConfig.HUNT_MIN_NIGHT.get()
                    || gameTime - lastHunt < HorrorConfig.HUNT_COOLDOWN_MIN.get() * 1200L;
        }
        if (type == EventType.OTHER) {
            return !HorrorConfig.OTHER_ENABLED.get()
                    || !HorrorUtil.entitiesAllowed(l)
                    || l.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL
                    || nights(p) < HorrorConfig.OTHER_MIN_NIGHT.get()
                    || gameTime - lastOther < HorrorConfig.OTHER_COOLDOWN_MIN.get() * 1200L
                    || !l.getEntitiesOfClass(com.anexohorror.entity.OtherEntity.class,
                            p.getBoundingBox().inflate(160.0D)).isEmpty();
        }
        return false;
    }

    private boolean triggerRandom(ServerPlayer p, ServerLevel l) {
        if (eventDeck.isEmpty()) refillEventDeck(p);
        int attempts = eventDeck.size();
        for (int i = 0; i < attempts; i++) {
            EventType type = eventDeck.removeFirst();
            if (type == lastEvent || temporarilyLocked(type, p, l)) {
                eventDeck.addLast(type);
                continue;
            }
            if (HorrorEvents.run(type, p, l, this)) {
                lastEvent = type;
                if (isStrong(type)) {
                    lastStrongEvent = now;
                    recoveryUntil = now + 600L;
                }
                recent.addLast(type);
                while (recent.size() > 6) recent.removeFirst();
                return true;
            }
            // No se descarta un evento fallido: se pospone para otra oportunidad.
            eventDeck.addLast(type);
        }
        // Si solo quedan los dos eventos especiales bloqueados por progresion/cooldown,
        // siguen esperando, pero la rotacion de sucesos comunes empieza otra vuelta.
        refillRoutineEvents(p);
        return false;
    }

    private static boolean hasActiveThreat(ServerPlayer p, ServerLevel l) {
        return !l.getEntitiesOfClass(HorrorEntity.class, p.getBoundingBox().inflate(160.0D),
                e -> e.isAlive() && p.getUUID().equals(e.getOwnerId()) && e.getMode() == HorrorMode.HUNT).isEmpty()
                || !l.getEntitiesOfClass(com.anexohorror.entity.OtherEntity.class,
                        p.getBoundingBox().inflate(160.0D), e -> e.isAlive()).isEmpty();
    }

    private static boolean isStrong(EventType type) {
        return type == EventType.HUNT || type == EventType.OTHER || type == EventType.BLACKOUT
                || type == EventType.STATIC_BURST || type == EventType.TAPE_CORRUPTION;
    }

    public long recoverySeconds() { return Math.max(0L, recoveryUntil - now + 19L) / 20L; }

    public String nextEventName() {
        return eventDeck.isEmpty() ? "baraja nueva" : eventDeck.peekFirst().name().toLowerCase();
    }

    public int pendingEvents() {
        return eventDeck.size();
    }

    static int nights(ServerPlayer p) {
        return p.getPersistentData().getCompound("PlayerPersisted").getInt("anexohorror_nights");
    }

    /** Se llama al empezar cada noche (13000): cuenta la noche y da margen al anochecer.
     * La musica ya no se reproduce aqui: ahora se selecciona aleatoriamente al aparecer una entidad. */
    void onNightfall(ServerPlayer p, ServerLevel l) {
        var data = p.getPersistentData();
        var persisted = data.getCompound("PlayerPersisted");
        long worldDay = l.getDayTime() / 24000L;
        if (!persisted.contains("anexohorror_last_night_day")
                || persisted.getLong("anexohorror_last_night_day") != worldDay) {
            persisted.putLong("anexohorror_last_night_day", worldDay);
            persisted.putInt("anexohorror_nights", persisted.getInt("anexohorror_nights") + 1);
        }
        data.put("PlayerPersisted", persisted);
        cooldown = Math.max(cooldown, 900 + p.getRandom().nextInt(900));
    }

    /** Adaptacion: el director aprende como reacciona este jugador. */
    void onVanish(HorrorEntity e, VanishReason r) {
        if (e.getMode() == HorrorMode.HUNT) {
            recoveryUntil = Math.max(recoveryUntil, now + 1200L);
            fear = Math.min(1.0D, fear + 0.15D);
        }
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
