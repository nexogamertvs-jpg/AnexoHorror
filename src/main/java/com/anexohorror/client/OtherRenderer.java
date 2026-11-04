package com.anexohorror.client;

import com.anexohorror.AnexoHorror;
import com.anexohorror.ClientConfig;
import com.anexohorror.entity.OtherEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Dibuja a "El Otro" con el modelo hecho en Blockbench (obj/other.obj + textures/entity/other.png).
 * Animaciones por codigo: respira, se mece, da sacudidas bruscas, carga con temblor antes de golpear y
 * "aparece" con un pequeno salto. Aura en distintos tonos de rojo.
 */
public class OtherRenderer extends EntityRenderer<OtherEntity> {
    private static final ResourceLocation TEX = new ResourceLocation(AnexoHorror.MODID, "textures/entity/other.png");
    private static final ResourceLocation AURA = new ResourceLocation(AnexoHorror.MODID, "textures/entity/aura_other.png");
    /** Alto del modelo en bloques (el .obj ya se exporta a esta altura). */
    private static final float HEIGHT = 4.0F;

    public OtherRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.2F;
    }

    @Override
    public ResourceLocation getTextureLocation(OtherEntity entity) {
        return TEX;
    }

    private static float hash(int n) {
        n = (n << 13) ^ n;
        return ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 2147483647.0F;
    }

    @Override
    public void render(OtherEntity e, float entityYaw, float pt, PoseStack ps, MultiBufferSource buf, int packedLight) {
        Vec3 cam = this.entityRenderDispatcher.camera.getPosition();
        double ex = Mth.lerp(pt, e.xo, e.getX());
        double ez = Mth.lerp(pt, e.zo, e.getZ());
        double cdx = cam.x - ex;
        double cdz = cam.z - ez;
        ObjModel m = ObjModel.other(cdx * cdx + cdz * cdz > 26.0D * 26.0D);
        if (m == null) return;

        float t = e.tickCount + pt;
        boolean windup = e.isWindup();
        float blinkAge = (float) (e.level().getGameTime() - e.getBlinkTick()) + pt;
        float death = e.deathTime > 0 ? Math.min(90.0F, (e.deathTime + pt) * 4.5F) : 0.0F;
        float alive = 1.0F - death / 120.0F;

        // ------------------------------------------------ aura roja (varios tonos, aditiva, brilla mas al cargar)
        if (ClientConfig.GLOW.get()) {
            float gs = ClientConfig.GLOW_STRENGTH.get().floatValue() * alive * (windup ? 1.5F : 1.0F);
            float pulseA = 0.75F + 0.25F * Mth.sin(t * 0.06F + e.getId());
            float pulseB = 0.70F + 0.30F * Mth.sin(t * 0.17F + 1.7F);
            if (Math.floorMod(e.tickCount * 3 + e.getId(), 89) < 2) { pulseA *= 0.5F; pulseB *= 0.4F; } // parpadeo
            ps.pushPose();
            ps.mulPose(Axis.YP.rotation((float) Mth.atan2(cdx, cdz)));
            PoseStack.Pose gp = ps.last();
            VertexConsumer gv = buf.getBuffer(RenderType.eyes(AURA));
            int full = LightTexture.FULL_BRIGHT;
            // capa exterior: rojo oscuro de sangre seca -> carmesi
            quad(gv, gp, 4.8F, 5.4F, -0.7F, -0.9F, Mth.clamp(0.50F * pulseA * gs, 0, 1),
                    0.50F, 0.00F, 0.04F, 0.95F, 0.12F, 0.10F, full);
            // capa interior: rojo vivo -> naranja rojizo
            quad(gv, gp, 3.0F, 4.6F, -0.3F, -0.7F, Mth.clamp(0.55F * pulseB * gs, 0, 1),
                    0.85F, 0.04F, 0.07F, 1.00F, 0.42F, 0.25F, full);
            // nucleo bajo, rojo profundo (donde se sienta)
            quad(gv, gp, 3.6F, 1.8F, -0.2F, -0.6F, Mth.clamp(0.45F * pulseA * gs, 0, 1),
                    0.70F, 0.00F, 0.03F, 0.90F, 0.08F, 0.08F, full);
            ps.popPose();
        }

        // ------------------------------------------------ orientacion: el frente del modelo es +X
        float bodyYaw = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot) * ((float) Math.PI / 180.0F);
        float fx = -Mth.sin(bodyYaw);
        float fz = Mth.cos(bodyYaw);
        float ang = (float) Mth.atan2(-fz, fx);

        // ------------------------------------------------ animaciones
        float breathe = 1.0F + 0.012F * Mth.sin(t * 0.11F);              // respira
        float sway = 1.5F * Mth.sin(t * 0.045F + e.getId());             // se mece
        int slot = e.tickCount / 6;                                       // pasos de ~0.3 s: movimiento entrecortado
        float jolt = 0.0F, joltYaw = 0.0F;
        if (hash(slot * 31 + e.getId()) > 0.88F) {                        // sacudida brusca ocasional
            jolt = (hash(slot * 17 + 3) - 0.5F) * 8.0F;
            joltYaw = (hash(slot * 13 + 7) - 0.5F) * 12.0F;
        }
        float lean = 2.5F + 1.5F * Mth.sin(t * 0.021F);
        float shakeX = 0.0F, shakeZ = 0.0F;
        if (windup) {                                                     // carga: se inclina y vibra violentamente
            lean = 14.0F;
            shakeX = Mth.sin(t * 6.1F) * 0.07F;
            shakeZ = Mth.sin(t * 7.3F) * 0.07F;
        }
        float pop = blinkAge < 6.0F ? 0.78F + 0.22F * (blinkAge / 6.0F) : 1.0F; // "aparece" con un saltito
        float s = HEIGHT / m.height * pop;

        ps.pushPose();
        ps.translate(shakeX, 0.0D, shakeZ);
        ps.mulPose(Axis.YP.rotation(ang));
        ps.mulPose(Axis.YP.rotationDegrees(joltYaw));
        ps.mulPose(Axis.ZP.rotationDegrees(-(lean + jolt)));
        ps.mulPose(Axis.XP.rotationDegrees(sway));
        if (death > 0.0F) ps.mulPose(Axis.ZP.rotationDegrees(-death));
        float side = 1.0F - 0.5F * (breathe - 1.0F);
        ps.scale(s * side, s * breathe, s * side);
        ps.translate(-m.cx, -m.minY, -m.cz);

        int light = LightTexture.pack(Math.max(LightTexture.block(packedLight), 6), LightTexture.sky(packedLight));
        int overlay = LivingEntityRenderer.getOverlayCoords(e, 0.0F);     // destello rojo al recibir dano
        // tinte: destello rojizo al aparecer y pulso rojo al cargar
        int gb = 255;
        if (blinkAge < 5.0F) gb = (int) (130 + 25 * blinkAge);
        else if (windup) gb = 175 + (int) (50 * Mth.sin(t * 1.4F));
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        PoseStack.Pose pose = ps.last();
        Matrix4f mt = pose.pose();
        Matrix3f nm = pose.normal();
        float a00 = mt.m00(), a01 = mt.m01(), a02 = mt.m02();
        float a10 = mt.m10(), a11 = mt.m11(), a12 = mt.m12();
        float a20 = mt.m20(), a21 = mt.m21(), a22 = mt.m22();
        float a30 = mt.m30(), a31 = mt.m31(), a32 = mt.m32();
        float n00 = nm.m00(), n01 = nm.m01(), n02 = nm.m02();
        float n10 = nm.m10(), n11 = nm.m11(), n12 = nm.m12();
        float n20 = nm.m20(), n21 = nm.m21(), n22 = nm.m22();
        float[] pos = m.pos;
        float[] uv = m.uv;
        float[] nrm = m.nrm;
        int[] idx = m.idx;
        for (int i = 0; i < idx.length; i++) {
            int v = idx[i];
            float x = pos[v * 3];
            float y = pos[v * 3 + 1];
            float z = pos[v * 3 + 2];
            float nx = nrm[v * 3];
            float ny = nrm[v * 3 + 1];
            float nz = nrm[v * 3 + 2];
            vc.vertex(a00 * x + a10 * y + a20 * z + a30,
                            a01 * x + a11 * y + a21 * z + a31,
                            a02 * x + a12 * y + a22 * z + a32)
                    .color(255, gb, gb, 255)
                    .uv(uv[v * 2], uv[v * 2 + 1])
                    .overlayCoords(overlay)
                    .uv2(light)
                    .normal(n00 * nx + n10 * ny + n20 * nz,
                            n01 * nx + n11 * ny + n21 * nz,
                            n02 * nx + n12 * ny + n22 * nz)
                    .endVertex();
        }
        ps.popPose();
    }

    /** Plano de aura con color distinto abajo y arriba (degradado de rojos). */
    private static void quad(VertexConsumer vc, PoseStack.Pose pose, float width, float height, float bottom, float z,
                             float a, float rb, float gb, float bb, float rt, float gt, float bt, int light) {
        float hw = width * 0.5F;
        vertex(vc, pose, -hw, bottom, z, 0.0F, 1.0F, rb, gb, bb, a, light);
        vertex(vc, pose, hw, bottom, z, 1.0F, 1.0F, rb, gb, bb, a, light);
        vertex(vc, pose, hw, bottom + height, z, 1.0F, 0.0F, rt, gt, bt, a, light);
        vertex(vc, pose, -hw, bottom + height, z, 0.0F, 0.0F, rt, gt, bt, a, light);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               float r, float g, float b, float a, int light) {
        vc.vertex(pose.pose(), x, y, z).color(r, g, b, a).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
    }
}
