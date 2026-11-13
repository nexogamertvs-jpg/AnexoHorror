package com.anexohorror.director;

import com.anexohorror.entity.HorrorVariant;
import com.anexohorror.entity.PeekPose;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Busca coberturas reales (troncos, esquinas, pilares, salientes) desde donde una presencia pueda
 * asomarse: escondida del todo desde el ojo del jugador, y con la cabeza visible al salir un poco.
 */
public final class CoverFinder {
    private static final double[] HIDE_STEPS = {0.65D, 0.95D, 1.25D, 1.60D};

    public static CoverSpot find(ServerPlayer p, ServerLevel l, HorrorVariant v, float scale,
                                 double minD, double maxD, int maxLight, int attempts) {
        RandomSource r = p.getRandom();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        double baseAng = Math.atan2(look.z, look.x);
        double hover = v.hover * scale;
        double h = v.height * scale;
        double w = h * v.aspect;
        double hc = v.headCenterY(scale);
        double headHalf = Math.max(0.12D, v.headWidth(scale) * 0.3D);
        float[] off = PeekPose.headOffset(v, scale, PeekPose.leanRad(1.0F, 1.0F), PeekPose.tiltRad(1.0F, 1.0F, 1.0F));

        for (int i = 0; i < attempts; i++) {
            double ang = r.nextInt(100) < 70
                    ? baseAng + (r.nextDouble() * 2.0D - 1.0D) * Math.toRadians(85.0D)
                    : r.nextDouble() * Math.PI * 2.0D;
            double dist = minD + r.nextDouble() * Math.max(0.1D, maxD - minD);
            Optional<BlockPos> g = HorrorUtil.ground(l, Mth.floor(p.getX() + Math.cos(ang) * dist),
                    Mth.floor(p.getZ() + Math.sin(ang) * dist), p.getBlockY(), 4, 8);
            if (g.isEmpty()) continue;
            BlockPos gp = g.get();
            if (l.getMaxLocalRawBrightness(gp) > maxLight) continue;
            Vec3 hidden = new Vec3(gp.getX() + 0.5D, gp.getY(), gp.getZ() + 0.5D);
            double hx = hidden.x - p.getX();
            double hz = hidden.z - p.getZ();
            double hd = Math.sqrt(hx * hx + hz * hz);
            if (hd < minD * 0.85D || hd > maxD * 1.1D || hd < 0.01D) continue;

            // La figura entera tiene que quedar oculta desde el ojo del jugador.
            if (HorrorUtil.visibleSamples(l, eye, p, hidden, hover, h, w) > 1) continue;
            // Y la cobertura (el bloque que la tapa) debe estar pegada a ella.
            Vec3 headHidden = hidden.add(0.0D, hc, 0.0D);
            BlockHitResult hit = l.clip(new ClipContext(eye, headHidden, ClipContext.Block.VISUAL,
                    ClipContext.Fluid.NONE, p));
            if (hit.getType() == HitResult.Type.MISS) continue;
            if (hit.getLocation().distanceToSqr(headHidden) > 2.6D * 2.6D) continue;

            Vec3 d = new Vec3(hx / hd, 0.0D, hz / hd);
            Vec3 t = new Vec3(-d.z, 0.0D, d.x);
            boolean firstPositive = r.nextBoolean();
            for (int si = 0; si < 2; si++) {
                double sign = (si == 0) == firstPositive ? 1.0D : -1.0D;
                Vec3 lean = t.scale(sign);
                for (double hide : HIDE_STEPS) {
                    Vec3 sh = hidden.add(lean.scale(hide));
                    Optional<BlockPos> g2 = HorrorUtil.ground(l, Mth.floor(sh.x), Mth.floor(sh.z), gp.getY(), 1, 2);
                    if (g2.isEmpty()) continue;
                    Vec3 shownFeet = new Vec3(sh.x, g2.get().getY(), sh.z);
                    Vec3 headShown = shownFeet.add(lean.scale(off[0])).add(0.0D, off[1], 0.0D);
                    if (HorrorUtil.headSamples(l, eye, p, headShown, headHalf) < 2) continue;
                    return new CoverSpot(shownFeet, lean, hide,
                            gripPoint(hit.getBlockPos(), lean, d, shownFeet.y + off[1] - 0.55D * scale, shownFeet),
                            scale, true);
                }
            }
        }
        return null;
    }

    /** Esquina vertical de la cobertura por la que sale la figura; la mano se apoya ahi. */
    private static Vec3 gripPoint(BlockPos cover, Vec3 lean, Vec3 d, double y, Vec3 shownFeet) {
        double cx0 = cover.getX() + 0.5D;
        double cz0 = cover.getZ() + 0.5D;
        double best = -1.0E9D;
        double gx = cx0;
        double gz = cz0;
        for (int ax = 0; ax < 2; ax++) {
            for (int az = 0; az < 2; az++) {
                double cx = cover.getX() + ax;
                double cz = cover.getZ() + az;
                double score = (cx - cx0) * lean.x + (cz - cz0) * lean.z
                        - 0.01D * ((cx - cx0) * d.x + (cz - cz0) * d.z); // a igualdad, la esquina cercana al jugador
                if (score > best) { best = score; gx = cx; gz = cz; }
            }
        }
        double dx = gx - shownFeet.x;
        double dz = gz - shownFeet.z;
        if (dx * dx + dz * dz > 1.8D * 1.8D) return null;
        double gy = Mth.clamp(y, cover.getY() + 0.15D, cover.getY() + 0.85D);
        return new Vec3(gx - d.x * 0.04D, gy, gz - d.z * 0.04D);
    }

    private CoverFinder() {}
}
