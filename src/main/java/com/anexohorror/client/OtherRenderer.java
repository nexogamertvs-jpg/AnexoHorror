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

/** Dibuja a "El Otro" con el modelo .obj de la criatura sentada, orientado segun su cuerpo. */
public class OtherRenderer extends EntityRenderer<OtherEntity> {
    private static final ResourceLocation TEX = new ResourceLocation(AnexoHorror.MODID, "textures/entity/slender.png");
    private static final ResourceLocation AURA = new ResourceLocation(AnexoHorror.MODID, "textures/entity/aura.png");
    private static final float HEIGHT = 3.0F;

    public OtherRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(OtherEntity entity) {
        return TEX;
    }

    @Override
    public void render(OtherEntity e, float entityYaw, float pt, PoseStack ps, MultiBufferSource buf, int packedLight) {
        Vec3 cam = this.entityRenderDispatcher.camera.getPosition();
        double ex = Mth.lerp(pt, e.xo, e.getX());
        double ez = Mth.lerp(pt, e.zo, e.getZ());
        double cdx = cam.x - ex;
        double cdz = cam.z - ez;
        ObjModel m = ObjModel.slender(cdx * cdx + cdz * cdz > 22.0D * 22.0D);
        if (m == null) return;

        float t = e.tickCount + pt;
        float death = e.deathTime > 0 ? Math.min(90.0F, (e.deathTime + pt) * 4.5F) : 0.0F;

        // aura (misma idea que las demas presencias)
        if (ClientConfig.GLOW.get()) {
            float pulse = 0.78F + 0.22F * Mth.sin(t * 0.07F + e.getId());
            float a = Mth.clamp(pulse * ClientConfig.GLOW_STRENGTH.get().floatValue() * (1.0F - death / 120.0F), 0.0F, 1.0F);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotation((float) Mth.atan2(cdx, cdz)));
            PoseStack.Pose gp = ps.last();
            VertexConsumer gv = buf.getBuffer(RenderType.eyes(AURA));
            float gw = 2.1F * 1.7F * 0.5F;
            float gh = HEIGHT * 1.25F;
            float gb = -HEIGHT * 0.08F;
            int full = LightTexture.FULL_BRIGHT;
            glowVertex(gv, gp, -gw, gb, -0.6F, 0.0F, 1.0F, a, full);
            glowVertex(gv, gp, gw, gb, -0.6F, 1.0F, 1.0F, a, full);
            glowVertex(gv, gp, gw, gb + gh, -0.6F, 1.0F, 0.0F, a, full);
            glowVertex(gv, gp, -gw, gb + gh, -0.6F, 0.0F, 0.0F, a, full);
            ps.popPose();
        }

        // orientacion: el frente del modelo es +X; lo alineamos con hacia donde mira el cuerpo
        float bodyYaw = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot) * ((float) Math.PI / 180.0F);
        float fx = -Mth.sin(bodyYaw);
        float fz = Mth.cos(bodyYaw);
        float ang = (float) Mth.atan2(-fz, fx);
        double speed = e.getDeltaMovement().horizontalDistance();
        float bob = speed > 0.02D ? Mth.sin(t * 0.9F) * 3.0F : Mth.sin(t * 0.05F) * 1.2F;
        float s = HEIGHT / m.height;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotation(ang));
        ps.mulPose(Axis.XP.rotationDegrees(bob));
        if (death > 0.0F) ps.mulPose(Axis.ZP.rotationDegrees(-death));
        ps.scale(s, s, s);
        ps.translate(-m.cx, -m.minY, -m.cz);

        int light = LightTexture.pack(Math.max(LightTexture.block(packedLight), 6), LightTexture.sky(packedLight));
        int overlay = LivingEntityRenderer.getOverlayCoords(e, 0.0F); // destello rojo al recibir dano
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
                    .color(255, 255, 255, 255)
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

    private static void glowVertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
                                   float u, float v, float a, int light) {
        vc.vertex(pose.pose(), x, y, z).color(1.0F, 1.0F, 1.0F, a).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
    }
}
