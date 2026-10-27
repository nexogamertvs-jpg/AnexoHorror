package com.anexohorror.director;

import com.anexohorror.HorrorConfig;
import com.anexohorror.entity.HorrorEntity;
import com.anexohorror.entity.HorrorMode;
import com.anexohorror.entity.HorrorVariant;
import com.anexohorror.registry.ModEntities;
import com.anexohorror.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
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

    // Pesos por variante, orden: PEEKER, GRIN, SMILER, DRAWN, CRAWLER, EYE, SLENDER
    private static final double[] W_STARE = {1.0, 1.5, 3.0, 3.0, 2.5, 1.0, 2.0};
    private static final double[] W_PERIPH = {1.5, 2.0, 3.0, 2.0, 3.0, 1.5, 1.5};
    private static final double[] W_PEEK = {4.0, 2.0, 1.0, 1.0, 1.5, 2.0, 2.5};
    private static final double[] W_DOOR = {3.0, 2.0, 1.0, 0.5, 1.0, 2.0, 1.5};
    private static final double[] W_BED = {3.0, 2.0, 0.0, 0.0, 2.0, 2.0, 0.0};
    private static final double[] W_CAVE = {0.5, 3.0, 2.0, 2.0, 2.0, 3.0, 2.0};

    private static final HorrorVariant[] VARIANTS = HorrorVariant.values();

    public static boolean run(EventType t, ServerPlayer p, ServerLevel l, PlayerState st) {
        return switch (t) {
            case STARE -> stare(p, l, st);
            case PERIPHERAL -> peripheral(p, l, st);
            case PEEK -> peek(p, l, st);
            case DOOR -> door(p, l, st);
            case BED -> bed(p, l, st, p.blockPosition(), 14, 3.0D);
            case CAVE -> cave(p, l, st);
            case FAKE_SOUND -> fakeSound(p, l, st);
            case FOOTSTEPS -> footsteps(p, l, st);
            case SILENCE -> silence(p, l, st);
        };
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

    private static boolean canSpawnMore(ServerPlayer p, ServerLevel l, boolean night) {
        if (!HorrorUtil.entitiesAllowed(l)) return false; // solo de noche
        int a = active(p, l);
        return night ? a < HorrorConfig.MAX_ENTITIES.get() : a < 1;
    }

    private static double minDist(PlayerState st, boolean night) {
        double d = night ? HorrorConfig.MIN_DIST_NIGHT.get() : HorrorConfig.MIN_DIST_DAY.get();
        return Math.max(7.0D, d - st.bold * 5.0D); // si el jugador sostiene la mirada, aparecen mas cerca
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
            case PEEK -> { maxLife = night ? 300 + r.nextInt(500) : 200 + r.nextInt(300);
                stareLimit = night ? 120 + r.nextInt(260) : 60 + r.nextInt(120); grace = 6 + r.nextInt(8);
                glimpse = 0; vanishDist = Math.min(4.0D, dist * 0.5D); }
            case LOW -> { maxLife = 260 + r.nextInt(300); stareLimit = (night ? 120 : 80) + r.nextInt(160);
                grace = 5 + r.nextInt(6); glimpse = 0; vanishDist = Math.min(3.0D, dist * 0.5D); }
            default -> { maxLife = night ? 400 + r.nextInt(700) : 220 + r.nextInt(400);
                stareLimit = night ? 200 + r.nextInt(400) : 90 + r.nextInt(180);
                grace = night ? 6 + r.nextInt(8) : 8 + r.nextInt(10); glimpse = 0;
                vanishDist = Mth.clamp(dist * 0.4D, 4.5D, 9.0D); }
        }
        e.setup(p.getUUID(), v, mode, scale, maxLife, stareLimit, grace, glimpse, vanishDist, chain);
        float yaw = (float) (Mth.atan2(p.getZ() - pos.z, p.getX() - pos.x) * (180.0D / Math.PI)) - 90.0F;
        e.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        e.setAllowDay(!HorrorUtil.isNightTime(l)); // solo true si se invoco por comando de dia
        l.addFreshEntity(e);

        float vol = Math.max(1.0F, (float) dist / 12.0F + 0.2F);
        float pitch = 0.85F + r.nextFloat() * 0.3F;
        l.playSound(null, pos.x, pos.y, pos.z, ModSounds.CRACK.get(), SoundSource.HOSTILE, vol, pitch);
        st.lastVariant = v;
        return e;
    }

    // ------------------------------------------------------------------ busqueda de lugares

    private static Vec3 findVisible(ServerPlayer p, ServerLevel l, HorrorVariant v, double minD, double maxD,
                                    double halfAngleDeg, int maxLight, int minSamples) {
        RandomSource r = p.getRandom();
        Vec3 hl = horizontalLook(p);
        Vec3 eye = p.getEyePosition();
        for (int i = 0; i < 60; i++) {
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
            double d = 6.0D + r.nextDouble() * 11.0D;
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
    private static Vec3 findPeek(ServerPlayer p, ServerLevel l, HorrorVariant v, double minD, double maxD,
                                 int maxLight, BlockPos near, double nearRadius) {
        RandomSource r = p.getRandom();
        Vec3 eye = p.getEyePosition();
        double[][] offs = {{0, 0}, {0.5, 0}, {-0.5, 0}, {0, 0.5}, {0, -0.5}};
        for (int i = 0; i < 140; i++) {
            double x, z;
            int baseY;
            if (near != null) {
                x = near.getX() + 0.5D + (r.nextDouble() * 2.0D - 1.0D) * nearRadius;
                z = near.getZ() + 0.5D + (r.nextDouble() * 2.0D - 1.0D) * nearRadius;
                baseY = near.getY();
            } else {
                double ang = r.nextDouble() * Math.PI * 2.0D;
                double d = minD + r.nextDouble() * (maxD - minD);
                x = p.getX() + Math.cos(ang) * d;
                z = p.getZ() + Math.sin(ang) * d;
                baseY = p.getBlockY();
            }
            Optional<BlockPos> g = HorrorUtil.ground(l, Mth.floor(x), Mth.floor(z), baseY, 3, 6);
            if (g.isEmpty()) continue;
            BlockPos gp = g.get();
            if (l.getMaxLocalRawBrightness(gp) > maxLight) continue;
            for (double[] o : offs) {
                Vec3 feet = new Vec3(gp.getX() + 0.5D + o[0], gp.getY(), gp.getZ() + 0.5D + o[1]);
                double hd = Math.sqrt((feet.x - p.getX()) * (feet.x - p.getX()) + (feet.z - p.getZ()) * (feet.z - p.getZ()));
                if (hd < 3.0D) continue;
                if (cosToEye(p, feet, v.hover + v.height * 0.6D) < 0.15D) continue; // delante del jugador
                int vis = HorrorUtil.visibleSamples(l, eye, p, feet, v.hover, v.height, v.height * v.aspect);
                if (vis >= 1 && vis <= 3) return feet;
            }
        }
        return null;
    }

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
        int maxLight = night ? 10 : 15;
        Vec3 pos = findVisible(p, l, v, minD, maxD, 40.0D, maxLight, 4);
        if (pos == null) pos = findVisible(p, l, v, minD, maxD, 75.0D, maxLight, 3);
        if (pos == null) return false;
        int chain = (night && HorrorConfig.ESCALATION.get() && r.nextInt(100) < 35) ? 1 + r.nextInt(3) : 0;
        spawn(p, l, st, v, HorrorMode.STARE, pos, chain, 1.0F);
        if (night && active(p, l) < HorrorConfig.MAX_ENTITIES.get() && r.nextInt(100) < 25) {
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
        if (!canSpawnMore(p, l, night)) return false;
        HorrorVariant v = pick(p.getRandom(), W_PEEK, st, night);
        double minD = Math.max(5.0D, minDist(st, night) * 0.45D);
        Vec3 pos = findPeek(p, l, v, minD, minD + 12.0D, night ? 9 : 11, null, 0.0D);
        if (pos == null) return false;
        spawn(p, l, st, v, HorrorMode.PEEK, pos, 0, 1.0F);
        return true;
    }

    private static boolean door(ServerPlayer p, ServerLevel l, PlayerState st) {
        BlockPos door = findBlock(p, l, p.blockPosition(), 14, 4,
                s -> s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER, 3.0D);
        if (door == null) return false;
        RandomSource r = p.getRandom();
        boolean night = st.isNight(l);
        boolean open = l.getBlockState(door).getValue(DoorBlock.OPEN);
        Vec3 dp = Vec3.atCenterOf(door);
        if (open && canSpawnMore(p, l, night)) {
            HorrorVariant v = pick(r, W_DOOR, st, night);
            Vec3 pos = findPeek(p, l, v, 0.0D, 0.0D, 12, door, 2.5D);
            if (pos != null) {
                spawn(p, l, st, v, HorrorMode.PEEK, pos, 0, 1.0F);
                return true;
            }
        }
        // Puerta cerrada (o sin hueco): golpes irregulares y, a veces, un chirrido.
        int n = 2 + r.nextInt(4);
        int t = 1;
        for (int i = 0; i < n; i++) {
            final float pitch = 0.75F + r.nextFloat() * 0.3F;
            st.schedule(t, (pl, lv) -> HorrorUtil.sound(pl, ModSounds.KNOCK.get(), SoundSource.HOSTILE, dp, 1.3F, pitch));
            t += 6 + r.nextInt(18);
        }
        if (r.nextInt(100) < 35) {
            st.schedule(t + 30 + r.nextInt(40), (pl, lv) ->
                    HorrorUtil.sound(pl, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, dp, 1.0F, 0.8F));
        }
        return true;
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
        Vec3 pos = findVisible(p, l, v, 9.0D, 26.0D, 50.0D, 3, 3);
        if (pos != null) {
            spawn(p, l, st, v, HorrorMode.STARE, pos, 0, 1.0F);
            return true;
        }
        pos = findPeek(p, l, v, 6.0D, 18.0D, 5, null, 0.0D);
        if (pos == null) return false;
        spawn(p, l, st, v, HorrorMode.PEEK, pos, 0, 1.0F);
        return true;
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

    private enum Fake { WHISPER, BREATH, KNOCK_WALL, DRONE, STATIC, CRACK_ALONE, SCRAPE, HEARTBEAT, CREAK }

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
        for (int idx : st.recentSounds) w.computeIfPresent(Fake.values()[idx], (k, v) -> v * 0.25D);
        Fake f = HorrorUtil.pickWeighted(r, w);
        st.recentSounds.addLast(f.ordinal());
        while (st.recentSounds.size() > 3) st.recentSounds.removeFirst();

        float pitch = 0.85F + r.nextFloat() * 0.3F;
        switch (f) {
            case WHISPER -> HorrorUtil.sound(p, ModSounds.WHISPER.get(), SoundSource.HOSTILE,
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

    private HorrorEvents() {}
}
