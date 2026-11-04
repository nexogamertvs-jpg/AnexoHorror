package com.anexohorror.client;

import com.anexohorror.entity.HorrorEntity;
import com.anexohorror.entity.HorrorMode;
import com.anexohorror.entity.HorrorVariant;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.anexohorror.AnexoHorror;
import com.anexohorror.ClientConfig;
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

/** Dibuja la imagen original como plano vertical que siempre mira a la camara (estetica analogica). */
public class HorrorRenderer extends EntityRenderer<HorrorEntity> {
    private static final ResourceLocation AURA = new ResourceLocation(AnexoHorror.MODID, "textures/entity/aura.png");

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
        HorrorVariant v = entity.getVariant();
        float h = entity.spriteHeight();
        float w = entity.spriteWidth();
        float hover = entity.spriteHover();

        Vec3 cam = this.entityRenderDispatcher.camera.getPosition();
        double ex = Mth.lerp(partialTick, entity.xo, entity.getX());
        double ez = Mth.lerp(partialTick, entity.zo, entity.getZ());
        float ang = (float) Mth.atan2(cam.x - ex, cam.z - ez);

        float alpha = Mth.clamp((entity.tickCount + partialTick) / 5.0F, 0.0F, 1.0F);
        int ph = Math.floorMod(entity.tickCount * 7 + entity.getId(), 61);
        if (ph < 2) alpha *= 0.35F; // parpadeo tipo cinta danada

        boolean headOnly = entity.isHeadOnly();
        poseStack.pushPose();
        poseStack.translate(0.0D, hover, 0.0D);
        if (entity.getMode() == HorrorMode.HUNT) { // cazando: temblor frenetico y parpadeo
            float tt = entity.tickCount + partialTick;
            poseStack.translate(Mth.sin(tt * 3.7F) * 0.07F, Mth.sin(tt * 5.3F) * 0.05F, Mth.sin(tt * 4.1F) * 0.07F);
            if (Math.floorMod(entity.tickCount * 5 + entity.getId(), 11) < 2) alpha *= 0.55F;
        }
        poseStack.mulPose(Axis.YP.rotation(ang));
        if (entity.getMode() == HorrorMode.PEEK) {
            poseStack.mulPose(Axis.ZP.rotationDegrees((Math.floorMod(entity.getId(), 5) - 2) * 3.0F));
        }

        int light = LightTexture.pack(Math.max(LightTexture.block(packedLight), 8), LightTexture.sky(packedLight));
        glow(buffer, v, poseStack.last(), w, h, alpha, entity, partialTick, -0.04F, headOnly);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(v.texture));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        float hw = w / 2.0F;
        float u0 = headOnly ? v.headU0 : 0.0F;
        float u1 = headOnly ? v.headU1 : 1.0F;
        float v0 = headOnly ? v.headV0 : 0.0F;
        float v1 = headOnly ? v.headV1 : 1.0F;
        vertex(vc, m, n, -hw, 0.0F, u0, v1, alpha, light);
        vertex(vc, m, n, hw, 0.0F, u1, v1, alpha, light);
        vertex(vc, m, n, hw, h, u1, v0, alpha, light);
        vertex(vc, m, n, -hw, h, u0, v0, alpha, light);
        poseStack.popPose();
    }


    /** Aura luminosa suave (emisiva, aditiva) detras de la presencia para que se note de noche. */
    private static void glow(MultiBufferSource buf, HorrorVariant v, PoseStack.Pose pose, float w, float h,
                             float alpha, HorrorEntity e, float pt, float zBack, boolean head) {
        if (!ClientConfig.GLOW.get()) return;
        float t = e.tickCount + pt;
        float pulse = 0.78F + 0.22F * Mth.sin(t * 0.07F + e.getId());
        if (Math.floorMod(e.tickCount * 3 + e.getId(), 97) < 2) pulse *= 0.5F; // parpadeo
        float a = Mth.clamp(alpha * pulse * ClientConfig.GLOW_STRENGTH.get().floatValue(), 0.0F, 1.0F);
        if (a < 0.02F) return;
        VertexConsumer gv = buf.getBuffer(RenderType.eyes(head ? AURA : v.glowTexture));
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        float gw = w * (head ? 1.9F : v.glowScaleX) * 0.5F;
        float gh = h * (head ? 1.7F : v.glowScaleY);
        float gb = -h * (head ? 0.35F : v.glowPadY);
        int full = LightTexture.FULL_BRIGHT;
        vertexZ(gv, m, n, -gw, gb, zBack, 0.0F, 1.0F, a, full);
        vertexZ(gv, m, n, gw, gb, zBack, 1.0F, 1.0F, a, full);
        vertexZ(gv, m, n, gw, gb + gh, zBack, 1.0F, 0.0F, a, full);
        vertexZ(gv, m, n, -gw, gb + gh, zBack, 0.0F, 0.0F, a, full);
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
