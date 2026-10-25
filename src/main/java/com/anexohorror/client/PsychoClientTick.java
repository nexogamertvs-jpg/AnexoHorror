package com.anexohorror.client;

import com.anexohorror.AnexoHorror;
import com.anexohorror.entity.HorrorEntity;
import com.anexohorror.entity.HorrorMode;
import com.anexohorror.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Calcula cuanto estas mirando a una presencia y alimenta los efectos de pantalla. */
@Mod.EventBusSubscriber(modid = AnexoHorror.MODID, value = Dist.CLIENT)
public final class PsychoClientTick {
    private static float intensity;
    private static int stare;
    private static int ticks;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            intensity = 0.0F; stare = 0; PsychoOverlay.glitch = 0.0F;
            return;
        }
        if (mc.isPaused()) return;

        float best = 0.0F;
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        for (Entity en : mc.level.entitiesForRendering()) {
            if (!(en instanceof HorrorEntity he)) continue;
            if (he.getMode() == HorrorMode.PERIPHERAL) continue;
            Vec3 to = he.position().add(0.0D, he.spriteHover() + he.spriteHeight() * 0.6D, 0.0D).subtract(eye);
            double d = to.length();
            if (d > 64.0D || d < 0.5D) continue;
            double dot = look.dot(to.scale(1.0D / d));
            if (dot < 0.6D || !p.hasLineOfSight(he)) continue;
            float prox = (float) (1.0D - d / 64.0D);
            float strength = (float) ((dot - 0.6D) / 0.4D) * (0.35F + 0.65F * prox);
            if (he.getVariant().model) strength *= 1.3F;
            best = Math.max(best, strength);
        }

        intensity += (best - intensity) * 0.08F;
        stare = best > 0.15F ? Math.min(200, stare + 1) : Math.max(0, stare - 2);
        float g = Math.min(1.0F, intensity * (0.4F + 0.6F * stare / 200.0F));
        PsychoOverlay.glitch = g;

        // Latido sincronizado cuando llevas rato sosteniendo la mirada.
        if (g > 0.35F && ++ticks % 34 == 0) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HEARTBEAT.get(), 1.0F, 0.5F * g));
        }
    }

    private PsychoClientTick() {}
}
