package com.anexohorror.client;

import com.anexohorror.entity.HorrorEntity;
import com.anexohorror.entity.HorrorMode;
import com.anexohorror.entity.HorrorVariant;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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

        poseStack.pushPose();
        poseStack.translate(0.0D, hover, 0.0D);
        poseStack.mulPose(Axis.YP.rotation(ang));
        if (entity.getMode() == HorrorMode.PEEK) {
            poseStack.mulPose(Axis.ZP.rotationDegrees((Math.floorMod(entity.getId(), 5) - 2) * 3.0F));
        }

        int light = LightTexture.pack(Math.max(LightTexture.block(packedLight), 5), LightTexture.sky(packedLight));
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(v.texture));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        float hw = w / 2.0F;
        vertex(vc, m, n, -hw, 0.0F, 0.0F, 1.0F, alpha, light);
        vertex(vc, m, n, hw, 0.0F, 1.0F, 1.0F, alpha, light);
        vertex(vc, m, n, hw, h, 1.0F, 0.0F, alpha, light);
        vertex(vc, m, n, -hw, h, 0.0F, 0.0F, alpha, light);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y,
                               float u, float v, float a, int light) {
        vc.vertex(m, x, y, 0.0F).color(1.0F, 1.0F, 1.0F, a).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0.0F, 0.0F, 1.0F).endVertex();
    }
}
