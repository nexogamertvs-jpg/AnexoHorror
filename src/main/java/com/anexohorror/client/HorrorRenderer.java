package com.anexohorror.client;

import com.anexohorror.AnexoHorror;
import com.anexohorror.entity.HorrorEntity;
import com.anexohorror.entity.HorrorMode;
import com.anexohorror.entity.HorrorVariant;
import com.anexohorror.entity.PeekPose;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Siluetas 2D. Los asomos (PEEK) se dibujan como una cinta deformable: el cuerpo se inclina por
 * detras de la cobertura, la cabeza se ladea a tirones, una mano se agarra al borde y, en el
 * ataque, un brazo largo se estira en 3D hacia el jugador.
 */
public class HorrorRenderer extends EntityRenderer<HorrorEntity> {
    private static final ResourceLocation HAND = new ResourceLocation(AnexoHorror.MODID, "textures/entity/hand.png");
    // Solo se usan desde el hilo de render.
    private static final float[] SX = new float[PeekPose.ROWS + 1];
    private static final float[] SY = new float[PeekPose.ROWS + 1];
    private static final float[] PHI = new float[PeekPose.ROWS + 1];
    /** Fraccion de la textura de la mano donde acaban la palma y los dedos (arriba) y empieza el antebrazo. */
    private static final float WRIST_V = 0.62F;

    public HorrorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(HorrorEntity entity) {
        return entity.getVariant().texture;
    }

    @Override
    public void render(HorrorEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (entity.getMode() == HorrorMode.PEEK) {
            renderPeek(entity, partialTick, poseStack, buffer, packedLight);
        } else {
            renderStanding(entity, partialTick, poseStack, buffer, packedLight);
        }
    }

    // ------------------------------------------------------------------ figura de pie (resto de modos)

    private void renderStanding(HorrorEntity entity, float partialTick, PoseStack poseStack,
                                MultiBufferSource buffer, int packedLight) {
        HorrorVariant v = entity.getVariant();
        float fullH = entity.spriteHeight();
        float fullW = entity.spriteWidth();
        float hover = entity.spriteHover();

        Vec3 cam = this.entityRenderDispatcher.camera.getPosition();
        double ex = Mth.lerp(partialTick, entity.xo, entity.getX());
        double ez = Mth.lerp(partialTick, entity.zo, entity.getZ());
        float ang = (float) Mth.atan2(cam.x - ex, cam.z - ez);

        float t = entity.tickCount + partialTick;
        float alpha = Mth.clamp(t / 5.0F, 0.0F, 1.0F);
        int ph = Math.floorMod(entity.tickCount * 7 + entity.getId(), 61);
        if (ph < 2) alpha *= 0.35F;

        float breath = 1.0F + 0.016F * Mth.sin(t * 0.075F + entity.getId() * 0.13F);
        float sway = Mth.sin(t * 0.035F + entity.getId()) * 1.35F;
        float bob = Mth.sin(t * 0.052F + entity.getId() * 0.7F) * 0.024F;
        float twitch = Math.floorMod(entity.tickCount / 7 + entity.getId(), 83) == 0
                ? Mth.sin(t * 1.9F) * 2.2F : 0.0F;

        poseStack.pushPose();
        poseStack.translate(0.0D, hover + bob, 0.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(sway + twitch));
        poseStack.scale(1.0F - (breath - 1.0F) * 0.55F, breath, 1.0F);

        if (entity.getMode() == HorrorMode.HUNT) {
            float tt = entity.tickCount + partialTick;
            poseStack.translate(Mth.sin(tt * 3.7F) * 0.07F, Mth.sin(tt * 5.3F) * 0.05F,
                    Mth.sin(tt * 4.1F) * 0.07F);
            if (Math.floorMod(entity.tickCount * 5 + entity.getId(), 11) < 2) alpha *= 0.55F;
        }

        // Billboard: la cara siempre se orienta hacia quien la observa.
        poseStack.mulPose(Axis.YP.rotation(ang));

        int light = packedLight; // el cuerpo y el rostro reciben la luz real del mundo
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(v.texture));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        float hw = fullW / 2.0F;
        vertex(vc, m, n, -hw, 0.0F, 0.0F, 1.0F, alpha, light);
        vertex(vc, m, n, hw, 0.0F, 1.0F, 1.0F, alpha, light);
        vertex(vc, m, n, hw, fullH, 1.0F, 0.0F, alpha, light);
        vertex(vc, m, n, -hw, fullH, 0.0F, 0.0F, alpha, light);

        // Overlay UV sincronizado con la textura: solo brillan los ojos.
        float eyePulse = 0.88F + 0.12F * Mth.sin(t * 0.11F + entity.getId() * 0.37F);
        if (Math.floorMod(entity.tickCount * 11 + entity.getId(), 137) < 2) eyePulse *= 0.35F;
        float eyeAlpha = alpha * eyePulse;
        VertexConsumer eyes = buffer.getBuffer(RenderType.eyes(v.eyeTexture));
        float zEye = 0.012F;
        vertexZ(eyes, m, n, -hw, 0.0F, zEye, 0.0F, 1.0F, eyeAlpha, LightTexture.FULL_BRIGHT);
        vertexZ(eyes, m, n, hw, 0.0F, zEye, 1.0F, 1.0F, eyeAlpha, LightTexture.FULL_BRIGHT);
        vertexZ(eyes, m, n, hw, fullH, zEye, 1.0F, 0.0F, eyeAlpha, LightTexture.FULL_BRIGHT);
        vertexZ(eyes, m, n, -hw, fullH, zEye, 0.0F, 0.0F, eyeAlpha, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    // ------------------------------------------------------------------ asomo real

    private void renderPeek(HorrorEntity entity, float partialTick, PoseStack poseStack,
                            MultiBufferSource buffer, int light) {
        if (entity.isPeekGone()) return;
        HorrorVariant v = entity.getVariant();
        float scale = entity.getVisualScale();
        float height = v.height * scale;
        float width = height * v.aspect;
        float hover = entity.spriteHover();
        float e = Mth.clamp(entity.getRenderPeekProgress(partialTick), 0.0F, 1.0F);
        float t = entity.tickCount + partialTick;
        int id = entity.getId();

        Camera camera = this.entityRenderDispatcher.camera;
        Vec3 cam = camera.getPosition();
        double ex = Mth.lerp(partialTick, entity.xo, entity.getX());
        double ey = Mth.lerp(partialTick, entity.yo, entity.getY());
        double ez = Mth.lerp(partialTick, entity.zo, entity.getZ());
        float ang = (float) Mth.atan2(cam.x - ex, cam.z - ez);
        float rx = Mth.cos(ang);           // eje derecho del billboard en el mundo
        float rz = -Mth.sin(ang);
        float nx = Mth.sin(ang);           // normal del billboard (hacia la camara)
        float nz = Mth.cos(ang);

        // Por que lado de la pantalla sale la figura segun donde este la camara.
        float lx = entity.getLeanX();
        float lz = entity.getLeanZ();
        float dot = lx * rx + lz * rz;
        float side = dot >= 0.0F ? 1.0F : -1.0F;
        float k = 0.35F + 0.65F * Math.abs(dot);

        float alpha = Mth.clamp(t / 5.0F, 0.0F, 1.0F);
        if (Math.floorMod(entity.tickCount * 7 + id, 61) < 2) alpha *= 0.35F;

        // Cuerpo doblado hacia fuera y cabeza ladeada a tirones (stop-motion): la cabeza se
        // reajusta de golpe cada ~0.8 s en vez de oscilar suavemente.
        float amp = entity.getPeekAmp();
        float lean = side * k * PeekPose.leanRad(e, amp);
        float tilt = side * k * PeekPose.tiltRad(e, amp, twitch(t, id) + 0.05F * Mth.sin(t * 0.05F + id));
        PeekPose.spine(height, v.headV1, lean, tilt, SX, SY, PHI);

        // La columna pasa por la cabeza; si la cabeza esta a un lado del sprite, se espeja
        // para que el cuerpo quede siempre del lado de la cobertura.
        float uc = v.headCenterU();
        boolean flip = (uc - 0.5F) * side < 0.0F;
        float uS = flip ? 1.0F - uc : uc;

        float hide = entity.getHideDist();
        double shx = -lx * hide * (1.0F - e);
        double shz = -lz * hide * (1.0F - e);

        // ---------- cuerpo y ojos
        poseStack.pushPose();
        poseStack.translate(shx, hover, shz);
        poseStack.mulPose(Axis.YP.rotation(ang));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        drawRibbon(buffer.getBuffer(RenderType.entityTranslucent(v.texture)), m, n, width, uS, flip, alpha, light, 0.0F);
        float eyePulse = 0.88F + 0.12F * Mth.sin(t * 0.11F + id * 0.37F);
        if (Math.floorMod(entity.tickCount * 11 + id, 137) < 2) eyePulse *= 0.35F;
        drawRibbon(buffer.getBuffer(RenderType.eyes(v.eyeTexture)), m, n, width, uS, flip,
                alpha * eyePulse, LightTexture.FULL_BRIGHT, 0.012F);
        poseStack.popPose();

        // ---------- manos (misma textura): agarre del borde y brazo estirado
        VertexConsumer hand = buffer.getBuffer(RenderType.entityTranslucent(HAND));
        float hs = Math.max(0.7F, scale);
        if (entity.hasGrip() && e > 0.04F) {
            Vec3 g = entity.getGripRel();
            float gxs = (float) ((g.x - shx) * rx + (g.z - shz) * rz);   // en el marco del sprite
            float gys = (float) (g.y - hover);
            float inward = -side;
            float hw = 0.25F * hs;
            int jS = Math.round(0.72F * PeekPose.ROWS);
            float sxs = SX[jS] + side * 0.20F * width;
            float sys = SY[jS];

            // antebrazo: del hombro al borde, en el plano del sprite (la cobertura lo tapa)
            poseStack.pushPose();
            poseStack.translate(shx, hover, shz);
            poseStack.mulPose(Axis.YP.rotation(ang));
            pose = poseStack.last();
            segment(hand, pose.pose(), pose.normal(), sxs, sys, gxs, gys, hw * 1.3F, 1.0F, WRIST_V, 0.02F,
                    alpha, light);
            poseStack.popPose();

            // dedos: sobre la cara de la cobertura, anclados al mundo
            poseStack.pushPose();
            poseStack.translate(g.x, g.y, g.z);
            poseStack.mulPose(Axis.YP.rotation(ang));
            pose = poseStack.last();
            float curl = 0.02F * Mth.sin(t * 0.21F + id);
            segment(hand, pose.pose(), pose.normal(), 0.0F, 0.0F, inward * 0.62F * hs, 0.05F + curl, hw,
                    WRIST_V, 0.0F, 0.0F, alpha, light);
            poseStack.popPose();
        }

        if (entity.hasHandAttack() && entity.getHandProgress() > 0.01F) {
            int jS = Math.round(0.72F * PeekPose.ROWS);
            Vec3 camLocal = new Vec3(cam.x - ex, cam.y - ey, cam.z - ez);
            poseStack.pushPose();
            pose = poseStack.last();
            renderReach(entity, hand, pose.pose(), pose.normal(), camera, camLocal, rx, rz, nx, nz,
                    SX[jS], SY[jS], width, hover, hs, t, light);
            poseStack.popPose();
        }
    }

    /** Cambia el ladeo de la cabeza de golpe cada 16 ticks (el cambio dura 3 ticks). */
    private static float twitch(float t, int id) {
        int slot = (int) Math.floor(t / 16.0F);
        float f = PeekPose.smooth(0.0F, 3.0F, t - slot * 16.0F);
        float a = hash01(slot - 1 + id * 31);
        float b = hash01(slot + id * 31);
        return 0.65F + 0.7F * (a + (b - a) * f);
    }

    private static float hash01(int n) {
        int h = n * 0x9E3779B1;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        return (h & 0xFFFF) / 65535.0F;
    }

    /** Cinta de ROWS tiras que sigue la columna doblada SX/SY/PHI. */
    private static void drawRibbon(VertexConsumer vc, Matrix4f m, Matrix3f n, float width, float uS, boolean flip,
                                   float alpha, int light, float z) {
        int rows = PeekPose.ROWS;
        float ul = flip ? 1.0F : 0.0F;
        float ur = flip ? 0.0F : 1.0F;
        float lw = uS * width;
        float rw = (1.0F - uS) * width;
        for (int j = 0; j < rows; j++) {
            float c0 = Mth.cos(PHI[j]);
            float s0 = Mth.sin(PHI[j]);
            float c1 = Mth.cos(PHI[j + 1]);
            float s1 = Mth.sin(PHI[j + 1]);
            float v0 = 1.0F - j / (float) rows;
            float v1 = 1.0F - (j + 1) / (float) rows;
            vertexZ(vc, m, n, SX[j] - c0 * lw, SY[j] + s0 * lw, z, ul, v0, alpha, light);
            vertexZ(vc, m, n, SX[j] + c0 * rw, SY[j] - s0 * rw, z, ur, v0, alpha, light);
            vertexZ(vc, m, n, SX[j + 1] + c1 * rw, SY[j + 1] - s1 * rw, z, ur, v1, alpha, light);
            vertexZ(vc, m, n, SX[j + 1] - c1 * lw, SY[j + 1] + s1 * lw, z, ul, v1, alpha, light);
        }
    }

    /** Tramo de la textura de la mano entre dos puntos del plano (v0 en el primero, v1 en el segundo). */
    private static void segment(VertexConsumer vc, Matrix4f m, Matrix3f n, float x0, float y0, float x1, float y1,
                                float halfW, float v0, float v1, float z, float alpha, int light) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = Mth.sqrt(dx * dx + dy * dy);
        if (len < 0.02F) return;
        dx /= len;
        dy /= len;
        float px = dy * halfW;
        float py = -dx * halfW;
        vertexZ(vc, m, n, x0 - px, y0 - py, z, 0.0F, v0, alpha, light);
        vertexZ(vc, m, n, x0 + px, y0 + py, z, 1.0F, v0, alpha, light);
        vertexZ(vc, m, n, x1 + px, y1 + py, z, 1.0F, v1, alpha, light);
        vertexZ(vc, m, n, x1 - px, y1 - py, z, 0.0F, v1, alpha, light);
    }

    // ------------------------------------------------------------------ brazo estirado en 3D

    /**
     * Brazo largo desde el hombro hasta el jugador, en el espacio del mundo (origen = entidad).
     * El antebrazo es una cinta que gira alrededor de su eje para encarar la camara; la mano es un
     * cartel que mira a la camara y crece segun se acerca, asi que cuando apunta directo al ojo
     * se ve una palma enorme y no una linea. El avance es a tirones, con pausas.
     */
    private void renderReach(HorrorEntity entity, VertexConsumer vc, Matrix4f m, Matrix3f n, Camera camera,
                             Vec3 camLocal, float rx, float rz, float nx, float nz,
                             float shoulderSpineX, float shoulderY, float width, float hover, float hs,
                             float t, int light) {
        float progress = Mth.clamp(entity.getHandProgress(), 0.0F, 1.0F);
        float q = progress * 6.0F;
        int qi = (int) q;
        float stut = Math.min(1.0F, (qi + PeekPose.smooth(0.0F, 1.0F, q - qi)) / 6.0F);
        if (stut < 0.01F) return;

        Vec3 target = new Vec3(entity.getHandX(), entity.getHandY(), entity.getHandZ());
        float sideArm = (target.x * rx + target.z * rz) >= 0.0D ? 1.0F : -1.0F;
        float sx = shoulderSpineX + sideArm * 0.20F * width;
        Vec3 shoulder = new Vec3(rx * sx + nx * 0.06D, hover + shoulderY, rz * sx + nz * 0.06D);
        Vec3 full = target.subtract(shoulder);
        double len = full.length();
        if (len < 0.3D) return;
        Vec3 dir = full.scale(1.0D / len);
        Vec3 tip = shoulder.add(dir.scale(len * stut));

        // ejes de la camara
        float yaw = camera.getYRot() * Mth.DEG_TO_RAD;
        float pitch = camera.getXRot() * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
        Vec3 up = new Vec3(-Mth.sin(yaw) * Mth.sin(pitch), Mth.cos(pitch), Mth.cos(yaw) * Mth.sin(pitch));
        Vec3 right = fwd.cross(up).normalize();
        float nxv = (float) -fwd.x;
        float nyv = (float) -fwd.y;
        float nzv = (float) -fwd.z;

        // orientacion de la mano: sigue la direccion del brazo proyectada en pantalla; si apunta a la
        // camara (proyeccion casi nula) se queda con un giro lento y nervioso
        float roll = 0.45F * Mth.sin(t * 0.07F + entity.getId()) + 0.12F * Mth.sin(t * 0.9F);
        Vec3 rollUp = up.scale(Mth.cos(roll)).add(right.scale(Mth.sin(roll)));
        Vec3 proj = dir.subtract(fwd.scale(dir.dot(fwd)));
        double pl = proj.length();
        float f = PeekPose.smooth(0.12F, 0.5F, (float) pl);
        Vec3 upv = rollUp.scale(1.0F - f);
        if (pl > 1.0E-4D) upv = upv.add(proj.scale(f / pl));
        upv = upv.normalize();
        Vec3 rv = fwd.cross(upv).normalize();

        // temblor de los dedos
        tip = tip.add(rv.scale(0.04D * Mth.sin(t * 0.9F))).add(upv.scale(0.03D * Mth.cos(t * 1.3F)));

        float grow = 0.85F + 0.4F * stut;
        float hl = 1.15F * hs * grow;
        float hw = 0.30F * hs * grow;
        float alpha = Mth.clamp(0.2F + progress * 1.5F, 0.0F, 1.0F);
        Vec3 center = tip.subtract(upv.scale(hl * 0.35D));
        Vec3 bottom = center.subtract(upv.scale(hl * 0.5D));
        Vec3 top = center.add(upv.scale(hl * 0.5D));

        // palma y dedos
        vertexW(vc, m, n, bottom.subtract(rv.scale(hw)), 0.0F, WRIST_V, alpha, light, nxv, nyv, nzv);
        vertexW(vc, m, n, bottom.add(rv.scale(hw)), 1.0F, WRIST_V, alpha, light, nxv, nyv, nzv);
        vertexW(vc, m, n, top.add(rv.scale(hw)), 1.0F, 0.0F, alpha, light, nxv, nyv, nzv);
        vertexW(vc, m, n, top.subtract(rv.scale(hw)), 0.0F, 0.0F, alpha, light, nxv, nyv, nzv);

        // antebrazo: del hombro a la muñeca, con un leve serpenteo
        int segs = 8;
        Vec3 wrist = bottom;
        Vec3 span = wrist.subtract(shoulder);
        if (span.length() < 0.05D) return;
        float armHalf = 0.34F * hs;
        Vec3 prevL = null;
        Vec3 prevR = null;
        for (int i = 0; i <= segs; i++) {
            float u = i / (float) segs;
            Vec3 p = shoulder.add(span.scale(u));
            double wob = Mth.sin(t * 0.35F + u * 5.0F + entity.getId()) * 0.05D * u * Math.min(1.0D, span.length() * 0.3D);
            p = p.add(up.scale(wob));
            Vec3 seg = span.normalize();
            Vec3 w = seg.cross(camLocal.subtract(p));
            if (w.lengthSqr() < 1.0E-6D) w = right;
            w = w.normalize();
            if (w.dot(right) < 0.0D) w = w.scale(-1.0D);
            Vec3 l = p.subtract(w.scale(armHalf));
            Vec3 r = p.add(w.scale(armHalf));
            if (prevL != null) {
                float v0 = 1.0F - (1.0F - WRIST_V) * ((i - 1) / (float) segs);
                float v1 = 1.0F - (1.0F - WRIST_V) * (i / (float) segs);
                vertexW(vc, m, n, prevL, 0.0F, v0, alpha, light, nxv, nyv, nzv);
                vertexW(vc, m, n, prevR, 1.0F, v0, alpha, light, nxv, nyv, nzv);
                vertexW(vc, m, n, r, 1.0F, v1, alpha, light, nxv, nyv, nzv);
                vertexW(vc, m, n, l, 0.0F, v1, alpha, light, nxv, nyv, nzv);
            }
            prevL = l;
            prevR = r;
        }
    }

    private static void vertexW(VertexConsumer vc, Matrix4f m, Matrix3f n, Vec3 p, float u, float v, float a,
                                int light, float nx, float ny, float nz) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(1.0F, 1.0F, 1.0F, a).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
    }

    private static void vertexZ(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z,
                                float u, float v, float a, int light) {
        vc.vertex(m, x, y, z).color(1.0F, 1.0F, 1.0F, a).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0.0F, 0.0F, 1.0F).endVertex();
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y,
                               float u, float v, float a, int light) {
        vc.vertex(m, x, y, 0.0F).color(1.0F, 1.0F, 1.0F, a).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0.0F, 0.0F, 1.0F).endVertex();
    }
}
