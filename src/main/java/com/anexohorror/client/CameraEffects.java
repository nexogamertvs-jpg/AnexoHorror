package com.anexohorror.client;

import com.anexohorror.AnexoHorror;
import com.anexohorror.ClientConfig;
import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Temblor de camara sutil (mirada fija a una presencia) o fuerte (screamer). */
@Mod.EventBusSubscriber(modid = AnexoHorror.MODID, value = Dist.CLIENT)
public final class CameraEffects {
    @SubscribeEvent
    public static void onAngles(ViewportEvent.ComputeCameraAngles event) {
        if (PsychoClientTick.isLockActive()) { // mirada congelada: la camara no obedece al raton
            event.setYaw(PsychoClientTick.lockYaw());
            event.setPitch(PsychoClientTick.lockPitch());
        }
        if (!ClientConfig.CAMERA_SHAKE.get()) return;
        float k = Math.max(PsychoOverlay.glitch * 0.6F, PsychoOverlay.screamerShake());
        if (k < 0.04F) return;
        double t = Util.getMillis() / 1000.0D;
        event.setRoll(event.getRoll() + (float) (Math.sin(t * 41.0D) * 0.9D + Math.sin(t * 17.3D) * 0.6D) * k);
        event.setPitch(event.getPitch() + (float) Math.sin(t * 53.0D) * 0.35F * k);
        event.setYaw(event.getYaw() + (float) Math.sin(t * 47.0D) * 0.35F * k);
    }

    private CameraEffects() {}
}
