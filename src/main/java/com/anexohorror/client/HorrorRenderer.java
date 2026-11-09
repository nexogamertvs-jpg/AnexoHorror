package com.anexohorror.client;

import com.anexohorror.AnexoHorror;
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

/** Siluetas 2D y asomos de cabeza individualizados para cada presencia. */
public class HorrorRenderer extends EntityRenderer<HorrorEntity> {
    private static final ResourceLocation HAND = new ResourceLocation(AnexoHorror.MODID, "textures/entity/hand.png");

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
        float fullH = entity.spriteHeight();
        float fullW = entity.spriteWidth();
        float hover = entity.spriteHover();
        boolean headPeek = entity.getMode() == HorrorMode.PEEK;

        // Cada entidad conserva su propio recorte facial. La cabeza queda centrada a la
        // altura anatomica de la figura, aunque no se dibuje el resto del cuerpo.
        float u0 = headPeek ? v.headU0 : 0.0F;
        float u1 = headPeek ? v.headU1 : 1.0F;
        float v0 = headPeek ? v.headV0 : 0.0F;
        float v1 = headPeek ? v.headV1 : 1.0F;
        float drawW = fullW * (u1 - u0);
        float drawH = fullH * (v1 - v0);
        float y0 = headPeek ? HorrorEntity.HEAD_CENTER - drawH * 0.5F : 0.0F;
        float y1 = y0 + drawH;

        Vec3 cam = this.entityRenderDispatcher.camera.getPosition();
        double ex = Mth.lerp(partialTick, entity.xo, entity.getX());
        double ez = Mth.lerp(partialTick, entity.zo, entity.getZ());
        float ang = (float) Mth.atan2(cam.x - ex, cam.z - ez);

        float t = entity.tickCount + partialTick;
        float alpha = Mth.clamp(t / 5.0F, 0.0F, 1.0F);
        int ph = Math.floorMod(entity.tickCount * 7 + entity.getId(), 61);
        if (ph < 2) alpha *= 0.35F;

        // El rostro no se balancea como una figura completa: apenas respira y se ladea.
        float breath = headPeek
                ? 1.0F + 0.0045F * Mth.sin(t * 0.075F + entity.getId() * 0.13F)
                : 1.0F + 0.016F * Mth.sin(t * 0.075F + entity.getId() * 0.13F);
        float sway = Mth.sin(t * 0.035F + entity.getId()) * (headPeek ? 0.22F : 1.35F);
        float bob = Mth.sin(t * 0.052F + entity.getId() * 0.7F) * (headPeek ? 0.008F : 0.024F);
        float twitch = Math.floorMod(entity.tickCount / 7 + entity.getId(), 83) == 0
                ? Mth.sin(t * 1.9F) * (headPeek ? 0.9F : 2.2F) : 0.0F;

        poseStack.pushPose();
        poseStack.translate(0.0D, hover + bob, 0.0D);
        if (!headPeek) poseStack.mulPose(Axis.ZP.rotationDegrees(sway + twitch));
        poseStack.scale(1.0F - (breath - 1.0F) * 0.55F, breath, 1.0F);

        if (entity.getMode() == HorrorMode.HUNT) {
            float tt = entity.tickCount + partialTick;
            poseStack.translate(Mth.sin(tt * 3.7F) * 0.07F, Mth.sin(tt * 5.3F) * 0.05F,
                    Mth.sin(tt * 4.1F) * 0.07F);
            if (Math.floorMod(entity.tickCount * 5 + entity.getId(), 11) < 2) alpha *= 0.55F;
        }

        // Billboard: la cara siempre se orienta hacia quien la observa.
        poseStack.mulPose(Axis.YP.rotation(ang));
        if (headPeek) {
            float lean = entity.getPeekLean() + Mth.sin(t * 0.045F) * 1.25F;
            float sign = entity.getPeekLean() < 0.0F ? -1.0F : 1.0F;
            float pivot = y0 + drawH * 0.52F;
            poseStack.translate(0.0D, pivot, 0.0D);
            // Giro de profundidad y ladeo suave para que parezca una cabeza que espia
            // desde el canto del bloque, no una imagen recortada pegada a la pantalla.
            poseStack.mulPose(Axis.YP.rotationDegrees(sign * 7.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-2.5F + Mth.sin(t * 0.031F) * 1.2F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(lean));
            poseStack.translate(0.0D, -pivot, 0.0D);
        }

        int light = packedLight; // el cuerpo y el rostro reciben la luz real del mundo
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(v.texture));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        float hw = drawW / 2.0F;
        vertex(vc, m, n, -hw, y0, u0, v1, alpha, light);
        vertex(vc, m, n, hw, y0, u1, v1, alpha, light);
        vertex(vc, m, n, hw, y1, u1, v0, alpha, light);
        vertex(vc, m, n, -hw, y1, u0, v0, alpha, light);

        // Overlay UV sincronizado con la textura facial: solo brillan los ojos.
        float eyePulse = 0.88F + 0.12F * Mth.sin(t * 0.11F + entity.getId() * 0.37F);
        if (Math.floorMod(entity.tickCount * 11 + entity.getId(), 137) < 2) eyePulse *= 0.35F;
        float eyeAlpha = alpha * eyePulse;
        VertexConsumer eyes = buffer.getBuffer(RenderType.eyes(v.eyeTexture));
        float zEye = 0.012F;
        vertexZ(eyes, m, n, -hw, y0, zEye, u0, v1, eyeAlpha, LightTexture.FULL_BRIGHT);
        vertexZ(eyes, m, n, hw, y0, zEye, u1, v1, eyeAlpha, LightTexture.FULL_BRIGHT);
        vertexZ(eyes, m, n, hw, y1, zEye, u1, v0, eyeAlpha, LightTexture.FULL_BRIGHT);
        vertexZ(eyes, m, n, -hw, y1, zEye, u0, v0, eyeAlpha, LightTexture.FULL_BRIGHT);

        if (entity.hasHandAttack() && entity.getHandProgress() > 0.01F) {
            renderHand(entity, poseStack, buffer, ang, light);
        }
        poseStack.popPose();
    }

    private static void renderHand(HorrorEntity entity, PoseStack poseStack, MultiBufferSource buffer,
                                   float cameraAngle, int light) {
        float progress = Mth.clamp(entity.getHandProgress(), 0.0F, 1.0F);
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        if (progress <= 0.001F) return;

        float dx = entity.getHandX();
        float shoulderY = entity.getMode() == HorrorMode.PEEK
                ? HorrorEntity.HEAD_CENTER - 0.18F
                : Mth.clamp(entity.spriteHeight() * 0.68F, 1.10F, 2.12F);
        float dy = entity.getHandY() - shoulderY;
        float dz = entity.getHandZ();
        float screenX = dx * Mth.cos(cameraAngle) - dz * Mth.sin(cameraAngle);
        float screenY = dy;
        if (Math.abs(screenX) < 0.34F) {
            screenX = (entity.getId() % 2 == 0 ? 1.0F : -1.0F) * 0.72F;
            screenY = Mth.clamp(screenY, -0.35F, 0.35F);
        }
        float projected = Mth.sqrt(screenX * screenX + screenY * screenY);
        if (projected < 0.001F) return;
        float ux = screenX / projected;
        float uy = screenY / projected;
        float worldReach = Mth.sqrt(dx * dx + (entity.getHandY() * entity.getHandY()) + dz * dz);
        float visualLength = Mth.clamp(0.72F + worldReach * 0.38F, 0.95F, 3.15F) * eased;
        float baseX = ux >= 0.0F ? 0.16F : -0.16F;
        float baseY = shoulderY;
        float tipX = baseX + ux * visualLength;
        float tipY = baseY + uy * visualLength;
        float halfWidth = 0.22F + progress * 0.08F;
        float px = -uy * halfWidth;
        float py = ux * halfWidth;

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 0.052D);
        PoseStack.Pose handPose = poseStack.last();
        float alpha = Mth.clamp(0.14F + eased * 0.96F, 0.0F, 1.0F);
        drawHandQuad(buffer.getBuffer(RenderType.entityTranslucent(HAND)), handPose.pose(), handPose.normal(),
                baseX, baseY, tipX, tipY, px, py, alpha, light);
        poseStack.popPose();
    }

    private static void drawHandQuad(VertexConsumer vc, Matrix4f m, Matrix3f n,
                                     float baseX, float baseY, float tipX, float tipY,
                                     float px, float py, float alpha, int light) {
        vertexZ(vc, m, n, baseX + px, baseY + py, 0.0F, 1.0F, 1.0F, alpha, light);
        vertexZ(vc, m, n, baseX - px, baseY - py, 0.0F, 0.0F, 1.0F, alpha, light);
        vertexZ(vc, m, n, tipX - px, tipY - py, 0.0F, 0.0F, 0.0F, alpha, light);
        vertexZ(vc, m, n, tipX + px, tipY + py, 0.0F, 1.0F, 0.0F, alpha, light);
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
