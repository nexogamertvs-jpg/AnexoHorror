package com.anexohorror.director;

import com.anexohorror.HorrorConfig;
import com.anexohorror.entity.HorrorEntity;
import com.anexohorror.entity.HorrorMode;
import com.anexohorror.entity.HorrorVariant;
import com.anexohorror.entity.OtherEntity;
import com.anexohorror.entity.PeekPose;
import com.anexohorror.net.ClientEffectPacket;
import com.anexohorror.net.ModNetwork;
import com.anexohorror.registry.ModEntities;
import com.anexohorror.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Ejecucion de cada tipo de evento. Devuelve false si no hubo lugar valido (el director prueba otro). */
public final class HorrorEvents {

    // Pesos por variante, orden: PEEKER, GRIN, SMILER, DRAWN, CRAWLER, EYE, STALKER
    private static final double[] W_STARE = {1.0, 1.5, 3.0, 3.0, 2.5, 1.0, 2.5};
    private static final double[] W_PERIPH = {1.5, 2.0, 3.0, 2.0, 3.0, 1.5, 2.0};
    private static final double[] W_PEEK = {4.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0};
    private static final double[] W_DOOR = {3.0, 2.5, 2.5, 2.0, 2.5, 2.5, 2.5};
    private static final double[] W_BED = {3.0, 2.0, 0.0, 0.0, 2.0, 2.0, 0.0};
    private static final double[] W_CAVE = {0.5, 3.0, 2.0, 2.0, 2.0, 3.0, 2.0};

    private static final HorrorVariant[] VARIANTS = HorrorVariant.values();

    public static boolean run(EventType t, ServerPlayer p, ServerLevel l, PlayerState st) {
        return switch (t) {
            case STARE -> stare(p, l, st) || presenceCue(p, l, true);
            case PERIPHERAL -> peripheral(p, l, st) || presenceCue(p, l, false);
            case PEEK -> peek(p, l, st) || presenceCue(p, l, true);
            case DOOR -> door(p, l, st);
            case BED -> bed(p, l, st, p.blockPosition(), 14, 3.0D) || bedEcho(p, l, st);
            case CAVE -> cave(p, l, st) || caveEcho(p, l, st);
            case FAKE_SOUND -> fakeSound(p, l, st);
            case FOOTSTEPS -> footsteps(p, l, st) || approachingSteps(p, l, st);
            case SILENCE -> silence(p, l, st);
            case BLACKOUT -> blackout(p, l, st);
            case STATIC_BURST -> staticBurst(p, l, st);
            case FAR_CALL -> farCall(p, l, st);
            case FAKE_CHAT -> fakeChat(p, l, st);
            case WINDOW -> window(p, l, st) || glassScratch(p, l, st);
            case HUNT -> hunt(p, l, st);
            case OTHER -> other(p, l, st);
            case WALL_TAPS -> wallTaps(p, l, st);
            case DOOR_RATTLE -> doorRattle(p, l, st);
            case GLASS_SCRATCH -> glassScratch(p, l, st);
            case CEILING_STEPS -> ceilingSteps(p, l, st);
            case TAPE_CORRUPTION -> tapeCorruption(p, l, st);
            case DOUBLE_PEEK -> doublePeek(p, l, st);
            case DISTANT_STARE -> distantStare(p, l, st) || presenceCue(p, l, false);
            case BREATH_BEHIND -> breathBehind(p, l, st);
            case APPROACHING_STEPS -> approachingSteps(p, l, st);
            case FALSE_BLOCK_BREAK -> falseBlockBreak(p, l, st);
            case SECOND_PRESENCE -> secondPresence(p, l, st);
            case VOICE_ECHO -> voiceEcho(p, l, st);
        };
    }

    /** Ejecucion determinista para comandos: no depende de probabilidad, horario ni cooldown. */
    public static boolean runCommand(EventType t, ServerPlayer p, ServerLevel l, PlayerState st) {
        boolean previousForce = HorrorUtil.forceDay;
        HorrorUtil.forceDay = true;
        try {
            return switch (t) {
                case STARE -> forcedStare(p, l, st);
                case PERIPHERAL -> forcedPeripheral(p, l, st);
                case PEEK -> forcedHeadPeek(p, l, st);
                case DOOR -> door(p, l, st); // busca una puerta real; sin puerta, usa una esquina visible
                case BED -> bed(p, l, st, p.blockPosition(), 6, 0.0D) || forcedCornerPeek(p, l, st);
                case CAVE -> cave(p, l, st) || forcedCornerPeek(p, l, st);
                case WINDOW -> window(p, l, st) || forcedCornerPeek(p, l, st);
                case HUNT -> forcedHunt(p, l, st);
                case WALL_TAPS -> wallTaps(p, l, st);
                case DOOR_RATTLE -> doorRattle(p, l, st);
                case GLASS_SCRATCH -> glassScratch(p, l, st);
                case CEILING_STEPS -> ceilingSteps(p, l, st);
                case TAPE_CORRUPTION -> tapeCorruption(p, l, st);
                case DOUBLE_PEEK -> doublePeek(p, l, st);
                case DISTANT_STARE -> distantStare(p, l, st) || presenceCue(p, l, false);
                case BREATH_BEHIND -> breathBehind(p, l, st);
                case APPROACHING_STEPS -> approachingSteps(p, l, st);
                case FALSE_BLOCK_BREAK -> falseBlockBreak(p, l, st);
                case SECOND_PRESENCE -> secondPresence(p, l, st);
                case VOICE_ECHO -> voiceEcho(p, l, st);
                default -> run(t, p, l, st);
            };
        } finally {
            HorrorUtil.forceDay = previousForce;
        }
    }

    private static boolean forcedStare(ServerPlayer p, ServerLevel l, PlayerState st) {
        HorrorVariant v = pick(p.getRandom(), W_STARE, st, st.isNight(l));
        Vec3 pos = forcedVisiblePosition(p, l, v);
        return pos != null && spawn(p, l, st, v, HorrorMode.STARE, pos, 0, 1.0F) != null;
    }

    private static boolean forcedPeripheral(ServerPlayer p, ServerLevel l, PlayerState st) {
        HorrorVariant v = pick(p.getRandom(), W_PERIPH, st, st.isNight(l));
        Vec3 look = horizontalLook(p);
        Vec3 pos = null;
        boolean edgePosition = false;
        for (double distance : new double[]{7.0D, 9.0D, 11.0D}) {
            for (double sign : new double[]{-1.0D, 1.0D}) {
                Vec3 dir = rotate(look, Math.toRadians(sign * 68.0D));
                Optional<BlockPos> ground = HorrorUtil.ground(l,
                        Mth.floor(p.getX() + dir.x * distance), Mth.floor(p.getZ() + dir.z * distance),
                        p.getBlockY(), 5, 12);
                if (ground.isEmpty()) continue;
                Vec3 candidate = new Vec3(ground.get().getX() + 0.5D, ground.get().getY(),
                        ground.get().getZ() + 0.5D);
                double dot = cosToEye(p, candidate, v.hover + v.height * 0.6D);
                if (dot > 0.05D && dot < 0.72D
                        && HorrorUtil.visibleSamples(l, p.getEyePosition(), p, candidate,
                        v.hover, v.height, v.height * v.aspect) > 0) {
                    pos = candidate;
                    edgePosition = true;
                    break;
                }
            }
            if (pos != null) break;
        }
        if (pos == null) pos = forcedVisiblePosition(p, l, v);
        HorrorMode mode = edgePosition ? HorrorMode.PERIPHERAL : HorrorMode.STARE;
        return pos != null && spawn(p, l, st, v, mode, pos, 0, 1.0F) != null;
    }

    private static boolean forcedHunt(ServerPlayer p, ServerLevel l, PlayerState st) {
        HorrorVariant v = VARIANTS[p.getRandom().nextInt(VARIANTS.length)];
        Vec3 pos = forcedVisiblePosition(p, l, v);
        if (pos == null) return false;
        for (HorrorEntity old : l.getEntitiesOfClass(HorrorEntity.class, p.getBoundingBox().inflate(96.0D),
                e -> p.getUUID().equals(e.getOwnerId()))) old.discard();
        HorrorEntity entity = spawn(p, l, st, v, HorrorMode.STARE, pos, 0, 1.0F);
        if (entity == null) return false;
        entity.beginHunt(p, 100);
        ModNetwork.send(p, ClientEffectPacket.LOCK, entity.getId(), 100, "NO ESCAPARÁS NUNCA MÁS DE MÍ");
        p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));
        p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.AMBIENT));
        return true;
    }

    // ------------------------------------------------------------------ utilidades

    private static HorrorVariant pick(RandomSource r, double[] w, PlayerState st, boolean night) {
        double[] ww = w.clone();
        if (night) { ww[HorrorVariant.CRAWLER.ordinal()] *= 1.4D; ww[HorrorVariant.GRIN.ordinal()] *= 1.4D; }
        if (st.lastVariant != null) ww[st.lastVariant.ordinal()] *= 0.35D;
        double sum = 0.0D;
        for (double d : ww) sum += d;
        double x = r.nextDouble() * sum;
        for (int i = 0; i < ww.length; i++) {
            x -= ww[i];
            if (x <= 0.0D && ww[i] > 0.0D) return VARIANTS[i];
        }
        return VARIANTS[0];
    }

    static int active(ServerPlayer p, ServerLevel l) {
        return l.getEntitiesOfClass(HorrorEntity.class, p.getBoundingBox().inflate(96.0D),
                e -> p.getUUID().equals(e.getOwnerId())).size();
    }

    /** Nunca mas de dos presencias comunes simultaneas por jugador, incluso con TOML antiguo. */
    private static int entityCap() {
        return Mth.clamp(HorrorConfig.MAX_ENTITIES.get(), 1, 2);
    }

    private static boolean canSpawnMore(ServerPlayer p, ServerLevel l, boolean night) {
        if (!HorrorUtil.entitiesAllowed(l)) return false; // presencias completas solo de noche
        int a = active(p, l);
        return night ? a < entityCap() : a < 1;
    }

    /** Alternativa sonora: el evento ocurre sin añadir otra entidad al mundo. */
    private static boolean presenceCue(ServerPlayer p, ServerLevel l, boolean behind) {
        RandomSource r = p.getRandom();
        Vec3 pos = around(p, 7.0D, 15.0D, behind);
        HorrorUtil.sound(p, ModSounds.DISTANT_SOUND.get(), SoundSource.HOSTILE, pos,
                0.42F + r.nextFloat() * 0.16F, 0.78F + r.nextFloat() * 0.20F);
        if (r.nextInt(100) < 55) {
            stScheduleWhisper(p, l, behind);
        }
        return true;
    }

    private static void stScheduleWhisper(ServerPlayer p, ServerLevel l, boolean behind) {
        PlayerState st = HorrorDirector.stateFor(p);
        st.schedule(28 + p.getRandom().nextInt(35), (pl, lv) -> HorrorUtil.sound(pl,
                ModSounds.WHISPER.get(), SoundSource.HOSTILE,
                around(pl, 2.0D, 4.0D, behind), 0.32F, 0.78F));
    }

    /** La escena de cama se conserva como audio bajo el suelo si no hay una cama cerca. */
    private static boolean bedEcho(ServerPlayer p, ServerLevel l, PlayerState st) {
        Vec3 pos = new Vec3(p.getX() + (p.getRandom().nextDouble() - 0.5D) * 2.0D,
                p.getY() - 0.35D, p.getZ() + (p.getRandom().nextDouble() - 0.5D) * 2.0D);
        HorrorUtil.sound(p, ModSounds.BREATH.get(), SoundSource.HOSTILE, pos, 0.48F, 0.68F);
        st.schedule(24 + p.getRandom().nextInt(24), (pl, lv) -> HorrorUtil.sound(pl,
                ModSounds.SCRAPE.get(), SoundSource.HOSTILE,
                new Vec3(pl.getX(), pl.getY() - 0.4D, pl.getZ()), 0.42F, 0.72F));
        return true;
    }

    /** En superficie, el evento de cueva se manifiesta como una resonancia que parece venir de abajo. */
    private static boolean caveEcho(ServerPlayer p, ServerLevel l, PlayerState st) {
        Vec3 below = new Vec3(p.getX(), p.getY() - 1.4D, p.getZ());
        HorrorUtil.sound(p, ModSounds.DRONE.get(), SoundSource.AMBIENT, below, 0.34F, 0.62F);
        st.schedule(42 + p.getRandom().nextInt(30), (pl, lv) -> HorrorUtil.sound(pl,
                ModSounds.DISTANT_SOUND.get(), SoundSource.HOSTILE,
                around(pl, 9.0D, 16.0D, true), 0.42F, 0.70F));
        return true;
    }

    private static double minDist(PlayerState st, boolean night) {
        double d = night ? HorrorConfig.MIN_DIST_NIGHT.get() : HorrorConfig.MIN_DIST_DAY.get();
        double floor = night ? 14.0D : 20.0D;
        return Math.max(floor, d - st.bold * 3.0D); // incluso si el jugador se acerca, mantienen distancia de acecho
    }

    private static Vec3 horizontalLook(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 h = new Vec3(look.x, 0.0D, look.z);
        return h.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : h.normalize();
    }

    private static Vec3 rotate(Vec3 v, double rad) {
        double c = Math.cos(rad), s = Math.sin(rad);
        return new Vec3(v.x * c - v.z * s, 0.0D, v.x * s + v.z * c);
    }

    private static double cosToEye(ServerPlayer p, Vec3 feet, double centerY) {
        Vec3 c = new Vec3(feet.x, feet.y + centerY, feet.z);
        return p.getLookAngle().dot(c.subtract(p.getEyePosition()).normalize());
    }

    /** Crea la entidad, reproduce el crujido original y la registra. */
    public static HorrorEntity spawn(ServerPlayer p, ServerLevel l, PlayerState st, HorrorVariant v,
                                     HorrorMode mode, Vec3 pos, int chain, float scale) {
        HorrorEntity e = ModEntities.WATCHER.get().create(l);
        if (e == null) return null;
        RandomSource r = p.getRandom();
        boolean night = st.isNight(l);
        double dist = Math.sqrt((pos.x - p.getX()) * (pos.x - p.getX()) + (pos.z - p.getZ()) * (pos.z - p.getZ()));
        int maxLife, stareLimit, grace, glimpse;
        double vanishDist;
        switch (mode) {
            case PERIPHERAL -> { maxLife = 50 + r.nextInt(70); stareLimit = 0; grace = 0; glimpse = 2 + r.nextInt(4); vanishDist = 3.0D; }
            case PEEK -> { maxLife = 420 + (night ? 300 + r.nextInt(600) : 150 + r.nextInt(250));
                stareLimit = night ? 70 + r.nextInt(110) : 50 + r.nextInt(80); grace = night ? 24 + r.nextInt(16) : 16 + r.nextInt(12);
                glimpse = 0; vanishDist = Math.min(4.0D, dist * 0.5D); }
            case LOW -> { maxLife = 260 + r.nextInt(300); stareLimit = (night ? 120 : 80) + r.nextInt(160);
                grace = 5 + r.nextInt(6); glimpse = 0; vanishDist = Math.min(3.0D, dist * 0.5D); }
            default -> { maxLife = night ? 400 + r.nextInt(700) : 220 + r.nextInt(400);
                stareLimit = night ? 200 + r.nextInt(400) : 90 + r.nextInt(180);
                grace = night ? 18 + r.nextInt(22) : 12 + r.nextInt(14); glimpse = 0;
                vanishDist = Mth.clamp(dist * 0.4D, 4.5D, 9.0D); }
        }
        e.setup(p.getUUID(), v, mode, scale, maxLife, stareLimit, grace, glimpse, vanishDist, chain);
        float yaw = (float) (Mth.atan2(p.getZ() - pos.z, p.getX() - pos.x) * (180.0D / Math.PI)) - 90.0F;
        e.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        e.setAllowDay(!HorrorUtil.isNightTime(l)); // solo true si se invoco por comando de dia
        l.addFreshEntity(e);

        float vol = Math.max(0.75F, (float) dist / 18.0F + 0.2F);
        float pitch = 0.85F + r.nextFloat() * 0.3F;
        l.playSound(null, pos.x, pos.y, pos.z, ModSounds.CRACK.get(), SoundSource.HOSTILE, vol, pitch);

        maybeEntityMusic(p, l, st, pos);
        st.lastVariant = v;
        return e;
    }

    /** Musica aleatoria vinculada a una aparicion, nunca al cambio de dia. */
    private static void maybeEntityMusic(ServerPlayer p, ServerLevel l, PlayerState st, Vec3 pos) {
        RandomSource r = p.getRandom();
        long now = l.getGameTime();
        if (now - st.lastEntityMusic < 2400L || r.nextInt(100) >= 34) return;
        net.minecraft.sounds.SoundEvent[] tracks = {
                ModSounds.ENTITY_MUSIC_1.get(), ModSounds.ENTITY_MUSIC_2.get(),
                ModSounds.ENTITY_MUSIC_3.get(), ModSounds.ENTITY_MUSIC_4.get()
        };
        net.minecraft.sounds.SoundEvent track = tracks[r.nextInt(tracks.length)];
        st.lastEntityMusic = now;
        st.schedule(12 + r.nextInt(36), (pl, lv) -> {
            if (pl.isAlive() && lv.dimension() == pl.level().dimension()) {
                HorrorUtil.sound(pl, track, SoundSource.MUSIC, pl.position(), 0.48F,
                        0.92F + pl.getRandom().nextFloat() * 0.16F);
            }
        });
    }

    /** Huevo generador: aparece donde clicas, se queda ~2 min y solo se va si te acercas. */
    public static HorrorEntity spawnFromEgg(ServerPlayer p, ServerLevel l, HorrorVariant v, Vec3 pos) {
        HorrorEntity e = ModEntities.WATCHER.get().create(l);
        if (e == null) return null;
        e.setup(p.getUUID(), v, HorrorMode.STARE, 1.0F, 2400, 2400, 2400, 0, 2.5D, 0);
        e.setAllowDay(true);
        float yaw = (float) (Mth.atan2(p.getZ() - pos.z, p.getX() - pos.x) * (180.0D / Math.PI)) - 90.0F;
        e.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        l.addFreshEntity(e);
        l.playSound(null, pos.x, pos.y, pos.z, ModSounds.CRACK.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        maybeEntityMusic(p, l, HorrorDirector.stateFor(p), pos);
        return e;
    }

    // ------------------------------------------------------------------ busqueda de lugares

    private static Vec3 findVisible(ServerPlayer p, ServerLevel l, HorrorVariant v, double minD, double maxD,
                                    double halfAngleDeg, int maxLight, int minSamples) {
        RandomSource r = p.getRandom();
        Vec3 hl = horizontalLook(p);
        Vec3 eye = p.getEyePosition();
        for (int i = 0; i < 120; i++) {
            double ang = Math.toRadians((r.nextDouble() * 2.0D - 1.0D) * halfAngleDeg);
            double d = minD + r.nextDouble() * (maxD - minD);
            Vec3 dir = rotate(hl, ang);
            Optional<BlockPos> g = HorrorUtil.ground(l, Mth.floor(p.getX() + dir.x * d),
                    Mth.floor(p.getZ() + dir.z * d), p.getBlockY(), 5, 14);
            if (g.isEmpty()) continue;
            BlockPos gp = g.get();
            if (l.getMaxLocalRawBrightness(gp) > maxLight) continue;
            Vec3 feet = new Vec3(gp.getX() + 0.5D, gp.getY(), gp.getZ() + 0.5D);
            if (HorrorUtil.visibleSamples(l, eye, p, feet, v.hover, v.height, v.height * v.aspect) >= minSamples) {
                return feet;
            }
        }
        return null;
    }

    private static Vec3 findPeripheral(ServerPlayer p, ServerLevel l, HorrorVariant v, int maxLight) {
        RandomSource r = p.getRandom();
        Vec3 hl = horizontalLook(p);
        Vec3 eye = p.getEyePosition();
        for (int i = 0; i < 50; i++) {
            double sign = r.nextBoolean() ? 1.0D : -1.0D;
            double ang = Math.toRadians(sign * (55.0D + r.nextDouble() * 30.0D));
            double d = 10.0D + r.nextDouble() * 11.0D;
            Vec3 dir = rotate(hl, ang);
            Optional<BlockPos> g = HorrorUtil.ground(l, Mth.floor(p.getX() + dir.x * d),
                    Mth.floor(p.getZ() + dir.z * d), p.getBlockY(), 4, 8);
            if (g.isEmpty()) continue;
            BlockPos gp = g.get();
            if (l.getMaxLocalRawBrightness(gp) > maxLight) continue;
            Vec3 feet = new Vec3(gp.getX() + 0.5D, gp.getY(), gp.getZ() + 0.5D);
            double c = cosToEye(p, feet, v.hover + v.height * 0.6D);
            if (c < 0.08D || c > 0.62D) continue; // borde del campo visual
            if (HorrorUtil.visibleSamples(l, eye, p, feet, v.hover, v.height, v.height * v.aspect) >= 1) return feet;
        }
        return null;
    }

    /** Busca un punto parcialmente tapado por esquinas, marcos, bloques... (asomandose). */
    private static BlockPos findBlock(ServerPlayer p, ServerLevel l, BlockPos c, int rad, int vr,
                                      Predicate<BlockState> f, double minDist) {
        RandomSource r = p.getRandom();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 260; i++) {
            m.set(c.getX() + r.nextInt(rad * 2 + 1) - rad, c.getY() + r.nextInt(vr * 2 + 1) - vr,
                    c.getZ() + r.nextInt(rad * 2 + 1) - rad);
            if (!l.hasChunkAt(m)) continue;
            if (f.test(l.getBlockState(m)) && m.distSqr(p.blockPosition()) >= minDist * minDist) {
                return m.immutable();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ eventos con entidad

    private static boolean stare(ServerPlayer p, ServerLevel l, PlayerState st) {
        boolean night = st.isNight(l);
        if (!canSpawnMore(p, l, night)) return false;
        RandomSource r = p.getRandom();
        HorrorVariant v = pick(r, W_STARE, st, night);
        double minD = minDist(st, night);
        double maxD = Math.min(48.0D, minD + (night ? 18.0D : 26.0D));
        int maxLight = night ? 12 : 15;
        Vec3 pos = findVisible(p, l, v, minD, maxD, 40.0D, maxLight, 4);
        if (pos == null) pos = findVisible(p, l, v, minD, maxD, 75.0D, maxLight, 3);
        if (pos == null) return false;
        int chainChance = Math.min(70, 30 + PlayerState.nights(p) * 3);
        int chainSize = 1 + Math.min(2, PlayerState.nights(p) / 4);
        int chain = (night && HorrorConfig.ESCALATION.get() && r.nextInt(100) < chainChance)
                ? 1 + r.nextInt(chainSize) : 0;
        spawn(p, l, st, v, HorrorMode.STARE, pos, chain, 1.0F);
        if (night && active(p, l) < entityCap() && r.nextInt(100) < 18) {
            HorrorVariant v2 = pick(r, W_STARE, st, true);
            Vec3 pos2 = findVisible(p, l, v2, minD, maxD, 60.0D, maxLight, 3);
            if (pos2 != null && pos2.distanceToSqr(pos) > 16.0D) {
                spawn(p, l, st, v2, HorrorMode.STARE, pos2, 0, 1.0F);
            }
        }
        return true;
    }

    private static boolean peripheral(ServerPlayer p, ServerLevel l, PlayerState st) {
        boolean night = st.isNight(l);
        if (!canSpawnMore(p, l, night)) return false;
        HorrorVariant v = pick(p.getRandom(), W_PERIPH, st, night);
        Vec3 pos = findPeripheral(p, l, v, night ? 12 : 15);
        if (pos == null) return false;
        spawn(p, l, st, v, HorrorMode.PERIPHERAL, pos, 0, 1.0F);
        return true;
    }

    private static boolean peek(ServerPlayer p, ServerLevel l, PlayerState st) {
        boolean night = st.isNight(l);
        // PEEK tambien puede ocurrir de dia, pero solo una cabeza y con cupo reducido.
        if (!canSpawnPeek(p, l, night)) return false;
        HorrorVariant v = pick(p.getRandom(), W_PEEK, st, night);
        double minD = night ? Math.max(4.5D, minDist(st, true) * 0.42D) : 4.0D;
        double maxD = night ? 15.0D : 11.0D;
        CoverSpot spot = findSpot(p, l, v, minD, maxD);
        if (spot == null && HorrorUtil.forceDay) spot = openSpot(p, l, v);
        if (spot == null) return false;
        return spawnCoverPeek(p, l, st, v, spot) != null;
    }

    // ------------------------------------------------------------------ asomos con cobertura real

    private static CoverSpot findSpot(ServerPlayer p, ServerLevel l, HorrorVariant v, double minD, double maxD) {
        return CoverFinder.find(p, l, v, 1.0F, minD, maxD, 15, 160);
    }

    /** Sin cobertura (solo comandos de prueba): aparece a la vista y se inclina. */
    private static CoverSpot openSpot(ServerPlayer p, ServerLevel l, HorrorVariant v) {
        Vec3 pos = forcedVisiblePosition(p, l, v);
        if (pos == null) return null;
        Vec3 d = new Vec3(p.getX() - pos.x, 0.0D, p.getZ() - pos.z);
        if (d.lengthSqr() < 1.0E-4D) d = new Vec3(0.0D, 0.0D, 1.0D);
        d = d.normalize();
        return CoverSpot.open(pos, new Vec3(-d.z, 0.0D, d.x));
    }

    /** Crea la presencia escondida tras su cobertura; ella sola se asoma, se esconde y se recoloca. */
    static HorrorEntity spawnCoverPeek(ServerPlayer p, ServerLevel l, PlayerState st, HorrorVariant v, CoverSpot spot) {
        RandomSource r = p.getRandom();
        HorrorEntity e = spawn(p, l, st, v, HorrorMode.PEEK, spot.shown, 0, spot.scale);
        if (e == null) return null;
        int cycles = (st.isNight(l) ? r.nextInt(3) : r.nextInt(2)) + (v == HorrorVariant.STALKER ? 2 : 0);
        e.setPeekCover(spot.lean, spot.hide, spot.grip, 0.75F + r.nextFloat() * 0.45F, spot.gaze, cycles);
        return e;
    }

    private static boolean canSpawnPeek(ServerPlayer p, ServerLevel l, boolean night) {
        if (!HorrorConfig.ENABLED.get()) return false;
        int limit = night ? entityCap() : 1;
        return active(p, l) < limit;
    }

    private static boolean door(ServerPlayer p, ServerLevel l, PlayerState st) {
        // Las puertas escogidas quedan cerca: la mano nunca tiene mas de siete bloques de alcance.
        boolean forced = HorrorUtil.forceDay;
        BlockPos door = forced
                ? findNearestDoor(p, l, 7, 4, 3.0D)
                : findBlock(p, l, p.blockPosition(), 7, 4,
                    state -> state.getBlock() instanceof DoorBlock
                            && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER, 3.0D);
        RandomSource r = p.getRandom();
        boolean night = st.isNight(l);
        if (door == null) {
            // Sin una puerta real, los golpes y el traqueteo se oyen en una pared cercana.
            // En el comando de prueba se intenta ademas el asomo de esquina.
            if (forced) return forcedCornerPeek(p, l, st);
            return doorRattle(p, l, st);
        }
        Vec3 dp = Vec3.atCenterOf(door);
        BlockState doorState = l.getBlockState(door);
        boolean open = doorState.getValue(DoorBlock.OPEN);

        if (forced) {
            HorrorVariant variant = pick(r, W_DOOR, st, night);
            if (!open) setDoorOpen(l, door, true);
            return triggerDoorPeek(p, l, st, variant, door, true, true);
        }

        if (canSpawnMore(p, l, night) && r.nextInt(100) < 42) {
            HorrorVariant variant = pick(r, W_DOOR, st, night);
            if (open) {
                triggerDoorPeek(p, l, st, variant, door, false, false);
                return true;
            }
            // Primero se oyen golpes; despues la puerta se abre sola y algo asoma la silueta.
            int delay = 35 + r.nextInt(65);
            st.schedule(delay, (pl, lv) -> {
                BlockState lower = lv.getBlockState(door);
                if (!(lower.getBlock() instanceof DoorBlock)
                        || lower.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) return;
                setDoorOpen(lv, door, true);
                triggerDoorPeek(pl, lv, st, variant, door, false, false);
            });
        }

        // Golpes irregulares y crujido: a veces la puerta se queda cerrada.
        int n = 2 + r.nextInt(4);
        int t = 1;
        for (int k = 0; k < n; k++) {
            final float pitch = 0.72F + r.nextFloat() * 0.34F;
            st.schedule(t, (pl, lv) ->
                    HorrorUtil.sound(pl, ModSounds.KNOCK.get(), SoundSource.HOSTILE, dp, 1.25F, pitch));
            t += 7 + r.nextInt(17);
        }
        if (r.nextInt(100) < 55) {
            st.schedule(t + 18 + r.nextInt(35), (pl, lv) ->
                    HorrorUtil.sound(pl, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, dp, 0.9F, 0.72F));
        }
        return true;
    }

    private static boolean triggerDoorPeek(ServerPlayer p, ServerLevel l, PlayerState st,
                                           HorrorVariant variant, BlockPos door, boolean allowDayTest, boolean forced) {
        if (!p.isAlive()) return false;
        if (!allowDayTest && !canSpawnMore(p, l, st.isNight(l))) return false;
        HorrorUtil.sound(p, ModSounds.CREAK.get(), SoundSource.BLOCKS,
                Vec3.atCenterOf(door), 1.0F, 0.68F + p.getRandom().nextFloat() * 0.12F);
        CoverSpot spot = doorSpot(p, l, variant, door);
        if (spot == null && forced) spot = openSpot(p, l, variant);
        if (spot == null) return false;
        HorrorEntity entity = spawnCoverPeek(p, l, st, variant, spot);
        if (entity == null) return false;

        // El comando garantiza tambien el brazo; en el juego normal sigue siendo una variante rara.
        // El brazo sale solo cuando la figura ya esta del todo asomada en el hueco de la puerta.
        if (forced || p.getRandom().nextInt(100) < 58) {
            entity.queueHandAttack(forced ? 26 : 40, forced ? 70 : 92);
        }
        return true;
    }

    /** Escala con la que la cabeza queda a la altura de un hueco de puerta de dos bloques. */
    private static float doorScale(HorrorVariant v) {
        return Mth.clamp(1.66F / v.headCenterY(1.0F), 0.5F, 1.0F);
    }

    /**
     * La presencia esta de pie en el hueco de la puerta, del lado contrario al jugador, y sale desde
     * detras del marco (dentro de la pared) hasta quedar enmarcada, con la cabeza ladeada.
     */
    private static CoverSpot doorSpot(ServerPlayer p, ServerLevel l, HorrorVariant v, BlockPos door) {
        BlockState ds = l.getBlockState(door);
        if (!(ds.getBlock() instanceof DoorBlock) || ds.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) return null;
        Direction facing = ds.getValue(DoorBlock.FACING);
        Vec3 axis = new Vec3(facing.getStepX(), 0.0D, facing.getStepZ());
        Vec3 center = new Vec3(door.getX() + 0.5D, door.getY(), door.getZ() + 0.5D);
        double side = Math.signum(axis.dot(new Vec3(p.getX() - center.x, 0.0D, p.getZ() - center.z)));
        if (side == 0.0D) side = 1.0D;
        RandomSource r = p.getRandom();
        float scale = doorScale(v);
        double h = v.height * scale;
        double w = h * v.aspect;
        double headHalf = Math.max(0.12D, v.headWidth(scale) * 0.3D);
        float[] off = PeekPose.headOffset(v, scale, PeekPose.leanRad(1.0F, 1.0F), PeekPose.tiltRad(1.0F, 1.0F, 1.0F));
        Vec3 eye = p.getEyePosition();
        Vec3 base = center.add(axis.scale(-side * 0.28D));
        Vec3 t = new Vec3(-axis.z, 0.0D, axis.x);
        boolean first = r.nextBoolean();
        for (int i = 0; i < 2; i++) {
            double sideSign = (i == 0) == first ? 1.0D : -1.0D;
            double hide = 0.95D + r.nextDouble() * 0.25D;
            Vec3 lean = t.scale(-sideSign);
            Vec3 hidden = base.add(t.scale(sideSign * hide));
            if (HorrorUtil.visibleSamples(l, eye, p, hidden, 0.0D, h, w) > 1) continue;
            Vec3 headShown = base.add(lean.scale(off[0])).add(0.0D, off[1], 0.0D);
            if (HorrorUtil.headSamples(l, eye, p, headShown, headHalf) < 1) continue;
            double gy = Math.max(base.y + 0.6D, base.y + off[1] - 0.55D * scale);
            Vec3 grip = new Vec3(center.x + axis.x * side * 0.46D + t.x * sideSign * 0.5D, gy,
                    center.z + axis.z * side * 0.46D + t.z * sideSign * 0.5D);
            return new CoverSpot(base, lean, hide, grip, scale, true);
        }
        // Sin pared a los lados: aparece directamente en el hueco.
        Vec3 head = base.add(0.0D, v.headCenterY(scale), 0.0D);
        if (HorrorUtil.headSamples(l, eye, p, head, headHalf) < 1) return null;
        return new CoverSpot(base, t, 0.0D, null, scale, true);
    }

    private static BlockPos findNearestDoor(ServerPlayer p, ServerLevel l, int radius, int vertical, double minDist) {
        BlockPos origin = p.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -vertical; y <= vertical; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos candidate = origin.offset(x, y, z);
                    if (!l.hasChunkAt(candidate)) continue;
                    BlockState state = l.getBlockState(candidate);
                    if (!(state.getBlock() instanceof DoorBlock)
                            || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) continue;
                    double d2 = Vec3.atCenterOf(candidate).distanceToSqr(p.position());
                    if (d2 < minDist * minDist || d2 > radius * radius + vertical * vertical) continue;
                    if (d2 < bestDistance) { best = candidate.immutable(); bestDistance = d2; }
                }
            }
        }
        return best;
    }

    private static void setDoorOpen(ServerLevel l, BlockPos door, boolean open) {
        BlockState lower = l.getBlockState(door);
        if (!(lower.getBlock() instanceof DoorBlock)) return;
        l.setBlock(door, lower.setValue(DoorBlock.OPEN, open), 3);
        BlockPos upperPos = door.above();
        BlockState upper = l.getBlockState(upperPos);
        if (upper.getBlock() instanceof DoorBlock && upper.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            l.setBlock(upperPos, upper.setValue(DoorBlock.OPEN, open), 3);
        }
    }

    private static Vec3 forcedPositionBeside(ServerPlayer p, ServerLevel l, BlockPos anchor) {
        Vec3 fromAnchor = new Vec3(p.getX() - (anchor.getX() + 0.5D), 0.0D,
                p.getZ() - (anchor.getZ() + 0.5D));
        if (fromAnchor.lengthSqr() < 1.0E-4D) fromAnchor = horizontalLook(p).scale(-1.0D);
        fromAnchor = fromAnchor.normalize();
        Vec3 right = new Vec3(-fromAnchor.z, 0.0D, fromAnchor.x);
        for (Vec3 side : new Vec3[]{right, right.scale(-1.0D), fromAnchor.scale(-0.7D), fromAnchor.scale(0.7D)}) {
            double x = anchor.getX() + 0.5D + side.x * 0.85D;
            double z = anchor.getZ() + 0.5D + side.z * 0.85D;
            Optional<BlockPos> ground = HorrorUtil.ground(l, Mth.floor(x), Mth.floor(z), anchor.getY(), 3, 4);
            if (ground.isEmpty()) continue;
            Vec3 feet = new Vec3(ground.get().getX() + 0.5D, ground.get().getY(), ground.get().getZ() + 0.5D);
            double d = feet.distanceTo(p.position());
            if (d >= 2.6D && d <= 7.0D) return feet;
        }
        return null;
    }

    private static boolean forcedCornerPeek(ServerPlayer p, ServerLevel l, PlayerState st) {
        HorrorVariant variant = pick(p.getRandom(), W_DOOR, st, st.isNight(l));
        CoverSpot spot = findSpot(p, l, variant, 5.0D, 9.0D);
        if (spot == null) spot = openSpot(p, l, variant);
        if (spot == null) return false;
        HorrorUtil.sound(p, ModSounds.KNOCK.get(), SoundSource.HOSTILE, spot.shown, 1.1F, 0.72F);
        HorrorEntity e = spawnCoverPeek(p, l, st, variant, spot);
        if (e == null) return false;
        e.queueHandAttack(26, 70);
        return true;
    }

    private static boolean forcedHeadPeek(ServerPlayer p, ServerLevel l, PlayerState st) {
        HorrorVariant variant = pick(p.getRandom(), W_PEEK, st, st.isNight(l));
        CoverSpot spot = findSpot(p, l, variant, 3.0D, 13.0D);
        if (spot == null) spot = openSpot(p, l, variant);
        return spot != null && spawnCoverPeek(p, l, st, variant, spot) != null;
    }

    private static Vec3 forcedVisiblePosition(ServerPlayer p, ServerLevel l, HorrorVariant v) {
        Vec3 forward = horizontalLook(p);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        for (double d : new double[]{7.0D, 9.0D, 11.0D, 6.0D, 5.0D}) {
            for (Vec3 dir : new Vec3[]{forward, right, right.scale(-1.0D)}) {
                Optional<BlockPos> ground = HorrorUtil.ground(l, Mth.floor(p.getX() + dir.x * d),
                        Mth.floor(p.getZ() + dir.z * d), p.getBlockY(), 5, 12);
                if (ground.isEmpty()) continue;
                Vec3 feet = new Vec3(ground.get().getX() + 0.5D, ground.get().getY(), ground.get().getZ() + 0.5D);
                if (HorrorUtil.visibleSamples(l, p.getEyePosition(), p, feet,
                        v.hover, v.height, v.height * v.aspect) > 0) return feet;
            }
        }
        return null;
    }

    /** Evento de cama: la presencia se asoma por debajo/junto a una cama cercana. */
    public static boolean bed(ServerPlayer p, ServerLevel l, PlayerState st, BlockPos center, int radius, double minDist) {
        boolean night = st.isNight(l);
        if (!canSpawnMore(p, l, night)) return false;
        BlockPos bed = findBlock(p, l, center, radius, 3, s -> s.getBlock() instanceof BedBlock, minDist);
        if (bed == null) return false;
        RandomSource r = p.getRandom();
        HorrorVariant v = pick(r, W_BED, st, night);
        Vec3 eye = p.getEyePosition();
        List<Direction> dirs = new ArrayList<>(List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST));
        Collections.shuffle(dirs, new java.util.Random(r.nextLong()));
        float scale = 0.5F;
        for (Direction d : dirs) {
            BlockPos n = bed.relative(d);
            if (!HorrorUtil.standable(l, n)) continue;
            Vec3 feet = new Vec3(n.getX() + 0.5D - d.getStepX() * 0.42D, n.getY(), n.getZ() + 0.5D - d.getStepZ() * 0.42D);
            double hd = Math.sqrt((feet.x - p.getX()) * (feet.x - p.getX()) + (feet.z - p.getZ()) * (feet.z - p.getZ()));
            if (hd < minDist) continue;
            if (HorrorUtil.visibleSamples(l, eye, p, feet, 0.0D, v.height * scale, v.height * scale * v.aspect) < 1) continue;
            spawn(p, l, st, v, HorrorMode.LOW, feet, 0, scale);
            return true;
        }
        return false;
    }

    private static boolean cave(ServerPlayer p, ServerLevel l, PlayerState st) {
        if (!st.isUnderground(p, l)) return false;
        boolean night = st.isNight(l);
        if (!canSpawnMore(p, l, true)) return false;
        HorrorVariant v = pick(p.getRandom(), W_CAVE, st, night);
        Vec3 pos = findVisible(p, l, v, 12.0D, 32.0D, 50.0D, 3, 3);
        if (pos != null) {
            spawn(p, l, st, v, HorrorMode.STARE, pos, 0, 1.0F);
            return true;
        }
        CoverSpot spot = CoverFinder.find(p, l, v, 1.0F, 6.0D, 18.0D, 5, 160);
        if (spot == null) return false;
        return spawnCoverPeek(p, l, st, v, spot) != null;
    }

    /** Escalada nocturna: tras apartar la vista, reaparece un poco mas cerca (nunca bajo 8.5 bloques, nunca persigue). */
    static void scheduleChain(PlayerState st, ServerPlayer p, HorrorEntity e) {
        double dx = e.getX() - p.getX();
        double dz = e.getZ() - p.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist <= 10.5D) return;
        final double newDist = Math.max(8.5D, dist * 0.8D);
        final double bearing = Math.atan2(dz, dx);
        final HorrorVariant v = e.getVariant();
        final int left = e.getChainLeft() - 1;
        RandomSource r = p.getRandom();
        st.schedule(40 + r.nextInt(100), (pl, lv) -> {
            if (!st.isNight(lv) || !canSpawnMore(pl, lv, true)) return;
            RandomSource rr = pl.getRandom();
            Vec3 eye = pl.getEyePosition();
            for (int i = 0; i < 20; i++) {
                double a = bearing + Math.toRadians((rr.nextDouble() * 2.0D - 1.0D) * 12.0D);
                double d = newDist + rr.nextDouble() * 2.0D;
                Optional<BlockPos> g = HorrorUtil.ground(lv, Mth.floor(pl.getX() + Math.cos(a) * d),
                        Mth.floor(pl.getZ() + Math.sin(a) * d), pl.getBlockY(), 5, 12);
                if (g.isEmpty()) continue;
                Vec3 feet = new Vec3(g.get().getX() + 0.5D, g.get().getY(), g.get().getZ() + 0.5D);
                if (HorrorUtil.visibleSamples(lv, eye, pl, feet, v.hover, v.height, v.height * v.aspect) >= 3) {
                    spawn(pl, lv, st, v, HorrorMode.STARE, feet, left, 1.0F);
                    return;
                }
            }
        });
    }

    // ------------------------------------------------------------------ eventos sin entidad (falsas presencias)

    private enum Fake { WHISPER, BREATH, KNOCK_WALL, DRONE, STATIC, CRACK_ALONE, SCRAPE, HEARTBEAT, CREAK, DISTANT }

    private static Vec3 around(ServerPlayer p, double lo, double hi, boolean behind) {
        RandomSource r = p.getRandom();
        Vec3 look = p.getLookAngle();
        double base = Math.atan2(look.z, look.x);
        double a = behind ? base + Math.PI + (r.nextDouble() * 2.0D - 1.0D) : r.nextDouble() * Math.PI * 2.0D;
        double d = lo + r.nextDouble() * (hi - lo);
        return new Vec3(p.getX() + Math.cos(a) * d, p.getEyeY() - 0.6D + r.nextDouble() * 0.8D, p.getZ() + Math.sin(a) * d);
    }

    private static boolean fakeSound(ServerPlayer p, ServerLevel l, PlayerState st) {
        RandomSource r = p.getRandom();
        boolean night = st.isNight(l);
        EnumMap<Fake, Double> w = new EnumMap<>(Fake.class);
        w.put(Fake.WHISPER, night ? 3.0D : 2.0D);
        w.put(Fake.BREATH, night ? 2.5D : 1.2D);
        w.put(Fake.KNOCK_WALL, 2.0D);
        w.put(Fake.DRONE, 1.2D);
        w.put(Fake.STATIC, 0.8D);
        w.put(Fake.CRACK_ALONE, 1.5D);
        w.put(Fake.SCRAPE, 1.3D);
        w.put(Fake.HEARTBEAT, 0.6D + st.fear);
        w.put(Fake.CREAK, 1.2D);
        w.put(Fake.DISTANT, night ? 1.8D : 0.55D);
        for (int idx : st.recentSounds) w.computeIfPresent(Fake.values()[idx], (k, v) -> v * 0.25D);
        Fake f = HorrorUtil.pickWeighted(r, w);
        st.recentSounds.addLast(f.ordinal());
        while (st.recentSounds.size() > 3) st.recentSounds.removeFirst();

        float pitch = 0.85F + r.nextFloat() * 0.3F;
        switch (f) {
            case WHISPER -> HorrorUtil.sound(p, r.nextInt(100) < 35 ? ModSounds.TAPE_VOICE.get() : ModSounds.WHISPER.get(), SoundSource.HOSTILE,
                    around(p, night ? 1.5D : 2.5D, night ? 5.0D : 7.0D, r.nextBoolean()), 0.6F + r.nextFloat() * 0.4F, pitch);
            case BREATH -> HorrorUtil.sound(p, ModSounds.BREATH.get(), SoundSource.HOSTILE,
                    around(p, 1.2D, 2.8D, true), 0.55F + r.nextFloat() * 0.3F, pitch);
            case KNOCK_WALL -> {
                Vec3 pos = around(p, 2.5D, 6.0D, false);
                int n = 1 + r.nextInt(4);
                int t = 1;
                for (int i = 0; i < n; i++) {
                    final float kp = 0.8F + r.nextFloat() * 0.3F;
                    st.schedule(t, (pl, lv) -> HorrorUtil.sound(pl, ModSounds.KNOCK.get(), SoundSource.HOSTILE, pos, 1.2F, kp));
                    t += 8 + r.nextInt(14);
                }
            }
            case DRONE -> HorrorUtil.sound(p, ModSounds.DRONE.get(), SoundSource.AMBIENT,
                    around(p, 2.0D, 4.0D, false), 0.5F, 0.9F + r.nextFloat() * 0.2F);
            case STATIC -> HorrorUtil.sound(p, ModSounds.STATIC.get(), SoundSource.AMBIENT,
                    around(p, 3.0D, 8.0D, false), 0.35F + r.nextFloat() * 0.2F, 0.9F + r.nextFloat() * 0.2F);
            case CRACK_ALONE -> HorrorUtil.sound(p, ModSounds.CRACK.get(), SoundSource.HOSTILE,
                    around(p, 12.0D, 24.0D, r.nextBoolean()), 1.4F, 0.8F + r.nextFloat() * 0.25F);
            case SCRAPE -> HorrorUtil.sound(p, ModSounds.SCRAPE.get(), SoundSource.HOSTILE,
                    around(p, 4.0D, 9.0D, false), 0.8F, pitch);
            case HEARTBEAT -> HorrorUtil.sound(p, ModSounds.HEARTBEAT.get(), SoundSource.AMBIENT,
                    p.getEyePosition(), 0.55F, 1.0F);
            case CREAK -> HorrorUtil.sound(p, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS,
                    around(p, 6.0D, 12.0D, false), 0.9F, 0.75F + r.nextFloat() * 0.2F);
            case DISTANT -> HorrorUtil.sound(p, ModSounds.DISTANT_SOUND.get(), SoundSource.AMBIENT,
                    around(p, 10.0D, 22.0D, true), 0.55F + r.nextFloat() * 0.2F, 0.82F + r.nextFloat() * 0.25F);
            default -> { }
        }
        return true;
    }

    private static boolean footsteps(ServerPlayer p, ServerLevel l, PlayerState st) {
        if (!p.onGround() || p.isInWaterOrBubble() || p.isSleeping()) return false;
        RandomSource r = p.getRandom();
        Vec3 hl = horizontalLook(p);
        final Vec3 back = rotate(hl.scale(-1.0D), Math.toRadians((r.nextDouble() * 2.0D - 1.0D) * 40.0D));
        int steps = 7 + r.nextInt(8);
        final double start = 10.0D;
        final double end = 2.5D + r.nextDouble() * 1.5D;
        int t = 1;
        for (int k = 0; k < steps; k++) {
            final double dist = HorrorUtil.lerp(start, end, k / (double) (steps - 1));
            final float pitch = 0.85F + r.nextFloat() * 0.15F;
            st.schedule(t, (pl, lv) -> {
                Vec3 pos = new Vec3(pl.getX() + back.x * dist, pl.getY(), pl.getZ() + back.z * dist);
                BlockState below = lv.getBlockState(BlockPos.containing(pos).below());
                if (below.isAir()) below = lv.getBlockState(pl.blockPosition().below());
                HorrorUtil.sound(pl, below.getSoundType().getStepSound(), SoundSource.HOSTILE, pos, 0.9F, pitch);
            });
            t += 8 + r.nextInt(5);
        }
        if (r.nextBoolean()) { // se detiene justo detras de ti
            st.schedule(t + 25, (pl, lv) -> HorrorUtil.sound(pl, ModSounds.BREATH.get(), SoundSource.HOSTILE,
                    new Vec3(pl.getX() + back.x * end, pl.getEyeY() - 0.4D, pl.getZ() + back.z * end), 0.6F, 0.9F));
        }
        return true;
    }

    private static boolean silence(ServerPlayer p, ServerLevel l, PlayerState st) {
        RandomSource r = p.getRandom();
        int dur = 200 + r.nextInt(300);
        st.silenceUntil = l.getGameTime() + dur;
        p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));
        p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.AMBIENT));
        // Tras el silencio, algo ocurre: o no. Es impredecible.
        int roll = r.nextInt(100);
        if (roll < 55) {
            st.schedule(dur + 20 + r.nextInt(60), (pl, lv) -> fakeSound(pl, lv, st));
        } else if (roll < 80) {
            st.schedule(dur + 20 + r.nextInt(60), (pl, lv) -> {
                if (!peripheral(pl, lv, st)) peek(pl, lv, st);
            });
        }
        return true;
    }

    // ------------------------------------------------------------------ asomos de cabeza desde los bordes y bloques

    /** [desplazamiento vertical, alto, ancho] de la cabeza visible, no del cuerpo oculto. */
    /**
     * Ventana: la presencia esta fuera, pegada al cristal, y sale desde detras de la pared junto al
     * marco hasta que parte de su rostro queda enmarcada en la ventana, con la cabeza ladeada.
     */
    private static CoverSpot windowSpot(ServerPlayer p, ServerLevel l, HorrorVariant v, BlockPos glass) {
        RandomSource r = p.getRandom();
        Vec3 pc = p.position();
        Vec3 eye = p.getEyePosition();
        float scale = 1.0F;
        double h = v.height * scale;
        double w = h * v.aspect;
        double headHalf = Math.max(0.12D, v.headWidth(scale) * 0.3D);
        float[] off = PeekPose.headOffset(v, scale, PeekPose.leanRad(1.0F, 1.0F), PeekPose.tiltRad(1.0F, 1.0F, 1.0F));
        Vec3 gc = Vec3.atCenterOf(glass);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos out = glass.relative(d);
            BlockPos in = glass.relative(d.getOpposite());
            if (!l.getBlockState(out).getCollisionShape(l, out).isEmpty()) continue;
            if (!l.getBlockState(in).getCollisionShape(l, in).isEmpty()) continue;
            if (Vec3.atCenterOf(in).distanceToSqr(pc) > Vec3.atCenterOf(out).distanceToSqr(pc)) continue; // el jugador esta dentro
            Vec3 outward = new Vec3(d.getStepX(), 0.0D, d.getStepZ());
            Vec3 t = new Vec3(-outward.z, 0.0D, outward.x);
            double feetY = glass.getY() + 0.55D - v.headCenterY(scale);
            Vec3 base = new Vec3(out.getX() + 0.5D - d.getStepX() * 0.40D, feetY, out.getZ() + 0.5D - d.getStepZ() * 0.40D);
            boolean first = r.nextBoolean();
            for (int i = 0; i < 2; i++) {
                double sideSign = (i == 0) == first ? 1.0D : -1.0D;
                double inFrame = 0.12D + r.nextDouble() * 0.26D; // cuanto se aleja del centro de la ventana
                double hide = 1.3D;
                Vec3 shown = base.add(t.scale(sideSign * inFrame));
                Vec3 hidden = shown.add(t.scale(sideSign * hide));
                Vec3 lean = t.scale(-sideSign);
                if (HorrorUtil.visibleSamples(l, eye, p, hidden, 0.0D, h, w) > 1) continue;
                Vec3 headShown = shown.add(lean.scale(off[0])).add(0.0D, off[1], 0.0D);
                if (HorrorUtil.headSamples(l, eye, p, headShown, headHalf) < 1) continue;
                Vec3 grip = new Vec3(gc.x + outward.x * 0.46D + t.x * sideSign * 0.5D, gc.y - 0.05D,
                        gc.z + outward.z * 0.46D + t.z * sideSign * 0.5D);
                return new CoverSpot(shown, lean, hide, grip, scale, true);
            }
        }
        return null;
    }

    /** Ventana de cristal: algo se asoma desde detras del marco, al otro lado del vidrio. */
    private static boolean window(ServerPlayer p, ServerLevel l, PlayerState st) {
        boolean night = st.isNight(l);
        if (!canSpawnMore(p, l, night)) return false;
        BlockPos glass = findBlock(p, l, p.blockPosition(), 12, 3,
                s -> s.getBlock() instanceof AbstractGlassBlock
                        || (s.getBlock() instanceof IronBarsBlock && !s.is(Blocks.IRON_BARS)), 3.0D);
        if (glass == null) return false;
        HorrorVariant v = pick(p.getRandom(), W_PEEK, st, night);
        CoverSpot spot = windowSpot(p, l, v, glass);
        return spot != null && spawnCoverPeek(p, l, st, v, spot) != null;
    }

    // ------------------------------------------------------------------ nuevos sucesos analogicos y de acecho

    /** Golpes secos que parecen venir de un bloque concreto; nunca rompe el bloque. */
    private static boolean wallTaps(ServerPlayer p, ServerLevel l, PlayerState st) {
        RandomSource r = p.getRandom();
        BlockPos wall = findBlock(p, l, p.blockPosition(), 7, 3,
                state -> !state.isAir() && state.getFluidState().isEmpty(), 2.0D);
        Vec3 pos = wall != null ? Vec3.atCenterOf(wall) : around(p, 3.0D, 6.0D, r.nextBoolean());
        int count = 2 + r.nextInt(4);
        int tick = 1;
        for (int i = 0; i < count; i++) {
            final float pitch = 0.62F + r.nextFloat() * 0.55F;
            final float volume = i == count - 1 ? 1.35F : 0.9F;
            st.schedule(tick, (pl, lv) -> HorrorUtil.sound(pl, ModSounds.KNOCK.get(),
                    SoundSource.HOSTILE, pos, volume, pitch));
            tick += 7 + r.nextInt(18);
        }
        if (r.nextInt(100) < 38) {
            st.schedule(tick + 25, (pl, lv) -> HorrorUtil.sound(pl, ModSounds.SCRAPE.get(),
                    SoundSource.HOSTILE, pos, 0.62F, 0.7F + pl.getRandom().nextFloat() * 0.2F));
        }
        return true;
    }

    /** Una manija parece moverse: cruje y golpea, pero no modifica ni desbloquea la puerta. */
    private static boolean doorRattle(ServerPlayer p, ServerLevel l, PlayerState st) {
        RandomSource r = p.getRandom();
        BlockPos door = findNearestDoor(p, l, 9, 4, 2.0D);
        Vec3 pos = door != null ? Vec3.atCenterOf(door) : around(p, 3.0D, 6.0D, false);
        int tick = 1;
        int rattles = 2 + r.nextInt(3);
        for (int i = 0; i < rattles; i++) {
            final float pitch = 0.68F + r.nextFloat() * 0.25F;
            st.schedule(tick, (pl, lv) -> HorrorUtil.sound(pl,
                    r.nextBoolean() ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE,
                    SoundSource.BLOCKS, pos, 0.9F, pitch));
            tick += 9 + r.nextInt(16);
        }
        st.schedule(tick + 18, (pl, lv) -> HorrorUtil.sound(pl, ModSounds.KNOCK.get(),
                SoundSource.HOSTILE, pos, 1.05F, 0.72F));
        return true;
    }

    /** Arañazos sobre cristal; en ocasiones terminan con una cabeza al otro lado. */
    private static boolean glassScratch(ServerPlayer p, ServerLevel l, PlayerState st) {
        RandomSource r = p.getRandom();
        final boolean forcedTest = HorrorUtil.forceDay;
        BlockPos glass = findBlock(p, l, p.blockPosition(), 12, 4,
                state -> state.getBlock() instanceof AbstractGlassBlock
                        || state.getBlock() instanceof IronBarsBlock, 2.0D);
        if (glass == null) {
            Vec3 fallback = around(p, 3.0D, 7.0D, false);
            HorrorUtil.sound(p, ModSounds.SCRAPE.get(), SoundSource.HOSTILE, fallback, 0.85F, 0.78F);
            st.schedule(18, (pl, lv) -> HorrorUtil.sound(pl, ModSounds.KNOCK.get(),
                    SoundSource.HOSTILE, fallback, 0.75F, 0.7F));
            return true;
        }
        Vec3 pos = Vec3.atCenterOf(glass);
        int count = 2 + r.nextInt(3);
        for (int i = 0; i < count; i++) {
            final int index = i;
            st.schedule(2 + i * (10 + r.nextInt(7)), (pl, lv) ->
                    HorrorUtil.sound(pl, ModSounds.SCRAPE.get(), SoundSource.HOSTILE, pos,
                            0.58F + index * 0.09F, 0.65F + pl.getRandom().nextFloat() * 0.25F));
        }
        if (r.nextInt(100) < 42 || forcedTest) {
            st.schedule(48 + r.nextInt(32), (pl, lv) -> {
                int limit = st.isNight(lv) ? entityCap() : 1;
                if (!pl.isAlive() || active(pl, lv) >= limit) return;
                HorrorVariant v = pick(pl.getRandom(), W_PEEK, st, st.isNight(lv));
                CoverSpot spot = windowSpot(pl, lv, v, glass);
                if (spot == null) spot = findSpot(pl, lv, v, 3.0D, 12.0D);
                if (spot != null) spawnCoverPeek(pl, lv, st, v, spot);
            });
        }
        return true;
    }

    /** Pasos sordos por encima del jugador, como si algo recorriera el techo. */
    private static boolean ceilingSteps(ServerPlayer p, ServerLevel l, PlayerState st) {
        RandomSource r = p.getRandom();
        BlockPos ceiling = null;
        double best = Double.MAX_VALUE;
        BlockPos origin = p.blockPosition();
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            for (int y = 3; y <= 8; y++) {
                BlockPos candidate = origin.offset(x, y, z);
                if (!l.hasChunkAt(candidate)) continue;
                BlockState state = l.getBlockState(candidate);
                if (state.isAir() || state.getCollisionShape(l, candidate).isEmpty()) continue;
                double d = candidate.distSqr(origin);
                if (d < best) { best = d; ceiling = candidate.immutable(); }
            }
        }
        Vec3 center = ceiling == null
                ? new Vec3(p.getX(), p.getEyeY() + 1.5D, p.getZ())
                : Vec3.atCenterOf(ceiling);
        net.minecraft.sounds.SoundEvent step = ceiling == null
                ? SoundEvents.STONE_STEP : l.getBlockState(ceiling).getSoundType().getStepSound();
        int tick = 1;
        int count = 3 + r.nextInt(4);
        for (int i = 0; i < count; i++) {
            final Vec3 pos = center.add((r.nextDouble() - 0.5D) * 2.5D, 0.0D,
                    (r.nextDouble() - 0.5D) * 2.5D);
            final float pitch = 0.58F + r.nextFloat() * 0.38F;
            st.schedule(tick, (pl, lv) -> HorrorUtil.sound(pl, step, SoundSource.HOSTILE, pos, 0.7F, pitch));
            tick += 8 + r.nextInt(12);
        }
        st.schedule(tick + 16, (pl, lv) -> HorrorUtil.sound(pl, ModSounds.SCRAPE.get(),
                SoundSource.HOSTILE, center, 0.55F, 0.68F));
        return true;
    }

    /** Fallo de cinta: baja la señal, corta el ambiente y deja una voz cuando vuelve. */
    private static boolean tapeCorruption(ServerPlayer p, ServerLevel l, PlayerState st) {
        RandomSource r = p.getRandom();
        int duration = 9 + r.nextInt(13);
        ModNetwork.send(p, ClientEffectPacket.STATIC, duration, 0, "");
        p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));
        HorrorUtil.sound(p, ModSounds.TAPE_STOP.get(), SoundSource.AMBIENT, p.position(), 0.48F, 0.82F);
        st.schedule(duration + 8, (pl, lv) -> HorrorUtil.sound(pl, ModSounds.TINNITUS.get(),
                SoundSource.AMBIENT, pl.getEyePosition(), 0.35F, 0.92F));
        st.schedule(duration + 35 + r.nextInt(35), (pl, lv) -> HorrorUtil.sound(pl,
                r.nextBoolean() ? ModSounds.TAPE_VOICE.get() : ModSounds.WHISPER.get(),
                SoundSource.HOSTILE, around(pl, 2.0D, 5.0D, true), 0.48F, 0.82F));
        return true;
    }

    /** Dos asomos separados por unos segundos, como si dos presencias se coordinaran. */
    private static boolean doublePeek(ServerPlayer p, ServerLevel l, PlayerState st) {
        boolean night = st.isNight(l);
        final boolean forcedTest = HorrorUtil.forceDay;
        int limit = night ? entityCap() : (forcedTest ? 2 : 1);
        if (!HorrorConfig.ENABLED.get() || active(p, l) >= limit) {
            return presenceCue(p, l, false);
        }
        RandomSource r = p.getRandom();
        HorrorVariant firstVariant = pick(r, W_PEEK, st, night);
        CoverSpot firstSpot = findSpot(p, l, firstVariant, 4.0D, night ? 17.0D : 12.0D);
        if (firstSpot == null && HorrorUtil.forceDay) firstSpot = openSpot(p, l, firstVariant);
        if (firstSpot == null) return presenceCue(p, l, false);
        final Vec3 firstPosition = firstSpot.shown;
        HorrorEntity first = spawnCoverPeek(p, l, st, firstVariant, firstSpot);
        if (first == null) return presenceCue(p, l, false);
        int delay = 76 + r.nextInt(55);
        st.schedule(delay, (pl, lv) -> {
            int currentLimit = st.isNight(lv) ? entityCap() : (forcedTest ? 2 : 1);
            if (!pl.isAlive()) return;
            if (active(pl, lv) >= currentLimit) {
                HorrorUtil.sound(pl, ModSounds.DISTANT_SOUND.get(), SoundSource.HOSTILE,
                        around(pl, 12.0D, 20.0D, false), 0.42F, 0.76F);
                return;
            }
            HorrorVariant secondVariant = pick(pl.getRandom(), W_PEEK, st, st.isNight(lv));
            CoverSpot secondSpot = findSpot(pl, lv, secondVariant, 4.0D, 16.0D);
            if (secondSpot != null && secondSpot.shown.distanceToSqr(firstPosition) > 25.0D) {
                spawnCoverPeek(pl, lv, st, secondVariant, secondSpot);
            } else {
                // Si no cabe una segunda entidad, el segundo asomo se sugiere solo con audio.
                HorrorUtil.sound(pl, ModSounds.WHISPER.get(), SoundSource.HOSTILE,
                        around(pl, 10.0D, 16.0D, false), 0.42F, 0.74F);
            }
        });
        return true;
    }

    /** Figura diminuta y lejana: no se acerca ni persigue, solo queda registrada en el horizonte. */
    private static boolean distantStare(ServerPlayer p, ServerLevel l, PlayerState st) {
        boolean night = st.isNight(l);
        if (!canSpawnMore(p, l, night)) return false;
        RandomSource r = p.getRandom();
        HorrorVariant v = pick(r, W_STARE, st, night);
        Vec3 pos = findVisible(p, l, v, 22.0D, 42.0D, 95.0D, night ? 8 : 12, 1);
        if (pos == null) {
            HorrorUtil.sound(p, ModSounds.DISTANT_SOUND.get(), SoundSource.HOSTILE,
                    around(p, 28.0D, 42.0D, true), 0.65F, 0.8F);
            return true;
        }
        return spawn(p, l, st, v, HorrorMode.STARE, pos, 0, 0.72F) != null;
    }

    /** Una respiración casi pegada al oído, seguida por una pausa larga. */
    private static boolean breathBehind(ServerPlayer p, ServerLevel l, PlayerState st) {
        RandomSource r = p.getRandom();
        Vec3 close = around(p, 0.75D, 1.35D, true);
        HorrorUtil.sound(p, ModSounds.BREATH.get(), SoundSource.HOSTILE, close,
                0.62F + r.nextFloat() * 0.25F, 0.76F + r.nextFloat() * 0.18F);
        if (r.nextInt(100) < 45) {
            st.schedule(34 + r.nextInt(38), (pl, lv) -> HorrorUtil.sound(pl,
                    ModSounds.WHISPER.get(), SoundSource.HOSTILE,
                    around(pl, 1.0D, 2.0D, true), 0.36F, 0.78F));
        }
        return true;
    }

    /** Pisadas que cruzan desde un lateral hacia la espalda y se detienen muy cerca. */
    private static boolean approachingSteps(ServerPlayer p, ServerLevel l, PlayerState st) {
        if (!p.onGround() || p.isInWaterOrBubble() || p.isSleeping()) return breathBehind(p, l, st);
        RandomSource r = p.getRandom();
        Vec3 look = horizontalLook(p);
        Vec3 back = rotate(look.scale(-1.0D), Math.toRadians(25.0D + r.nextDouble() * 35.0D));
        int count = 5 + r.nextInt(4);
        int tick = 1;
        for (int i = 0; i < count; i++) {
            final double distance = 8.5D - i * (6.6D / Math.max(1, count - 1));
            final double lateral = (r.nextDouble() - 0.5D) * 0.75D;
            final Vec3 side = new Vec3(-back.z, 0.0D, back.x);
            final float pitch = 0.72F + r.nextFloat() * 0.24F;
            st.schedule(tick, (pl, lv) -> {
                Vec3 pos = new Vec3(pl.getX() + back.x * distance + side.x * lateral,
                        pl.getY(), pl.getZ() + back.z * distance + side.z * lateral);
                BlockPos under = BlockPos.containing(pos).below();
                BlockState floor = lv.getBlockState(under);
                if (floor.isAir()) floor = lv.getBlockState(pl.blockPosition().below());
                HorrorUtil.sound(pl, floor.getSoundType().getStepSound(), SoundSource.HOSTILE, pos, 0.86F, pitch);
            });
            tick += 9 + r.nextInt(7);
        }
        st.schedule(tick + 22, (pl, lv) -> HorrorUtil.sound(pl, ModSounds.BREATH.get(),
                SoundSource.HOSTILE, around(pl, 0.8D, 1.5D, true), 0.55F, 0.8F));
        return true;
    }

    /** Un bloque parece romperse a pocos metros, pero el mundo no se modifica. */
    private static boolean falseBlockBreak(ServerPlayer p, ServerLevel l, PlayerState st) {
        BlockPos block = findBlock(p, l, p.blockPosition(), 9, 4,
                state -> !state.isAir() && state.getFluidState().isEmpty(),
                2.0D);
        Vec3 pos;
        net.minecraft.sounds.SoundEvent sound;
        if (block != null) {
            BlockState state = l.getBlockState(block);
            pos = Vec3.atCenterOf(block);
            sound = state.getSoundType().getBreakSound();
        } else {
            pos = around(p, 4.0D, 8.0D, p.getRandom().nextBoolean());
            sound = ModSounds.CRACK.get();
        }
        HorrorUtil.sound(p, sound, SoundSource.BLOCKS, pos, 0.86F,
                0.72F + p.getRandom().nextFloat() * 0.25F);
        if (p.getRandom().nextInt(100) < 35) {
            st.schedule(24 + p.getRandom().nextInt(24), (pl, lv) -> HorrorUtil.sound(pl,
                    ModSounds.SCRAPE.get(), SoundSource.HOSTILE,
                    around(pl, 3.0D, 6.0D, true), 0.5F, 0.76F));
        }
        return true;
    }

    /** Si el jugador llega a ver el primer rostro, otro aparece desde un lugar distinto. */
    private static boolean secondPresence(ServerPlayer p, ServerLevel l, PlayerState st) {
        final boolean forcedTest = HorrorUtil.forceDay;
        if (!canSpawnPeek(p, l, st.isNight(l))) return presenceCue(p, l, false);
        HorrorVariant v = pick(p.getRandom(), W_PEEK, st, st.isNight(l));
        CoverSpot spot = findSpot(p, l, v, 4.0D, 14.0D);
        if (spot == null && HorrorUtil.forceDay) spot = openSpot(p, l, v);
        if (spot == null) return presenceCue(p, l, false);
        HorrorEntity first = spawnCoverPeek(p, l, st, v, spot);
        if (first == null) return presenceCue(p, l, false);
        st.schedule(130 + p.getRandom().nextInt(70), (pl, lv) -> {
            if (!pl.isAlive() || first.isRemoved() || !first.wasSeen()) return;
            int limit = st.isNight(lv) ? entityCap() : (forcedTest ? 2 : 1);
            if (active(pl, lv) >= limit) {
                HorrorUtil.sound(pl, ModSounds.WHISPER.get(), SoundSource.HOSTILE,
                        around(pl, 8.0D, 14.0D, false), 0.40F, 0.78F);
                return;
            }
            HorrorVariant next = pick(pl.getRandom(), W_PEEK, st, st.isNight(lv));
            CoverSpot other = findSpot(pl, lv, next, 4.0D, 15.0D);
            if (other != null && other.shown.distanceToSqr(first.position()) >= 16.0D) {
                spawnCoverPeek(pl, lv, st, next, other);
            } else {
                HorrorUtil.sound(pl, ModSounds.DISTANT_SOUND.get(), SoundSource.HOSTILE,
                        around(pl, 11.0D, 18.0D, false), 0.40F, 0.72F);
            }
        });
        return true;
    }

    /** Una voz aparece a lo lejos y la misma frase parece repetirse desde la espalda. */
    private static boolean voiceEcho(ServerPlayer p, ServerLevel l, PlayerState st) {
        RandomSource r = p.getRandom();
        Vec3 first = around(p, 12.0D, 22.0D, false);
        HorrorUtil.sound(p, ModSounds.TAPE_VOICE.get(), SoundSource.HOSTILE, first,
                0.65F, 0.82F + r.nextFloat() * 0.14F);
        st.schedule(55 + r.nextInt(45), (pl, lv) -> HorrorUtil.sound(pl,
                ModSounds.WHISPER.get(), SoundSource.HOSTILE,
                around(pl, 1.5D, 3.0D, true), 0.42F, 0.72F + pl.getRandom().nextFloat() * 0.15F));
        return true;
    }

    // ------------------------------------------------------------------ persecucion

    /** Una presencia (cualquiera) congela tu mirada 5 s con un mensaje y despues te persigue sin parar. Sin dano. */
    private static boolean hunt(ServerPlayer p, ServerLevel l, PlayerState st) {
        if (!HorrorConfig.HUNT_ENABLED.get() || !HorrorUtil.entitiesAllowed(l)) return false;
        long now = l.getGameTime();
        if (!HorrorUtil.forceDay) {
            if (PlayerState.nights(p) < HorrorConfig.HUNT_MIN_NIGHT.get()) return false;
            if (now - st.lastHunt < HorrorConfig.HUNT_COOLDOWN_MIN.get() * 1200L) return false;
        }
        RandomSource r = p.getRandom();
        HorrorVariant[] all = HorrorVariant.values();
        HorrorVariant v = all[r.nextInt(all.length)];
        Vec3 pos = findVisible(p, l, v, 14.0D, 20.0D, 22.0D, 15, 5);
        if (pos == null) pos = findVisible(p, l, v, 12.0D, 24.0D, 50.0D, 15, 4);
        if (pos == null) pos = forcedVisiblePosition(p, l, v);
        if (pos == null) return false;
        for (HorrorEntity o : l.getEntitiesOfClass(HorrorEntity.class, p.getBoundingBox().inflate(96.0D),
                e -> p.getUUID().equals(e.getOwnerId()))) {
            o.discard(); // un solo cazador
        }
        HorrorEntity e = spawn(p, l, st, v, HorrorMode.STARE, pos, 0, 1.0F);
        if (e == null) return false;
        e.beginHunt(p, 100);
        st.lastHunt = now;
        ModNetwork.send(p, ClientEffectPacket.LOCK, e.getId(), 100, "NO ESCAPAR\u00c1S NUNCA M\u00c1S DE M\u00cd");
        p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));
        p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.AMBIENT));
        return true;
    }

    // ------------------------------------------------------------------ El Otro (jefe final)

    /** Crea a El Otro en 'pos' y empieza el combate para 'p'. */
    public static OtherEntity spawnOther(ServerPlayer p, ServerLevel l, Vec3 pos) {
        OtherEntity e = ModEntities.OTHER.get().create(l);
        if (e == null) return null;
        float yaw = (float) (Mth.atan2(p.getZ() - pos.z, p.getX() - pos.x) * (180.0D / Math.PI)) - 90.0F;
        e.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        e.startFight(p);
        l.addFreshEntity(e);
        l.playSound(null, pos.x, pos.y, pos.z, ModSounds.CRACK.get(), SoundSource.HOSTILE, 2.0F, 0.7F);
        l.playSound(null, pos.x, pos.y, pos.z, ModSounds.CREATURE_CALL.get(), SoundSource.HOSTILE, 3.0F, 0.9F);
        maybeEntityMusic(p, l, HorrorDirector.stateFor(p), pos);
        ModNetwork.send(p, ClientEffectPacket.TITLE, 0, 0, "EL OTRO");
        return e;
    }

    /** Muy poco comun: aparece El Otro y tienes 120 s para matarlo. */
    private static boolean other(ServerPlayer p, ServerLevel l, PlayerState st) {
        if (!HorrorConfig.OTHER_ENABLED.get() || !HorrorUtil.entitiesAllowed(l)) return false;
        if (l.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return false;
        long now = l.getGameTime();
        if (!HorrorUtil.forceDay) {
            if (PlayerState.nights(p) < HorrorConfig.OTHER_MIN_NIGHT.get()) return false;
            if (now - st.lastOther < HorrorConfig.OTHER_COOLDOWN_MIN.get() * 1200L) return false;
        }
        if (!l.getEntitiesOfClass(OtherEntity.class, p.getBoundingBox().inflate(160.0D), e -> true).isEmpty()) return false;
        for (int i = 0; i < 12; i++) {
            Vec3 pos = findVisible(p, l, HorrorVariant.CRAWLER, 14.0D, 22.0D, 45.0D, 15, 3);
            if (pos == null) continue;
            OtherEntity probe = ModEntities.OTHER.get().create(l);
            if (probe == null) return false;
            probe.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
            if (!l.noCollision(probe)) continue; // hueco insuficiente para su tamano
            for (HorrorEntity o : l.getEntitiesOfClass(HorrorEntity.class, p.getBoundingBox().inflate(96.0D),
                    e2 -> p.getUUID().equals(e2.getOwnerId()))) {
                o.discard();
            }
            spawnOther(p, l, pos);
            st.lastOther = now;
            p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ eventos nocturnos nuevos

    /** Apagon: la pantalla se va a negro unos instantes; al volver, a veces hay algo de reojo. */
    private static boolean blackout(ServerPlayer p, ServerLevel l, PlayerState st) {
        RandomSource r = p.getRandom();
        int ticks = 8 + r.nextInt(14);
        ModNetwork.send(p, ClientEffectPacket.BLACKOUT, ticks, 0, "");
        p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));
        p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.AMBIENT));
        if (r.nextInt(100) < 45) {
            st.schedule(ticks + 2, (pl, lv) -> peripheral(pl, lv, st));
        } else if (r.nextBoolean()) {
            st.schedule(ticks + 10, (pl, lv) -> HorrorUtil.sound(pl, ModSounds.BREATH.get(), SoundSource.HOSTILE,
                    around(pl, 1.2D, 2.4D, true), 0.6F, 0.9F));
        }
        return true;
    }

    /** Rafaga de estatica de television con su sonido. */
    private static boolean staticBurst(ServerPlayer p, ServerLevel l, PlayerState st) {
        ModNetwork.send(p, ClientEffectPacket.STATIC, 6 + p.getRandom().nextInt(10), 0, "");
        return true;
    }

    /** Llamadas lejanas: criatura, sirena o tono anomalo, muy lejos y de forma posicional. */
    private static boolean farCall(ServerPlayer p, ServerLevel l, PlayerState st) {
        RandomSource r = p.getRandom();
        int roll = r.nextInt(100);
        if (roll < 45) {
            HorrorUtil.sound(p, ModSounds.CREATURE_CALL.get(), SoundSource.HOSTILE, around(p, 38.0D, 62.0D, false),
                    3.0F, 0.9F + r.nextFloat() * 0.15F);
        } else if (roll < 75) {
            HorrorUtil.sound(p, ModSounds.SIREN_CALL.get(), SoundSource.HOSTILE, around(p, 40.0D, 64.0D, false),
                    3.2F, 0.95F + r.nextFloat() * 0.1F);
        } else {
            HorrorUtil.sound(p, ModSounds.ANOMALY_TONE.get(), SoundSource.AMBIENT, around(p, 6.0D, 12.0D, false),
                    0.55F, 0.95F);
        }
        return true;
    }

    /** Mensaje de chat falso: alguien "entra" a la partida o te habla. */
    private static boolean fakeChat(ServerPlayer p, ServerLevel l, PlayerState st) {
        RandomSource r = p.getRandom();
        String[] names = {"???", "Observador", "Anexo"};
        String name = names[r.nextInt(names.length)];
        if (r.nextInt(100) < 55) {
            p.sendSystemMessage(Component.translatable("multiplayer.player.joined", name).withStyle(ChatFormatting.YELLOW));
        } else {
            String[] msgs = {"no mires atr\u00e1s", "te veo", "ya casi", "shhh", "sigues ah\u00ed", "abre los ojos"};
            p.sendSystemMessage(Component.translatable("chat.type.text", name, msgs[r.nextInt(msgs.length)])
                    .withStyle(ChatFormatting.DARK_RED));
        }
        return true;
    }

    private HorrorEvents() {}

}
