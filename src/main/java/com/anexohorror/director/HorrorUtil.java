package com.anexohorror.director;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Optional;

public final class HorrorUtil {

    /** Solo para comandos de prueba: permite apariciones de dia. */
    public static volatile boolean forceDay;

    /** Noche de Minecraft (13000-23000). Las presencias solo existen en este tramo. */
    public static boolean isNightTime(ServerLevel l) {
        long t = l.getDayTime() % 24000L;
        return t >= 13000L && t < 23000L;
    }

    public static boolean entitiesAllowed(ServerLevel l) {
        return forceDay || isNightTime(l);
    }

    /** Celda de aire de 2 de alto, sin liquido y con suelo solido debajo. */
    public static boolean standable(ServerLevel l, BlockPos p) {
        if (!l.hasChunkAt(p)) return false;
        BlockPos up = p.above();
        BlockPos down = p.below();
        return l.getBlockState(p).getCollisionShape(l, p).isEmpty()
                && l.getBlockState(up).getCollisionShape(l, up).isEmpty()
                && l.getFluidState(p).isEmpty()
                && l.getFluidState(up).isEmpty()
                && l.getBlockState(down).isFaceSturdy(l, down, Direction.UP);
    }

    public static Optional<BlockPos> ground(ServerLevel l, int x, int z, int y, int up, int down) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int yy = y + up; yy >= y - down; yy--) {
            m.set(x, yy, z);
            if (standable(l, m)) return Optional.of(m.immutable());
        }
        return Optional.empty();
    }

    /**
     * Cuantos de 6 puntos del sprite (3 en ancho x 2 en alto) ve el ojo del jugador.
     * 6 = totalmente visible, 1..5 = parcialmente oculto tras un obstaculo, 0 = oculto.
     */
    public static int visibleSamples(ServerLevel l, Vec3 eye, Entity ctx, Vec3 feet,
                                     double hover, double h, double w) {
        Vec3 f = new Vec3(feet.x - eye.x, 0.0D, feet.z - eye.z);
        if (f.lengthSqr() < 1.0E-4D) return 6;
        f = f.normalize();
        Vec3 right = new Vec3(-f.z, 0.0D, f.x);
        int c = 0;
        for (double ox : new double[]{-0.4D, 0.0D, 0.4D}) {
            for (double oy : new double[]{0.3D, 0.85D}) {
                Vec3 t = feet.add(right.scale(ox * w)).add(0.0D, hover + h * oy, 0.0D);
                BlockHitResult r = l.clip(new ClipContext(eye, t, ClipContext.Block.VISUAL,
                        ClipContext.Fluid.NONE, ctx));
                if (r.getType() == HitResult.Type.MISS) c++;
            }
        }
        return c;
    }

    /** Envia un sonido posicional solo a este jugador (nadie mas lo oye). */
    public static void sound(ServerPlayer p, SoundEvent ev, SoundSource src, Vec3 pos, float vol, float pitch) {
        Holder<SoundEvent> holder = ForgeRegistries.SOUND_EVENTS.getHolder(ev).map(h -> (Holder<SoundEvent>) h)
                .orElseGet(() -> Holder.direct(ev));
        p.connection.send(new ClientboundSoundPacket(holder, src, pos.x, pos.y, pos.z, vol, pitch,
                p.getRandom().nextLong()));
    }

    public static <T> T pickWeighted(RandomSource r, Map<T, Double> weights) {
        double sum = 0.0D;
        for (double d : weights.values()) sum += d;
        double x = r.nextDouble() * sum;
        T last = null;
        for (Map.Entry<T, Double> e : weights.entrySet()) {
            last = e.getKey();
            x -= e.getValue();
            if (x <= 0.0D) return e.getKey();
        }
        return last;
    }

    public static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    public static boolean isSolidVisualBlock(BlockState s) { return !s.isAir(); }

    private HorrorUtil() {}
}
