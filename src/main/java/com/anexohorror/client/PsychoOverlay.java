package com.anexohorror.client;

import com.anexohorror.AnexoHorror;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Efectos de pantalla de terror analogico mientras miras una presencia:
 * tinte mauve, vineta, lineas de escaneo, grano y cortes de cinta. Sin parpadeos fuertes (sin estroboscopio).
 */
@Mod.EventBusSubscriber(modid = AnexoHorror.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PsychoOverlay {
    /** 0..1, actualizado por PsychoClientTick. */
    public static volatile float glitch;
    private static final Random RNG = new Random();

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("psycho", (gui, g, partialTick, w, h) -> render(g, w, h));
    }

    private static void render(GuiGraphics g, int w, int h) {
        float k = glitch;
        if (k < 0.02F || Minecraft.getInstance().options.hideGui) return;

        // 1) Tinte mauve oscuro (paleta de la entidad).
        g.fill(0, 0, w, h, argb((int) (70 * k), 0x3A, 0x28, 0x30));

        // 2) Vineta que se cierra.
        int layers = 14;
        int bw = Math.max(2, w / 60);
        int bh = Math.max(2, h / 60);
        for (int i = 0; i < layers; i++) {
            int a = (int) (110 * k * (layers - i) / (float) layers);
            int col = argb(a, 0x10, 0x08, 0x0A);
            g.fill(i * bw, 0, (i + 1) * bw, h, col);
            g.fill(w - (i + 1) * bw, 0, w - i * bw, h, col);
            g.fill(0, i * bh, w, (i + 1) * bh, col);
            g.fill(0, h - (i + 1) * bh, w, h - i * bh, col);
        }

        // 3) Lineas de escaneo.
        int scan = argb((int) (38 * k), 0, 0, 0);
        for (int y = 0; y < h; y += 4) g.fill(0, y, w, y + 1, scan);

        // 4) Grano.
        int grains = (int) (30 + 70 * k);
        for (int i = 0; i < grains; i++) {
            int x = RNG.nextInt(w);
            int y = RNG.nextInt(h);
            int s = 1 + RNG.nextInt(2);
            int c = RNG.nextBoolean() ? 0xFF : 0x20;
            g.fill(x, y, x + s, y + s, argb((int) (60 * k), c, c, c));
        }

        // 5) Cortes de cinta: bandas horizontales con aberracion cromatica, mas frecuentes con k alto.
        if (RNG.nextFloat() < 0.04F + 0.12F * k) {
            int bandH = 4 + RNG.nextInt(14);
            int y = RNG.nextInt(Math.max(1, h - bandH));
            int off = 2 + RNG.nextInt(6);
            g.fill(0, y, w, y + bandH, argb((int) (34 * k), 0xB9, 0xAA, 0xB0));
            g.fill(off, y, w, y + 1, argb((int) (90 * k), 0xFF, 0x30, 0x40));
            g.fill(0, y + bandH - 1, w - off, y + bandH, argb((int) (90 * k), 0x30, 0xC0, 0xFF));
        }
    }

    private static int argb(int a, int r, int gr, int b) {
        a = Math.max(0, Math.min(255, a));
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    private PsychoOverlay() {}
}
