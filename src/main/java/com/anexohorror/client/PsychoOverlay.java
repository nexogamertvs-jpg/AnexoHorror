package com.anexohorror.client;

import com.anexohorror.AnexoHorror;
import com.anexohorror.ClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Capa de terror analogico: tinte/vineta/escaneo/grano al mirar una presencia, frases rojas,
 * signos extranos y flashbacks a pantalla completa. El estado lo actualiza PsychoClientTick.
 */
@Mod.EventBusSubscriber(modid = AnexoHorror.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PsychoOverlay {
    /** 0..1, cuanto estas mirando a una presencia. */
    public static volatile float glitch;

    // frase en pantalla
    static String text;
    static int textTicks;
    static int textTotal;
    static float textYFrac = 0.4F;

    // signos
    static final class Symbol {
        float x, y, scale;
        int ttl, total, color;
        String glyph;
    }
    static final List<Symbol> SYMBOL_LIST = new ArrayList<>();
    static final String GLYPHS = "ЖΨΞΩΣЯ∴∵※†‡¶§ᚦᛉᛟᚷᛞᛃᚨ☥╳░▒";
    private static final int[] SYMBOL_COLORS = {0xB0101C, 0xB9AAB0, 0x7A5A80, 0x8A1020};

    // flashback
    static int flashTicks;
    static int flashTotal;
    static int flashImage;
    private static final ResourceLocation[] FLASH = {
            new ResourceLocation(AnexoHorror.MODID, "textures/gui/flash1.png"),
            new ResourceLocation(AnexoHorror.MODID, "textures/gui/flash2.png")};
    private static final int[] FLASH_W = {712, 567};
    private static final int[] FLASH_H = {1080, 1080};

    private static final Random RNG = new Random();

    static void reset() {
        glitch = 0.0F;
        textTicks = 0;
        flashTicks = 0;
        SYMBOL_LIST.clear();
    }

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("psycho", (gui, g, partialTick, w, h) -> render(g, w, h));
    }

    private static void render(GuiGraphics g, int w, int h) {
        if (Minecraft.getInstance().options.hideGui) return;
        renderGlitch(g, w, h);
        renderSymbols(g, w, h);
        renderText(g, w, h);
        renderFlash(g, w, h);
    }

    // ---------------------------------------------------------------- tinte, vineta, escaneo, grano

    private static void renderGlitch(GuiGraphics g, int w, int h) {
        float k = glitch;
        if (k < 0.02F || !ClientConfig.SCREEN_EFFECTS.get()) return;

        g.fill(0, 0, w, h, argb((int) (70 * k), 0x3A, 0x28, 0x30));

        int layers = 10;
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

        int scan = argb((int) (38 * k), 0, 0, 0);
        for (int y = 0; y < h; y += 6) g.fill(0, y, w, y + 1, scan);

        int grains = (int) (20 + 40 * k);
        for (int i = 0; i < grains; i++) {
            int x = RNG.nextInt(w);
            int y = RNG.nextInt(h);
            int s = 1 + RNG.nextInt(2);
            int c = RNG.nextBoolean() ? 0xFF : 0x20;
            g.fill(x, y, x + s, y + s, argb((int) (60 * k), c, c, c));
        }

        if (RNG.nextFloat() < 0.04F + 0.12F * k) {
            int bandH = 4 + RNG.nextInt(14);
            int y = RNG.nextInt(Math.max(1, h - bandH));
            int off = 2 + RNG.nextInt(6);
            g.fill(0, y, w, y + bandH, argb((int) (34 * k), 0xB9, 0xAA, 0xB0));
            g.fill(off, y, w, y + 1, argb((int) (90 * k), 0xFF, 0x30, 0x40));
            g.fill(0, y + bandH - 1, w - off, y + bandH, argb((int) (90 * k), 0x30, 0xC0, 0xFF));
        }
    }

    // ---------------------------------------------------------------- signos extranos (pocos, sin saturar)

    private static void renderSymbols(GuiGraphics g, int w, int h) {
        if (SYMBOL_LIST.isEmpty()) return;
        Font font = Minecraft.getInstance().font;
        for (Symbol sy : SYMBOL_LIST) {
            if (RNG.nextInt(5) == 0) continue; // parpadeo
            float life = sy.ttl / (float) sy.total;
            float in = Math.min(1.0F, (sy.total - sy.ttl + 1) / 3.0F);
            int alpha = (int) (150 * Math.min(1.0F, life * 4.0F) * in);
            if (alpha < 8) continue;
            drawScaled(g, font, sy.glyph, sy.x * w, sy.y * h, sy.scale, (alpha << 24) | sy.color);
        }
    }

    // ---------------------------------------------------------------- frases

    private static void renderText(GuiGraphics g, int w, int h) {
        if (textTicks <= 0 || text == null) return;
        Font font = Minecraft.getInstance().font;
        int age = textTotal - textTicks;
        float a = Math.min(1.0F, age / 3.0F) * Math.min(1.0F, textTicks / 18.0F); // entrada seca, salida suave
        if (RNG.nextFloat() < 0.07F) a *= 0.25F;                                  // parpadeo
        String t = text;
        if (RNG.nextFloat() < 0.10F) { // una letra se corrompe por un instante
            char[] c = t.toCharArray();
            int i = RNG.nextInt(c.length);
            if (c[i] != ' ') c[i] = GLYPHS.charAt(RNG.nextInt(GLYPHS.length()));
            t = new String(c);
        }
        int tw = font.width(t);
        float s = Mth.clamp(w * 0.62F / Math.max(1, tw), 1.5F, 7.0F);
        float x = (w - tw * s) / 2.0F + (RNG.nextFloat() - 0.5F) * (age < 6 ? 14.0F : 3.0F);
        float y = h * textYFrac + (RNG.nextFloat() - 0.5F) * 3.0F;

        // banda oscura para que se lea sobre cualquier fondo
        g.fill(0, (int) (y - 6 * s * 0.5F), w, (int) (y + 9 * s + 6 * s * 0.5F), argb((int) (110 * a), 0, 0, 0));
        // fantasmas cromaticos
        drawScaled(g, font, t, x + 4.0F, y, s, argb((int) (90 * a), 0x30, 0xD0, 0xFF));
        drawScaled(g, font, t, x - 4.0F, y, s, argb((int) (120 * a), 0xFF, 0x20, 0x40));
        // resplandor rojo
        for (int[] o : new int[][]{{-2, 0}, {2, 0}, {0, -2}, {0, 2}}) {
            drawScaled(g, font, t, x + o[0], y + o[1], s, argb((int) (60 * a), 0xC8, 0x10, 0x1E));
        }
        // rojo sangre
        drawScaled(g, font, t, x, y, s, argb((int) (255 * a), 0xC8, 0x10, 0x1E));
    }

    private static void drawScaled(GuiGraphics g, Font font, String s, float x, float y, float scale, int color) {
        if (((color >>> 24) & 255) < 8) return;
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(font, s, 0, 0, color, false);
        g.pose().popPose();
    }

    // ---------------------------------------------------------------- flashback a pantalla completa

    private static void renderFlash(GuiGraphics g, int w, int h) {
        if (flashTicks <= 0) return;
        int age = flashTotal - flashTicks;
        if (age == 4 || age == 9) return; // el destello "tartamudea"
        g.fill(0, 0, w, h, 0xFF000000);
        int tw = FLASH_W[flashImage];
        int th = FLASH_H[flashImage];
        // Se ajusta entera a la pantalla (sin recortar): la entidad se ve completa.
        float s = Math.min(w / (float) tw, h / (float) th);
        int dw = Math.round(tw * s);
        int dh = Math.round(th * s);
        int x = (w - dw) / 2 + (int) ((RNG.nextFloat() - 0.5F) * w * 0.012F);
        int y = (h - dh) / 2 + (int) ((RNG.nextFloat() - 0.5F) * h * 0.012F);
        ResourceLocation rl = FLASH[flashImage];

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1.0F, 0.15F, 0.2F, 0.45F);
        g.blit(rl, x - 6, y, dw, dh, 0.0F, 0.0F, tw, th, tw, th);
        g.setColor(0.1F, 0.9F, 1.0F, 0.45F);
        g.blit(rl, x + 6, y, dw, dh, 0.0F, 0.0F, tw, th, tw, th);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(rl, x, y, dw, dh, 0.0F, 0.0F, tw, th, tw, th);

        int scan = argb(40, 0, 0, 0);
        for (int yy = 0; yy < h; yy += 5) g.fill(0, yy, w, yy + 1, scan);
        for (int i = 0; i < 60; i++) {
            int gx = RNG.nextInt(w);
            int gy = RNG.nextInt(h);
            g.fill(gx, gy, gx + 2, gy + 2, argb(70, 0xFF, 0xFF, 0xFF));
        }
    }

    private static int argb(int a, int r, int gr, int b) {
        a = Math.max(0, Math.min(255, a));
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    private PsychoOverlay() {}
}
